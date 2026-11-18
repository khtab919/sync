package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.api.LayaliApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/** Displays real server-issued creator items, never exposes originals to unpaid viewers. */
@Composable
internal fun CreatorContentCard(
    api:LayaliApi,item:JSONObject,mine:Boolean,
    onChanged:(JSONObject)->Unit,onDeleted:()->Unit,
    onPlayReel:(Int)->Unit,onOpenService:(JSONObject)->Unit
){
    val scope=rememberCoroutineScope()
    val type=item.optString("kind")
    val id=item.optInt("id")
    val label=when(type){"photo"->"صورة مدفوعة";"reel"->"ريلز";else->item.optString("category","خدمة")}
    val title=if(type=="service")item.optString("title")else item.optString("caption").ifBlank{label}
    val locked=item.optBoolean("locked")
    val image=when(type){
        "photo"->if(locked)item.optString("preview_url")else item.optString("media_url")
        "reel"->item.optString("preview_url")
        else->item.optString("image_url")
    }
    val endpoint=when(type){"photo"->"/api/paid-posts/"+id;"reel"->"/api/reels/"+id;else->"/api/services/"+id}
    var busy by remember(id,type){mutableStateOf(false)}
    var error by remember(id,type){mutableStateOf("")}
    var purchase by remember(id,type){mutableStateOf(false)}
    var deletion by remember(id,type){mutableStateOf(false)}
    var editing by remember(id,type){mutableStateOf(false)}
    var menu by remember(id,type){mutableStateOf(false)}
    var draft by remember(id,type){mutableStateOf(title)}
    var price by remember(id,type){mutableStateOf(item.optInt("price_cents").toString())}
    var key by remember(id,type){mutableStateOf(UUID.randomUUID().toString())}
    fun perform(block:suspend()->Unit){
        if(busy)return
        busy=true;error=""
        scope.launch{
            try{block()}catch(e:CancellationException){throw e}
            catch(e:Exception){error=e.message?:"تعذّر إتمام العملية"}
            finally{busy=false}
        }
    }
    Card(Modifier.fillMaxWidth(),shape=RoundedCornerShape(20.dp)){
        Column(Modifier.fillMaxWidth().padding(14.dp),verticalArrangement=Arrangement.spacedBy(9.dp)){
            Row(Modifier.fillMaxWidth(),verticalAlignment=Alignment.CenterVertically){
                Text(label,color=MaterialTheme.colorScheme.primary)
                Spacer(Modifier.weight(1f))
                if(locked){
                    Icon(Icons.Rounded.Lock,null,Modifier.size(16.dp))
                    Text(" "+item.optInt("price_cents")+" كوين")
                }
                if(mine)Box {
                    IconButton(onClick={menu=true}){Icon(Icons.Rounded.MoreVert,"إدارة المنشور")}
                    DropdownMenu(expanded=menu,onDismissRequest={menu=false}){
                        DropdownMenuItem(text={Text("تعديل")},onClick={menu=false;editing=true})
                        DropdownMenuItem(text={Text("حذف")},onClick={menu=false;deletion=true})
                    }
                }
            }
            Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.Bold)
            if(image.startsWith("https://")){
                Box(Modifier.fillMaxWidth().height(210.dp)){
                    ReferenceImage(image,Modifier.fillMaxSize())
                    if(locked)Surface(
                        color=Color(0xB20F2342),shape=RoundedCornerShape(16.dp),
                        modifier=Modifier.align(Alignment.Center)){
                        Text("محتوى مدفوع · معاينة مموّهة",color=Color.White,
                            modifier=Modifier.padding(14.dp))
                    }
                }
            }
            when{
                type=="service"->{
                    Text(item.optString("description"),style=MaterialTheme.typography.bodySmall)
                    Button(onClick={onOpenService(item)},modifier=Modifier.fillMaxWidth()){
                        Text("عرض الخدمة")
                    }
                }
                locked->Button(onClick={purchase=true},enabled=!busy,
                    modifier=Modifier.fillMaxWidth(),
                    colors=ButtonDefaults.buttonColors(containerColor=Color(0xFF0F2342))){
                    Text("فتح مقابل "+item.optInt("price_cents")+" كوين")
                }
                type=="reel"->OutlinedButton(onClick={onPlayReel(id)},
                    modifier=Modifier.fillMaxWidth()){Text("تشغيل الريلز")}
            }
            if(error.isNotBlank())Text(error,color=MaterialTheme.colorScheme.error)
            if(busy)LinearProgressIndicator(Modifier.fillMaxWidth())
        }
    }
    if(purchase)AlertDialog(onDismissRequest={purchase=false},
        title={Text("تأكيد الدفع")},
        text={Text("فتح المحتوى مقابل "+item.optInt("price_cents")+" كوين؟ سيتم الخصم مرة واحدة بعد موافقتك.")},
        confirmButton={TextButton(onClick={purchase=false;perform{
            val result=api.request("/api/content/"+type+"/"+id+"/unlock","POST",
                JSONObject().put("price_cents",item.optInt("price_cents"))
                    .put("request_key",key))
            onChanged(result.getJSONObject("item").put("kind",type))
        }}){Text("تأكيد الشراء")}},
        dismissButton={TextButton(onClick={purchase=false}){Text("إلغاء")}})
    if(deletion)AlertDialog(onDismissRequest={deletion=false},
        title={Text("حذف المنشور؟")},
        text={Text("سيتوقف ظهور المنشور للزوار. المدفوعات السابقة تحتاج مراجعة مستقلة.")},
        confirmButton={TextButton(onClick={deletion=false;perform{
            api.request(endpoint,"DELETE")
            onDeleted()
        }}){Text("حذف")}},
        dismissButton={TextButton(onClick={deletion=false}){Text("إلغاء")}})
    if(editing)AlertDialog(onDismissRequest={editing=false},
        title={Text("تعديل المنشور")},
        text={Column(verticalArrangement=Arrangement.spacedBy(8.dp)){
            OutlinedTextField(value=draft,onValueChange={draft=it.take(500)},
                label={Text("العنوان أو الوصف")})
            OutlinedTextField(value=price,
                onValueChange={price=it.filter(Char::isDigit).take(7)},
                label={Text("السعر (كوين)")})
        }},
        confirmButton={TextButton(
            enabled=!busy&&price.toIntOrNull() in (if(type=="reel")0..1000000 else 1..1000000),
            onClick={editing=false;perform{
                val payload=JSONObject().put(if(type=="service")"title"else"caption",draft.trim())
                    .put("price_cents",price.toInt())
                val result=api.request(endpoint,"PATCH",payload)
                val updated=when(type){
                    "photo"->result.getJSONObject("item").put("kind","photo")
                    "reel"->JSONObject(item.toString()).put("caption",draft.trim())
                        .put("price_cents",price.toInt())
                    else->result.put("kind","service")
                }
                onChanged(updated)
            }}
        ){Text("حفظ")}},
        dismissButton={TextButton(onClick={editing=false}){Text("إلغاء")}})
}
