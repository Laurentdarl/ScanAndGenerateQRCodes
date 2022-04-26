package com.laurentdarl.scanandgenerateqrcodeswithimages

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.SparseArray
import android.view.SurfaceHolder
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.laurentdarl.scanandgenerateqrcodeswithimages.databinding.ActivityScanningBinding
import java.io.IOException

class ScanningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanningBinding
    private lateinit var cameraSource: CameraSource
    private lateinit var barCodeDetector: BarcodeDetector
    private val mDialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        barCodeDetector = BarcodeDetector.Builder(applicationContext)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        cameraSource = CameraSource.Builder(applicationContext, barCodeDetector)
            .setRequestedPreviewSize(640, 480)
            .setAutoFocusEnabled(true)
            .build()
        binding.svScanQr.holder!!.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.CAMERA
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    return
                }
                try {
                    cameraSource.start(holder)
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {}

            override fun surfaceDestroyed(p0: SurfaceHolder) {
                cameraSource.stop()
            }

        })

        barCodeDetector.setProcessor(object : Detector.Processor<Barcode> {
            override fun release() {}

            override fun receiveDetections(detections: Detector.Detections<Barcode>) {
                val qrCodes = detections.detectedItems
                if (qrCodes.size() != 0) {

                    val qrC = qrCodes.valueAt(0).displayValue.toString()

                    val handler = Handler(Looper.getMainLooper())
                    handler.postDelayed({
                        // Run your task here
                        dialog(qrC)
                    }, 1000)
                    mDialog

//                        Timer().schedule(3000) {
//                        }


                    binding.tvScanTextResult.text = qrC
                }
            }
        })
    }

    fun toString(pSparseArray: SparseArray<*>): String {
        val stringBuilder = StringBuilder()
        val size = pSparseArray.size()
        stringBuilder.append("{")
        for (i in 0 until size) {
            stringBuilder.append(pSparseArray.keyAt(i)).append("=")
                .append(pSparseArray.valueAt(i))
            if (i < size - 1) {
                stringBuilder.append(", ")
            } //from w ww  .j  av  a2 s  .co m
        }
        stringBuilder.append("}")
        return stringBuilder.toString()
    }

    private fun dialog(message: String) {

        if (mDialog == null || mDialog.isShowing) {
            val builder = AlertDialog.Builder(this)
            // dialog title
            builder.apply {
                setTitle("QR Code Result")
                setIcon(R.drawable.ic_notifications)
                setMessage(message)
                setPositiveButton("Visit") { _, _ ->
                    // do something on positive button click
                    if (message.contains("https://") || message.contains("http://")) {
                        val intent = Intent(
                            this@ScanningActivity,
                            WebView::class.java
                        )
                        intent.putExtra("Message", message)
                        intent.addFlags(FLAG_ACTIVITY_SINGLE_TOP)
                        startActivity(intent)
                    }
                }
            }
            // finally, create the alert dialog and show it
            val dialog = builder.create()
            dialog.show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        barCodeDetector.release()
        cameraSource.stop()
        cameraSource.release()
    }

}

