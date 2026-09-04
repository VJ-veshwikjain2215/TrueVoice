package com.trustmesh.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustmesh.app.core.incident.IncidentStatus
import com.trustmesh.app.core.incident.SecurityIncident
import com.trustmesh.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun IncidentCard(incident: SecurityIncident, onClick: () -> Unit) {
    val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
    val timeString = timeFormat.format(Date(incident.createdAt))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(StitchCardDark)
            .border(1.dp, StitchBorderGlow, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            val title = incident.incidentType.name.replace("_", " ")
            Text(
                text = title,
                style = Typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = StitchTextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RiskBadge(incident.severity)
                if (incident.status != IncidentStatus.ACTIVE) {
                    Text(
                        text = incident.status.name,
                        style = Typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp),
                        color = StitchTextSecondary
                    )
                }
            }
        }
        Text(
            text = timeString,
            style = Typography.bodyMedium.copy(fontSize = 12.sp),
            color = StitchTextSecondary
        )
    }
}
