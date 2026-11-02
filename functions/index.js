"use strict";

/**
 * Trusted money, booking and paid-chat endpoints.
 *
 * The Android client may REQUEST an operation but may not declare it paid,
 * verified, complete, or eligible for a payout. All balances are integer cents.
 * Deposits and external booking receipts require independent ADMIN verification.
 */
const { createHash } = require("node:crypto");
const { initializeApp } = require("firebase-admin/app");
const { getFirestore, FieldValue } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");
const { onCall, HttpsError } = require("firebase-functions/v2/https");

const app = initializeApp();
const DATABASE_ID = process.env.FIRESTORE_DATABASE_ID ||
  "ai-studio-android-nightlyc-90974dd0-99e1-42a7-a4b5-a3bd95657476";
const db = getFirestore(app, DATABASE_ID);
const region = "europe-west1";
const options = { region, maxInstances: 25 };

function auth(req) {
  if (!req.auth || !req.auth.uid) throw new HttpsError("unauthenticated", "Sign in with Firebase first.");
  return req.auth.uid;
}
function admin(req) {
  auth(req);
  if (req.auth.token.admin !== true) throw new HttpsError("permission-denied", "Server admin claim required.");
}
function str(v, label, min = 1, max = 150) {
  if (typeof v !== "string" || v.trim().length < min || v.trim().length > max)
    throw new HttpsError("invalid-argument", "Invalid " + label);
  return v.trim();
}
function whole(v, label, min, max) {
  if (!Number.isSafeInteger(v) || v < min || v > max)
    throw new HttpsError("invalid-argument", "Invalid " + label);
  return v;
}
function hash(v) {
  return createHash("sha256").update(v).digest("hex");
}
function cents(dollars) { return whole(dollars, "amountUsd", 1, 100000) * 100; }
function amount(doc, key = "balanceCents") {
  const x = doc.exists ? doc.get(key) : 0;
  if (!Number.isSafeInteger(x) || x < 0) throw new HttpsError("failed-precondition", "Invalid server balance");
  return x;
}
async function verifiedUpload(uid, path) {
  const key = str(path, "storagePath", 16, 300);
  if (!/^receipts\/[a-zA-Z0-9_-]{6,128}\/[a-zA-Z0-9_-]{8,128}\.(jpg|jpeg|png|pdf)$/.test(key) ||
      !key.startsWith("receipts/" + uid + "/"))
    throw new HttpsError("permission-denied", "Receipt must be owned by the caller.");
  const file = getStorage().bucket().file(key);
  const [metadata] = await file.getMetadata().catch(() => {
    throw new HttpsError("failed-precondition", "Upload the receipt before submitting it.");
  });
  if (!["image/jpeg", "image/png", "application/pdf"].includes(metadata.contentType) ||
      Number(metadata.size) < 100 || Number(metadata.size) > 8 * 1024 * 1024)
    throw new HttpsError("invalid-argument", "Unsupported receipt content.");
  return key;
}

exports.createBookingRequest = onCall(options, async (req) => {
  const uid = auth(req);
  const d = req.data || {};
  const serviceId = str(d.serviceId, "serviceId", 1, 128);
  const bookingDate = str(d.bookingDate, "bookingDate", 4, 50);
  const timeSlot = str(d.timeSlot, "timeSlot", 2, 50);
  const notes = typeof d.notes === "string" ? d.notes.trim().slice(0, 400) : "";
  const guests = whole(d.guestCount, "guestCount", 1, 20);
  const payment = str(d.paymentMethod, "paymentMethod", 6, 8);
  if (!["wallet", "external"].includes(payment))
    throw new HttpsError("invalid-argument", "Invalid payment method");
  const serviceRef = db.collection("published_services").doc(serviceId);
  const bookingRef = db.collection("users").doc(uid).collection("bookings").doc();
  const walletRef = db.collection("wallets").doc(uid);
  const ledgerRef = db.collection("wallet_ledger").doc("booking_" + bookingRef.id);
  let totalCents;
  await db.runTransaction(async (tx) => {
    const service = await tx.get(serviceRef);
    if (!service.exists || service.get("status") !== "active")
      throw new HttpsError("not-found", "Service is not active");
    const priceUsd = whole(service.get("priceUsd"), "servicePrice", 1, 10000);
    totalCents = priceUsd * guests * 100;
    if (totalCents > 1000000) throw new HttpsError("invalid-argument", "Booking amount exceeds limit");
    const providerId = str(service.get("authorId"), "providerId", 6, 128);
    if (providerId === uid) throw new HttpsError("failed-precondition", "Cannot buy your own service");
    let balance = 0;
    if (payment === "wallet") {
      balance = amount(await tx.get(walletRef));
      if (balance < totalCents) throw new HttpsError("failed-precondition", "Insufficient verified funds");
    }
    tx.create(bookingRef, {
      userId: uid, serviceId, venueId: serviceId, venueName: service.get("title"),
      cityName: service.get("cityName"), country: service.get("country"),
      serviceTitle: service.get("title"), category: service.get("category"),
      providerId, bookingDate, timeSlot, guestCount: guests, notes,
      totalPriceUsd: totalCents / 100, totalCents,
      paymentMethodTitle: payment, receiptImageUri: "", transactionReference: "",
      status: payment === "wallet" ? "confirmed" : "pending_payment",
      createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()
    });
    if (payment === "wallet") {
      tx.set(walletRef, { balanceCents: balance - totalCents, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
      tx.create(ledgerRef, { userId: uid, amountCents: -totalCents, type: "booking", bookingId: bookingRef.id, createdAt: FieldValue.serverTimestamp() });
    }
  });
  return { bookingId: bookingRef.id, status: payment === "wallet" ? "confirmed" : "pending_payment", totalCents };
});

exports.submitPaymentReceipt = onCall(options, async (req) => {
  const uid = auth(req);
  const d = req.data || {};
  const method = str(d.method, "method", 5, 20);
  if (!["SHAM_CASH", "USDT_BEP20"].includes(method))
    throw new HttpsError("invalid-argument", "Unsupported method");
  const reference = str(d.transactionReference, "transactionReference", 6, 120);
  const purpose = str(d.purpose, "purpose", 6, 7);
  if (!["wallet", "booking"].includes(purpose))
    throw new HttpsError("invalid-argument", "Invalid purpose");
  const path = await verifiedUpload(uid, d.storagePath);
  const receiptId = str(d.submissionId, "submissionId", 8, 100);
  if (!/^[a-zA-Z0-9_-]+$/.test(receiptId)) throw new HttpsError("invalid-argument", "Invalid submission ID");
  const receiptRef = db.collection("payment_receipts").doc(uid + "_" + receiptId);
  let balanceCents = 0;
  let bookingId = null;
  if (purpose === "wallet") balanceCents = cents(d.amountUsd);
  if (purpose === "booking") {
    bookingId = str(d.bookingId, "bookingId", 6, 128);
    const booking = await db.collection("users").doc(uid).collection("bookings").doc(bookingId).get();
    if (!booking.exists || booking.get("status") !== "pending_payment")
      throw new HttpsError("failed-precondition", "Booking is not waiting for payment");
    balanceCents = whole(booking.get("totalCents"), "bookingTotal", 100, 1000000);
  }
  await db.runTransaction(async (tx) => {
    const current = await tx.get(receiptRef);
    if (current.exists) {
      if (current.get("userId") !== uid || current.get("storagePath") !== path)
        throw new HttpsError("already-exists", "Submission ID already used");
      return;
    }
    tx.create(receiptRef, {
      userId: uid, purpose, bookingId, method, transactionReference: reference,
      storagePath: path, amountCents: balanceCents, status: "pending",
      createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()
    });
  });
  return { receiptId: receiptRef.id, status: "pending" };
});

exports.approvePaymentReceipt = onCall(options, async (req) => {
  admin(req);
  const d = req.data || {};
  const receiptId = str(d.receiptId, "receiptId", 9, 230);
  const receiptRef = db.collection("payment_receipts").doc(receiptId);
  await db.runTransaction(async (tx) => {
    const receipt = await tx.get(receiptRef);
    if (!receipt.exists || receipt.get("status") !== "pending")
      throw new HttpsError("failed-precondition", "Only pending receipts can be approved");
    const uid = receipt.get("userId");
    const method = receipt.get("method");
    const reference = receipt.get("transactionReference");
    const paymentKey = hash(method + ":" + reference.toLowerCase().replace(/\s+/g, ""));
    const uniqueRef = db.collection("verified_payment_refs").doc(paymentKey);
    const used = await tx.get(uniqueRef);
    if (used.exists) throw new HttpsError("already-exists", "Payment reference has already been credited");
    const total = whole(receipt.get("amountCents"), "amountCents", 100, 10000000);
    if (receipt.get("purpose") === "wallet") {
      const walletRef = db.collection("wallets").doc(uid);
      const wallet = await tx.get(walletRef);
      tx.set(walletRef, { balanceCents: amount(wallet) + total, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
      tx.create(db.collection("wallet_ledger").doc("deposit_" + receipt.id), {
        userId: uid, amountCents: total, type: "verified_deposit",
        receiptId: receipt.id, createdAt: FieldValue.serverTimestamp()
      });
    } else {
      const bookingId = receipt.get("bookingId");
      const bookingRef = db.collection("users").doc(uid).collection("bookings").doc(bookingId);
      const booking = await tx.get(bookingRef);
      if (!booking.exists || booking.get("status") !== "pending_payment" ||
          booking.get("totalCents") !== total)
        throw new HttpsError("failed-precondition", "Booking amount or status changed");
      tx.update(bookingRef, { status: "confirmed", updatedAt: FieldValue.serverTimestamp() });
    }
    tx.create(uniqueRef, { receiptId: receipt.id, verifiedAt: FieldValue.serverTimestamp() });
    tx.update(receiptRef, { status: "verified", verifiedAt: FieldValue.serverTimestamp(), verifiedBy: req.auth.uid });
  });
  return { status: "verified" };
});

exports.completeBooking = onCall(options, async (req) => {
  admin(req);
  const uid = str(req.data?.buyerId, "buyerId", 6, 128);
  const bookingId = str(req.data?.bookingId, "bookingId", 6, 128);
  const bookingRef = db.collection("users").doc(uid).collection("bookings").doc(bookingId);
  await db.runTransaction(async (tx) => {
    const booking = await tx.get(bookingRef);
    if (!booking.exists || booking.get("status") !== "confirmed")
      throw new HttpsError("failed-precondition", "Booking must be confirmed first");
    const providerId = str(booking.get("providerId"), "providerId", 6, 128);
    const providerWallet = db.collection("creator_wallets").doc(providerId);
    const wallet = await tx.get(providerWallet);
    const gross = whole(booking.get("totalCents"), "totalCents", 100, 1000000);
    const share = Math.floor(gross * 80 / 100);
    tx.set(providerWallet, {
      availableCents: amount(wallet, "availableCents") + share,
      reservedCents: amount(wallet, "reservedCents"),
      updatedAt: FieldValue.serverTimestamp()
    }, { merge: true });
    tx.update(bookingRef, { status: "completed", updatedAt: FieldValue.serverTimestamp() });
    tx.create(db.collection("wallet_ledger").doc("earnings_" + bookingId), {
      userId: providerId, amountCents: share, grossCents: gross, type: "creator_earnings",
      buyerId: uid, bookingId, createdAt: FieldValue.serverTimestamp()
    });
  });
  return { status: "completed" };
});

exports.requestWithdrawal = onCall(options, async (req) => {
  const uid = auth(req);
  const amountCents = cents(req.data?.amountUsd);
  const destination = str(req.data?.destination, "destination", 8, 160);
  const withdrawalRef = db.collection("withdrawals").doc();
  const walletRef = db.collection("creator_wallets").doc(uid);
  await db.runTransaction(async (tx) => {
    const wallet = await tx.get(walletRef);
    const available = amount(wallet, "availableCents");
    if (available < amountCents) throw new HttpsError("failed-precondition", "Insufficient creator earnings");
    tx.set(walletRef, {
      availableCents: available - amountCents,
      reservedCents: amount(wallet, "reservedCents") + amountCents,
      updatedAt: FieldValue.serverTimestamp()
    }, { merge: true });
    tx.create(withdrawalRef, {
      userId: uid, amountCents, destination, status: "pending",
      createdAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp()
    });
  });
  return { withdrawalId: withdrawalRef.id, status: "pending" };
});

exports.recordWithdrawalPayout = onCall(options, async (req) => {
  admin(req);
  const id = str(req.data?.withdrawalId, "withdrawalId", 6, 128);
  const externalReference = str(req.data?.externalReference, "externalReference", 6, 160);
  const withdrawRef = db.collection("withdrawals").doc(id);
  await db.runTransaction(async (tx) => {
    const withdraw = await tx.get(withdrawRef);
    if (!withdraw.exists || withdraw.get("status") !== "pending")
      throw new HttpsError("failed-precondition", "Withdrawal is not pending");
    const walletRef = db.collection("creator_wallets").doc(withdraw.get("userId"));
    const wallet = await tx.get(walletRef);
    const reserved = amount(wallet, "reservedCents");
    const withdrawalAmount = withdraw.get("amountCents");
    if (reserved < withdrawalAmount) throw new HttpsError("failed-precondition", "Insufficient reserved earnings");
    tx.update(walletRef, { reservedCents: reserved - withdrawalAmount, updatedAt: FieldValue.serverTimestamp() });
    tx.update(withdrawRef, {
      status: "paid", externalReference, paidAt: FieldValue.serverTimestamp(),
      paidBy: req.auth.uid, updatedAt: FieldValue.serverTimestamp()
    });
  });
  return { status: "paid" };
});

exports.unlockPaidChat = onCall(options, async (req) => {
  const uid = auth(req);
  const creatorId = str(req.data?.creatorId, "creatorId", 6, 128);
  if (uid === creatorId) throw new HttpsError("invalid-argument", "Cannot pay yourself");
  const threadId = hash([uid, creatorId].sort().join(":"));
  const threadRef = db.collection("chat_threads").doc(threadId);
  const profileRef = db.collection("users").doc(creatorId);
  const walletRef = db.collection("wallets").doc(uid);
  await db.runTransaction(async (tx) => {
    const thread = await tx.get(threadRef);
    if (thread.exists) {
      if (!thread.get("participants").includes(uid)) throw new HttpsError("permission-denied", "Not your chat");
      return;
    }
    const profile = await tx.get(profileRef);
    if (!profile.exists) throw new HttpsError("not-found", "Creator does not exist");
    const priceUsd = profile.get("isPaidChatEnabled") === true
      ? whole(profile.get("chatPriceUsd"), "chatPriceUsd", 1, 5000) : 0;
    const cost = priceUsd * 100;
    let currentBalance = 0;
    if (cost > 0) {
      currentBalance = amount(await tx.get(walletRef));
      if (currentBalance < cost) throw new HttpsError("failed-precondition", "Insufficient verified wallet funds");
    }
    tx.create(threadRef, {
      participants: [uid, creatorId], creatorId, buyerId: uid, unlocked: true,
      paidCents: cost, createdAt: FieldValue.serverTimestamp()
    });
    if (cost > 0) {
      tx.set(walletRef, { balanceCents: currentBalance - cost, updatedAt: FieldValue.serverTimestamp() }, { merge: true });
      tx.create(db.collection("wallet_ledger").doc("chat_" + threadId), {
        userId: uid, amountCents: -cost, type: "paid_chat", threadId,
        createdAt: FieldValue.serverTimestamp()
      });
    }
  });
  return { threadId, status: "unlocked" };
});

exports.sendVerifiedMessage = onCall(options, async (req) => {
  const uid = auth(req);
  const threadId = str(req.data?.threadId, "threadId", 64, 64);
  const message = str(req.data?.message, "message", 1, 2000);
  const thread = await db.collection("chat_threads").doc(threadId).get();
  if (!thread.exists || !thread.get("unlocked") || !thread.get("participants").includes(uid))
    throw new HttpsError("permission-denied", "Chat is locked");
  const doc = await thread.ref.collection("messages").add({
    senderId: uid, text: message, createdAt: FieldValue.serverTimestamp()
  });
  return { messageId: doc.id, status: "sent" };
});
