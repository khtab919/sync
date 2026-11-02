package com.example.platform

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.UUID

@Composable
internal fun AdminPanel(vm:PlatformViewModel,state:PlatformState){
    var section by rememberSaveable{mutableStateOf("withdrawals")};var selected by remember{mutableStateOf<Record?>(null)};var settings by remember{mutableStateOf(false)};var service by remember{mutableStateOf<Record?>(null)}
    val labels=linkedMapOf("withdrawals" to "السحوبات","deposits" to "الإيداعات","accounts" to "الحسابات","bookings" to "الحجوزات","services" to "العروض")
    LazyColumn(Modifier.fillMaxSize(),contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        item {var expanded by remember{mutableStateOf(false)};Box {OutlinedButton(onClick={expanded=true},modifier=Modifier.fillMaxWidth()){Text(labels[section]?:"")};DropdownMenu(expanded=expanded,onDismissRequest={expanded=false}){labels.forEach{(key,label)->DropdownMenuItem(text={Text(label)},onClick={section=key;expanded=false})}}}
        item {TextButton(onClick={settings=true}){Text("إعداد وجهات الإيداع")}}
        if(section=="services") {
            item {Primary("إضافة عرض"){service=emptyMap()}}
            items(state.services,key={it.str("id")}) {r->Panel {Text(r.str("title"));Text(usd(r.num("priceUsd")*100));OutlinedButton(onClick={service=r}){Text("تعديل العرض")}}}
        } else {
            if(state.admin.rows(section).isEmpty())item{Text("لا توجد سجلات")}
            items(state.admin.rows(section),key={it.str("id")}) {r->Panel{
                Text(if(section=="accounts")r.str("displayName") else if(section=="bookings")r.str("title") else usd(r.num("amountCents")),style=MaterialTheme.typography.titleMedium)
                Text("${statusLabel(r.str("status"))} • ${r.str("id")}")
                if(section=="accounts")Text("${r.str("email")}\n${if(r.str("role")=="creator")"مذيعة" else "عضو"}")
                if(section=="withdrawals")Text("${r.str("beneficiary")}\n${r.str("method")} • ${r.str("destination")}")
                if(section=="deposits"){Text("${r.str("method")} • ${r.str("reference")}");ReceiptButton(vm,"deposits",r.str("id"))}
                if(section=="withdrawals"&&r.str("status")=="paid")ReceiptButton(vm,"withdrawals",r.str("id"))
                if(section=="bookings")Text("${r.str("date")} • ${r.num("guests")} أشخاص")
                if(section=="accounts"&&r.str("status")!="deleted"||section!="accounts"&&r.str("status") in listOf("pending","processing"))OutlinedButton(onClick={selected=r},enabled=!state.busy){Text("إدارة")}
            }}
        }
    }
    selected?.let{r->AdminActionDialog(vm,state,section,r){selected=null}}
    if(settings)SettingsDialog(vm,state){settings=false}
    service?.let{r->ServiceDialog(vm,state,r){service=null}}
}
@Composable
private fun AdminActionDialog(vm:PlatformViewModel,state:PlatformState,section:String,r:Record,close:()->Unit){
    val options=when(section){
        "accounts"->linkedMapOf("warn" to "إنذار","suspend" to "حظر مؤقت","ban" to "حظر دائم","restore" to "رفع الحظر","creator" to "تفعيل كمذيعة","member" to "إلغاء دور المذيعة","delete" to "حذف الحساب")
        "withdrawals"->if(r.str("status")=="pending")linkedMapOf("approve" to "بدء التحويل","reject" to "رفض وإعادة المبلغ")else linkedMapOf("paid" to "تأكيد التحويل وإرفاق الإيصال")
        "bookings"->linkedMapOf("confirmed" to "تأكيد التوافر","rejected" to "رفض الحجز")
        else->linkedMapOf("approve" to "اعتماد التحويل","reject" to "رفض الإيصال")
    }
    var action by rememberSaveable{mutableStateOf(options.keys.first())};var reason by rememberSaveable{mutableStateOf("")};var hours by rememberSaveable{mutableStateOf("24")};var reference by rememberSaveable{mutableStateOf("")};var confirmation by remember{mutableStateOf("")};var uri by remember{mutableStateOf<Uri?>(null)}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri=it}
    AlertDialog(onDismissRequest={if(!state.busy)close()},title={Text("إجراء إداري")},text={Column(Modifier.verticalScroll(rememberScrollState()),verticalArrangement=Arrangement.spacedBy(12.dp)){
        Text("السجل: ${r.str("id")}")
        options.forEach{(key,label)->Row {RadioButton(selected=action==key,onClick={action=key});Text(label,Modifier.padding(top=12.dp))}}
        if(section=="withdrawals")Text("المبلغ ${usd(r.num("amountCents"))}\nالمستفيدة ${r.str("beneficiary")}\n${r.str("method")}\n${r.str("destination")}")
        Field(reason,{reason=it},"السبب / ملاحظة تظهر لصاحب الحساب")
        if(action=="suspend")Field(hours,{hours=it},"مدة الحظر بالساعات")
        if(action=="paid"){Field(reference,{reference=it},"مرجع التحويل الفعلي");OutlinedButton(onClick={picker.launch("image/*")},enabled=!state.busy){Text(if(uri==null)"إرفاق إيصال التحويل" else "تم اختيار الإيصال")};Text("نفّذ التحويل فعليًا ثم سجّل الإيصال هنا. هذا الزر لا يحوّل الأموال تلقائيًا.")}
        if(action=="delete"){Text("الحذف نهائي ويمنع إذا بقي رصيد أو طلب سحب. اكتب حذف للتأكيد.");Field(confirmation,{confirmation=it},"تأكيد الحذف")}
    }},confirmButton={TextButton(enabled=!state.busy&&reason.isNotBlank()&&(action!="paid"||(uri!=null&&reference.isNotBlank()))&&(action!="delete"||confirmation=="حذف"),onClick={
        when(section){
            "accounts"->{val n=hours.toLongOrNull();if(action=="suspend"&&(n==null||n !in 1..8760))vm.fail("مدة الحظر من ساعة إلى سنة")else vm.perform("moderateAccount",mapOf("uid" to r.str("id"),"action" to action,"reason" to reason,"untilMs" to (System.currentTimeMillis()+(n?:24)*3600000)),"تم تنفيذ الإجراء وتسجيله",after=close)}
            "withdrawals"->vm.perform("processWithdrawal",mapOf("id" to r.str("id"),"action" to action,"reason" to reason,"reference" to reference),"تم تحديث طلب السحب",if(action=="paid")uri else null,if(action=="paid")"payoutReceipts" else null,close)
            "bookings"->vm.perform("reviewBooking",mapOf("id" to r.str("id"),"status" to action,"reason" to reason),"تم تحديث الحجز",after=close)
            else->vm.perform("reviewDeposit",mapOf("id" to r.str("id"),"action" to action,"reason" to reason),"تمت مراجعة الإيصال",after=close)
        }
    }){Text("تنفيذ الإجراء")}},dismissButton={TextButton(onClick=close,enabled=!state.busy){Text("إلغاء")}})
}
@Composable
private fun SettingsDialog(vm:PlatformViewModel,state:PlatformState,close:()->Unit){
    var sham by rememberSaveable{mutableStateOf(state.settings.str("shamCash"))};var usdt by rememberSaveable{mutableStateOf(state.settings.str("usdt"))}
    AlertDialog(onDismissRequest=close,title={Text("وجهات الإيداع")},text={Column{Field(sham,{sham=it},"حساب شام كاش");Field(usdt,{usdt=it},"عنوان USDT BEP-20")}},confirmButton={TextButton(enabled=!state.busy,onClick={vm.perform("savePaymentSettings",mapOf("shamCash" to sham,"usdt" to usdt),"حُفظت وجهات الإيداع",after=close)}){Text("حفظ")}},dismissButton={TextButton(onClick=close){Text("إلغاء")}})
}
@Composable
private fun ServiceDialog(vm:PlatformViewModel,state:PlatformState,r:Record,close:()->Unit){
    val id=rememberSaveable{r.str("id").ifBlank{UUID.randomUUID().toString()}};var title by rememberSaveable{mutableStateOf(r.str("title"))};var city by rememberSaveable{mutableStateOf(r.str("cityName"))};var description by rememberSaveable{mutableStateOf(r.str("description"))};var price by rememberSaveable{mutableStateOf(r.num("priceUsd").toString())};var active by rememberSaveable{mutableStateOf(true)}
    AlertDialog(onDismissRequest=close,title={Text("العرض")},text={Column(Modifier.verticalScroll(rememberScrollState())){Field(title,{title=it},"العنوان");Field(city,{city=it},"المدينة");Field(description,{description=it},"تفاصيل العرض");Field(price,{price=it},"السعر بالدولار للشخص");Row{Checkbox(checked=active,onCheckedChange={active=it});Text("منشور",Modifier.padding(top=12.dp))}}},confirmButton={TextButton(enabled=!state.busy,onClick={vm.perform("saveService",mapOf("id" to id,"title" to title,"cityName" to city,"description" to description,"priceUsd" to (price.toLongOrNull()?:0),"status" to if(active)"active" else "inactive"),"تم حفظ العرض",after=close)}){Text("حفظ")}},dismissButton={TextButton(onClick=close){Text("إلغاء")}})
}
