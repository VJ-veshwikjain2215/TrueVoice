package com.trustmesh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustmesh.app.core.events.RiskLevel
import com.trustmesh.app.ui.theme.*

@Composable
fun CallerRiskCard(identity: String, relationship: String, riskLevel: RiskLevel, reason: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(StitchCardDark, RoundedCornerShape(16.dp))
            .border(1.dp, StitchBorderGlow, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = identity,
                    style = Typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = StitchTextPrimary
                )
                Text(
                    text = relationship,
                    style = Typography.bodyMedium,
                    color = StitchTextSecondary
                )
            }
            RiskBadge(riskLevel)
        }
        
        Spacer(modifier = Modifier.height(14.dp))
        
        Text(
            text = reason,
            style = Typography.bodyMedium.copy(fontSize = 14.sp, lineHeight = 20.sp),
            color = StitchTextPrimary
        )
    }
}
