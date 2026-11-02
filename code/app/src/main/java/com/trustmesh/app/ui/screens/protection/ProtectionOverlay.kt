package com.trustmesh.app.ui.screens.protection

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.trustmesh.app.MainActivity
import com.trustmesh.app.core.events.RiskLevel
import com.trustmesh.app.core.identity.CallerIdentity
import com.trustmesh.app.core.identity.IdentitySource
import com.trustmesh.app.core.voicescan.VoiceScanController
import com.trustmesh.app.core.voicescan.VoiceScanPhase
import com.trustmesh.app.core.voicescan.VoiceScanState
import com.trustmesh.app.core.voicescan.VoiceScanVerdict
import com.trustmesh.app.interaction.InteractionManager
import com.trustmesh.app.ui.theme.*

enum class OverlayPresentationMode {
    COMPACT, FLOATING, BOTTOM_SHEET, FULL_SCREEN
}

@Composable
fun ProtectionOverlay(
    state: CallOverlayState,
    callerIdentity: CallerIdentity?,
    callerReputation: com.trustmesh.app.core.identity.CallerReputation?,
    fallbackName: String,
    fallbackNumber: String,
    riskLevel: RiskLevel,
    riskAssessment: com.trustmesh.app.core.intelligence.risk.RiskAssessment?,
    activeIncident: com.trustmesh.app.core.incident.SecurityIncident? = null,
    activeCallDurationSeconds: Long = 0,
    scan: VoiceScanState = VoiceScanState(),
    currentInteraction: com.trustmesh.app.interaction.Interaction? = null,
    onDismiss: () -> Unit
) {
    val identityName = callerIdentity?.displayName ?: "Unknown Caller"
    android.util.Log.d("TrustMeshIdentity", "overlayRecomposed=true identity=$identityName")

    var isMinimizedByUser by remember(fallbackNumber) { mutableStateOf(false) }

    LaunchedEffect(state) {
        if (state == CallOverlayState.INCOMING) {
            isMinimizedByUser = false
        }
    }

    val effectiveRiskLevel = scan.effectiveRiskLevel(riskLevel)

    val context = LocalContext.current
    LaunchedEffect(effectiveRiskLevel) {
        if (state == CallOverlayState.ACTIVE || state == CallOverlayState.INCOMING) {
            if (effectiveRiskLevel == RiskLevel.CRITICAL || effectiveRiskLevel == RiskLevel.HIGH) {
                isMinimizedByUser = false
                if (scan.active) return@LaunchedEffect
                try {
                    val vibrator = context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                    if (vibrator != null && vibrator.hasVibrator()) {
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                            val timings = longArrayOf(0, 500, 200, 500)
                            val amplitudes = intArrayOf(0, 255, 0, 255)
                            val effect = android.os.VibrationEffect.createWaveform(timings, amplitudes, -1)
                            vibrator.vibrate(effect)
                        } else {
                            @Suppress("DEPRECATION")
                            vibrator.vibrate(longArrayOf(0, 500, 200, 500), -1)
                        }
                        android.util.Log.i("TrustMeshOverlay", "Critical risk vibration triggered")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("TrustMeshOverlay", "Failed to vibrate", e)
                }
            }
        }
    }

    when (state) {
        CallOverlayState.INCOMING -> {
            if (isMinimizedByUser) {
                ActiveCallStatusBox(
                    callerIdentity = callerIdentity,
                    fallbackName = fallbackName,
                    fallbackNumber = fallbackNumber,
                    riskLevel = riskLevel,
                    riskAssessment = riskAssessment,
                    activeIncident = activeIncident,
                    interaction = currentInteraction,
                    scan = scan,
                    onDismiss = { isMinimizedByUser = true },
                    onExpandRequested = { isMinimizedByUser = false }
                )
            } else {
                if (effectiveRiskLevel == RiskLevel.CRITICAL) {
                    FullScreenSecurityOverlay(callerIdentity, callerReputation, fallbackName, fallbackNumber, riskLevel, riskAssessment, activeIncident, currentInteraction, scan, onDismiss = { isMinimizedByUser = true })
                } else {
                    FloatingRiskCard(callerIdentity, callerReputation, fallbackName, fallbackNumber, riskLevel, riskAssessment, activeIncident, currentInteraction, scan, onDismiss = { isMinimizedByUser = true })
                }
            }
        }
        CallOverlayState.ACTIVE -> {
            if (isMinimizedByUser) {
                ActiveCallStatusBox(
                    callerIdentity = callerIdentity,
                    fallbackName = fallbackName,
                    fallbackNumber = fallbackNumber,
                    riskLevel = riskLevel,
                    riskAssessment = riskAssessment,
                    activeIncident = activeIncident,
                    interaction = currentInteraction,
                    scan = scan,
                    onDismiss = { isMinimizedByUser = true },
                    onExpandRequested = { isMinimizedByUser = false }
                )
            } else {
                if (effectiveRiskLevel == RiskLevel.CRITICAL) {
                    FullScreenSecurityOverlay(callerIdentity, callerReputation, fallbackName, fallbackNumber, riskLevel, riskAssessment, activeIncident, currentInteraction, scan, onDismiss = { isMinimizedByUser = true })
                } else if (effectiveRiskLevel == RiskLevel.HIGH) {
                    FloatingRiskCard(callerIdentity, callerReputation, fallbackName, fallbackNumber, riskLevel, riskAssessment, activeIncident, currentInteraction, scan, onDismiss = { isMinimizedByUser = true })
                } else {
                    ActiveCallStatusBox(
                        callerIdentity = callerIdentity,
                        fallbackName = fallbackName,
                        fallbackNumber = fallbackNumber,
                        riskLevel = riskLevel,
                        riskAssessment = riskAssessment,
                        activeIncident = activeIncident,
                        interaction = currentInteraction,
                        scan = scan,
                        onDismiss = { isMinimizedByUser = true },
                        onExpandRequested = null
                    )
                }
            }
        }
        CallOverlayState.SUMMARY -> {
            CallSummaryOverlay(currentInteraction, activeIncident, activeCallDurationSeconds, scan, onDismiss)
        }
        CallOverlayState.HIDDEN -> {
            // Drawn nothing when hidden
        }
    }
}

/** Palette shared by every risk surface in the overlay. */
private val DangerRed = StitchAlertRed
private val WarnAmber = StitchWarningAmber
private val SafeGreen = StitchMintGlow
private val Ink = StitchTextPrimary
private val Slate = StitchTextMuted
private val Cyan = StitchPrimaryTeal
private val Hairline = StitchGlassBorder

private fun riskColor(level: RiskLevel): Color = when (level) {
    RiskLevel.CRITICAL, RiskLevel.HIGH -> DangerRed
    RiskLevel.ELEVATED -> WarnAmber
    RiskLevel.LOW -> SafeGreen
}

private fun displayScore(scan: VoiceScanState, riskAssessment: com.trustmesh.app.core.intelligence.risk.RiskAssessment?): Int =
    if (scan.hasScore) scan.riskScore else riskAssessment?.score ?: 0

@Composable
fun RealtimeRiskGraph(
    currentScore: Int,
    riskAssessment: com.trustmesh.app.core.intelligence.risk.RiskAssessment?,
    modifier: Modifier = Modifier,
    scan: VoiceScanState = VoiceScanState()
) {
    val liveTrend = scan.active && scan.trend.size >= 2
    val targetScore = (if (scan.hasScore) scan.riskScore else currentScore).coerceIn(0, 100)

    val animatedScore by animateFloatAsState(
        targetValue = targetScore.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "RiskScoreAnim"
    )

    var animationTime by remember { mutableStateOf(0f) }
    LaunchedEffect(liveTrend) {
        if (liveTrend) return@LaunchedEffect
        val startTime = System.currentTimeMillis()
        while (true) {
            animationTime = (System.currentTimeMillis() - startTime) / 1000f
            kotlinx.coroutines.delay(32)
        }
    }

    val basePoints: List<Float> = if (liveTrend) {
        scan.trend.map { it.toFloat() }
    } else remember(targetScore, riskAssessment, animationTime) {
        val score = targetScore.toFloat()
        val baseOsc = (Math.sin(animationTime.toDouble() * 2.5) * 3.5 + Math.cos(animationTime.toDouble() * 4.2) * 1.8).toFloat()

        val l1Score = (15f + baseOsc * 0.4f).coerceIn(0f, 100f)
        val l2Score = (if (riskAssessment?.factors?.any { it.description.contains("Unknown", ignoreCase = true) || it.description.contains("Contact", ignoreCase = true) } == true) 35f else 15f) + baseOsc * 0.8f
        val l3Score = (if (riskAssessment?.factors?.any { it.description.contains("Voice", ignoreCase = true) || it.description.contains("Fingerprint", ignoreCase = true) } == true) 55f else (l2Score + 10f)) + baseOsc * 1.1f
        val l4Score = (if (riskAssessment?.factors?.any { it.description.contains("Groq", ignoreCase = true) || it.description.contains("Semantic", ignoreCase = true) } == true) 75f else (l3Score + 10f)) + baseOsc * 0.9f
        val l5Score = (if (riskAssessment?.attackContext != null) 85f else l4Score) + baseOsc * 1.3f
        val l6Score = score + baseOsc * 1.5f

        listOf(
            (10f + baseOsc * 0.2f).coerceIn(0f, 100f),
            l1Score.coerceIn(0f, 100f),
            l2Score.coerceIn(0f, 100f),
            l3Score.coerceIn(0f, 100f),
            l4Score.coerceIn(0f, 100f),
            l5Score.coerceIn(0f, 100f),
            l6Score.coerceIn(0f, 100f)
        )
    }

    val yMin: Float
    val yMax: Float
    if (liveTrend) {
        val lo = (basePoints.min() - 6f).coerceAtLeast(0f)
        val hi = (basePoints.max() + 6f).coerceAtMost(100f)
        yMin = lo
        yMax = if (hi - lo < 12f) (lo + 12f).coerceAtMost(100f) else hi
    } else {
        yMin = 0f
        yMax = 100f
    }
    val span = (yMax - yMin).coerceAtLeast(1f)

    val lineColor = when {
        animatedScore >= 50f -> DangerRed
        animatedScore >= 25f -> WarnAmber
        else -> SafeGreen
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(StitchCardDark, RoundedCornerShape(16.dp))
            .border(BorderStroke(1.dp, Hairline), RoundedCornerShape(16.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (liveTrend) "LIVE VOICE-ANALYSIS TREND" else "LIVE MULTI-LAYER RISK",
                    color = Ink,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = when {
                        scan.phase == VoiceScanPhase.BUFFERING -> "● BUFFERING AUDIO — ${scan.secondsRemaining}s"
                        scan.verdict == VoiceScanVerdict.SYNTHETIC -> "● SYNTHESIS MARKERS SCORING"
                        scan.verdict == VoiceScanVerdict.GENUINE -> "● VOICE VERIFIED — MONITORING"
                        else -> "● ANALYZING CALL IN REAL TIME"
                    },
                    color = Cyan,
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = "${animatedScore.toInt()}% THREAT",
                color = lineColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Black
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            val width = size.width
            val height = size.height
            val points = basePoints

            if (points.size < 2) return@Canvas

            fun yFor(value: Float) = height - ((value - yMin) / span * height)

            val dashEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            for (pct in listOf(0.25f, 0.50f, 0.75f)) {
                val y = height * (1f - pct)
                drawLine(
                    color = Hairline,
                    start = Offset(0f, y),
                    end = Offset(width, y),
                    pathEffect = dashEffect,
                    strokeWidth = 1.dp.toPx()
                )
            }

            val stepX = width / (points.size - 1)
            val path = Path()
            val fillPath = Path()

            val startY = yFor(points[0])
            path.moveTo(0f, startY)
            fillPath.moveTo(0f, height)
            fillPath.lineTo(0f, startY)

            for (i in 1 until points.size) {
                val x1 = (i - 1) * stepX
                val y1 = yFor(points[i - 1])
                val x2 = i * stepX
                val y2 = yFor(points[i])

                val controlX1 = x1 + (x2 - x1) / 2f
                val controlY1 = y1
                val controlX2 = x1 + (x2 - x1) / 2f
                val controlY2 = y2

                path.cubicTo(controlX1, controlY1, controlX2, controlY2, x2, y2)
                fillPath.cubicTo(controlX1, controlY1, controlX2, controlY2, x2, y2)
            }

            fillPath.lineTo(width, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = 0.35f),
                        lineColor.copy(alpha = 0.02f)
                    )
                )
            )

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 2.5.dp.toPx())
            )

            val markEvery = if (points.size > 10) points.size - 1 else 0
            for (i in points.indices) {
                if (markEvery != 0 && i != markEvery) continue
                val px = i * stepX
                val py = yFor(points[i])
                val isCurrent = (i == points.size - 1)

                drawCircle(
                    color = if (isCurrent) lineColor else StitchBgDark,
                    radius = if (isCurrent) 5.dp.toPx() else 2.5.dp.toPx(),
                    center = Offset(px, py)
                )
                if (isCurrent) {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.3f),
                        radius = 10.dp.toPx(),
                        center = Offset(px, py)
                    )
                } else {
                    drawCircle(
                        color = lineColor.copy(alpha = 0.5f),
                        radius = 2.5.dp.toPx(),
                        center = Offset(px, py),
                        style = Stroke(width = 1.dp.toPx())
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (liveTrend) {
                val observedLow = basePoints.min().toInt()
                val observedHigh = basePoints.max().toInt()
                Text(
                    text = "axis ${yMin.toInt()}–${yMax.toInt()}%",
                    fontSize = 8.sp,
                    color = Slate
                )
                Text(
                    text = "observed $observedLow–$observedHigh%",
                    fontSize = 8.sp,
                    color = Ink,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "now ${basePoints.last().toInt()}%",
                    fontSize = 8.sp,
                    color = lineColor,
                    fontWeight = FontWeight.Bold
                )
            } else {
                val layers = listOf("L1 Sig", "L2 Id", "L3 Voice", "L4 Groq", "L5 Ctx", "L6 Fuse")
                layers.forEachIndexed { idx, label ->
                    Text(
                        text = label,
                        fontSize = 8.sp,
                        color = Ink,
                        fontWeight = if (idx == layers.lastIndex) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
private fun VoiceAnalysisPanel(scan: VoiceScanState) {
    if (!scan.active) return

    val buffering = scan.phase == VoiceScanPhase.BUFFERING
    val accent = when (scan.verdict) {
        null -> Cyan
        VoiceScanVerdict.GENUINE -> if (scan.riskScore >= 50) DangerRed else SafeGreen
        VoiceScanVerdict.SYNTHETIC -> DangerRed
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StitchCardDark, RoundedCornerShape(14.dp))
            .border(BorderStroke(1.dp, accent.copy(alpha = 0.45f)), RoundedCornerShape(14.dp))
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🎙 VOICE AUTHENTICITY ANALYSIS",
                color = accent,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            if (scan.hasScore) {
                Text(
                    text = "${scan.cloneConfidence}%",
                    color = accent,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = scan.headline,
            color = Ink,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )

        if (scan.hasScore) {
            Text(
                text = "Confidence the voice is AI-cloned",
                color = Slate,
                fontSize = 10.sp
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = scan.summary,
            color = StitchTextSecondary,
            fontSize = 11.sp,
            lineHeight = 15.sp
        )

        if (buffering) {
            Spacer(modifier = Modifier.height(8.dp))
            val progress = (scan.bufferSeconds - scan.secondsRemaining)
                .toFloat() / scan.bufferSeconds.coerceAtLeast(1)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(Hairline)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress.coerceIn(0f, 1f))
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(Cyan)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "${scan.secondsRemaining}s of buffer remaining",
                color = Slate,
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        if (scan.markers.isEmpty()) return@Column

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "WHAT IT TAKES TO CALL A VOICE SYNTHETIC — ${scan.flaggedMarkers} OF ${scan.markers.size} MARKERS FAILED",
            color = Slate,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(4.dp))

        scan.markers.forEach { marker ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = if (marker.flagged) "✗" else "✓",
                    color = if (marker.flagged) DangerRed else SafeGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.width(14.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = marker.label,
                        color = Ink,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = marker.reading,
                        color = Slate,
                        fontSize = 10.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = scan.decisionRule,
            color = Slate,
            fontSize = 10.sp,
            lineHeight = 14.sp
        )
    }
}

@Composable
fun PremiumNotificationOverlayCard(
    callerIdentity: CallerIdentity?,
    callerReputation: com.trustmesh.app.core.identity.CallerReputation?,
    fallbackName: String,
    fallbackNumber: String,
    riskLevel: RiskLevel,
    riskAssessment: com.trustmesh.app.core.intelligence.risk.RiskAssessment?,
    activeIncident: com.trustmesh.app.core.incident.SecurityIncident?,
    interaction: com.trustmesh.app.interaction.Interaction? = null,
    scan: VoiceScanState = VoiceScanState(),
    onDismiss: () -> Unit,
    onCollapse: (() -> Unit)? = null
) {
    val effectiveRiskLevel = scan.effectiveRiskLevel(riskLevel)
    val themeColor = riskColor(effectiveRiskLevel)
    val score = displayScore(scan, riskAssessment)

    val explanation = if (scan.hasScore) {
        scan.summary
    } else {
        activeIncident?.explanation
            ?: riskAssessment?.attackContext?.explanation
            ?: riskAssessment?.explanation
            ?: "Analyzing live conversation metrics..."
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(StitchCardDark, RoundedCornerShape(24.dp))
            .border(BorderStroke(1.5.dp, themeColor), RoundedCornerShape(24.dp))
            .padding(18.dp)
    ) {
        // 1. Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = com.trustmesh.app.R.drawable.ic_trinetra_orbit_eye),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "TRINETRA",
                        color = StitchTextPrimary,
                        fontWeight = FontWeight.Black,
                        fontSize = 11.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "CALL SECURITY",
                        color = StitchTextMuted,
                        fontWeight = FontWeight.Bold,
                        fontSize = 8.sp,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "● LIVE PROTECTION",
                    color = StitchPrimaryTeal,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(StitchGlassBorder, RoundedCornerShape(12.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✕",
                        color = StitchTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Caller Info and Reputation Hero Section
        val name = callerIdentity?.displayName ?: fallbackName.ifBlank { "Unknown Caller" }
        val num = callerIdentity?.phoneNumber ?: fallbackNumber

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StitchBgDark.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .border(BorderStroke(1.dp, StitchGlassBorder), RoundedCornerShape(16.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(StitchPrimaryTeal.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                    .border(1.dp, StitchPrimaryTeal, RoundedCornerShape(20.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.take(1).uppercase(),
                    color = StitchMintGlow,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    color = StitchTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    maxLines = 1
                )
                if (num.isNotBlank()) {
                    Text(
                        text = num,
                        color = StitchTextMuted,
                        fontSize = 12.sp
                    )
                }
                if (callerReputation != null && callerReputation.reputationLevel != com.trustmesh.app.core.identity.ReputationLevel.UNKNOWN) {
                    Text(
                        text = "Directory: ${callerReputation.displayName ?: callerReputation.category.name}",
                        color = StitchPrimaryTeal,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${score}%",
                    color = themeColor,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp
                )
                Text(
                    text = effectiveRiskLevel.displayName,
                    color = themeColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (scan.active) {
            VoiceAnalysisPanel(scan)
            Spacer(modifier = Modifier.height(12.dp))
        }

        Text(
            text = explanation,
            color = StitchTextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )

        val groqResp = interaction?.groqResponse.takeIf { !scan.hasScore }
        if (groqResp != null) {
            Spacer(modifier = Modifier.height(10.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StitchPrimaryTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, StitchPrimaryTeal.copy(alpha = 0.3f)), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = "🧠 AI SEMANTIC ANALYSIS (GROQ)",
                    color = StitchPrimaryTeal,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = groqResp.summaryReasoning,
                    color = StitchTextPrimary,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
                if (groqResp.psychologicalTriggers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Triggers detected: ${groqResp.psychologicalTriggers.joinToString(", ")}",
                        color = StitchWarningAmber,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        RealtimeRiskGraph(
            currentScore = score,
            riskAssessment = riskAssessment,
            scan = scan
        )

        val evidence = riskAssessment?.evidence ?: listOf("Incoming call stream active")
        val recommendations = activeIncident?.recommendedActions ?: emptyList()

        if (evidence.isNotEmpty() || recommendations.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(StitchBgDark.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .border(BorderStroke(1.dp, StitchGlassBorder), RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                if (evidence.isNotEmpty()) {
                    Text(
                        text = "SECURITY SIGNALS",
                        color = StitchTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    evidence.take(3).forEach { ev ->
                        Text(
                            text = "• $ev",
                            color = StitchTextPrimary,
                            fontSize = 11.sp
                        )
                    }
                }
                if (recommendations.isNotEmpty()) {
                    if (evidence.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    Text(
                        text = "RECOMMENDED ACTIONS",
                        color = StitchAlertRed,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    recommendations.forEach { rec ->
                        Text(
                            text = "• $rec",
                            color = StitchTextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(StitchBgDark, RoundedCornerShape(8.dp))
                .border(BorderStroke(1.dp, StitchGlassBorder), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🛡 Zero audio is recorded or processed externally. Active call cannot be retroactively blocked.",
                color = StitchMintGlow,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        if (onCollapse != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                TextButton(
                    onClick = onCollapse,
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(28.dp)
                ) {
                    Text(
                        text = "Collapse Overlay",
                        color = StitchPrimaryTeal,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ActiveCallStatusBox(
    callerIdentity: CallerIdentity?,
    fallbackName: String,
    fallbackNumber: String,
    riskLevel: RiskLevel,
    riskAssessment: com.trustmesh.app.core.intelligence.risk.RiskAssessment?,
    activeIncident: com.trustmesh.app.core.incident.SecurityIncident?,
    interaction: com.trustmesh.app.interaction.Interaction? = null,
    scan: VoiceScanState = VoiceScanState(),
    onDismiss: () -> Unit,
    onExpandRequested: (() -> Unit)? = null
) {
    var isExpanded by remember(onExpandRequested) { mutableStateOf(onExpandRequested == null) }

    val effectiveRiskLevel = scan.effectiveRiskLevel(riskLevel)
    val themeColor = riskColor(effectiveRiskLevel)

    Box(
        modifier = Modifier
            .padding(8.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    ProtectionController.updatePosition(dragAmount.x.toInt(), dragAmount.y.toInt())
                }
            }
    ) {
        if (!isExpanded) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = StitchCardDark,
                border = BorderStroke(width = 1.5.dp, color = themeColor),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .clickable { if (onExpandRequested != null) onExpandRequested() else isExpanded = true }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = com.trustmesh.app.R.drawable.ic_trinetra_orbit_eye),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "TriNetra: ${effectiveRiskLevel.displayName}",
                        color = themeColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                    val identifier = if (scan.hasScore) {
                        scan.headline
                    } else {
                        callerIdentity?.displayName ?: fallbackName.ifBlank { callerIdentity?.phoneNumber ?: fallbackNumber }
                    }
                    if (identifier.isNotBlank()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "• $identifier",
                            color = StitchTextPrimary,
                            fontSize = 12.sp,
                            maxLines = 1
                        )
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .width(300.dp)
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                PremiumNotificationOverlayCard(
                    callerIdentity = callerIdentity,
                    callerReputation = null,
                    fallbackName = fallbackName,
                    fallbackNumber = fallbackNumber,
                    riskLevel = riskLevel,
                    riskAssessment = riskAssessment,
                    activeIncident = activeIncident,
                    interaction = interaction,
                    scan = scan,
                    onDismiss = onDismiss,
                    onCollapse = {
                        if (onExpandRequested != null) {
                            onDismiss()
                        } else {
                            isExpanded = false
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CallSummaryOverlay(
    interaction: com.trustmesh.app.interaction.Interaction?,
    activeIncident: com.trustmesh.app.core.incident.SecurityIncident?,
    activeCallDurationSeconds: Long,
    scan: VoiceScanState = VoiceScanState(),
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    Surface(
        modifier = Modifier
            .padding(16.dp)
            .width(320.dp),
        shape = RoundedCornerShape(24.dp),
        color = StitchCardDark,
        shadowElevation = 16.dp,
        border = BorderStroke(width = 1.5.dp, color = StitchGlassBorder)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Image(
                    painter = painterResource(id = com.trustmesh.app.R.drawable.ic_trinetra_orbit_eye),
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TriNetra Call Summary",
                    color = StitchTextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(16.dp))

            val callerIdentity = interaction?.callerIdentity
            val displayName = callerIdentity?.displayName ?: "Unknown Name"
            val phoneNumber = callerIdentity?.phoneNumber ?: interaction?.title ?: "Not available"

            Text("Caller:", color = StitchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(displayName, color = StitchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text(phoneNumber, color = StitchTextMuted, fontSize = 14.sp)
            Spacer(modifier = Modifier.height(10.dp))

            val dateTime = interaction?.timestamp ?: "Not available"
            Text("Date / Time:", color = StitchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(dateTime, color = StitchTextPrimary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))

            val durationText = if (activeCallDurationSeconds > 0) "$activeCallDurationSeconds seconds" else "Not available"
            Text("Call Duration:", color = StitchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(durationText, color = StitchTextPrimary, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(10.dp))

            val riskLevel = scan.effectiveRiskLevel(interaction?.riskLevel ?: RiskLevel.LOW)
            val score = displayScore(scan, interaction?.riskAssessment)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Risk Level:", color = StitchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        text = riskLevel.displayName,
                        color = riskColor(riskLevel),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Risk Score:", color = StitchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text("$score / 100", color = StitchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))

            RealtimeRiskGraph(
                currentScore = score,
                riskAssessment = interaction?.riskAssessment,
                scan = scan
            )
            Spacer(modifier = Modifier.height(10.dp))

            if (scan.hasScore) {
                VoiceAnalysisPanel(scan)
                Spacer(modifier = Modifier.height(10.dp))
            }

            val rep = interaction?.callerReputation
            if (rep != null && rep.reputationLevel != com.trustmesh.app.core.identity.ReputationLevel.UNKNOWN) {
                Text("Reputation:", color = StitchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${rep.reputationLevel.name.replace("_", " ")} (${rep.source})",
                    color = if (rep.reputationLevel == com.trustmesh.app.core.identity.ReputationLevel.HIGH_RISK) StitchAlertRed else StitchWarningAmber,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            if (activeIncident != null) {
                Text("Security Incident:", color = StitchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "${activeIncident.incidentType.name.replace("_", " ")} [${activeIncident.severity}]",
                    color = StitchAlertRed,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            val explanation = interaction?.riskAssessment?.explanation ?: "No details available."
            Text("Explanation:", color = StitchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(explanation, color = StitchTextPrimary, fontSize = 12.sp, maxLines = 3)
            Spacer(modifier = Modifier.height(8.dp))

            val factors = interaction?.riskAssessment?.factors ?: emptyList()
            if (factors.isNotEmpty()) {
                Text("Risk Factors:", color = StitchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                factors.take(3).forEach { factor ->
                    Text("• ${factor.description}", color = StitchTextPrimary, fontSize = 11.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            val groqResp = interaction?.groqResponse
            if (groqResp != null) {
                Text("AI Semantic Threat Intelligence (Groq):", color = StitchPrimaryTeal, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Text("• Category: ${groqResp.scamCategory.replace("_", " ")} (${groqResp.confidence} confidence)", color = StitchTextPrimary, fontSize = 11.sp)
                if (groqResp.psychologicalTriggers.isNotEmpty()) {
                    Text("• Triggers: ${groqResp.psychologicalTriggers.joinToString(", ")}", color = StitchTextMuted, fontSize = 11.sp)
                }
                if (groqResp.summaryReasoning.isNotBlank()) {
                    Text("• ${groqResp.summaryReasoning}", color = StitchTextPrimary, fontSize = 11.sp, maxLines = 2)
                }
                Spacer(modifier = Modifier.height(10.dp))
            }

            val whatTrustMeshDid = if (riskLevel == RiskLevel.CRITICAL) {
                "Monitored call context and recommended full security intervention."
            } else {
                "Monitored call interaction locally and verified caller reputation."
            }
            Text("Action Taken:", color = StitchTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            Text(whatTrustMeshDid, color = StitchTextPrimary, fontSize = 12.sp)
            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "🛡 Zero call audio was recorded or processed locally or externally.",
                color = StitchMintGlow,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("Close", color = StitchTextMuted)
                }

                Button(
                    onClick = {
                        val interactionId = interaction?.id ?: ""
                        if (interactionId.isNotBlank()) {
                            val intent = android.content.Intent(context, MainActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("navigate_to", "report/$interactionId")
                            }
                            context.startActivity(intent)
                        } else {
                            val intent = android.content.Intent(context, MainActivity::class.java).apply {
                                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                                putExtra("navigate_to", "history")
                            }
                            context.startActivity(intent)
                        }
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryTeal),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                ) {
                    Text("View Report", color = StitchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
fun CompactFloatingOverlay(
    callerIdentity: CallerIdentity?,
    callerReputation: com.trustmesh.app.core.identity.CallerReputation?,
    fallbackName: String,
    fallbackNumber: String,
    riskLevel: RiskLevel,
    riskAssessment: com.trustmesh.app.core.intelligence.risk.RiskAssessment?,
    activeIncident: com.trustmesh.app.core.incident.SecurityIncident?,
    scan: VoiceScanState = VoiceScanState(),
    onDismiss: () -> Unit
) {
    val effectiveRiskLevel = scan.effectiveRiskLevel(riskLevel)
    val themeColor = riskColor(effectiveRiskLevel)

    Surface(
        modifier = Modifier.padding(16.dp),
        shape = RoundedCornerShape(16.dp),
        color = StitchCardDark,
        border = BorderStroke(width = 1.5.dp, color = themeColor),
        shadowElevation = 8.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🛡 TriNetra Call Security",
                    color = StitchPrimaryTeal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                val title = if (scan.hasScore) {
                    scan.headline
                } else if (callerIdentity?.isKnown == true) {
                    callerIdentity.displayName ?: fallbackName.ifBlank { "Unknown" }
                } else {
                    fallbackName.ifBlank { "Unknown Caller" }
                }
                Text(title, color = StitchTextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = effectiveRiskLevel.displayName,
                    color = themeColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = StitchGlassBorder),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Text("Dismiss", color = StitchTextPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun FloatingRiskCard(
    callerIdentity: CallerIdentity?,
    callerReputation: com.trustmesh.app.core.identity.CallerReputation?,
    fallbackName: String,
    fallbackNumber: String,
    riskLevel: RiskLevel,
    riskAssessment: com.trustmesh.app.core.intelligence.risk.RiskAssessment?,
    activeIncident: com.trustmesh.app.core.incident.SecurityIncident?,
    interaction: com.trustmesh.app.interaction.Interaction? = null,
    scan: VoiceScanState = VoiceScanState(),
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(320.dp)
            .padding(8.dp)
            .heightIn(max = 600.dp)
            .verticalScroll(rememberScrollState())
    ) {
        PremiumNotificationOverlayCard(
            callerIdentity = callerIdentity,
            callerReputation = callerReputation,
            fallbackName = fallbackName,
            fallbackNumber = fallbackNumber,
            riskLevel = riskLevel,
            riskAssessment = riskAssessment,
            activeIncident = activeIncident,
            interaction = interaction,
            scan = scan,
            onDismiss = onDismiss,
            onCollapse = null
        )
    }
}

@Composable
fun BottomRiskSheet(
    callerIdentity: CallerIdentity?,
    callerReputation: com.trustmesh.app.core.identity.CallerReputation?,
    fallbackName: String,
    fallbackNumber: String,
    riskLevel: RiskLevel,
    riskAssessment: com.trustmesh.app.core.intelligence.risk.RiskAssessment?,
    activeIncident: com.trustmesh.app.core.incident.SecurityIncident?,
    interaction: com.trustmesh.app.interaction.Interaction? = null,
    scan: VoiceScanState = VoiceScanState(),
    onDismiss: () -> Unit
) {
    val themeColor = riskColor(scan.effectiveRiskLevel(riskLevel))

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        color = StitchCardDark,
        shadowElevation = 16.dp,
        border = BorderStroke(width = 1.5.dp, color = themeColor)
    ) {
        Column(
            modifier = Modifier
                .padding(18.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PremiumNotificationOverlayCard(
                callerIdentity = callerIdentity,
                callerReputation = callerReputation,
                fallbackName = fallbackName,
                fallbackNumber = fallbackNumber,
                riskLevel = riskLevel,
                riskAssessment = riskAssessment,
                activeIncident = activeIncident,
                interaction = interaction,
                scan = scan,
                onDismiss = onDismiss,
                onCollapse = null
            )
        }
    }
}

@Composable
fun FullScreenSecurityOverlay(
    callerIdentity: CallerIdentity?,
    callerReputation: com.trustmesh.app.core.identity.CallerReputation?,
    fallbackName: String,
    fallbackNumber: String,
    riskLevel: RiskLevel,
    riskAssessment: com.trustmesh.app.core.intelligence.risk.RiskAssessment?,
    activeIncident: com.trustmesh.app.core.incident.SecurityIncident?,
    interaction: com.trustmesh.app.interaction.Interaction? = null,
    scan: VoiceScanState = VoiceScanState(),
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBgDark.copy(alpha = 0.85f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(340.dp)
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState())
        ) {
            PremiumNotificationOverlayCard(
                callerIdentity = callerIdentity,
                callerReputation = callerReputation,
                fallbackName = fallbackName,
                fallbackNumber = fallbackNumber,
                riskLevel = riskLevel,
                riskAssessment = riskAssessment,
                activeIncident = activeIncident,
                interaction = interaction,
                scan = scan,
                onDismiss = onDismiss,
                onCollapse = null
            )
        }
    }
}
