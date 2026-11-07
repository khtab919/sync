package com.example.ui.auth

import android.app.DatePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import java.util.Calendar

/** A self-declaration gate, not identity verification or a backend authorization check. */
@Composable
fun AdultWelcomeScreen(onContinue: () -> Unit) {
    val context = LocalContext.current
    var birthYear by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthMonth by rememberSaveable { mutableStateOf<Int?>(null) }
    var birthDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var accepted by rememberSaveable { mutableStateOf(false) }
    val adult = birthYear?.let { year ->
        isAdultOnDate(year, birthMonth ?: 0, birthDay ?: 1, Calendar.getInstance())
    } ?: false

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.safeDrawingPadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 32.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Text("Layali", style = MaterialTheme.typography.headlineLarge)
                Text("مساحة للبالغين فقط · 18+", style = MaterialTheme.typography.titleLarge)
                Text("تواصل، ريلز وخدمات المذيعات في مساحة تحترم الخصوصية والحدود.",
                    style = MaterialTheme.typography.bodyLarge)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("حدود واضحة للجميع", style = MaterialTheme.typography.titleMedium)
                        Text("يُمنع العري والمحتوى الجنسي الصريح والخدمات الجنسية، وأي محتوى يتضمن قاصرين. يُمنع التحرش والتهديد ومشاركة صور الآخرين أو معلوماتهم دون موافقتهم.",
                            style = MaterialTheme.typography.bodyLarge)
                        Text("الدفع مقابل خدمة لا يمنح حق تجاوز حدود صاحبها أو نشر محتواه دون إذنه.",
                            style = MaterialTheme.typography.bodyLarge)
                    }
                }
                OutlinedButton(onClick = {
                    val today = Calendar.getInstance()
                    DatePickerDialog(context, { _, year, month, day ->
                        birthYear = year
                        birthMonth = month
                        birthDay = day
                    }, birthYear ?: today.get(Calendar.YEAR),
                        birthMonth ?: today.get(Calendar.MONTH),
                        birthDay ?: today.get(Calendar.DAY_OF_MONTH)).apply {
                        datePicker.maxDate = today.timeInMillis
                    }.show()
                }, modifier = Modifier.fillMaxWidth()) {
                    Text(birthYear?.let { "تاريخ الميلاد: $birthDay / ${birthMonth!! + 1} / $it" }
                        ?: "اختر تاريخ ميلادك")
                }
                if (birthYear != null && !adult) {
                    Text("ليالي متاح لمن أتمّوا 18 عامًا فقط.", color = MaterialTheme.colorScheme.error)
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = accepted, onCheckedChange = { accepted = it })
                    Text("أؤكد صحة تاريخ ميلادي وأوافق على ضوابط المحتوى أعلاه.",
                        modifier = Modifier.weight(1f).padding(top = 12.dp),
                        style = MaterialTheme.typography.bodyLarge)
                }
                Text("هذا إقرار بالعمر وليس توثيقًا للهوية. لا يُحفظ تاريخ الميلاد من هذه الشاشة.",
                    style = MaterialTheme.typography.bodyMedium)
                Button(onClick = onContinue, enabled = adult && accepted,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Text("المتابعة إلى ليالي")
                }
            }
        }
    }
}

internal fun isAdultOnDate(year: Int, month: Int, day: Int, today: Calendar): Boolean {
    val cutoff = (today.clone() as Calendar).apply { add(Calendar.YEAR, -18) }
    val birth = Calendar.getInstance().apply {
        clear()
        isLenient = false
        set(year, month, day)
    }
    return try {
        birth.timeInMillis <= cutoff.timeInMillis
    } catch (_: IllegalArgumentException) {
        false
    }
}
