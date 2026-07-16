package com.drift.droiddrift

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.view.WindowManager.LayoutParams

class CursorOverlayManager(private val context: Context) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var cursorView: CursorView? = null
    private var layoutParams: LayoutParams? = null
    private var isAdded = false
    private val mainHandler = Handler(Looper.getMainLooper())

    private val cursorSize = (20 * context.resources.displayMetrics.density).toInt()

    fun updatePosition(x: Float, y: Float) {
        mainHandler.post {
            try {
                if (cursorView == null) {
                    cursorView = CursorView(context)
                    layoutParams = LayoutParams(
                        cursorSize,
                        cursorSize,
                        LayoutParams.TYPE_APPLICATION_OVERLAY,
                        LayoutParams.FLAG_NOT_FOCUSABLE or LayoutParams.FLAG_NOT_TOUCHABLE or LayoutParams.FLAG_LAYOUT_NO_LIMITS or LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                        PixelFormat.TRANSLUCENT
                    ).apply {
                        gravity = Gravity.TOP or Gravity.START
                        this.x = x.toInt() - cursorSize / 2
                        this.y = y.toInt() - cursorSize / 2
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                            layoutInDisplayCutoutMode = LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                    }
                }

                val params = layoutParams!!
                params.x = x.toInt() - cursorSize / 2
                params.y = y.toInt() - cursorSize / 2

                if (!isAdded) {
                    windowManager.addView(cursorView, params)
                    isAdded = true
                } else {
                    windowManager.updateViewLayout(cursorView, params)
                }
            } catch (e: Exception) {
                // Occurs if overlay permission is not granted
                e.printStackTrace()
            }
        }
    }

    fun hide() {
        mainHandler.post {
            try {
                if (isAdded && cursorView != null) {
                    windowManager.removeView(cursorView)
                    isAdded = false
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private inner class CursorView(context: Context) : View(context) {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#A680A6D0") // Translucent premium pointer fill (iPad style)
            style = Paint.Style.FILL
        }

        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F2FFFFFF") // Pure white border
            style = Paint.Style.STROKE
            strokeWidth = 3f
            setShadowLayer(4f, 0f, 2f, Color.parseColor("#4D000000")) // Soft drop shadow
        }

        init {
            setLayerType(LAYER_TYPE_SOFTWARE, null) // Required for setShadowLayer support
        }

        override fun onDraw(canvas: Canvas) {
            super.onDraw(canvas)
            val radius = width / 2f
            canvas.drawCircle(radius, radius, radius - 4f, paint)
            canvas.drawCircle(radius, radius, radius - 4f, borderPaint)
        }
    }
}
