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
import androidx.compose.material.icons.rounded.Toll
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
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

private data class StorePackage(val coins:Int,val bonusPoints:Int){ val id:String get()="pack_$coins" }
private data class StorePaymentMethod(val code:String,val name:String,val description:String,val symbol:String,val enabled:Boolean)

private fun storeMethods(info:JSONObject):List<StorePaymentMethod> = listOf(
    StorePaymentMethod("stripe","فيزا / ماستركارد","بوابة الدفع العالمية · بطاقات بنكية","💳",info.optBoolean("stripe_enabled")),
    StorePaymentMethod("nowpayments","بوابة الدفع الرقمية","NOWPayments · مدفوعات إلكترونية","✦",info.optBoolean("nowpayments_enabled")),
    StorePaymentMethod("usdt","USDT · BEP20","تحويل USDT على شبكة BNB Smart Chain","₮",info.optString("address").isNotBlank()),
    StorePaymentMethod("shamcash","شام كاش","تحويل محلي مع إثبات الدفع","◈",info.optBoolean("shamcash_enabled"))
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

private val CoinNavy=Color(0xFF0B162C)
private val CoinGlass=Color(0xD916223B)
private val CoinGold=Color(0xFFF4C37D)

@Composable
private fun Coin3DArtwork(coins:Int) {
    val context=LocalContext.current
    val bitmap=remember(context,coins){
        runCatching {
            context.assets.open("coins/ic_coin_${coins}.webp").use { android.graphics.BitmapFactory.decodeStream(it) }
        }.getOrNull()
    }
    if(bitmap!=null) {
        androidx.compose.foundation.Image(
            bitmap=bitmap.asImageBitmap(),
            contentDescription="مجسم كوينز بقيمة ${coins}",
            modifier=Modifier.size(69.dp),
            contentScale=androidx.compose.ui.layout.ContentScale.Fit
        )
    } else {
        // Standalone signed APK still renders a functional card before art is packaged.
        Icon(androidx.compose.material.icons.Icons.Rounded.Toll,
            contentDescription="كوينز ${coins}",
            modifier=Modifier.size(53.dp),tint=CoinGold)
    }
}

@Composable
internal fun CoinPackage(coins:Int,bonus:Int,dollars:String,selected:Boolean,badge:String?,choose:()->Unit){
    Surface(
        onClick=choose,
        modifier=Modifier.testTag("pack_$coins"),
        shape=RoundedCornerShape(16.dp),
        color=CoinGlass,
        contentColor=Color.White,
        shadowElevation=if(selected)9.dp else 2.dp,
        border=BorderStroke(if(selected)1.5.dp else 1.dp,
            if(selected)Color(0xFFF59E0B) else Color(0xFF465876))
    ){
        Column(
            modifier=Modifier.fillMaxWidth().heightIn(min=164.dp)
                .padding(horizontal=4.dp,vertical=8.dp),
            horizontalAlignment=Alignment.CenterHorizontally,
            verticalArrangement=Arrangement.spacedBy(4.dp)
        ){
            if(badge!=null) {
                Surface(shape=RoundedCornerShape(10.dp),color=Color(0xFF2D3955)){
                    Text(badge,Modifier.padding(horizontal=7.dp,vertical=2.dp),
                        color=Color.White,fontSize=9.sp,fontWeight=FontWeight.Bold,maxLines=1)
                }
            } else Spacer(Modifier.height(16.dp))
            Text(java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(coins)+" كوين",
                fontSize=if(coins>=10000)13.sp else 14.sp,fontWeight=FontWeight.ExtraBold,
                color=Color.White,maxLines=1)
            Coin3DArtwork(coins)
            if(bonus>0) {
                Text("+${java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(bonus)} نقطة ترويجية",
                    fontSize=9.sp,fontWeight=FontWeight.SemiBold,color=Color(0xFF4DE6A0),
                    maxLines=1)
            } else Spacer(Modifier.height(14.dp))
            Surface(color=Color(0xFF2C3955),shape=RoundedCornerShape(50.dp)){
                Text("USD ${dollars}",Modifier.padding(horizontal=10.dp,vertical=7.dp),
                    color=Color.White,fontSize=12.sp,fontWeight=FontWeight.Bold,maxLines=1)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CoinShopScreen(api:LayaliApi,back:()->Unit,preferredMethod:String?=null) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var invoiceUrl by rememberSaveable{mutableStateOf("")}
    var checkoutUrl by rememberSaveable{mutableStateOf("")}
    var checkoutProvider by rememberSaveable{mutableStateOf("")}
    var info by remember{mutableStateOf(JSONObject())}
    var balance by remember{mutableStateOf<Long?>(null)}
    var rows by remember{mutableStateOf<List<JSONObject>>(emptyList())}
    var selected by rememberSaveable{mutableIntStateOf(1000)}
    var method by rememberSaveable{mutableStateOf(preferredMethod ?: "stripe")}
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
    val methods=remember(info,preferredMethod){
        storeMethods(info).filter { preferredMethod==null || it.code==preferredMethod }
    }
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
        val available=storeMethods(updated).filter{it.enabled && (preferredMethod==null || it.code==preferredMethod)}
        if(available.none{it.code==method})method=available.firstOrNull()?.code.orEmpty()
        balance=api.request("/api/wallet").optLong("available_cents")
        val a=api.request("/api/topups").optJSONArray("items")
        rows=if(a==null)emptyList()else(0 until a.length()).map{a.getJSONObject(it)}
    }
    fun perform(block:suspend()->Unit){if(busy)return;busy=true;note="";scope.launch{try{block()}catch(e:CancellationException){throw e}catch(e:Exception){note=commerceError(e)}finally{busy=false}}}
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->if(uri!=null)perform{val r=api.upload(context,uri,"receipt");receipt=r.getString("id");preview=r.getString("url");note="تم رفع الإيصال."}}
    LaunchedEffect(Unit){busy=true;try{reload()}catch(e:CancellationException){throw e}catch(e:Exception){note=commerceError(e)}finally{busy=false}}
    LaunchedEffect(quoteText, submittedId, showOrders) { scrollState.scrollTo(0) }

    val showSelection=!showOrders && quote==null && submittedId.isBlank()
    if(checkoutUrl.isNotBlank()){
        PaymentCheckoutScreen(url=checkoutUrl,provider=checkoutProvider,
            onClose={checkoutUrl=""},
            onCheck={checkoutUrl="";perform{
                val q=quote
                if(q!=null){
                    val rail=checkoutProvider
                    val route=if(rail=="stripe")"/api/stripe/status/"else"/api/nowpayments/status/"
                    val state=api.request(route+q.getString("id"))
                    if(state.optBoolean("credited")){
                        quoteText="";invoiceUrl="";reload()
                        note="تم تأكيد الدفع وإضافة الكوينز بنجاح."
                    }else{
                        note="عملية الدفع بانتظار تأكيد بوابة الدفع. لا يُضاف الرصيد قبل التحقق."
                    }
                }
            }}
        )
        return
    }
    Surface(Modifier.fillMaxSize(),color=CoinNavy,contentColor=Color.White){
        Box(Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF112D56),CoinNavy,Color(0xFF070E1C))))
            .safeDrawingPadding()){
            Canvas(Modifier.align(Alignment.TopCenter).fillMaxWidth().height(255.dp)){
                val stars=listOf(.04f to .22f,.19f to .07f,.29f to .53f,
                    .40f to .15f,.54f to .38f,.62f to .11f,.74f to .59f,
                    .83f to .25f,.94f to .48f,.17f to .73f,.48f to .81f)
                for((x,y) in stars)
                    drawCircle(Color(0xFFE2C387).copy(alpha=.42f),
                        radius=1.5.dp.toPx(),center=Offset(size.width*x,size.height*y))
            }
            Column(Modifier.fillMaxSize().imePadding().verticalScroll(scrollState)
                .padding(horizontal=18.dp)
                .padding(top=12.dp,bottom=if(showSelection)142.dp else 28.dp),
                verticalArrangement=Arrangement.spacedBy(14.dp)){
        Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
            TextButton(onClick=back,enabled=!busy){Text("رجوع")}
            Spacer(Modifier.weight(1f))
            Text("Relaxia",fontWeight=FontWeight.SemiBold,color=Color(0xFFA5B9D8))
        }
        Text(if(quote==null)"اختر باقة الشحن"else"إتمام الشحن",style=MaterialTheme.typography.headlineMedium,fontWeight=FontWeight.ExtraBold,color=Color.White)
        Surface(shape=RoundedCornerShape(50.dp),shadowElevation=2.dp,color=Color(0xFF263857),border=BorderStroke(1.dp,Color(0xFF7288AA))){
            Row(Modifier.fillMaxWidth().padding(horizontal=16.dp,vertical=10.dp),verticalAlignment=Alignment.CenterVertically){
                Icon(androidx.compose.material.icons.Icons.Rounded.Toll, null, Modifier.size(23.dp), tint=CoinGold)
                Spacer(Modifier.width(10.dp))
                Column{
                    Text("رصيدك الحالي",style=MaterialTheme.typography.bodySmall,color=Color(0xFFA5B9D8))
                    Text(if(balance==null)"—"else java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(balance)+" كوين",style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold,color=Color.White)
                }
            }
        }
        Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
            val chipColors=FilterChipDefaults.filterChipColors(
                containerColor=Color(0xFF21334F),
                labelColor=Color(0xFFCDDBEF),
                selectedContainerColor=Color(0xFF385A8A),
                selectedLabelColor=Color.White
            )
            FilterChip(!showOrders,{showOrders=false},label={Text("شراء كوينز")},colors=chipColors)
            FilterChip(showOrders,{showOrders=true},label={Text("طلباتي")},colors=chipColors)
        }
        if(!showOrders && submittedId.isBlank() && quote!=null) Text("أكمل عملية الدفع",style=MaterialTheme.typography.titleMedium,color=Color.White)
        if(!showSelection) Text("100 كوين = 1 USD كقيمة أساسية داخل التطبيق. يظهر المبلغ النهائي قبل الدفع. النقاط الترويجية منفصلة وغير قابلة للسحب.",style=MaterialTheme.typography.bodySmall,color=Color(0xFFA5B9D8))
        if(note.isNotBlank())Text(note,color=MaterialTheme.colorScheme.primary)
        if(busy)LuxuryLoadingStatus("جارٍ تحديث الطلب")
        if(submittedId.isNotBlank()) {
            PurchaseStatusCard(submittedId,submittedAmount,submittedStatus)
            OutlinedButton(onClick={perform{reload();rows.firstOrNull{it.optString("id")==submittedId}?.let{submittedStatus=it.optString("status")}}},enabled=!busy,modifier=Modifier.fillMaxWidth()){Text("تحديث حالة الطلب")}
            TextButton(onClick={submittedId="";showOrders=true}){Text("عرض جميع طلباتي")}
            if(submittedStatus!="pending")TextButton(onClick={submittedId="";showOrders=false}){Text("شراء رصيد جديد")}
        } else if(!showOrders && quote==null){
            if(packages.isEmpty()&&!busy)Text("لا تتوفر باقات شحن حاليًا. أعد تحديث المتجر لاحقًا.",color=Color(0xFFB2C3DB))
            val maxBonus=packages.maxOfOrNull{it.bonusPoints}?:0
            BoxWithConstraints(Modifier.fillMaxWidth()){
                val columns=if(maxWidth>=280.dp)3 else 2
                Column(verticalArrangement=Arrangement.spacedBy(9.dp)){
                    packages.chunked(columns).forEach{line->
                        Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(9.dp)){
                            line.forEach{option->
                                val badge=if(option.bonusPoints>0&&option.bonusPoints==maxBonus)"أكبر مكافأة" else null
                                Box(Modifier.weight(1f)){
                                    CoinPackage(
                                        coins=option.coins,bonus=option.bonusPoints,
                                        dollars=BigDecimal.valueOf(option.coins.toLong(),2).toPlainString(),
                                        selected=selected==option.coins,badge=badge
                                    ){selected=option.coins}
                                }
                            }
                            repeat(columns-line.size){Spacer(Modifier.weight(1f))}
                        }
                    }
                }
            }
            Text("اختر إحدى الباقات ثم اضغط اشحن الآن لعرض وسائل الدفع والسعر النهائي.",style=MaterialTheme.typography.bodySmall,color=Color(0xFFA5B9D8))
        }else if(!showOrders && quote!=null){
            Text("${quote.optInt("amount_cents")} كوين" + if(quote.optInt("bonus_cents")>0) " · +${quote.optInt("bonus_cents")} نقطة ترويجية" else "",fontSize=20.sp,fontWeight=FontWeight.Bold)
            Text(
                if(quoteSeconds>0L)"صلاحية عرض السعر: %02d:%02d".format(quoteSeconds/60,quoteSeconds%60)
                else "انتهت صلاحية عرض السعر. إن لم تدفع، اطلب عرضًا جديدًا. إن دفعت بالفعل، أكمل إرسال الإثبات للمراجعة.",
                style=MaterialTheme.typography.bodySmall,color=Color(0xFFB2C3DB)
            )
            Text("المطلوب: ${quote.optString("payment_amount")} ${quote.optString("payment_currency")}",style=MaterialTheme.typography.titleLarge)
            val rail=quote.optString("method")
            if(rail=="nowpayments"||rail=="stripe") {
                val viaStripe=rail=="stripe"
                Text(if(viaStripe)"الدفع بالفيزا أو ماستركارد"else"دفع آمن عبر NOWPayments",style=MaterialTheme.typography.titleLarge)
                Text(
                    if(viaStripe)"تُفتح صفحة الدفع الرسمية داخل Relaxia. يتم اعتماد الشحن فقط بعد إشعار الدفع الموقّع."
                    else "تختار العملة داخل صفحة الدفع الرسمية. لا تُضاف الكوينز قبل تحقق الخادم من العملية.",
                    style=MaterialTheme.typography.bodySmall
                )
                Button(onClick={perform{
                    val endpoint=if(viaStripe)"/api/stripe/checkout"else"/api/nowpayments/invoices"
                    val invoice=api.request(endpoint,"POST",JSONObject().put("quote_id",quote.getString("id")))
                    val link=invoice.getString(if(viaStripe)"checkout_url"else"invoice_url")
                    val parsed=Uri.parse(link)
                    val trusted=if(viaStripe)parsed.host=="checkout.stripe.com"
                        else parsed.host in listOf("nowpayments.io","www.nowpayments.io")
                    require(parsed.scheme=="https"&&trusted&&parsed.userInfo==null)
                    invoiceUrl=link
                    checkoutProvider=rail
                    checkoutUrl=link
                }},enabled=!busy,modifier=Modifier.fillMaxWidth().height(52.dp)){
                    Text(if(invoiceUrl.isBlank())"فتح بوابة الدفع"else"فتح صفحة الدفع مجددًا")
                }
                OutlinedButton(onClick={perform{
                    val route=if(viaStripe)"/api/stripe/status/"else"/api/nowpayments/status/"
                    val result=api.request(route+quote.getString("id"))
                    if(result.optBoolean("credited")){
                        quoteText="";invoiceUrl="";reload();note="تم تأكيد الدفع وإضافة الرصيد."
                    }else if(result.optBoolean("review_required")){
                        note="العملية قيد المراجعة المالية. لم نؤكد إضافة رصيد جديد."
                    }else note="الدفع بانتظار تأكيد البوابة. لا يمكن إضافة الكوينز قبل التحقق."
                }},enabled=!busy&&invoiceUrl.isNotBlank(),modifier=Modifier.fillMaxWidth()){
                    Text("التحقق من الدفع وتحديث الرصيد")
                }
            } else {
            Text(if(rail=="usdt")"USDT على BEP20 فقط"else"استلام عبر شام كاش",fontWeight=FontWeight.Bold)
            Surface(color=androidx.compose.ui.graphics.Color.White,shape=RoundedCornerShape(24.dp)) { ReferenceImage(quote.optString("qr_url"),Modifier.fillMaxWidth().height(256.dp).padding(16.dp),contentScale=androidx.compose.ui.layout.ContentScale.Fit) }
            Text(quote.optString("destination"),style=MaterialTheme.typography.bodyMedium.copy(textDirection=androidx.compose.ui.text.style.TextDirection.Ltr))
            OutlinedButton(onClick={(context.getSystemService(Context.CLIPBOARD_SERVICE)as ClipboardManager).setPrimaryClip(ClipData.newPlainText("Relaxia payment",quote.optString("destination")));note="تم نسخ بيانات الاستلام."}){Text("نسخ بيانات الاستلام")}
            if(rail=="shamcash"&&info.optString("shamcash_instructions").isNotBlank())Text(info.optString("shamcash_instructions"))
            OutlinedTextField(reference,{reference=it.trim()},label={Text(if(rail=="usdt")"رقم المعاملة TxID"else"رقم إشعار الدفع")},modifier=Modifier.fillMaxWidth())
            Text(if(rail=="usdt")"أدخل TxID كاملًا: 64 حرفًا أو رقمًا من إيصال التحويل."else"أدخل رقم إشعار التحويل كما يظهر في الإيصال.",style=MaterialTheme.typography.bodySmall)
            OutlinedButton(onClick={picker.launch("image/*")},enabled=!busy){Text(if(receipt.isBlank())"رفع صورة إثبات الدفع"else"استبدال الإيصال")}
            if(preview.isNotBlank())ReferenceImage(preview,Modifier.fillMaxWidth().height(150.dp))
            Text("الرصيد يضاف بعد تحقق المسؤول من وصول المبلغ فعليًا. صورة الإيصال وحدها لا تكفي.",fontSize=12.sp,color=Color(0xFFB2C3DB))
            Button(onClick={perform{val result=api.request("/api/topups","POST",JSONObject().put("quote_id",quote.getString("id")).put("transaction_hash",reference).put("receipt_id",receipt));submittedId=result.getString("id");submittedStatus=result.optString("status","pending");submittedAmount=quote.optLong("amount_cents");quoteText="";receipt="";reference="";preview="";showOrders=true;try{reload()}catch(e:CancellationException){throw e}catch(e:Exception){note="تم استلام الطلب. تعذّر تحديث القائمة؛ يمكنك تحديث الحالة لاحقًا."}}},enabled=!busy&&receipt.isNotBlank()&&validPaymentReference(rail,reference),modifier=Modifier.fillMaxWidth().height(52.dp)){Text(if(busy)"جارٍ تقديم الطلب…"else"تقديم طلب الشراء")}
            }
            TextButton(onClick={quoteText=""},enabled=!busy){Text("العودة إلى الباقات")}
        }
        if(showOrders && submittedId.isBlank()) {
            Text("طلبات الشراء",style=MaterialTheme.typography.titleLarge)
            OutlinedButton(onClick={perform{reload()}},enabled=!busy){Text("تحديث الطلبات")}
            if(rows.isEmpty()&&!busy)Text("لا توجد طلبات شراء حتى الآن.",color=Color(0xFFB2C3DB))
            rows.forEach { r ->
                PurchaseStatusCard(r.optString("id"),r.optLong("amount_cents"),r.optString("status"),r.optString("review_note"))
            }
        }
            } // scrolling content
            if(showSelection) {
                Column(
                    modifier=Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color(0x00101E38),Color(0xFF070E1C))))
                        .padding(horizontal=20.dp,vertical=12.dp),
                    horizontalAlignment=Alignment.CenterHorizontally,
                    verticalArrangement=Arrangement.spacedBy(8.dp)
                ) {
                    val canCharge=!busy&&packages.any{it.coins==selected}&&methods.any{it.enabled}
                    Surface(onClick={showPaymentSheet=true},
                        enabled=canCharge,
                        modifier=Modifier.fillMaxWidth().height(52.dp),
                        shape=RoundedCornerShape(50.dp),
                        color=Color(0xFF1E40AF)
                    ){
                        Box(Modifier.fillMaxSize().background(
                            Brush.horizontalGradient(listOf(Color(0xFF1E40AF),Color(0xFFF0BC71)))),
                            contentAlignment=Alignment.Center){
                            Text("اشحن الآن",color=Color.White,fontWeight=FontWeight.ExtraBold,fontSize=17.sp)
                        }
                    }
                    Text("100 كوين = 1 USD كقيمة أساسية داخل التطبيق. يظهر المبلغ النهائي قبل الدفع. النقاط الترويجية منفصلة وغير قابلة للسحب.",
                        fontSize=10.sp,lineHeight=13.sp,color=Color(0xFFB2C3DB),
                        modifier=Modifier.fillMaxWidth())
                }
            }
        } // safeDrawingPadding box
    }
    if(showPaymentSheet && !showOrders && quote==null && submittedId.isBlank()){
        ModalBottomSheet(onDismissRequest={showPaymentSheet=false},shape=RoundedCornerShape(topStart=32.dp,topEnd=32.dp)){
            Column(
                Modifier.fillMaxWidth().navigationBarsPadding().verticalScroll(rememberScrollState()).padding(horizontal=22.dp).padding(bottom=20.dp),
                verticalArrangement=Arrangement.spacedBy(13.dp)
            ){
                Text("اختر طريقة الدفع",style=MaterialTheme.typography.headlineSmall,fontWeight=FontWeight.Bold)
                val chosen=packages.firstOrNull{it.coins==selected}
                Surface(color=MaterialTheme.colorScheme.primaryContainer,shape=RoundedCornerShape(16.dp)){
                    Column(Modifier.fillMaxWidth().padding(16.dp),verticalArrangement=Arrangement.spacedBy(5.dp)){
                        Text("${java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(selected)} كوين",fontSize=21.sp,fontWeight=FontWeight.Bold)
                        Text("القيمة الأساسية: ${BigDecimal.valueOf(selected.toLong(),2).toPlainString()} USD",style=MaterialTheme.typography.bodyMedium)
                        if((chosen?.bonusPoints?:0)>0)Text("+${chosen?.bonusPoints} نقطة ترويجية إضافية",color=MaterialTheme.colorScheme.primary)
                    }
                }
                methods.forEach{option->
                    Surface(
                        onClick={method=option.code},enabled=option.enabled,
                        color=if(method==option.code&&option.enabled)MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
                        border=BorderStroke(1.dp,if(method==option.code&&option.enabled)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant),
                        shape=RoundedCornerShape(18.dp)
                    ){
                        Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)){
                            Text(option.symbol,fontSize=23.sp,color=MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f)){
                                Text(option.name,fontWeight=FontWeight.SemiBold)
                                Text(if(option.enabled)option.description else "غير مفعّل حاليًا",fontSize=12.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if(option.code=="stripe"){
                                Row(horizontalArrangement=Arrangement.spacedBy(4.dp),
                                    verticalAlignment=Alignment.CenterVertically){
                                    Surface(shape=RoundedCornerShape(6.dp),color=Color.White){
                                        Text("VISA",color=Color(0xFF163F85),fontSize=11.sp,
                                            fontWeight=FontWeight.ExtraBold,
                                            modifier=Modifier.padding(horizontal=5.dp,vertical=3.dp))
                                    }
                                    Surface(shape=RoundedCornerShape(6.dp),color=Color.White){
                                        Text("●●",color=Color(0xFFF18632),fontSize=14.sp,
                                            modifier=Modifier.padding(horizontal=5.dp,vertical=2.dp))
                                    }
                                }
                            }
                            RadioButton(selected=method==option.code&&option.enabled,onClick=null,enabled=option.enabled)
                        }
                    }
                }
                Text("لن يتم الخصم عند اختيار الوسيلة. سيعرض الخادم المبلغ النهائي وبيانات التحويل قبل الدفع.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(methods.none{it.enabled})Text("لا توجد وسيلة دفع مفعّلة حاليًا. يرجى التواصل مع الدعم.",color=MaterialTheme.colorScheme.error,style=MaterialTheme.typography.bodySmall)
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
            Text("$amount كوين",style=MaterialTheme.typography.titleMedium)
            Text("رقم الطلب: ${id.take(8)}",style=MaterialTheme.typography.bodySmall)
            Text(when(status){"approved"->"تم تأكيد الدفع وإضافة الرصيد إلى محفظتك.";"rejected"->reason.ifBlank{"راجع بيانات التحويل أو تواصل مع الدعم."};else->"طلبك قيد المراجعة. يمكنك متابعة حالته هنا؛ لا تُعد إرسال التحويل نفسه."},style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
