package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.platform.LocalUriHandler
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.api.LayaliApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal
import java.time.Instant

@Composable
internal fun CoinPackage(coins:Int,bonus:Int,dollars:String,selected:Boolean,choose:()->Unit) {
    Surface(onClick=choose,shape=RoundedCornerShape(22.dp),color=if(selected)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,border=BorderStroke(if(selected)2.dp else 1.dp,if(selected)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.spacedBy(6.dp)) {
            Text("◉",fontSize=28.sp,color=Color(0xFFC7955D))
            Text("$coins",fontSize=27.sp,fontWeight=FontWeight.Bold)
            Text("كوينز",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(bonus>0) Text("+$bonus هدية",color=MaterialTheme.colorScheme.primary,fontSize=12.sp)
            Text("$$dollars",fontSize=17.sp,fontWeight=FontWeight.SemiBold)
        }
    }
}

@Composable
internal fun CoinShopScreen(api:LayaliApi,back:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val uriHandler=LocalUriHandler.current
    var invoiceUrl by rememberSaveable{mutableStateOf("")}
    var info by remember{mutableStateOf(JSONObject())}
    var balance by remember{mutableStateOf<Long?>(null)}
    var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())}
    var selected by rememberSaveable{mutableIntStateOf(1000)}
    var method by rememberSaveable{mutableStateOf("usdt")}
    var quoteText by rememberSaveable{mutableStateOf("")}
    var offset by rememberSaveable{mutableLongStateOf(0L)}
    var remaining by remember{mutableLongStateOf(0L)}
    var receipt by rememberSaveable{mutableStateOf("")}
    var preview by remember{mutableStateOf("")}
    var reference by rememberSaveable{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var note by remember{mutableStateOf("")}
    val quote=remember(quoteText){if(quoteText.isBlank())null else runCatching{JSONObject(quoteText)}.getOrNull()}
    suspend fun reload(){info=api.request("/api/payment-info");balance=api.request("/api/wallet").optLong("available_cents");val a=api.request("/api/topups").optJSONArray("items");rows=if(a==null)emptyList()else(0 until a.length()).map{a.getJSONObject(it)}}
    fun perform(block:suspend()->Unit){if(busy)return;busy=true;note="";scope.launch{try{block()}catch(e:CancellationException){throw e}catch(e:Exception){note=commerceError(e)}finally{busy=false}}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)perform{val r=api.upload(context,uri,"receipt");receipt=r.getString("id");preview=r.getString("url");note="تم رفع الإيصال."}}
    LaunchedEffect(Unit){busy=true;try{reload()}catch(e:CancellationException){throw e}catch(e:Exception){note=commerceError(e)}finally{busy=false}}
    LaunchedEffect(quoteText){while(quote!=null){remaining=((Instant.parse(quote.optString("expires_at")).toEpochMilli()-System.currentTimeMillis()-offset)/1000).coerceAtLeast(0);delay(1000)}}
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(rememberScrollState()).padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){TextButton(back,enabled=!busy){Text("رجوع")};Spacer(Modifier.weight(1f));Text(if(balance==null)"الرصيد —"else"${balance} كوينز",fontSize=13.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        Text("متجر الكوينز",style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.Bold)
        Text("اختر باقتك ووسيلة الدفع. كل 100 كوينز = 1 دولار رصيد داخلي.",color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(note.isNotBlank())Text(note,color=MaterialTheme.colorScheme.primary)
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(quote==null){
            listOf(100,300,1000,2500,5000,10000).chunked(2).forEach{pair->Row(horizontalArrangement=Arrangement.spacedBy(12.dp)){pair.forEach{amount->val bonus=amount*(if(amount>=10000)8 else if(amount>=5000)6 else if(amount>=2500)4 else 0)/100;Box(Modifier.weight(1f)){CoinPackage(amount,bonus,BigDecimal.valueOf(amount.toLong(),2).toPlainString(),selected==amount){selected=amount}}}}}
            Text("وسيلة الدفع",style=MaterialTheme.typography.titleLarge)
            Row(Modifier.horizontalScroll(rememberScrollState()),horizontalArrangement=Arrangement.spacedBy(10.dp)){
                FilterChip(method=="usdt",{method="usdt"},label={Text("USDT · BEP20")})
                FilterChip(method=="shamcash",{method="shamcash"},label={Text("شام كاش")},enabled=info.optBoolean("shamcash_enabled"))
                FilterChip(method=="nowpayments",{method="nowpayments"},label={Text("NOWPayments")},enabled=info.optBoolean("nowpayments_enabled"))
            }
            if(!info.optBoolean("nowpayments_enabled"))Text("NOWPayments يحتاج إعداد مفاتيح حساب التاجر على الخادم.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(!info.optBoolean("shamcash_enabled"))Text("شام كاش ينتظر إعداد بيانات الاستلام من الإدارة.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick={perform{val result=api.request("/api/topup-quotes","POST",JSONObject().put("method",method).put("amount_cents",selected));offset=result.optLong("server_now_ms",System.currentTimeMillis())-System.currentTimeMillis();quoteText=result.toString();invoiceUrl="";receipt="";reference="";preview=""}},enabled=!busy&&info.has("address"),modifier=Modifier.fillMaxWidth().height(54.dp),shape=RoundedCornerShape(17.dp)){Text("متابعة الدفع",fontSize=17.sp)}
        }else{
            Text("${quote.optInt("coins")} كوينز",fontSize=30.sp,fontWeight=FontWeight.Bold)
            Text("المطلوب: ${quote.optString("payment_amount")} ${quote.optString("payment_currency")}",style=MaterialTheme.typography.titleLarge)
            Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.primaryContainer){Column(Modifier.fillMaxWidth().padding(18.dp),horizontalAlignment=Alignment.CenterHorizontally){
                Text("مدة صلاحية طلب الشراء",fontSize=12.sp)
                Text(String.format("%02d:%02d",remaining/60,remaining%60),fontSize=34.sp,fontWeight=FontWeight.Bold)
                Text(if(remaining>0)"العدّاد مرتبط بهذا الطلب ولا يُعاد عند فتح الصفحة."else"انتهت المهلة. إن كنت دفعت، ارفع إثبات الدفع للمراجعة.",fontSize=12.sp)
            }}
            val rail=quote.optString("method")
            if(rail=="nowpayments") {
                Text("دفع آمن عبر NOWPayments",style=MaterialTheme.typography.titleLarge)
                Text("تختار العملة في صفحة البوابة. الرصيد يضاف بعد اكتمال الدفع وتأكيد الخادم؛ العودة من صفحة الدفع وحدها لا تعتمد العملية.",style=MaterialTheme.typography.bodySmall)
                Button(onClick={perform{
                    val invoice=api.request("/api/nowpayments/invoices","POST",JSONObject().put("quote_id",quote.getString("id")))
                    val link=invoice.getString("invoice_url");val parsed=Uri.parse(link)
                    require(parsed.scheme=="https"&&parsed.host in listOf("nowpayments.io","www.nowpayments.io")&&parsed.userInfo==null)
                    invoiceUrl=link;uriHandler.openUri(link)
                }},enabled=!busy&&remaining>0,modifier=Modifier.fillMaxWidth().height(52.dp)){Text(if(invoiceUrl.isBlank())"فتح بوابة الدفع"else"فتح فاتورة الدفع مجددًا")}
                OutlinedButton(onClick={perform{
                    val result=api.request("/api/nowpayments/status/${quote.getString("id")}")
                    if(result.optBoolean("credited")){quoteText="";reload();note="تم تأكيد الدفع وإضافة الكوينز."}else note="الدفع بانتظار اكتمال تأكيد البوابة. لم يضف الرصيد بعد."
                }},enabled=!busy&&invoiceUrl.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("تحقق من الدفع وتحديث الرصيد")}
            } else {
            Text(if(rail=="usdt")"USDT على BEP20 فقط"else"استلام عبر شام كاش",fontWeight=FontWeight.Bold)
            ReferenceImage(quote.optString("qr_url"),Modifier.fillMaxWidth().height(235.dp))
            Text(quote.optString("destination"),fontSize=15.sp)
            OutlinedButton(onClick={(context.getSystemService(Context.CLIPBOARD_SERVICE)as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Relaxia payment",quote.optString("destination")));note="تم نسخ بيانات الاستلام."}){Text("نسخ بيانات الاستلام")}
            if(rail=="shamcash"&&info.optString("shamcash_instructions").isNotBlank())Text(info.optString("shamcash_instructions"))
            OutlinedTextField(reference,{reference=it.trim()},label={Text(if(rail=="usdt")"رقم المعاملة TxID"else"رقم إشعار الدفع")},modifier=Modifier.fillMaxWidth())
            OutlinedButton(onClick={picker.launch("image/*")},enabled=!busy){Text(if(receipt.isBlank())"رفع صورة إثبات الدفع"else"استبدال الإيصال")}
            if(preview.isNotBlank())ReferenceImage(preview,Modifier.fillMaxWidth().height(150.dp))
            Text("الرصيد يضاف بعد تحقق المسؤول من وصول المبلغ فعليًا. صورة الإيصال وحدها لا تكفي.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick={perform{api.request("/api/topups","POST",JSONObject().put("quote_id",quote.getString("id")).put("transaction_hash",reference).put("receipt_id",receipt));quoteText="";receipt="";reference="";reload();note="أُرسل الدفع للتحقق. ستظهر نتيجة المراجعة في طلباتك."}},enabled=!busy&&receipt.isNotBlank()&&reference.isNotBlank(),modifier=Modifier.fillMaxWidth().height(52.dp)){Text("إرسال للتحقق")}
            }
            TextButton(onClick={quoteText=""},enabled=!busy){Text("العودة إلى الباقات")}
        }
        if(rows.isNotEmpty()){Text("طلبات الشراء",style=MaterialTheme.typography.titleLarge);rows.forEach{r->ListItem(headlineContent={Text("${r.optLong("amount_cents")+r.optLong("bonus_cents")} كوينز")},supportingContent={Text(when(r.optString("status")){"pending"->"قيد المراجعة";"approved"->"تمت الإضافة";else->"مرفوض · ${r.optString("review_note")}"})})}}
    }}
}
