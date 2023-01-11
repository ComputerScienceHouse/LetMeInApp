package com.atom.letmein

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI

class MainActivity : AppCompatActivity() {
    lateinit var notificationManager: NotificationManager
    lateinit var notificationChannel: NotificationChannel
    lateinit var builder: Notification.Builder
    private val channelId = "i.apps.notifications"

    private lateinit var webSocketClient: WebSocketClient
    companion object {
        const val TAG = "LETMEIN"
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun initWebSocket(location: String) {
        //val hostString = R.string.wsURI
        val wsURL = "wss://letmein.csh.rit.edu/knock/socket/$location"
        val wsURI: URI? = URI(wsURL)

        createWebSocketClient(wsURI, location)
    }

    private fun createWebSocketClient(wsURI: URI?, location: String) {
        webSocketClient = object : WebSocketClient(wsURI) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("Connection opened!")
                val name = findViewById<EditText>(R.id.userName).text
                val payload = """{"Event": "NAME", "Name": "$name", "Location": "$location"}"""
                send(payload)
                //TODO("link nvm button")
            }

            override fun onMessage(message: String?) {
                message?.let {
                    val moshi = Moshi.Builder().build()
                    val adapter: JsonAdapter<ServerResponse> = moshi.adapter(ServerResponse::class.java)
                    val response = adapter.fromJson(message)
                    when (response?.event) {
                        "LOCATION" -> println("LOCATION")
                        "COUNTDOWN" -> println("COUNTDOWN")
                        "ACKNOWLEDGE" -> println("ACKNOWLEDGE")
                        "TIMEOUT" -> println("TIMEOUT")
                        else -> println("Unknown message received...")
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("Connection was closed! Reason: $reason")
            }

            override fun onError(ex: Exception?) {
                println("Connection ran into an error: ${ex.toString()}")
            }
        }
    }

    fun sendPush(title: String, description: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.MAGENTA
            notificationChannel.enableVibration(true)
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this, channelId)
        } else {
            builder = Notification.Builder(this)
        }
        builder.setSmallIcon(R.drawable.csh_logo)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.csh_logo))
            .setContentText(description)
        notificationManager.notify(1234, builder.build())
    }

    fun showAlert(title: String, description: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
        builder.setMessage(description)
        builder.setNeutralButton(R.string.ok) { dialog, _ ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    /** button events **/
    fun reqLWell(view: View) {
        if (findViewById<EditText>(R.id.userName).text.isEmpty()) {
            showAlert("Error", "Missing name!")
            return
        } else if (webSocketClient.isOpen) {
            showAlert("Error", "Please wait before putting in another request!")
            return
        }
        sendPush("LetMeIn", "Your request was sent!")
        initWebSocket("l_well")
        webSocketClient.connect()
    }

    fun reqFirstElevator(view: View) {
        if (findViewById<EditText>(R.id.userName).text.isEmpty()) {
            showAlert("Error", "Missing name!")
            return
        } else if (webSocketClient.isOpen) {
            showAlert("Error", "Please wait before putting in another request!")
            return
        }
        sendPush("LetMeIn", "Your request was sent!")
        initWebSocket("level_1")
        webSocketClient.connect()
    }

    fun reqAElevator(view: View) {
        if (findViewById<EditText>(R.id.userName).text.isEmpty()) {
            showAlert("Error", "Missing name!")
            return
        } else if (webSocketClient.isOpen) {
            showAlert("Error", "Please wait before putting in another request!")
            return
        }
        sendPush("LetMeIn", "Your request was sent!")
        initWebSocket("level_a")
        webSocketClient.connect()
    }

    fun reqNorthStairwell(view: View) {
        if (findViewById<EditText>(R.id.userName).text.isEmpty()) {
            showAlert("Error", "Missing name!")
            return
        } else if (webSocketClient.isOpen) {
            showAlert("Error", "Please wait before putting in another request!")
            return
        }
        sendPush("LetMeIn", "Your request was sent!")
        initWebSocket("n_stairs")
        webSocketClient.connect()
    }

    fun reqSouthStairwell(view: View) {
        if (findViewById<EditText>(R.id.userName).text.isEmpty()) {
            showAlert("Error", "Missing name!")
            return
        } else if (webSocketClient.isOpen) {
            showAlert("Error", "Please wait before putting in another request!")
            return
        }
        sendPush("LetMeIn", "Your request was sent!")
        initWebSocket("s_stairs")
        webSocketClient.connect()
    }
}

@JsonClass(generateAdapter = true)
data class ServerResponse(val event: String?, val name: String?, val id: String?)