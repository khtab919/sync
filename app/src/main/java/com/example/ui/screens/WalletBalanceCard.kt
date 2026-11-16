package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun WalletSummary(balanceCents: Long?, loading: Boolean, error: String, buy: () -> Unit, wallet: () -> Unit, retry: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical=6.dp), verticalAlignment=Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("رصيدك", fontSize=12.sp, color=MaterialTheme.colorScheme.onSurfaceVariant)
            Text(if(balanceCents==null) "—" else "$balanceCents رصيد", fontSize=22.sp, fontWeight=FontWeight.Bold)
            if(loading) Text("جارٍ التحديث…",fontSize=11.sp,color=MaterialTheme.colorScheme.onSurfaceVariant)
            if(error.isNotBlank()) TextButton(retry,contentPadding=PaddingValues(0.dp)){Text("إعادة تحميل الرصيد",fontSize=11.sp)}
        }
        TextButton(buy){Text("متجر الرصيد",fontWeight=FontWeight.SemiBold)}
        IconButton(wallet){Icon(Icons.Rounded.AccountBalanceWallet,"المحفظة")}
    }
}
