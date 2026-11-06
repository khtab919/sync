package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.api.LayaliApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import java.util.UUID

private fun JSONObject.platformItems():List<JSONObject>{val a=optJSONArray("items")?:return emptyList();return(0 until a.length()).map{a.getJSONObject(it)}}
internal fun operationLabel(status:String)=when(status){"held","paid_reserved"->"المبلغ محجوز";"accepted"->"قُبل الطلب · بانتظار تنفيذ الخدمة";"settled","completed"->"مكتمل";"refunded"->"أُعيد المبلغ";"cancelled"->"ملغى";"awaiting_payment"->"بانتظار الدفع";"paid"->"تم السحب";"pending"->"قيد المراجعة";"approved"->"معتمد";"rejected"->"مرفوض";else->status}

@Composable internal fun ServiceBookingButton(api:LayaliApi,service:JSONObject,done:()->Unit){
    val scope=rememberCoroutineScope();var confirm by remember{mutableStateOf(false)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};val key=remember(service.optInt("id")){UUID.randomUUID().toString()}
    Column{Button({confirm=true},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("حجز الخدمة")};if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)}
    if(confirm)AlertDialog(onDismissRequest={if(!busy)confirm=false},title={Text("تأكيد الحجز")},text={Column(verticalArrangement=Arrangement.spacedBy(10.dp)){Text(service.optString("title"));Text("السعر: ${usd(service.optLong("price_cents"))}");Text("المدة: ${service.optInt("duration_minutes",30)} دقيقة");Text(service.optString("terms").ifBlank{service.optString("description")});Text("يُحجز المبلغ حتى تؤكد استلام الخدمة. الإلغاء قبل قبول البائع يعيد المبلغ كاملًا. تحصل الجهة المقدمة على 80٪ بعد الإتمام.",style=MaterialTheme.typography.bodySmall)}},confirmButton={TextButton({if(!busy){busy=true;scope.launch{try{if(api.request("/health/").optInt("booking_payment_version")<1)throw IllegalStateException("update_server");api.request("/api/bookings","POST",JSONObject().put("service_id",service.optInt("id")).put("confirmed",true).put("price_cents",service.optLong("price_cents")).put("request_key",key));confirm=false;done()}catch(e:CancellationException){throw e}catch(e:Exception){error=commerceError(e);confirm=false}finally{busy=false}}}},enabled=!busy){Text("تأكيد وحجز المبلغ")}},dismissButton={TextButton({confirm=false},enabled=!busy){Text("إلغاء")}})
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun BookingHistory(api:LayaliApi,back:()->Unit){
    val scope=rememberCoroutineScope();var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var revision by remember{mutableIntStateOf(0)};var busy by remember{mutableStateOf(false)};var error by remember{mutableStateOf("")};var decision by remember{mutableStateOf<Pair<JSONObject,String>?>(null)}
    LaunchedEffect(revision){try{rows=api.request("/api/bookings").platformItems()}catch(e:Exception){error=commerceError(e)}}
    Scaffold(topBar={TopAppBar(title={Text("الحجوزات والطلبات")},navigationIcon={IconButton(back){Icon(Icons.Outlined.ArrowForward,"رجوع")}},actions={TextButton({revision++}){Text("تحديث")}})}){padding->LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(12.dp)){
        if(error.isNotBlank())item{Text(error,color=MaterialTheme.colorScheme.error)}
        if(rows.isEmpty())item{Text("لا توجد حجوزات بعد.")}
        items(rows,key={it.getString("id")}){row->Card(Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(8.dp)){
            Text(row.optString("title",row.optString("service__title")),style=MaterialTheme.typography.titleMedium);Text(if(row.optBoolean("owner"))row.optString("buyer_name")else row.optString("owner_name"));Text(usd(row.optLong("price_cents")));Text(operationLabel(row.optString("status")));Text("${row.optInt("duration_minutes",30)} دقيقة");Text(row.optString("terms"),style=MaterialTheme.typography.bodySmall)
            Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){
                if(row.optString("status")=="paid_reserved"){
                    if(row.optBoolean("owner")){TextButton({decision=row to "accept"},enabled=!busy){Text("قبول")};TextButton({decision=row to "reject"},enabled=!busy){Text("رفض وإعادة المبلغ")}}
                    else TextButton({decision=row to "cancel"},enabled=!busy){Text("إلغاء واسترداد")}
                }
                if(row.optString("status")=="accepted"&&!row.optBoolean("owner"))TextButton({decision=row to "complete"},enabled=!busy){Text("استلمت الخدمة")}
            }
        }}}
    }}
    decision?.let{(row,action)->AlertDialog(onDismissRequest={decision=null},title={Text("تأكيد الإجراء")},text={Text(if(action=="complete")"تأكيد استلام الخدمة يحوّل مستحقات البائع ويُنهي الحجز. أكّد بعد تنفيذ الخدمة فقط."else"سيتم تحديث حالة الطلب وإعادة الحجز المالي عند الإلغاء أو الرفض.")},confirmButton={TextButton({decision=null;busy=true;scope.launch{try{api.request("/api/bookings/${row.getString("id")}","POST",JSONObject().put("action",action));revision++}catch(e:Exception){error=commerceError(e)}finally{busy=false}}}){Text("تأكيد")}},dismissButton={TextButton({decision=null}){Text("إلغاء")}})}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun FinancialHistory(api:LayaliApi,back:()->Unit){
    var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var error by remember{mutableStateOf("")};var revision by remember{mutableIntStateOf(0)};var receipt by remember{mutableStateOf<JSONObject?>(null)};val scope=rememberCoroutineScope()
    LaunchedEffect(revision){try{rows=api.request("/api/financial-history").platformItems()}catch(e:Exception){error=commerceError(e)}}
    Scaffold(topBar={TopAppBar(title={Text("سجل العمليات")},navigationIcon={IconButton(back){Icon(Icons.Outlined.ArrowForward,"رجوع")}},actions={TextButton({revision++}){Text("تحديث")}})}){padding->LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp),verticalArrangement=Arrangement.spacedBy(10.dp)){
        if(error.isNotBlank())item{Text(error,color=MaterialTheme.colorScheme.error)}
        if(rows.isEmpty()&&error.isBlank())item{Text("لا توجد عمليات مسجّلة.")}
        items(rows,key={it.getString("id")}){row->OutlinedCard(onClick={scope.launch{try{receipt=api.request("/api/receipts/"+java.net.URLEncoder.encode(row.getString("id"),"UTF-8"))}catch(e:Exception){error=commerceError(e)}}},modifier=Modifier.fillMaxWidth()){Column(Modifier.padding(16.dp),verticalArrangement=Arrangement.spacedBy(6.dp)){
            Text(when(row.optString("kind")){"live_entry"->"دخول بث خاص";"live_gift"->"هدية بث";"booking"->"حجز خدمة";"chat_message"->"رسالة مدفوعة";"chat_unlock"->"فتح مرفق";else->"شحن الرصيد"},style=MaterialTheme.typography.titleMedium)
            Text((if(row.optBoolean("incoming"))"وارد · "else"صادر · ")+usd(row.optLong("amount_cents")));Text(operationLabel(row.optString("status")));Text(row.optString("created_at").take(19).replace('T',' '),style=MaterialTheme.typography.bodySmall)
        }}}
    }}
    receipt?.let{r->AlertDialog(onDismissRequest={receipt=null},title={Text("إيصال العملية")},text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){Text(r.optString("account_name"));Text(usd(r.optLong("amount_cents")));Text(operationLabel(r.optString("status")));Text(r.optString("id"));Text(r.optString("reference"));Text(r.optString("receipt_notice"),style=MaterialTheme.typography.bodySmall)}},confirmButton={TextButton({receipt=null}){Text("إغلاق")}})}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable internal fun BlockedAccounts(api:LayaliApi,back:()->Unit){
    var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())};var revision by remember{mutableIntStateOf(0)};var error by remember{mutableStateOf("")};val scope=rememberCoroutineScope()
    LaunchedEffect(revision){try{rows=api.request("/api/blocks").platformItems()}catch(e:Exception){error=commerceError(e)}}
    Scaffold(topBar={TopAppBar(title={Text("الحسابات المحظورة")},navigationIcon={IconButton(back){Icon(Icons.Outlined.ArrowForward,"رجوع")}})}){padding->LazyColumn(Modifier.fillMaxSize().padding(padding),contentPadding=PaddingValues(16.dp)){
        if(error.isNotBlank())item{Text(error,color=MaterialTheme.colorScheme.error)}
        if(rows.isEmpty()&&error.isBlank())item{Text("لا توجد حسابات محظورة.")}
        items(rows,key={it.optInt("id")}){row->Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text(row.optString("name"));TextButton({scope.launch{try{api.request("/api/blocks","POST",JSONObject().put("user_id",row.optInt("id")).put("blocked",false));revision++}catch(e:Exception){error=commerceError(e)}}}){Text("إلغاء الحظر")}}}
    }}
}
