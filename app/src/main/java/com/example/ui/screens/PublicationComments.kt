package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.data.api.LayaliApi
import org.json.JSONObject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun PublicationComments(api:LayaliApi,kind:String,id:Int){
    var open by remember(kind,id){mutableStateOf(false)}
    TextButton({open=true}){Text("التعليقات")}
    if(!open)return
    val scope=rememberCoroutineScope()
    val path="/api/publications/$kind/$id/comments"
    var text by remember{mutableStateOf("")}
    var rows by remember{mutableStateOf(emptyList<JSONObject>())}
    var busy by remember{mutableStateOf(false)}
    var error by remember{mutableStateOf("")}
    fun load(send:Boolean){
        if(busy)return
        busy=true;error=""
        scope.launch{
            try{
                val response=if(send)api.request(path,"POST",JSONObject().put("body",text.trim()))else api.request(path)
                val list=response.getJSONArray("items")
                rows=(0 until list.length()).map{list.getJSONObject(it)}
                if(send)text=""
            }catch(e:CancellationException){throw e}catch(e:Exception){error=if(e.message=="comments_closed")"التعليقات مغلقة" else "تعذّر تحديث التعليقات"}finally{busy=false}
        }
    }
    LaunchedEffect(path){load(false)}
    AlertDialog(onDismissRequest={open=false},title={Text("التعليقات")},text={Column{
        LazyColumn(Modifier.fillMaxWidth().heightIn(max=250.dp)){
            items(rows){row->Text(row.optString("name"),style=MaterialTheme.typography.labelMedium);Text(row.optString("body"))}
        }
        if(rows.isEmpty()&&!busy&&error.isBlank())Text("كن أول من يعلّق")
        if(error.isNotBlank())Text(error)
        OutlinedTextField(text,{text=it.take(500)},label={Text("اكتب تعليقًا…")})
    }},confirmButton={TextButton({load(true)},enabled=!busy&&text.isNotBlank()){Text("إرسال")}},
    dismissButton={TextButton({open=false}){Text("إغلاق")}})
}
