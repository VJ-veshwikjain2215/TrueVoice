package com.trustmesh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustmesh.app.core.events.RiskLevel
import com.trustmesh.app.ui.theme.*

@Composable
fun RiskBadge(riskLevel: RiskLevel) {
    val (badgeColor, bgAlphaColor) = when (riskLevel) {
        RiskLevel.LOW -> Pair(StitchMintGlow, Color(0x2288D5C0))
        RiskLevel.ELEVATED -> Pair(StitchWarningAmber, Color(0x22FEB64E))
        RiskLevel.HIGH -> Pair(StitchTertiaryRed, Color(0x22D9622B))
        RiskLevel.CRITICAL -> Pair(StitchAlertRed, Color(0x22BA1A1A))
    }
    
    Box(
        modifier = Modifier
            .background(bgAlphaColor, RoundedCornerShape(12.dp))
            .border(1.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .background(badgeColor, CircleShape)
            )
            Text(
                text = riskLevel.displayName.uppercase(),
                color = badgeColor,
                style = Typography.labelMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.5.sp
                )
            )
        }
    }
}
