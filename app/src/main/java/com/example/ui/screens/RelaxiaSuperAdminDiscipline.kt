package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.api.LayaliApi
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import org.json.JSONObject

/** Server-permission-gated Super Admin dashboard; no direct Firestore writes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RelaxiaSuperAdminDiscipline(api:LayaliApi) {
    val scope=rememberCoroutineScope()
    var status by remember { mutableStateOf("") }
    var target by remember { mutableStateOf("") }
    var reason by remember { mutableStateOf("") }
    var duration by remember { mutableIntStateOf(24) }
    var customHours by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var revision by remember { mutableIntStateOf(0) }
    var entries by remember { mutableStateOf<List<JSONObject>>(emptyList()) }
    fun submit(action:String,userId:Int){
        if(busy || userId<=0 || reason.trim().length<3)return
        busy=true;status=""
        scope.launch{
            try{
                val hours=if(duration==0)customHours.toIntOrNull()?:0 else duration
                val p=JSONObject().put("action",action).put("user_id",userId)
                    .put("reason",reason.trim())
                if(action=="suspend")p.put("hours",hours)
                api.request("/api/admin/control/discipline","POST",p)
                status="تم تنفيذ القرار على الخادم وتسجيله للمراجعة."
                revision++
            }catch(e:CancellationException){throw e}
            catch(e:Exception){status="تعذر التنفيذ: ${e.message}. لم تتغير الصلاحيات من هذا الطلب."}
            finally{busy=false}
        }
    }
    LaunchedEffect(revision){
        try{
            val rows=api.request("/api/admin/control/discipline").optJSONArray("items")
            entries=if(rows==null)emptyList()else(0 until rows.length()).mapNotNull{rows.optJSONObject(it)}
        }catch(e:CancellationException){throw e}
        catch(e:Exception){status="غير مصرح بعرض عقوبات الحسابات أو تعذر الاتصال."}
    }
    Column(Modifier.fillMaxWidth().padding(14.dp),
        verticalArrangement=Arrangement.spacedBy(13.dp)){
        Text("العقوبات والإشراف العام",style=MaterialTheme.typography.headlineSmall,
            fontWeight=FontWeight.Bold)
        Text("تطبق العقوبات على الحساب والجلسات والبث والمحفظة من الخادم. جميع الإجراءات تتطلب سببًا وتُسجّل في سجل التدقيق.",
            style=MaterialTheme.typography.bodySmall,
            color=MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(value=target,onValueChange={target=it.filter(Char::isDigit).take(10)},
            label={Text("الرقم الداخلي للحساب")},modifier=Modifier.fillMaxWidth(),
            singleLine=true)
        OutlinedTextField(value=reason,onValueChange={reason=it.take(500)},
            label={Text("سبب القرار الإداري")},modifier=Modifier.fillMaxWidth(),
            minLines=2)
        Text("مدة التعليق المؤقت",fontWeight=FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement=Arrangement.spacedBy(7.dp)){
            listOf(24 to "24 ساعة",72 to "3 أيام",168 to "7 أيام",720 to "30 يوم",0 to "مخصص").forEach{(hours,label)->
                FilterChip(selected=duration==hours,onClick={duration=hours},label={Text(label)})
            }
        }
        if(duration==0) OutlinedTextField(value=customHours,
            onValueChange={customHours=it.filter(Char::isDigit).take(5)},
            label={Text("المدة بالساعات (1 إلى 8760)")},singleLine=true,
            modifier=Modifier.fillMaxWidth())
        Row(horizontalArrangement=Arrangement.spacedBy(9.dp)){
            Button(onClick={submit("suspend",target.toIntOrNull()?:0)},
                enabled=!busy&&reason.trim().length>=3&&(duration!=0||(customHours.toIntOrNull()?:0) in 1..8760)){
                Text("تعليق الحساب")
            }
            OutlinedButton(onClick={submit("ban_permanent",target.toIntOrNull()?:0)},
                enabled=!busy&&reason.trim().length>=3){
                Text("حظر دائم")
            }
        }
        if(status.isNotBlank())Text(status,
            color=if(status.startsWith("تم"))Color(0xFF16875B) else MaterialTheme.colorScheme.error)
        HorizontalDivider()
        Text("الحسابات الموقوفة والمحظورة",style=MaterialTheme.typography.titleMedium,
            fontWeight=FontWeight.Bold)
        if(entries.isEmpty())Text("لا توجد عقوبات معلقة ضمن آخر النتائج.")
        entries.take(100).forEach{row->
            ElevatedCard(Modifier.fillMaxWidth()){
                Column(Modifier.padding(14.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                    Text(row.optString("username")+" · #"+row.optString("public_id"),
                        fontWeight=FontWeight.Bold)
                    val st=row.optString("status")
                    Text(if(st=="banned_permanent")"حظر دائم" else if(st=="suspended")"تعليق مؤقت" else "نشط")
                    if(row.has("suspended_until")&&!row.isNull("suspended_until"))
                        Text("النهاية: "+row.optString("suspended_until"),
                            style=MaterialTheme.typography.bodySmall)
                    Text("السبب: "+row.optString("reason"),
                        style=MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick={submit("restore",row.optInt("user_id"))},
                        enabled=!busy&&reason.trim().length>=3){
                        Text("فك الحظر/التعليق")
                    }
                }
            }
        }
        TextButton(onClick={revision++},enabled=!busy){Text("تحديث قائمة العقوبات")}
    }
}
