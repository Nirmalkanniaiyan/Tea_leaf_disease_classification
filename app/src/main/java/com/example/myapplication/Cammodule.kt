package com.example.myapplication
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel
import java.io.FileInputStream
import java.nio.ByteOrder

class Cammodule : AppCompatActivity() {
    private lateinit var loadBtn: Button
    private lateinit var takeBtn: Button
    private lateinit var predictBtn: Button
    private lateinit var imageView: ImageView
    private lateinit var flowerNameTextView: TextView

    private lateinit var tflite: Interpreter
    private var currentBitmap: Bitmap? = null // Store the currently loaded bitmap

    private val NUM_CLASSES = 8 // Replace this with the actual number of flower categories in your model

    private val galleryResultLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            try {
                currentBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                imageView.setImageBitmap(currentBitmap)
            } catch (e: IOException) {
                Log.e("Cammodule", "Error loading image from gallery", e)
                flowerNameTextView.text = "Error loading image from gallery"
            }
        }
    }

    private val cameraResultLauncher = registerForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap: Bitmap? ->
        if (bitmap != null) {
            currentBitmap = bitmap
            imageView.setImageBitmap(currentBitmap)
        } else {
            flowerNameTextView.text = "Error capturing image"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.cammodule)
        enableEdgeToEdge()
        supportActionBar?.hide()

        // Check camera permission and request if not granted
        checkCameraPermission()

        // Initialize buttons and image view
        loadBtn = findViewById(R.id.loadbtn)
        takeBtn = findViewById(R.id.takeBtn)
        predictBtn = findViewById(R.id.predictBtn)
        imageView = findViewById(R.id.imageView)
        flowerNameTextView = findViewById(R.id.output_textview)

        loadBtn.setOnClickListener { openGallery() }
        takeBtn.setOnClickListener { openCamera() }

        predictBtn.setOnClickListener {
            currentBitmap?.let { bitmap ->
                predictFlower(bitmap)
            } ?: run {
                flowerNameTextView.text = "Please load an image first."
            }
        }

        // Load the model
        try {
            tflite = Interpreter(loadModelFile("ensemble_model.tflite")) // Update with your model's name
        } catch (e: IOException) {
            flowerNameTextView.text = "Model loading failed."
            Log.e("Cammodule", "Error loading model", e)
        }
    }

    private fun openGallery() {
        galleryResultLauncher.launch("image/*")
    }

    private fun openCamera() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            cameraResultLauncher.launch(null)
        } else {
            requestCameraPermission()
        }
    }

    private fun predictFlower(bitmap: Bitmap) {
        if (::tflite.isInitialized) {
            val inputBuffer = convertBitmapToByteBuffer(bitmap)

            // Assuming the output is a float array with size equal to the number of classes
            val outputArray = Array(1) { FloatArray(NUM_CLASSES) }

            // Run inference
            tflite.run(inputBuffer, outputArray)

            // Get the predicted flower name
            val predictedIndex = outputArray[0].indices.maxByOrNull { outputArray[0][it] } ?: -1

            // Check for valid index and update text
            flowerNameTextView.text = if (predictedIndex != -1) {
                "Output: ${getFlowerName(predictedIndex)}"
            } else {
                "Prediction failed."
            }
        } else {
            flowerNameTextView.text = "Model is not initialized."
        }
    }


    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val width = 224 // Use the actual input width required by your model
        val height = 224 // Use the actual input height required by your model

        val byteBuffer = ByteBuffer.allocateDirect(4 * width * height * 3) // 4 bytes per float, 3 channels (RGB)
        byteBuffer.order(ByteOrder.nativeOrder())

        // Resize the bitmap to match the model's input size
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, width, height, true)

        val intValues = IntArray(width * height)
        resizedBitmap.getPixels(intValues, 0, resizedBitmap.width, 0, 0, resizedBitmap.width, resizedBitmap.height)

        // Convert the image pixels to floats in the range [0, 1]
        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16 and 0xFF) / 127.5f) - 1f) // Red channel
            byteBuffer.putFloat(((pixelValue shr 8 and 0xFF) / 127.5f) - 1f)  // Green channel
            byteBuffer.putFloat(((pixelValue and 0xFF) / 127.5f) - 1f)      // Blue channel
        }

        return byteBuffer
    }

    private fun loadModelFile(filename: String): MappedByteBuffer {
        val fileDescriptor = assets.openFd(filename)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = fileDescriptor.startOffset
        val declaredLength = fileDescriptor.declaredLength
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
    }

    private fun getFlowerName(index: Int): String {
        return when (index) {
            0 -> "Anthracnose"
            1 -> "Algal leaf"
            2 -> "Bird Eye Spot"
            3 -> "Brown Blight"
            4 -> "Gray Light"
            5 -> "Healthy"
            6 -> "Red Leaf Spot"
            7 -> "White Spot"

            else -> "Unknown Disease"
        }
    }

    private fun checkCameraPermission() {
        if (checkSelfPermission(android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(android.Manifest.permission.CAMERA), CAMERA_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_REQUEST_CODE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Log.d("Cammodule", "Camera permission granted")
            } else {
                Log.d("Cammodule", "Camera permission denied")
                flowerNameTextView.text = "Camera permission is required to take pictures."
            }
        }
    }

    companion object {
        const val CAMERA_REQUEST_CODE = 100
    }

    // Release resources
    override fun onDestroy() {
        super.onDestroy()
        if (::tflite.isInitialized) {
            tflite.close()
        }
    }
}
