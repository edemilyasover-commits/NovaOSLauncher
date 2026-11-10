package com.edemilyasover.tetrisclassic

import android.app.Activity
import android.os.Bundle
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.os.Handler
import android.os.Looper
import kotlin.random.Random

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = Color.BLACK
        window.navigationBarColor = Color.BLACK
        window.decorView.systemUiVisibility = 0
        setContentView(GameView(this))
    }
}

private class GameView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cols = 10
    private val rows = 20
    private val board = Array(rows) { IntArray(cols) }
    private val shapes = listOf(
        arrayOf(intArrayOf(1,1,1,1)),
        arrayOf(intArrayOf(1,1), intArrayOf(1,1)),
        arrayOf(intArrayOf(0,1,0), intArrayOf(1,1,1)),
        arrayOf(intArrayOf(1,0,0), intArrayOf(1,1,1)),
        arrayOf(intArrayOf(0,0,1), intArrayOf(1,1,1)),
        arrayOf(intArrayOf(0,1,1), intArrayOf(1,1,0)),
        arrayOf(intArrayOf(1,1,0), intArrayOf(0,1,1))
    )
    private var shape = shapes[Random.nextInt(shapes.size)].map { it.clone() }.toTypedArray()
    private var px = 3
    private var py = 0
    private var score = 0
    private var lines = 0
    private var paused = false
    private var over = false
    private val prefs = context.getSharedPreferences("tetris", Context.MODE_PRIVATE)
    private var best = prefs.getInt("best", 0)
    private val handler = Handler(Looper.getMainLooper())
    private val tick = object : Runnable {
        override fun run() {
            if (!paused && !over) stepDown()
            invalidate()
            handler.postDelayed(this, (650L - (lines / 5) * 45L).coerceAtLeast(120L))
        }
    }

    init { setLayerType(View.LAYER_TYPE_SOFTWARE, null); handler.postDelayed(tick, 650) }

    private fun collides(s: Array<IntArray>, x: Int, y: Int): Boolean {
        for (r in s.indices) for (c in s[r].indices) if (s[r][c] != 0) {
            val nx = x + c; val ny = y + r
            if (nx !in 0 until cols || ny >= rows || (ny >= 0 && board[ny][nx] != 0)) return true
        }
        return false
    }

    private fun stepDown() {
        if (!collides(shape, px, py + 1)) py++ else lockPiece()
    }

    private fun lockPiece() {
        for (r in shape.indices) for (c in shape[r].indices) if (shape[r][c] != 0) {
            val y = py + r; val x = px + c
            if (y < 0) { gameOver(); return }
            if (y in 0 until rows && x in 0 until cols) board[y][x] = 1
        }
        var cleared = 0
        for (r in rows - 1 downTo 0) {
            if (board[r].all { it != 0 }) {
                for (y in r downTo 1) board[y] = board[y - 1].clone()
                board[0] = IntArray(cols)
                cleared++; // recheck this row after shifting
                // keep the same index for the next loop iteration
                if (r < rows - 1) { /* normal iteration continues */ }
            }
        }
        if (cleared > 0) {
            lines += cleared
            score += when (cleared) { 1 -> 100; 2 -> 300; 3 -> 500; else -> 800 }
            if (score > best) { best = score; prefs.edit().putInt("best", best).apply() }
        }
        spawn()
    }

    private fun spawn() {
        shape = shapes[Random.nextInt(shapes.size)].map { it.clone() }.toTypedArray()
        px = (cols - shape[0].size) / 2
        py = 0
        if (collides(shape, px, py)) gameOver()
    }

    private fun gameOver() { over = true; if (score > best) { best = score; prefs.edit().putInt("best", best).apply() } }

    private fun rotate() {
        val h = shape.size; val w = shape[0].size
        val rotated = Array(w) { r -> IntArray(h) { c -> shape[h - 1 - c][r] } }
        if (!collides(rotated, px, py)) shape = rotated
        else if (!collides(rotated, px - 1, py)) { px--; shape = rotated }
        else if (!collides(rotated, px + 1, py)) { px++; shape = rotated }
    }

    private fun restart() {
        for (r in 0 until rows) board[r] = IntArray(cols)
        score = 0; lines = 0; px = 3; py = 0; paused = false; over = false; spawn(); invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.BLACK)
        val cell = (width * 0.88f / cols).coerceAtMost((height * 0.62f) / rows)
        val left = (width - cell * cols) / 2f
        val top = height * 0.16f
        paint.color = Color.WHITE; paint.textAlign = Paint.Align.CENTER; paint.textSize = width * 0.075f; paint.typeface = android.graphics.Typeface.create("monospace", android.graphics.Typeface.BOLD)
        canvas.drawText("T E T R I S", width / 2f, height * 0.075f, paint)
        paint.textSize = width * 0.038f
        canvas.drawText("SCORE  $score        BEST  $best", width / 2f, height * 0.125f, paint)
        paint.style = Paint.Style.STROKE; paint.strokeWidth = 2f; paint.color = Color.WHITE
        canvas.drawRect(left - 2, top - 2, left + cell * cols + 2, top + cell * rows + 2, paint)
        paint.style = Paint.Style.FILL
        for (r in 0 until rows) for (c in 0 until cols) if (board[r][c] != 0) drawCell(canvas, left, top, cell, c, r)
        for (r in shape.indices) for (c in shape[r].indices) if (shape[r][c] != 0) drawCell(canvas, left, top, cell, px + c, py + r)
        val controlsY = top + cell * rows + height * 0.065f
        paint.color = Color.WHITE; paint.textSize = width * 0.043f
        canvas.drawText("◀       ▼       ▶       ↻", width / 2f, controlsY, paint)
        paint.textSize = width * 0.032f
        canvas.drawText("MOVE       DROP       MOVE       ROTATE", width / 2f, controlsY + height * 0.035f, paint)
        paint.textSize = width * 0.04f
        canvas.drawText(if (over) "GAME OVER  •  TAP TO RESTART" else if (paused) "PAUSED  •  TAP TO RESUME" else "TAP LEFT / RIGHT / ROTATE", width / 2f, height * 0.97f, paint)
        if (over || paused) {
            paint.color = Color.BLACK; paint.alpha = 190
            canvas.drawRect(0f, top + cell * 7, width.toFloat(), top + cell * 13, paint)
            paint.alpha = 255; paint.color = Color.WHITE; paint.textSize = width * 0.055f
            canvas.drawText(if (over) "GAME OVER" else "PAUSED", width / 2f, top + cell * 10, paint)
            canvas.drawText(if (over) "TAP TO PLAY AGAIN" else "TAP TO CONTINUE", width / 2f, top + cell * 11.2f, paint)
        }
    }

    private fun drawCell(canvas: Canvas, left: Float, top: Float, cell: Float, x: Int, y: Int) {
        paint.color = Color.WHITE; paint.style = Paint.Style.FILL
        val inset = cell * 0.045f
        canvas.drawRect(left + x * cell + inset, top + y * cell + inset, left + (x + 1) * cell - inset, top + (y + 1) * cell - inset, paint)
        paint.color = Color.BLACK; paint.style = Paint.Style.STROKE; paint.strokeWidth = 1.5f
        canvas.drawRect(left + x * cell + cell * 0.18f, top + y * cell + cell * 0.18f, left + (x + 1) * cell - cell * 0.18f, top + (y + 1) * cell - cell * 0.18f, paint)
        paint.style = Paint.Style.FILL
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action != MotionEvent.ACTION_UP) return true
        if (over) restart() else if (paused) paused = false else {
            val x = event.x / width
            when {
                x < 0.30f -> if (!collides(shape, px - 1, py)) px--
                x < 0.52f -> { while (!collides(shape, px, py + 1)) py++; stepDown() }
                x < 0.75f -> if (!collides(shape, px + 1, py)) px++
                else -> rotate()
            }
        }
        invalidate()
        return true
    }

    override fun onDetachedFromWindow() { handler.removeCallbacks(tick); super.onDetachedFromWindow() }
}
