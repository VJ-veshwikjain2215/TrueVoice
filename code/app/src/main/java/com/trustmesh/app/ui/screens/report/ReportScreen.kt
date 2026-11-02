package com.trustmesh.app.ui.screens.report

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustmesh.app.interaction.InteractionManager
import com.trustmesh.app.core.incident.SecurityIncidentManager
import com.trustmesh.app.core.incident.IncidentStatus
import com.trustmesh.app.ui.components.RiskBadge
import com.trustmesh.app.ui.components.TrustMeshTopBar
import com.trustmesh.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ReportScreen(interactionId: String, onBackClick: () -> Unit = {}) {
    val interactions by InteractionManager.interactions.collectAsState()
    val incidents by SecurityIncidentManager.incidents.collectAsState()
    
    val incident = incidents.find { it.incidentId == interactionId }
    val interaction = interactions.find { it.id == interactionId }
    
    Scaffold(
        topBar = {
            TrustMeshTopBar(title = "Security Report", onBackClick = onBackClick)
        },
        containerColor = StitchBgDark
    ) { paddingValues ->
        if (incident != null) {
            IncidentReportScreen(incident = incident, modifier = Modifier.padding(paddingValues))
            return@Scaffold
        }

        if (interaction == null) {
            Text(
                text = "Report not found",
                fontSize = 16.sp,
                color = StitchTextMuted,
                modifier = Modifier
                    .fillMaxSize()
                    .background(StitchBgDark)
                    .padding(paddingValues)
                    .padding(20.dp)
            )
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(StitchBgDark)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(
                text = interaction.title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = StitchTextPrimary
            )
            
            Spacer(modifier = Modifier.height(10.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                RiskBadge(interaction.riskLevel)
                
                interaction.riskAssessment?.attackContext?.let { context ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0x33FEB64E))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = context.inferredIntent.name.replace("_", " "),
                            color = StitchWarningAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            interaction.riskAssessment?.attackContext?.let { context ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(StitchCardDark)
                        .padding(16.dp)
                ) {
                    Column {
                        Text(
                            text = "CORRELATED ATTACK CONTEXT",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp,
                            color = StitchMintGlow
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = context.explanation,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = StitchTextPrimary
                        )
                    }
                }
            }
            
            if (!interaction.associatedKey.isNullOrEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(StitchCardDark)
                        .padding(16.dp)
                ) {
                    Column {
                        Text("Source App", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchMintGlow, letterSpacing = 1.sp)
                        Text(interaction.appName ?: "Unknown App", fontSize = 15.sp, color = StitchTextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        Text("Package Name", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchMintGlow, letterSpacing = 1.sp)
                        Text(interaction.packageName ?: "Unknown Package", fontSize = 13.sp, color = StitchTextSecondary)
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        if (!interaction.notificationTitle.isNullOrBlank()) {
                            Text("Notification Title", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchMintGlow, letterSpacing = 1.sp)
                            Text(interaction.notificationTitle, fontSize = 14.sp, color = StitchTextPrimary)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        
                        if (!interaction.notificationText.isNullOrBlank()) {
                            Text("Content", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchMintGlow, letterSpacing = 1.sp)
                            Text(interaction.notificationText, fontSize = 14.sp, color = StitchTextSecondary)
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                        
                        Text("Observed Time", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = StitchMintGlow, letterSpacing = 1.sp)
                        Text(interaction.timestamp, fontSize = 13.sp, color = StitchTextMuted)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (interaction.evidence.isNotEmpty()) {
                Text("Observed Signals", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                interaction.evidence.forEach { evidence ->
                    Text("• $evidence", fontSize = 14.sp, color = StitchTextSecondary, modifier = Modifier.padding(vertical = 2.dp))
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            if (interaction.timeline.isNotEmpty()) {
                Text("Interaction Timeline", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                interaction.timeline.forEach { timelineEntry ->
                    Text(timelineEntry, fontSize = 13.sp, color = StitchTextMuted, modifier = Modifier.padding(vertical = 2.dp))
                }
            }
        }
    }
}

@Composable
fun IncidentReportScreen(incident: com.trustmesh.app.core.incident.SecurityIncident, modifier: Modifier = Modifier) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(StitchBgDark)
            .verticalScroll(rememberScrollState())
            .padding(20.dp)
    ) {
        Text(
            text = "SECURITY INCIDENT",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            color = StitchAlertRed
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = incident.incidentType.name.replace("_", " "),
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = StitchTextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Severity:", fontSize = 14.sp, color = StitchTextSecondary)
            RiskBadge(incident.severity)
        }
        Spacer(modifier = Modifier.height(8.dp))
        
        Text("Risk Score: ${incident.riskScore} / 100", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = StitchMintGlow)
        Spacer(modifier = Modifier.height(16.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(StitchCardDark)
                .padding(16.dp)
        ) {
            Column {
                Text("Why TrueVoice Flagged This", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Text(incident.explanation, fontSize = 14.sp, lineHeight = 20.sp, color = StitchTextSecondary)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        if (incident.callerIdentity != null) {
            Text("Caller Identity:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
            Text(incident.callerIdentity.displayName ?: incident.callerIdentity.phoneNumber, fontSize = 14.sp, color = StitchMintGlow)
            Spacer(modifier = Modifier.height(12.dp))
        }
        
        if (incident.callerReputation != null) {
            Text("Caller Reputation:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
            Text("${incident.callerReputation.source} — ${incident.callerReputation.reputationLevel.name}", fontSize = 14.sp, color = StitchTextSecondary)
            Spacer(modifier = Modifier.height(16.dp))
        }
        
        Text("Evidence Timeline", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        val interactions by InteractionManager.interactions.collectAsState()
        val relatedInteractions = interactions.filter { it.id in incident.relatedInteractionIds }
        
        relatedInteractions.forEach { inter ->
            Text("${timeFormat.format(Date(inter.timestampMs))}", fontSize = 11.sp, color = StitchTextMuted)
            Text(inter.summary, fontSize = 13.sp, color = StitchTextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text("Recommended Actions:", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = StitchTextPrimary)
        Spacer(modifier = Modifier.height(6.dp))
        incident.recommendedActions.forEach { action ->
            Text("• $action", fontSize = 14.sp, color = StitchTextSecondary, modifier = Modifier.padding(vertical = 2.dp))
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text("Status: ${incident.status.name}", fontSize = 14.sp, color = StitchTextMuted)
        Spacer(modifier = Modifier.height(24.dp))
        
        if (incident.status == IncidentStatus.ACTIVE) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { SecurityIncidentManager.resolveIncident(incident.incidentId) },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Resolve", color = StitchTextPrimary, fontWeight = FontWeight.Bold)
                }
                
                Button(
                    onClick = { SecurityIncidentManager.dismissIncident(incident.incidentId) },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchCardDark),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Dismiss", color = StitchTextSecondary)
                }
            }
        }
    }
}
