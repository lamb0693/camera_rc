package com.example.lamb0693.testcompose

import android.content.Context
import android.net.wifi.WifiManager
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import java.io.PrintWriter
import java.net.Socket

object NetworkUtil {
    fun checkWifiAndSendHello(context: Context) {
        val wifiManager = context.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val ssid = wifiManager.connectionInfo?.ssid?.replace("\"", "")

        if (ssid == "ESP32-CAMERA-SCREEN") {
            Thread {
                try {
                    val socket = Socket("192.168.4.1", 8888)
                    val output = PrintWriter(socket.getOutputStream(), true)
                    output.println("hello")
                    socket.close()
                } catch (e: Exception) {
                    Log.e(">>>>", "Socket error: ${e.message}")
                }
            }.start()
        } else {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(
                    context,
                    "ESP32 AP에 연결되어 있지 않습니다. 먼저 WiFi 설정에서 연결 후 앱을 실행해 주세요.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

}