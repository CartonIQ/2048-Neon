package com.neon2048.app

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.neon2048.app.data.AppDatabase
import com.neon2048.app.data.ScoreEntity
import com.neon2048.app.databinding.FragmentStatsBinding
import com.neon2048.app.databinding.ItemScoreBinding
import java.text.DateFormat
import java.util.Date
import kotlin.math.max
import kotlinx.coroutines.launch

class StatsFragment : Fragment() {
    private var _binding: FragmentStatsBinding? = null
    private val binding get() = _binding!!
    private val adapter = ScoreAdapter()
    private var glowAnimator: ObjectAnimator? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStatsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        UiFx.neonGradient(binding.statsTitle)
        binding.scoresRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.scoresRecycler.adapter = adapter

        binding.root.alpha = 0f
        binding.root.animate().alpha(1f).setDuration(300).start()

        glowAnimator = ObjectAnimator.ofFloat(binding.emptyIcon, View.ALPHA, 0.45f, 1f).apply {
            duration = 1200
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        bindStats()
        loadScores()
    }

    private fun bindStats() {
        val p = Prefs.get(requireContext())
        val games = p.getInt(Prefs.STAT_GAMES, 0)
        val wins = p.getInt(Prefs.STAT_WINS, 0)
        val total = p.getInt(Prefs.STAT_TOTAL_SCORE, 0)
        val rateBase = max(games, wins)
        val rate = if (rateBase > 0) wins * 100 / rateBase else 0
        val avg = if (games > 0) total / games else 0
        binding.statGames.text = games.toString()
        binding.statWins.text = wins.toString()
        binding.statWinRate.text = rate.toString() + "%"
        binding.statBestTile.text = p.getInt(Prefs.STAT_BEST_TILE, 0).toString()
        binding.statAvg.text = avg.toString()
        binding.statMoves.text = p.getInt(Prefs.STAT_MOVES, 0).toString()
    }

    private fun loadScores() {
        val dao = AppDatabase.get(requireContext()).scoreDao()
        viewLifecycleOwner.lifecycleScope.launch {
            val list = dao.getTop10()
            adapter.submit(list)
            val empty = list.isEmpty()
            binding.emptyState.visibility = if (empty) View.VISIBLE else View.GONE
            binding.scoresRecycler.visibility = if (empty) View.GONE else View.VISIBLE
        }
    }

    override fun onDestroyView() {
        glowAnimator?.cancel()
        glowAnimator = null
        _binding = null
        super.onDestroyView()
    }
}

class ScoreAdapter : RecyclerView.Adapter<ScoreAdapter.Holder>() {
    private var items: List<ScoreEntity> = emptyList()
    private val dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

    fun submit(list: List<ScoreEntity>) {
        items = list
        notifyDataSetChanged()
    }

    class Holder(val binding: ItemScoreBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(ItemScoreBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val item = items[position]
        val ctx = holder.binding.root.context
        val rankColor = when (position) {
            0 -> Color.parseColor("#FFD93D")
            1 -> Color.parseColor("#C9D1E3")
            2 -> Color.parseColor("#E8985E")
            else -> ContextCompat.getColor(ctx, R.color.neon_cyan)
        }
        holder.binding.rankText.setTextColor(rankColor)
        holder.binding.rankText.text = "#" + (position + 1)
        holder.binding.scoreText.text = item.score.toString()
        holder.binding.winBadge.visibility =
            if (item.maxTile >= GameBoard.WIN_VALUE) View.VISIBLE else View.GONE
        holder.binding.metaText.text = ctx.getString(
            R.string.best_tile_format,
            item.maxTile,
            dateFormat.format(Date(item.timestamp))
        )
    }
}
