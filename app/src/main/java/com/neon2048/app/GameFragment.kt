package com.neon2048.app

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.GridLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.animation.doOnEnd
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.neon2048.app.ads.AdManager
import com.neon2048.app.databinding.FragmentGameBinding
import kotlin.math.abs
import kotlin.math.max

class GameFragment : Fragment() {
    private var _binding: FragmentGameBinding? = null
    private val binding get() = _binding!!

    private val vm: GameViewModel by activityViewModels()
    private val tileViews = ArrayList<TextView>(GameBoard.CELLS)
    private var tileTheme = TileTheme.NEON
    private var firstRender = true
    private var downX = 0f
    private var downY = 0f

    private var slideAnimator: ValueAnimator? = null
    private var slideToken = 0
    private var scoreAnimator: ValueAnimator? = null
    private var displayedScore = 0

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentGameBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tileTheme = TileTheme.fromIndex(
            Prefs.get(requireContext()).getInt(Prefs.KEY_TILE_THEME, 0)
        )
        firstRender = true
        buildGrid()
        setupButtons()
        setupSwipe()
        setupHelp()

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(300).start()

        vm.state.observe(viewLifecycleOwner) { render(it) }
        vm.hint.observe(viewLifecycleOwner) { dir ->
            if (dir != null) {
                showHint(dir)
                vm.consumeHint()
            }
        }
    }

    private fun setupHelp() {
        UiFx.neonGradient(binding.titleText)
        binding.helpButton.enablePressScale()
        binding.helpButton.setOnClickListener { HowToPlay.show(requireContext()) }

        // First launch: show the rules automatically, once.
        val prefs = Prefs.get(requireContext())
        if (!prefs.getBoolean(Prefs.KEY_HOWTO_SEEN, false)) {
            prefs.edit().putBoolean(Prefs.KEY_HOWTO_SEEN, true).apply()
            binding.root.post {
                if (_binding != null && isAdded) HowToPlay.show(requireContext())
            }
        }
    }

    private fun dp(v: Float): Float = v * resources.displayMetrics.density

    private fun newCellView(): TextView {
        val tv = TextView(requireContext())
        tv.gravity = Gravity.CENTER
        tv.typeface = Typeface.DEFAULT_BOLD
        tv.includeFontPadding = false
        return tv
    }

    private fun buildGrid() {
        tileViews.clear()
        binding.gridLayout.removeAllViews()
        val margin = dp(4f).toInt()
        for (i in 0 until GameBoard.CELLS) {
            val tv = newCellView()
            val lp = GridLayout.LayoutParams(
                GridLayout.spec(i / GameBoard.SIZE, GridLayout.FILL, 1f),
                GridLayout.spec(i % GameBoard.SIZE, GridLayout.FILL, 1f)
            )
            lp.width = 0
            lp.height = 0
            lp.setMargins(margin, margin, margin, margin)
            tv.layoutParams = lp
            binding.gridLayout.addView(tv)
            tileViews.add(tv)
        }
    }

    private fun setupButtons() {
        binding.undoButton.enablePressScale()
        binding.hintButton.enablePressScale()
        binding.newGameButton.enablePressScale()

        binding.undoButton.setOnClickListener {
            if (!vm.canUndo()) {
                toast(R.string.nothing_to_undo)
                return@setOnClickListener
            }
            AdManager.showRewarded(
                requireActivity(),
                onReward = { vm.undo() },
                onUnavailable = { toast(R.string.ad_not_ready) }
            )
        }

        binding.hintButton.setOnClickListener {
            if (vm.currentStatus() != GameStatus.PLAYING) {
                toast(R.string.no_moves)
                return@setOnClickListener
            }
            AdManager.showRewarded(
                requireActivity(),
                onReward = { vm.requestHint() },
                onUnavailable = { toast(R.string.ad_not_ready) }
            )
        }

        binding.newGameButton.setOnClickListener {
            if (vm.currentStatus() == GameStatus.PLAYING && vm.currentScore() > 0) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.confirm_new_title)
                    .setMessage(R.string.confirm_new_message)
                    .setPositiveButton(R.string.start) { _, _ -> startNewGame() }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            } else {
                startNewGame()
            }
        }
    }

    private fun startNewGame() {
        val act = activity ?: return
        if (vm.currentScore() == 0) {
            vm.newGame()
        } else {
            AdManager.showInterstitial(act) { vm.newGame() }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupSwipe() {
        val threshold = dp(40f)
        binding.root.setOnTouchListener { _, ev ->
            when (ev.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    downX = ev.x
                    downY = ev.y
                }
                MotionEvent.ACTION_UP -> {
                    val dx = ev.x - downX
                    val dy = ev.y - downY
                    if (max(abs(dx), abs(dy)) >= threshold) {
                        val dir = if (abs(dx) > abs(dy)) {
                            if (dx > 0) Direction.RIGHT else Direction.LEFT
                        } else {
                            if (dy > 0) Direction.DOWN else Direction.UP
                        }
                        vm.move(dir)
                    }
                }
            }
            true
        }
    }

    // ------------------------------------------------------------------ rendering

    private fun render(s: GameUiState) {
        cancelSlide()
        updateScores(s)
        binding.undoButton.alpha = if (s.canUndo) 1f else 0.6f

        val canAnimate = !firstRender &&
            s.moves.isNotEmpty() &&
            binding.gridLayout.width > 0 &&
            tileViews.all { it.width > 0 }
        firstRender = false

        if (canAnimate) {
            feedbackFor(s)
            slide(s)
        } else {
            applyBoard(s)
            maybeShowDialog(s)
        }
    }

    /** Styles one tile view: text, gradient/flat background, text color and neon glow. */
    private fun styleTile(tv: TextView, v: Int) {
        val ctx = tv.context
        tv.text = if (v == 0) "" else v.toString()
        tv.background = Tile.background(ctx, tileTheme, v)
        tv.setTextColor(Tile.textColor(ctx, tileTheme, v))
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, Tile.textSizeSp(v))
        val glow = tileTheme == TileTheme.NEON && v >= 8
        tv.elevation = if (glow) dp(6f) else 0f
        if (glow && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val c = Tile.fillColor(TileTheme.NEON, v)
            tv.outlineSpotShadowColor = c
            tv.outlineAmbientShadowColor = c
        }
    }

    private fun applyBoard(s: GameUiState) {
        for (i in 0 until GameBoard.CELLS) {
            val tv = tileViews[i]
            tv.animate().cancel()
            tv.scaleX = 1f
            tv.scaleY = 1f
            styleTile(tv, s.cells[i])
        }
    }

    /** Smooth slide: every tile glides from its old cell to its new one in 110ms. */
    private fun slide(s: GameUiState) {
        val overlay = binding.tileOverlay
        overlay.removeAllViews()
        for (tv in tileViews) styleTile(tv, 0)

        val n = s.moves.size
        val views = ArrayList<TextView>(n)
        val fx = FloatArray(n)
        val fy = FloatArray(n)
        val tx = FloatArray(n)
        val ty = FloatArray(n)
        for (k in 0 until n) {
            val m = s.moves[k]
            val src = tileViews[m.from]
            val dst = tileViews[m.to]
            val tv = newCellView()
            styleTile(tv, m.value)
            overlay.addView(tv, FrameLayout.LayoutParams(src.width, src.height))
            fx[k] = src.left.toFloat()
            fy[k] = src.top.toFloat()
            tx[k] = dst.left.toFloat()
            ty[k] = dst.top.toFloat()
            tv.translationX = fx[k]
            tv.translationY = fy[k]
            views.add(tv)
        }

        val token = ++slideToken
        val anim = ValueAnimator.ofFloat(0f, 1f)
        anim.duration = 110
        anim.interpolator = DecelerateInterpolator()
        anim.addUpdateListener { a ->
            val t = a.animatedValue as Float
            for (k in 0 until n) {
                views[k].translationX = fx[k] + (tx[k] - fx[k]) * t
                views[k].translationY = fy[k] + (ty[k] - fy[k]) * t
            }
        }
        anim.doOnEnd {
            if (token == slideToken) finishSlide(s)
        }
        slideAnimator = anim
        anim.start()
    }

    private fun finishSlide(s: GameUiState) {
        slideAnimator = null
        binding.tileOverlay.removeAllViews()
        applyBoard(s)
        for (i in s.merged) pulse(tileViews[i])
        if (s.spawned in 0 until GameBoard.CELLS) popIn(tileViews[s.spawned])
        if (s.gained > 0) showGain(s.gained)
        maybeShowDialog(s)
    }

    private fun cancelSlide() {
        slideToken++
        slideAnimator?.cancel()
        slideAnimator = null
        _binding?.tileOverlay?.removeAllViews()
    }

    private fun feedbackFor(s: GameUiState) {
        if (s.merged.isNotEmpty()) {
            Feedback.merged(binding.root, s.merged.maxOf { s.cells[it] })
        } else {
            Feedback.moved(binding.root)
        }
    }

    /** Merge animation: scale 1.0 -> 1.1 -> 1.0 over 150ms. */
    private fun pulse(tv: View) {
        tv.animate()
            .scaleX(1.1f).scaleY(1.1f)
            .setDuration(75)
            .withEndAction { tv.animate().scaleX(1f).scaleY(1f).setDuration(75).start() }
            .start()
    }

    private fun popIn(tv: View) {
        tv.scaleX = 0f
        tv.scaleY = 0f
        tv.animate().scaleX(1f).scaleY(1f).setDuration(120).start()
    }

    private fun showGain(gain: Int) {
        val tv = binding.gainText
        tv.animate().cancel()
        tv.text = "+$gain"
        tv.alpha = 1f
        tv.translationY = 0f
        tv.visibility = View.VISIBLE
        tv.animate()
            .translationY(-dp(36f))
            .alpha(0f)
            .setStartDelay(0)
            .setDuration(650)
            .withEndAction { tv.visibility = View.GONE }
            .start()
    }

    private fun updateScores(s: GameUiState) {
        binding.bestScore.text = s.best.toString()
        scoreAnimator?.cancel()
        val from = displayedScore
        val to = s.score
        displayedScore = to
        if (firstRender || to <= from) {
            binding.scoreValue.text = to.toString()
            return
        }
        val a = ValueAnimator.ofInt(from, to)
        a.duration = 250
        a.addUpdateListener { anim ->
            _binding?.scoreValue?.text = (anim.animatedValue as Int).toString()
        }
        scoreAnimator = a
        a.start()
        val tv = binding.scoreValue
        tv.animate().cancel()
        tv.animate()
            .scaleX(1.15f).scaleY(1.15f)
            .setDuration(100)
            .withEndAction { tv.animate().scaleX(1f).scaleY(1f).setDuration(100).start() }
            .start()
    }

    // ------------------------------------------------------------------ dialogs & hints

    private fun maybeShowDialog(s: GameUiState) {
        if (s.status == GameStatus.PLAYING || vm.dialogHandled) return
        vm.dialogHandled = true
        if (s.status == GameStatus.WON) {
            // Win logic: fanfare + confetti first, then the Victory screen.
            Feedback.won(binding.root)
            binding.confettiView.burst()
            binding.root.postDelayed({
                if (_binding != null && isAdded) {
                    showEndDialog(s)
                } else {
                    vm.dialogHandled = false
                }
            }, 1100)
        } else {
            Feedback.over(binding.root)
            showEndDialog(s)
        }
    }

    private fun showEndDialog(s: GameUiState) {
        val ctx = requireContext()
        if (s.status == GameStatus.WON) {
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.victory_title)
                .setMessage(getString(R.string.victory_message, s.score))
                .setCancelable(false)
                .setPositiveButton(R.string.keep_going) { _, _ -> vm.continuePlaying() }
                .setNegativeButton(R.string.new_game_short) { _, _ -> startNewGame() }
                .show()
        } else {
            MaterialAlertDialogBuilder(ctx)
                .setTitle(R.string.game_over_title)
                .setMessage(getString(R.string.game_over_message, s.score))
                .setCancelable(false)
                .setPositiveButton(R.string.new_game_short) { _, _ -> startNewGame() }
                .setNegativeButton(R.string.close, null)
                .show()
        }
    }

    private fun showHint(dir: Direction) {
        val arrow = when (dir) {
            Direction.LEFT -> "\u2190"
            Direction.RIGHT -> "\u2192"
            Direction.UP -> "\u2191"
            Direction.DOWN -> "\u2193"
        }
        val msg = when (dir) {
            Direction.LEFT -> R.string.hint_left
            Direction.RIGHT -> R.string.hint_right
            Direction.UP -> R.string.hint_up
            Direction.DOWN -> R.string.hint_down
        }
        toast(msg)
        val tv = binding.hintArrow
        tv.animate().cancel()
        tv.text = arrow
        tv.alpha = 0f
        tv.visibility = View.VISIBLE
        tv.animate()
            .alpha(1f)
            .setStartDelay(0)
            .setDuration(250)
            .withEndAction {
                tv.animate()
                    .alpha(0f)
                    .setStartDelay(1500)
                    .setDuration(400)
                    .withEndAction { tv.visibility = View.GONE }
                    .start()
            }
            .start()
    }

    private fun toast(resId: Int) {
        context?.let { Toast.makeText(it, resId, Toast.LENGTH_SHORT).show() }
    }

    override fun onDestroyView() {
        cancelSlide()
        scoreAnimator?.cancel()
        scoreAnimator = null
        tileViews.forEach { it.animate().cancel() }
        tileViews.clear()
        binding.hintArrow.animate().cancel()
        binding.gainText.animate().cancel()
        binding.confettiView.cancel()
        _binding = null
        super.onDestroyView()
    }
}
