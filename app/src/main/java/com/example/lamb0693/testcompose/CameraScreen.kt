package com.example.lamb0693.testcompose

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.lamb0693.testcompose.ImageUtils.resizeAndSave
import java.io.File
import java.util.concurrent.ExecutorService

@Composable
fun MainAppUI(cameraExecutor: ExecutorService) {
    MaterialTheme {
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current

        val cameraPermissionGranted = remember { mutableStateOf(false) }
        val locationPermissionGranted = remember { mutableStateOf(false) }

        // 플래그로 두 권한의 요청 단계를 제어
        val permissionStage = remember { mutableStateOf(0) }

        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            cameraPermissionGranted.value = isGranted
            permissionStage.value = 1 // 다음은 위치 권한 요청
        }

        val locationPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            locationPermissionGranted.value = isGranted
        }

        // 권한 순차 요청 로직
        LaunchedEffect(permissionStage.value) {
            when (permissionStage.value) {
                0 -> {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.CAMERA
                    ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        cameraPermissionGranted.value = true
                        permissionStage.value = 1
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }

                1 -> {
                    val granted = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    if (granted) {
                        locationPermissionGranted.value = true
                    } else {
                        locationPermissionLauncher.launch(Manifest.permission.ACCESS_COARSE_LOCATION)
                    }
                }
            }
        }

        if (cameraPermissionGranted.value && locationPermissionGranted.value) {
            LaunchedEffect(Unit) {
                NetworkUtil.checkWifiAndSendHello(context)
            }
            CameraUI(context, lifecycleOwner, cameraExecutor)
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Text(
                    "카메라와 위치 권한이 모두 필요합니다.",
                    modifier = Modifier.padding(24.dp)
                )
            }
        }
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