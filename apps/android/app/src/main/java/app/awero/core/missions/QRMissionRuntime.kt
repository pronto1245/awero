package app.awero.core.missions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.lifecycle.LifecycleOwner
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

class QRMissionRuntime(private val context: Context) {
    var scannedCode: String? = null
        private set

    private val executor = Executors.newSingleThreadExecutor()
    private var provider: ProcessCameraProvider? = null
    private var scanner: com.google.mlkit.vision.barcode.BarcodeScanner? = null
    private var closed = false

    fun start(owner: LifecycleOwner, preview: PreviewView, onCode: (String) -> Unit) {
        closed = false
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            preview.post { onCode("") }
            return
        }
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            if (closed) return@addListener
            try {
                val cameraProvider = providerFuture.get()
                provider = cameraProvider
                val previewUseCase = Preview.Builder().build().also {
                    it.surfaceProvider = preview.surfaceProvider
                }
                val options = BarcodeScannerOptions.Builder()
                    .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                    .build()
                val barcodeScanner = BarcodeScanning.getClient(options)
                scanner = barcodeScanner
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { imageProxy ->
                    if (closed) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val mediaImage = imageProxy.image
                    if (mediaImage == null) {
                        imageProxy.close()
                        return@setAnalyzer
                    }
                    val image = InputImage.fromMediaImage(
                        mediaImage,
                        imageProxy.imageInfo.rotationDegrees
                    )
                    barcodeScanner.process(image)
                        .addOnSuccessListener { barcodes ->
                            if (closed) return@addOnSuccessListener
                            val value = barcodes.firstOrNull()?.rawValue
                            if (!value.isNullOrBlank() && scannedCode == null) {
                                scannedCode = value
                                onCode(value)
                                cameraProvider.unbind(analysis)
                            }
                        }
                        .addOnCompleteListener { imageProxy.close() }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    owner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    previewUseCase,
                    analysis
                )
            } catch (_: Exception) {
                close()
                preview.post { onCode("") }
            }
        }, androidx.core.content.ContextCompat.getMainExecutor(context))
    }

    fun close() {
        if (closed) return
        closed = true
        provider?.unbindAll()
        provider = null
        scanner?.close()
        scanner = null
        executor.shutdown()
    }
}
