const { test, before } = require("node:test");
const assert = require("node:assert/strict");
process.env.GCLOUD_PROJECT = "demo-layali";
process.env.FIRESTORE_DATABASE_ID =
  "ai-studio-android-nightlyc-90974dd0-99e1-42a7-a4b5-a3bd95657476";
const api = require("../../src/index");
const { getFirestore } = require("firebase-admin/firestore");
const { getAuth } = require("firebase-admin/auth");
const db = getFirestore(process.env.FIRESTORE_DATABASE_ID),
  auth = getAuth();
const invoke = (name, uid, data = {}) =>
  api[name].run({ auth: uid ? { uid, token: {} } : null, data });
async function seed(uid, role = "member", balance = 0, earnings = 0) {
  await auth.createUser({ uid, email: `${uid}@example.test` });
  await db
    .doc(`accounts/${uid}`)
    .set({ status: "active", role, displayName: uid, createdAt: new Date() });
  await db.doc(`wallets/${uid}`).set({ balanceCents: balance });
  await db
    .doc(`creatorBalances/${uid}`)
    .set({ availableCents: earnings, heldCents: 0, lifetimeCents: earnings });
}
before(async () => {
  await seed("viewer", "member", 10000);
  await seed("creator", "creator");
  await seed("admin");
  await auth.setCustomUserClaims("admin", { admin: true });
  await seed("creator2", "creator", 0, 10000);
});
test("unauthenticated calls cannot create withdrawals", async () => {
  await assert.rejects(invoke("requestWithdrawal", null, {}), /سجل الدخول/);
});
test("concurrent duplicate gift credits exactly once at 80%", async () => {
  const args = {
    creatorId: "creator",
    amountCents: 1000,
    requestId: "same-gift",
  };
  await Promise.all([
    invoke("sendGift", "viewer", args),
    invoke("sendGift", "viewer", args),
  ]);
  assert.equal(
    (await db.doc("wallets/viewer").get()).data().balanceCents,
    9000,
  );
  assert.equal(
    (await db.doc("creatorBalances/creator").get()).data().availableCents,
    800,
  );
});
test("concurrent withdrawals cannot overdraw available earnings", async () => {
  const base = {
    amountCents: 7000,
    method: "SHAM_CASH",
    destination: "account",
    beneficiary: "Creator",
  };
  const results = await Promise.allSettled([
    invoke("requestWithdrawal", "creator2", { ...base, requestId: "w1" }),
    invoke("requestWithdrawal", "creator2", { ...base, requestId: "w2" }),
  ]);
  assert.equal(results.filter((r) => r.status === "fulfilled").length, 1);
  const b = (await db.doc("creatorBalances/creator2").get()).data();
  assert.equal(b.availableCents, 3000);
  assert.equal(b.heldCents, 7000);
});
test("cancel returns held balance only once", async () => {
  const q = await db
    .collection("withdrawals")
    .where("ownerId", "==", "creator2")
    .get();
  const id = q.docs[0].id;
  await invoke("processWithdrawal", "creator2", {
    id,
    action: "cancel",
    reason: "cancel request",
  });
  await assert.rejects(
    invoke("processWithdrawal", "creator2", {
      id,
      action: "cancel",
      reason: "retry",
    }),
  );
  const b = (await db.doc("creatorBalances/creator2").get()).data();
  assert.equal(b.availableCents, 10000);
  assert.equal(b.heldCents, 0);
});
test("ordinary member cannot moderate or grant creator role", async () => {
  await assert.rejects(
    invoke("moderateAccount", "viewer", {
      uid: "creator",
      action: "ban",
      reason: "test",
    }),
    /المسؤول/,
  );
});
test("suspension blocks gifting and withdrawing on the server", async () => {
  await invoke("moderateAccount", "admin", {
    uid: "creator",
    action: "suspend",
    untilMs: Date.now() + 3600000,
    reason: "test suspension",
  });
  await assert.rejects(
    invoke("sendGift", "viewer", {
      creatorId: "creator",
      amountCents: 100,
      requestId: "blocked-gift",
    }),
    /مؤقت/,
  );
  await assert.rejects(
    invoke("requestWithdrawal", "creator", {
      amountCents: 100,
      requestId: "blocked-w",
      method: "SHAM_CASH",
      destination: "test",
      beneficiary: "name",
    }),
    /مؤقت/,
  );
  await invoke("moderateAccount", "admin", {
    uid: "creator",
    action: "restore",
    reason: "restored",
  });
});
test("deletion is blocked when creator earnings are outstanding", async () => {
  await assert.rejects(
    invoke("moderateAccount", "admin", {
      uid: "creator",
      action: "delete",
      reason: "delete",
    }),
    /تسوية/,
  );
});
test("viewing public reels requires no identity or login", async () => {
  const r = await invoke("listReels", null);
  assert.ok(Array.isArray(r.items));
});
test("rejection returns held funds exactly once and non-owner cannot cancel", async () => {
  await invoke("requestWithdrawal", "creator2", {
    amountCents: 1000,
    requestId: "reject-me",
    method: "SHAM_CASH",
    destination: "account",
    beneficiary: "Creator",
  });
  await assert.rejects(
    invoke("processWithdrawal", "viewer", {
      id: "creator2_reject-me",
      action: "cancel",
      reason: "wrong owner",
    }),
  );
  await invoke("processWithdrawal", "admin", {
    id: "creator2_reject-me",
    action: "reject",
    reason: "invalid destination",
  });
  await assert.rejects(
    invoke("processWithdrawal", "admin", {
      id: "creator2_reject-me",
      action: "reject",
      reason: "again",
    }),
  );
  assert.equal(
    (await db.doc("creatorBalances/creator2").get()).data().availableCents,
    10000,
  );
});
test("paid status requires an attached payout receipt", async () => {
  await invoke("requestWithdrawal", "creator2", {
    amountCents: 1000,
    requestId: "pay-me",
    method: "SHAM_CASH",
    destination: "account",
    beneficiary: "Creator",
  });
  await invoke("processWithdrawal", "admin", {
    id: "creator2_pay-me",
    action: "approve",
    reason: "checked beneficiary",
  });
  await assert.rejects(
    invoke("processWithdrawal", "admin", {
      id: "creator2_pay-me",
      action: "paid",
      reason: "sent",
      reference: "transfer-1",
    }),
  );
  assert.equal(
    (await db.doc("withdrawals/creator2_pay-me").get()).data().status,
    "processing",
  );
});
test("non-creator cannot request payout and creators cannot gift themselves", async () => {
  await assert.rejects(
    invoke("requestWithdrawal", "viewer", {
      amountCents: 100,
      requestId: "not-creator",
      method: "SHAM_CASH",
      destination: "account",
      beneficiary: "name",
    }),
  );
  await assert.rejects(
    invoke("sendGift", "creator", {
      creatorId: "creator",
      amountCents: 100,
      requestId: "self",
    }),
  );
});
test("valid payout receipt completes held withdrawal exactly once", async () => {
  const { getStorage } = require("firebase-admin/storage");
  const storagePath = "payoutReceipts/admin/receipt.png";
  await getStorage()
    .bucket()
    .file(storagePath)
    .save(Buffer.from("test-image"), {
      metadata: { contentType: "image/png" },
    });
  const args = {
    id: "creator2_pay-me",
    action: "paid",
    reason: "external transfer completed",
    reference: "payout-unique-1",
    storagePath,
  };
  await invoke("processWithdrawal", "admin", args);
  await assert.rejects(invoke("processWithdrawal", "admin", args));
  const b = (await db.doc("creatorBalances/creator2").get()).data();
  assert.equal(b.heldCents, 0);
  assert.equal(b.availableCents, 9000);
  assert.equal(
    (await db.doc("withdrawals/creator2_pay-me").get()).data().receiptPath,
    storagePath,
  );
});
test("deposit credits only after approval and transfer cannot be reused", async () => {
  const { getStorage } = require("firebase-admin/storage");
  const storagePath = "depositReceipts/viewer/deposit.png";
  await getStorage()
    .bucket()
    .file(storagePath)
    .save(Buffer.from("test-image"), {
      metadata: { contentType: "image/png" },
    });
  const base = {
    amountCents: 500,
    method: "SHAM_CASH",
    reference: "deposit-unique-1",
    storagePath,
  };
  const before = (await db.doc("wallets/viewer").get()).data().balanceCents;
  await invoke("submitDeposit", "viewer", { ...base, requestId: "d1" });
  assert.equal(
    (await db.doc("wallets/viewer").get()).data().balanceCents,
    before,
  );
  await invoke("reviewDeposit", "admin", {
    id: "viewer_d1",
    action: "approve",
    reason: "transfer checked externally",
  });
  assert.equal(
    (await db.doc("wallets/viewer").get()).data().balanceCents,
    before + 500,
  );
  await assert.rejects(
    invoke("reviewDeposit", "admin", {
      id: "viewer_d1",
      action: "approve",
      reason: "retry",
    }),
  );
  await invoke("submitDeposit", "viewer", { ...base, requestId: "d2" });
  await assert.rejects(
    invoke("reviewDeposit", "admin", {
      id: "viewer_d2",
      action: "approve",
      reason: "same transfer",
    }),
    /سابقًا/,
  );
});
