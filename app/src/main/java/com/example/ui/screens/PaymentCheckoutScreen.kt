package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.SslErrorHandler
import android.net.http.SslError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp

/** Only HTTPS hosted provider checkout is displayed. No card credentials enter our app. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
internal fun PaymentCheckoutScreen(
    url:String,provider:String,
    onClose:()->Unit,onCheck:()->Unit
){
    val uri=remember(url){Uri.parse(url)}
    val firstHost=uri.host.orEmpty().lowercase()
    val trusted=uri.scheme=="https" && uri.userInfo==null &&
        (if(provider=="stripe")firstHost=="checkout.stripe.com"
            else firstHost=="nowpayments.io" || firstHost=="www.nowpayments.io")
    var shownHost by remember(url){mutableStateOf(firstHost)}
    var loading by remember(url){mutableStateOf(true)}
    var error by remember(url){mutableStateOf("")}
    BackHandler { onClose() }
    Surface(Modifier.fillMaxSize(),color=Color(0xFF0B162C),contentColor=Color.White){
        Column(Modifier.fillMaxSize().safeDrawingPadding()){
            Row(Modifier.fillMaxWidth().padding(14.dp),verticalAlignment=Alignment.CenterVertically){
                IconButton(onClick=onClose){Icon(Icons.Rounded.ArrowBack,"رجوع")}
                Column(Modifier.weight(1f),horizontalAlignment=Alignment.CenterHorizontally){
                    Text("RELAXIA · الدفع الآمن",style=MaterialTheme.typography.titleMedium)
                    Row(verticalAlignment=Alignment.CenterVertically){
                        Icon(Icons.Rounded.Lock,null,Modifier.size(13.dp),tint=Color(0xFFACD4FD))
                        Spacer(Modifier.width(4.dp))
                        Text(shownHost,maxLines=1,style=MaterialTheme.typography.labelSmall,
                            color=Color(0xFFB9CDED))
                    }
                }
                TextButton(onClick=onCheck){Text("تحقق",color=Color.White)}
            }
            if(!trusted){
                Text("تعذّر التحقق من عنوان بوابة الدفع.",Modifier.padding(18.dp),
                    color=MaterialTheme.colorScheme.error)
            } else {
                if(loading)LinearProgressIndicator(Modifier.fillMaxWidth())
                AndroidView(
                    factory={context->
                        WebView(context).apply{
                            settings.javaScriptEnabled=true
                            settings.domStorageEnabled=true
                            settings.allowFileAccess=false
                            settings.allowContentAccess=false
                            settings.javaScriptCanOpenWindowsAutomatically=false
                            settings.setSupportMultipleWindows(false)
                            if(android.os.Build.VERSION.SDK_INT>=26)
                                settings.safeBrowsingEnabled=true
                            webViewClient=object:WebViewClient(){
                                override fun shouldOverrideUrlLoading(v:WebView?,request:WebResourceRequest):Boolean{
                                    val target=request.url
                                    if(target.scheme!="https")return true
                                    shownHost=target.host.orEmpty()
                                    if(target.path?.startsWith("/api/stripe/return")==true ||
                                        target.path?.startsWith("/api/nowpayments/return")==true){
                                        onCheck()
                                        return true
                                    }
                                    return false
                                }
                                override fun onPageStarted(v:WebView?,link:String?,favicon:Bitmap?){
                                    loading=true
                                    shownHost=Uri.parse(link.orEmpty()).host.orEmpty()
                                }
                                override fun onPageFinished(v:WebView?,link:String?){
                                    loading=false
                                }
                                override fun onReceivedSslError(v:WebView?,
                                    handler:SslErrorHandler?,ssl:SslError?){
                                    handler?.cancel()
                                    error="اتصال الدفع غير آمن. تم إيقاف الصفحة."
                                }
                            }
                            loadUrl(url)
                        }
                    },
                    modifier=Modifier.fillMaxWidth().weight(1f)
                )
            }
            if(error.isNotBlank())Text(error,Modifier.padding(10.dp),color=Color(0xFFFFD2D2))
            Button(onClick=onCheck,modifier=Modifier.fillMaxWidth().height(54.dp)
                .padding(horizontal=15.dp,vertical=3.dp),
                colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF1D4F92)),
                shape=RoundedCornerShape(16.dp)){
                Text("التحقق من الدفع وتحديث الرصيد")
            }
            Text("يُضاف الرصيد بعد تأكيد بوابة الدفع على الخادم فقط.",
                modifier=Modifier.fillMaxWidth().padding(10.dp),
                color=Color(0xFFACBFDB),style=MaterialTheme.typography.bodySmall)
        }
    }
}
