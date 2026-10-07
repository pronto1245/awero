package app.awero.core.missions

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import java.util.concurrent.Executors

class QRMissionRuntime(private val context: Context) {
    var scannedCode: String? = null
        private set

    fun start(owner: LifecycleOwner, preview: PreviewView, onCode: (String) -> Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val previewUseCase = androidx.camera.core.Preview.Builder().build().also {
                it.surfaceProvider = preview.surfaceProvider
            }
            val analysis = ImageAnalysis.Builder().build()
            val selector = CameraSelector.DEFAULT_BACK_CAMERA
            provider.unbindAll()
            provider.bindToLifecycle(owner, selector, previewUseCase, analysis)
            // Barcode decoding is deliberately isolated behind this runtime boundary.
            // ML Kit Barcode Scanning can be added without changing MissionEngine.
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    fun accept(code:String) {
        scannedCode = code
    }

    fun matches(expected:String) = scannedCode == expected
}
