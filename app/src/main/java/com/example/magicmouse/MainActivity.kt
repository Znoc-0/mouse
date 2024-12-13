package com.example.magicmouse

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.magicmouse.ui.theme.MagicMouseTheme
import java.io.OutputStream
import java.net.Socket

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MagicMouseTheme {
                MagicMouseScreen()
            }
        }
    }
}

@Composable
fun MagicMouseScreen() {
    val sensorManager = LocalContext.current.getSystemService(SensorManager::class.java)
    val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    var socket by remember { mutableStateOf<Socket?>(null) }
    var isSending by remember { mutableStateOf(false) }
    var xRotation by remember { mutableStateOf(0f) }
    var yRotation by remember { mutableStateOf(0f) }
    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                if (event.sensor.type == Sensor.TYPE_GYROSCOPE && isSending) {
                     xRotation = event.values[0]
                     yRotation = event.values[1]

                    // Send data to server
                    socket?.let {
                        Thread {
                            sendMouseData(it, xRotation, yRotation)
                        }.start()
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(listener, gyroscope, SensorManager.SENSOR_DELAY_UI)

        onDispose {
            sensorManager.unregisterListener(listener)
            socket?.close()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Magic Mouse")
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "sending data = ${gyroscope != null} and ${socket != null}")
        Spacer(modifier = Modifier.height(16.dp))
        Text(text ="x_rotation = ${xRotation}")
        Spacer(modifier = Modifier.height(16.dp))
        Text(text ="y_rotation = ${yRotation}")

        Button(onClick = {
            Thread{
            if (isSending) {
                socket?.close()
                socket = null
                isSending = false
            } else {
                try {
                    socket = Socket("192.168.1.37", 8000) // Replace with server IP
                    isSending = true
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }.start()
    }) {
            Text(if (isSending) "Stop" else "Start")
        }
    }
}

fun sendMouseData(socket: Socket, xRotation: Float, yRotation: Float) {
    val data = """{"x_rotation":$xRotation,"y_rotation":$yRotation}\n"""
    try {
        val outputStream: OutputStream = socket.getOutputStream()
        outputStream.write(data.toByteArray())
        outputStream.flush()
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
