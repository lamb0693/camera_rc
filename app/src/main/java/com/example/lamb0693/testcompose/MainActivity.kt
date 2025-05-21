package com.example.lamb0693.testcompose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import com.example.lamb0693.testcompose.NetworkUtil.checkWifiAndSendHello


class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        setContent {
            MainAppUI(cameraExecutor)
        }

        //checkWifiAndSendHello(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

}

