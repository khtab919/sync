package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.data.api.LayaliApi
import kotlinx.coroutines.launch
import org.json.JSONObject

@Composable internal fun AccountSecurityDialog(api:LayaliApi,recovery:Boolean,close:()->Unit,sessionEnded:()->Unit={}) {
    val scope=rememberCoroutineScope()
    var email by remember{mutableStateOf("")};var code by remember{mutableStateOf("")}
    var current by remember{mutableStateOf("")};var password by remember{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)};var notice by remember{mutableStateOf("")}
    var confirmDelete by remember{mutableStateOf(false)}
    fun act(block:suspend()->Unit){if(busy)return;busy=true;scope.launch{try{block()}catch(e:Exception){notice=when(e.message){"email_not_configured"->"خدمة البريد غير مفعلة على الخادم بعد.";"invalid_reset_code"->"رمز غير صالح أو انتهت مدته.";"account_has_balance"->"اسحب الرصيد وأكمل العمليات المالية أولًا.";"account_has_bookings"->"أكمل الحجوزات المفتوحة أولًا.";"invalid_credentials"->"كلمة المرور الحالية غير صحيحة.";else->"تعذّر إكمال العملية. تحقق من البيانات واتصالك."}}finally{busy=false}}}
    AlertDialog(onDismissRequest={if(!busy)close()},title={Text(if(recovery)"استعادة الحساب" else "أمان الحساب")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){
        if(recovery){
            OutlinedTextField(email,{email=it},label={Text("البريد الإلكتروني")},singleLine=true)
            TextButton(enabled=!busy,onClick={act{api.request("/api/account/reset/request","POST",JSONObject().put("email",email));notice="إذا كان البريد مسجلاً فستصلك رسالة الاستعادة."}}){Text("إرسال رمز الاستعادة")}
            OutlinedTextField(code,{code=it},label={Text("رمز الاستعادة")})
        }else OutlinedTextField(current,{current=it},label={Text("كلمة المرور الحالية")},visualTransformation=PasswordVisualTransformation())
        OutlinedTextField(password,{password=it},label={Text("كلمة المرور الجديدة")},visualTransformation=PasswordVisualTransformation())
        if(!recovery){
            Text("حذف الحساب يخفي ملفك وينهي جلساتك. تُحتفظ سجلات العمليات اللازمة للمراجعة المالية.",style=MaterialTheme.typography.bodySmall)
            TextButton(enabled=!busy,onClick={confirmDelete=!confirmDelete}){Text("حذف الحساب")}
            if(confirmDelete)Button(enabled=!busy,onClick={act{api.request("/api/account/security","POST",JSONObject().put("action","delete").put("current_password",current));api.forgetSession();sessionEnded();close()}}){Text("تأكيد حذف حسابي")}
        }
        if(notice.isNotBlank())Text(notice)
        if(busy)LuxuryLoadingStatus("جارٍ التحميل")
    }},confirmButton={TextButton(enabled=!busy&&password.length>=8,onClick={act{
        if(recovery)api.request("/api/account/reset/confirm","POST",JSONObject().put("code",code.trim()).put("password",password))
        else api.request("/api/account/security","POST",JSONObject().put("action","password").put("current_password",current).put("password",password))
        api.forgetSession();sessionEnded();close()
    }}){Text("حفظ كلمة المرور")}},dismissButton={TextButton(enabled=!busy,onClick=close){Text("رجوع")}})
}
