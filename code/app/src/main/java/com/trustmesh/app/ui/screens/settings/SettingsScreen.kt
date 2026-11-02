package com.trustmesh.app.ui.screens.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.trustmesh.app.core.firewall.OverlayPermissionHelper
import com.trustmesh.app.core.protection.ProtectionAction
import com.trustmesh.app.core.protection.ProtectionMode
import com.trustmesh.app.core.settings.TrustMeshSettings
import com.trustmesh.app.data.local.TrustMeshDatabase
import com.trustmesh.app.data.local.entity.ProtectionPolicyEntity
import com.trustmesh.app.data.local.entity.TrustedCallerEntity
import com.trustmesh.app.data.repository.ProtectionPolicyRepository
import com.trustmesh.app.firewall.NotificationAccessHelper
import com.trustmesh.app.firewall.RoleManagerHelper
import com.trustmesh.app.ui.components.TrustMeshTopBar
import com.trustmesh.app.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(onBackClick: () -> Unit = {}) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    var isCallScreeningActive by remember { mutableStateOf(RoleManagerHelper.isCallScreeningRoleGranted(context)) }
    var isNotificationAccessGranted by remember { mutableStateOf(NotificationAccessHelper.isNotificationAccessGranted(context)) }
    var isOverlayPermissionGranted by remember { mutableStateOf(OverlayPermissionHelper.hasOverlayPermission(context)) }

    var isContactsPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isCallLogPermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isPhoneStatePermissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isPostNotificationsPermissionGranted by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    val db = remember { TrustMeshDatabase.getDatabase(context) }
    val policyRepository = remember { ProtectionPolicyRepository(db.protectionPolicyDao(), db.trustedCallerDao()) }

    val policy by policyRepository.getPolicyFlow().collectAsState(initial = ProtectionPolicyEntity.default())
    val trustedCallers by policyRepository.getTrustedCallersFlow().collectAsState(initial = emptyList())

    var showCustomizer by remember { mutableStateOf(false) }
    var showTrustedCallersDialog by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isCallScreeningActive = RoleManagerHelper.isCallScreeningRoleGranted(context)
                isNotificationAccessGranted = NotificationAccessHelper.isNotificationAccessGranted(context)
                isOverlayPermissionGranted = OverlayPermissionHelper.hasOverlayPermission(context)
                
                val wasContactsGranted = isContactsPermissionGranted
                val wasCallLogGranted = isCallLogPermissionGranted
                
                isContactsPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
                isCallLogPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALL_LOG) == PackageManager.PERMISSION_GRANTED
                isPhoneStatePermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    isPostNotificationsPermissionGranted = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
                }
                
                if (isCallLogPermissionGranted && !wasCallLogGranted) {
                    com.trustmesh.app.interaction.InteractionManager.loadRealCallLogs(context)
                }
                if (isContactsPermissionGranted && !wasContactsGranted) {
                    com.trustmesh.app.interaction.InteractionManager.loadRealContacts(context)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val roleLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isCallScreeningActive = RoleManagerHelper.isCallScreeningRoleGranted(context)
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        isNotificationAccessGranted = NotificationAccessHelper.isNotificationAccessGranted(context)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val newContactsGranted = permissions[Manifest.permission.READ_CONTACTS] ?: isContactsPermissionGranted
        val newCallLogGranted = permissions[Manifest.permission.READ_CALL_LOG] ?: isCallLogPermissionGranted
        
        if (newCallLogGranted && !isCallLogPermissionGranted) {
            com.trustmesh.app.interaction.InteractionManager.loadRealCallLogs(context)
        }
        if (newContactsGranted && !isContactsPermissionGranted) {
            com.trustmesh.app.interaction.InteractionManager.loadRealContacts(context)
        }
        
        isContactsPermissionGranted = newContactsGranted
        isCallLogPermissionGranted = newCallLogGranted
        isPhoneStatePermissionGranted = permissions[Manifest.permission.READ_PHONE_STATE] ?: isPhoneStatePermissionGranted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            isPostNotificationsPermissionGranted = permissions[Manifest.permission.POST_NOTIFICATIONS] ?: isPostNotificationsPermissionGranted
        }
    }

    if (showTrustedCallersDialog) {
        TrustedCallersDialog(
            trustedCallers = trustedCallers,
            onAddCaller = { number, name ->
                scope.launch { policyRepository.addTrustedCaller(number, name) }
            },
            onRemoveCaller = { number ->
                scope.launch { policyRepository.removeTrustedCaller(number) }
            },
            onDismiss = { showTrustedCallersDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TrustMeshTopBar(title = "Settings", onBackClick = onBackClick)
        },
        containerColor = StitchBgDark
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(StitchBgDark)
                .padding(paddingValues)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

        // ─── HEADER ────────────────────────────────────────────────────────
        item {
            Spacer(Modifier.height(12.dp))
            Text(
                "Settings & Configuration",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = StitchTextPrimary
            )
            Text(
                "Customize protection mode, privacy & access permissions",
                fontSize = 13.sp,
                color = StitchTextMuted,
                modifier = Modifier.padding(top = 2.dp, bottom = 20.dp)
            )
        }

        // ─── PROTECTION MODE ────────────────────────────────────────────────
        item {
            SectionHeader("🛡  Protection Mode")
            Spacer(Modifier.height(10.dp))

            ProtectionModeSelector(
                currentMode = policy.mode,
                onModeSelected = { selectedMode ->
                    scope.launch { policyRepository.setMode(selectedMode) }
                    showCustomizer = selectedMode == ProtectionMode.CUSTOM
                }
            )

            AnimatedVisibility(
                visible = policy.mode == ProtectionMode.CUSTOM,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                ProtectionCustomizer(
                    policy = policy,
                    onUpdate = { low, elevated, high, critical, unknown, autoBlock ->
                        scope.launch {
                            policyRepository.updateCustomPolicy(low, elevated, high, critical, unknown, autoBlock)
                        }
                    }
                )
            }
            Spacer(Modifier.height(20.dp))
        }

        // ─── TRUSTED CALLERS ────────────────────────────────────────────────
        item {
            SectionHeader("✅  Trusted Callers")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Callers in this list skip TrueVoice risk checks. Only add people you fully trust.",
                        color = StitchTextSecondary, fontSize = 13.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    if (trustedCallers.isEmpty()) {
                        Text("No trusted callers added yet.", color = StitchTextMuted, fontSize = 13.sp)
                    } else {
                        trustedCallers.take(3).forEach { caller ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(caller.name, color = StitchTextPrimary, fontWeight = FontWeight.Medium)
                                    Text(caller.phoneNumber, color = StitchTextMuted, fontSize = 12.sp)
                                }
                                TextButton(onClick = {
                                    scope.launch { policyRepository.removeTrustedCaller(caller.phoneNumber) }
                                }) {
                                    Text("Remove", color = StitchAlertRed, fontSize = 12.sp)
                                }
                            }
                        }
                        if (trustedCallers.size > 3) {
                            Text("+${trustedCallers.size - 3} more", color = StitchTextMuted, fontSize = 12.sp)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = { showTrustedCallersDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Manage Trusted Callers", color = StitchTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ─── PERMISSIONS ────────────────────────────────────────────────────
        item {
            SectionHeader("🔐  Permissions")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                Column(Modifier.padding(12.dp)) {
                    PermissionRow(
                        label = "Call Screening",
                        description = "Observe incoming call metadata",
                        granted = isCallScreeningActive,
                        onClick = {
                            if (!isCallScreeningActive) {
                                val intent = RoleManagerHelper.getCallScreeningRoleIntent(context)
                                if (intent != null) roleLauncher.launch(intent)
                            }
                        }
                    )
                    HorizontalDivider(color = StitchGlassBorder, modifier = Modifier.padding(vertical = 4.dp))
                    PermissionRow(
                        label = "Notification Monitoring",
                        description = "Observe notification signals",
                        granted = isNotificationAccessGranted,
                        onClick = {
                            if (!isNotificationAccessGranted) {
                                notificationLauncher.launch(NotificationAccessHelper.getNotificationAccessIntent())
                            }
                        }
                    )
                    HorizontalDivider(color = StitchGlassBorder, modifier = Modifier.padding(vertical = 4.dp))
                    PermissionRow(
                        label = "Overlay Permission",
                        description = "Show real-time protection UI",
                        granted = isOverlayPermissionGranted,
                        onClick = {
                            if (!isOverlayPermissionGranted) {
                                OverlayPermissionHelper.requestOverlayPermission(context)
                            }
                        }
                    )
                    HorizontalDivider(color = StitchGlassBorder, modifier = Modifier.padding(vertical = 4.dp))
                    PermissionRow(
                        label = "Contacts Access",
                        description = "Resolve contact names for incoming calls",
                        granted = isContactsPermissionGranted,
                        onClick = {
                            if (!isContactsPermissionGranted) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_CONTACTS))
                            }
                        }
                    )
                    HorizontalDivider(color = StitchGlassBorder, modifier = Modifier.padding(vertical = 4.dp))
                    PermissionRow(
                        label = "Call Logs Access",
                        description = "Read call logs to identify spam call history",
                        granted = isCallLogPermissionGranted,
                        onClick = {
                            if (!isCallLogPermissionGranted) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_CALL_LOG, Manifest.permission.WRITE_CALL_LOG))
                            }
                        }
                    )
                    HorizontalDivider(color = StitchGlassBorder, modifier = Modifier.padding(vertical = 4.dp))
                    PermissionRow(
                        label = "Phone State Access",
                        description = "Detect calls and read phone numbers",
                        granted = isPhoneStatePermissionGranted,
                        onClick = {
                            if (!isPhoneStatePermissionGranted) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.READ_PHONE_STATE))
                            }
                        }
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        HorizontalDivider(color = StitchGlassBorder, modifier = Modifier.padding(vertical = 4.dp))
                        PermissionRow(
                            label = "Post Notifications",
                            description = "Notify about risk updates and caller info",
                            granted = isPostNotificationsPermissionGranted,
                            onClick = {
                                if (!isPostNotificationsPermissionGranted) {
                                    permissionLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
                                }
                            }
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ─── PRIVACY & IDENTITY ─────────────────────────────────────────────
        item {
            SectionHeader("🔎  Privacy & Identity")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                Column(Modifier.padding(16.dp)) {
                    val isExternalLookupEnabled by TrustMeshSettings.isExternalLookupEnabled.collectAsState()
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("External Caller Lookup", color = StitchTextPrimary, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Query external caller directory for reputations",
                                color = StitchTextSecondary, fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = isExternalLookupEnabled,
                            onCheckedChange = { TrustMeshSettings.setExternalLookupEnabled(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = StitchMintGlow,
                                checkedTrackColor = StitchPrimaryTeal
                            )
                        )
                    }
                    if (isExternalLookupEnabled) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚠ Only the incoming phone number is queried. Your contacts are never uploaded.",
                            color = StitchWarningAmber, fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
        }

        // ─── PRIVACY GUARANTEE ──────────────────────────────────────────────
        item {
            SectionHeader("🔒  Privacy Guarantee")
            Spacer(Modifier.height(10.dp))
            SettingsCard {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PrivacyGuaranteeRow("✓", "Zero audio recording or processing")
                    PrivacyGuaranteeRow("✓", "No call content accessed")
                    PrivacyGuaranteeRow("✓", "Local-first data storage")
                    PrivacyGuaranteeRow("✓", "No contact data uploaded")
                    PrivacyGuaranteeRow("✓", "No Accessibility Service used")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
        }
    }
}

// ─── PROTECTION MODE SELECTOR ──────────────────────────────────────────────────

@Composable
fun ProtectionModeSelector(currentMode: ProtectionMode, onModeSelected: (ProtectionMode) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ProtectionModeCard(
            mode = ProtectionMode.STANDARD,
            title = "Standard",
            description = "Monitor + warn on high/critical risks. Recommended for most users.",
            selected = currentMode == ProtectionMode.STANDARD,
            accentColor = StitchMintGlow,
            onSelect = { onModeSelected(ProtectionMode.STANDARD) }
        )
        ProtectionModeCard(
            mode = ProtectionMode.STRICT,
            title = "Strict",
            description = "Elevated warnings at all risk levels. Auto-block critical callers.",
            selected = currentMode == ProtectionMode.STRICT,
            accentColor = StitchWarningAmber,
            onSelect = { onModeSelected(ProtectionMode.STRICT) }
        )
        ProtectionModeCard(
            mode = ProtectionMode.CUSTOM,
            title = "Custom",
            description = "Configure each risk level's behavior independently.",
            selected = currentMode == ProtectionMode.CUSTOM,
            accentColor = StitchPrimaryTeal,
            onSelect = { onModeSelected(ProtectionMode.CUSTOM) }
        )
    }
}

@Composable
fun ProtectionModeCard(
    mode: ProtectionMode,
    title: String,
    description: String,
    selected: Boolean,
    accentColor: Color,
    onSelect: () -> Unit
) {
    val borderColor = if (selected) accentColor else StitchGlassBorder
    val bgColor = if (selected) accentColor.copy(alpha = 0.12f) else StitchCardDark

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable { onSelect() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(selectedColor = accentColor, unselectedColor = StitchTextMuted)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = if (selected) accentColor else StitchTextPrimary, fontWeight = FontWeight.SemiBold)
            Text(description, color = StitchTextSecondary, fontSize = 12.sp)
        }
    }
}

// ─── CUSTOM POLICY EDITOR ──────────────────────────────────────────────────────

@Composable
fun ProtectionCustomizer(
    policy: ProtectionPolicyEntity,
    onUpdate: (ProtectionAction, ProtectionAction, ProtectionAction, ProtectionAction, ProtectionAction, Boolean) -> Unit
) {
    var lowRisk by remember(policy) { mutableStateOf(policy.lowRiskBehavior) }
    var elevatedRisk by remember(policy) { mutableStateOf(policy.elevatedRiskBehavior) }
    var highRisk by remember(policy) { mutableStateOf(policy.highRiskBehavior) }
    var criticalRisk by remember(policy) { mutableStateOf(policy.criticalRiskBehavior) }
    var unknownCaller by remember(policy) { mutableStateOf(policy.unknownCallerBehavior) }
    var autoBlock by remember(policy) { mutableStateOf(policy.autoBlockCritical) }

    SettingsCard(modifier = Modifier.padding(top = 8.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text("Custom Behavior", color = StitchMintGlow, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Spacer(Modifier.height(12.dp))

            PolicyActionRow("Low Risk", lowRisk, StitchMintGlow) { lowRisk = it; onUpdate(lowRisk, elevatedRisk, highRisk, criticalRisk, unknownCaller, autoBlock) }
            PolicyActionRow("Elevated Risk", elevatedRisk, StitchWarningAmber) { elevatedRisk = it; onUpdate(lowRisk, elevatedRisk, highRisk, criticalRisk, unknownCaller, autoBlock) }
            PolicyActionRow("High Risk", highRisk, Color(0xFFFF6D00)) { highRisk = it; onUpdate(lowRisk, elevatedRisk, highRisk, criticalRisk, unknownCaller, autoBlock) }
            PolicyActionRow("Critical Risk", criticalRisk, StitchAlertRed) { criticalRisk = it; onUpdate(lowRisk, elevatedRisk, highRisk, criticalRisk, unknownCaller, autoBlock) }
            PolicyActionRow("Unknown Caller", unknownCaller, StitchTextMuted) { unknownCaller = it; onUpdate(lowRisk, elevatedRisk, highRisk, criticalRisk, unknownCaller, autoBlock) }

            HorizontalDivider(color = StitchGlassBorder, modifier = Modifier.padding(vertical = 12.dp))

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Auto-block Critical", color = StitchTextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Requires explicit user opt-in", color = StitchAlertRed, fontSize = 11.sp)
                }
                Switch(
                    checked = autoBlock,
                    onCheckedChange = {
                        autoBlock = it
                        onUpdate(lowRisk, elevatedRisk, highRisk, criticalRisk, unknownCaller, autoBlock)
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = StitchAlertRed,
                        checkedTrackColor = StitchAlertRed.copy(alpha = 0.4f)
                    )
                )
            }
        }
    }
}

@Composable
fun PolicyActionRow(label: String, currentAction: ProtectionAction, dotColor: Color, onActionChanged: (ProtectionAction) -> Unit) {
    val displayableActions = listOf(
        ProtectionAction.MONITOR_ONLY to "Monitor Only",
        ProtectionAction.SHOW_COMPACT_WARNING to "Compact Warning",
        ProtectionAction.SHOW_RISK_CARD to "Risk Card",
        ProtectionAction.SHOW_BOTTOM_SHEET to "Bottom Sheet",
        ProtectionAction.SHOW_SECURITY_INTERVENTION to "Full Intervention",
        ProtectionAction.ASK_USER to "Ask User"
    )

    var expanded by remember { mutableStateOf(false) }
    val currentLabel = displayableActions.find { it.first == currentAction }?.second ?: currentAction.name

    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(8.dp).background(dotColor, shape = RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(8.dp))
            Text(label, color = StitchTextPrimary, fontSize = 14.sp)
        }
        Box {
            TextButton(onClick = { expanded = true }) {
                Text(currentLabel, color = StitchMintGlow, fontSize = 13.sp)
                Text(" ▾", color = StitchMintGlow, fontSize = 11.sp)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(StitchCardDark)
            ) {
                displayableActions.forEach { (action, name) ->
                    DropdownMenuItem(
                        text = { Text(name, color = if (action == currentAction) StitchMintGlow else StitchTextPrimary) },
                        onClick = { onActionChanged(action); expanded = false }
                    )
                }
            }
        }
    }
}

// ─── TRUSTED CALLERS DIALOG ────────────────────────────────────────────────────

@Composable
fun TrustedCallersDialog(
    trustedCallers: List<TrustedCallerEntity>,
    onAddCaller: (String, String) -> Unit,
    onRemoveCaller: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var newNumber by remember { mutableStateOf("") }
    var newName by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StitchCardDark,
        title = { Text("Trusted Callers", color = StitchTextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    "These callers bypass TrueVoice risk analysis.",
                    color = StitchTextSecondary, fontSize = 13.sp
                )
                Spacer(Modifier.height(12.dp))

                if (trustedCallers.isEmpty()) {
                    Text("No trusted callers.", color = StitchTextMuted, fontSize = 13.sp)
                } else {
                    trustedCallers.forEach { caller ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(caller.name, color = StitchTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                Text(caller.phoneNumber, color = StitchTextMuted, fontSize = 12.sp)
                            }
                            TextButton(onClick = { onRemoveCaller(caller.phoneNumber) }) {
                                Text("✕", color = StitchAlertRed)
                            }
                        }
                    }
                }

                HorizontalDivider(color = StitchGlassBorder, modifier = Modifier.padding(vertical = 8.dp))

                Text("Add Trusted Caller", color = StitchMintGlow, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Name", color = StitchTextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StitchMintGlow,
                        unfocusedBorderColor = StitchGlassBorder,
                        focusedTextColor = StitchTextPrimary,
                        unfocusedTextColor = StitchTextPrimary
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = newNumber,
                    onValueChange = { newNumber = it },
                    label = { Text("Phone number", color = StitchTextMuted, fontSize = 12.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = StitchMintGlow,
                        unfocusedBorderColor = StitchGlassBorder,
                        focusedTextColor = StitchTextPrimary,
                        unfocusedTextColor = StitchTextPrimary
                    )
                )
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = {
                        if (newNumber.isNotBlank() && newName.isNotBlank()) {
                            onAddCaller(newNumber.trim(), newName.trim())
                            newNumber = ""
                            newName = ""
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = StitchPrimaryTeal),
                    enabled = newNumber.isNotBlank() && newName.isNotBlank()
                ) {
                    Text("Add Caller", color = StitchTextPrimary, fontWeight = FontWeight.Bold)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done", color = StitchMintGlow) }
        }
    )
}

// ─── SHARED COMPONENTS ─────────────────────────────────────────────────────────

@Composable
fun SectionHeader(text: String) {
    Text(text, color = StitchTextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
}

@Composable
fun SettingsCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StitchCardDark,
        border = androidx.compose.foundation.BorderStroke(1.dp, StitchGlassBorder)
    ) {
        content()
    }
}

@Composable
fun PermissionRow(label: String, description: String, granted: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !granted) { onClick() }
            .padding(vertical = 12.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(label, color = StitchTextPrimary, fontWeight = FontWeight.Medium)
            Text(description, color = StitchTextSecondary, fontSize = 12.sp)
        }
        if (granted) {
            Text("✓  Granted", color = StitchMintGlow, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        } else {
            Text("Enable →", color = StitchWarningAmber, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
    }
}

@Composable
fun PrivacyGuaranteeRow(icon: String, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(icon, color = StitchMintGlow, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Spacer(Modifier.width(8.dp))
        Text(text, color = StitchTextSecondary, fontSize = 13.sp)
    }
}
