package com.laurentdarl.scanandgenerateqrcodeswithimages

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.SparseArray
import android.view.SurfaceHolder
import androidx.core.app.ActivityCompat
import com.google.android.gms.vision.CameraSource
import com.google.android.gms.vision.Detector
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.laurentdarl.scanandgenerateqrcodeswithimages.databinding.ActivityScanningBinding
import java.io.IOException

class ScanningActivity : AppCompatActivity() {

    private lateinit var binding: ActivityScanningBinding
    private lateinit var cameraSource: CameraSource
    private lateinit var barCodeDetector: BarcodeDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScanningBinding.inflate(layoutInflater)
        setContentView(binding.root)

        barCodeDetector = BarcodeDetector.Builder(applicationContext)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
        cameraSource = CameraSource.Builder(applicationContext, barCodeDetector)
            .setRequestedPreviewSize(640, 480)
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
                    binding.tvScanTextResult.post(Runnable
                    // Use the post method of the TextView
                    {
                        val qrC = toString(qrCodes)
                        if (qrC.contains("https://") || qrC.contains("http://")) {
                            val intent = Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse(qrC)
                            )
                            startActivity(intent)
                            dialogBox(qrCodes.valueAt(0).displayValue.toString(), intent)
                        } else {
                            binding.tvScanTextResult.text =
                                qrCodes.valueAt(0).displayValue.toString()
                        }
                    })
                }
            }
        })

        binding.tvScanTextResult.setOnClickListener {
            if (TextUtils.isEmpty(binding.tvScanTextResult.toString())
                && binding.tvScanTextResult.toString()
                    .contains("https://") || binding.tvScanTextResult.toString().contains("http://")
            ) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(binding.tvScanTextResult.toString())
                )
                startActivity(intent)
            }
        }
    }

    private fun dialogBox(message: String, intent: Intent) {
        val builder = MaterialAlertDialogBuilder(applicationContext)
        builder.apply {
            setTitle("QR scan result!")
            setMessage("Visit: $message now")
            setPositiveButton("Allow") { _, _ ->
                // do something when positive button clicked
            }
            setPositiveButtonIcon(getDrawable(R.drawable.ic_camera))
            setCancelable(true)
        }
        val dialog = builder.create()
        dialog.show()
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

    override fun onDestroy() {
        super.onDestroy()
        barCodeDetector.release()
        cameraSource.stop()
        cameraSource.release()
    }

}

