/*
 * Copyright (C) 2025 AxionOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.settings.display

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.provider.Settings
import android.os.SystemProperties
import android.hardware.display.DisplayManager
import android.view.Display
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.Fragment
import com.android.axion.compose.theme.AxionTheme
import com.android.settings.R
import kotlin.math.absoluteValue

class RefreshRateSettings : Fragment() {

    enum class Page {
        MAIN, PER_APP
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                AxionTheme {
                    RefreshRateScreen()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        activity?.title = getString(R.string.refresh_rate_settings_title)
    }

    @Composable
    fun RefreshRateScreen() {
        var currentPage by remember { mutableStateOf(Page.MAIN) }
        
        BackHandler(enabled = currentPage != Page.MAIN) {
            currentPage = Page.MAIN
        }

        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState == Page.PER_APP) {
                    slideIntoContainer(
                        towards = SlideDirection.Left,
                        animationSpec = tween(300)
                    ) togetherWith slideOutOfContainer(
                        towards = SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                } else {
                    slideIntoContainer(
                        towards = SlideDirection.Right,
                        animationSpec = tween(300)
                    ) togetherWith slideOutOfContainer(
                        towards = SlideDirection.Right,
                        animationSpec = tween(300)
                    )
                }
            },
            label = "ScreenTransition"
        ) { page ->
            when (page) {
                Page.MAIN -> RefreshRateSettingsScreen(onNavigateToPerApp = { currentPage = Page.PER_APP })
                Page.PER_APP -> PerAppRefreshRateScreen(onBack = { currentPage = Page.MAIN })
            }
        }
    }

    @Composable
    fun RefreshRateSettingsScreen(onNavigateToPerApp: () -> Unit) {
        val context = LocalContext.current
        val cr = context.contentResolver
        
        var selectedMode by remember { 
            mutableStateOf(Settings.Global.getInt(cr, "display_refresh_rate_mode", 0)) 
        }
        
        var lockscreenLimitEnabled by remember {
            mutableStateOf(Settings.System.getInt(cr, "lockscreen_limit_refresh_rate", 0) != 0)
        }

        val modes = remember { getModes(context) }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                item {
                    RefreshRateIllustration(modes)
                }

                item {
                    SettingsGroupCard {
                        modes.forEachIndexed { index, pair ->
                            val (hz, label) = pair
                            SettingsRadioItem(
                                title = label,
                                selected = selectedMode == hz,
                                showDivider = index < modes.size - 1,
                                onClick = {
                                    selectedMode = hz
                                    Settings.Global.putInt(cr, "display_refresh_rate_mode", hz)
                                }
                            )
                        }
                    }
                }

                item {
                    Text(
                        text = stringResource(R.string.category_name_display_controls),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(start = 8.dp, top = 8.dp)
                    )
                }

                item {
                    SettingsGroupCard {
                        SettingsSwitchItem(
                            title = stringResource(R.string.aod_limit_refresh_rate_title),
                            summary = stringResource(R.string.aod_limit_refresh_rate_summary),
                            checked = lockscreenLimitEnabled,
                            showDivider = true,
                            onClick = {
                                lockscreenLimitEnabled = !lockscreenLimitEnabled
                                Settings.System.putInt(cr, "lockscreen_limit_refresh_rate", if (lockscreenLimitEnabled) 1 else 0)
                            }
                        )
                        SettingsNavigationItem(
                            title = stringResource(R.string.per_app_refresh_rate_title),
                            summary = stringResource(R.string.per_app_refresh_rate_summary),
                            onClick = onNavigateToPerApp
                        )
                    }
                }
            }
        }
    }

    @Composable
    fun PerAppRefreshRateScreen(onBack: () -> Unit) {
        val context = LocalContext.current
        val cr = context.contentResolver
        val pm = context.packageManager

        var searchQuery by remember { mutableStateOf("") }
        var perAppConfig by remember { mutableStateOf(getPerAppConfig(cr)) }
        var selectedApp by remember { mutableStateOf<AppInfo?>(null) }
        var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
        var isLoading by remember { mutableStateOf(true) }

        LaunchedEffect(Unit) {
            withContext(Dispatchers.IO) {
                val mainLauncherIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                }
                
                val launcherApps = pm.queryIntentActivities(mainLauncherIntent, PackageManager.MATCH_ALL)
                    .mapNotNull { it.activityInfo.packageName }
                    .toSet()
                
                val excludedPackages = setOf("com.android.launcher3", "com.android.settings")
                
                val loadedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
                    .filter { 
                        launcherApps.contains(it.packageName) && !excludedPackages.contains(it.packageName)
                    }
                    .map { AppInfo(it.packageName, it.loadLabel(pm).toString(), it.loadIcon(pm)) }
                    .sortedBy { it.label }
                
                withContext(Dispatchers.Main) {
                    apps = loadedApps
                    isLoading = false
                }
            }
        }

        val filteredApps = remember(searchQuery, apps) {
            if (searchQuery.isEmpty()) apps else apps.filter { 
                it.label.contains(searchQuery, ignoreCase = true) || 
                it.packageName.contains(searchQuery, ignoreCase = true) 
            }
        }

        val modes = remember { getModes(context) }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            topBar = {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainer,
                    modifier = Modifier.statusBarsPadding()
                ) {
                    TextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        placeholder = { Text("Search apps...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        shape = RoundedCornerShape(24.dp),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceBright,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceBright
                        )
                    )
                }
            }
        ) { paddingValues ->
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(bottom = 32.dp)
                ) {
                    items(filteredApps) { app ->
                        val appConfig = perAppConfig[app.packageName]
                        AppItem(
                            app = app,
                            config = appConfig,
                            onClick = { selectedApp = app }
                        )
                    }
                }
            }
        }

        if (selectedApp != null) {
            val appConfig = perAppConfig[selectedApp!!.packageName]
            var tempRate by remember { mutableStateOf(appConfig?.rate ?: 0) }

            AlertDialog(
                onDismissRequest = { selectedApp = null },
                title = { Text(selectedApp!!.label) },
                text = {
                    Column {
                        SettingsRadioItem(
                            title = "Default",
                            selected = tempRate == 0,
                            showDivider = true,
                            onClick = {
                                perAppConfig = perAppConfig.toMutableMap().apply { remove(selectedApp!!.packageName) }
                                savePerAppConfig(cr, perAppConfig)
                                selectedApp = null
                            }
                        )
                        
                        modes.filter { it.first > 0 }.forEachIndexed { index, pair ->
                            val (hz, label) = pair
                            SettingsRadioItem(
                                title = label,
                                selected = tempRate == hz,
                                showDivider = index < modes.size - 2,
                                onClick = { tempRate = hz }
                            )
                        }
                    }
                },
                confirmButton = {
                    if (tempRate > 0) {
                        TextButton(onClick = {
                            perAppConfig = perAppConfig.toMutableMap().apply {
                                put(selectedApp!!.packageName, AppConfig(tempRate))
                            }
                            savePerAppConfig(cr, perAppConfig)
                            selectedApp = null
                        }) {
                            Text("Save")
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = { selectedApp = null }) {
                        Text("Cancel")
                    }
                },
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surfaceBright
            )
        }
    }

    data class AppInfo(val packageName: String, val label: String, val icon: Drawable)
    data class AppConfig(val rate: Int)

    @Composable
    fun AppItem(app: AppInfo, config: AppConfig?, onClick: () -> Unit) {
        Column(modifier = Modifier.clickable(onClick = onClick)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = app.icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (config == null) "Default" else "${config.rate}Hz",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (config == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.primary,
                        fontWeight = if (config == null) FontWeight.Normal else FontWeight.Bold
                    )
                }
            }
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.surfaceContainer
            )
        }
    }

    private fun getModes(context: Context): List<Pair<Int, String>> {
        val modes = mutableListOf<Pair<Int, String>>()
        val customList = SystemProperties.get("persist.sys.display_refresh_rates_list", "")
        val refreshRates = if (customList.isNotEmpty()) {
            customList.split(",").mapNotNull { it.trim().toFloatOrNull()?.toInt() }.distinct().sorted()
        } else {
            val displayManager = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
            val display = displayManager.getDisplay(Display.DEFAULT_DISPLAY)
            display?.supportedModes?.map { it.refreshRate.toInt() }?.distinct()?.sorted() ?: emptyList()
        }
        val supportsDynamic = SystemProperties.getBoolean(
            "ro.surface_flinger.use_content_detection_for_refresh_rate", false
        )
        if (supportsDynamic) {
            modes.add(0 to context.getString(R.string.refresh_rate_dynamic))
        }
        refreshRates.forEach { hz ->
            modes.add(hz to "${hz}Hz")
        }
        return modes
    }

    private fun getPerAppConfig(cr: ContentResolver): Map<String, AppConfig> {
        val config = Settings.System.getString(cr, "per_app_refresh_rate") ?: return emptyMap()
        if (config.isEmpty()) return emptyMap()
        return config.split(",").mapNotNull {
            val parts = it.split(":")
            if (parts.size >= 2) {
                val pkg = parts[0]
                val rate = parts[1].toIntOrNull() ?: 0
                pkg to AppConfig(rate)
            } else null
        }.toMap()
    }

    private fun savePerAppConfig(cr: ContentResolver, config: Map<String, AppConfig>) {
        val configString = config.entries.joinToString(",") { 
            "${it.key}:${it.value.rate}" 
        }
        Settings.System.putString(cr, "per_app_refresh_rate", configString)
    }

    @Composable
    fun SettingsGroupCard(content: @Composable ColumnScope.() -> Unit) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            )
        ) {
            Column(content = content)
        }
    }

    @Composable
    fun SettingsRadioItem(
        title: String,
        selected: Boolean,
        showDivider: Boolean,
        onClick: () -> Unit
    ) {
        Column(modifier = Modifier.clickable(onClick = onClick)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )
                RadioButton(
                    selected = selected,
                    onClick = null
                )
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.surfaceContainer
                )
            }
        }
    }

    @Composable
    fun SettingsSwitchItem(
        title: String,
        summary: String,
        checked: Boolean,
        showDivider: Boolean = false,
        onClick: () -> Unit
    ) {
        Column {
            Surface(
                onClick = onClick,
                color = Color.Transparent
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 16.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Switch(
                        checked = checked,
                        onCheckedChange = null,
                        thumbContent = {
                            Icon(
                                imageVector = if (checked) Icons.Filled.Check else Icons.Filled.Close,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        }
                    )
                }
            }
            if (showDivider) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                )
            }
        }
    }

    @Composable
    fun RefreshRateIllustration(modes: List<Pair<Int, String>>) {
        val rates = remember(modes) {
            modes.map { it.first }.filter { it > 0 }.sorted()
        }
        val maxRate = rates.lastOrNull()?.toFloat() ?: 120f
        val minRate = rates.firstOrNull()?.toFloat() ?: 60f

        val infiniteTransition = rememberInfiniteTransition(label = "refresh_rate")
        val scrollOffset by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(2000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "scrollOffset"
        )

        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val surfaceColor = MaterialTheme.colorScheme.surfaceContainerHigh

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                RefreshRatePreviewIllustration(
                    modifier = Modifier.weight(1f),
                    label = "${maxRate.toInt()}Hz",
                    subLabel = "Smooth Scrolling",
                    progress = scrollOffset,
                    isHighRate = true,
                    rateRatio = 1f,
                    accentColor = primaryColor,
                    surfaceColor = surfaceColor
                )

                RefreshRatePreviewIllustration(
                    modifier = Modifier.weight(1f),
                    label = "${minRate.toInt()}Hz",
                    subLabel = "Standard",
                    progress = scrollOffset,
                    isHighRate = false,
                    rateRatio = minRate / maxRate,
                    accentColor = secondaryColor,
                    surfaceColor = surfaceColor
                )
            }
        }
    }

    @Composable
    private fun RefreshRatePreviewIllustration(
        modifier: Modifier,
        label: String,
        subLabel: String,
        progress: Float,
        isHighRate: Boolean,
        rateRatio: Float,
        accentColor: Color,
        surfaceColor: Color
    ) {
        val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        val contentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)

        Column(
            modifier = modifier,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = surfaceColor),
                border = BorderStroke(2.dp, outlineColor)
            ) {
                Box(modifier = Modifier.fillMaxSize().padding(8.dp)) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        
                        clipRect {
                            val itemHeight = 40.dp.toPx()
                            val spacing = 12.dp.toPx()
                            val totalItemHeight = itemHeight + spacing
                            
                            val currentProgress = if (isHighRate) {
                                progress
                            } else {
                                val steps = (20f * rateRatio).toInt().coerceIn(5, 15)
                                (progress * steps).toInt().toFloat() / steps
                            }

                            val cycleSize = 3
                            val scrollDistance = currentProgress * totalItemHeight * cycleSize
                            val itemsOnScreen = (canvasHeight / totalItemHeight).toInt()

                            for (i in -1..itemsOnScreen + cycleSize) {
                                val yPos = i * totalItemHeight - scrollDistance
                                val itemIndex = (i + cycleSize * 10) % cycleSize
                                
                                drawRoundRect(
                                    color = if (itemIndex == 0) accentColor.copy(alpha = 0.4f) else contentColor,
                                    topLeft = Offset(0f, yPos),
                                    size = Size(canvasWidth, itemHeight),
                                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                                )
                                
                                val innerSpacing = 8.dp.toPx()
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.3f),
                                    topLeft = Offset(innerSpacing, yPos + innerSpacing),
                                    size = Size(canvasWidth * 0.4f, 6.dp.toPx()),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                                drawRoundRect(
                                    color = Color.White.copy(alpha = 0.2f),
                                    topLeft = Offset(innerSpacing, yPos + innerSpacing + 12.dp.toPx()),
                                    size = Size(canvasWidth * 0.7f, 6.dp.toPx()),
                                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    @Composable
    fun SettingsNavigationItem(
        title: String,
        summary: String,
        onClick: () -> Unit
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = summary,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
