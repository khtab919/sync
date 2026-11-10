package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

internal fun isMilestoneFrame(frame: Int) = frame in 110..200 && frame % 10 == 0

@Composable
internal fun RelaxiaAvatar(name: String, frame: Int, size: Int, imageUrl: String = "") {
    // Retired IDs may still arrive from older servers or cached profiles.
    if (isMilestoneFrame(frame)) MilestoneAvatar(name, frame, size, imageUrl)
    else PlainAvatar(name, size, imageUrl)
}

@Composable
internal fun RelaxiaFramePicker(current: Int, name: String, imageUrl: String, onChoose: (Int) -> Unit) {
    RelaxiaFramePicker(current, name, imageUrl, 1, setOf(0), onChoose)
}

@Composable
internal fun RelaxiaFramePicker(current: Int, name: String, imageUrl: String,
    level: Int, ownedFrames: Set<Int>, onChoose: (Int) -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal=16.dp), verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("إطاراتي", style=MaterialTheme.typography.headlineMedium)
        RelaxiaLevelBadge(level)
        Text("إطار جديد عند كل 10 مستويات. يبدأ اللمعان المتحرك من المستوى 50 ويزداد في المستويات العليا.", style=MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick={onChoose(0)}, enabled=0 in ownedFrames) { Text(if(current==0)"بدون إطار ✓"else"استخدام الصورة بدون إطار") }
        (0..9).chunked(2).forEach { pair ->
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                pair.forEach { index ->
                    val milestone=(index+1)*10
                    val id=100+milestone
                    val unlocked=id in ownedFrames
                    Surface(onClick={if(unlocked)onChoose(id)}, enabled=unlocked,
                        modifier=Modifier.weight(1f), shape=androidx.compose.foundation.shape.RoundedCornerShape(20.dp),
                        color=MaterialTheme.colorScheme.surface,
                        border=BorderStroke(if(current==id)2.dp else 1.dp,if(current==id)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
                        Column(Modifier.padding(vertical=12.dp,horizontal=4.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.spacedBy(6.dp)) {
                            RelaxiaAvatar(name,id,128,imageUrl)
                            Text("Lv.$milestone",fontWeight=FontWeight.Bold)
                            Text(milestoneNames[index],style=MaterialTheme.typography.labelMedium)
                            Text(if(current==id)"مُجهّز ✓"else if(unlocked)"تجهيز الإطار"else"مقفل · مستوى $milestone",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
