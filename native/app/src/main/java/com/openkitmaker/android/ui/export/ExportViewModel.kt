// Implements: SCR-EXPORT / FUN-EXPORT-PNG / FUN-EXPORT-ZIP, STEP 4
package com.openkitmaker.android.ui.export

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.openkitmaker.android.data.repository.KitRepository
import com.openkitmaker.android.domain.ExportResolution
import com.openkitmaker.android.domain.Kit
import com.openkitmaker.android.export.PngExporter
import com.openkitmaker.android.export.ZipExporter
import com.openkitmaker.android.i18n.StringsIt
import com.openkitmaker.android.render.TextureEngine
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ExportViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    @ApplicationContext private val appContext: Context,
    private val kitRepository: KitRepository
) : ViewModel() {

    private val kitId: String = checkNotNull(savedStateHandle["kitId"])
    private val workManager = WorkManager.getInstance(appContext)

    private val _kit = MutableStateFlow<Kit?>(null)
    val kit: StateFlow<Kit?> = _kit.asStateFlow()

    private val _previewBitmap = MutableStateFlow<Bitmap?>(null)
    val previewBitmap: StateFlow<Bitmap?> = _previewBitmap.asStateFlow()

    private val _selectedResolution = MutableStateFlow(ExportResolution.RES_2K)
    val selectedResolution: StateFlow<ExportResolution> = _selectedResolution.asStateFlow()

    private val _exportStatus = MutableStateFlow<String?>(null)
    val exportStatus: StateFlow<String?> = _exportStatus.asStateFlow()

    private val _isExporting = MutableStateFlow(false)
    val isExporting: StateFlow<Boolean> = _isExporting.asStateFlow()

    init {
        viewModelScope.launch {
            val loaded = kitRepository.getKitById(kitId)
            _kit.value = loaded
            if (loaded != null) {
                val bmp = withContext(Dispatchers.Default) {
                    TextureEngine.renderFullKitMockupBitmap(loaded, 512, 512)
                }
                _previewBitmap.value = bmp
            }
        }
    }

    fun setResolution(res: ExportResolution) {
        _selectedResolution.value = res
    }

    fun exportGarmentPngLossless(garmentType: com.openkitmaker.android.domain.GarmentType) {
        val current = _kit.value ?: return
        val res = _selectedResolution.value
        _isExporting.value = true
        _exportStatus.value = "Avvio WorkManager per texture ${garmentType.name} (${res.pixelSize}×${res.pixelSize})..."

        val request = OneTimeWorkRequestBuilder<PngExporter>()
            .setInputData(
                workDataOf(
                    PngExporter.KEY_KIT_ID to current.id,
                    PngExporter.KEY_GARMENT_TYPE to garmentType.name,
                    PngExporter.KEY_RESOLUTION to res.pixelSize
                )
            )
            .build()

        workManager.enqueue(request)
        observeWork(request.id)
    }

    fun exportPngLossless() {
        exportGarmentPngLossless(com.openkitmaker.android.domain.GarmentType.JERSEY)
    }

    fun exportInteroperableZip() {
        val current = _kit.value ?: return
        _isExporting.value = true
        _exportStatus.value = "Avvio WorkManager per kit_${current.id}.zip..."

        val request = OneTimeWorkRequestBuilder<ZipExporter>()
            .setInputData(
                workDataOf(
                    PngExporter.KEY_KIT_ID to current.id,
                    ZipExporter.KEY_EXPORT_MODE to ZipExporter.MODE_INTEROPERABLE_ZIP
                )
            )
            .build()

        workManager.enqueue(request)
        observeWork(request.id)
    }

    fun exportPbrBundle() {
        val current = _kit.value ?: return
        _isExporting.value = true
        _exportStatus.value = "Generazione Bundle Materiali PBR (pbr_bundle_${current.id}.zip)..."

        val request = OneTimeWorkRequestBuilder<ZipExporter>()
            .setInputData(
                workDataOf(
                    PngExporter.KEY_KIT_ID to current.id,
                    ZipExporter.KEY_EXPORT_MODE to ZipExporter.MODE_PBR_BUNDLE
                )
            )
            .build()

        workManager.enqueue(request)
        observeWork(request.id)
    }

    fun exportGlb3D() {
        val current = _kit.value ?: return
        _isExporting.value = true
        _exportStatus.value = "Esportazione Modello 3D (kit_${current.id}.glb)..."

        val request = OneTimeWorkRequestBuilder<ZipExporter>()
            .setInputData(
                workDataOf(
                    PngExporter.KEY_KIT_ID to current.id,
                    ZipExporter.KEY_EXPORT_MODE to ZipExporter.MODE_3D_GLB
                )
            )
            .build()

        workManager.enqueue(request)
        observeWork(request.id)
    }

    private fun observeWork(workId: UUID) {
        viewModelScope.launch {
            workManager.getWorkInfoByIdFlow(workId).collect { info ->
                if (info == null) return@collect
                when (info.state) {
                    WorkInfo.State.SUCCEEDED -> {
                        _isExporting.value = false
                        val fileName = info.outputData.getString(PngExporter.KEY_FILE_NAME)
                        _exportStatus.value = "Esportazione completata su MediaStore.Downloads: $fileName"
                    }
                    WorkInfo.State.FAILED -> {
                        _isExporting.value = false
                        val err = info.outputData.getString(PngExporter.KEY_ERROR_MESSAGE)
                            ?: StringsIt.ERRORS_EXPORT_FAILED
                        _exportStatus.value = err
                    }
                    else -> Unit
                }
            }
        }
    }
}
