package xyz.donot.detector

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import io.realm.Realm
import twitter4j.*
import xyz.donot.detector.model.StatusObject
import xyz.donot.detector.model.UserObject
import xyz.donot.detector.model.getImageUrls
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class StreamService : IntentService("StreamService") {
    val twitter=getTwitter()

    override fun onHandleIntent(intent: Intent?) {
        val stream = TwitterStreamFactory().getInstance(twitter.authorization)
        StreamCreateUtil.addStatusListener(stream,MyStreamAdapter())
        stream.user()
        val mNotification = NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.tw__ic_logo_default)
                .setContentTitle("Streaming")
                .setAutoCancel(false)
                .setContentText("ストリームは正常に稼働中です。")
                .build()
       // mNotification.flags= Notification.FLAG_NO_CLEAR
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        mNotificationManager.notify(0, mNotification )
    }

    override fun onDestroy() {
        super.onDestroy()

    }
    inner class MyStreamAdapter: UserStreamAdapter(){

        override fun onStatus(x: Status) {

            if(x.retweetedStatus==null){
                val realm=Realm.getDefaultInstance()
                val statusMediaIds=getImageUrls(x)
                    realm.executeTransaction {
                        val s: StatusObject = it.createObject(StatusObject::class.java, x.id)
                        s.status = x.getSerialized()
                }
                if(statusMediaIds.isNotEmpty()){
                    statusMediaIds.forEach { Save(it,x.id) }
                }
            }

        }

        override fun onException(ex: Exception) {
            super.onException(ex)
            ex.printStackTrace()
        }

        override fun onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {
            val preference=PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val myId =Realm.getDefaultInstance().where(UserObject::class.java).findFirst()?.user!!.getDeserialized<Twitter>().id
            //自分を含める
            val includeMe=preference.getBoolean("includeMe",true)
            //これが自分のツイート
            val isMyTweet=statusDeletionNotice.userId== myId

    val s=   Realm.getDefaultInstance().where(StatusObject::class.java).equalTo("statusId",statusDeletionNotice.statusId).findFirst()
            if(s != null){
              //  自分のツイートでないか自分を含めかつ自分の
                if(statusDeletionNotice.userId!= myId||isMyTweet&&includeMe){
               val status=s.status!!.getDeserialized<Status>()
                    var string= status.user.screenName+"『"+status.text+"』"+preference.getString("tweetText","")+" #ツイ消し感知"
                    if (preference.getBoolean("toMe",true)){
                       string="@null "+string
                    }
                twitter.updateStatus(string)
            }}
        }

        override fun onFavorite(source: User, target: User, favoritedStatus: Status) {
            super.onFavorite(source, target, favoritedStatus)

        }
    }
    fun Save(stringURL:String,id:Long){
     val a=  object: AsyncTask< String, Any,Bitmap?>(){
           override fun doInBackground(vararg p0: String?): Bitmap? {
 var connection:    HttpURLConnection? = null
               var  inputStream: InputStream? = null
               var bitmap: Bitmap?  = null

               try{

       val url: URL =URL(p0[0].toString())
        connection =url.openConnection() as HttpURLConnection
                   connection.requestMethod = "GET"
        connection.connect()
        inputStream = connection.inputStream

        bitmap = BitmapFactory.decodeStream(inputStream)
    }catch (exception: MalformedURLException){

    }catch ( exception:IOException){

    }finally {
        if (connection != null){
            connection.disconnect()
        }
        try{
            if (inputStream != null){
                inputStream.close()
            }
        }catch (exception:IOException){
        }
    }

    return bitmap

           }

           override fun onPostExecute(result: Bitmap?) {
               if (result!=null){
               ByteArrayOutputStream().use {
                  result.compress(Bitmap.CompressFormat.JPEG, 100,it)
                   val byte :ByteArray= it.toByteArray()

               Realm.getDefaultInstance().executeTransaction {
                  val statusObject= it.where(StatusObject::class.java).equalTo("statusId",id).findFirst()
                  statusObject?.picture=byte
               }  }
           }}
       }

        a.execute(stringURL)
    }
}
