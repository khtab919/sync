"use strict";
class DomainError extends Error {
  constructor(code, message) {
    super(message);
    this.code = code;
  }
}
function check(ok, code, message) {
  if (!ok) throw new DomainError(code, message);
}
function money(v) {
  check(
    Number.isSafeInteger(v) && v > 0 && v <= 100000000,
    "invalid-argument",
    "المبلغ غير صالح",
  );
  return v;
}
function splitRevenue(v) {
  money(v);
  const creatorCents = Math.floor((v * 80) / 100);
  return { grossCents: v, creatorCents, platformCents: v - creatorCents };
}
function active(a, now = Date.now()) {
  check(a && a.status !== "deleted", "permission-denied", "الحساب غير متاح");
  check(a.status !== "banned", "permission-denied", "الحساب محظور");
  check(
    !(a.status === "suspended" && a.suspendedUntilMs > now),
    "permission-denied",
    "الحساب محظور مؤقتًا",
  );
  check(["active", "suspended"].includes(a.status), "permission-denied", "حالة الحساب غير صالحة");
  return a;
}
function transition(from, action) {
  const p = {
    approve: ["pending", "processing"],
    reject: ["pending", "rejected"],
    cancel: ["pending", "cancelled"],
    paid: ["processing", "paid"],
  }[action];
  check(
    p && p[0] === from,
    "failed-precondition",
    "تغيرت حالة الطلب؛ حدّث الصفحة",
  );
  return p[1];
}
function text(v, max = 200, min = 1) {
  check(
    typeof v === "string" && v.trim().length >= min && v.trim().length <= max,
    "invalid-argument",
    "يرجى استكمال الحقول بشكل صحيح",
  );
  return v.trim();
}
function key(v) {
  const s = text(v, 128);
  check(/^[a-zA-Z0-9_-]+$/.test(s), "invalid-argument", "معرف غير صالح");
  return s;
}
module.exports = {
  DomainError,
  check,
  money,
  splitRevenue,
  active,
  transition,
  text,
  key,
};
