package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.api.LayaliApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URLEncoder

private val AdminRoyalPalette = darkColorScheme(
    primary = Color(0xFFA8C8FF),
    onPrimary = Color(0xFF0A1D3B),
    secondary = Color(0xFFB7C9E5),
    background = Color(0xFF0A1730),
    surface = Color(0xFF142745),
    onSurface = Color(0xFFF2F6FF),
    surfaceVariant = Color(0xFF203855),
    onSurfaceVariant = Color(0xFFBDCCE2),
    outline = Color(0xFF5B7595),
    error = Color(0xFFFFA8A8)
)

private fun JSONObject.list(key:String="items"):List<JSONObject>{val a=optJSONArray(key)?:return emptyList();return (0 until a.length()).map{a.getJSONObject(it)}}
private val adminLabels=mapOf("users" to "المستخدمون","creators" to "صنّاع المحتوى","active_users" to "النشطون خلال الفترة","online" to "متصلون الآن","live" to "البثوث الجارية","calls" to "المكالمات الجارية","bookings" to "الحجوزات الجديدة","pending_reports" to "البلاغات المعلقة","banned" to "الحسابات المحظورة","restricted" to "الحسابات المقيدة","pending_cases" to "طلبات الدعم","security_alerts" to "تنبيهات الأمان","failed_live" to "تعثر البث","revenue_cents" to "الإيراد المستحق · سنت","commission_cents" to "العمولة · سنت","pending_withdrawals" to "طلبات السحب","ban" to "حظر الحساب","warning" to "تحذير","messages" to "الرسائل","withdrawals" to "السحب","store" to "المتجر","payments" to "المدفوعات","owner" to "المالك الرئيسي","general" to "المدير العام","finance" to "المدير المالي","users" to "إدارة المستخدمين","live" to "إدارة البث","store" to "إدارة المتجر","support" to "الدعم الفني","role" to "الرتبة")
private fun label(key:String)=adminLabels[key]?:mapOf("dashboard.view" to "عرض الإحصائيات","users.view" to "عرض الحسابات","users.edit" to "تعديل الحسابات","users.sanction" to "إدارة العقوبات","users.preview" to "معاينة الحسابات","finance.view" to "عرض الأموال","finance.adjust" to "طلب تعديل مالي","finance.approve" to "اعتماد مالي مستقل","live.view" to "عرض البث","live.moderate" to "الإشراف على البث","store.view" to "عرض الحجوزات","store.edit" to "إدارة المتجر","wheel.view" to "عرض العجلة","wheel.edit" to "إدارة العجلة","support.view" to "عرض طلبات الدعم","support.edit" to "معالجة الطلبات","roles.edit" to "إدارة الصلاحيات","audit.view" to "عرض سجل التدقيق","settings.edit" to "التحكم بالميزات","notices.send" to "إرسال إشعارات","benefits.view" to "عرض المزايا","benefits.edit" to "إدارة المزايا")[key]?:key
private data class AdminForm(val title:String,val fields:List<Pair<String,String>>,val submit:(Map<String,String>)->Unit)

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
internal fun AdminCentre(api:LayaliApi,name:String,ownerId:Int,back:()->Unit){
 val scope=rememberCoroutineScope();var auth by remember{mutableStateOf<JSONObject?>(null)};var secret by remember{mutableStateOf("")};var code by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
 var page by remember{mutableStateOf("الرئيسية")};var data by remember{mutableStateOf(JSONObject())};var detail by remember{mutableStateOf<JSONObject?>(null)};var preview by remember{mutableStateOf<JSONObject?>(null)}
 var query by remember{mutableStateOf("")};var period by remember{mutableStateOf("day")};var revision by remember{mutableIntStateOf(0)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var success by remember{mutableStateOf("")};var form by remember{mutableStateOf<AdminForm?>(null)}
 var selectedRole by remember{mutableStateOf("support")};var roleUser by remember{mutableStateOf("")};var custom by remember{mutableStateOf(setOf<String>())};var legacy by remember{mutableStateOf(false)};var navigation by remember{mutableStateOf(false)}
 fun perform(block:suspend()->Unit){if(busy)return;busy=true;error="";success="";scope.launch{try{block()}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){if(e.message=="admin_mfa_required")auth=null;error=when(e.message){"admin_mfa_required"->"أعد التحقق برمز المصادقة للمتابعة.";"independent_approval_required"->"سُجل طلب موافقة مستقلة. افتح الموافقات المالية لمراجعته وتنفيذه بعد الاعتماد.";"independent_reviewer_required"->"يجب أن يراجع الطلب مسؤول آخر لا تخصه العملية.";"invalid_totp"->"الرمز غير صحيح أو استُخدم سابقًا. انتظر الرمز التالي.";"too_many_attempts"->"تجاوزت المحاولات. انتظر 15 دقيقة.";else->"تعذر تنفيذ الطلب. تحقق من البيانات والصلاحية والاتصال."}}finally{busy=false}}}
 fun post(path:String,payload:JSONObject){perform{
    val updated=api.request(path,"POST",payload)
    if(path.startsWith("/api/admin/accounts/")&&detail!=null)detail=updated
    revision++
    success="تم حفظ الإجراء بنجاح"
}}
 fun ask(title:String,fields:List<Pair<String,String>> = emptyList(),action:(Map<String,String>)->Unit){form=AdminForm(title,fields+listOf("reason" to "سبب الإجراء"),action)}
 fun navigateBack(){
    when{
        form!=null -> form=null
        navigation -> navigation=false
        preview!=null -> preview=null
        detail!=null -> detail=null
        page!="الرئيسية" -> {page="الرئيسية";query=""}
        else -> back()
    }
 }
 val adminListState=rememberLazyListState()
 LaunchedEffect(page,detail?.optInt("id")){adminListState.scrollToItem(0)}
 LaunchedEffect(Unit){try{auth=api.request("/api/admin/auth")}catch(e:Exception){error="تعذر التحقق من صلاحية الإدارة."}}
 val context=androidx.compose.ui.platform.LocalContext.current
 DisposableEffect(Unit){val window=(context as? android.app.Activity)?.window;val existing=window?.attributes?.flags?:0;window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);onDispose{if(existing and android.view.WindowManager.LayoutParams.FLAG_SECURE==0)window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE)}}
 val verified=auth?.optBoolean("verified")==true
 val permissions=auth?.optJSONArray("permissions");fun can(s:String)=permissions!=null&&(0 until permissions.length()).any{permissions.optString(it)==s}
 val sections=listOf("الرئيسية" to "dashboard.view","الحسابات" to "users.view","الصلاحيات" to "roles.edit","المستويات وVIP" to "benefits.view","هدايا البث" to "live.view","العمليات الحالية" to "live.view","الدعم والبلاغات" to "support.view","حالات التواصل" to "support.view","العقوبات" to "users.sanction","الموافقات المالية" to (if(can("finance.approve"))"finance.approve"else"finance.adjust"),"حدود العجلة" to "wheel.view","سجل التدقيق" to "audit.view","التحكم العام" to "settings.edit","الإشعارات" to "notices.send").filter{can(it.second)}
 LaunchedEffect(verified){if(verified&&sections.none{it.first==page})page=sections.firstOrNull()?.first?:"الرئيسية"}
 LaunchedEffect(page,query,period,revision,verified){if(!verified)return@LaunchedEffect;do{try{val path=when(page){"هدايا البث"->"/api/admin/live/gifts";"الرئيسية"->"/api/admin/control/dashboard?period=$period";"الحسابات"->"/api/admin/accounts?q="+URLEncoder.encode(query,"UTF-8");"العقوبات"->"/api/admin/control/sanctions?q="+URLEncoder.encode(query,"UTF-8");"الدعم والبلاغات"->"/api/admin/control/cases";"حالات التواصل"->"/api/admin/control/communications";"الموافقات المالية"->"/api/admin/control/approvals";"الصلاحيات"->"/api/admin/control/roles";"المستويات وVIP"->"/api/admin/control/benefits";"حدود العجلة"->"/api/admin/control/wheel-limits";"سجل التدقيق"->"/api/admin/control/audit?q="+URLEncoder.encode(query,"UTF-8");"التحكم العام"->"/api/admin/control/settings";"الإشعارات"->"/api/admin/control/notices";else->"/api/admin/control/dashboard"};data=api.request(path);if(page=="الصلاحيات"&&custom.isEmpty()){val a=data.optJSONObject("roles")?.optJSONArray(selectedRole);if(a!=null)custom=(0 until a.length()).map{a.getString(it)}.toSet()}}catch(e:kotlinx.coroutines.CancellationException){throw e}catch(e:Exception){if(e.message=="admin_mfa_required")auth=null;error="تعذر تحديث البيانات."};if(page=="الرئيسية")delay(15000)}while(page=="الرئيسية"&&verified)}
 BackHandler(enabled=verified&&!legacy){navigateBack()}
 if(legacy&&verified){MaterialTheme(colorScheme=AdminRoyalPalette){MobileAdminScreen(api,name,allowed=buildList{if(can("live.view"))add("البث المباشر");if(can("store.view")){add("الحجوزات");add("المحتوى")};if(can("finance.view")){add("السحب");add("الدفع");add("الطلبات")};if(can("wheel.view"))add("العجلة")},exit={legacy=false}){legacy=false}};return}
 if(preview!=null){
    MaterialTheme(colorScheme=AdminRoyalPalette) {
        AdminAccountPreviewScreen(preview!!) { preview=null }
    }
    return
 }
 MaterialTheme(colorScheme=AdminRoyalPalette) {
 Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
 LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(),state=adminListState,contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
  item {
     Card(
         shape=RoundedCornerShape(26.dp),
         colors=CardDefaults.cardColors(containerColor=Color(0xFF1A3154)),
         modifier=Modifier.fillMaxWidth()
     ) {
         Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
             Text("RELAXIA  /  ADMIN", color=Color(0xFF9BB9E6), style=MaterialTheme.typography.labelSmall, fontWeight=FontWeight.Bold)
             Row(Modifier.fillMaxWidth(), horizontalArrangement=Arrangement.SpaceBetween, verticalAlignment=Alignment.CenterVertically) {
                 Column(Modifier.weight(1f)) {
                     Text("مركز الإدارة",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
                     Text("مرحبًا، $name",color=Color(0xFFCCD9EF),style=MaterialTheme.typography.bodyMedium)
                 }
                 TextButton(onClick=back){Text("العودة للتطبيق")}
             }
             if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
             if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
         }
     }
 }
  if(success.isNotBlank())item{Text(success,color=MaterialTheme.colorScheme.primary)}
 if(!verified){item{Text("التحقق الثنائي",style=MaterialTheme.typography.titleLarge);Text("استخدم تطبيق مصادقة. لا تشارك مفتاح الإعداد أو الرمز مع أحد.");if(auth?.optBoolean("enabled")==false&&secret.isBlank()){OutlinedTextField(password,{password=it},label={Text("كلمة المرور لتأكيد إعداد المصادقة")},visualTransformation=PasswordVisualTransformation(),modifier=Modifier.fillMaxWidth());Button({perform{secret=api.request("/api/admin/auth","POST",JSONObject().put("action","setup").put("password",password)).getString("secret");password=""}},enabled=!busy){Text("إعداد المصادقة")}};if(secret.isNotBlank()){Text("مفتاح الإعداد: $secret");TextButton({val clip=android.content.ClipData.newPlainText("Relaxia MFA",secret);if(android.os.Build.VERSION.SDK_INT>=33)clip.description.extras=android.os.PersistableBundle().apply{putBoolean(android.content.ClipDescription.EXTRA_IS_SENSITIVE,true)};(context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager).setPrimaryClip(clip)}){Text("نسخ مفتاح الإعداد")}};OutlinedTextField(code,{code=it.filter{c->c.isDigit()}.take(6)},label={Text("رمز المصادقة · 6 أرقام")},modifier=Modifier.fillMaxWidth());Button({perform{api.request("/api/admin/auth","POST",JSONObject().put("action","verify").put("code",code));code="";secret="";auth=api.request("/api/admin/auth");revision++}},enabled=code.length==6&&!busy){Text("تحقق")}}}
  else{
   item {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,verticalAlignment=Alignment.CenterVertically) {
            Text("أقسام الإدارة",fontWeight=FontWeight.SemiBold)
            TextButton({navigation=true}){Text("عرض الكل")}
        }
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            sections.forEach { (title,_) ->
                FilterChip(page==title, onClick={page=title;detail=null;query=""}, label={Text(title)})
            }
        }
        if(can("finance.view")||can("store.view")||can("wheel.view"))
            OutlinedButton({legacy=true}){Text("أدوات البث والمتجر والسحب")}
    }
}
   item{Text(page,style=MaterialTheme.typography.titleLarge);if(page in listOf("الحسابات","العقوبات","سجل التدقيق"))OutlinedTextField(query,{query=it.take(100)},label={Text("بحث")},modifier=Modifier.fillMaxWidth());TextButton({revision++},enabled=!busy){Text("تحديث")}}
   when(page){
    "الرئيسية"->{item{Row{listOf("day" to "يومي","week" to "أسبوعي","month" to "شهري").forEach{(k,v)->FilterChip(period==k,{period=k},label={Text(v)})}}};val stats=data.optJSONObject("stats")?:JSONObject();items(stats.keys().asSequence().toList().chunked(2)){pair->
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            pair.forEach { k -> Box(Modifier.weight(1f)){AdminDataCard(label(k),stats.optString(k))} }
            if(pair.size==1)Spacer(Modifier.weight(1f))
        }
    }}
    "الحسابات"->{val user=detail;if(user==null){items(data.list()){u->OutlinedCard({perform{detail=api.request("/api/admin/accounts/${u.optInt("id")}")}},modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(u.optString("display_name"));Text(u.optString("username"));Text(u.optString("email"))}}}}else{item{TextButton({detail=null}){Text("العودة للحسابات")};Text(user.optString("display_name"),style=MaterialTheme.typography.titleMedium);Text(user.optString("email"));Text("@"+user.optString("username")+" · #"+user.optString("public_id","—"),style=LocalTextStyle.current.copy(textDirection=androidx.compose.ui.text.style.TextDirection.Ltr));Text("الإنشاء: "+user.optString("created_at"));Text(if(user.optBoolean("active"))"نشط"else"محظور");if(can("users.preview"))Button({ask("معاينة الحساب"){v->perform{preview=api.request("/api/admin/control/preview/${user.optInt("id")}","POST",JSONObject().put("reason",v["reason"]))}}}){Text("معاينة الحساب")};if(can("users.edit")&&!user.optBoolean("protected"))TextButton({ask("تغيير صلاحية النشر"){v->post("/api/admin/accounts/${user.optInt("id")}",JSONObject().put("action",if(user.optBoolean("publishing_allowed"))"publishing_off"else"publishing_on").put("reason",v["reason"]))}}){Text("تفعيل / إيقاف النشر")};if(can("users.edit")&&!user.optBoolean("protected"))TextButton({ask("قرار توثيق الحساب"){v->post("/api/admin/accounts/${user.optInt("id")}",JSONObject().put("action",if(user.optBoolean("identity_verified"))"verification_off"else"verification_on").put("reason",v["reason"]))}}){Text(if(user.optBoolean("identity_verified"))"إلغاء اعتماد التوثيق"else"اعتماد التوثيق بعد المراجعة")};if(can("finance.adjust"))TextButton({ask("طلب إضافة رصيد",listOf("amount" to "المبلغ بالسنت")){v->post("/api/admin/accounts/${user.optInt("id")}",JSONObject().put("action","credit").put("amount_cents",v["amount"]?.toLongOrNull()?:0).put("request_key",java.util.UUID.randomUUID().toString()).put("reason",v["reason"]))}}){Text("إضافة رصيد بموافقة مستقلة")}}}}
    "العقوبات"->{item{Button({ask("عقوبة محددة",listOf("user" to "معرف الحساب · 5 أرقام","kind" to "warning / ban / messages / calls / live / store / withdrawals","hours" to "المدة بالساعات · صفر للدائمة")){v->post("/api/admin/control/sanctions",JSONObject().put("public_id",v["user"]).put("kind",v["kind"]).put("hours",v["hours"]?.toIntOrNull()?:-1).put("reason",v["reason"]))}}){Text("إضافة عقوبة أو تحذير")}};items(data.list()){r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(r.optString("username")+" · "+label(r.optString("kind")));Text(r.optString("reason"));Text("المسؤول: "+r.optString("actor"));Text("انتهاء: "+r.optString("expires_at","دائمة"));if(r.isNull("revoked_at"))TextButton({ask("إزالة العقوبة"){v->post("/api/admin/control/sanctions",JSONObject().put("action","revoke").put("id",r.optLong("id")).put("reason",v["reason"]))}}){Text("إزالة العقوبة")}}}}}
    "هدايا البث"->{item{Text("القيم بالسنت من الرصيد. تغييرات الأسعار لا تعدّل إيصالات الهدايا السابقة.");if(can("finance.adjust"))Button({ask("إضافة هدية",listOf("code" to "رمز فريد بحروف إنجليزية صغيرة","name" to "اسم الهدية","symbol" to "رمز الهدية","price" to "القيمة بالسنت","effect" to "simple أو animated أو depth")){v->post("/api/admin/live/gifts",JSONObject().put("code",v["code"]).put("name",v["name"]).put("symbol",v["symbol"]).put("price_cents",v["price"]?.toIntOrNull()?:0).put("effect",v["effect"]).put("enabled",true).put("reason",v["reason"]))}}){Text("إضافة هدية")}};items(data.list()){g->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(g.optString("symbol")+" "+g.optString("name"));Text("${g.optLong("price_cents")} · "+if(g.optBoolean("enabled"))"متاحة"else"معطلة");if(can("finance.adjust")){TextButton({ask("تغيير سعر الهدية",listOf("price" to "السعر الجديد بالسنت")){v->post("/api/admin/live/gifts",JSONObject(g.toString()).put("price_cents",v["price"]?.toIntOrNull()?:0).put("reason",v["reason"]))}}){Text("تعديل السعر")};TextButton({ask("تغيير إتاحة الهدية"){v->post("/api/admin/live/gifts",JSONObject(g.toString()).put("enabled",!g.optBoolean("enabled")).put("reason",v["reason"]))}}){Text(if(g.optBoolean("enabled"))"تعطيل"else"تفعيل")}}}}}}
    "الصلاحيات" -> {
        item {
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("إضافة مسؤول وتحديد صلاحياته",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                Text("استخدم معرّف الحساب العام. لا يمكن منح صلاحية المالك الرئيسي من هنا.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value=roleUser,
                    onValueChange={roleUser=it.filter { c->c.isDigit() }.take(5)},
                    label={Text("معرّف المستخدم · 5 أرقام")},
                    modifier=Modifier.fillMaxWidth(),
                    singleLine=true
                )
                Text("نوع المسؤول",fontWeight=FontWeight.SemiBold)
                val roles=data.optJSONObject("roles")?:JSONObject()
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(6.dp)) {
                    listOf("support","users","live","store","finance","general").forEach { k ->
                        if(roles.has(k))FilterChip(selectedRole==k,onClick={
                            selectedRole=k
                            val a=roles.optJSONArray(k)
                            custom=if(a==null)emptySet() else (0 until a.length()).map { a.getString(it) }.toSet()
                        },label={Text(label(k))})
                    }
                }
                Text("الصلاحيات التفصيلية",fontWeight=FontWeight.SemiBold)
                val ps=data.optJSONArray("permissions")
                if(ps!=null)for(i in 0 until ps.length()){
                    val p=ps.getString(i)
                    Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically) {
                        Checkbox(p in custom,onCheckedChange={custom=if(it) custom+p else custom-p})
                        Text(label(p),style=MaterialTheme.typography.bodyMedium)
                    }
                }
                Button(
                    onClick={ask("تأكيد تعيين المسؤول"){v->
                        post("/api/admin/control/roles",JSONObject()
                            .put("public_id",roleUser)
                            .put("role",selectedRole)
                            .put("permissions",org.json.JSONArray(custom.toList()))
                            .put("reason",v["reason"]))
                    }},
                    modifier=Modifier.fillMaxWidth(),
                    enabled=roleUser.length==5&&!busy
                ){Text("حفظ المسؤول وصلاحياته")}
                Text("يُلغى تسجيل دخول المسؤول عند تغيير صلاحياته، ويجب أن يعيد الدخول والتحقق الثنائي.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(data.list()) { r ->
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)) {
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
                    Text(r.optString("username"),fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                    Text("#"+r.optString("public_id")+"  •  "+label(r.optString("role")),color=MaterialTheme.colorScheme.onSurfaceVariant)
                    val perms=r.optJSONArray("permissions")
                    Text("الصلاحيات المفعّلة: ${perms?.length()?:0}",style=MaterialTheme.typography.bodySmall)
                    if(r.optString("role")!="owner")TextButton({ask("سحب جميع صلاحيات المسؤول"){v->
                        post("/api/admin/control/roles",JSONObject()
                            .put("action","revoke")
                            .put("user_id",r.optInt("id"))
                            .put("reason",v["reason"]))
                    }}){Text("سحب الصلاحيات وإبطال الجلسات")}
                }
            }
        }
    }
    "الموافقات المالية"->{items(data.list()){r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){Text(r.optString("path"));Text(r.optString("reason"));Text("الحالة: "+r.optString("status"));Text("صاحب الطلب: ${r.optString("requester_public_id","—")} · الحساب: ${r.optString("target_user_public_id","—")}");Text(r.optJSONObject("payload")?.toString(2)?:"");if(r.optString("status")=="pending"&&r.optInt("requester_id")!=ownerId&&r.optInt("target_user_id")!=ownerId&&can("finance.approve"))listOf("approve" to "موافقة مستقلة","reject" to "رفض").forEach{(action,title)->TextButton({ask(title){v->post("/api/admin/control/approvals",JSONObject().put("id",r.getString("id")).put("action",action).put("reason",v["reason"]))}}){Text(title)}};if(r.optString("status")=="approved"&&r.isNull("used_at")&&r.optInt("requester_id")==ownerId)TextButton({ask("تنفيذ العملية المعتمدة"){_->post(r.getString("path"),JSONObject(r.getJSONObject("payload").toString()).put("approval_id",r.getString("id")))}}){Text("تنفيذ مرة واحدة")}}}}}
    "المستويات وVIP" -> {
        item {
            Column(verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text("رحلة المستويات",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(4.dp)){
                    listOf(0xFF32A871,0xFF3589D3,0xFF8C69CB,0xFFD3A248,0xFFB8C8D8,0xFF9F557B).forEach { color ->
                        Box(Modifier.weight(1f).height(12.dp).background(Color(color),RoundedCornerShape(5.dp)))
                    }
                }
                Text("أخضر 1–10  •  أزرق 11–25  •  بنفسجي 26–40  •  ذهبي 41–60  •  ماسي 61–80  •  ملكي 81–100",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text("VIP من 1 إلى 5، مستقل عن المستوى ونقاط الخبرة. تنتهي المكافأة بانتهاء المدة دون المساس بالتقدم المكتسب.",color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(can("benefits.edit"))Button(
                    onClick={ask("منح مكافأة تحفيزية",listOf(
                        "user" to "معرّف المستخدم · 5 أرقام",
                        "kind" to "نوع المكافأة",
                        "value" to "المستوى 1–100 / VIP 1–5 / الإطار 110–200 / نقاط",
                        "days" to "المدة بالأيام 1–365"
                    )) { v->
                        post("/api/admin/control/benefits",JSONObject()
                            .put("public_id",v["user"])
                            .put("kind",v["kind"])
                            .put("value",v["value"]?.toIntOrNull()?:-1)
                            .put("days",v["days"]?.toIntOrNull()?:30)
                            .put("request_key",java.util.UUID.randomUUID().toString())
                            .put("reason",v["reason"]))
                    }},
                    modifier=Modifier.fillMaxWidth()
                ){Text("منح مستوى أو VIP أو إطار")}
                Text("تُسجّل كل مكافأة باسم المسؤول. النقاط الترويجية ليست أموالًا ولا يمكن سحبها.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        items(data.list()) { r ->
            Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(18.dp)){
                Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
                    val caption=when(r.optString("kind")){
                        "level"->"مستوى ${r.optInt("value")}"
                        "vip"->"VIP ${r.optInt("value")}"
                        "frame"->"إطار ${r.optInt("value")}"
                        else->"ميزة ${r.optString("kind")}"
                    }
                    Text(caption,fontWeight=FontWeight.Bold,style=MaterialTheme.typography.titleMedium)
                    Text("الحساب #"+r.optString("public_id")+"  •  حتى "+r.optString("expires_at"),color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.bodySmall)
                    Text(r.optString("reason"),style=MaterialTheme.typography.bodySmall)
                    if(can("benefits.edit"))TextButton({ask("سحب المكافأة"){v->
                        post("/api/admin/control/benefits",JSONObject().put("action","revoke").put("id",r.optInt("id")).put("reason",v["reason"]))
                    }}){Text("إنهاء المكافأة")}
                }
            }
        }
        items(data.list("promotions")) { r ->
            AdminDataCard("نقاط ترويجية • #"+r.optString("public_id"),r.optInt("points").toString()+" • "+r.optString("reason"))
        }
    }
    "حدود العجلة"->{item{Text("لفات اليوم: ${data.optInt("spins")} · الإيراد: ${data.optLong("cost_cents")} · الجوائز: ${data.optLong("prizes_cents")} سنت");Text("حد المستخدم اليومي: ${data.optInt("daily_limit")} · ميزانية الجوائز اليومية: ${data.optLong("daily_budget_cents")} سنت");if(can("wheel.edit"))Button({ask("تعديل حدود العجلة",listOf("limit" to "أقصى لفات يومية لكل مستخدم","budget" to "سقف الجوائز اليومي بالسنت")){v->post("/api/admin/control/wheel-limits",JSONObject().put("daily_limit",v["limit"]?.toIntOrNull()?:-1).put("daily_budget_cents",v["budget"]?.toIntOrNull()?:-1).put("reason",v["reason"]))}}){Text("ضبط الحدود")}};items(data.list()){r->AdminDataCard("الحساب ${r.optString("public_id","—")}","تكلفة ${r.optInt("cost_cents")} · جائزة ${r.optInt("prize_cents")} · "+r.optString("created_at"))}}
    "سجل التدقيق"->items(data.list()){r->AdminDataCard(r.optString("action")+" · "+r.optString("result"),r.optString("actor")+" · "+r.optString("created_at")+"\n"+r.optString("reason")+"\n"+r.optString("target"))}
    "التحكم العام"->items(data.list()){r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(label(r.getString("key")));Text(if(r.optBoolean("enabled"))"مفعلة"else"متوقفة");TextButton({ask("تغيير حالة الميزة"){v->post("/api/admin/control/settings",JSONObject().put("key",r.getString("key")).put("enabled",!r.optBoolean("enabled")).put("reason",v["reason"]))}}){Text(if(r.optBoolean("enabled"))"إيقاف للطوارئ"else"تفعيل")}}}}
    "الإشعارات"->{item{Button({ask("إشعار داخل التطبيق",listOf("user" to "معرف المستلم · اتركه فارغًا للجميع","title" to "العنوان","body" to "النص")){v->post("/api/admin/control/notices",JSONObject().put("public_id",v["user"]?.ifBlank{null}).put("title",v["title"]).put("body",v["body"]).put("reason",v["reason"]))}}){Text("إنشاء إشعار")}};items(data.list()){r->AdminDataCard(r.optString("title"),r.optString("body"))}}
    "حالات التواصل"->{item{val states=data.optJSONObject("message_states")?:JSONObject();Text("بانتظار التسليم: ${states.optInt("pending_delivery")} · مستلمة: ${states.optInt("delivered")} · مقروءة: ${states.optInt("read")}");Text("لا يُعرض محتوى الرسائل أو إشارات المكالمات.")};items(data.list()){r->AdminDataCard("مكالمة ${r.optString("mode")} · ${r.optString("status")}","${r.optString("caller_public_id","—")} ← ${r.optString("callee_public_id","—")} · "+r.optString("created_at"))}}
    "الدعم والبلاغات"->{items(data.list()){r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text(r.optString("body"));Text(r.optString("response"));if(r.optString("status")=="pending"&&can("support.edit"))TextButton({ask("الرد على الاعتراض",listOf("response" to "الرد")){v->post("/api/admin/control/cases",JSONObject().put("id",r.optInt("id")).put("response",v["response"]).put("reason",v["reason"]))}}){Text("رد وإغلاق")}}}};items(data.list("reports")){r->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp)){Text("بلاغ ${r.optInt("id")}");Text(r.optString("reason"));if(can("support.edit"))TextButton({ask("اعتماد مراجعة البلاغ"){v->post("/api/admin/control/cases",JSONObject().put("action","resolve_report").put("id",r.optInt("id")).put("reason",v["reason"]))}}){Text("تمت المراجعة")}}}}}
    "العمليات الحالية"->item{Button({legacy=true}){Text("فتح أدوات البث")}}
   }
  }
 }
 if(navigation)AlertDialog(onDismissRequest={navigation=false},title={Text("أقسام الإدارة")},text={Column(Modifier.verticalScroll(rememberScrollState())){sections.forEach{(title,_)->TextButton({page=title;detail=null;query="";navigation=false},modifier=Modifier.fillMaxWidth()){Text(title)}}}},confirmButton={TextButton({navigation=false}){Text("إغلاق")}})
 form?.let{f->var values by remember(f){mutableStateOf(f.fields.associate{it.first to ""})};AlertDialog(onDismissRequest={form=null},title={Text(f.title)},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(8.dp)){f.fields.forEach{(key,title)->if(key=="kind"){Text("نوع الإجراء");val choices=if(f.title.contains("عقوبة"))listOf("warning" to "تحذير","ban" to "حظر الحساب","messages" to "تقييد الرسائل","calls" to "تقييد المكالمات","live" to "تقييد البث","store" to "تقييد المتجر","withdrawals" to "تعليق السحب")else listOf("level" to "منح مستوى","vip" to "عضوية VIP","frame" to "إطار مميز","promotion" to "نقاط تحفيزية");choices.forEach{(value,caption)->FilterChip(values[key]==value,{values=values+(key to value)},label={Text(caption)})}}else OutlinedTextField(values[key]?:"",{values=values+(key to it.take(1000))},label={Text(title)},modifier=Modifier.fillMaxWidth())};Text("سيُسجل الإجراء باسم حسابك.")}},confirmButton={TextButton({form=null;f.submit(values)},enabled=(values["reason"]?.trim()?.length?:0)>=3&&!busy){Text("تأكيد")}},dismissButton={TextButton({form=null}){Text("إلغاء")}})}
 }
 }
}
@Composable private fun AdminDataCard(title:String,body:String) {
    Card(
        modifier=Modifier.fillMaxWidth(),
        shape=RoundedCornerShape(18.dp),
        colors=CardDefaults.cardColors(containerColor=MaterialTheme.colorScheme.surface)
    ) {
        Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(7.dp)) {
            Text(title,color=MaterialTheme.colorScheme.onSurfaceVariant,style=MaterialTheme.typography.labelMedium)
            Text(body,fontWeight=FontWeight.SemiBold,style=MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable internal fun AccountNotices(api:LayaliApi,back:()->Unit){
 val scope=rememberCoroutineScope();var data by remember{mutableStateOf(JSONObject())};var cases by remember{mutableStateOf(JSONObject())};var body by remember{mutableStateOf("")};var error by remember{mutableStateOf("")};var revision by remember{mutableIntStateOf(0)};var busy by remember{mutableStateOf(false)}
 LaunchedEffect(revision){try{data=api.request("/api/notices");cases=api.request("/api/my-cases")}catch(e:Exception){error="تعذر تحميل التنبيهات."}}
 Surface(Modifier.fillMaxSize()){LazyColumn(Modifier.fillMaxSize().safeDrawingPadding(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
 item{TextButton(back){Text("رجوع")};Text("الإشعارات والاعتراضات",style=MaterialTheme.typography.titleLarge);if(error.isNotBlank())Text(error);OutlinedTextField(body,{body=it.take(2000)},label={Text("اعتراض أو طلب دعم")},modifier=Modifier.fillMaxWidth());Button({busy=true;scope.launch{try{api.request("/api/my-cases","POST",JSONObject().put("body",body));body="";revision++}catch(e:Exception){error="تعذر إرسال الطلب."}finally{busy=false}}},enabled=body.trim().length>=3&&!busy){Text("إرسال الطلب")}}
 items(data.list()){r->AdminDataCard(r.optString("title"),r.optString("body"))};items(data.list("sanctions")){r->AdminDataCard(label(r.optString("kind")),r.optString("reason"))};items(cases.list()){r->AdminDataCard(r.optString("body"),r.optString("response").ifBlank{"قيد المراجعة"})}
 }}
}
