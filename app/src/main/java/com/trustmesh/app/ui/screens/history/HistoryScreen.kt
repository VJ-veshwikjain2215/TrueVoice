package com.trustmesh.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.trustmesh.app.interaction.InteractionManager
import com.trustmesh.app.core.incident.SecurityIncidentManager
import com.trustmesh.app.ui.components.InteractionCard
import com.trustmesh.app.ui.components.IncidentCard
import com.trustmesh.app.ui.theme.*

@Composable
fun HistoryScreen(onInteractionClick: (String) -> Unit) {
    val interactions by InteractionManager.interactions.collectAsState()
    val incidents by SecurityIncidentManager.incidents.collectAsState()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                InteractionManager.loadRealCallLogs(context)
                InteractionManager.loadRealContacts(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(StitchBgDark)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Interaction History",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = StitchTextPrimary
        )
        Text(
            text = "Recent call logs & threat incidents",
            fontSize = 13.sp,
            color = StitchTextMuted,
            modifier = Modifier.padding(top = 2.dp)
        )
        
        Spacer(modifier = Modifier.height(20.dp))
        
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (incidents.isNotEmpty()) {
                item {
                    Text(
                        text = "SECURITY INCIDENTS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        color = StitchAlertRed,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
                items(incidents) { incident ->
                    IncidentCard(incident = incident, onClick = { onInteractionClick(incident.incidentId) })
                }
                item {
                    Spacer(modifier = Modifier.height(10.dp))
                }
            }

            item {
                Text(
                    text = "ALL INTERACTIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = StitchMintGlow,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            items(interactions) { interaction ->
                InteractionCard(interaction = interaction, onClick = { onInteractionClick(interaction.id) })
            }
        }
    }
}
