package com.neon2048.app

import kotlin.random.Random

/** Pure 2048 game logic on a 4x4 grid stored row-major in [cells] (0 = empty). */
class GameBoard {
    companion object {
        const val SIZE = 4
        const val CELLS = SIZE * SIZE
        const val WIN_VALUE = 2048
    }

    val cells = IntArray(CELLS)
    var score = 0

    fun reset() {
        cells.fill(0)
        score = 0
        spawn()
        spawn()
    }

    fun copy(): GameBoard {
        val b = GameBoard()
        cells.copyInto(b.cells)
        b.score = score
        return b
    }

    fun maxTile(): Int = cells.maxOrNull() ?: 0

    fun emptyCount(): Int = cells.count { it == 0 }

    /** Spawns a 2 (90%) or 4 (10%) on a random empty cell. Returns its index or -1. */
    fun spawn(): Int {
        val empties = cells.indices.filter { cells[it] == 0 }
        if (empties.isEmpty()) return -1
        val idx = empties[Random.nextInt(empties.size)]
        cells[idx] = if (Random.nextInt(10) == 0) 4 else 2
        return idx
    }

    fun canMove(): Boolean {
        if (cells.any { it == 0 }) return true
        for (r in 0 until SIZE) {
            for (c in 0 until SIZE) {
                val v = cells[r * SIZE + c]
                if (c < SIZE - 1 && cells[r * SIZE + c + 1] == v) return true
                if (r < SIZE - 1 && cells[(r + 1) * SIZE + c] == v) return true
            }
        }
        return false
    }

    /** Slides and merges all tiles in [dir]. Does not spawn a new tile. */
    fun slide(dir: Direction): MoveResult {
        var moved = false
        var gained = 0
        val merged = mutableSetOf<Int>()
        val moves = ArrayList<TileMove>()
        for (line in 0 until SIZE) {
            val idx = IntArray(SIZE) { k ->
                when (dir) {
                    Direction.LEFT -> line * SIZE + k
                    Direction.RIGHT -> line * SIZE + (SIZE - 1 - k)
                    Direction.UP -> k * SIZE + line
                    Direction.DOWN -> (SIZE - 1 - k) * SIZE + line
                }
            }
            val src = idx.filter { cells[it] != 0 }
            val values = src.map { cells[it] }
            val out = IntArray(SIZE)
            var w = 0
            var i = 0
            while (i < values.size) {
                if (i + 1 < values.size && values[i] == values[i + 1]) {
                    val m = values[i] * 2
                    out[w] = m
                    gained += m
                    merged.add(idx[w])
                    moves.add(TileMove(src[i], idx[w], values[i]))
                    moves.add(TileMove(src[i + 1], idx[w], values[i + 1]))
                    w++
                    i += 2
                } else {
                    out[w] = values[i]
                    moves.add(TileMove(src[i], idx[w], values[i]))
                    w++
                    i++
                }
            }
            for (k in 0 until SIZE) {
                if (cells[idx[k]] != out[k]) {
                    moved = true
                    cells[idx[k]] = out[k]
                }
            }
        }
        score += gained
        return MoveResult(moved, gained, merged, moves)
    }

    private fun heuristic(b: GameBoard, gained: Int): Double {
        var h = gained * 2.0 + b.emptyCount() * 50.0
        val max = b.maxTile()
        if (intArrayOf(0, SIZE - 1, CELLS - SIZE, CELLS - 1).any { b.cells[it] == max }) h += 100.0
        return h
    }

    /** Greedy two-ply lookahead used by the Hint feature. Null when no move is possible. */
    fun bestMove(): Direction? {
        var best: Direction? = null
        var bestValue = Double.NEGATIVE_INFINITY
        for (d in Direction.values()) {
            val b1 = copy()
            val r1 = b1.slide(d)
            if (!r1.moved) continue
            var value = heuristic(b1, r1.gained)
            var follow = Double.NEGATIVE_INFINITY
            for (d2 in Direction.values()) {
                val b2 = b1.copy()
                val r2 = b2.slide(d2)
                if (r2.moved) follow = maxOf(follow, heuristic(b2, r2.gained))
            }
            if (follow > Double.NEGATIVE_INFINITY) value += 0.5 * follow
            if (value > bestValue) {
                bestValue = value
                best = d
            }
        }
        return best
    }
}
