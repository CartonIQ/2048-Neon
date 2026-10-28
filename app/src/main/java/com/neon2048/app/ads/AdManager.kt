package com.neon2048.app.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.OnUserEarnedRewardListener
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

/** AdMob wrapper. All unit IDs are Google's official TEST IDs. */
object AdManager {
    private const val TAG = "AdManager"
    const val APP_ID = "ca-app-pub-3940256099942544~3347511713"
    const val BANNER_ID = "ca-app-pub-9771662959477951/3669038764"
    const val INTERSTITIAL_ID = "ca-app-pub-9771662959477951/7465025572"
    const val REWARDED_ID = "ca-app-pub-9771662959477951/2944607891"

    private const val MIN_INTERSTITIAL_GAP_MS = 60_000L

    private var initialized = false
    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var rewarded: RewardedAd? = null
    private var rewardedLoading = false
    private var lastInterstitialShown = 0L

    fun init(context: Context) {
        if (initialized) return
        initialized = true
        val app = context.applicationContext
        MobileAds.initialize(app) { }
        loadInterstitial(app)
        loadRewarded(app)
    }

    fun createBanner(context: Context): AdView {
        val view = AdView(context)
        view.setAdSize(AdSize.BANNER)
        view.adUnitId = BANNER_ID
        view.loadAd(AdRequest.Builder().build())
        return view
    }

    fun loadInterstitial(context: Context) {
        if (interstitial != null || interstitialLoading) return
        interstitialLoading = true
        InterstitialAd.load(
            context.applicationContext,
            INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitial = ad
                    interstitialLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed: ${error.message}")
                    interstitial = null
                    interstitialLoading = false
                }
            }
        )
    }

    /** Shows an interstitial if one is ready and not too frequent. [onDone] is always called once. */
    fun showInterstitial(activity: Activity, onDone: () -> Unit) {
        val ad = interstitial
        val now = SystemClock.elapsedRealtime()
        val tooSoon = lastInterstitialShown != 0L && (now - lastInterstitialShown) < MIN_INTERSTITIAL_GAP_MS
        if (ad == null || tooSoon) {
            loadInterstitial(activity)
            onDone()
            return
        }
        interstitial = null
        val app = activity.applicationContext
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                lastInterstitialShown = SystemClock.elapsedRealtime()
                loadInterstitial(app)
                onDone()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadInterstitial(app)
                onDone()
            }
        }
        ad.show(activity)
    }

    fun loadRewarded(context: Context) {
        if (rewarded != null || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(
            context.applicationContext,
            REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewarded = ad
                    rewardedLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed: ${error.message}")
                    rewarded = null
                    rewardedLoading = false
                }
            }
        )
    }

    /** Shows a rewarded ad. [onReward] fires when earned; [onUnavailable] if no ad could be shown. */
    fun showRewarded(activity: Activity, onReward: () -> Unit, onUnavailable: () -> Unit) {
        val ad = rewarded
        if (ad == null) {
            loadRewarded(activity)
            onUnavailable()
            return
        }
        rewarded = null
        val app = activity.applicationContext
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewarded(app)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                loadRewarded(app)
                onUnavailable()
            }
        }
        ad.show(activity, OnUserEarnedRewardListener { _ -> onReward() })
    }
}
