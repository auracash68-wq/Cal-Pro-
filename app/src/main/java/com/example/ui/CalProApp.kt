package com.example.ui

import android.app.DatePickerDialog
import android.content.Context
import android.widget.Toast
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.HistoryItem
import com.example.ui.theme.*
import com.example.util.AdManager
import com.example.util.CalEngines
import com.example.util.SoundManager
import com.example.viewmodel.CalProViewModel
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.LoadAdError
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun CalProApp(viewModel: CalProViewModel) {
    val context = LocalContext.current
    val currentScreen = viewModel.currentScreen

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (viewModel.isDarkTheme) DeepBlackBackground else SoftOffWhite
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            // Core Screens Navigation Switcher
            AnimatedContent(
                targetState = currentScreen,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "ScreenTransition"
            ) { targetScreen ->
                when (targetScreen) {
                    "splash" -> SplashScreenView(
                        isDark = viewModel.isDarkTheme,
                        onFinished = {
                            viewModel.navigateTo("home")
                        }
                    )
                    "home" -> HomeScreenView(viewModel = viewModel)
                    "history" -> HistoryScreenView(viewModel = viewModel)
                    "privacy" -> PrivacyPolicyScreenView(viewModel = viewModel)
                }
            }

            // Google AdMob Rewarded Test Ad Overlay
            if (AdManager.isFallbackRewardedShowing) {
                RewardedAdOverlay(
                    countdown = AdManager.rewardedCountdown,
                    onDismiss = { AdManager.dismissRewarded() }
                )
            }
        }
    }
}

// ================= SPLASH SCREEN =================
@Composable
fun SplashScreenView(isDark: Boolean, onFinished: () -> Unit) {
    var timerTicks by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        // Run timeline in 100ms intervals (total 3000ms)
        for (i in 0..30) {
            timerTicks = i * 100
            delay(100)
        }
        onFinished()
    }

    // Calculated Animation Parameters from Timeline
    val calProSlideOffset = if (timerTicks < 200) -300f else {
        val progress = ((timerTicks - 200).coerceIn(0, 1000) / 1000f)
        -300f * (1f - progress) // Slides left side in
    }
    val calProScale = if (timerTicks < 500) 1.0f else {
        1.0f + 0.12f * (timerTicks - 500).coerceIn(0, 500) / 500f
    }
    val calProAlpha = if (timerTicks >= 2500) {
        1f - ((timerTicks - 2500).coerceIn(0, 500) / 500f)
    } else 1.0f

    val showCalcIcon = timerTicks >= 500

    val madeSlideProgress = ((timerTicks - 800).coerceIn(0, 500) / 500f)
    val madeOffset = -400f * (1f - madeSlideProgress) // slides in from left

    val indiaSlideProgress = ((timerTicks - 800).coerceIn(0, 500) / 500f)
    val indiaOffset = 400f * (1f - indiaSlideProgress) // slides in from right

    val assembledProgress = ((timerTicks - 1300).coerceIn(0, 500) / 500f)
    val assembledScale = 1.0f + 0.08f * assembledProgress

    val showMadeInIndia = timerTicks >= 800
    val madeInIndiaAlpha = if (timerTicks >= 2500) {
        1f - ((timerTicks - 2500).coerceIn(0, 500) / 500f)
    } else 1.0f

    val devCreditAlpha = if (timerTicks < 1500) 0f else {
        val entry = ((timerTicks - 1500).coerceIn(0, 700) / 700f)
        if (timerTicks >= 2500) {
            val exit = ((timerTicks - 2500).coerceIn(0, 500) / 500f)
            entry * (1f - exit)
        } else {
            entry
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (isDark) DeepBlackBackground else Color.White)
            .padding(16.dp)
    ) {
        // App Title Section (Top Area)
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 100.dp)
                .offset(x = calProSlideOffset.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = "CAL PRO",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepProfessionalBlue,
                    modifier = Modifier.testTag("splash_title")
                )
                if (showCalcIcon) {
                    Spacer(modifier = Modifier.width(12.dp))
                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .background(DeepProfessionalBlue, CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculator Icon",
                            tint = Color.White
                        )
                    }
                }
            }
        }

        // Made in India Section (Exact Center)
        if (showMadeInIndia) {
            Row(
                modifier = Modifier
                    .align(Alignment.Center)
                    .alpha(madeInIndiaAlpha)
                    .wrapContentSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Saffron MADE
                Text(
                    text = "MADE ",
                    fontSize = if (timerTicks >= 1300) 24.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFFFF9933), // Indian Saffron
                    modifier = Modifier
                        .offset(x = madeOffset.dp)
                        .testTag("splash_made")
                )
                // White IN
                Text(
                    text = "IN ",
                    fontSize = if (timerTicks >= 1300) 24.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (isDark) Color.White else Color.Gray,
                    modifier = Modifier.testTag("splash_in")
                )
                // Green INDIA
                Text(
                    text = "INDIA",
                    fontSize = if (timerTicks >= 1300) 24.sp else 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF138808), // Indian Green
                    modifier = Modifier
                        .offset(x = indiaOffset.dp)
                        .testTag("splash_india")
                )
            }
        }

        // Developer Credit Section (Bottom Center)
        Text(
            text = "Presented by Aura Tools",
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = Color.Gray,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .alpha(devCreditAlpha)
                .padding(bottom = 60.dp)
                .testTag("splash_dev_credit")
        )
    }
}

// ================= AD BANNER REPLICA (MIME IN HOME STYLE) =================
@Composable
fun AdMobBannerView(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var retryCount by remember { mutableStateOf(0) }
    var isFailedToLoad by remember(retryCount) { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = Color.LightGray.copy(alpha = 0.15f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("ad_banner")
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            val hasInternet = remember(context, retryCount) { AdManager.isNetworkAvailable(context) }
            
            if (hasInternet && !isFailedToLoad) {
                AndroidView(
                    factory = { ctx ->
                        Log.d("AdManager", "Ad Request - Loading AdMob Banner Ad (ID: ${AdManager.BANNER_AD_UNIT_ID})...")
                        AdView(ctx).apply {
                            adUnitId = AdManager.BANNER_AD_UNIT_ID
                            setAdSize(AdSize.BANNER)
                            
                            adListener = object : AdListener() {
                                override fun onAdLoaded() {
                                    super.onAdLoaded()
                                    Log.d("AdManager", "Ad Load Success - Banner Live Ad Loaded successfully")
                                    AdManager.isBannerLoaded = true
                                    AdManager.isBannerLoading = false
                                    isFailedToLoad = false
                                }

                                override fun onAdFailedToLoad(error: LoadAdError) {
                                    super.onAdFailedToLoad(error)
                                    Log.e("AdManager", "Ad Load Failure - Banner failed: ${error.message} (code: ${error.code})")
                                    AdManager.isBannerLoaded = false
                                    AdManager.isBannerLoading = false
                                    AdManager.bannerStatusMessage = error.message
                                    isFailedToLoad = true
                                }
                            }
                            
                            loadAd(AdRequest.Builder().build())
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ad",
                            fontSize = 11.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(Color(0xFFF9A825), RoundedCornerShape(3.dp))
                                .padding(horizontal = 5.dp, vertical = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Aura Tools Premium Upgrades Available",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepProfessionalBlue,
                                maxLines = 1
                            )
                            Text(
                                text = "Offline Mode: Click retry when internet is connected.",
                                fontSize = 9.sp,
                                color = Color.Gray,
                                maxLines = 1
                            )
                        }
                    }
                    Button(
                        onClick = { retryCount++ },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(28.dp)
                    ) {
                        Text("Retry", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

// ================= REWARDED AD OVERLAY =================
@Composable
fun RewardedAdOverlay(countdown: Int, onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.95f))
            .clickable(enabled = false) {}
            .testTag("rewarded_overlay")
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Watching Google AdMob Rewarded Test Ad",
                fontSize = 13.sp,
                color = Color(0xFF66BB6A),
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "ca-app-pub-3940256099942544/5224354917",
                fontSize = 10.sp,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(30.dp))
            IconButton(
                onClick = {},
                modifier = Modifier
                    .background(Color(0xFF66BB6A).copy(alpha = 0.2f), CircleShape)
                    .size(70.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.OfflineBolt,
                    contentDescription = null,
                    tint = Color(0xFF66BB6A),
                    modifier = Modifier.size(40.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Unlocking Download History Report",
                fontSize = 18.sp,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Thank you for supporting Aura Tools developer! Your download starts immediately after watching this short video.",
                fontSize = 13.sp,
                color = Color.LightGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 20.dp)
            )

            Spacer(modifier = Modifier.height(40.dp))
            
            // ProgressBar replica
            val progress = (5f - countdown) / 5f
            LinearProgressIndicator(
                progress = progress,
                color = Color(0xFF66BB6A),
                trackColor = Color.DarkGray,
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .height(6.dp)
                    .clip(CircleShape)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (countdown > 0) "Reward active in $countdown seconds..." else "Rewarded Successfully!",
                color = Color.LightGray,
                fontSize = 12.sp
            )
        }

        // Custom Dismiss Option at any point, cancels rewards
        IconButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .background(Color.White.copy(alpha = 0.1f), CircleShape)
                .testTag("close_rewarded_button")
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Cancel ad support",
                tint = Color.White
            )
        }
    }
}

// ================= HOME SCREEN =================
@Composable
fun HomeScreenView(viewModel: CalProViewModel) {
    val context = LocalContext.current
    val activeToolIndex = viewModel.activeToolIndex
    val isDark = viewModel.isDarkTheme

    // Safe area layout avoiding notches or bottom navigations
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                // Configured Banner Ad at Fixed Top Position
                AdMobBannerView(modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp))
                
                // Smart Header Toolbar Row
                HomeHeaderRow(viewModel = viewModel)
            }
        },
        containerColor = if (isDark) DeepBlackBackground else SoftOffWhite
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                // Displays Live Output/Result Area (Dynamic scaling)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(if (activeToolIndex == 0) 1.5f else 1.2f)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val secondaryText = when (activeToolIndex) {
                            0 -> viewModel.calcInputDisplay
                            1 -> "${dfFormat(viewModel.emiLoanAmount)} @ ${viewModel.emiInterestRate}% for ${viewModel.emiTenureYears} yr"
                            2 -> "DOB: " + viewModel.ageDobDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                            3 -> "${dfFormat(viewModel.gstAmountStr)} + ${viewModel.gstRateStr}% GST"
                            4 -> "${viewModel.unitInputVal} ${viewModel.unitFromUnit}"
                            5 -> "${viewModel.currencyInputVal} ${viewModel.currencyFromCode}"
                            6 -> "${viewModel.pctInputVal}% of ${viewModel.pctTotalVal}"
                            7 -> "Orig: ${dfFormat(viewModel.discountOrigPrice)} | Less: ${viewModel.discountPct}%"
                            8 -> "${dfFormat(viewModel.loanAmount)} @ ${viewModel.loanInterestRate}% for ${viewModel.loanTenureYears} yr"
                            9 -> "From: " + viewModel.dateDiffStartDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
                            else -> ""
                        }
                        val primaryText = when (activeToolIndex) {
                            0 -> if (viewModel.calcResultDisplay.isNotEmpty()) viewModel.calcResultDisplay else "0"
                            1 -> "${viewModel.df.format(viewModel.emiResultState.monthlyEmi)} / mo"
                            2 -> "${viewModel.ageResultState.years} Years, ${viewModel.ageResultState.months} Months"
                            3 -> viewModel.df.format(viewModel.gstResultState.finalAmount)
                            4 -> viewModel.df.format(viewModel.unitResultVal) + " " + viewModel.unitToUnit
                            5 -> viewModel.df.format(viewModel.currencyResultVal) + " " + viewModel.currencyToCode
                            6 -> viewModel.df.format(viewModel.pctResultVal)
                            7 -> "${viewModel.df.format(viewModel.discountFinalPrice)} Final"
                            8 -> "${viewModel.df.format(viewModel.loanEmi)} / mo"
                            9 -> "${viewModel.dateDiffDays} Days Diff"
                            else -> "0"
                        }

                        if (secondaryText.isNotEmpty()) {
                            Text(
                                text = secondaryText,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Gray.copy(alpha = 0.82f),
                                textAlign = TextAlign.End,
                                modifier = Modifier.padding(bottom = 4.dp),
                                letterSpacing = (-0.5).sp
                            )
                        }
                        Text(
                            text = primaryText,
                            fontSize = if (primaryText.length > 12) 36.sp else 54.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) Color.White else DeepProfessionalBlue,
                            textAlign = TextAlign.End,
                            lineHeight = 56.sp,
                            letterSpacing = (-1.5).sp,
                            modifier = Modifier.testTag("result_main_display")
                        )
                    }
                }

                // 6 Rounded horizontal Shortcut Selector Cards
                ShortcutRowComponent(viewModel = viewModel)
                
                Spacer(modifier = Modifier.height(10.dp))

                // Active Tool Workspaces Grid/Keypad Separator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(3f)
                ) {
                    AnimatedContent(
                        targetState = activeToolIndex,
                        transitionSpec = {
                            slideInVertically(animationSpec = tween(250)) { it } togetherWith fadeOut(animationSpec = tween(200))
                        },
                        label = "ToolWorkspaceSwitch"
                    ) { tool ->
                        when (tool) {
                            0 -> StandardCalculatorKeypad(viewModel = viewModel)
                            1 -> EmiWorkspace(viewModel = viewModel)
                            2 -> AgeWorkspace(viewModel = viewModel)
                            3 -> GstWorkspace(viewModel = viewModel)
                            4 -> UnitConverterWorkspace(viewModel = viewModel)
                            5 -> CurrencyWorkspace(viewModel = viewModel)
                            6 -> PercentageCalculatorWorkspace(viewModel = viewModel)
                            7 -> DiscountCalculatorWorkspace(viewModel = viewModel)
                            8 -> LoanCalculatorWorkspace(viewModel = viewModel)
                            9 -> DateDifferenceCalculatorWorkspace(viewModel = viewModel)
                        }
                    }
                }
            }

            // Smart Instant Search Overlay List matching typed queries
            if (viewModel.isSearchActive && viewModel.searchQuery.isNotEmpty()) {
                SmartSearchPopupView(viewModel = viewModel)
            }
        }
    }
}

// Standard Dynamic Formatter Helper
private fun dfFormat(v: String): String {
    val d = v.toDoubleOrNull() ?: return "0"
    return java.text.DecimalFormat("#,###.##").format(d)
}

// ================= HOME HEADER ROW (WITH ROTATED ACTIONS) =================
@Composable
fun HomeHeaderRow(viewModel: CalProViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left side circular triggers stacked
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Circular History Button
            IconButton(
                onClick = { viewModel.navigateTo("history") },
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (viewModel.isDarkTheme) Color.DarkGray.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .testTag("history_nav_button")
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = "Open History log",
                    tint = if (viewModel.isDarkTheme) Color.White else DeepProfessionalBlue
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            // Circular Theme Toggle Button
            IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (viewModel.isDarkTheme) Color.DarkGray.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .testTag("theme_toggle_button")
            ) {
                Icon(
                    imageVector = if (viewModel.isDarkTheme) Icons.Default.LightMode else Icons.Default.DarkMode,
                    contentDescription = "Toggle color scheme",
                    tint = if (viewModel.isDarkTheme) Color(0xFFFFD54F) else Color.Gray
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            // Circular Sound Toggle Button
            var soundOn by remember { mutableStateOf(SoundManager.soundEnabled) }
            val context = LocalContext.current
            IconButton(
                onClick = {
                    soundOn = !soundOn
                    SoundManager.toggleSound(context, soundOn)
                    if (soundOn) {
                        SoundManager.playTap()
                    }
                },
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (viewModel.isDarkTheme) Color.DarkGray.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f),
                        CircleShape
                    )
                    .testTag("sound_toggle_button")
            ) {
                Icon(
                    imageVector = if (soundOn) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                    contentDescription = "Toggle sound effects on key press",
                    tint = if (soundOn) (if (viewModel.isDarkTheme) Color(0xFFFFD54F) else DeepProfessionalBlue) else Color.Gray
                )
            }
        }

        // Right side circular Search Button
        IconButton(
            onClick = {
                viewModel.isSearchActive = !viewModel.isSearchActive
                if (!viewModel.isSearchActive) viewModel.searchQuery = ""
            },
            modifier = Modifier
                .size(46.dp)
                .background(
                    if (viewModel.isDarkTheme) Color.DarkGray.copy(alpha = 0.5f) else Color.LightGray.copy(alpha = 0.3f),
                    CircleShape
                )
                .testTag("search_search_button")
        ) {
            Icon(
                imageVector = if (viewModel.isSearchActive) Icons.Default.Close else Icons.Default.Search,
                contentDescription = "Open search utilities",
                tint = if (viewModel.isDarkTheme) Color.White else DeepProfessionalBlue
            )
        }
    }

    // Interactive Instant Search text field inside header Row if toggled active
    if (viewModel.isSearchActive) {
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.searchQuery = it },
            placeholder = { Text("Search tool matching query...", fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, "search icon", modifier = Modifier.size(20.dp)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = if (viewModel.isDarkTheme) Color.White else DarkCharcoalText,
                focusedContainerColor = if (viewModel.isDarkTheme) DarkGrayCard else Color.White,
                unfocusedContainerColor = if (viewModel.isDarkTheme) DarkGrayCard else Color.White,
                focusedBorderColor = DeepProfessionalBlue,
                unfocusedBorderColor = Color.LightGray
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .height(52.dp)
                .testTag("search_input_field"),
            singleLine = true,
            shape = RoundedCornerShape(26.dp)
        )
    }
}

// ================= SMART SEARCH OVERLAY SYSTEM =================
@Composable
fun BoxScope.SmartSearchPopupView(viewModel: CalProViewModel) {
    Card(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(
            containerColor = if (viewModel.isDarkTheme) DeepGraySurface else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .align(Alignment.TopCenter)
            .padding(horizontal = 16.dp)
            .offset(y = 10.dp)
            .heightIn(max = 240.dp)
            .testTag("search_overlay_card")
    ) {
        val matches = viewModel.searchResults
        if (matches.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No calculator match found.",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.padding(8.dp)) {
                items(matches) { match ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.activeToolIndex = match.second
                                viewModel.isSearchActive = false
                                viewModel.searchQuery = ""
                            }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowRight,
                            contentDescription = null,
                            tint = DeepProfessionalBlue
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = match.first,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewModel.isDarkTheme) Color.White else DarkCharcoalText
                        )
                    }
                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                }
            }
        }
    }
}

// ================= SHORTCUT CARD SELECTOR ROW =================
@Composable
fun ShortcutRowComponent(viewModel: CalProViewModel) {
    val toolsList = listOf(
        Pair("Calculator", Icons.Default.Calculate),
        Pair("EMI Calc", Icons.Default.AccountBalance),
        Pair("Age Calc", Icons.Default.Cake),
        Pair("GST Calc", Icons.Default.ReceiptLong),
        Pair("Unit Conv", Icons.Default.Straighten),
        Pair("Currency", Icons.Default.CurrencyExchange)
    )

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("shortcut_row_lazy"),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 6.dp)
    ) {
        items(toolsList.size) { idx ->
            val isSelected = viewModel.activeToolIndex == idx
            val item = toolsList[idx]

            val cardBg = if (viewModel.isDarkTheme) {
                if (isSelected) DeepProfessionalBlue else DarkGrayCard
            } else {
                if (isSelected) SelectionBlueBg else LightGrayCard
            }
            val cardBorder = if (viewModel.isDarkTheme) {
                if (isSelected) BorderStroke(1.5.dp, Color.White.copy(alpha = 0.3f)) else BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
            } else {
                if (isSelected) BorderStroke(1.5.dp, SelectionBlueBorder) else null
            }
            val contentColor = if (viewModel.isDarkTheme) {
                Color.White
            } else {
                if (isSelected) DeepProfessionalBlue else DarkCharcoalText
            }

            Card(
                colors = CardDefaults.cardColors(
                    containerColor = cardBg
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .width(104.dp)
                    .height(64.dp)
                    .clickable {
                        viewModel.activeToolIndex = idx
                    }
                    .testTag("shortcut_card_$idx"),
                border = cardBorder
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = item.second,
                        contentDescription = item.first,
                        tint = contentColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.first,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = contentColor
                    )
                }
            }
        }
    }
}

// ================= KEYPAD COMPOSABLE FOR STANDARD CALCULATOR =================
@Composable
fun StandardCalculatorKeypad(viewModel: CalProViewModel) {
    val keys = listOf(
        listOf("AC", "DEL", "+/-", "÷"),
        listOf("7", "8", "9", "×"),
        listOf("4", "5", "6", "-"),
        listOf("1", "2", "3", "+"),
        listOf("%", "0", ".", "=")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("standard_keypad_container"),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { char ->
                    val isClearOp = char == "AC" || char == "DEL"
                    val isOperator = char == "÷" || char == "×" || char == "-" || char == "+" || char == "%"
                    
                    val bgCol = if (viewModel.isDarkTheme) {
                        when {
                            char == "=" -> DeepProfessionalBlue
                            isClearOp -> Color(0xFF34171A)
                            isOperator -> DarkGrayCard
                            else -> DeepGraySurface
                        }
                    } else {
                        when {
                            char == "=" -> DeepProfessionalBlue
                            isClearOp -> DangerRedBg
                            isOperator -> LightGrayCard
                            else -> Color(0xFFF8F9FA)
                        }
                    }

                    val textCol = if (viewModel.isDarkTheme) {
                        when {
                            char == "=" -> Color.White
                            isClearOp -> Color(0xFFFF8B8B)
                            isOperator -> Color(0xFF90CAF9)
                            else -> Color.White
                        }
                    } else {
                        when {
                            char == "=" -> Color.White
                            isClearOp -> DangerRedText
                            isOperator -> DeepProfessionalBlue
                            else -> DarkCharcoalText
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1.23f)
                            .padding(4.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(bgCol)
                            .clickable { viewModel.onCalcKeyPress(char) }
                            .testTag("key_$char"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = textCol
                        )
                    }
                }
            }
        }
    }
}

// ================= EMI CALCULATOR WORKSPACE =================
@Composable
fun EmiWorkspace(viewModel: CalProViewModel) {
    val isDark = viewModel.isDarkTheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
            .testTag("workspace_emi"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("EMI Inputs", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        OutlinedTextField(
            value = viewModel.emiLoanAmount,
            onValueChange = {
                viewModel.emiLoanAmount = it
                viewModel.updateEmi()
            },
            label = { Text("Loan Amount ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("emi_input_amount"),
            colors = textColors(isDark)
        )

        OutlinedTextField(
            value = viewModel.emiInterestRate,
            onValueChange = {
                viewModel.emiInterestRate = it
                viewModel.updateEmi()
            },
            label = { Text("Annual Interest Rate (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("emi_input_rate"),
            colors = textColors(isDark)
        )

        OutlinedTextField(
            value = viewModel.emiTenureYears,
            onValueChange = {
                viewModel.emiTenureYears = it
                viewModel.updateEmi()
            },
            label = { Text("Tenure Years") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("emi_input_tenure"),
            colors = textColors(isDark)
        )

        // Summary Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDark) DarkGrayCard else LightGrayCard
            ),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Interest Payable:", fontSize = 11.sp, color = Color.Gray)
                    Text("$" + viewModel.df.format(viewModel.emiResultState.totalInterest), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total Amount Plan:", fontSize = 11.sp, color = Color.Gray)
                    Text("$" + viewModel.df.format(viewModel.emiResultState.totalPayment), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(
                onClick = { viewModel.saveEmiClicked() },
                colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
                modifier = Modifier.weight(1f).testTag("emi_save_button")
            ) {
                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Show Result", fontSize = 13.sp)
            }
        }
    }
}

// ================= AGE CALCULATOR WORKSPACE =================
@Composable
fun AgeWorkspace(viewModel: CalProViewModel) {
    val isDark = viewModel.isDarkTheme
    val context = LocalContext.current
    
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 4.dp)
            .testTag("workspace_age"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Age calculation workspace", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) DarkGrayCard else LightGrayCard,
                    RoundedCornerShape(14.dp)
                )
                .clickable {
                    val date = viewModel.ageDobDate
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            viewModel.ageDobDate = LocalDate.of(year, month + 1, dayOfMonth)
                            viewModel.updateAge()
                        },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    ).show()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Select Date of Birth", fontSize = 12.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.ageDobDate.format(formatter),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DarkCharcoalText
                )
            }
            Icon(Icons.Default.CalendarToday, contentDescription = "Pick Date DOB", tint = DeepProfessionalBlue)
        }

        // Live breakdown card details
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkGrayCard else LightGrayCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("YEARS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("${viewModel.ageResultState.years}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DeepProfessionalBlue)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("MONTHS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("${viewModel.ageResultState.months}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DeepProfessionalBlue)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("DAYS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("${viewModel.ageResultState.days}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = DeepProfessionalBlue)
                }
            }
        }

        Button(
            onClick = { viewModel.saveAgeClicked() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
            modifier = Modifier.fillMaxWidth().testTag("age_save_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show Result", fontSize = 13.sp)
        }
    }
}

// ================= GST CALCULATOR WORKSPACE =================
@Composable
fun GstWorkspace(viewModel: CalProViewModel) {
    val isDark = viewModel.isDarkTheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
            .testTag("workspace_gst"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("GST Inputs Configuration", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        OutlinedTextField(
            value = viewModel.gstAmountStr,
            onValueChange = {
                viewModel.gstAmountStr = it
                viewModel.updateGst()
            },
            label = { Text("Original Net Amount ($)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("gst_input_amount"),
            colors = textColors(isDark)
        )

        Text("GST Rate Percent (%)", fontSize = 12.sp, color = Color.Gray)
        val rates = listOf("5", "12", "18", "28")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            rates.forEach { rate ->
                val active = viewModel.gstRateStr == rate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .background(
                            if (active) DeepProfessionalBlue else {
                                if (isDark) DarkGrayCard else LightGrayCard
                            },
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            viewModel.gstRateStr = rate
                            viewModel.updateGst()
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$rate%",
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else {
                            if (isDark) Color.LightGray else DarkCharcoalText
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = viewModel.gstRateStr,
            onValueChange = {
                viewModel.gstRateStr = it
                viewModel.updateGst()
            },
            label = { Text("Custom Rate (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("gst_input_custom_rate"),
            colors = textColors(isDark)
        )

        // Summary breakdowns
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkGrayCard else LightGrayCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Calculated TAX amount:", fontSize = 11.sp, color = Color.Gray)
                    Text("$" + viewModel.df.format(viewModel.gstResultState.gstAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BlueAccent)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Final Gross Amount:", fontSize = 11.sp, color = Color.Gray)
                    Text("$" + viewModel.df.format(viewModel.gstResultState.finalAmount), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Button(
            onClick = { viewModel.saveGstClicked() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
            modifier = Modifier.fillMaxWidth().testTag("gst_save_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show Result", fontSize = 13.sp)
        }
    }
}

// ================= UNIT CONVERTER WORKSPACE =================
@Composable
fun UnitConverterWorkspace(viewModel: CalProViewModel) {
    val isDark = viewModel.isDarkTheme
    val categories = listOf("Length", "Weight", "Area", "Temperature")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
            .testTag("workspace_units"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Selection category scroll
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(categories) { cat ->
                val active = viewModel.unitCategory == cat
                Box(
                    modifier = Modifier
                        .background(
                            if (active) DeepProfessionalBlue else {
                                if (isDark) DarkGrayCard else LightGrayCard
                            },
                            RoundedCornerShape(18.dp)
                        )
                        .clickable { viewModel.changeUnitCategory(cat) }
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else {
                            if (isDark) Color.LightGray else DarkCharcoalText
                        }
                    )
                }
            }
        }

        OutlinedTextField(
            value = viewModel.unitInputVal,
            onValueChange = {
                viewModel.unitInputVal = it
                viewModel.updateUnits()
            },
            label = { Text("Input value to convert") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("unit_input_val"),
            colors = textColors(isDark)
        )

        // Selectors for From / To
        val activeUnits = when (viewModel.unitCategory) {
            "Length" -> CalEngines.Units.lengthFactors.keys.toList()
            "Weight" -> CalEngines.Units.weightFactors.keys.toList()
            "Area" -> CalEngines.Units.areaFactors.keys.toList()
            else -> listOf("Celsius", "Fahrenheit", "Kelvin")
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            var expandedFrom by remember { mutableStateOf(false) }
            var expandedTo by remember { mutableStateOf(false) }

            // Unit From
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedFrom = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) DarkGrayCard else LightGrayCard
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(viewModel.unitFromUnit, color = if (isDark) Color.White else DarkCharcoalText, maxLines = 1)
                }
                DropdownMenu(
                    expanded = expandedFrom,
                    onDismissRequest = { expandedFrom = false }
                ) {
                    activeUnits.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit) },
                            onClick = {
                                viewModel.unitFromUnit = unit
                                viewModel.updateUnits()
                                expandedFrom = false
                            }
                        )
                    }
                }
            }

            // Converter Icon spacer
            IconButton(
                onClick = {
                    val swap = viewModel.unitFromUnit
                    viewModel.unitFromUnit = viewModel.unitToUnit
                    viewModel.unitToUnit = swap
                    viewModel.updateUnits()
                },
                modifier = Modifier
                    .background(DeepProfessionalBlue.copy(alpha = 0.1f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, contentDescription = "Swap directions", tint = DeepProfessionalBlue)
            }

            // Unit To
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedTo = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) DarkGrayCard else LightGrayCard
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(viewModel.unitToUnit, color = if (isDark) Color.White else DarkCharcoalText, maxLines = 1)
                }
                DropdownMenu(
                    expanded = expandedTo,
                    onDismissRequest = { expandedTo = false }
                ) {
                    activeUnits.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit) },
                            onClick = {
                                viewModel.unitToUnit = unit
                                viewModel.updateUnits()
                                expandedTo = false
                            }
                        )
                    }
                }
            }
        }

        Button(
            onClick = { viewModel.saveUnitsClicked() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
            modifier = Modifier.fillMaxWidth().testTag("unit_save_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show Result", fontSize = 13.sp)
        }
    }
}

// ================= CURRENCY CONVERTER WORKSPACE =================
@Composable
fun CurrencyWorkspace(viewModel: CalProViewModel) {
    val isDark = viewModel.isDarkTheme
    val availableCurrencies = viewModel.currencyRatesMap.keys.sorted().toList()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
            .testTag("workspace_currency"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Currency Conversion Settings", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)
            
            // Manual API Refresh Action
            IconButton(
                onClick = { viewModel.fetchExchangeRates() },
                modifier = Modifier.size(32.dp).testTag("currency_refresh_btn")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sync latest exchange rates",
                    tint = DeepProfessionalBlue,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        OutlinedTextField(
            value = viewModel.currencyInputVal,
            onValueChange = {
                viewModel.currencyInputVal = it
                viewModel.updateCurrency()
            },
            label = { Text("Base Amount to Exchange") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("currency_input_val"),
            colors = textColors(isDark)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            var expandedFrom by remember { mutableStateOf(false) }
            var expandedTo by remember { mutableStateOf(false) }

            // Currency From select
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedFrom = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) DarkGrayCard else LightGrayCard
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(viewModel.currencyFromCode, color = if (isDark) Color.White else DarkCharcoalText)
                }
                DropdownMenu(
                    expanded = expandedFrom,
                    onDismissRequest = { expandedFrom = false }
                ) {
                    availableCurrencies.forEach { curr ->
                        DropdownMenuItem(
                            text = { Text(curr) },
                            onClick = {
                                viewModel.currencyFromCode = curr
                                viewModel.updateCurrency()
                                expandedFrom = false
                            }
                        )
                    }
                }
            }

            // Swap icon
            IconButton(
                onClick = {
                    val swap = viewModel.currencyFromCode
                    viewModel.currencyFromCode = viewModel.currencyToCode
                    viewModel.currencyToCode = swap
                    viewModel.updateCurrency()
                },
                modifier = Modifier
                    .background(DeepProfessionalBlue.copy(alpha = 0.1f), CircleShape)
                    .size(40.dp)
            ) {
                Icon(Icons.Default.SwapHoriz, "Swap currency direction", tint = DeepProfessionalBlue)
            }

            // Currency To select
            Box(modifier = Modifier.weight(1f)) {
                Button(
                    onClick = { expandedTo = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) DarkGrayCard else LightGrayCard
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(viewModel.currencyToCode, color = if (isDark) Color.White else DarkCharcoalText)
                }
                DropdownMenu(
                    expanded = expandedTo,
                    onDismissRequest = { expandedTo = false }
                ) {
                    availableCurrencies.forEach { curr ->
                        DropdownMenuItem(
                            text = { Text(curr) },
                            onClick = {
                                viewModel.currencyToCode = curr
                                viewModel.updateCurrency()
                                expandedTo = false
                            }
                        )
                    }
                }
            }
        }

        // Live Rate Reference Summary Card
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkGrayCard else LightGrayCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            val fromRate = viewModel.currencyRatesMap[viewModel.currencyFromCode] ?: 1.0
            val toRate = viewModel.currencyRatesMap[viewModel.currencyToCode] ?: 1.0
            val reference = toRate / fromRate

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Reference Rate Value:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "1 ${viewModel.currencyFromCode} = ${viewModel.df.format(reference)} ${viewModel.currencyToCode}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueAccent
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Last Updated:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = viewModel.lastUpdatedTimeByStr,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.LightGray else DarkCharcoalText
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Status:",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = viewModel.currencyStatusMessage,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (viewModel.currencyStatusMessage == "Updated Just Now") DeepProfessionalBlue else Color.Gray
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.saveCurrencyClicked() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
            modifier = Modifier.fillMaxWidth().testTag("currency_save_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show Result", fontSize = 13.sp)
        }
    }
}

// Global text field color setup helper
@Composable
fun textColors(isDark: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedTextColor = if (isDark) Color.White else DarkCharcoalText,
    unfocusedTextColor = if (isDark) Color.White else DarkCharcoalText,
    focusedLabelColor = DeepProfessionalBlue,
    unfocusedLabelColor = Color.Gray,
    focusedBorderColor = DeepProfessionalBlue,
    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.7f),
    focusedContainerColor = if (isDark) DarkGrayCard else Color.White,
    unfocusedContainerColor = if (isDark) DarkGrayCard else Color.White
)

// ================= HISTORY SCREEN =================
@Composable
fun HistoryScreenView(viewModel: CalProViewModel) {
    val context = LocalContext.current
    val isDark = viewModel.isDarkTheme
    val historyGroupFlow = viewModel.historyItemsStateList.collectAsState()
    val historyList = historyGroupFlow.value

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Column {
                // Configured Banner Ad on History Screen too (Consistent Design System)
                AdMobBannerView(modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp))
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = { viewModel.navigateTo("home") },
                        modifier = Modifier.testTag("history_back_arrow")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate to Home Screen",
                            tint = if (isDark) Color.White else DarkCharcoalText
                        )
                    }
                    Text(
                        text = "History",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else DarkCharcoalText
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            onClick = { viewModel.navigateTo("privacy") },
                            modifier = Modifier.testTag("privacy_policy_nav_button")
                        ) {
                            Text(
                                text = "Privacy",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepProfessionalBlue
                            )
                        }
                        IconButton(
                            onClick = { viewModel.clearAllHistory() },
                            modifier = Modifier.testTag("history_clear_all_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "Clear all database items",
                                tint = Color.Red.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // High highlight Blue Action Download Button at bottom of History Screen
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = {
                        val formatSelection = "pdf" // Defaults or allows custom formats
                        viewModel.triggerHistoryDownload(context, formatSelection) { message ->
                            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("download_history_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(Icons.Default.Download, contentDescription = "Download custom file report")
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Download History", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
        },
        containerColor = if (isDark) DeepBlackBackground else SoftOffWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            if (historyList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.HistoryToggleOff,
                            contentDescription = null,
                            tint = Color.Gray.copy(alpha = 0.6f),
                            modifier = Modifier.size(54.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "No history items recorded yet.",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Group by Today, Yesterday, Older
                val groupedMap = remember(historyList) {
                    groupHistoryItems(historyList)
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .testTag("history_list_lazy"),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 10.dp)
                ) {
                    groupedMap.forEach { (dateGroupTitle, items) ->
                        item {
                            Text(
                                text = dateGroupTitle,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                        }

                        items(items, key = { it.id }) { item ->
                            HistoryItemCard(item = item, isDark = isDark) {
                                viewModel.deleteHistoryItem(item.id)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Group calculation items chronologically
fun groupHistoryItems(list: List<HistoryItem>): Map<String, List<HistoryItem>> {
    val grouped = LinkedHashMap<String, ArrayList<HistoryItem>>()
    val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val todayStr = dateFormat.format(Date())
    
    val cal = Calendar.getInstance()
    cal.add(Calendar.DATE, -1)
    val yesterdayStr = dateFormat.format(cal.time)

    list.forEach { item ->
        val groupName = when (item.dateString) {
            todayStr -> "TODAY"
            yesterdayStr -> "YESTERDAY"
            else -> "OLDER"
        }
        if (!grouped.containsKey(groupName)) {
            grouped[groupName] = ArrayList()
        }
        grouped[groupName]?.add(item)
    }
    return grouped
}

// ================= CUSTOM POLISHED HISTORY RECORD CARD =================
@Composable
fun HistoryItemCard(item: HistoryItem, isDark: Boolean, onDelete: () -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) DarkGrayCard else Color.White
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("history_card_item_${item.id}"),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Category & Timestamp Time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.toolName,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Gray
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.timeString,
                        fontSize = 10.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete item",
                        tint = Color.Red.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onDelete() }
                            .testTag("delete_item_icon_${item.id}")
                    )
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
            
            // Formula Input
            Text(
                text = item.inputs,
                fontSize = 12.sp,
                color = if (isDark) Color.LightGray else DarkCharcoalText
            )
            Spacer(modifier = Modifier.height(4.dp))
            
            // Evaluated Bold Output
            Text(
                text = item.outputs,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BlueAccent
            )
        }
    }
}

// ================= 1. PERCENTAGE CALCULATOR WORKSPACE =================
@Composable
fun PercentageCalculatorWorkspace(viewModel: CalProViewModel) {
    val isDark = viewModel.isDarkTheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
            .testTag("workspace_percentage"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Percentage calculation workspace", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        OutlinedTextField(
            value = viewModel.pctInputVal,
            onValueChange = {
                viewModel.pctInputVal = it
                viewModel.updatePercentage()
            },
            label = { Text("Percentage (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("percentage_input_pct"),
            colors = textColors(isDark)
        )

        // Quick presets row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val presets = listOf("5", "10", "15", "20", "25")
            presets.forEach { pr ->
                Button(
                    onClick = {
                        viewModel.pctInputVal = pr
                        viewModel.updatePercentage()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (viewModel.pctInputVal == pr) DeepProfessionalBlue else (if (isDark) DarkGrayCard else LightGrayCard)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "$pr%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (viewModel.pctInputVal == pr) Color.White else (if (isDark) Color.White else DarkCharcoalText)
                    )
                }
            }
        }

        OutlinedTextField(
            value = viewModel.pctTotalVal,
            onValueChange = {
                viewModel.pctTotalVal = it
                viewModel.updatePercentage()
            },
            label = { Text("Total Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("percentage_input_total"),
            colors = textColors(isDark)
        )

        // Result Live Card format
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkGrayCard else LightGrayCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Percentage Result Value:", fontSize = 11.sp, color = Color.Gray)
                Text(
                    text = viewModel.df.format(viewModel.pctResultVal),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = BlueAccent
                )
            }
        }

        Button(
            onClick = { viewModel.savePercentageClicked() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
            modifier = Modifier.fillMaxWidth().testTag("percentage_save_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show Result", fontSize = 13.sp)
        }
    }
}

// ================= 2. DISCOUNT CALCULATOR WORKSPACE =================
@Composable
fun DiscountCalculatorWorkspace(viewModel: CalProViewModel) {
    val isDark = viewModel.isDarkTheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
            .testTag("workspace_discount"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Discount calculation workspace", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        OutlinedTextField(
            value = viewModel.discountOrigPrice,
            onValueChange = {
                viewModel.discountOrigPrice = it
                viewModel.updateDiscount()
            },
            label = { Text("Original Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("discount_input_orig"),
            colors = textColors(isDark)
        )

        OutlinedTextField(
            value = viewModel.discountPct,
            onValueChange = {
                viewModel.discountPct = it
                viewModel.updateDiscount()
            },
            label = { Text("Discount Percentage (%)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("discount_input_pct"),
            colors = textColors(isDark)
        )

        // Summary details
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkGrayCard else LightGrayCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Saved Amount:", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = viewModel.df.format(viewModel.discountSavings),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Red.copy(alpha = 0.8f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Final Discounted Price:", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = viewModel.df.format(viewModel.discountFinalPrice),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlueAccent
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.saveDiscountClicked() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
            modifier = Modifier.fillMaxWidth().testTag("discount_save_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show Result", fontSize = 13.sp)
        }
    }
}

// ================= 3. LOAN CALCULATOR WORKSPACE =================
@Composable
fun LoanCalculatorWorkspace(viewModel: CalProViewModel) {
    val isDark = viewModel.isDarkTheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
            .testTag("workspace_loan"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Loan calculation workspace", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        OutlinedTextField(
            value = viewModel.loanAmount,
            onValueChange = {
                viewModel.loanAmount = it
                viewModel.updateLoan()
            },
            label = { Text("Loan Amount") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("loan_input_amount"),
            colors = textColors(isDark)
        )

        OutlinedTextField(
            value = viewModel.loanInterestRate,
            onValueChange = {
                viewModel.loanInterestRate = it
                viewModel.updateLoan()
            },
            label = { Text("Interest Rate (% per annum)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("loan_input_rate"),
            colors = textColors(isDark)
        )

        OutlinedTextField(
            value = viewModel.loanTenureYears,
            onValueChange = {
                viewModel.loanTenureYears = it
                viewModel.updateLoan()
            },
            label = { Text("Duration (Years)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth().testTag("loan_input_years"),
            colors = textColors(isDark)
        )

        // Loan card layout breakdown
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkGrayCard else LightGrayCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Monthly Instalment:", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = "${viewModel.df.format(viewModel.loanEmi)} / mo",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueAccent
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Calculated Interest:", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = viewModel.df.format(viewModel.loanTotalInterest),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Payment Due:", fontSize = 11.sp, color = Color.Gray)
                    Text(
                        text = viewModel.df.format(viewModel.loanTotalPayment),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlueAccent
                    )
                }
            }
        }

        Button(
            onClick = { viewModel.saveLoanClicked() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
            modifier = Modifier.fillMaxWidth().testTag("loan_save_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show Result", fontSize = 13.sp)
        }
    }
}

// ================= 4. DATE DIFFERENCE WORKSPACE =================
@Composable
fun DateDifferenceCalculatorWorkspace(viewModel: CalProViewModel) {
    val isDark = viewModel.isDarkTheme
    val context = LocalContext.current
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 4.dp)
            .testTag("workspace_date_difference"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("Date difference calculation workspace", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.Gray)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) DarkGrayCard else LightGrayCard,
                    RoundedCornerShape(14.dp)
                )
                .clickable {
                    val date = viewModel.dateDiffStartDate
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            viewModel.dateDiffStartDate = LocalDate.of(year, month + 1, dayOfMonth)
                            viewModel.updateDateDifference()
                        },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    ).show()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Choose Start Date", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.dateDiffStartDate.format(formatter),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DarkCharcoalText
                )
            }
            Icon(Icons.Default.CalendarToday, contentDescription = "Pick start date", tint = DeepProfessionalBlue)
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isDark) DarkGrayCard else LightGrayCard,
                    RoundedCornerShape(14.dp)
                )
                .clickable {
                    val date = viewModel.dateDiffEndDate
                    DatePickerDialog(
                        context,
                        { _, year, month, dayOfMonth ->
                            viewModel.dateDiffEndDate = LocalDate.of(year, month + 1, dayOfMonth)
                            viewModel.updateDateDifference()
                        },
                        date.year,
                        date.monthValue - 1,
                        date.dayOfMonth
                    ).show()
                }
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Choose End Date", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = viewModel.dateDiffEndDate.format(formatter),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DarkCharcoalText
                )
            }
            Icon(Icons.Default.CalendarToday, contentDescription = "Pick end date", tint = DeepProfessionalBlue)
        }

        // Live breakdown details card
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = if (isDark) DarkGrayCard else LightGrayCard),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("YEARS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("${viewModel.dateDiffYears}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = BlueAccent)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("MONTHS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("${viewModel.dateDiffMonths}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = BlueAccent)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    Text("DAYS", fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Text("${viewModel.dateDiffDays}", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = BlueAccent)
                }
            }
        }

        Button(
            onClick = { viewModel.saveDateDifferenceClicked() },
            colors = ButtonDefaults.buttonColors(containerColor = DeepProfessionalBlue),
            modifier = Modifier.fillMaxWidth().testTag("date_difference_save_button")
        ) {
            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Show Result", fontSize = 13.sp)
        }
    }
}

// ================= PRIVACY POLICY SCREEN =================
@Composable
fun PrivacyPolicyScreenView(viewModel: CalProViewModel) {
    val isDark = viewModel.isDarkTheme
    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        modifier = Modifier.fillMaxSize(),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { viewModel.navigateTo("history") },
                    modifier = Modifier.testTag("privacy_back_arrow")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to History Screen",
                        tint = if (isDark) Color.White else DarkCharcoalText
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Privacy Policy",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else DarkCharcoalText
                )
            }
        },
        containerColor = if (isDark) DeepBlackBackground else SoftOffWhite
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) DarkGrayCard else LightGrayCard),
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Last Updated: 16 June 2026",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "Welcome to Cal Pro.\n\n" +
                               "Cal Pro is designed to provide calculation and utility tools while respecting user privacy. We are committed to transparency regarding how the application works and what information is collected.\n\n" +
                               "Cal Pro does not collect personal information such as:\n" +
                               "• Name\n" +
                               "• Email Address\n" +
                               "• Phone Number\n" +
                               "• Home Address\n" +
                               "• Government Identification\n" +
                               "unless explicitly provided by the user.\n\n" +
                               "Cal Pro stores certain information locally on the user\'s device. Examples include:\n" +
                               "• Calculator History\n" +
                               "• Currency Cache\n" +
                               "• Theme Preferences\n" +
                               "• App Settings\n\n" +
                               "This information remains on the user\'s device and is not uploaded to external servers by Cal Pro.\n\n" +
                               "The Currency Converter retrieves exchange rates from public currency exchange APIs. Only the information necessary to perform currency conversion is requested. No personal user information is transmitted for currency conversion.\n\n" +
                               "Cal Pro contains advertisements. Advertisements may be provided through Google AdMob or another advertising platform. Advertising providers may collect limited technical information required for ad delivery, measurement, fraud prevention and performance analysis according to their own privacy policies.\n\n" +
                               "Cal Pro does not sell user data.\n\n" +
                               "Cal Pro requests only the permissions required for core functionality. Examples may include:\n" +
                               "• Internet Access\n" +
                               "• Network Status Access\n" +
                               "• File Export Access (if required by Android version)\n\n" +
                               "No unnecessary permissions should be requested.\n\n" +
                               "Cal Pro does not perform hidden background activities unrelated to its stated functionality. The application does not secretly monitor users. The application does not secretly record audio. The application does not secretly access cameras. The application does not secretly track user location.\n\n" +
                               "When users download history files: The generated files are stored locally on the user\'s device, and users maintain full control over exported files.\n\n" +
                               "Cal Pro is optimized for modern Android devices. Recommended: Android 7.0 (Nougat), Android 7.1 (Nougat), Android 8.0 and above, Android 9, Android 10, Android 11, Android 12, Android 13, Android 14, Android 15. The application supports phones and tablets where compatible.\n\n" +
                               "Reasonable measures are taken to protect locally stored information. However, users are responsible for securing their own devices.\n\n" +
                               "The application may use:\n" +
                               "• Currency Exchange APIs\n" +
                               "• Google AdMob\n" +
                               "• Android System Services\n\n" +
                               "Each third-party provider is responsible for its own privacy practices. This Privacy Policy may be updated in future versions. Changes will become effective when the updated version of the application is released.\n\n" +
                               "Developer: Aura Tools\n" +
                               "Application: Cal Pro\n\n" +
                               "For support, feedback or privacy-related questions, users may contact the developer through the application\'s official support channel.",
                        fontSize = 14.sp,
                        lineHeight = 22.sp,
                        color = if (isDark) Color.White else DarkCharcoalText
                    )
                }
            }
        }
    }
}
