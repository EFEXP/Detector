package xyz.donot.detector

import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import com.google.firebase.analytics.FirebaseAnalytics
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import kotlinx.android.synthetic.main.activity_initial.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.User
import twitter4j.auth.AccessToken
import xyz.donot.detector.model.AppDatabase
import xyz.donot.detector.model.UserEntity

class InitialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)
        loginButton.callback = object : Callback<TwitterSession>() {
            override fun failure(exception: TwitterException?) {
            }
            override fun success(result: Result<TwitterSession>) {
                launch(UI) {
                    val accessToken = AccessToken(result.data.authToken.token, result.data.authToken.secret)
                    val twitter = TwitterFactory().instance
                    twitter.setOAuthConsumer(getString(R.string.twitter_consumer_key), getString(R.string.twitter_consumer_secret))
                    twitter.oAuthAccessToken = accessToken
                    async(CommonPool) { saveToken(twitter, result.data.userId) }.await()
                    async(CommonPool) { logUser(twitter.verifyCredentials()) }.await()
                    PreferenceManager.getDefaultSharedPreferences(this@InitialActivity).edit().putBoolean("initialize", false).apply()
                    startActivity(Intent(this@InitialActivity, MainActivity::class.java))
                    finish()
                }


            }
        }
    }
private fun logUser(user: User) {
    FirebaseAnalytics.getInstance(application).apply {
        setUserProperty("screenname", user.screenName)
        setUserId(user.id.toString())
    }.logEvent(FirebaseAnalytics.Event.LOGIN, Bundle().apply {
        putString(FirebaseAnalytics.Param.CONTENT, user.screenName)
        putString("UserName", user.name)
    })
}

fun saveToken(x: Twitter, id: Long) {
    AppDatabase.getInstance(this).userRoomDao().insertUser(UserEntity(x, id))
}

override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    // Make sure that the loginButton hears the result from any
    // Activity that it triggered.
    loginButton.onActivityResult(requestCode, resultCode, data)
}


}
