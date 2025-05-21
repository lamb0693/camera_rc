package com.example.lamb0693.testcompose

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import androidx.core.graphics.scale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.LifecycleOwner
import java.io.OutputStream

class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            MaterialTheme {
                val context = LocalContext.current
                val lifecycleOwner = this@MainActivity

                val permissionGranted = remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    permissionGranted.value = isGranted
                }

                // 권한 요청 시도
                LaunchedEffect(Unit) {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        permissionGranted.value = true
                    } else {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                if (permissionGranted.value) {
                    CameraUI(context, lifecycleOwner, cameraExecutor)
                } else {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text("카메라 권한이 필요합니다", modifier = Modifier.padding(24.dp))
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun resizeAndSave(bitmapFile: File, context: Context) {
        val bitmap = BitmapFactory.decodeFile(bitmapFile.absolutePath)
        val resized = bitmap.scale(320, 200)

        val filename = "resized_${System.currentTimeMillis()}.jpg"

        val resolver = context.contentResolver
        val imageCollection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }

        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, filename)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val imageUri = resolver.insert(imageCollection, contentValues)

        if (imageUri != null) {
            try {
                val outputStream: OutputStream? = resolver.openOutputStream(imageUri)
                if (outputStream != null) {
                    resized.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
                    outputStream.close()

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        contentValues.clear()
                        contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(imageUri, contentValues, null, null)
                    }

                    Log.d(">>>>", "Image saved to gallery: $imageUri")
                }
            } catch (e: Exception) {
                Log.e(">>>>", "Failed to save image to gallery", e)
            }
        } else {
            Log.e(">>>>", "Failed to create new MediaStore record.")
        }
    }

    @Composable
    fun CameraUI(context: Context, lifecycleOwner: LifecycleOwner, cameraExecutor: ExecutorService) {
        var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .background(Color.Cyan)
            ) {
                Text("Header", modifier = Modifier.padding(16.dp))
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
            ) {
                CameraPreview(context, lifecycleOwner) { capture ->
                    imageCapture = capture
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = { /* 향후 기능 추가 */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("촬영", color = Color.Black)
                }

                Button(
                    onClick = {
                        Log.i(">>>>", "capture button clicked")
                        val capture = imageCapture
                        if (capture != null) {
                            val photoFile = File(
                                context.cacheDir,
                                "captured_${System.currentTimeMillis()}.jpg"
                            )

                            val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                            capture.takePicture(
                                outputOptions,
                                cameraExecutor,
                                object : ImageCapture.OnImageSavedCallback {
                                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                        Log.i(">>>>", "onImageSaved called")
                                        resizeAndSave(photoFile, context)
                                    }

                                    override fun onError(exception: ImageCaptureException) {
                                        Log.e(">>>>", "Image capture failed", exception)
                                    }
                                }
                            )
                        } else {
                            Log.w(">>>>", "imageCapture is null")
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White)
                ) {
                    Text("Capture", color = Color.Black)
                }
            }
        }
    }
}

