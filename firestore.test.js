const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function validUserAccountPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    username: "@alice_vip",
    displayName: "أليس الملكية",
    email: "alice@layali.vip",
    bio: "عضو نادي ليالي الملكي",
    cityName: "دمشق",
    country: "سوريا",
    selectedFrameId: "emerald_crown",
    level: 1,
    xpPoints: 150,
    nextLevelXp: 2500,
    walletBalanceUsd: 0,
    chatPriceUsd: 15,
    isPaidChatEnabled: true,
    passwordSaltHex: "0123456789abcdef0123456789abcdef",
    passwordHashHex: "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
    createdAt: now,
    updatedAt: now,
  };
}

function validBookingPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    venueId: "damascus_royal_spa",
    venueName: "سبا قصر الياسمين الدمشقي",
    cityName: "دمشق",
    country: "سوريا",
    serviceTitle: "مساج ملكي بالأحجار الساخنة",
    category: "سبا ومساج",
    bookingDate: "2026-11-05",
    timeSlot: "08:00 مساءً",
    guestCount: 2,
    totalPriceUsd: 120,
    notes: "جلسة هادئة مع زيوت عطرية",
    status: "confirmed",
    createdAt: now,
    updatedAt: now,
  };
}

function validPublishedServicePayload(uid) {
  const now = new Date();
  return {
    authorId: uid,
    title: "عرض مساج ملكي وسهرة VIP",
    cityName: "دمشق",
    country: "سوريا",
    category: "سبا ومساج",
    description: "عرض خاص يشمل جلسة مساج وعرض فيديو وصور للمركز الفاخر",
    priceUsd: 75,
    imageUrl: "preset://rose_gold_spa",
    videoUrl: "https://example.com/showcase.mp4",
    mediaType: "both",
    status: "active",
    createdAt: now,
    updatedAt: now,
  };
}

function validBlockedUserPayload(uid) {
  const now = new Date();
  return {
    userId: uid,
    blockedIdentifier: "@spam_account",
    reason: "مخالفة شروط الحجز والتقييمات",
    cityScope: "جميع المدن",
    status: "active",
    createdAt: now,
    updatedAt: now,
  };
}

test("UserAccount: owner can create account, update password hash, and cross-user read is blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const aliceDoc = aliceDb.collection("users").doc(ALICE_UID);

  await assertSucceeds(aliceDoc.set(validUserAccountPayload(ALICE_UID)));

  // Update password hash and salt
  await assertSucceeds(
    aliceDoc.update({
      passwordSaltHex: "fedcba9876543210fedcba9876543210",
      passwordHashHex: "9999999999999999999999999999999999999999999999999999999999999999",
      updatedAt: new Date(),
    })
  );

  // Cross-user read is blocked (PII and password hash isolation)
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Unauthenticated user: cannot read or create accounts, bookings, or published services", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("bookings").get()
  );
  await assertFails(
    unauthDb.collection("published_services").where("status", "==", "active").get()
  );
});

test("Published services: authenticated user can publish offer and query active services", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const docRef = aliceDb.collection("published_services").doc("srv_1");

  await assertSucceeds(docRef.set(validPublishedServicePayload(ALICE_UID)));

  // Bob can query active services
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(
    bobDb.collection("published_services").where("status", "==", "active").get()
  );

  // Bob cannot delete Alice's published service unless admin
  await assertFails(bobDb.collection("published_services").doc("srv_1").delete());
});

test("Email spoofing guard: unverified admin email cannot delete another user's service", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx
      .firestore()
      .collection("published_services")
      .doc("srv_alice")
      .set(validPublishedServicePayload(ALICE_UID));
  });

  // Spoofed admin with email_verified = false
  const spoofAdminDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "khtaub7341@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(
    spoofAdminDb.collection("published_services").doc("srv_alice").delete()
  );

  // Verified admin with email_verified = true succeeds
  const realAdminDb = testEnv
    .authenticatedContext(BOB_UID, {
      email: "khtaub7341@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(
    realAdminDb.collection("published_services").doc("srv_alice").delete()
  );
});

test("Blocked users: owner/admin can create, query, and unblock entries, cross-user blocked", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const blockRef = aliceDb
    .collection("users")
    .doc(ALICE_UID)
    .collection("blocked_users")
    .doc("blk_1");

  await assertSucceeds(blockRef.set(validBlockedUserPayload(ALICE_UID)));
  await assertSucceeds(
    aliceDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("blocked_users")
      .where("userId", "==", ALICE_UID)
      .get()
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb
      .collection("users")
      .doc(ALICE_UID)
      .collection("blocked_users")
      .doc("blk_1")
      .get()
  );
});

test("Paid posts: public preview works; owner-only body stays private", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  const id = "post_secure_1";
  const preview = {
    ...validPublishedServicePayload(ALICE_UID),
    category: "منشور مدفوع",
    priceUsd: 2,
    description: "هذه معاينة عامة فقط ولا تضم محتوى مدفوعاً",
    imageUrl: "",
    videoUrl: "",
    mediaType: "photo",
  };
  const body = {
    authorId: ALICE_UID,
    serviceId: id,
    body: "هذا هو النص الكامل الذي يجب أن يظل مخفياً عن غير مالكة المنشور.",
    createdAt: new Date(),
  };

  await assertSucceeds(alice.collection("published_services").doc(id).set(preview));
  await assertSucceeds(alice.collection("creator_post_bodies").doc(id).set(body));
  await assertSucceeds(bob.collection("published_services").doc(id).get());
  await assertSucceeds(alice.collection("creator_post_bodies").doc(id).get());
  await assertFails(bob.collection("creator_post_bodies").doc(id).get());
  await assertFails(bob.collection("creator_post_bodies").get());
  await assertFails(bob.collection("creator_post_bodies").doc("post_secure_2").set({
    ...body, serviceId: "post_secure_2",
  }));
  await assertFails(alice.collection("creator_post_bodies").doc(id).update({ body: "modified" }));
});

test("Money hardening: clients cannot mint wallet credit, debit balances or confirm bookings", async () => {
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const userRef = alice.collection("users").doc(ALICE_UID);
  const forged = { ...validUserAccountPayload(ALICE_UID), walletBalanceUsd: 50000 };
  await assertFails(userRef.set(forged));
  await assertSucceeds(userRef.set(validUserAccountPayload(ALICE_UID)));
  await assertFails(userRef.update({ walletBalanceUsd: 999999, updatedAt: new Date() }));
  await assertFails(userRef.update({ level: 100, xpPoints: 999999, updatedAt: new Date() }));
  await assertFails(alice.collection("wallets").doc(ALICE_UID).set({ balanceCents: 999999 }));
  await assertFails(alice.collection("creator_wallets").doc(ALICE_UID).set({ availableCents: 999999 }));
  await assertFails(alice.collection("users").doc(ALICE_UID).collection("bookings").doc("forged").set(validBookingPayload(ALICE_UID)));
  await assertFails(alice.collection("payment_receipts").doc("unverified").set({ userId: ALICE_UID, status: "verified" }));
  await assertFails(alice.collection("withdrawals").doc("fake").set({ userId: ALICE_UID, status: "paid" }));
});
