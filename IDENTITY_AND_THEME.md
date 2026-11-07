# Relaxia 10.5 — الحساب والهوية البصرية

## الحساب

- التسجيل الجديد: اسم ظاهر، بريد إلكتروني، كلمة مرور؛ يبقى تأكيد 18+ وضوابط المحتوى موجودًا دون طلب تاريخ ميلاد أو هوية.
- الدخول بالبريد وكلمة المرور فقط، بما في ذلك حسابات المسؤولين. لا تُلغى حماية المصادقة الثنائية للإدارة.
- الاسم الظاهر مستقل عن اسم المستخدم. يُولّد للحساب الجديد اسم مستخدم تلقائي فريد، ويمكن تغييره من تعديل الملف إلى اسم يبدأ بحرف إنجليزي ويحتوي 3–30 خانة من الأحرف والأرقام والنقطة والشرطة السفلية.
- `public_id` رقم عرض دائم وفريد بين 10000 و99999، محفوظ كسلسلة من خمسة أرقام ومحمي بقيد فريد وقيد صيغة في قاعدة البيانات. السعة القصوى لهذه الصيغة 90000 حساب. معرّف قاعدة البيانات الداخلي لم يتغير، وتستمر روابط الرسائل والحجوزات والأرصدة والجلسات باستخدامه.
- البحث العام والإداري يدعمان الاسم واسم المستخدم ورقم العرض. إدخال رقم العرض في أدوات الإدارة لا يُفسّر كمعرف داخلي لحساب آخر.
- يُثبّت الترحيل الاسم الظاهر القديم الذي كان يعتمد ضمنيًا على اسم المستخدم، حتى لا يتغير عند اختيار اسم مستخدم جديد.
- الترحيل 0022 يضيف رقم العرض للحسابات الحالية دون تعديل كلمات المرور أو البريد أو الأموال أو العلاقات؛ 0023 يفرض قيد الأرقام الخمسة.
- التسجيل الجديد وتغيير البريد إلى قيمة جديدة يمنعان تكرار البريد دون حساسية لحالة الحروف. الحسابات القديمة التي تشترك ببريد واحد لا تُدمج ولا تُحذف: الدخول الغامض يُرفض بدل اختيار حساب عشوائي. يمكن لصاحب جلسة قائمة تصحيح بريده إلى بريد فريد؛ يحتاج المستخدم الذي لا يملك جلسة إلى معالجة عبر الدعم بعد التحقق من ملكية الحساب. تُحفظ الأسماء القديمة كما هي، ولا يُفرض تغييرها لمجرد تعديل النبذة.

## الألوان

نهاري: #277C9D أساسي، #153E53 كحلي، #F4F8FA خلفية، #FFFFFF بطاقات، #D7AD72 ذهبي، #202D36 نص، #788792 ثانوي.

ليلي: #101820 خلفية، #1C2933 بطاقات، #3187A8 أساسي، #F4F7F9 نص، #A3B1BA ثانوي، #D7AD72 ذهبي.

تُطبق الألوان على الثيم المشترك، الواجهات الرئيسية، الحساب والتسجيل، الإدارة، المحادثات، المتجر والمحفظة والعجلة والإطارات. تتلقى صفحات WebRTC للمكالمات والبث الوضع المختار داخل التطبيق. الصور والشعار المرفوع ومحتوى المستخدمين لا يعاد تلوينها آليًا.

## التحديث

```bash
cd /opt/layali
git pull --ff-only origin feat/layali-adult-non-explicit
bash backend/deploy.sh
```

يجب استخدام التطبيق 10.5 مع تحديث الخادم لأن تسجيل الدخول أصبح بالبريد فقط. لا حاجة لإنشاء حساب مالك جديد أو تغيير كلمات المرور. لم يُنفذ نشر على الخادم بواسطة الوكيل، ولا تُعد المدفوعات الإنتاجية مختبرة من خلال اختبارات البرمجيات وحدها.

## التحقق

اختبارات الخلفية تغطي الفصل بين الاسم واسم المستخدم، البريد المكرر، حفظ الحسابات القديمة، عدم قابلية تغيير رقم العرض، التصادم والتفرد، البحث والخصوصية، والتسجيل المتزامن على PostgreSQL. اختبار ترحيل فعلي يرجع المخطط إلى 0021 ثم يعيد تطبيق ترحيل الهوية ويتحقق من بقاء المعرفات الداخلية وكلمات المرور والرسائل والأرصدة. تستمر اختبارات المدفوعات والمصادقة الثنائية والمكالمات والبث الحالية.

## 10.6 — registration gender and universal access

Registration now requires `gender: male|female` alongside `display_name`, email,
and password (existing adult/policy confirmation remains). Email/password login
is unchanged. Migration 0024 leaves existing genders NULL; accounts may choose
one in profile settings. Internal IDs, public numbers, passwords, balances and
existing relationships remain intact. Gender is shown on profiles and native
administration, including read-only preview. A database check rejects other values.

Publishing/upload/service/paid-media access uses `publishing_allowed` (default
true for existing and new accounts), independently of the historical `creator`
approval badge. Live hosting uses `broadcast_allowed`, not gender or that badge.
The people directory and following include regular accounts and respect blocks
and active bans. Review of published content, age confirmation, sanctions,
feature switches and financial authorization remain enforced. Native admins may
revoke/restore publishing independently; older creator_on/off actions also update
the permission for compatibility. No automatic verification badges are granted.

Install Android 10.6 (versionCode 22) and deploy the matching backend/migration.
No production deployment or real-money operation is implied by repository tests.
