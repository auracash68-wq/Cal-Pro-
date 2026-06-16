package com.example.util

import android.app.Activity
import android.app.Application
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.*

object AdManager {
    private const val TAG = "AdManager"

    // Configurable Google AdMob Test Ad IDs
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"

    // Calculation counter state
    var calculationCount by mutableStateOf(0)
        private set

    // Ad presentation states
    var isRewardedShowing by mutableStateOf(false)
    var isFallbackRewardedShowing by mutableStateOf(false)

    var rewardedCountdown by mutableStateOf(5)

    // Banner states for adaptive rendering
    var isBannerLoading by mutableStateOf(false)
    var isBannerLoaded by mutableStateOf(false)
    var bannerStatusMessage by mutableStateOf("Initializing...")

    // Real Ad SDK instances
    var interstitialAd: InterstitialAd? = null
    var rewardedAd: RewardedAd? = null

    private var onRewardedCompleted: (() -> Unit)? = null
    private var jobRetry: Job? = null
    private var isInitialized = false

    // Active Activity Reference to safely display ads without crash / context failures
    private var currentActivity: Activity? = null

    private fun findActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        return null
    }

    private fun clearWebViewCache(context: Context) {
        try {
            Log.d(TAG, "Corrupted WebView Cache - Repairing and clearing...")
            // Clear standard WebView caches via directories
            val webViewCacheDir = java.io.File(context.cacheDir, "WebView")
            if (webViewCacheDir.exists()) {
                val deleted = webViewCacheDir.deleteRecursively()
                Log.d(TAG, "WebView Cache directory deleted: $deleted")
            }
            // Clear database files as well
            context.deleteDatabase("webview.db")
            context.deleteDatabase("webviewCache.db")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear WebView cache", e)
        }
    }

    // Initialize AdMob SDK Immediately
    fun initializeAdMob(context: Context) {
        if (isInitialized) return
        Log.d(TAG, "Google Mobile Ads Initialization - Starting...")
        
        // Safely clear WebView cache to prevent Chromium cache corruption errors before initialization
        clearWebViewCache(context)
        
        // Track the current activity for future ad rendering
        findActivity(context)?.let {
            currentActivity = it
        }

        // Dynamically track current activity across its lifecycle
        (context.applicationContext as? Application)?.registerActivityLifecycleCallbacks(
            object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    currentActivity = activity
                }
                override fun onActivityStarted(activity: Activity) {
                    currentActivity = activity
                }
                override fun onActivityResumed(activity: Activity) {
                    currentActivity = activity
                }
                override fun onActivityPaused(activity: Activity) {}
                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (currentActivity == activity) {
                        currentActivity = null
                    }
                }
            }
        )

        try {
            MobileAds.initialize(context) { status ->
                isInitialized = true
                Log.d(TAG, "Google Mobile Ads Initialization - Success: Initialized successfully")
                
                // Once initialized, load interstitial and rewarded ads in advance
                loadInterstitial(context)
                loadRewarded(context)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Google Mobile Ads Initialization - Failure", e)
        }
    }

    // Check Network Availability
    fun isNetworkAvailable(context: Context): Boolean {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
            val activeNetwork = connectivityManager.activeNetwork ?: return true
            val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork) ?: return true
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            true
        }
    }

    // Adaptive Banner Ad Loader with Automatic 30 Seconds Retry & Error Handling
    fun loadBannerAd(context: Context) {
        isBannerLoading = true
        bannerStatusMessage = "Loading Ad..."
        
        CoroutineScope(Dispatchers.Main).launch {
            delay(1000)
            if (!isNetworkAvailable(context)) {
                Log.e(TAG, "Ad Load Failure - Banner failed: Network Error (No Internet)")
                isBannerLoaded = false
                isBannerLoading = false
                bannerStatusMessage = "No Internet Connection"
                scheduleBannerRetry(context)
                return@launch
            }
            
            // AdManager keeps tracking banner status
            isBannerLoading = false
            isBannerLoaded = true
            bannerStatusMessage = "Banner Ready"
        }
    }

    private fun scheduleBannerRetry(context: Context) {
        jobRetry?.cancel()
        jobRetry = CoroutineScope(Dispatchers.Main).launch {
            Log.d(TAG, "AdMob: Banner retry scheduled in 30 seconds...")
            delay(30000)
            loadBannerAd(context)
        }
    }

    fun loadInterstitial(context: Context) {
        Log.d("INTERSTITIAL", "Loading")
        Log.d(TAG, "Ad Request - Loading Interstitial Ad...")
        val adRequest = AdRequest.Builder().build()
        
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_UNIT_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d("INTERSTITIAL", "Loaded")
                    Log.d(TAG, "Ad Load Success - Interstitial Loaded successfully")
                    
                    // Setup callback for dismiss/failure
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Log.d("INTERSTITIAL", "Showing")
                            Log.d(TAG, "Ad Show Success - Interstitial Shown")
                        }
                        
                        override fun onAdDismissedFullScreenContent() {
                            Log.d("INTERSTITIAL", "Dismissed")
                            Log.d(TAG, "Ad Dismissed - Interstitial Dismissed")
                            interstitialAd = null
                            loadInterstitial(context) // Preload next one
                        }
                        
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.e("INTERSTITIAL", "Show Failed - Code: ${error.code}, Domain: ${error.domain}, Message: ${error.message}")
                            Log.e(TAG, "Ad Show Failure - Interstitial Failed to show: ${error.message}")
                            interstitialAd = null
                            loadInterstitial(context)
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e("INTERSTITIAL", "Failed: ${error.message}")
                    Log.e("INTERSTITIAL", "Details - Code: ${error.code}, Domain: ${error.domain}, ResponseInfo: ${error.responseInfo}")
                    Log.e(TAG, "Ad Load Failure - Interstitial Failed to load: ${error.message}. Retrying in 15 seconds...")
                    interstitialAd = null
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(15000)
                        loadInterstitial(context)
                    }
                }
            }
        )
    }

    fun loadRewarded(context: Context) {
        Log.d(TAG, "Ad Request - Loading Rewarded Ad...")
        val adRequest = AdRequest.Builder().build()
        
        RewardedAd.load(
            context,
            REWARDED_AD_UNIT_ID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d(TAG, "Ad Load Success - Rewarded Loaded successfully")
                    
                    ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                        override fun onAdShowedFullScreenContent() {
                            Log.d(TAG, "Ad Show Success - Rewarded Shown")
                            isRewardedShowing = true
                        }
                        
                        override fun onAdDismissedFullScreenContent() {
                            Log.d(TAG, "Ad Dismissed - Rewarded Dismissed")
                            isRewardedShowing = false
                            rewardedAd = null
                            loadRewarded(context) // Preload next
                        }
                        
                        override fun onAdFailedToShowFullScreenContent(error: AdError) {
                            Log.e(TAG, "Ad Show Failure - Rewarded Failed to show: ${error.message}")
                            isRewardedShowing = false
                            rewardedAd = null
                            loadRewarded(context)
                        }
                    }
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.e(TAG, "Ad Load Failure - Rewarded Failed to load: ${error.message}. Retrying in 15 seconds...")
                    rewardedAd = null
                    CoroutineScope(Dispatchers.Main).launch {
                        delay(15000)
                        loadRewarded(context)
                    }
                }
            }
        )
    }

    // Register a calculation. Triggers interstitial every 4 calculations.
    fun incrementCalculation(context: Context, scope: CoroutineScope) {
        calculationCount++
        if (calculationCount >= 4) {
            calculationCount = 0
            scope.launch {
                delay(4000)
                showInterstitial(context)
            }
        }
    }

    fun showInterstitial(context: Context) {
        val activity = findActivity(context) ?: currentActivity
        if (activity != null && interstitialAd != null) {
            interstitialAd?.show(activity)
        } else {
            Log.d(TAG, "Ad Show Failure - Interstitial ad not ready or invalid activity context. activity: $activity, adReady: ${interstitialAd != null}")
        }
    }

    fun showRewarded(context: Context, onCompleted: () -> Unit) {
        onRewardedCompleted = onCompleted
        val activity = findActivity(context) ?: currentActivity
        if (activity != null && rewardedAd != null) {
            rewardedAd?.show(activity) { rewardItem ->
                Log.d(TAG, "Reward Earned - User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                onRewardedCompleted?.invoke()
                onRewardedCompleted = null
            }
        } else {
            Log.d(TAG, "Ad Show Failure - Rewarded ad not ready or invalid context. Providing reward fallback. activity: $activity, adReady: ${rewardedAd != null}")
            // Fallback: Provide fallback reward and countdown to never dead-unlock features
            simulateFallbackRewarded()
        }
    }

    private fun simulateFallbackRewarded() {
        rewardedCountdown = 5
        isFallbackRewardedShowing = true
        CoroutineScope(Dispatchers.Main).launch {
            while (rewardedCountdown > 0 && isFallbackRewardedShowing) {
                delay(1000)
                rewardedCountdown--
            }
            if (isFallbackRewardedShowing) {
                Log.d(TAG, "Reward Earned - User completed premium ad viewing simulated reward")
                onRewardedCompleted?.invoke()
                onRewardedCompleted = null
                isFallbackRewardedShowing = false
                isRewardedShowing = false
            }
        }
    }

    fun dismissRewarded() {
        Log.e(TAG, "Ad Dismissed - Rewarded Dismissed early by user")
        isFallbackRewardedShowing = false
        isRewardedShowing = false
        onRewardedCompleted = null
    }
}
