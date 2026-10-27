// Implements: SCR-EXPORT / FUN-EXPORT-ZIP
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
 * WorkManager CoroutineWorker per l'esportazione dell'archivio interoperabile `kit_<id>.zip`
 * contenente `texture.png` (2048x2048), `preview.png` (512x512) e `manifest.json` conforme a `export-format.md`.
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

        try {
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
}
