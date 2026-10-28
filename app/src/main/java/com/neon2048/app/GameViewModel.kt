package com.neon2048.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.neon2048.app.data.AppDatabase
import com.neon2048.app.data.ScoreEntity
import kotlinx.coroutines.launch

enum class GameStatus { PLAYING, WON, OVER }

class GameUiState(
    val cells: IntArray,
    val score: Int,
    val best: Int,
    val merged: Set<Int>,
    val spawned: Int,
    val status: GameStatus,
    val canUndo: Boolean,
    val moves: List<TileMove>,
    val before: IntArray?,
    val gained: Int
)

class GameViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs.get(app)
    private val dao = AppDatabase.get(app).scoreDao()
    private val board = GameBoard()

    private var prevCells: IntArray? = null
    private var prevScore = 0
    private var keepPlaying = prefs.getBoolean(Prefs.KEY_KEEP, false)
    private var scoreSaved = prefs.getBoolean(Prefs.KEY_SAVED, false)
    private var winRecorded = prefs.getBoolean(Prefs.KEY_WIN_RECORDED, false)
    private var lastSavedId: Long? = null
    private var status = GameStatus.PLAYING

    var bestScore: Int = prefs.getInt(Prefs.KEY_BEST, 0)
        private set

    /** Set by the UI once the end-of-game dialog for the current state was shown. */
    var dialogHandled = false

    private val _state = MutableLiveData<GameUiState>()
    val state: LiveData<GameUiState> get() = _state

    private val _hint = MutableLiveData<Direction?>()
    val hint: LiveData<Direction?> get() = _hint

    init {
        if (!restore()) {
            board.reset()
            keepPlaying = false
            scoreSaved = false
            winRecorded = false
        }
        status = computeStatus()
        if (status == GameStatus.OVER) saveScore()
        persist()
        publish(emptySet(), -1)
    }

    fun currentStatus(): GameStatus = status

    fun currentScore(): Int = board.score

    fun canUndo(): Boolean = prevCells != null

    fun move(dir: Direction): Boolean {
        if (status != GameStatus.PLAYING) return false
        val snapshot = board.cells.copyOf()
        val snapshotScore = board.score
        val result = board.slide(dir)
        if (!result.moved) return false
        prevCells = snapshot
        prevScore = snapshotScore
        val spawned = board.spawn()
        if (board.score > bestScore) {
            bestScore = board.score
            prefs.edit().putInt(Prefs.KEY_BEST, bestScore).apply()
        }
        status = computeStatus()
        dialogHandled = false

        bump(Prefs.STAT_MOVES, 1)
        if (board.maxTile() > prefs.getInt(Prefs.STAT_BEST_TILE, 0)) {
            prefs.edit().putInt(Prefs.STAT_BEST_TILE, board.maxTile()).apply()
        }
        if (status == GameStatus.WON && !winRecorded) {
            winRecorded = true
            bump(Prefs.STAT_WINS, 1)
        }
        if (status == GameStatus.OVER) saveScore()

        persist()
        publish(result.merged, spawned, result.moves, snapshot, result.gained)
        return true
    }

    fun undo(): Boolean {
        val previous = prevCells ?: return false
        if (scoreSaved) {
            // The finished game was already counted; take it back.
            bump(Prefs.STAT_GAMES, -1)
            bump(Prefs.STAT_TOTAL_SCORE, -board.score)
            scoreSaved = false
            val id = lastSavedId
            lastSavedId = null
            if (id != null) viewModelScope.launch { dao.deleteById(id) }
        }
        previous.copyInto(board.cells)
        board.score = prevScore
        prevCells = null
        status = computeStatus()
        dialogHandled = false
        persist()
        publish(emptySet(), -1)
        return true
    }

    fun newGame() {
        saveScore()
        board.reset()
        prevCells = null
        prevScore = 0
        keepPlaying = false
        scoreSaved = false
        winRecorded = false
        lastSavedId = null
        status = GameStatus.PLAYING
        dialogHandled = false
        persist()
        publish(emptySet(), -1)
    }

    fun continuePlaying() {
        keepPlaying = true
        status = computeStatus()
        dialogHandled = false
        persist()
        publish(emptySet(), -1)
    }

    fun requestHint() {
        _hint.value = board.bestMove()
    }

    fun consumeHint() {
        _hint.value = null
    }

    /** Clears leaderboard, best score and all statistics. */
    fun resetStats() {
        val e = prefs.edit()
        e.remove(Prefs.STAT_GAMES)
        e.remove(Prefs.STAT_WINS)
        e.remove(Prefs.STAT_MOVES)
        e.remove(Prefs.STAT_BEST_TILE)
        e.remove(Prefs.STAT_TOTAL_SCORE)
        e.putInt(Prefs.KEY_BEST, 0)
        e.apply()
        bestScore = board.score
        lastSavedId = null
        viewModelScope.launch { dao.clear() }
        publish(emptySet(), -1)
    }

    private fun bump(key: String, delta: Int) {
        val next = (prefs.getInt(key, 0) + delta).coerceAtLeast(0)
        prefs.edit().putInt(key, next).apply()
    }

    private fun computeStatus(): GameStatus = when {
        !keepPlaying && board.maxTile() >= GameBoard.WIN_VALUE -> GameStatus.WON
        !board.canMove() -> GameStatus.OVER
        else -> GameStatus.PLAYING
    }

    private fun saveScore() {
        if (scoreSaved || board.score <= 0) return
        scoreSaved = true
        bump(Prefs.STAT_GAMES, 1)
        bump(Prefs.STAT_TOTAL_SCORE, board.score)
        val entity = ScoreEntity(
            score = board.score,
            maxTile = board.maxTile(),
            timestamp = System.currentTimeMillis()
        )
        viewModelScope.launch {
            lastSavedId = dao.insert(entity)
            dao.trim()
        }
    }

    private fun restore(): Boolean {
        val saved = prefs.getString(Prefs.KEY_BOARD, null) ?: return false
        val parts = saved.split(",")
        if (parts.size != GameBoard.CELLS) return false
        val values = IntArray(GameBoard.CELLS)
        for (i in parts.indices) {
            values[i] = parts[i].trim().toIntOrNull() ?: return false
        }
        values.copyInto(board.cells)
        board.score = prefs.getInt(Prefs.KEY_SCORE, 0)
        return true
    }

    private fun persist() {
        prefs.edit()
            .putString(Prefs.KEY_BOARD, board.cells.joinToString(","))
            .putInt(Prefs.KEY_SCORE, board.score)
            .putBoolean(Prefs.KEY_KEEP, keepPlaying)
            .putBoolean(Prefs.KEY_SAVED, scoreSaved)
            .putBoolean(Prefs.KEY_WIN_RECORDED, winRecorded)
            .apply()
    }

    private fun publish(
        merged: Set<Int>,
        spawned: Int,
        moves: List<TileMove> = emptyList(),
        before: IntArray? = null,
        gained: Int = 0
    ) {
        _state.value = GameUiState(
            cells = board.cells.copyOf(),
            score = board.score,
            best = bestScore,
            merged = merged,
            spawned = spawned,
            status = status,
            canUndo = prevCells != null,
            moves = moves,
            before = before,
            gained = gained
        )
    }
}
