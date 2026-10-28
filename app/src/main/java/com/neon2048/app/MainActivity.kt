package com.neon2048.app

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.google.android.gms.ads.AdView
import com.neon2048.app.ads.AdManager
import com.neon2048.app.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var adView: AdView? = null
    private var tabsReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHost = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val nav = navHost.navController
        binding.bottomNav.setupWithNavController(nav)

        // Tab polish: the selected tab bounces and gives a tiny tick (skips the initial callback).
        nav.addOnDestinationChangedListener { _, destination, _ ->
            if (tabsReady) {
                binding.bottomNav.findViewById<View>(destination.id)?.let { bounce(it) }
                Feedback.tab(binding.bottomNav)
            }
            tabsReady = true
        }

        AdManager.init(this)
        val banner = AdManager.createBanner(this)
        binding.bannerContainer.addView(
            banner,
            FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL
            )
        )
        adView = banner

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(350).start()

        if (savedInstanceState == null) {
            // The glass dock floats up into place.
            val dock = binding.bottomNav
            dock.translationY = 120f * resources.displayMetrics.density
            dock.animate()
                .translationY(0f)
                .setStartDelay(150)
                .setDuration(600)
                .setInterpolator(OvershootInterpolator(0.9f))
                .start()
        }
    }

    private fun bounce(v: View) {
        v.animate().cancel()
        v.scaleX = 0.82f
        v.scaleY = 0.82f
        v.animate()
            .scaleX(1f)
            .scaleY(1f)
            .setStartDelay(0)
            .setDuration(360)
            .setInterpolator(OvershootInterpolator(3f))
            .start()
    }

    override fun onPause() {
        adView?.pause()
        super.onPause()
    }

    override fun onResume() {
        super.onResume()
        adView?.resume()
    }

    override fun onDestroy() {
        adView?.destroy()
        adView = null
        super.onDestroy()
    }
}

/** Button click feedback: scales to 0.95 while pressed plus a light haptic tick. Never consumes the touch. */
@SuppressLint("ClickableViewAccessibility")
fun View.enablePressScale() {
    setOnTouchListener { v, event ->
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(80).start()
                Feedback.click(v)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL ->
                v.animate().scaleX(1f).scaleY(1f).setDuration(80).start()
        }
        false
    }
}
