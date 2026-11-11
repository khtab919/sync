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

private data class StorePackage(val coins:Int,val bonusPoints:Int)
private data class StorePaymentMethod(val code:String,val name:String,val description:String,val symbol:String,val enabled:Boolean)

private fun storeMethods(info:JSONObject):List<StorePaymentMethod> = listOf(
    StorePaymentMethod("usdt","USDT · BEP20","تحويل USDT على شبكة BNB Smart Chain","₮",info.optString("address").isNotBlank()),
    StorePaymentMethod("shamcash","شام كاش","تحويل محلي مع إثبات الدفع","◈",info.optBoolean("shamcash_enabled")),
    StorePaymentMethod("nowpayments","NOWPayments","فاتورة إلكترونية تؤكدها بوابة الدفع","✦",info.optBoolean("nowpayments_enabled"))
)

private fun storePackages(info:JSONObject):List<StorePackage>{
    val detailed=info.optJSONArray("packages")
    if(detailed!=null)return (0 until detailed.length()).mapNotNull{ i ->
        val row=detailed.optJSONObject(i)?:return@mapNotNull null
        val coins=row.optInt("coins")
        if(coins>=100)StorePackage(coins,row.optInt("bonus_points").coerceAtLeast(0))else null
    }
    val legacy=info.optJSONArray("packages_cents")?:return emptyList()
    return (0 until legacy.length()).mapNotNull{i->
        legacy.optInt(i).takeIf{it>=100}?.let{StorePackage(it,0)}
    }
}

@Composable
internal fun CoinPackage(coins:Int,bonus:Int,dollars:String,selected:Boolean,badge:String?,choose:()->Unit){
    val colors=MaterialTheme.colorScheme
    Surface(
        onClick=choose,
        shape=RoundedCornerShape(18.dp),
        color=if(selected)colors.primaryContainer else colors.surface,
        shadowElevation=if(selected)4.dp else 1.dp,
        border=BorderStroke(if(selected)2.dp else 1.dp,if(selected)colors.primary else colors.outlineVariant)
    ){
        Column(
            Modifier.fillMaxWidth().heightIn(min=174.dp).padding(horizontal=6.dp,vertical=10.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(5.dp)
        ){
            if(badge!=null){
                Surface(color=colors.primary,shape=RoundedCornerShape(10.dp)){
                    Text(badge,Modifier.padding(horizontal=6.dp,vertical=3.dp),fontSize=10.sp,color=colors.onPrimary,fontWeight=FontWeight.SemiBold,maxLines=1)
                }
            }else Spacer(Modifier.height(19.dp))
            Text("🪙",fontSize=33.sp)
            Text(java.text.NumberFormat.getIntegerInstance().format(coins),fontSize=20.sp,fontWeight=FontWeight.Bold,maxLines=1)
            Text("كوين",fontSize=12.sp,color=colors.onSurfaceVariant)
            Text(
                if(bonus>0)"+${java.text.NumberFormat.getIntegerInstance().format(bonus)} نقطة ترويجية" else "دون مكافأة إضافية",
                fontSize=10.sp,color=if(bonus>0)colors.primary else colors.onSurfaceVariant,maxLines=2,lineHeight=13.sp
            )
            Spacer(Modifier.weight(1f))
            Surface(color=colors.surfaceVariant,shape=RoundedCornerShape(10.dp)){
                Text("$dollars USD",Modifier.padding(horizontal=7.dp,vertical=6.dp),fontSize=11.sp,fontWeight=FontWeight.SemiBold,maxLines=1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    var showPaymentSheet by rememberSaveable{mutableStateOf(false)}
    var quoteText by rememberSaveable{mutableStateOf("")}
    var submittedId by rememberSaveable { mutableStateOf("") }
    var submittedStatus by rememberSaveable { mutableStateOf("pending") }
    var submittedAmount by rememberSaveable { mutableLongStateOf(0L) }
    var showOrders by rememberSaveable { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    var receipt by rememberSaveable{mutableStateOf("")}
    var preview by remember{mutableStateOf("")}
    var reference by rememberSaveable{mutableStateOf("")}
    var busy by remember{mutableStateOf(false)}
    var note by remember{mutableStateOf("")}
    val quote=remember(quoteText){if(quoteText.isBlank())null else runCatching{JSONObject(quoteText)}.getOrNull()}
    val packages=remember(info){storePackages(info)}
    val methods=remember(info){storeMethods(info)}
    var quoteSeconds by remember{mutableLongStateOf(0L)}
    LaunchedEffect(quoteText){
        val quotation=quote ?: return@LaunchedEffect
        val expiry=runCatching{Instant.parse(quotation.getString("expires_at")).toEpochMilli()}.getOrDefault(0L)
        val clockOffset=quotation.optLong("server_now_ms",System.currentTimeMillis())-System.currentTimeMillis()
        while(true){
            quoteSeconds=((expiry-System.currentTimeMillis()-clockOffset+999L)/1000L).coerceAtLeast(0L)
            if(quoteSeconds<=0L)break
            delay(1000)
        }
    }
    suspend fun reload(){
        val updated=api.request("/api/payment-info")
        info=updated
        val available=storeMethods(updated).filter{it.enabled}
        if(available.none{it.code==method})method=available.firstOrNull()?.code.orEmpty()
        balance=api.request("/api/wallet").optLong("available_cents")
        val a=api.request("/api/topups").optJSONArray("items")
        rows=if(a==null)emptyList()else(0 until a.length()).map{a.getJSONObject(it)}
    }
    fun perform(block:suspend()->Unit){if(busy)return;busy=true;note="";scope.launch{try{block()}catch(e:CancellationException){throw e}catch(e:Exception){note=commerceError(e)}finally{busy=false}}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)perform{val r=api.upload(context,uri,"receipt");receipt=r.getString("id");preview=r.getString("url");note="تم رفع الإيصال."}}
    LaunchedEffect(Unit){busy=true;try{reload()}catch(e:CancellationException){throw e}catch(e:Exception){note=commerceError(e)}finally{busy=false}}
    LaunchedEffect(quoteText, submittedId, showOrders) { scrollState.scrollTo(0) }

    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().verticalScroll(scrollState).padding(22.dp),verticalArrangement=Arrangement.spacedBy(18.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            TextButton(onClick=back,enabled=!busy){Text("رجوع")}
            Spacer(Modifier.weight(1f))
            Text("Relaxia",fontWeight=FontWeight.SemiBold,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("متجر الكوينز",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.Bold)
        Surface(shape=RoundedCornerShape(18.dp),color=MaterialTheme.colorScheme.primaryContainer){
            Row(Modifier.fillMaxWidth().padding(16.dp),verticalAlignment=Alignment.CenterVertically){
                Text("🪙",fontSize=30.sp)
                Spacer(Modifier.width(10.dp))
                Column{
                    Text("رصيد الكوينز المتاح",style=MaterialTheme.typography.bodySmall)
                    Text(if(balance==null)"—"else java.text.NumberFormat.getIntegerInstance().format(balance)+" كوين",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                }
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            FilterChip(!showOrders,{showOrders=false},label={Text("شراء رصيد")})
            FilterChip(showOrders,{showOrders=true},label={Text("طلباتي")})
        }
        if(!showOrders && submittedId.isBlank()) Text(if(quote==null)"اختر باقة الشحن"else"أكمل عملية الدفع",style=MaterialTheme.typography.titleMedium)
        Text("100 كوين = 1 USD كقيمة أساسية داخل التطبيق. يظهر المبلغ النهائي قبل الدفع. النقاط الترويجية منفصلة وغير قابلة للسحب.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(note.isNotBlank())Text(note,color=MaterialTheme.colorScheme.primary)
        if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        if(submittedId.isNotBlank()) {
            PurchaseStatusCard(submittedId,submittedAmount,submittedStatus)
            OutlinedButton(onClick={perform{reload();rows.firstOrNull{it.optString("id")==submittedId}?.let{submittedStatus=it.optString("status")}}},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("تحديث حالة الطلب")}
            TextButton(onClick={submittedId="";showOrders=true}){Text("عرض جميع طلباتي")}
            if(submittedStatus!="pending")TextButton(onClick={submittedId="";showOrders=false}){Text("شراء رصيد جديد")}
        } else if(!showOrders && quote==null){
            if(packages.isEmpty()&&!busy)Text("لا تتوفر باقات شحن حاليًا. أعد تحديث المتجر لاحقًا.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            val maxBonus=packages.maxOfOrNull{it.bonusPoints}?:0
            BoxWithConstraints(Modifier.fillMaxWidth()){
                val columns=if(maxWidth>=335.dp)3 else 2
                packages.chunked(columns).forEach{line->
                    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
                        line.forEach{option->
                            val badge=if(option.bonusPoints>0&&option.bonusPoints==maxBonus)"أكبر مكافأة" else null
                            Box(Modifier.weight(1f)){
                                CoinPackage(
                                    coins=option.coins,bonus=option.bonusPoints,
                                    dollars=BigDecimal.valueOf(option.coins.toLong(),2).toPlainString(),
                                    selected=selected==option.coins,badge=badge
                                ){selected=option.coins;showPaymentSheet=true}
                            }
                        }
                        repeat(columns-line.size){Spacer(Modifier.weight(1f))}
                    }
                    Spacer(Modifier.height(9.dp))
                }
            }
            Text("اضغط على أي باقة لعرض وسائل الدفع المتاحة وتأكيد السعر.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }else if(!showOrders && quote!=null){
            Text("${quote.optInt("amount_cents")} كوين · +${quote.optInt("bonus_cents")} نقطة ترويجية",fontSize=20.sp,fontWeight=FontWeight.Bold)
            Text(
                if(quoteSeconds>0L)"صلاحية عرض السعر: %02d:%02d".format(quoteSeconds/60,quoteSeconds%60)
                else "انتهت صلاحية عرض السعر. إن لم تدفع، اطلب عرضًا جديدًا. إن دفعت بالفعل، أكمل إرسال الإثبات للمراجعة.",
                style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text("المطلوب: ${quote.optString("payment_amount")} ${quote.optString("payment_currency")}",style=MaterialTheme.typography.titleLarge)
            val rail=quote.optString("method")
            if(rail=="nowpayments") {
                Text("دفع آمن عبر NOWPayments",style=MaterialTheme.typography.titleLarge)
                Text("تختار العملة في صفحة البوابة. الرصيد يضاف بعد اكتمال الدفع وتأكيد الخادم؛ العودة من صفحة الدفع وحدها لا تعتمد العملية.",style=MaterialTheme.typography.bodySmall)
                Button(onClick={perform{
                    val invoice=api.request("/api/nowpayments/invoices","POST",JSONObject().put("quote_id",quote.getString("id")))
                    val link=invoice.getString("invoice_url");val parsed=Uri.parse(link)
                    require(parsed.scheme=="https"&&parsed.host in listOf("nowpayments.io","www.nowpayments.io")&&parsed.userInfo==null)
                    invoiceUrl=link;uriHandler.openUri(link)
                }},enabled=!busy,modifier=Modifier.fillMaxWidth().height(52.dp)){Text(if(invoiceUrl.isBlank())"فتح بوابة الدفع"else"فتح فاتورة الدفع مجددًا")}
                OutlinedButton(onClick={perform{
                    val result=api.request("/api/nowpayments/status/${quote.getString("id")}")
                    if(result.optBoolean("credited")){quoteText="";reload();note="تم تأكيد الدفع وإضافة الرصيد."}else note="الدفع بانتظار اكتمال تأكيد البوابة. لم يضف الرصيد بعد."
                }},enabled=!busy&&invoiceUrl.isNotBlank(),modifier=Modifier.fillMaxWidth()){Text("تحقق من الدفع وتحديث الرصيد")}
            } else {
            Text(if(rail=="usdt")"USDT على BEP20 فقط"else"استلام عبر شام كاش",fontWeight=FontWeight.Bold)
            ReferenceImage(quote.optString("qr_url"),Modifier.fillMaxWidth().height(235.dp))
            Text(quote.optString("destination"),style=MaterialTheme.typography.bodyMedium.copy(textDirection=androidx.compose.ui.text.style.TextDirection.Ltr))
            OutlinedButton(onClick={(context.getSystemService(Context.CLIPBOARD_SERVICE)as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Relaxia payment",quote.optString("destination")));note="تم نسخ بيانات الاستلام."}){Text("نسخ بيانات الاستلام")}
            if(rail=="shamcash"&&info.optString("shamcash_instructions").isNotBlank())Text(info.optString("shamcash_instructions"))
            OutlinedTextField(reference,{reference=it.trim()},label={Text(if(rail=="usdt")"رقم المعاملة TxID"else"رقم إشعار الدفع")},modifier=Modifier.fillMaxWidth())
            Text(if(rail=="usdt")"أدخل TxID كاملًا: 64 حرفًا أو رقمًا من إيصال التحويل."else"أدخل رقم إشعار التحويل كما يظهر في الإيصال.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick={picker.launch("image/*")},enabled=!busy){Text(if(receipt.isBlank())"رفع صورة إثبات الدفع"else"استبدال الإيصال")}
            if(preview.isNotBlank())ReferenceImage(preview,Modifier.fillMaxWidth().height(150.dp))
            Text("الرصيد يضاف بعد تحقق المسؤول من وصول المبلغ فعليًا. صورة الإيصال وحدها لا تكفي.",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            Button(onClick={perform{val result=api.request("/api/topups","POST",JSONObject().put("quote_id",quote.getString("id")).put("transaction_hash",reference).put("receipt_id",receipt));submittedId=result.getString("id");submittedStatus=result.optString("status","pending");submittedAmount=quote.optLong("amount_cents");quoteText="";receipt="";reference="";preview="";showOrders=true;try{reload()}catch(e:CancellationException){throw e}catch(e:Exception){note="تم استلام الطلب. تعذّر تحديث القائمة؛ يمكنك تحديث الحالة لاحقًا."}}},enabled=!busy&&receipt.isNotBlank()&&validPaymentReference(rail,reference),modifier=Modifier.fillMaxWidth().height(52.dp)){Text(if(busy)"جارٍ تقديم الطلب…"else"تقديم طلب الشراء")}
            }
            TextButton(onClick={quoteText=""},enabled=!busy){Text("العودة إلى الباقات")}
        }
        if(showOrders && submittedId.isBlank()) {
            Text("طلبات الشراء",style=MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick={perform{reload()}},enabled=!busy){Text("تحديث الطلبات")}
            if(rows.isEmpty()&&!busy)Text("لا توجد طلبات شراء حتى الآن.",color=MaterialTheme.colorScheme.onSurfaceVariant)
            rows.forEach { r ->
                PurchaseStatusCard(r.optString("id"),r.optLong("amount_cents"),r.optString("status"),r.optString("review_note"))
            }
        }
    }}
    if(showPaymentSheet && !showOrders && quote==null && submittedId.isBlank()){
        ModalBottomSheet(onDismissRequest={showPaymentSheet=false}){
            Column(
                Modifier.fillMaxWidth().padding(horizontal=22.dp).padding(bottom=28.dp),
                verticalArrangement=Arrangement.spacedBy(13.dp)
            ){
                Text("اختر طريقة الدفع",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                val chosen=packages.firstOrNull{it.coins==selected}
                Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(16.dp)){
                    Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text("🪙 ${java.text.NumberFormat.getIntegerInstance().format(selected)} كوين",fontSize=21.sp,fontWeight=FontWeight.Bold)
                        Text("القيمة الأساسية: ${BigDecimal.valueOf(selected.toLong(),2).toPlainString()} USD",style=MaterialTheme.typography.bodyMedium)
                        if((chosen?.bonusPoints?:0)>0)Text("+${chosen?.bonusPoints} نقطة ترويجية إضافية",color=MaterialTheme.colorScheme.primary)
                    }
                }
                methods.forEach{option->
                    Surface(
                        onClick={method=option.code},enabled=option.enabled,
                        color=if(method==option.code&&option.enabled)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        border=BorderStroke(1.dp,if(method==option.code&&option.enabled)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        shape=RoundedCornerShape(14.dp)
                    ){
                        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            Text(option.symbol,fontSize=23.sp,color=MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)){
                                Text(option.name,fontWeight=FontWeight.SemiBold)
                                Text(if(option.enabled)option.description else "غير مفعّل حاليًا",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            RadioButton(selected=method==option.code&&option.enabled,onClick=null,enabled=option.enabled)
                        }
                    }
                }
                Text("لن يتم الخصم عند اختيار الوسيلة. سيعرض الخادم المبلغ النهائي وبيانات التحويل قبل الدفع.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(note.isNotBlank())Text(note,color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
                Button(
                    onClick={perform{
                        val result=api.request("/api/topup-quotes","POST",JSONObject().put("method",method).put("amount_cents",selected))
                        quoteText=result.toString();invoiceUrl="";receipt="";reference="";preview="";showPaymentSheet=false
                    }},
                    enabled=!busy&&methods.any{it.code==method&&it.enabled},
                    modifier=Modifier.fillMaxWidth().height(52.dp),shape=RoundedCornerShape(15.dp)
                ){Text(if(busy)"جارٍ تجهيز الطلب…" else "متابعة الدفع",fontSize=16.sp)}
            }
        }
    }
}

internal fun validPaymentReference(method: String, value: String): Boolean =
    if(method=="usdt") Regex("[0-9a-fA-F]{64}").matches(value.trim().removePrefix("0x").removePrefix("0X"))
    else Regex("[A-Za-z0-9_-]{4,80}").matches(value.trim())

@Composable
internal fun PurchaseStatusCard(id: String, amount: Long, status: String, reason: String = "") {
    ReferenceCard {
        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Text(when(status){"approved"->"تمت إضافة الرصيد";"rejected"->"الطلب مرفوض";else->"تم تقديم الطلب، انتظر قليلًا"},style=MaterialTheme.typography.titleLarge)
            Text("$amount رصيد",style=MaterialTheme.typography.titleMedium)
            Text("رقم الطلب: ${id.take(8)}",style=MaterialTheme.typography.bodySmall)
            Text(when(status){"approved"->"تم تأكيد الدفع وإضافة الرصيد إلى محفظتك.";"rejected"->reason.ifBlank{"راجع بيانات التحويل أو تواصل مع الدعم."};else->"طلبك قيد المراجعة. يمكنك متابعة حالته هنا؛ لا تُعد إرسال التحويل نفسه."},style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
