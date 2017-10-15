package xyz.donot.detector

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import android.widget.Toast
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import twitter4j.*
import xyz.donot.detector.model.AppDatabase
import xyz.donot.detector.model.StatusEntity
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL
import kotlin.concurrent.thread


class StreamingService : Service() {
    lateinit private var twitter: Twitter
    private var id: Long = 0
    lateinit private var stream: TwitterStream
    private fun handleActionStream() {
        launch(UI) {
            async(CommonPool) { twitter = AppDatabase.getInstance(this@StreamingService).userRoomDao().getMyAccount().account }.await()
            async(CommonPool) { stream = TwitterStreamFactory().getInstance(twitter.authorization) }.await()
            async(CommonPool) { id = AppDatabase.getInstance(this@StreamingService).userRoomDao().getMyAccount().id }.await()
            val notificationIntent = Intent(this@StreamingService, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(this@StreamingService, 0, notificationIntent, 0)
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (Build.VERSION.SDK_INT >= 26) {
                val channel = NotificationChannel(
                        "delete_stream",
                        "ツイ消し感知ストリーム",
                        NotificationManager.IMPORTANCE_DEFAULT
                )
                manager.createNotificationChannel(channel)
            }

            val mNotification =
                    NotificationCompat.Builder(this@StreamingService, "delete_stream").setSmallIcon(R.drawable.tw__ic_logo_default)
                            .setContentTitle("ストリーム起動中")
                            .setAutoCancel(false)
                            .setContentIntent(pendingIntent)
                            .setContentText("ツイ消し感知が動作中です。")
                            .build()

            startForeground(10, mNotification)
            StreamCreateUtil.addStatusListener(stream, MyStreamAdapter())
            stream.addConnectionLifeCycleListener(MyConnectionListener())
            stream.user()
        }
    }


    override fun onCreate() {
        super.onCreate()
        try {
            handleActionStream()
        } catch (e: TwitterException) {
            e.printStackTrace()
        }
    }

    override fun onBind(intent: Intent): IBinder? = null
    inner class MyConnectionListener : ConnectionLifeCycleListener {
        val app = application as Detector
        override fun onConnect() {
            app.isConnected = true
        }

        override fun onCleanUp() {
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.cancel(10)
            app.isConnected = false
        }

        override fun onDisconnect() {
            val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            mNotificationManager.cancel(10)
            app.isConnected = false
        }
    }

    inner class MyStreamAdapter : UserStreamAdapter() {
        override fun onStatus(x: Status) {
            if (!x.isRetweet && !x.text.contains(applicationContext.getString(R.string.tracking_tag))) {
                AppDatabase.getInstance(this@StreamingService).statusDao().insertStatus(StatusEntity(x, x.id))
            }
        }

        override fun onException(ex: Exception) {
            super.onException(ex)
            ex.printStackTrace()
        }

        private fun getBitmap(url_: String): Bitmap? {
            var connection: HttpURLConnection? = null
            var inputStream: InputStream? = null
            var bitmap: Bitmap? = null
            try {
                val url = URL(url_)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()
                inputStream = connection.inputStream

                bitmap = BitmapFactory.decodeStream(inputStream)
            } catch (exception: MalformedURLException) {

            } catch (exception: IOException) {

            } finally {
                if (connection != null) {
                    connection.disconnect()
                }
                try {
                    if (inputStream != null) {
                        inputStream.close()
                    }
                } catch (exception: IOException) {
                }
            }

            return bitmap
        }

        fun Save(stringURL: String, id: Long) {
            launch(UI) {
                val result = async(CommonPool) { getBitmap(stringURL) }.await()
                if (result != null) {
                    ByteArrayOutputStream().use {
                        result.compress(Bitmap.CompressFormat.JPEG, 100, it)
                        val byte: ByteArray = it.toByteArray()


                        // Realm.getDefaultInstance().use {  it.executeTransaction {
                        //     val statusObject= it.where(StatusObject::class.java).equalTo("statusId",id).findFirst() //     statusObject?.picture=byte
                        //  }  }
                    }
                }
            }

        }

        override fun onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {
            val preference = PreferenceManager.getDefaultSharedPreferences(applicationContext)
            //自分を含める
            val includeMe = preference.getBoolean("includeMe", true)
            //これが自分のツイート
            val isMyTweet = statusDeletionNotice.userId == id
            val s=AppDatabase.getInstance(this@StreamingService).statusDao().findById(statusDeletionNotice.statusId).status
            if (s != null) {
                //  自分のツイートでないか自分を含めかつ自分の
                if (statusDeletionNotice.userId != id || isMyTweet && includeMe) {
                    val string = s.user.screenName + "『" + getExpandedText(s) + "』" + preference.getString("tweetText", "") + getString(R.string.tracking_tag)
                    launch(UI) {
                        if (preference.getBoolean("direct_message", true)) {
                            async(CommonPool) {
                                twitter.directMessages().sendDirectMessage(id, string)
                            }.await()
                            Toast.makeText(this@StreamingService, "感知", Toast.LENGTH_SHORT).show()
                        } else {
                            async(CommonPool) {
                                twitter.updateStatus(string)
                            }
                        }

                    }


                }
            }
        }
    }

    override fun stopService(name: Intent?): Boolean {
        stream.clearListeners()
        try {
            thread {
                Runnable {
                    stream.cleanUp()
                    stream.shutdown()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        this.stopForeground(true)

        return super.stopService(name)
    }

    override fun onDestroy() {
        super.onDestroy()
        stream.clearListeners()
        try {
            thread {
                Runnable {
                    stream.cleanUp()
                    stream.shutdown()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        this.stopForeground(true)

    }


}

