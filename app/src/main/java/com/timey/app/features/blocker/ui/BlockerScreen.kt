package com.timey.app.features.blocker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timey.app.core.blocker.InstalledAppInfo
import com.timey.app.core.blocker.InstalledAppProvider
import com.timey.app.core.blocker.PermissionHelper
import com.timey.app.features.timer.viewmodel.TimerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun BlockerScreen(
    viewModel: TimerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = MaterialTheme.colorScheme
    val uiState by viewModel.uiState.collectAsState()

    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }
    var showPinDialog by remember { mutableStateOf(false) }
    var pinValue by remember { mutableStateOf("") }

    // Permission check states
    var isAccessibilityGranted by remember { mutableStateOf(PermissionHelper.isAccessibilityServiceEnabled(context)) }
    var isExactAlarmGranted by remember { mutableStateOf(PermissionHelper.isExactAlarmAllowed(context)) }
    var isBatteryOptIgnored by remember { mutableStateOf(PermissionHelper.isBatteryOptimizationIgnored(context)) }

    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = InstalledAppProvider.getInstalledLaunchableApps(context)
            withContext(Dispatchers.Main) {
                installedApps = apps
            }
        }
    }

    val filteredApps = remember(installedApps, searchQuery) {
        if (searchQuery.isBlank()) {
            installedApps
        } else {
            installedApps.filter {
                it.appName.contains(searchQuery, ignoreCase = true) ||
                it.packageName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 18.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Screen Header
        Text(
            text = "Focus Shield & App Blocker",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = colors.onBackground
        )
        Text(
            text = "Prevent opening distracting apps during study blocks",
            fontSize = 12.sp,
            color = colors.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Master Controls Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Master Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.appBlockingEnabled) colors.primary.copy(alpha = 0.15f) else colors.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = "Shield",
                                        tint = if (uiState.appBlockingEnabled) colors.primary else colors.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Block Distracting Apps",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = if (uiState.appBlockingEnabled) "Active during study sessions" else "Disabled",
                                        fontSize = 12.sp,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = uiState.appBlockingEnabled,
                                onCheckedChange = { viewModel.toggleAppBlocking(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = colors.onPrimary,
                                    checkedTrackColor = colors.primary
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // PIN Lock Toggle
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(if (uiState.pinProtectionEnabled) colors.secondary.copy(alpha = 0.15f) else colors.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Lock,
                                        contentDescription = "PIN Lock",
                                        tint = if (uiState.pinProtectionEnabled) colors.secondary else colors.onSurfaceVariant,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "PIN Password Protection",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface
                                    )
                                    Text(
                                        text = if (uiState.pinProtectionEnabled) "Requires PIN to exit or bypass" else "Off",
                                        fontSize = 11.sp,
                                        color = colors.onSurfaceVariant
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (uiState.pinProtectionEnabled) {
                                    TextButton(onClick = { showPinDialog = true }) {
                                        Text("Edit PIN", fontSize = 12.sp, color = colors.primary)
                                    }
                                }
                                Switch(
                                    checked = uiState.pinProtectionEnabled,
                                    onCheckedChange = { enabled ->
                                        if (enabled) {
                                            showPinDialog = true
                                        } else {
                                            viewModel.updatePinProtection(false, "")
                                        }
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = colors.onPrimary,
                                        checkedTrackColor = colors.secondary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Permission Status & Onboarding Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colors.surface)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = "Permission Health Check",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )
                        Text(
                            text = "Android requires explicit permissions to detect app launches and keep timers reliable",
                            fontSize = 11.sp,
                            color = colors.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Accessibility Item
                        PermissionRowItem(
                            title = "Accessibility Blocker Service",
                            subtitle = if (isAccessibilityGranted) "Active • Instant distraction intercept" else "Required for immediate app blocking",
                            isGranted = isAccessibilityGranted,
                            onFix = {
                                PermissionHelper.openAccessibilitySettings(context)
                                isAccessibilityGranted = PermissionHelper.isAccessibilityServiceEnabled(context)
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Exact Alarm Item
                        PermissionRowItem(
                            title = "Exact Hardware Alarms",
                            subtitle = if (isExactAlarmGranted) "Guaranteed alarm clocks even in Doze mode" else "Tap to allow zero-drift exact alarms",
                            isGranted = isExactAlarmGranted,
                            onFix = {
                                PermissionHelper.openExactAlarmSettings(context)
                                isExactAlarmGranted = PermissionHelper.isExactAlarmAllowed(context)
                            }
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        // Battery Optimization Item
                        PermissionRowItem(
                            title = "Battery Optimization Exemption",
                            subtitle = if (isBatteryOptIgnored) "Unrestricted background timer operation" else "Prevent OEM systems from stopping timer",
                            isGranted = isBatteryOptIgnored,
                            onFix = {
                                PermissionHelper.openBatteryOptimizationSettings(context)
                                isBatteryOptIgnored = PermissionHelper.isBatteryOptimizationIgnored(context)
                            }
                        )
                    }
                }
            }

            // Apps List Header & Quick Actions
            item {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Restricted Apps (${uiState.blockedPackages.size} selected)",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.onSurface
                        )

                        Row {
                            TextButton(
                                onClick = {
                                    val socialPackages = installedApps
                                        .filter { it.isDistractionRisk }
                                        .map { it.packageName }
                                        .toSet()
                                    viewModel.setBlockedPackages(socialPackages)
                                }
                            ) {
                                Text("Popular", fontSize = 12.sp, color = colors.primary)
                            }

                            TextButton(
                                onClick = { viewModel.setBlockedPackages(emptySet()) }
                            ) {
                                Text("Clear", fontSize = 12.sp, color = colors.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Search TextField
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Search installed apps...", fontSize = 13.sp) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = colors.onSurfaceVariant)
                        },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.outline.copy(alpha = 0.35f),
                            focusedContainerColor = colors.surface,
                            unfocusedContainerColor = colors.surface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Apps List
            items(filteredApps) { app ->
                val isBlocked = uiState.blockedPackages.contains(app.packageName)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isBlocked) colors.primary.copy(alpha = 0.08f) else colors.surface)
                        .border(
                            1.dp,
                            if (isBlocked) colors.primary.copy(alpha = 0.4f) else colors.outline.copy(alpha = 0.15f),
                            RoundedCornerShape(12.dp)
                        )
                        .clickable { viewModel.toggleBlockedPackage(app.packageName) }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Checkbox(
                                checked = isBlocked,
                                onCheckedChange = { viewModel.toggleBlockedPackage(app.packageName) },
                                colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = app.appName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colors.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (app.isDistractionRisk) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(colors.error.copy(alpha = 0.15f))
                                                .padding(horizontal = 4.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "Distraction",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = colors.error
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = app.packageName,
                                    fontSize = 11.sp,
                                    color = colors.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Set / Edit PIN Dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = {
                showPinDialog = false
                pinValue = ""
            },
            title = {
                Text(text = "Set 4-Digit Security PIN", fontWeight = FontWeight.Bold)
            },
            text = {
                Column {
                    Text(
                        text = "This PIN will be required to end focus sessions early or access blocked apps:",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pinValue,
                        onValueChange = { if (it.length <= 4 && it.all { ch -> ch.isDigit() }) pinValue = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        placeholder = { Text("e.g. 1234") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (pinValue.isNotBlank()) {
                            viewModel.updatePinProtection(true, pinValue)
                        } else {
                            viewModel.updatePinProtection(true, "1234")
                        }
                        showPinDialog = false
                        pinValue = ""
                    }
                ) {
                    Text("Save PIN")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PermissionRowItem(
    title: String,
    subtitle: String,
    isGranted: Boolean,
    onFix: () -> Unit
) {
    val colors = MaterialTheme.colorScheme

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (isGranted) colors.primary else colors.error,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onSurface
                )
                Text(
                    text = subtitle,
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant
                )
            }
        }

        if (!isGranted) {
            Button(
                onClick = onFix,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("Enable", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
