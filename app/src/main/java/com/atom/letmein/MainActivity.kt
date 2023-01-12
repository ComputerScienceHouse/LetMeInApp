package com.atom.letmein

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import com.beust.klaxon.Json
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import com.beust.klaxon.*

private val klaxon = Klaxon()

class MainActivity : AppCompatActivity() {
    // Declaring the notification items
    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationChannel: NotificationChannel
    private lateinit var builder: Notification.Builder
    private val channelId = "i.apps.notifications"

    // Declaring websocket items
    private lateinit var webSocketClient: WebSocketClient
    private var locationWS: String = ""
    private var idWS: Int = 0

    // Function called on creation of activity
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    private fun initWebSocket(location: String) {
        val hostString = getString(R.string.wsURI)
        val wsURL = "wss://$hostString/knock/socket/$location"
        val wsURI = URI(wsURL)

        // Verify that name is filled in
        if (findViewById<EditText>(R.id.userName).text.isEmpty()) {
            showAlert("Error", "Missing name!")
            return
        }

        // Verify that web socket isn't in use
        if (this::webSocketClient.isInitialized) {
            if (webSocketClient.isOpen) {
                showAlert("Error", "There is already a request running!")
                return
            }
        }

        createWebSocketClient(wsURI, location)
        webSocketClient.connect()
    }

    private fun createWebSocketClient(wsURI: URI?, location: String) {
        webSocketClient = object : WebSocketClient(wsURI) {

            override fun onOpen(handshakedata: ServerHandshake?) {
                println("Connection opened!")
                val name = findViewById<EditText>(R.id.userName).text
                val payload = """{"Event": "NAME", "Name": "$name", "Location": "$location"}"""
                send(payload)
                runOnUiThread { findViewById<Button>(R.id.cancelButton).visibility = View.VISIBLE }
            }

            override fun onMessage(message: String?) {
                message?.let {
                    val response = ServerResponse.fromJson(message)
                    println(response?.toJson())
                    val id = response?.id?.split("_")?.last()?.toInt()
                    id?.let {
                        when (response.event) {
                            "LOCATION" -> {
                                if (idWS == 0) {
                                    idWS = it
                                    sendPermanentPush(getString(R.string.req_sent), "${getString(R.string.req_sent_exp)} (${response.currentTime}s)", it)
                                }
                            }
                            "COUNTDOWN" -> updatePush("${getString(R.string.req_sent_exp)} (${response.currentTime}s)", it)
                            "TIMEOUT" -> {
                                sendTemporaryPush(getString(R.string.req_timeout), getString(R.string.req_timeout_exp))
                                this.close(1000, "Request timed out")
                            }
                            "ACKNOWLEDGE" -> {
                                sendTemporaryPush(getString(R.string.req_ack), getString(R.string.req_ack_exp))
                                this.close(1000, "Request was acknowledged")
                            }
                        }
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("Connection was closed! Reason: $reason")
                notificationManager.cancel(idWS)
                idWS = 0
                runOnUiThread { findViewById<Button>(R.id.cancelButton).visibility = View.INVISIBLE }
            }

            override fun onError(ex: Exception?) {
                println("Connection ran into an error: ${ex.toString()}")
                ex?.printStackTrace()
                notificationManager.cancel(idWS)
                sendTemporaryPush(getString(R.string.req_error), getString(R.string.req_error_exp), 2024)
                idWS = 0
                runOnUiThread { findViewById<Button>(R.id.cancelButton).visibility = View.INVISIBLE }
                close(1000, "Ran into an error!")
            }
        }
    }

    fun sendPermanentPush(subject: String, description: String, code: Int = 2023) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_HIGH)
            notificationChannel.lightColor = Color.MAGENTA
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this, channelId)
        } else {
            builder = Notification.Builder(this)
        }

        // Add intent to return to application
        val actIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, actIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setSmallIcon(R.drawable.csh_logo)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.csh_logo))
            .setContentTitle(subject)
            .setContentText(description)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(pendingIntent)
            .setColor(Color.rgb(55, 0, 179))
            .style = Notification.BigTextStyle()
            .bigText(description)
        notificationManager.notify(code, builder.build())
    }

    fun sendTemporaryPush(subject: String, description: String, code: Int = 2023) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notificationChannel =
                NotificationChannel(channelId, description, NotificationManager.IMPORTANCE_LOW)
            notificationChannel.lightColor = Color.MAGENTA
            notificationManager.createNotificationChannel(notificationChannel)

            builder = Notification.Builder(this, channelId)
        } else {
            builder = Notification.Builder(this)
        }

        // Add intent to return to application
        val actIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Intent.ACTION_MAIN
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, actIntent,
            PendingIntent.FLAG_UPDATE_CURRENT)

        builder.setSmallIcon(R.drawable.csh_logo)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.csh_logo))
            .setContentTitle(subject)
            .setContentText(description)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setColor(Color.rgb(55, 0, 179))
            .style = Notification.BigTextStyle()
            .bigText(description)
        notificationManager.notify(code, builder.build())
    }

    fun updatePush(description: String, code: Int = 2023) {
        builder.setContentText(description)
            .style = Notification.BigTextStyle()
            .bigText(description)
        notificationManager.notify(code, builder.build())
    }

    fun sendNVM(view: View) {
        println("running nvm")
        if (this::webSocketClient.isInitialized) {
            if (webSocketClient.isOpen) {
                val payload = """{"Event":"NEVERMIND", "LOCATION": "$locationWS"}"""
                webSocketClient.send(payload)
                webSocketClient.close(1000, "Request cancelled")
                sendTemporaryPush(getString(R.string.req_cancel), getString(R.string.req_cancel_exp), 2024)
            }
        }
    }

    private fun showAlert(title: String, description: String) {
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
        initWebSocket("l_well")
    }

    fun reqFirstElevator(view: View) {
        initWebSocket("level_1")
    }

    fun reqAElevator(view: View) {
        initWebSocket("level_a")
    }

    fun reqNorthStairwell(view: View) {
        initWebSocket("n_stairs")
    }

    fun reqSouthStairwell(view: View) {
        initWebSocket("s_stairs")
    }
}

data class ServerResponse (
    @Json(name = "ID")
    val id: String,

    @Json(name = "Event")
    val event: String,

    @Json(name = "CurrentTime")
    val currentTime: Long,

    @Json(name = "MaxTime")
    val maxTime: Long,

    @Json(name = "Name")
    val name: String,

    @Json(name = "Location")
    val location: String,

    @Json(name = "ShortLocation")
    val shortLocation: String,

    @Json(name = "ClientLocation")
    val clientLocation: String,

    @Json(name = "SlackMessageTS")
    val slackMessageTS: String
)
{
    fun toJson() = klaxon.toJsonString(this)

    companion object {
        fun fromJson(json: String) = klaxon.parse<ServerResponse>(json)
    }
}
