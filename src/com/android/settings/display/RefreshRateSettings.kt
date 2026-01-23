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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
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

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun RefreshRateScreen() {
        var currentPage by remember { mutableStateOf(Page.MAIN) }
        val motionScheme = MaterialTheme.motionScheme
        
        BackHandler(enabled = currentPage != Page.MAIN) {
            currentPage = Page.MAIN
        }

        AnimatedContent(
            targetState = currentPage,
            transitionSpec = {
                if (targetState == Page.PER_APP) {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    ) togetherWith slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(300)
                    )
                } else {
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    ) togetherWith slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(300)
                    )
                }
            },
            label = "ScreenTransition"
        ) { page ->
            when (page) {
                Page.MAIN -> RefreshRateMainScreen(onNavigateToPerApp = { currentPage = Page.PER_APP })
                Page.PER_APP -> PerAppRefreshRateScreen(onBack = { currentPage = Page.MAIN })
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    fun RefreshRateMainScreen(onNavigateToPerApp: () -> Unit) {
        val context = LocalContext.current
        val cr = context.contentResolver
        
        var selectedMode by remember { 
            mutableIntStateOf(Settings.Global.getInt(cr, "display_refresh_rate_mode", 0)) 
        }
        
        var lockscreenLimitEnabled by remember {
            mutableStateOf(Settings.System.getInt(cr, "lockscreen_limit_refresh_rate", 0) != 0)
        }

        val modes = remember { getModes(context) }

        Scaffold(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    RefreshRateIllustrationCard(modes)
                }

                item {
                    RefreshRateSelector(
                        modes = modes,
                        selectedMode = selectedMode,
                        onModeSelected = { mode ->
                            selectedMode = mode
                            Settings.Global.putInt(cr, "display_refresh_rate_mode", mode)
                        }
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.extraLarge,
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceBright
                        )
                    ) {
                        Column {
                            ModernSwitchItem(
                                title = stringResource(R.string.aod_limit_refresh_rate_title),
                                summary = stringResource(R.string.aod_limit_refresh_rate_summary),
                                checked = lockscreenLimitEnabled,
                                icon = Icons.Default.LockClock,
                                showDivider = true,
                                onClick = {
                                    lockscreenLimitEnabled = !lockscreenLimitEnabled
                                    Settings.System.putInt(cr, "lockscreen_limit_refresh_rate", 
                                        if (lockscreenLimitEnabled) 1 else 0)
                                }
                            )
                            ModernNavigationItem(
                                title = stringResource(R.string.per_app_refresh_rate_title),
                                summary = stringResource(R.string.per_app_refresh_rate_summary),
                                icon = Icons.Default.Apps,
                                onClick = onNavigateToPerApp
                            )
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
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
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        TextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search apps...") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            singleLine = true
                        )
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, "Clear")
                            }
                        }
                    }
                }

                if (isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Loading apps...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps) { app ->
                            val appConfig = perAppConfig[app.packageName]
                            ModernAppItem(
                                app = app,
                                config = appConfig,
                                onClick = { selectedApp = app }
                            )
                        }
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        if (selectedApp != null) {
            AppRateDialog(
                app = selectedApp!!,
                modes = modes,
                currentConfig = perAppConfig[selectedApp!!.packageName],
                onDismiss = { selectedApp = null },
                onConfirm = { rate ->
                    perAppConfig = perAppConfig.toMutableMap().apply {
                        if (rate == 0) {
                            remove(selectedApp!!.packageName)
                        } else {
                            put(selectedApp!!.packageName, AppConfig(rate))
                        }
                    }
                    savePerAppConfig(cr, perAppConfig)
                    selectedApp = null
                }
            )
        }
    }

    @Composable
    private fun RefreshRateSelector(
        modes: List<Pair<Int, String>>,
        selectedMode: Int,
        onModeSelected: (Int) -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Refresh Rate Mode",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    modes.forEach { (hz, label) ->
                        RefreshRateOption(
                            label = label,
                            isSelected = selectedMode == hz,
                            onClick = { onModeSelected(hz) }
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun RefreshRateOption(
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = if (isSelected) 
                RoundedCornerShape(24.dp)
            else 
                RoundedCornerShape(16.dp),
            color = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = if (isSelected) 
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun RefreshRateIllustrationCard(modes: List<Pair<Int, String>>) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceBright
            )
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                RefreshRateIllustration(modes)
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
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "Visual Comparison",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RefreshRatePreviewIllustration(
                    modifier = Modifier.weight(1f),
                    label = "${maxRate.toInt()}Hz",
                    subLabel = "Smooth",
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
                shape = MaterialTheme.shapes.large,
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

    @OptIn(ExperimentalMaterial3ExpressiveApi::class)
    @Composable
    private fun ModernSwitchItem(
        title: String,
        summary: String,
        checked: Boolean,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        showDivider: Boolean = false,
        onClick: () -> Unit
    ) {
        Column {
            Surface(
                onClick = onClick,
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(40.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = if (checked) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (checked) 
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
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
                            Crossfade(
                                targetState = checked,
                                animationSpec = MaterialTheme.motionScheme.slowEffectsSpec(),
                                label = "switch_icon"
                            ) { isChecked ->
                                Icon(
                                    imageVector = if (isChecked) Icons.Filled.Check else Icons.Filled.Close,
                                    contentDescription = null,
                                    modifier = Modifier.size(SwitchDefaults.IconSize)
                                )
                            }
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
    private fun ModernNavigationItem(
        title: String,
        summary: String,
        icon: androidx.compose.ui.graphics.vector.ImageVector,
        onClick: () -> Unit
    ) {
        Surface(
            onClick = onClick,
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
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
                
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    @Composable
    private fun ModernAppItem(
        app: AppInfo,
        config: AppConfig?,
        onClick: () -> Unit
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(
                containerColor = if (config != null) 
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else 
                    MaterialTheme.colorScheme.surfaceBright
            ),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    bitmap = app.icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = app.packageName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (config != null) 
                        MaterialTheme.colorScheme.primary
                    else 
                        MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = if (config == null) "Default" else "${config.rate}Hz",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (config != null) 
                            MaterialTheme.colorScheme.onPrimary
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }

    @Composable
    private fun AppRateDialog(
        app: AppInfo,
        modes: List<Pair<Int, String>>,
        currentConfig: AppConfig?,
        onDismiss: () -> Unit,
        onConfirm: (Int) -> Unit
    ) {
        var selectedRate by remember { mutableIntStateOf(currentConfig?.rate ?: 0) }

        AlertDialog(
            onDismissRequest = onDismiss,
            icon = {
                Image(
                    bitmap = app.icon.toBitmap().asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.medium)
                )
            },
            title = { 
                Text(
                    app.label,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    RateOption(
                        label = "Default",
                        isSelected = selectedRate == 0,
                        onClick = { selectedRate = 0 }
                    )
                    
                    modes.filter { it.first > 0 }.forEach { (hz, label) ->
                        RateOption(
                            label = label,
                            isSelected = selectedRate == hz,
                            onClick = { selectedRate = hz }
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { onConfirm(selectedRate) },
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Apply")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = onDismiss,
                    shape = MaterialTheme.shapes.medium
                ) {
                    Text("Cancel")
                }
            },
            shape = MaterialTheme.shapes.extraLarge
        )
    }

    @Composable
    private fun RateOption(
        label: String,
        isSelected: Boolean,
        onClick: () -> Unit
    ) {
        Surface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            border = if (isSelected) 
                BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
            else null
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) 
                        MaterialTheme.colorScheme.onPrimaryContainer
                    else 
                        MaterialTheme.colorScheme.onSurface
                )
                
                if (isSelected) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }

    data class AppInfo(val packageName: String, val label: String, val icon: Drawable)
    data class AppConfig(val rate: Int)

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
}
