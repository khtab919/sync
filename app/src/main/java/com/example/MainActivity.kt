package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.screens.ServerAppScreen
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                if (BuildConfig.LAYALI_API_URL.isNotBlank()) {
                    ServerAppScreen(BuildConfig.LAYALI_API_URL)
                } else {
                    Surface(Modifier.fillMaxSize()) {
                        Column(Modifier.safeDrawingPadding().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text("Relaxia", style = MaterialTheme.typography.headlineLarge)
                            Text("نسخة إعداد الخادم", style = MaterialTheme.typography.titleLarge)
                            Text("لم يُربط عنوان الخادم بهذه النسخة بعد. يلزم تثبيت النسخة المربوطة بخادم Relaxia بعد اكتمال النشر.")
                        }
                    }
                }
            }
        }
    }
}
