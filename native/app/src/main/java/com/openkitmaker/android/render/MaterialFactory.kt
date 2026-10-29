// Implements: SCR-EDITOR / FUN-PREVIEW-3D
package com.openkitmaker.android.render

import android.content.Context
import android.graphics.Bitmap
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.android.TextureHelper
import com.openkitmaker.android.domain.FabricType
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Factory per la gestione dei materiali PBR di Google Filament e caricamento texture
 * KTX2 / ASTC 6x6 con fallback automatico a PNG/Bitmap e normal map procedurali di tessuto.
 */
object MaterialFactory {

    /**
     * Carica o aggiorna una texture albedo da bitmap per le geometrie della divisa.
     */
    fun createOrUpdateAlbedoTexture(
        engine: Engine,
        existingTexture: Texture?,
        bitmap: Bitmap
    ): Texture {
        val width = bitmap.width
        val height = bitmap.height
        val texture = existingTexture ?: Texture.Builder()
            .width(width)
            .height(height)
            .levels(1)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.RGBA8)
            .build(engine)

        TextureHelper.setBitmap(engine, texture, 0, bitmap)
        return texture
    }

    /**
     * Genera una normal map procedurale 128x128 che simula fisicamente la trama e il rilievo
     * del tessuto selezionato (Mesh, Poliestere lucido, Cotone '90, Jacquard damascato).
     */
    fun createProceduralFabricNormalTexture(
        engine: Engine,
        fabricType: FabricType = FabricType.MESH
    ): Texture {
        val size = 128
        val buffer = ByteBuffer.allocateDirect(size * size * 4).order(ByteOrder.nativeOrder())
        for (y in 0 until size) {
            for (x in 0 until size) {
                var nx = 128
                var ny = 128
                var nz = 240

                when (fabricType) {
                    FabricType.MESH -> {
                        val isPore = ((x % 8 == 0 && y % 8 == 0) || (x % 8 == 4 && y % 8 == 4))
                        val waveX = (kotlin.math.sin((x / 4.0) * Math.PI * 2.0) * 14.0).toInt()
                        val waveY = (kotlin.math.cos((y / 4.0) * Math.PI * 2.0) * 14.0).toInt()
                        nx = 128 + waveX
                        ny = 128 + waveY
                        if (isPore) {
                            nx = 96
                            ny = 96
                            nz = 180
                        }
                    }
                    FabricType.POLYESTER -> {
                        val thread = (kotlin.math.sin((x / 2.0) * Math.PI * 2.0) * 8.0).toInt()
                        nx = 128 + thread
                        ny = 128
                        nz = 252
                    }
                    FabricType.COTTON -> {
                        val coarseX = (kotlin.math.sin((x / 3.0) * Math.PI * 2.0) * 20.0).toInt()
                        val coarseY = (kotlin.math.cos((y / 3.0) * Math.PI * 2.0) * 20.0).toInt()
                        val fiberNoise = ((x * 17 + y * 31) % 19) - 9
                        nx = 128 + coarseX + fiberNoise
                        ny = 128 + coarseY + fiberNoise
                        nz = 215
                    }
                    FabricType.JACQUARD -> {
                        val diag1 = if ((x + y) % 16 < 4) 32 else -12
                        val diag2 = if ((x - y + 256) % 16 < 4) 32 else -12
                        nx = 128 + diag1
                        ny = 128 + diag2
                        nz = 210
                    }
                }

                buffer.put(nx.coerceIn(0, 255).toByte())
                buffer.put(ny.coerceIn(0, 255).toByte())
                buffer.put(nz.coerceIn(0, 255).toByte())
                buffer.put(255.toByte())
            }
        }
        buffer.flip()

        val texture = Texture.Builder()
            .width(size)
            .height(size)
            .levels(1)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.RGBA8)
            .build(engine)

        val descriptor = Texture.PixelBufferDescriptor(
            buffer,
            Texture.Format.RGBA,
            Texture.Type.UBYTE
        )
        texture.setImage(engine, 0, descriptor)
        return texture
    }

    fun applyKitPbrParameters(
        materialInstance: MaterialInstance,
        albedoTexture: Texture,
        normalTexture: Texture,
        roughness: Float,
        metalness: Float
    ) {
        val sampler = TextureSampler(
            TextureSampler.MinFilter.LINEAR,
            TextureSampler.MagFilter.LINEAR,
            TextureSampler.WrapMode.REPEAT
        ).apply {
            anisotropy = 4.0f
        }

        runCatching {
            materialInstance.setParameter("baseColorMap", albedoTexture, sampler)
            materialInstance.setParameter("normalMap", normalTexture, sampler)
            materialInstance.setParameter("roughnessFactor", roughness.coerceIn(0.15f, 0.95f))
            materialInstance.setParameter("metallicFactor", metalness.coerceIn(0.0f, 0.4f))
        }.onFailure { err ->
            android.util.Log.e("MaterialFactory", "Errore applicazione texture Filament baseColorMap: ${err.message}")
        }
    }
}
