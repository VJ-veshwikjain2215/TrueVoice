package com.trustmesh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustmesh.app.interaction.Interaction
import com.trustmesh.app.ui.theme.*

@Composable
fun InteractionCard(interaction: Interaction, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(StitchCardDark, RoundedCornerShape(16.dp))
            .border(1.dp, StitchBorderGlow, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = interaction.title,
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StitchTextPrimary
            )
            Text(
                text = interaction.timestamp,
                style = Typography.bodyMedium.copy(fontSize = 12.sp),
                color = StitchTextSecondary
            )
        }
        
        if (!interaction.notificationTitle.isNullOrBlank() || !interaction.notificationText.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            if (!interaction.notificationTitle.isNullOrBlank()) {
                Text(
                    text = interaction.notificationTitle,
                    style = Typography.titleMedium.copy(fontSize = 14.sp),
                    color = StitchTextPrimary
                )
            }
            if (!interaction.notificationText.isNullOrBlank()) {
                Text(
                    text = interaction.notificationText,
                    style = Typography.bodyMedium.copy(fontSize = 13.sp),
                    color = StitchTextSecondary
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            RiskBadge(riskLevel = interaction.riskLevel)
            
            interaction.riskAssessment?.attackContext?.let { context ->
                Box(
                    modifier = Modifier
                        .background(Color(0x22FEB64E), RoundedCornerShape(12.dp))
                        .border(1.dp, StitchWarningAmber.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = context.inferredIntent.name.replace("_", " "),
                        color = StitchWarningAmber,
                        style = Typography.labelMedium.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = interaction.riskAssessment?.attackContext?.explanation ?: interaction.summary,
            style = Typography.bodyMedium.copy(fontSize = 13.sp, lineHeight = 18.sp),
            color = StitchTextSecondary
        )
    }
}
