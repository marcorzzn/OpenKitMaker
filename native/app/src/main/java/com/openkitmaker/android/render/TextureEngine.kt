// Implements: SCR-EDITOR / SCR-EXPORT / FUN-COMPOSITE-2D / FUN-EDIT-COLOR / FUN-MANAGE-LAYERS
package com.openkitmaker.android.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.BlurMaskFilter
import android.graphics.RadialGradient
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import com.openkitmaker.android.domain.BlendMode
import com.openkitmaker.android.domain.CollarStyle
import com.openkitmaker.android.domain.GarmentType
import com.openkitmaker.android.domain.Kit
import com.openkitmaker.android.domain.Layer
import com.openkitmaker.android.domain.LayerType
import com.openkitmaker.android.domain.PatternStyle
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

    fun compositeJerseyBitmap(
        kit: Kit,
        width: Int = FULL_RESOLUTION,
        height: Int = FULL_RESOLUTION
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val layers = kit.garments.jersey.layers.ifEmpty { kit.layers }
        val orderedLayers = layers
            .filter { it.visible && it.opacity > 0.01f }
            .sortedBy { it.zIndex }

        for (layer in orderedLayers) {
            renderLayer(canvas, layer, width.toFloat(), height.toFloat())
        }

        drawUvSeamGuides(canvas, width.toFloat(), height.toFloat())
        return bitmap
    }

    fun compositeShortsBitmap(
        kit: Kit,
        width: Int = FULL_RESOLUTION,
        height: Int = FULL_RESOLUTION
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val layers = kit.garments.shorts.layers
        val orderedLayers = layers
            .filter { it.visible && it.opacity > 0.01f }
            .sortedBy { it.zIndex }

        for (layer in orderedLayers) {
            renderLayer(canvas, layer, width.toFloat(), height.toFloat())
        }

        drawShortsUvSeamGuides(canvas, width.toFloat(), height.toFloat())
        return bitmap
    }

    fun compositeSocksBitmap(
        kit: Kit,
        width: Int = FULL_RESOLUTION,
        height: Int = FULL_RESOLUTION
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

        val layers = kit.garments.socks.layers
        val orderedLayers = layers
            .filter { it.visible && it.opacity > 0.01f }
            .sortedBy { it.zIndex }

        for (layer in orderedLayers) {
            renderLayer(canvas, layer, width.toFloat(), height.toFloat())
        }

        drawSocksUvSeamGuides(canvas, width.toFloat(), height.toFloat())
        return bitmap
    }

    fun compositeGarmentBitmap(
        kit: Kit,
        garmentType: GarmentType,
        width: Int = FULL_RESOLUTION,
        height: Int = FULL_RESOLUTION
    ): Bitmap = when (garmentType) {
        GarmentType.JERSEY -> compositeJerseyBitmap(kit, width, height)
        GarmentType.SHORTS -> compositeShortsBitmap(kit, width, height)
        GarmentType.SOCKS -> compositeSocksBitmap(kit, width, height)
    }

    /**
     * FUN-COMPOSITE-2D / FIX 3: Rende il Mockup 2D fotorealistico della maglia (sagoma fronte/retro con maniche)
     * campionando dalla texture 2048x2048 e applicando ombra realistica e volume 3D sartoriale.
     */
    fun renderJerseyMockupBitmap(
        kit: Kit,
        fullTexture: Bitmap,
        width: Int = 1024,
        height: Int = 1024
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // 1. Studio Dark Backdrop Radial Gradient
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f, height * 0.45f, width * 0.65f,
                Color.parseColor("#1E293B"),
                Color.parseColor("#0F141C"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        // 2. Griglia architettonica dello studio
        val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#2E3D56")
            alpha = (0.35f * 255f).toInt()
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }
        val step = width / 16f
        var gx = step
        while (gx < width) {
            canvas.drawLine(gx, 0f, gx, height.toFloat(), gridPaint)
            gx += step
        }
        var gy = step
        while (gy < height) {
            canvas.drawLine(0f, gy, width.toFloat(), gy, gridPaint)
            gy += step
        }

        fun drawJerseySilhouette(cx: Float, cy: Float, scale: Float, isBack: Boolean) {
            canvas.save()
            canvas.translate(cx, cy)
            canvas.scale(scale, scale)

            // Sagoma sartoriale anatomica della maglia
            val path = Path().apply {
                moveTo(-58f, -148f)
                if (isBack) {
                    quadTo(0f, -134f, 58f, -148f)
                } else {
                    lineTo(0f, -108f)
                    lineTo(58f, -148f)
                }
                lineTo(106f, -136f)
                lineTo(154f, -52f)
                lineTo(116f, -24f)
                lineTo(92f, -66f)
                lineTo(92f, 148f)
                lineTo(-92f, 148f)
                lineTo(-92f, -66f)
                lineTo(-116f, -24f)
                lineTo(-154f, -52f)
                lineTo(-106f, -136f)
                close()
            }

            // Ombra realistica morbida (Drop Shadow multi-pass)
            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                alpha = (0.58f * 255f).toInt()
                maskFilter = runCatching { BlurMaskFilter(22f, BlurMaskFilter.Blur.NORMAL) }.getOrNull()
            }
            canvas.save()
            canvas.translate(0f, 16f)
            canvas.drawPath(path, shadowPaint)
            canvas.restore()

            // Maschera di ritaglio sulla sagoma della maglia
            canvas.save()
            canvas.clipPath(path)

            // Campionamento del busto dalla texture 2048x2048
            val srcRect = if (isBack) {
                Rect(1024, 0, 2048, 1360)
            } else {
                Rect(0, 0, 1024, 1360)
            }
            val dstRect = RectF(-105f, -152f, 105f, 152f)
            canvas.drawBitmap(fullTexture, srcRect, dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

            // Manica Sinistra
            canvas.save()
            canvas.translate(-120f, -80f)
            canvas.rotate(30f)
            val sleeveLeftSrc = Rect(0, 1360, 680, 2048)
            val sleeveLeftDst = RectF(-36f, -56f, 38f, 70f)
            canvas.drawBitmap(fullTexture, sleeveLeftSrc, sleeveLeftDst, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()

            // Manica Destra
            canvas.save()
            canvas.translate(120f, -80f)
            canvas.rotate(-30f)
            val sleeveRightSrc = Rect(1024, 1360, 1704, 2048)
            val sleeveRightDst = RectF(-38f, -56f, 36f, 70f)
            canvas.drawBitmap(fullTexture, sleeveRightSrc, sleeveRightDst, Paint(Paint.FILTER_BITMAP_FLAG))
            canvas.restore()

            // Sfumatura volumetrica 3D sul tessuto (luce e pieghe realistiche)
            val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    -140f, 0f, 140f, 0f,
                    intArrayOf(
                        Color.argb((0.30f * 255f).toInt(), 0, 0, 0),
                        Color.argb((0.08f * 255f).toInt(), 255, 255, 255),
                        Color.TRANSPARENT,
                        Color.argb((0.08f * 255f).toInt(), 255, 255, 255),
                        Color.argb((0.34f * 255f).toInt(), 0, 0, 0)
                    ),
                    floatArrayOf(0f, 0.2f, 0.5f, 0.8f, 1.0f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(-160f, -160f, 160f, 160f, shadePaint)

            canvas.restore() // Fine maschera clip

            // Contorno sartoriale netto
            val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = (0.28f * 255f).toInt()
                strokeWidth = 2.5f
                style = Paint.Style.STROKE
            }
            canvas.drawPath(path, outlinePaint)

            canvas.restore()
        }

        // Sagoma Retro a destra e Fronte in primo piano a sinistra
        val s = width / 512f
        drawJerseySilhouette(330f * s, 258f * s, 0.92f * s, isBack = true)
        drawJerseySilhouette(188f * s, 244f * s, 1.02f * s, isBack = false)

        // Banner informativo inferiore
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F141C")
            alpha = (0.90f * 255f).toInt()
        }
        canvas.drawRect(0f, height - 44f * s, width.toFloat(), height.toFloat(), footerPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText(kit.name.uppercase().take(28), 18f * s, height - 16f * s, textPaint)

        val metaPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#00C49F")
            textSize = 13f * s
            textAlign = Paint.Align.RIGHT
            typeface = Typeface.MONOSPACE
        }
        canvas.drawText("${kit.category} · Mockup 2D", width - 18f * s, height - 16f * s, metaPaint)

        return bitmap
    }

    /**
     * Rende il Mockup 2D fotorealistico dei pantaloncini (sagoma fronte/retro).
     */
    fun renderShortsMockupBitmap(
        kit: Kit,
        fullTexture: Bitmap,
        width: Int = 1024,
        height: Int = 1024
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Backdrop
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f, height * 0.45f, width * 0.65f,
                Color.parseColor("#1E293B"),
                Color.parseColor("#0F141C"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val s = width / 512f

        // Sagoma Pantaloncini
        val path = Path().apply {
            moveTo(-100f * s, -90f * s)
            lineTo(100f * s, -90f * s)
            lineTo(110f * s, 85f * s)
            lineTo(20f * s, 85f * s)
            lineTo(0f * s, -15f * s)
            lineTo(-20f * s, 85f * s)
            lineTo(-110f * s, 85f * s)
            close()
        }

        // Drop shadow
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            alpha = (0.50f * 255f).toInt()
            maskFilter = runCatching { BlurMaskFilter(20f * s, BlurMaskFilter.Blur.NORMAL) }.getOrNull()
        }

        canvas.save()
        canvas.translate(width / 2f, height * 0.45f)
        canvas.drawPath(path, shadowPaint)

        // Texture clipping
        canvas.save()
        canvas.clipPath(path)
        val dstRect = RectF(-115f * s, -95f * s, 115f * s, 95f * s)
        canvas.drawBitmap(fullTexture, Rect(0, 0, fullTexture.width, fullTexture.height), dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

        // Shading
        val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = LinearGradient(
                -110f * s, 0f, 110f * s, 0f,
                intArrayOf(
                    Color.argb((0.25f * 255f).toInt(), 0, 0, 0),
                    Color.TRANSPARENT,
                    Color.argb((0.28f * 255f).toInt(), 0, 0, 0)
                ),
                floatArrayOf(0f, 0.5f, 1f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(dstRect, shadePaint)
        canvas.restore()

        // Outline
        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            alpha = (0.28f * 255f).toInt()
            strokeWidth = 2f * s
            style = Paint.Style.STROKE
        }
        canvas.drawPath(path, outlinePaint)
        canvas.restore()

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F141C")
            alpha = (0.90f * 255f).toInt()
        }
        canvas.drawRect(0f, height - 44f * s, width.toFloat(), height.toFloat(), footerPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("${kit.name.uppercase().take(24)} · SHORTS", 18f * s, height - 16f * s, textPaint)

        return bitmap
    }

    /**
     * Rende il Mockup 2D fotorealistico dei calzettoni.
     */
    fun renderSocksMockupBitmap(
        kit: Kit,
        fullTexture: Bitmap,
        width: Int = 1024,
        height: Int = 1024
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Backdrop
        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = RadialGradient(
                width / 2f, height * 0.45f, width * 0.65f,
                Color.parseColor("#1E293B"),
                Color.parseColor("#0F141C"),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

        val s = width / 512f

        fun drawSock(cx: Float, cy: Float, flip: Boolean) {
            canvas.save()
            canvas.translate(cx, cy)
            if (flip) canvas.scale(-1f, 1f)

            val path = Path().apply {
                moveTo(-28f * s, -140f * s)
                lineTo(28f * s, -140f * s)
                lineTo(28f * s, 60f * s)
                lineTo(80f * s, 110f * s)
                lineTo(55f * s, 135f * s)
                lineTo(-15f * s, 85f * s)
                lineTo(-28f * s, 60f * s)
                close()
            }

            val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.BLACK
                alpha = (0.45f * 255f).toInt()
                maskFilter = runCatching { BlurMaskFilter(16f * s, BlurMaskFilter.Blur.NORMAL) }.getOrNull()
            }
            canvas.drawPath(path, shadowPaint)

            canvas.save()
            canvas.clipPath(path)
            val dstRect = RectF(-35f * s, -145f * s, 85f * s, 140f * s)
            canvas.drawBitmap(fullTexture, Rect(0, 0, fullTexture.width, fullTexture.height), dstRect, Paint(Paint.FILTER_BITMAP_FLAG))

            val shadePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    -30f * s, 0f, 30f * s, 0f,
                    intArrayOf(
                        Color.argb((0.28f * 255f).toInt(), 0, 0, 0),
                        Color.TRANSPARENT,
                        Color.argb((0.25f * 255f).toInt(), 0, 0, 0)
                    ),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(dstRect, shadePaint)
            canvas.restore()

            val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = (0.28f * 255f).toInt()
                strokeWidth = 2f * s
                style = Paint.Style.STROKE
            }
            canvas.drawPath(path, outlinePaint)
            canvas.restore()
        }

        drawSock(width * 0.38f, height * 0.45f, flip = false)
        drawSock(width * 0.62f, height * 0.45f, flip = true)

        // Footer
        val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F141C")
            alpha = (0.90f * 255f).toInt()
        }
        canvas.drawRect(0f, height - 44f * s, width.toFloat(), height.toFloat(), footerPaint)

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 17f * s
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        canvas.drawText("${kit.name.uppercase().take(24)} · SOCKS", 18f * s, height - 16f * s, textPaint)

        return bitmap
    }

    /**
     * Rende la composizione fotorealistica del Full Kit (Maglia + Pantaloncini + Calzettoni).
     */
    fun renderFullKitMockupBitmap(
        kit: Kit,
        width: Int = 1024,
        height: Int = 1024
    ): Bitmap {
        val jerseyTex = compositeJerseyBitmap(kit, 1024, 1024)
        val shortsTex = compositeShortsBitmap(kit, 1024, 1024)
        val socksTex = compositeSocksBitmap(kit, 1024, 1024)

        val jerseyMockup = renderJerseyMockupBitmap(kit, jerseyTex, width, height)
        val shortsMockup = renderShortsMockupBitmap(kit, shortsTex, width / 2, height / 2)
        val socksMockup = renderSocksMockupBitmap(kit, socksTex, width / 2, height / 2)

        jerseyTex.recycle()
        shortsTex.recycle()
        socksTex.recycle()

        val fullBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(fullBmp)
        val filterPaint = Paint(Paint.FILTER_BITMAP_FLAG)

        // Draw jersey
        canvas.drawBitmap(jerseyMockup, 0f, 0f, filterPaint)
        jerseyMockup.recycle()

        // Draw shorts inset on bottom-left
        val insetPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F141C")
            style = Paint.Style.STROKE
            strokeWidth = 3f
        }
        val shortsDst = RectF(width * 0.03f, height * 0.58f, width * 0.38f, height * 0.93f)
        canvas.drawBitmap(shortsMockup, null, shortsDst, filterPaint)
        canvas.drawRect(shortsDst, insetPaint)
        shortsMockup.recycle()

        // Draw socks inset on bottom-right
        val socksDst = RectF(width * 0.62f, height * 0.58f, width * 0.97f, height * 0.93f)
        canvas.drawBitmap(socksMockup, null, socksDst, filterPaint)
        canvas.drawRect(socksDst, insetPaint)
        socksMockup.recycle()

        return fullBmp
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
        val yTop = h * (1360f / 2048f) // 0.664 h
        val sleeveLeftW = w * (680f / 2048f) // 0.332 w
        val sleeveRightX = w * 0.50f
        val sleeveRightW = w * (680f / 2048f)
        val cuffH = h * 0.045f // ~92px on 2048x2048

        // 1. Sleeve Base Fills (Left: [0..0.332w, 0.664h..h], Right: [0.50..0.832w, 0.664h..h])
        paint.style = Paint.Style.FILL
        paint.color = primaryInt
        canvas.drawRect(0f, yTop, sleeveLeftW, h, paint)
        canvas.drawRect(sleeveRightX, yTop, sleeveRightX + sleeveRightW, h, paint)

        // 2. Contrasting Sleeve Cuffs & Accent Stripe (Secondary Color)
        paint.color = cuffInt
        canvas.drawRect(0f, h - cuffH, sleeveLeftW, h, paint)
        canvas.drawRect(sleeveRightX, h - cuffH, sleeveRightX + sleeveRightW, h, paint)

        // Subtle accent pinstripe above cuffs
        val stripeH = (h * 0.006f).coerceAtLeast(4f)
        val stripeGap = (h * 0.010f).coerceAtLeast(8f)
        canvas.drawRect(0f, h - cuffH - stripeGap - stripeH, sleeveLeftW, h - cuffH - stripeGap, paint)
        canvas.drawRect(sleeveRightX, h - cuffH - stripeGap - stripeH, sleeveRightX + sleeveRightW, h - cuffH - stripeGap, paint)

        // 3. Raglan Shoulder Wedges on Front & Back Torso Panels
        val shoulderW = w * (250f / 2048f)
        val shoulderH = h * (320f / 2048f)
        paint.color = primaryInt

        // Front Panel: Left & Right shoulder cuts
        val frontLeftShoulder = Path().apply {
            moveTo(0f, 0f)
            lineTo(shoulderW, 0f)
            lineTo(0f, shoulderH)
            close()
        }
        canvas.drawPath(frontLeftShoulder, paint)

        val frontRightShoulder = Path().apply {
            moveTo(w * 0.50f, 0f)
            lineTo(w * 0.50f - shoulderW, 0f)
            lineTo(w * 0.50f, shoulderH)
            close()
        }
        canvas.drawPath(frontRightShoulder, paint)

        // Back Panel: Left & Right shoulder cuts
        val backLeftShoulder = Path().apply {
            moveTo(w * 0.50f, 0f)
            lineTo(w * 0.50f + shoulderW, 0f)
            lineTo(w * 0.50f, shoulderH)
            close()
        }
        canvas.drawPath(backLeftShoulder, paint)

        val backRightShoulder = Path().apply {
            moveTo(w, 0f)
            lineTo(w - shoulderW, 0f)
            lineTo(w, shoulderH)
            close()
        }
        canvas.drawPath(backRightShoulder, paint)

        // Raglan seam accent stroke in secondary color
        val seamStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = (w * 0.005f).coerceAtLeast(3f)
            color = cuffInt
        }
        canvas.drawLine(shoulderW, 0f, 0f, shoulderH, seamStrokePaint)
        canvas.drawLine(w * 0.50f - shoulderW, 0f, w * 0.50f, shoulderH, seamStrokePaint)
        canvas.drawLine(w * 0.50f + shoulderW, 0f, w * 0.50f, shoulderH, seamStrokePaint)
        canvas.drawLine(w - shoulderW, 0f, w, shoulderH, seamStrokePaint)
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
            color = Color.argb(40, 255, 255, 255)
        }
        val yTop = h * (1360f / 2048f)
        val sleeveLeftW = w * (680f / 2048f)
        val collarW = w * (344f / 2048f)
        val sleeveRightX = w * 0.50f
        val sleeveRightW = w * (680f / 2048f)

        // Horizontal division between Torso panels and Lower components (Sleeves, Collar, Trim)
        canvas.drawLine(0f, yTop, w, yTop, seamPaint)
        // Vertical division between Front and Back Torso
        canvas.drawLine(w * 0.50f, 0f, w * 0.50f, yTop, seamPaint)

        // Lower zone divisions
        canvas.drawLine(sleeveLeftW, yTop, sleeveLeftW, h, seamPaint)
        canvas.drawLine(sleeveLeftW + collarW, yTop, sleeveLeftW + collarW, h, seamPaint)
        canvas.drawLine(sleeveRightX + sleeveRightW, yTop, sleeveRightX + sleeveRightW, h, seamPaint)

        // Sleeve cuff guidelines
        val cuffH = h * 0.045f
        canvas.drawLine(0f, h - cuffH, sleeveLeftW, h - cuffH, seamPaint)
        canvas.drawLine(sleeveRightX, h - cuffH, sleeveRightX + sleeveRightW, h - cuffH, seamPaint)
    }

    private fun drawShortsUvSeamGuides(canvas: Canvas, w: Float, h: Float) {
        val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(40, 255, 255, 255)
        }
        // Vertical dividing seam
        canvas.drawLine(w * 0.5f, 0f, w * 0.5f, h, seamPaint)
        // Horizontal dividing seam (front / back)
        canvas.drawLine(0f, h * 0.5f, w, h * 0.5f, seamPaint)
    }

    private fun drawSocksUvSeamGuides(canvas: Canvas, w: Float, h: Float) {
        val seamPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = Color.argb(40, 255, 255, 255)
        }
        // Vertical divider between left and right sock
        canvas.drawLine(w * 0.5f, 0f, w * 0.5f, h, seamPaint)
        // Top cuff turnover line
        canvas.drawLine(0f, h * 0.22f, w, h * 0.22f, seamPaint)
        // Ankle turnover line
        canvas.drawLine(0f, h * 0.65f, w, h * 0.65f, seamPaint)
    }

    fun parseHexColor(hex: String, fallback: Int): Int =
        runCatching { Color.parseColor(hex) }.getOrDefault(fallback)
}
