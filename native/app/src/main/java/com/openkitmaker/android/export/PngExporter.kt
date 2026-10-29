// Implements: SCR-EXPORT / FUN-EXPORT-PNG
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
import com.openkitmaker.android.render.TextureEngine
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * WorkManager CoroutineWorker per l'esportazione in background della texture PNG 2048x2048 lossless
 * su MediaStore.Downloads (Scoped Storage Android 10+).
 */
@HiltWorker
class PngExporter @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val kitRepository: KitRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val kitId = inputData.getString(KEY_KIT_ID) ?: return@withContext Result.failure()
        val kit = kitRepository.getKitById(kitId) ?: return@withContext Result.failure()
        val garmentTypeName = inputData.getString(KEY_GARMENT_TYPE) ?: com.openkitmaker.android.domain.GarmentType.JERSEY.name
        val garmentType = runCatching { com.openkitmaker.android.domain.GarmentType.valueOf(garmentTypeName) }
            .getOrDefault(com.openkitmaker.android.domain.GarmentType.JERSEY)

        val resolution = inputData.getInt(KEY_RESOLUTION, TextureEngine.FULL_RESOLUTION)

        try {
            val baseBitmap = TextureEngine.compositeGarmentBitmap(
                kit = kit,
                garmentType = garmentType,
                width = TextureEngine.FULL_RESOLUTION,
                height = TextureEngine.FULL_RESOLUTION
            )

            val bitmap = if (resolution == 4096) {
                val scaled = Bitmap.createScaledBitmap(baseBitmap, 4096, 4096, true)
                baseBitmap.recycle()
                scaled
            } else {
                baseBitmap
            }

            val prefix = garmentType.name.lowercase()
            val fileName = "${prefix}_${kit.id}_${resolution}.png"
            val savedUri = writePngToMediaStore(appContext, fileName, bitmap)
            bitmap.recycle()

            Result.success(
                workDataOf(
                    KEY_OUTPUT_URI to savedUri,
                    KEY_FILE_NAME to fileName
                )
            )
        } catch (e: IOException) {
            Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "Spazio di archiviazione locale insufficiente per completare l'operazione.")
            )
        } catch (e: Exception) {
            Result.failure(
                workDataOf(KEY_ERROR_MESSAGE to "Si è verificato un errore durante l'esportazione del file. Riprova.")
            )
        }
    }

    companion object {
        const val KEY_KIT_ID = "kit_id"
        const val KEY_GARMENT_TYPE = "garment_type"
        const val KEY_RESOLUTION = "resolution"
        const val KEY_OUTPUT_URI = "output_uri"
        const val KEY_FILE_NAME = "file_name"
        const val KEY_ERROR_MESSAGE = "error_message"

        fun writePngToMediaStore(context: Context, fileName: String, bitmap: Bitmap): String {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/OpenKitMaker")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
            }

            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }

            val uri = resolver.insert(collection, contentValues)
                ?: throw IOException("Unable to create MediaStore entry")

            resolver.openOutputStream(uri)?.use { stream ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                    throw IOException("PNG lossless compression failed")
                }
            } ?: throw IOException("Unable to open OutputStream")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            return uri.toString()
        }
    }
}
