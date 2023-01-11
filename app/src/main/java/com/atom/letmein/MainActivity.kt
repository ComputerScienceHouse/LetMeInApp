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
import com.beust.klaxon.Json
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import com.beust.klaxon.*

private val klaxon = Klaxon()

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
        val hostString = getString(R.string.wsURI)
        val wsURL = "wss://$hostString/knock/socket/$location"
        val wsURI: URI? = URI(wsURL)

        if (findViewById<EditText>(R.id.userName).text.isEmpty()) {
            showAlert("Error", "Missing name!")
            return
        }
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
            var idWS: Int = 0
            override fun onOpen(handshakedata: ServerHandshake?) {
                println("Connection opened!")
                val name = findViewById<EditText>(R.id.userName).text
                val payload = """{"Event": "NAME", "Name": "$name", "Location": "$location"}"""
                send(payload)
                //TODO("link nvm button")
            }

            override fun onMessage(message: String?) {
                message?.let {
                    val response = ServerResponse.fromJson(message)
                    println(response?.toJson())
                    val id = response?.id?.split("_")?.last()?.toInt()
                    id?.let {
                        when (response?.event) {
                            "LOCATION" -> {
                                if (idWS == 0) {
                                    idWS = it
                                    sendPermanentPush(getString(R.string.req_sent), "${getString(R.string.req_sent_exp)} (${response?.currentTime}s)", it)
                                }
                            }
                            "COUNTDOWN" -> updatePush("${getString(R.string.req_sent_exp)} (${response?.currentTime}s)", it)
                            "TIMEOUT" -> {
                                sendTemporaryPush(getString(R.string.req_timeout), getString(R.string.req_timeout_exp))
                                this.close()
                            }
                            "ACKNOWLEDGE" -> {
                                sendTemporaryPush(getString(R.string.req_ack), getString(R.string.req_ack_exp))
                                this.close()
                            }
                        }
                    }
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                println("Connection was closed! Reason: $reason")
                notificationManager.cancel(idWS)
            }

            override fun onError(ex: Exception?) {
                println("Connection ran into an error: ${ex.toString()}")
                ex?.printStackTrace()
                notificationManager.cancel(idWS)
                sendTemporaryPush(getString(R.string.req_error), getString(R.string.req_error_exp), 2024)
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
        builder.setSmallIcon(R.drawable.csh_logo)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.csh_logo))
            .setContentTitle(subject)
            .setContentText(description)
            .setAutoCancel(false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
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
        builder.setSmallIcon(R.drawable.csh_logo)
            .setLargeIcon(BitmapFactory.decodeResource(this.resources, R.drawable.csh_logo))
            .setContentTitle(subject)
            .setContentText(description)
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
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<ServerResponse>(json)
    }
}
