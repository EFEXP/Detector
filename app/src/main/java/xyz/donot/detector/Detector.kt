package xyz.donot.detector

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.twitter.sdk.android.core.Twitter
import com.twitter.sdk.android.core.TwitterAuthConfig
import com.twitter.sdk.android.core.TwitterConfig
import io.realm.Realm
import io.realm.RealmConfiguration
import twitter4j.Status
import xyz.donot.detector.model.UserObject
import java.io.*
import java.text.BreakIterator
import java.util.regex.Pattern


class Detector : Application() {
    var isConnected = false
    override fun onCreate() {
        super.onCreate()
        val twitterConfig = TwitterConfig.Builder(this).twitterAuthConfig(TwitterAuthConfig(this.getString(R.string.twitter_consumer_key), this.getString(R.string.twitter_consumer_secret))).build()
        Twitter.initialize(twitterConfig)
        Realm.init(this)
        val config = RealmConfiguration.Builder()
                .deleteRealmIfMigrationNeeded()
                .build()
        Realm.setDefaultConfiguration(config)
        MobileAds.initialize(applicationContext, "ca-app-pub-1408046229935275~9337272741")
        isConnected= isServiceWorking()
    }
    private fun isServiceWorking(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Integer.MAX_VALUE).any { StreamingService::class.java.name == it.service.className }
    }
}

fun <T : Serializable> T.getSerialized(): ByteArray {
    ByteArrayOutputStream().use {
        ObjectOutputStream(it).use {
            it.writeObject(this)
        }
        return it.toByteArray()
    }
}

fun getTwitter(): twitter4j.Twitter = Realm.getDefaultInstance().use {
    return it.where(UserObject::class.java).findFirst()?.user!!.getDeserialized()
}

fun <T> ByteArray.getDeserialized(): T {
    @Suppress("UNCHECKED_CAST")
    return ObjectInputStream(ByteArrayInputStream(this)).readObject() as T
}


fun getMyId(): Long = Realm.getDefaultInstance().use {
    return it.where(UserObject::class.java).findFirst()!!.id
}



fun getExpandedText(status: Status): String {
    var text:String = status.text
    if (status.displayTextRangeStart>=0&&status.displayTextRangeEnd>=0) {
        text = emojiSubString(text, status.displayTextRangeStart, status.displayTextRangeEnd)
    }
    for   (url in status.urlEntities) {
        text =  Pattern.compile(url.url).matcher(text).replaceAll(url.expandedURL)
    }

    /* for (url in status.urlEntities) {
         text =  Pattern.compile(url.url).matcher(text).replaceAll(url.displayURL)
     }
     for (url in status.mediaEntities) {
         text = Pattern.compile(url.url).matcher(text).replaceAll("")
     }
     for (screen in status.userMentionEntities) {
         text = Pattern.compile("@"+screen.screenName).matcher(text).replaceAll("")
     }*/
    return text
}

private fun emojiSubString(target:String, startIndex: Int, endIndex: Int): String {
    val bi = BreakIterator.getCharacterInstance()
    bi.setText(target)
    val sb = StringBuffer()

    // 繰り返し用開始位置
    var start = bi.first()
    // 繰り返し用終了位置
    var end :Int
    // 文字数
    var count = 0
    // 文字の最後まで繰り返し
    while (bi.next() != BreakIterator.DONE) {
        end = bi.current()
        // 文字数カウントアップ
        count++
        // 引数の開始位置と終了位置の間に文字を取得する
        if (count >= startIndex + 1 && count <= endIndex) {
            sb.append(target.substring(start, end))
        }
        start = end
    }
    return sb.toString()
}
