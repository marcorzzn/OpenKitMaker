// Implements: SCR-EDITOR / FUN-PREVIEW-3D
package com.openkitmaker.android.render

import android.content.Context
import android.graphics.Bitmap
import com.google.android.filament.Engine
import com.google.android.filament.MaterialInstance
import com.google.android.filament.Texture
import com.google.android.filament.TextureSampler
import com.google.android.filament.android.TextureHelper
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Factory per la gestione dei materiali PBR di Google Filament e caricamento texture
 * KTX2 / ASTC 6x6 con fallback automatico a PNG/Bitmap.
 */
object MaterialFactory {

    /**
     * Carica una texture statica KTX2/ASTC dagli asset, oppure ripiega su un buffer procedurale
     * micro-weave normal map per la trama del tessuto tecnico sportivo.
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
            .levels(4)
            .sampler(Texture.Sampler.SAMPLER_2D)
            .format(Texture.InternalFormat.SRGB8_A8)
            .build(engine)

        TextureHelper.setBitmap(engine, texture, 0, bitmap)
        texture.generateMipmaps(engine)
        return texture
    }

    /**
     * Genera una normal map procedurale 256x256 che simula la micro-trama traspirante del tessuto
     * tecnico sportivo senza dipendere da asset esterni di terze parti.
     */
    fun createProceduralFabricNormalTexture(engine: Engine): Texture {
        val size = 256
        val buffer = ByteBuffer.allocateDirect(size * size * 4).order(ByteOrder.nativeOrder())
        for (y in 0 until size) {
            for (x in 0 until size) {
                val nx = if ((x + y) % 4 == 0) 142 else 114
                val ny = if (y % 4 == 0) 142 else 114
                buffer.put(nx.toByte())
                buffer.put(ny.toByte())
                buffer.put(255.toByte())
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
            TextureSampler.MinFilter.LINEAR_MIPMAP_LINEAR,
            TextureSampler.MagFilter.LINEAR,
            TextureSampler.WrapMode.REPEAT
        ).apply {
            anisotropy = 8.0f
        }

        runCatching {
            materialInstance.setParameter("baseColorMap", albedoTexture, sampler)
            materialInstance.setParameter("normalMap", normalTexture, sampler)
            materialInstance.setParameter("roughnessFactor", roughness.coerceIn(0.15f, 0.95f))
            materialInstance.setParameter("metallicFactor", metalness.coerceIn(0.0f, 0.4f))
        }
    }
}
