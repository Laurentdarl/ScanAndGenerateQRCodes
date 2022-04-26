package com.laurentdarl.scanandgenerateqrcodeswithimages

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.drawable.AnimationDrawable
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.transition.Visibility
import android.util.DisplayMetrics
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.isVisible
import com.google.android.material.snackbar.Snackbar
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.laurentdarl.scanandgenerateqrcodeswithimages.databinding.ActivityMainBinding
import java.io.ByteArrayOutputStream

class MainActivity : AppCompatActivity() {

    companion object {
        const val CAMERA_REQUEST_CODE = 1
        const val STORAGE_REQUEST_CODE = 2
    }

    private lateinit var binding: ActivityMainBinding
    private var bitmap: Bitmap? = null
    private var uri: Uri? = null

    private val getImage =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
            // 'ActivityResultCallback': Handle the returned Uri
            binding.imgDemo.setImageURI(uri)
            if (Build.VERSION.SDK_INT > 27) {
                bitmap = ImageDecoder.decodeBitmap(
                    ImageDecoder.createSource(
                        contentResolver,
                        uri!!
                    )
                )
                binding.imgDemo.setImageBitmap(bitmap)
                // Compress Image here
                compressImage(bitmap!!, 50)
            } else {
                binding.imgDemo.setImageURI(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
//        prevent the app from being switched to night/dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
//        Hide status bar and get a full screen
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)

        val animationDrawable = binding.mainActivityXml.background as AnimationDrawable
        animationDrawable.setEnterFadeDuration(1500)
        animationDrawable.setExitFadeDuration(3000)
        animationDrawable.start()

        val cameraPermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
                if (permissionGranted) {
                    val intent = Intent(this@MainActivity, ScanningActivity::class.java)
                    startActivity(intent)
                } else {
                    Snackbar.make(
                        binding.root,
                        "Camera permission is required for this action.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

        val storagePermission =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
                if (permissionGranted) {
                    val bitm = (binding.imgGenerateQr.drawable as BitmapDrawable).bitmap

                    val path = MediaStore.Images.Media.insertImage(
                        contentResolver,
                        bitm,
                        "QR Code",
                        "A saved QR Code Image"
                    )
                    uri = Uri.parse(path)

                    Toast.makeText(
                        applicationContext,
                        "Image saved to gallery successfully",
                        Toast.LENGTH_SHORT
                    ).show()
                } else {
                    Snackbar.make(
                        binding.root,
                        "Grant permission to save images in gallery.",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

        binding.btnScan.setOnClickListener {
            cameraPermission.launch(android.Manifest.permission.CAMERA)
        }

        binding.btnGenerateQrCode.setOnClickListener {

            if (binding.imgDemo.drawable != null) {
                qrCodeOverLay()
            } else {
                Toast.makeText(applicationContext, "Please select an image", Toast.LENGTH_SHORT)
                    .show()
            }
            binding.etQrCodeText.text = null
            binding.imgDemo.isVisible = false
        }

        binding.btnSelectImage.setOnClickListener {
            getImage.launch("image/*")
            binding.imgDemo.isVisible = true
        }

        binding.btnSaveImage.setOnClickListener {
            storagePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        fun innerCheck(name: String) {
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(applicationContext, "$name permission refused", Toast.LENGTH_SHORT)
                    .show()
            } else {
                Toast.makeText(applicationContext, "$name permission granted", Toast.LENGTH_SHORT)
                    .show()
            }
        }
        when (requestCode) {
            CAMERA_REQUEST_CODE -> {
                innerCheck("Camera")
            }
            STORAGE_REQUEST_CODE -> innerCheck("Storage")
        }
    }

    @Throws(WriterException::class)
    fun generateQrCodeBitmap(
        dimension: Int,
        overlayBitmap: Bitmap? = null,
        @ColorInt color1: Int = Color.BLACK,
        @ColorInt color2: Int = Color.WHITE
    ): Bitmap? {

        val result: BitMatrix
        val data = binding.etQrCodeText.text.toString().trim()
        if (data.isEmpty()) {
            Toast.makeText(
                applicationContext,
                "Please enter some data in the text field.",
                Toast.LENGTH_SHORT
            ).show()
        }
        try {
            result = MultiFormatWriter().encode(
                data,
                BarcodeFormat.QR_CODE,
                dimension,
                dimension,
                hashMapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H)
            )
        } catch (e: IllegalArgumentException) {
            // Unsupported format
            return null
        }

        val w = result.width
        val h = result.height
        val pixels = IntArray(w * h)
        for (y in 0 until h) {
            val offset = y * w
            for (x in 0 until w) {
                pixels[offset + x] = if (result.get(x, y)) color1 else color2
            }
        }
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, dimension, 0, 0, w, h)

        return if (overlayBitmap != null) {
            bitmap.addOverlayToCenter(overlayBitmap)
        } else {
            bitmap
        }
    }

    private fun Bitmap.addOverlayToCenter(overlayBitmap: Bitmap): Bitmap {
        val bitmap2Width = overlayBitmap.width
        val bitmap2Height = overlayBitmap.height
        val marginLeft = (this.width * 0.5 - bitmap2Width * 0.5).toFloat()
        val marginTop = (this.height * 0.5 - bitmap2Height * 0.5).toFloat()
        val canvas = Canvas(this)
        canvas.drawBitmap(this, Matrix(), null)
        canvas.drawBitmap(overlayBitmap, marginLeft, marginTop, null)
        return this
    }

    private fun qrCodeOverLay() {
        try {
            val displayMetrics = DisplayMetrics()
            windowManager?.defaultDisplay?.getMetrics(displayMetrics)
            val size = displayMetrics.widthPixels.coerceAtMost(displayMetrics.heightPixels)
            val bitmap = generateQrCodeBitmap(size, screenShotImageView(binding.imgDemo))

            bitmap?.let {
                binding.imgGenerateQr.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            // handle Errors here
            e.printStackTrace()
        }
    }

    private fun compressImage(bitmap: Bitmap, quality: Int): Bitmap {
        // Initialize a new ByteArrayStream
        val stream = ByteArrayOutputStream()

        // Compress the bitmap with JPEG format and quality 50%
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)

        val byteArray = stream.toByteArray()

        // Finally, return the compressed bitmap
        return BitmapFactory.decodeByteArray(byteArray, 0, byteArray.size)
    }

    private fun screenShotImageView(view: View): Bitmap? {
        val bitmap =
            Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        return bitmap
    }
}