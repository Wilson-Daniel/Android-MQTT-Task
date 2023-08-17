package com.WILSON.mqttandroidsend

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import com.WILSON.mqttandroidsend.mqtt.MqttManagerImpl
import com.WILSON.mqttandroidsend.mqtt.MqttStatusListener
import kotlinx.android.synthetic.main.activity_main.*
import org.eclipse.paho.client.mqttv3.MqttMessage

const val TAG = "msg"
const val serverUri = "tcp://broker.hivemq.com:1883"
const val subscriptionTopic = "messagesFromWilsonSensor/#"
const val publishTopic = "messagesFromWilsonSensor"

class MainActivity : AppCompatActivity() {

    private var clientId = "MyAndroidClientId" + System.currentTimeMillis()
    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null

    lateinit var mqttManager: MqttManagerImpl

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        mqttManager = MqttManagerImpl(
                applicationContext,
                serverUri,
                clientId,
                arrayOf(subscriptionTopic),
                IntArray(1) { 0 })
        mqttManager.init()
        initMqttStatusListener()
        mqttManager.connect()

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        accelerometer = sensorManager!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val accelerometerListener: SensorEventListener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val xAxis = event.values[0]
                val yAxis = event.values[1]
                val zAxis = event.values[2]
                main_X.setText("X: $xAxis")
                main_Y.setText("Y: $yAxis")
                main_Z.setText("Z: $zAxis")

                // Publish accelerometer data to MQTT topic

            }

            override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
                // Handle accuracy changes if needed
            }
        }

        // Register the listener
        sensorManager!!.registerListener(
            accelerometerListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        buttonSubmit.setOnClickListener {
            submitMessage()
        }
    }

    private fun initMqttStatusListener() {
        mqttManager.mqttStatusListener = object : MqttStatusListener {
            override fun onConnectComplete(reconnect: Boolean, serverURI: String) {
                if (reconnect) {
                    displayInDebugLog("Reconnected to : $serverURI")
                } else {
                    displayInDebugLog("Connected to: $serverURI")
                }
            }

            override fun onConnectFailure(exception: Throwable) {
                displayInDebugLog("Failed to connect")
            }

            override fun onConnectionLost(exception: Throwable) {
                displayInDebugLog("The Connection was lost.")
            }

            override fun onMessageArrived(topic: String, message: MqttMessage) {
                displayInMessagesList(String(message.payload))
            }

            override fun onTopicSubscriptionSuccess() {
                displayInDebugLog("Subscribed!")
            }

            override fun onTopicSubscriptionError(exception: Throwable) {
                displayInDebugLog("Failed to subscribe")
            }

        }
    }

    private fun displayInMessagesList(message: String) {
        textLogs.apply {
            setText(message + "\n" + text)
        }
    }

    private fun displayInDebugLog(message: String) {
        Log.i(TAG, message)
    }

    private fun submitMessage() {
        //val message = editTextMessage.text.toString()
        val messs = main_X.text.toString() + " "+main_Y.text.toString()+" "+main_Z.text.toString();
        if (TextUtils.isEmpty(messs)) {
            displayToast(R.string.general_please_write_some_message)
            return
        }
        mqttManager.sendMessage(messs, publishTopic)

    }



    private fun displayToast(@StringRes messageId: Int) {
        Toast.makeText(this, messageId, Toast.LENGTH_LONG).show()
    }

}

