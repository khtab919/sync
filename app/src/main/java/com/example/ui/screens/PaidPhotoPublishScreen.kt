package com.example.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.api.LayaliApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Photo stays server-private; viewers receive only a downscaled blurred preview. */
@Composable
internal fun PaidPhotoPublishScreen(api:LayaliApi,back:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    var caption by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var media by remember { mutableStateOf(JSONObject()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val navy=Color(0xFF0F2342)
    fun perform(block:suspend()->Unit){
        if(busy)return
        busy=true;error=""
        scope.launch{
            try{block()}catch(e:CancellationException){throw e}
            catch(e:Exception){error=e.message?:"تعذّرت العملية، أعد المحاولة."}
            finally{busy=false}
        }
    }
    val picker=rememberLauncherForActivityResult(ActivityResultContracts.GetContent()){uri->
        if(uri!=null)perform{
            media=api.upload(context,uri,"paid_photo")
        }
    }
    Surface(Modifier.fillMaxSize(),color=MaterialTheme.colorScheme.background){
        Column(Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(horizontal=18.dp)){
            Box(Modifier.fillMaxWidth().height(60.dp),contentAlignment=Alignment.Center){
                Text("نشر صورة مدفوعة",style=MaterialTheme.typography.titleLarge,
                    fontWeight=FontWeight.Bold)
                IconButton(onClick=back,modifier=Modifier.align(Alignment.CenterEnd)){
                    Icon(Icons.Rounded.ArrowBack,"رجوع")
                }
            }
            Column(Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement=Arrangement.spacedBy(14.dp)){
                Text("يشاهد الزوار معاينة مموّهة فقط، ولا تظهر الصورة الأصلية إلا بعد دفع الكوينز.",
                    style=MaterialTheme.typography.bodyMedium,
                    color=MaterialTheme.colorScheme.onSurfaceVariant)
                Surface(onClick={picker.launch("image/*")},enabled=!busy,
                    shape=RoundedCornerShape(22.dp),
                    color=Color(0xFFE8EEF5),border=BorderStroke(1.dp,Color(0xFF8498B5)),
                    modifier=Modifier.fillMaxWidth().height(185.dp)){
                    Column(horizontalAlignment=Alignment.CenterHorizontally,
                        verticalArrangement=Arrangement.Center,
                        modifier=Modifier.fillMaxSize()){
                        Icon(Icons.Rounded.PhotoCamera,null,Modifier.size(40.dp),tint=navy)
                        Spacer(Modifier.height(12.dp))
                        Text(if(media.has("id"))"تم رفع الصورة · اضغط للاستبدال"
                            else"اختر صورة من الهاتف · حتى 8MB",color=navy,
                            fontWeight=FontWeight.SemiBold)
                    }
                }
                OutlinedTextField(value=caption,onValueChange={caption=it.take(500)},
                    label={Text("وصف الصورة")},modifier=Modifier.fillMaxWidth(),
                    minLines=2,maxLines=4,enabled=!busy,shape=RoundedCornerShape(18.dp))
                OutlinedTextField(value=price,
                    onValueChange={price=it.filter(Char::isDigit).take(7)},
                    label={Text("سعر فتح الصورة (كوين)")},
                    modifier=Modifier.fillMaxWidth(),singleLine=true,enabled=!busy,
                    shape=RoundedCornerShape(18.dp))
                if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
                if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
            }
            Button(onClick={perform{
                api.request("/api/paid-posts","POST",JSONObject()
                    .put("media_id",media.getString("id"))
                    .put("caption",caption.trim())
                    .put("price_cents",price.toInt()))
                back()
            }},enabled=!busy&&media.has("id")&&price.toIntOrNull() in 1..1000000,
                modifier=Modifier.fillMaxWidth().height(52.dp).padding(bottom=2.dp),
                shape=RoundedCornerShape(16.dp),
                colors=ButtonDefaults.buttonColors(containerColor=navy)){
                Icon(Icons.Rounded.Lock,null)
                Spacer(Modifier.width(8.dp))
                Text("نشر الآن",fontWeight=FontWeight.Bold)
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
