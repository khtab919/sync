package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal

@Composable
internal fun WalletBalanceCard(balanceCents: Long?, loading: Boolean, error: String, buy: () -> Unit, wallet: () -> Unit, retry: () -> Unit) {
    Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp), shadowElevation = 8.dp, color = Color.Transparent) {
        Column(Modifier.background(Brush.linearGradient(listOf(Color(0xFF563044), Color(0xFF2F1B2A)))).padding(22.dp), verticalArrangement = Arrangement.spacedBy(15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccountBalanceWallet, null, tint = Color(0xFFF0C1D2), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Text("بطاقة رصيدك", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("Relaxia", color = Color.White.copy(alpha = .65f), fontSize = 14.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text("الرصيد المتاح", color = Color.White.copy(alpha = .7f), fontSize = 13.sp)
                Text(if(balanceCents == null) "—" else BigDecimal.valueOf(balanceCents,2).toPlainString() + " USD", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Bold)
                if(loading) LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp), color = Color(0xFFE5A3BE))
                if(error.isNotBlank()) TextButton(onClick = retry, contentPadding = PaddingValues(0.dp)) { Text("تعذّر تحديث الرصيد · إعادة المحاولة", color = Color(0xFFF5CEDD), fontSize = 12.sp) }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(onClick = buy, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0BDD0), contentColor = Color(0xFF3D2030))) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(19.dp)); Spacer(Modifier.width(5.dp)); Text("شراء رصيد", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                OutlinedButton(onClick = wallet, modifier = Modifier.height(48.dp), shape = RoundedCornerShape(14.dp), border = BorderStroke(1.dp, Color.White.copy(alpha = .28f)), colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("المحفظة") }
            }
        }
    }
}
