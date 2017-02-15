package xyz.donot.detector

import android.app.Application
import android.os.Handler
import android.os.Looper
import com.crashlytics.android.Crashlytics
import com.google.android.gms.ads.MobileAds
import com.twitter.sdk.android.Twitter
import com.twitter.sdk.android.core.TwitterAuthConfig
import io.fabric.sdk.android.Fabric
import io.realm.Realm
import xyz.donot.detector.model.UserObject
import java.io.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future




class Detector :Application(){
    override fun onCreate() {
        super.onCreate()
        val authConfig = TwitterAuthConfig(this.getString(R.string.twitter_consumer_key),this.getString(R.string.twitter_consumer_secret))
        Fabric.with(this, Twitter(authConfig), Crashlytics())
        Realm.init(this@Detector)
        MobileAds.initialize(applicationContext, "ca-app-pub-1408046229935275~9337272741")
    }
}
private val uiHandler = Handler(Looper.getMainLooper())
fun<T: Serializable> T.getSerialized():ByteArray{
    ByteArrayOutputStream().use {
        val out = ObjectOutputStream(it)
        out.writeObject(this)
        val bytes = it.toByteArray()
        out.close()
        return bytes
    }
}


fun<T> ByteArray.getDeserialized():T{
    @Suppress("UNCHECKED_CAST")
    return ObjectInputStream(ByteArrayInputStream(this)).readObject()as T
}
fun getTwitter():twitter4j.Twitter{
 return  Realm.getDefaultInstance().where(UserObject::class.java).findFirst().user!!.getDeserialized()
}
fun mainThread(runnable: () -> Unit) {
    uiHandler.post(runnable)
}
fun async(runnable: () -> Unit) {
    Thread(runnable).start()
}

fun async(runnable: () -> Unit, executor: ExecutorService): Future<out Any?> {
    return executor.submit(runnable)
}
