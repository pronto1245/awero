package app.awero.core.missions

import android.content.Context
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QRMissionRuntime(private val context: Context) {
    var scannedCode: String? = null
        private set

    private val executor = Executors.newSingleThreadExecutor()

    fun start(owner: LifecycleOwner, preview: PreviewView, onCode: (String) -> Unit) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val previewUseCase = Preview.Builder().build().also {
                it.surfaceProvider = preview.surfaceProvider
            }
            val options = BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
            val scanner = BarcodeScanning.getClient(options)
            val analysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { imageProxy ->
                val mediaImage = imageProxy.image
                if (mediaImage == null) {
                    imageProxy.close()
                    return@setAnalyzer
                }
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                scanner.process(image)
                    .addOnSuccessListener { barcodes ->
                        val value = barcodes.firstOrNull()?.rawValue
                        if (!value.isNullOrBlank() && scannedCode == null) {
                            scannedCode = value
                            onCode(value)
                            provider.unbind(analysis)
                        }
                    }
                    .addOnCompleteListener { imageProxy.close() }
            }
            provider.unbindAll()
            try {
                provider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase,
                    analysis
                )
            } catch (_: Exception) {
                provider.unbindAll()
                onCode("")
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    fun accept(code: String) {
        scannedCode = code
    }

    fun matches(expected: String) = scannedCode == expected

    fun close() {
        executor.shutdown()
    }
}
