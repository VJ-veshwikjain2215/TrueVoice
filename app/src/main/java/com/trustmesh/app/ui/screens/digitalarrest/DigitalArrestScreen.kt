package com.trustmesh.app.ui.screens.digitalarrest

import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.trustmesh.app.core.digitalarrest.*
import com.trustmesh.app.ui.theme.*
import java.io.File

@Composable
fun DigitalArrestScreen(onBack: () -> Unit) {
    val state by DigitalArrestController.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    when (state.phase) {
        DaWorkflowPhase.IDLE    -> DaIdleContent(context, onBack)
        DaWorkflowPhase.COMPLETE -> DaCriticalDashboard(state, context, onBack)
        DaWorkflowPhase.ERROR   -> DaErrorContent(state.errorMessage, context, onBack)
        else                    -> DaProgressContent(state.phase)
    }
}

// ── Idle state ────────────────────────────────────────────────────────────────

@Composable
private fun DaIdleContent(context: Context, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(StitchBgDark)
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(StitchAlertRed.copy(alpha = 0.15f))
                .border(1.5.dp, StitchAlertRed.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Shield, null, Modifier.size(44.dp), tint = StitchAlertRed)
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "DIGITAL ARREST PROTECTION",
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            letterSpacing = 1.sp,
            color = StitchTextPrimary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Tap Simulate trigger below to run the guided Digital Arrest defense workflow.",
            fontSize = 13.sp,
            color = StitchTextMuted,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
        Spacer(Modifier.height(36.dp))
        Button(
            onClick = { DigitalArrestController.simulateTrigger(context) },
            colors = ButtonDefaults.buttonColors(containerColor = StitchAlertRed),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
        ) {
            Icon(Icons.Default.PlayArrow, null, tint = StitchTextPrimary)
            Spacer(Modifier.width(8.dp))
            Text("SIMULATE THREAT TRIGGER", fontWeight = FontWeight.Bold, color = StitchTextPrimary)
        }
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onBack) { Text("← Back to Home", color = StitchTextMuted) }
    }
}

// ── Progress animation ────────────────────────────────────────────────────────

private val PHASE_LABELS = mapOf(
    DaWorkflowPhase.SMS_DETECTED        to "SMS RECEIVED",
    DaWorkflowPhase.TRIGGER_VERIFIED    to "TRIGGER VERIFIED",
    DaWorkflowPhase.CAPTURING_EVIDENCE  to "CAPTURING EVIDENCE",
    DaWorkflowPhase.COLLECTING_METADATA to "COLLECTING METADATA",
    DaWorkflowPhase.EVALUATING_RULES    to "EVALUATING RULES",
    DaWorkflowPhase.SCORING_RISK        to "SCORING RISK",
    DaWorkflowPhase.SEALING_EVIDENCE    to "SEALING EVIDENCE",
    DaWorkflowPhase.GENERATING_REPORT   to "GENERATING CASE REPORT",
    DaWorkflowPhase.NOTIFYING_CONTACTS  to "NOTIFYING TRUSTED CONTACTS",
)

private val ALL_PHASES = listOf(
    DaWorkflowPhase.SMS_DETECTED,
    DaWorkflowPhase.TRIGGER_VERIFIED,
    DaWorkflowPhase.CAPTURING_EVIDENCE,
    DaWorkflowPhase.COLLECTING_METADATA,
    DaWorkflowPhase.EVALUATING_RULES,
    DaWorkflowPhase.SCORING_RISK,
    DaWorkflowPhase.SEALING_EVIDENCE,
    DaWorkflowPhase.GENERATING_REPORT,
    DaWorkflowPhase.NOTIFYING_CONTACTS,
)

@Composable
private fun DaProgressContent(current: DaWorkflowPhase) {
    val infiniteT = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteT.animateFloat(
        0.6f, 1f,
        infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "pulse"
    )

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF0F1715), Color(0xFF1E0A0E), Color(0xFF0F1715)))
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                Modifier
                    .size(80.dp)
                    .scale(pulse)
                    .clip(CircleShape)
                    .background(StitchAlertRed.copy(alpha = 0.2f))
                    .border(2.dp, StitchAlertRed.copy(alpha = pulse), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Security, null, Modifier.size(40.dp), tint = StitchAlertRed)
            }
            Spacer(Modifier.height(24.dp))
            Text(
                "DIGITAL ARREST PROTECTION",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = StitchTextPrimary,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "WORKFLOW ACTIVE",
                fontSize = 11.sp,
                color = StitchAlertRed,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(32.dp))

            ALL_PHASES.forEachIndexed { idx, phase ->
                val done    = ALL_PHASES.indexOf(current) > idx
                val active  = phase == current
                DaPhaseRow(label = PHASE_LABELS[phase] ?: phase.name, done = done, active = active, pulse = pulse)
                if (idx < ALL_PHASES.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun DaPhaseRow(label: String, done: Boolean, active: Boolean, pulse: Float) {
    val color = when {
        done   -> StitchMintGlow
        active -> StitchAlertRed.copy(alpha = pulse)
        else   -> StitchTextMuted.copy(alpha = 0.5f)
    }
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            if (done) Icon(Icons.Default.CheckCircle, null, Modifier.size(18.dp), tint = StitchMintGlow)
            else if (active) CircularProgressIndicator(Modifier.size(16.dp), color = StitchAlertRed, strokeWidth = 2.dp)
            else Box(Modifier.size(8.dp).clip(CircleShape).background(StitchGlassBorder))
        }
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 12.sp, color = color, fontWeight = if (active) FontWeight.Bold else FontWeight.Normal)
    }
}

// ── CRITICAL Dashboard ────────────────────────────────────────────────────────

@Composable
private fun DaCriticalDashboard(state: DigitalArrestState, context: Context, onBack: () -> Unit) {
    val inc = state.incident ?: return
    val scroll = rememberScrollState()
    val infiniteT = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteT.animateFloat(
        0.5f, 1f,
        infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "pulse"
    )

    Column(
        Modifier
            .fillMaxSize()
            .background(StitchBgDark)
            .verticalScroll(scroll)
    ) {
        // Critical header
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFF1E0A0E), Color(0xFF2C1015))))
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(72.dp)
                        .scale(0.9f + 0.1f * pulse)
                        .clip(CircleShape)
                        .background(StitchAlertRed.copy(0.15f))
                        .border(2.dp, StitchAlertRed.copy(pulse), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.GppBad, null, Modifier.size(36.dp), tint = StitchAlertRed)
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "CRITICAL THREAT DETECTED",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = StitchAlertRed,
                    letterSpacing = 1.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(4.dp))
                Text("Digital Arrest / Authority Impersonation", fontSize = 13.sp, color = StitchTextSecondary, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                // Big score
                Text(
                    "${inc.threat.overallRisk}",
                    fontSize = 64.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchAlertRed,
                    textAlign = TextAlign.Center
                )
                Text("/ 100   CRITICAL THREAT", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = StitchTextMuted, textAlign = TextAlign.Center)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Status pills
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatusPill("Evidence", "SECURED", StitchMintGlow, Modifier.weight(1f))
            StatusPill("Report",   "GENERATED", StitchPrimaryTeal, Modifier.weight(1f))
            StatusPill("Contacts", if (inc.notificationStatus == "SENT") "NOTIFIED" else "PENDING",
                if (inc.notificationStatus == "SENT") StitchMintGlow else StitchWarningAmber, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        // Incident info card
        DaCard(Modifier.padding(horizontal = 16.dp)) {
            DaFieldRow("Case ID", inc.caseId)
            DaFieldRow("Caller", inc.caller.displayName)
            DaFieldRow("Agency", inc.caller.claimedAgency)
            DaFieldRow("Identity", "UNVERIFIED", StitchAlertRed)
            DaFieldRow("Platform", "${inc.communicationPlatform} ${inc.communicationType}")
        }

        Spacer(Modifier.height(12.dp))

        // Threat bars
        DaCard(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "TRUEVOICE THREAT ANALYSIS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StitchMintGlow,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(12.dp))
            DaThreatBar("Authority Impersonation", inc.threat.authorityImpersonation)
            DaThreatBar("Arrest Threat", inc.threat.arrestThreat)
            DaThreatBar("Coercion", inc.threat.coercion)
            DaThreatBar("Identity Verification", inc.threat.identityVerification)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = StitchGlassBorder)
            Spacer(Modifier.height(10.dp))
            Text(
                "OVERALL RISK: ${inc.threat.overallRisk} / 100 — ${inc.threat.severity}",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = StitchAlertRed
            )
            Text("TrueVoice Risk Assessment — Local heuristic detection", fontSize = 9.sp, color = StitchTextMuted)
        }

        Spacer(Modifier.height(12.dp))

        // Rules
        DaCard(Modifier.padding(horizontal = 16.dp)) {
            Text(
                "DETECTED VIOLATIONS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = StitchMintGlow,
                letterSpacing = 1.sp
            )
            Spacer(Modifier.height(8.dp))
            inc.rules.forEach { rule ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(rule.title, fontSize = 12.sp, color = StitchTextPrimary, modifier = Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    val sevColor = if (rule.severity == DaRuleSeverity.CRITICAL) StitchAlertRed else StitchWarningAmber
                    Text(rule.severity.name, fontSize = 10.sp, color = sevColor, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Text(rule.status.name, fontSize = 10.sp, color = StitchAlertRed, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = StitchGlassBorder, thickness = 0.5.dp)
            }
        }

        Spacer(Modifier.height(12.dp))

        // Evidence hash
        inc.evidence.firstOrNull()?.let { ev ->
            DaCard(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "EVIDENCE INTEGRITY",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = StitchMintGlow,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(8.dp))
                Text("SHA-256 HASH", fontSize = 10.sp, color = StitchTextMuted)
                Text(ev.sha256.take(48) + "...", fontSize = 10.sp, color = StitchTextPrimary, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Action buttons
        Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            state.reportPath?.let { path ->
                Button(
                    onClick = { openPdf(context, path) },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryTeal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) {
                    Icon(Icons.Default.PictureAsPdf, null, tint = StitchTextPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("VIEW INCIDENT REPORT", fontWeight = FontWeight.Bold, color = StitchTextPrimary)
                }
            }
            OutlinedButton(
                onClick = { DigitalArrestController.reset() },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(48.dp),
                border = BorderStroke(1.dp, StitchGlassBorder)
            ) {
                Text("RESET DEMO", color = StitchTextSecondary)
            }
            TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
                Text("← Back to Home", color = StitchTextMuted)
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun DaCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StitchCardDark,
        border = BorderStroke(1.dp, StitchGlassBorder)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun DaFieldRow(key: String, value: String, valueColor: Color = StitchTextPrimary) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(key, fontSize = 12.sp, color = StitchTextMuted)
        Text(
            value,
            fontSize = 12.sp,
            color = valueColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1f).padding(start = 8.dp)
        )
    }
}

@Composable
private fun DaThreatBar(label: String, score: Int) {
    Column(Modifier.padding(vertical = 4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 12.sp, color = StitchTextPrimary)
            Text("$score%", fontSize = 12.sp, color = StitchAlertRed, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(StitchGlassBorder)
        ) {
            Box(
                Modifier
                    .fillMaxWidth(score / 100f)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(StitchAlertRed)
            )
        }
    }
}

@Composable
private fun StatusPill(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = StitchCardDark,
        border = BorderStroke(1.dp, StitchGlassBorder)
    ) {
        Column(Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, fontSize = 9.sp, color = StitchTextMuted, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(value, fontSize = 11.sp, color = color, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun DaErrorContent(msg: String?, context: Context, onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .background(StitchBgDark)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Error, null, Modifier.size(48.dp), tint = StitchAlertRed)
        Spacer(Modifier.height(12.dp))
        Text("Workflow Error", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = StitchTextPrimary)
        Spacer(Modifier.height(8.dp))
        Text(msg ?: "Unknown error", fontSize = 13.sp, color = StitchTextMuted, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = { DigitalArrestController.simulateTrigger(context) },
            colors = ButtonDefaults.buttonColors(containerColor = StitchAlertRed),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("RETRY", fontWeight = FontWeight.Bold, color = StitchTextPrimary)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onBack) { Text("← Back", color = StitchTextMuted) }
    }
}

private fun openPdf(context: Context, path: String) {
    try {
        val file = File(path)
        val uri = androidx.core.content.FileProvider.getUriForFile(
            context, "${context.packageName}.provider", file
        )
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        android.util.Log.e("DaScreen", "Cannot open PDF", e)
    }
}
