// Implements: SCR-EXPORT / FUN-EXPORT-ZIP, STEP 4
package com.openkitmaker.android.export

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.openkitmaker.android.data.repository.KitRepository
import com.openkitmaker.android.domain.ExportChecksums
import com.openkitmaker.android.domain.ExportDimensions
import com.openkitmaker.android.domain.ExportManifest
import com.openkitmaker.android.domain.FabricType
import com.openkitmaker.android.domain.GeneratorInfo
import com.openkitmaker.android.domain.ManifestKitData
import com.openkitmaker.android.render.TextureEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.MessageDigest
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * WorkManager CoroutineWorker per l'esportazione di:
 * 1. Archivio interoperabile `kit_<id>.zip` (Full Kit + Manifest JSON)
 * 2. Bundle Materiali PBR `pbr_bundle_<id>.zip` (Albedo, Normal, Roughness, Metallic)
 * 3. Modello 3D Standalone `kit_<id>.glb` con geometrie clean-room e texture coordinate
 */
@HiltWorker
class ZipExporter @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val kitRepository: KitRepository,
    private val json: Json
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val kitId = inputData.getString(PngExporter.KEY_KIT_ID) ?: return@withContext Result.failure()
        val kit = kitRepository.getKitById(kitId) ?: return@withContext Result.failure()
        val exportMode = inputData.getString(KEY_EXPORT_MODE) ?: MODE_INTEROPERABLE_ZIP

        try {
            when (exportMode) {
                MODE_3D_GLB -> {
                    val fileName = "kit_${kit.id}.glb"
                    val savedUri = writeGlbToMediaStore(
                        context = appContext,
                        fileName = fileName,
                        assetPath = "models/kit_full_outfit_clean.glb"
                    )
                    Result.success(
                        workDataOf(
                            PngExporter.KEY_OUTPUT_URI to savedUri,
                            PngExporter.KEY_FILE_NAME to fileName
                        )
                    )
                }
                MODE_PBR_BUNDLE -> {
                    val fileName = "pbr_bundle_${kit.id}.zip"
                    val jerseyBmp = TextureEngine.compositeJerseyBitmap(kit, TextureEngine.FULL_RESOLUTION, TextureEngine.FULL_RESOLUTION)
                    val shortsBmp = TextureEngine.compositeShortsBitmap(kit, TextureEngine.FULL_RESOLUTION, TextureEngine.FULL_RESOLUTION)
                    val socksBmp = TextureEngine.compositeSocksBitmap(kit, TextureEngine.FULL_RESOLUTION, TextureEngine.FULL_RESOLUTION)
                    val previewBmp = TextureEngine.renderFullKitMockupBitmap(kit, TextureEngine.PREVIEW_RESOLUTION, TextureEngine.PREVIEW_RESOLUTION)

                    val fabric = kit.metadata.fabricType
                    val normalBmp = generateProceduralNormalBitmap(fabric, 1024)
                    val roughnessBmp = generateMonochromeBitmap(fabric.defaultRoughness, 1024)
                    val metallicBmp = generateMonochromeBitmap(fabric.defaultMetalness, 1024)

                    val jerseyBytes = bitmapToPngBytes(jerseyBmp)
                    val shortsBytes = bitmapToPngBytes(shortsBmp)
                    val socksBytes = bitmapToPngBytes(socksBmp)
                    val previewBytes = bitmapToPngBytes(previewBmp)
                    val normalBytes = bitmapToPngBytes(normalBmp)
                    val roughnessBytes = bitmapToPngBytes(roughnessBmp)
                    val metallicBytes = bitmapToPngBytes(metallicBmp)

                    jerseyBmp.recycle()
                    shortsBmp.recycle()
                    socksBmp.recycle()
                    previewBmp.recycle()
                    normalBmp.recycle()
                    roughnessBmp.recycle()
                    metallicBmp.recycle()

                    val pbrManifest = """
                        {
                          "schemaVersion": "1.0.0",
                          "kitId": "${kit.id}",
                          "kitName": "${kit.name}",
                          "category": "${kit.category}",
                          "fabricType": "${fabric.name}",
                          "fabricDescription": "${fabric.labelIt}",
                          "pbrParameters": {
                            "roughnessFactor": ${fabric.defaultRoughness},
                            "metallicFactor": ${fabric.defaultMetalness},
                            "normalMapFormat": "Tangent Space OpenGL (+Y)"
                          },
                          "textureMaps": {
                            "albedo": "albedo.png (Primary Jersey)",
                            "albedoJersey": "albedo_jersey.png (2048x2048)",
                            "albedoShorts": "albedo_shorts.png (2048x2048)",
                            "albedoSocks": "albedo_socks.png (2048x2048)",
                            "normal": "normal.png (1024x1024 Micro-Weave)",
                            "roughness": "roughness.png (1024x1024 Greyscale)",
                            "metallic": "metallic.png (1024x1024 Greyscale)",
                            "preview": "preview.png (512x512 Mockup)"
                          },
                          "compatibleEngines": ["Blender 4+", "Unity URP/HDRP", "Unreal Engine 5", "Godot 4"],
                          "exportedAt": "${Instant.now()}"
                        }
                    """.trimIndent().encodeToByteArray()

                    val savedUri = writePbrZipToMediaStore(
                        context = appContext,
                        fileName = fileName,
                        jerseyBytes = jerseyBytes,
                        shortsBytes = shortsBytes,
                        socksBytes = socksBytes,
                        previewBytes = previewBytes,
                        normalBytes = normalBytes,
                        roughnessBytes = roughnessBytes,
                        metallicBytes = metallicBytes,
                        pbrManifestBytes = pbrManifest
                    )

                    Result.success(
                        workDataOf(
                            PngExporter.KEY_OUTPUT_URI to savedUri,
                            PngExporter.KEY_FILE_NAME to fileName
                        )
                    )
                }
                else -> {
                    // Standard Interoperable Full Kit ZIP
                    val jerseyBmp = TextureEngine.compositeJerseyBitmap(kit, TextureEngine.FULL_RESOLUTION, TextureEngine.FULL_RESOLUTION)
                    val shortsBmp = TextureEngine.compositeShortsBitmap(kit, TextureEngine.FULL_RESOLUTION, TextureEngine.FULL_RESOLUTION)
                    val socksBmp = TextureEngine.compositeSocksBitmap(kit, TextureEngine.FULL_RESOLUTION, TextureEngine.FULL_RESOLUTION)
                    val previewBmp = TextureEngine.renderFullKitMockupBitmap(kit, TextureEngine.PREVIEW_RESOLUTION, TextureEngine.PREVIEW_RESOLUTION)

                    val jerseyBytes = bitmapToPngBytes(jerseyBmp)
                    val shortsBytes = bitmapToPngBytes(shortsBmp)
                    val socksBytes = bitmapToPngBytes(socksBmp)
                    val previewBytes = bitmapToPngBytes(previewBmp)

                    jerseyBmp.recycle()
                    shortsBmp.recycle()
                    socksBmp.recycle()
                    previewBmp.recycle()

                    val jerseySha = sha256Hex(jerseyBytes)
                    val shortsSha = sha256Hex(shortsBytes)
                    val socksSha = sha256Hex(socksBytes)
                    val previewSha = sha256Hex(previewBytes)

                    val manifest = ExportManifest(
                        kitId = kit.id,
                        name = kit.name,
                        category = kit.category,
                        exportTimestamp = Instant.now().toString(),
                        generator = GeneratorInfo(
                            platform = "OpenKitMaker-Native",
                            version = "1.1.0",
                            buildNumber = "110"
                        ),
                        dimensions = ExportDimensions(width = 2048, height = 2048),
                        checksums = ExportChecksums(
                            textureSha256 = jerseySha,
                            previewSha256 = previewSha,
                            jerseySha256 = jerseySha,
                            shortsSha256 = shortsSha,
                            socksSha256 = socksSha
                        ),
                        kitData = ManifestKitData(
                            templateId = kit.templateId,
                            layers = kit.layers,
                            garments = kit.garments,
                            metadata = kit.metadata
                        )
                    )

                    val manifestBytes = json.encodeToString(manifest).encodeToByteArray()
                    val fileName = "kit_${kit.id}.zip"
                    val savedUri = writeZipToMediaStore(
                        context = appContext,
                        fileName = fileName,
                        jerseyBytes = jerseyBytes,
                        shortsBytes = shortsBytes,
                        socksBytes = socksBytes,
                        previewBytes = previewBytes,
                        manifestBytes = manifestBytes
                    )

                    Result.success(
                        workDataOf(
                            PngExporter.KEY_OUTPUT_URI to savedUri,
                            PngExporter.KEY_FILE_NAME to fileName
                        )
                    )
                }
            }
        } catch (e: IOException) {
            Result.failure(
                workDataOf(PngExporter.KEY_ERROR_MESSAGE to "Spazio di archiviazione locale insufficiente per completare l'operazione.")
            )
        } catch (e: Exception) {
            Result.failure(
                workDataOf(PngExporter.KEY_ERROR_MESSAGE to "Si è verificato un errore durante l'esportazione del file. Riprova.")
            )
        }
    }

    private fun bitmapToPngBytes(bitmap: Bitmap): ByteArray {
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    private fun sha256Hex(bytes: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(bytes)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun generateProceduralNormalBitmap(fabricType: FabricType, size: Int = 1024): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                var nx = 128
                var ny = 128
                var nz = 240
                when (fabricType) {
                    FabricType.MESH -> {
                        val isPore = ((x % 16 == 0 && y % 16 == 0) || (x % 16 == 8 && y % 16 == 8))
                        val waveX = (kotlin.math.sin((x / 8.0) * Math.PI * 2.0) * 14.0).toInt()
                        val waveY = (kotlin.math.cos((y / 8.0) * Math.PI * 2.0) * 14.0).toInt()
                        nx = 128 + waveX
                        ny = 128 + waveY
                        if (isPore) {
                            nx = 96
                            ny = 96
                            nz = 180
                        }
                    }
                    FabricType.POLYESTER -> {
                        val thread = (kotlin.math.sin((x / 4.0) * Math.PI * 2.0) * 8.0).toInt()
                        nx = 128 + thread
                        ny = 128
                        nz = 252
                    }
                    FabricType.COTTON -> {
                        val coarseX = (kotlin.math.sin((x / 6.0) * Math.PI * 2.0) * 20.0).toInt()
                        val coarseY = (kotlin.math.cos((y / 6.0) * Math.PI * 2.0) * 20.0).toInt()
                        val fiberNoise = ((x * 17 + y * 31) % 19) - 9
                        nx = 128 + coarseX + fiberNoise
                        ny = 128 + coarseY + fiberNoise
                        nz = 215
                    }
                    FabricType.JACQUARD -> {
                        val diag1 = if ((x + y) % 32 < 8) 32 else -12
                        val diag2 = if ((x - y + 1024) % 32 < 8) 32 else -12
                        nx = 128 + diag1
                        ny = 128 + diag2
                        nz = 210
                    }
                }
                val r = nx.coerceIn(0, 255)
                val g = ny.coerceIn(0, 255)
                val b = nz.coerceIn(0, 255)
                pixels[y * size + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return bmp
    }

    private fun generateMonochromeBitmap(factor: Float, size: Int = 1024): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(size * size)
        val baseByte = (factor.coerceIn(0f, 1f) * 255f).toInt()
        for (y in 0 until size) {
            for (x in 0 until size) {
                val noise = (((x * 19 + y * 37) % 21 - 10) * 0.03f * 25.5f).toInt()
                val v = (baseByte + noise).coerceIn(0, 255)
                pixels[y * size + x] = (0xFF shl 24) or (v shl 16) or (v shl 8) or v
            }
        }
        bmp.setPixels(pixels, 0, size, 0, 0, size, size)
        return bmp
    }

    private fun writeGlbToMediaStore(
        context: Context,
        fileName: String,
        assetPath: String = "models/kit_full_outfit_clean.glb"
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "model/gltf-binary")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OpenKitMaker")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, values)
            ?: throw IOException("Unable to create MediaStore GLB entry")

        resolver.openOutputStream(uri)?.use { out ->
            context.assets.open(assetPath).use { input ->
                input.copyTo(out)
            }
        } ?: throw IOException("Unable to open GLB OutputStream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        return uri.toString()
    }

    private fun writePbrZipToMediaStore(
        context: Context,
        fileName: String,
        jerseyBytes: ByteArray,
        shortsBytes: ByteArray,
        socksBytes: ByteArray,
        previewBytes: ByteArray,
        normalBytes: ByteArray,
        roughnessBytes: ByteArray,
        metallicBytes: ByteArray,
        pbrManifestBytes: ByteArray
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OpenKitMaker")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, values)
            ?: throw IOException("Unable to create MediaStore PBR ZIP entry")

        resolver.openOutputStream(uri)?.use { rawOut ->
            ZipOutputStream(rawOut).use { zip ->
                zip.putNextEntry(ZipEntry("albedo.png"))
                zip.write(jerseyBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("albedo_jersey.png"))
                zip.write(jerseyBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("albedo_shorts.png"))
                zip.write(shortsBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("albedo_socks.png"))
                zip.write(socksBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("normal.png"))
                zip.write(normalBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("roughness.png"))
                zip.write(roughnessBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("metallic.png"))
                zip.write(metallicBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("preview.png"))
                zip.write(previewBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("pbr_manifest.json"))
                zip.write(pbrManifestBytes)
                zip.closeEntry()
            }
        } ?: throw IOException("Unable to open PBR ZIP OutputStream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        return uri.toString()
    }

    private fun writeZipToMediaStore(
        context: Context,
        fileName: String,
        jerseyBytes: ByteArray,
        shortsBytes: ByteArray,
        socksBytes: ByteArray,
        previewBytes: ByteArray,
        manifestBytes: ByteArray
    ): String {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, "application/zip")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OpenKitMaker")
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }

        val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Files.getContentUri("external")
        }

        val uri = resolver.insert(collection, values)
            ?: throw IOException("Unable to create MediaStore ZIP entry")

        resolver.openOutputStream(uri)?.use { rawOut ->
            ZipOutputStream(rawOut).use { zip ->
                zip.putNextEntry(ZipEntry("jersey.png"))
                zip.write(jerseyBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("shorts.png"))
                zip.write(shortsBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("socks.png"))
                zip.write(socksBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("preview.png"))
                zip.write(previewBytes)
                zip.closeEntry()

                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifestBytes)
                zip.closeEntry()

                // Retrocompatibilità con v1.0
                zip.putNextEntry(ZipEntry("texture.png"))
                zip.write(jerseyBytes)
                zip.closeEntry()
            }
        } ?: throw IOException("Unable to open ZIP OutputStream")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
        }

        return uri.toString()
    }

    companion object {
        const val KEY_EXPORT_MODE = "export_mode"
        const val MODE_INTEROPERABLE_ZIP = "mode_interoperable_zip"
        const val MODE_PBR_BUNDLE = "mode_pbr_bundle"
        const val MODE_3D_GLB = "mode_3d_glb"
    }
}
