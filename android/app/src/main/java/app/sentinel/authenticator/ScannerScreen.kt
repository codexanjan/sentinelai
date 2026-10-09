package app.sentinel.authenticator

import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@Composable fun ScannerScreen(onResult:(String)->Unit,onBack:()->Unit) {
    val context=LocalContext.current;val owner=LocalLifecycleOwner.current
    val preview=remember { PreviewView(context) };var failure by remember { mutableStateOf(false) }
    val result by rememberUpdatedState(onResult)
    DisposableEffect(owner) {
        val executor=Executors.newSingleThreadExecutor();val completed=AtomicBoolean(false);val disposed=AtomicBoolean(false)
        val future=ProcessCameraProvider.getInstance(context)
        var provider:ProcessCameraProvider?=null;var boundPreview:Preview?=null;var boundAnalysis:ImageAnalysis?=null
        future.addListener({
            if(!disposed.get())try {
                provider=future.get()
                val cameraPreview=Preview.Builder().build().apply { surfaceProvider=preview.surfaceProvider }
                val analysis=ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                analysis.setAnalyzer(executor) { image ->
                    try {
                        if(!completed.get() && !disposed.get()) {
                            val plane=image.planes[0];val buffer=plane.buffer;val data=ByteArray(buffer.remaining());buffer.get(data)
                            val pixels=ByteArray(image.width*image.height)
                            for(y in 0 until image.height)for(x in 0 until image.width)pixels[y*image.width+x]=data[y*plane.rowStride+x*plane.pixelStride]
                            var source:LuminanceSource=PlanarYUVLuminanceSource(pixels,image.width,image.height,0,0,image.width,image.height,false)
                            val reader=MultiFormatReader();reader.setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE),DecodeHintType.TRY_HARDER to true))
                            for(rotation in 0..3) {
                                try {
                                    val decoded=reader.decodeWithState(BinaryBitmap(HybridBinarizer(source)))
                                    if(completed.compareAndSet(false,true))ContextCompat.getMainExecutor(context).execute { if(!disposed.get())result(decoded.text) }
                                    break
                                } catch(_:NotFoundException) { if(source.isRotateSupported)source=source.rotateCounterClockwise() else break }
                                finally { reader.reset() }
                            }
                        }
                    } catch(_:Exception) { /* Bad frames are discarded; camera data is never logged. */ }
                    finally { image.close() }
                }
                boundPreview=cameraPreview;boundAnalysis=analysis
                provider!!.bindToLifecycle(owner,CameraSelector.DEFAULT_BACK_CAMERA,cameraPreview,analysis)
            } catch(_:Exception) { failure=true }
        },ContextCompat.getMainExecutor(context))
        onDispose { disposed.set(true);boundAnalysis?.clearAnalyzer();boundPreview?.let { p -> boundAnalysis?.let { a -> provider?.unbind(p,a) } };executor.shutdown() }
    }
    Column(Modifier.fillMaxSize().padding(24.dp),verticalArrangement=Arrangement.spacedBy(16.dp)) {
        Text("Point your camera at the QR code in the service’s 2FA setup. Images stay on your device.")
        if(failure)Text("Camera unavailable. Enter the setup key instead.") else AndroidView(factory={preview},modifier=Modifier.fillMaxWidth().weight(1f))
        OutlinedButton(onClick=onBack,modifier=Modifier.fillMaxWidth()) { Text("Enter setup key instead") }
    }
}
