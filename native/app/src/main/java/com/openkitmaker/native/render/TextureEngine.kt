// Implements: SCR-EDITOR / SCR-EXPORT / FUN-COMPOSITE-2D / FUN-EDIT-COLOR / FUN-MANAGE-LAYERS
package com.openkitmaker.native.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.openkitmaker.native.domain.BlendMode
import com.openkitmaker.native.domain.CollarStyle
import com.openkitmaker.native.domain.DecalShape
import com.openkitmaker.native.domain.Kit
import com.openkitmaker.native.domain.Layer
import com.openkitmaker.native.domain.LayerType
import com.openkitmaker.native.domain.PatternStyle
import kotlin.math.cos
import kotlin.math.sin

/**
 * Motore di compositing 2D preservato e adattato dalla baseline al Contratto di Parità.
 * Supporta risoluzione 2048x2048 RGBA_8888, testo curvato su arco (drawTextOnPath),
 * numeri bicolore con contorno inline/outline e geometrie procedurali prive di asset di terzi.
 */
object TextureEngine {

    const val FULL_RESOLUTION = 2048
    const val PREVIEW_RESOLUTION = 512

    fun compositeKitBitmap(
        kit: Kit,
        width: Int = FULL_RESOLUTION,
        height: Int = FULL_RESOLUTION
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val orderedLayers = kit.layers
            .filter { it.visible && it.opacity > 0.01f }
            .sortedBy { it.zIndex }

        for (layer in orderedLayers) {
            renderLayer(canvas, layer, width.toFloat(), height.toFloat())
        }

        drawUvSeamGuides(canvas, width.toFloat(), height.toFloat())
        return bitmap
    }

    private fun renderLayer(canvas: Canvas, layer: Layer, w: Float, h: Float) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            isFilterBitmap = true
            alpha = (layer.opacity.coerceIn(0f, 1f) * 255f).toInt()
            xfermode = when (layer.blendMode) {
                BlendMode.NORMAL -> null
                BlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.MULTIPLY)
                BlendMode.OVERLAY -> PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            }
        }

        val primaryInt = parseHexColor(layer.color, Color.parseColor("#0F52BA"))
        val secondaryInt = parseHexColor(layer.secondaryColor ?: "#FFFFFF", Color.WHITE)

        canvas.save()
        when (layer.type) {
            LayerType.BASE -> {
                paint.style = Paint.Style.FILL
                paint.color = primaryInt
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            LayerType.PATTERN -> {
                renderPattern(canvas, layer, w, h, primaryInt, secondaryInt, paint)
            }
            LayerType.SLEEVES -> {
                renderSleeves(canvas, w, h, primaryInt, secondaryInt, paint)
            }
            LayerType.COLLAR -> {
                renderCollar(canvas, layer, w, h, primaryInt, secondaryInt, paint)
            }
            LayerType.BADGE -> {
                renderGeometricBadge(canvas, layer, w, h, primaryInt, secondaryInt, paint)
            }
            LayerType.SPONSOR -> {
                renderProceduralSponsor(canvas, layer, w, h, primaryInt, secondaryInt, paint)
            }
            LayerType.TEXT_NAME -> {
                renderCurvedPlayerName(canvas, layer, w, h, primaryInt, secondaryInt, paint)
            }
            LayerType.NUMBER -> {
                renderBicolorNumber(canvas, layer, w, h, primaryInt, secondaryInt, paint)
            }
        }
        canvas.restore()
    }

    private fun renderPattern(
        canvas: Canvas,
        layer: Layer,
        w: Float,
        h: Float,
        col1: Int,
        col2: Int,
        paint: Paint
    ) {
        val freq = layer.properties.patternFrequency.coerceIn(2, 24)
        // Torso UV region (front: x in 0.05..0.48, back: x in 0.52..0.95)
        when (layer.properties.patternStyle) {
            PatternStyle.SOLID -> {
                paint.style = Paint.Style.FILL
                paint.color = col1
                canvas.drawRect(0f, 0f, w, h, paint)
            }
            PatternStyle.VERTICAL_STRIPES -> {
                val stripeW = w / (freq * 2f)
                paint.style = Paint.Style.FILL
                paint.color = col1
                for (i in 0 until freq * 2 step 2) {
                    canvas.drawRect(i * stripeW, 0f, (i + 1) * stripeW, h * 0.72f, paint)
                }
            }
            PatternStyle.HORIZONTAL_HOOPS -> {
                val hoopH = (h * 0.72f) / (freq * 2f)
                paint.style = Paint.Style.FILL
                paint.color = col1
                for (i in 0 until freq * 2 step 2) {
                    canvas.drawRect(0f, i * hoopH, w, (i + 1) * hoopH, paint)
                }
            }
            PatternStyle.DIAGONAL_SASH -> {
                paint.style = Paint.Style.FILL
                paint.color = col1
                val sashW = w * 0.09f
                val path = Path().apply {
                    moveTo(w * 0.06f, h * 0.08f)
                    lineTo(w * 0.06f + sashW, h * 0.08f)
                    lineTo(w * 0.46f, h * 0.68f)
                    lineTo(w * 0.46f - sashW, h * 0.68f)
                    close()
                }
                canvas.drawPath(path, paint)
            }
            PatternStyle.CHEVRON -> {
                paint.style = Paint.Style.FILL
                paint.color = col1
                val count = freq.coerceAtMost(8)
                val stepY = (h * 0.62f) / count
                val frontCx = w * 0.26f
                for (i in 0 until count step 2) {
                    val yTop = h * 0.12f + i * stepY
                    val path = Path().apply {
                        moveTo(w * 0.05f, yTop)
                        lineTo(frontCx, yTop + stepY * 0.85f)
                        lineTo(w * 0.47f, yTop)
                        lineTo(w * 0.47f, yTop + stepY * 0.55f)
                        lineTo(frontCx, yTop + stepY * 1.40f)
                        lineTo(w * 0.05f, yTop + stepY * 0.55f)
                        close()
                    }
                    canvas.drawPath(path, paint)
                }
            }
            PatternStyle.CHECKERBOARD -> {
                val cells = freq * 2
                val cellW = w / cells
                val cellH = (h * 0.72f) / cells
                paint.style = Paint.Style.FILL
                paint.color = col1
                for (r in 0 until cells) {
                    for (c in 0 until cells) {
                        if ((r + c) % 2 == 0) {
                            canvas.drawRect(c * cellW, r * cellH, (c + 1) * cellW, (r + 1) * cellH, paint)
                        }
                    }
                }
            }
            PatternStyle.HALVES -> {
                paint.style = Paint.Style.FILL
                paint.color = col1
                canvas.drawRect(0f, 0f, w * 0.26f, h * 0.72f, paint)
                canvas.drawRect(w * 0.50f, 0f, w * 0.74f, h * 0.72f, paint)
            }
            PatternStyle.PINSTRIPES -> {
                val lines = freq * 3
                val step = w / lines
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = w * 0.003f
                paint.color = col1
                for (i in 0 until lines) {
                    canvas.drawLine(i * step, 0f, i * step, h * 0.72f, paint)
                }
            }
            PatternStyle.HEX_GRID -> {
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = w * 0.0025f
                paint.color = col1
                val radius = (w / (freq * 3.5f)).coerceAtLeast(24f)
                var row = 0
                var y = 0f
                while (y < h * 0.72f) {
                    val offsetX = if (row % 2 == 1) radius * 1.5f else 0f
                    var x = offsetX
                    while (x < w) {
                        drawPolygon(canvas, x, y, radius, 6, paint)
                        x += radius * 3f
                    }
                    y += radius * 0.866f
                    row++
                }
            }
            PatternStyle.DIAMOND_ARGYLE -> {
                paint.style = Paint.Style.FILL
                paint.color = col1
                val cellW = w / freq
                val cellH = (h * 0.72f) / freq
                for (r in 0 until freq) {
                    for (c in 0 until freq) {
                        val cx = (c + 0.5f) * cellW
                        val cy = (r + 0.5f) * cellH
                        val path = Path().apply {
                            moveTo(cx, cy - cellH * 0.42f)
                            lineTo(cx + cellW * 0.42f, cy)
                            lineTo(cx, cy + cellH * 0.42f)
                            lineTo(cx - cellW * 0.42f, cy)
                            close()
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }
    }

    private fun renderSleeves(
        canvas: Canvas,
        w: Float,
        h: Float,
        primaryInt: Int,
        cuffInt: Int,
        paint: Paint
    ) {
        // Bottom UV Quadrant [y: 0.72..0.96] maps left and right sleeves
        paint.style = Paint.Style.FILL
        paint.color = primaryInt
        canvas.drawRect(w * 0.04f, h * 0.74f, w * 0.48f, h * 0.95f, paint)
        canvas.drawRect(w * 0.52f, h * 0.74f, w * 0.96f, h * 0.95f, paint)

        // Contrasting sleeve cuffs
        paint.color = cuffInt
        canvas.drawRect(w * 0.04f, h * 0.91f, w * 0.48f, h * 0.95f, paint)
        canvas.drawRect(w * 0.52f, h * 0.91f, w * 0.96f, h * 0.95f, paint)
    }

    private fun renderCollar(
        canvas: Canvas,
        layer: Layer,
        w: Float,
        h: Float,
        primaryInt: Int,
        secondaryInt: Int,
        paint: Paint
    ) {
        val frontCx = w * 0.26f
        val collarY = h * 0.09f
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.018f
        paint.color = primaryInt

        when (layer.properties.collarStyle) {
            CollarStyle.CREW_NECK -> {
                canvas.drawCircle(frontCx, collarY, w * 0.065f, paint)
            }
            CollarStyle.V_NECK -> {
                val vPath = Path().apply {
                    moveTo(frontCx - w * 0.07f, collarY - h * 0.02f)
                    lineTo(frontCx, collarY + h * 0.075f)
                    lineTo(frontCx + w * 0.07f, collarY - h * 0.02f)
                }
                canvas.drawPath(vPath, paint)
            }
            CollarStyle.POLO_RIB -> {
                paint.style = Paint.Style.FILL
                val flapL = Path().apply {
                    moveTo(frontCx - w * 0.08f, collarY - h * 0.02f)
                    lineTo(frontCx - w * 0.01f, collarY + h * 0.05f)
                    lineTo(frontCx - w * 0.06f, collarY + h * 0.07f)
                    close()
                }
                val flapR = Path().apply {
                    moveTo(frontCx + w * 0.08f, collarY - h * 0.02f)
                    lineTo(frontCx + w * 0.01f, collarY + h * 0.05f)
                    lineTo(frontCx + w * 0.06f, collarY + h * 0.07f)
                    close()
                }
                canvas.drawPath(flapL, paint)
                canvas.drawPath(flapR, paint)
            }
            CollarStyle.STAND_COLLAR -> {
                val rect = RectF(
                    frontCx - w * 0.07f,
                    collarY - h * 0.03f,
                    frontCx + w * 0.07f,
                    collarY + h * 0.04f
                )
                canvas.drawArc(rect, 0f, 180f, false, paint)
                paint.strokeWidth = w * 0.005f
                paint.color = secondaryInt
                canvas.drawArc(rect, 15f, 150f, false, paint)
            }
        }
    }

    private fun renderGeometricBadge(
        canvas: Canvas,
        layer: Layer,
        w: Float,
        h: Float,
        primaryInt: Int,
        secondaryInt: Int,
        paint: Paint
    ) {
        val decal = layer.properties.decal
        // Left chest position on front UV island
        val cx = w * 0.335f + (decal?.posX ?: 0f) * w * 0.05f
        val cy = h * 0.22f + (decal?.posY ?: 0f) * h * 0.05f
        val radius = w * 0.034f * (decal?.scale ?: 1f)

        paint.style = Paint.Style.FILL
        paint.color = primaryInt
        drawPolygon(canvas, cx, cy, radius, 6, paint)

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = w * 0.004f
        paint.color = secondaryInt
        drawPolygon(canvas, cx, cy, radius * 0.82f, 6, paint)

        // Right chest abstract geometric manufacturer emblem (3 geometric chevrons)
        val rx = w * 0.185f
        paint.style = Paint.Style.FILL
        paint.color = secondaryInt
        drawPolygon(canvas, rx, cy, radius * 0.55f, 3, paint)
    }

    private fun renderProceduralSponsor(
        canvas: Canvas,
        layer: Layer,
        w: Float,
        h: Float,
        primaryInt: Int,
        secondaryInt: Int,
        paint: Paint
    ) {
        val text = layer.properties.textValue.ifBlank { "AERO DYNAMICS" }
        val cx = w * 0.26f
        val cy = h * 0.36f

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = (layer.properties.fontSize.coerceIn(28f, 96f)) * (w / 2048f) * 1.4f

        // Geometric framing bar above sponsor
        paint.style = Paint.Style.FILL
        paint.color = secondaryInt
        canvas.drawRoundRect(
            cx - w * 0.08f,
            cy - h * 0.052f,
            cx + w * 0.08f,
            cy - h * 0.042f,
            8f,
            8f,
            paint
        )

        paint.color = primaryInt
        canvas.drawText(text.uppercase(), cx, cy, paint)
    }

    /**
     * FUN-COMPOSITE-2D: Testo giocatore curvato su arco sul dorso della maglia (UV back island).
     */
    private fun renderCurvedPlayerName(
        canvas: Canvas,
        layer: Layer,
        w: Float,
        h: Float,
        primaryInt: Int,
        secondaryInt: Int,
        paint: Paint
    ) {
        val name = layer.properties.textValue.ifBlank { "ROSSI" }.uppercase()
        val backCx = w * 0.74f
        val backCy = h * 0.21f
        val arcDeg = layer.properties.textArcDegrees.coerceIn(-60f, 60f)

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        paint.textSize = (layer.properties.fontSize.coerceIn(32f, 110f)) * (w / 2048f) * 1.35f

        if (kotlin.math.abs(arcDeg) > 1f) {
            val radius = w * 0.24f
            val arcRect = RectF(
                backCx - radius,
                backCy,
                backCx + radius,
                backCy + radius * 2f
            )
            val path = Path().apply {
                addArc(arcRect, 270f - arcDeg, arcDeg * 2f)
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = w * 0.003f
            paint.color = secondaryInt
            canvas.drawTextOnPath(name, path, 0f, 0f, paint)

            paint.style = Paint.Style.FILL
            paint.color = primaryInt
            canvas.drawTextOnPath(name, path, 0f, 0f, paint)
        } else {
            paint.style = Paint.Style.FILL
            paint.color = primaryInt
            canvas.drawText(name, backCx, backCy, paint)
        }
    }

    /**
     * FUN-COMPOSITE-2D: Numero di gara bicolore con bordo esterno + inline geometrico.
     */
    private fun renderBicolorNumber(
        canvas: Canvas,
        layer: Layer,
        w: Float,
        h: Float,
        primaryInt: Int,
        secondaryInt: Int,
        paint: Paint
    ) {
        val numberStr = layer.properties.textValue.ifBlank { "10" }.take(2)
        val backCx = w * 0.74f
        val backCy = h * 0.46f
        val scale = w / 2048f

        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textSize = layer.properties.fontSize.coerceIn(120f, 340f) * scale * 1.55f

        // Outer secondary bicolor stroke
        if (layer.properties.strokeWidth > 0f) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = layer.properties.strokeWidth * scale * 3.2f
            paint.color = secondaryInt
            canvas.drawText(numberStr, backCx, backCy, paint)
        }

        // Primary body fill
        paint.style = Paint.Style.FILL
        paint.color = primaryInt
        canvas.drawText(numberStr, backCx, backCy, paint)

        // Optional front chest/belly small number
        if (layer.properties.frontNumberVisible) {
            val frontCx = w * 0.185f
            val frontCy = h * 0.29f
            paint.textSize = 72f * scale
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 6f * scale
            paint.color = secondaryInt
            canvas.drawText(numberStr, frontCx, frontCy, paint)

            paint.style = Paint.Style.FILL
            paint.color = primaryInt
            canvas.drawText(numberStr, frontCx, frontCy, paint)
        }
    }

    private fun drawPolygon(
        canvas: Canvas,
        cx: Float,
        cy: Float,
        radius: Float,
        sides: Int,
        paint: Paint
    ) {
        val path = Path()
        for (i in 0 until sides) {
            val rad = Math.toRadians((i * (360.0 / sides) - 90.0))
            val x = cx + (radius * cos(rad)).toFloat()
            val y = cy + (radius * sin(rad)).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        canvas.drawPath(path, paint)
    }

    private fun drawUvSeamGuides(canvas: Canvas, w: Float, h: Float) {
        val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(28, 255, 255, 255)
        }
        // Subtle technical stitching lines separating front torso, back torso, and sleeves
        canvas.drawLine(w * 0.50f, 0f, w * 0.50f, h * 0.72f, seamPaint)
        canvas.drawLine(0f, h * 0.72f, w, h * 0.72f, seamPaint)
    }

    fun parseHexColor(hex: String, fallback: Int): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(fallback)
}
