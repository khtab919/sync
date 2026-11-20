package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Payments
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.api.LayaliApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.math.BigDecimal

private val CashNavy = Color(0xFF0B1528)
private val CashRoyal = Color(0xFF1A4789)
private val CashGold = Color(0xFFE5B84B)

@Composable
internal fun RelaxiaTopupMethodsScreen(api:LayaliApi,back:()->Unit){
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFFACCEFF),
            onPrimary = Color(0xFF082449),
            secondaryContainer = Color(0xFF264F82),
            onSecondaryContainer = Color.White,
            surface = Color(0xFF13223D),
            onSurface = Color.White,
            onSurfaceVariant = Color(0xFFCCD9EC),
            outline = Color(0xFF8DA4C2),
            background = Color(0xFF0B1528),
            onBackground = Color.White
        ),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes
    ) {
        PaymentMethodsContent(api, back)
    }
}

@Composable
private fun PaymentMethodsContent(api: LayaliApi, back: () -> Unit) {
    var selected by rememberSaveable { mutableStateOf("choose") }
    var cashAvailable by remember { mutableStateOf(false) }
    var cryptoAvailable by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(selected,retry){
        if(selected!="choose")return@LaunchedEffect
        loading=true
        try {
            val info=api.request("/api/payment-info")
            cashAvailable=info.optBoolean("syriatel_cash_enabled")
            cryptoAvailable=info.optBoolean("nowpayments_enabled")
            loadError=false
        } catch(e:CancellationException){throw e}
        catch(_:Exception){loadError=true}
        finally{loading=false}
    }
    when(selected){
        "trc20" -> NativeUsdtTopupScreen(api){selected="choose"}
        "syriatel" -> SyriatelCashTopupScreen(api){selected="choose"}
        else -> Surface(Modifier.fillMaxSize(),color=CashNavy,contentColor=Color.White){
            Column(Modifier.fillMaxSize().safeDrawingPadding().verticalScroll(rememberScrollState()).padding(9.dp),
                verticalArrangement=Arrangement.spacedBy(9.dp)){
                TextButton(onClick=back){ Icon(Icons.Rounded.ArrowForward,null); Text("رجوع") }
                Text("شحن الكوينز",style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                Text("اختر طريقة الدفع داخل Relaxia",color=Color(0xFFC4D6EE))
                if(loading) CircularProgressIndicator()
                else if(loadError) TextButton(onClick={retry++}){Text("تعذّر تحميل طرق الشحن · إعادة المحاولة")}
                else if(!cashAvailable&&!cryptoAvailable) Text("لا توجد وسيلة شحن مفعّلة حالياً.")
                if(!loading&&!loadError&&cryptoAvailable) CashMethodCard(
                    title="USDT · TRC20",
                    detail="شبكة Tron فقط · التحقق من بوابة الدفع",
                    onClick={selected="trc20"})
                if(!loading&&!loadError&&cashAvailable) CashMethodCard(
                    title="سيريتيل كاش",
                    detail="تحويل بالليرة السورية الجديدة",
                    onClick={selected="syriatel"})
                Text("لا تُضاف الكوينز بعد إرسال إيصال فقط. يجب تأكيد وصول الدفعة الفعلية.",
                    color=Color(0xFFBFD0EA),style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CashMethodCard(title:String,detail:String,onClick:()->Unit){
    Card(onClick=onClick,modifier=Modifier.fillMaxWidth().heightIn(min=52.dp),
        shape=RoundedCornerShape(25.dp),
        colors=CardDefaults.cardColors(containerColor=CashRoyal)){
        Row(Modifier.fillMaxWidth().padding(9.dp),verticalAlignment=Alignment.CenterVertically,
            horizontalArrangement=Arrangement.spacedBy(12.dp)){
            Icon(Icons.Rounded.AccountBalanceWallet,null,tint=CashGold,modifier=Modifier.size(32.dp))
            Column(Modifier.weight(1f)){
                Text(title,color=Color.White,fontWeight=FontWeight.Bold,
                    style=MaterialTheme.typography.titleMedium)
                Text(detail,color=Color(0xFFE1EAF6),style=MaterialTheme.typography.bodySmall)
            }
            Icon(Icons.Rounded.ArrowForward,null,tint=CashGold)
        }
    }
}

@Composable
private fun SyriatelCashTopupScreen(api:LayaliApi,back:()->Unit){
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var enabled by remember { mutableStateOf(false) }
    var phone by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var usd by rememberSaveable { mutableIntStateOf(10) }
    var quoteText by rememberSaveable { mutableStateOf("") }
    var receiptId by rememberSaveable { mutableStateOf("") }
    var reference by rememberSaveable { mutableStateOf("") }
    var history by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    var historyVisible by remember { mutableStateOf(false) }
    val quote=remember(quoteText){runCatching{JSONObject(quoteText)}.getOrNull()}
    val current=quote?.optInt("amount_cents")==usd*100

    fun action(block:suspend()->Unit){
        if(busy)return
        busy=true;note=""
        scope.launch {
            try{block()}
            catch(e:CancellationException){throw e}
            catch(e:Exception){note=commerceError(e)}
            finally{busy=false}
        }
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
        if(uri!=null) action{
            val result=api.upload(context,uri,"receipt")
            receiptId=result.optString("id")
            note="تم رفع صورة الإيصال. لن يُعتمد الرصيد حتى يتحقق المسؤول من وصول المبلغ."
        }
    }
    LaunchedEffect(Unit){
        try{
            val info=api.request("/api/payment-info")
            enabled=info.optBoolean("syriatel_cash_enabled") && info.optString("syriatel_cash_number").matches(Regex("09[0-9]{8}"))
            phone=info.optString("syriatel_cash_number","")
        }catch(e:CancellationException){throw e}
        catch(_:Exception){note="تعذر التحقق من إعدادات سيريتيل كاش. حاول مجددًا."}
    }

    Surface(Modifier.fillMaxSize(),color=CashNavy,contentColor=Color.White){
        Column(Modifier.fillMaxSize().safeDrawingPadding()
            .verticalScroll(rememberScrollState()).padding(9.dp),
            verticalArrangement=Arrangement.spacedBy(9.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                TextButton(back){Icon(Icons.Rounded.ArrowForward,null);Text("طرق الدفع")}
                Spacer(Modifier.weight(1f))
                Icon(Icons.Rounded.Payments,null,tint=CashGold)
            }
            Text("سيريتيل كاش",style=MaterialTheme.typography.titleLarge,
                fontWeight=FontWeight.ExtraBold,color=CashGold)
            Text("الدفع بالليرة السورية الجديدة",
                color=Color(0xFFE0ECFA))
            Surface(color=CashRoyal,shape=RoundedCornerShape(23.dp)){
                Column(Modifier.fillMaxWidth().padding(9.dp),
                    verticalArrangement=Arrangement.spacedBy(9.dp)){
                    if(enabled && phone.isNotBlank()) {
                    Text("رقم الاستلام",color=Color(0xFFDCEAF8))
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Text(phone,modifier=Modifier.weight(1f),color=CashGold,
                            fontWeight=FontWeight.Bold,style=MaterialTheme.typography.headlineSmall)
                        IconButton(onClick={
                            (context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager)
                                .setPrimaryClip(ClipData.newPlainText("سيريتيل كاش",phone))
                            note="تم نسخ الرقم"
                        }){Icon(Icons.Rounded.ContentCopy,"نسخ رقم سيريتيل كاش",tint=Color.White)}
                    }
                    }
                    Text(if(enabled)"جاهز لإنشاء فاتورة تحويل"
                         else "هذه الوسيلة تنتظر تفعيل المسؤول وتحديد سعر الصرف؛ لا تحول الآن.",
                        color=if(enabled)Color(0xFFB6EECC) else Color(0xFFFFE1A1),
                        style=MaterialTheme.typography.bodySmall)
                }
            }
            Text("اختر قيمة الرصيد بالدولار الداخلي",fontWeight=FontWeight.SemiBold)
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal=4.dp),horizontalArrangement=Arrangement.spacedBy(7.dp)){
                listOf(5,10,25,50).forEach{value->
                    FilterChip(selected=usd==value,onClick={
                        if(usd!=value){usd=value;quoteText="";receiptId="";reference=""}
                    },label={Text("$value")},modifier=Modifier.widthIn(min=52.dp))
                }
            }
            Button(onClick={action{
                val result=api.request("/api/topup-quotes","POST",
                    JSONObject().put("method","syriatelcash").put("amount_cents",usd*100))
                require(result.optString("payment_currency")=="SYP" &&
                    result.optString("destination")==phone){"invalid_payment_method"}
                quoteText=result.toString()
                receiptId="";reference=""
                note="راجع المبلغ بالليرة السورية ثم حوّله إلى رقم الاستلام."
            }},enabled=enabled&&!busy,modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
                colors=ButtonDefaults.buttonColors(containerColor=CashRoyal,contentColor=Color.White)){
                Text(if(busy)"جارٍ إنشاء الطلب"else"إنشاء فاتورة $usd دولار رصيد")
            }
            if(quote!=null&&current){
                Surface(color=Color(0xFF193E73),shape=RoundedCornerShape(21.dp)){
                    Column(Modifier.fillMaxWidth().padding(9.dp),
                        verticalArrangement=Arrangement.spacedBy(10.dp)){
                        Text("تفاصيل الدفع المطلوبة",color=CashGold,
                            style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
                        Text("المبلغ: ${quote.optString("payment_amount")} ليرة سورية جديدة",
                            style=MaterialTheme.typography.titleLarge)
                        Text("المستلم: ${quote.optString("destination")}")
                        Text("صلاحية السعر حتى: ${quote.optString("expires_at")}",
                            style=MaterialTheme.typography.bodySmall)
                        Text("ادفع هذا المبلغ فقط بعد التأكد من تفاصيل الفاتورة. ثم أدخل رقم التحويل وأرفق الإثبات.",
                            color=Color(0xFFE0EAF7),style=MaterialTheme.typography.bodySmall)
                    }
                }
                OutlinedTextField(value=reference,onValueChange={
                    reference=it.filter{c->c.isLetterOrDigit()||c=='_'||c=='-'}.take(80)
                },label={Text("رقم مرجع العملية من سيريتيل كاش")},
                    singleLine=true,modifier=Modifier.fillMaxWidth())
                OutlinedButton(onClick={picker.launch("image/*")},enabled=!busy,
                    modifier=Modifier.fillMaxWidth()){
                    Icon(Icons.Rounded.ReceiptLong,null)
                    Spacer(Modifier.width(8.dp))
                    Text(if(receiptId.isBlank())"إرفاق صورة إيصال الدفع"else"تغيير صورة الإيصال")
                }
                Button(onClick={action{
                    val result=api.request("/api/topups","POST",JSONObject()
                        .put("quote_id",quote.optString("id"))
                        .put("transaction_hash",reference.trim())
                        .put("receipt_id",receiptId))
                    quoteText="";receiptId="";reference=""
                    note="تم إرسال الطلب #${result.optString("id")}. ينتظر فحص وصول الدفع واعتماد الإدارة."
                }},enabled=!busy&&receiptId.isNotBlank()&&
                    Regex("^[A-Za-z0-9_-]{4,80}$").matches(reference),
                    modifier=Modifier.fillMaxWidth().heightIn(min=48.dp),
                    colors=ButtonDefaults.buttonColors(containerColor=CashGold,
                        contentColor=CashNavy)){
                    Icon(Icons.Rounded.CheckCircle,null)
                    Spacer(Modifier.width(9.dp))
                    Text("إرسال الإثبات للمراجعة")
                }
            }
            if(note.isNotBlank()) Text(note,color=Color(0xFFF5D393))
            OutlinedButton(onClick={action{
                val list=api.request("/api/topups").optJSONArray("items")
                history=(0 until (list?.length()?:0)).mapNotNull{list?.optJSONObject(it)}
                    .filter{it.optString("method")=="syriatelcash"}
                historyVisible=true
            }},modifier=Modifier.fillMaxWidth(),enabled=!busy){
                Text("متابعة طلبات سيريتيل كاش")
            }
            if(historyVisible) history.forEach{row->
                Text("رقم ${row.optString("id").take(8)} · "+
                     when(row.optString("status")){
                         "approved"->"تم اعتماد الرصيد"
                         "rejected"->"مرفوض"
                         else->"قيد مراجعة الإدارة"
                     },color=Color(0xFFE4EEFD))
            }
            Text("لا يقوم التطبيق بخصم الأموال من هاتفك. التحويل يتم في خدمة سيريتيل كاش خارج التطبيق، ويُضاف الرصيد بعد تحقق الإدارة من وصوله.",
                color=Color(0xFFB6CAE5),style=MaterialTheme.typography.bodySmall,
                textAlign=TextAlign.Start)
        }
    }
}
