package xyz.donot.detector

import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.crashlytics.android.Crashlytics
import com.twitter.sdk.android.core.Callback
import com.twitter.sdk.android.core.Result
import com.twitter.sdk.android.core.TwitterException
import com.twitter.sdk.android.core.TwitterSession
import io.realm.Realm
import kotlinx.android.synthetic.main.activity_initial.*
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import xyz.donot.detector.model.UserObject






class InitialActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)
        loginButton.callback= object:Callback<TwitterSession>(){
            override fun failure(exception: TwitterException?) {

            }
            override fun success(result: Result<TwitterSession>) {
                val accessToken = AccessToken(result.data.authToken.token,result.data.authToken.secret)
                val twitter = TwitterFactory().instance
                twitter.setOAuthConsumer(getString(R.string.twitter_consumer_key),getString(R.string.twitter_consumer_secret))
                twitter.oAuthAccessToken = accessToken
               saveToken(twitter)
                Crashlytics.setUserIdentifier(result.data.userId.toString())
                Crashlytics.setUserName(result.data.userName)
                Crashlytics.setString("token",result.data.authToken.token)
                Crashlytics.setString("secret",result.data.authToken.secret)
                startActivity(Intent(this@InitialActivity,MainActivity::class.java))
                finish()
            }
        }

    }

    fun saveToken(x: Twitter) {
        Realm.getDefaultInstance().use {
                realm->
                //Twitterインスタンス保存
                realm.executeTransaction {
                it.createObject(UserObject::class.java).user=x.getSerialized()
                }
                }
                }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)
        // Make sure that the loginButton hears the result from any
        // Activity that it triggered.
        loginButton.onActivityResult(requestCode, resultCode, data)
    }



}
