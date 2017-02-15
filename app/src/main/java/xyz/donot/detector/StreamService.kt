package xyz.donot.detector

import android.app.IntentService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.preference.PreferenceManager
import android.support.v4.app.NotificationCompat
import io.realm.Realm
import twitter4j.*
import xyz.donot.detector.model.StatusObject
import xyz.donot.detector.model.UserObject


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
            val realm=Realm.getDefaultInstance()
            if(x.retweetedStatus==null){
                    realm.executeTransaction {
                        val s: StatusObject = it.createObject(StatusObject::class.java, x.id)
                        s.status = x.getSerialized()

                }}
        }

        override fun onException(ex: Exception) {
            super.onException(ex)
            ex.printStackTrace()
        }

        override fun onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {
            val preference=PreferenceManager.getDefaultSharedPreferences(applicationContext)
            val myId =Realm.getDefaultInstance().where(UserObject::class.java).findFirst().user!!.getDeserialized<Twitter>().id
            //自分を含める
            val includeMe=preference.getBoolean("includeMe",true)
            //これが自分のツイート
            val isMyTweet=statusDeletionNotice.userId== myId

    val s=   Realm.getDefaultInstance().where(StatusObject::class.java).equalTo("statusId",statusDeletionNotice.statusId).findFirst()
            if(s != null){
              //  自分のツイートでないか自分を含めかつ自分の
                if(statusDeletionNotice.userId!= myId||isMyTweet&&includeMe){
               val status=s.status!!.getDeserialized<Status>()
                    var string= status.user.screenName+"『"+status.text+"』"+preference.getString("tweetText","")+" #黒歴史"
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
}
