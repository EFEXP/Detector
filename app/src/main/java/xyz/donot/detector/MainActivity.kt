package xyz.donot.detector

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import io.realm.Realm
import kotlinx.android.synthetic.main.content_main.*
import xyz.donot.detector.model.UserObject



class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val preference=PreferenceManager.getDefaultSharedPreferences(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val toolbar = findViewById(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        if (Realm.getDefaultInstance().where(UserObject::class.java).findAll().isEmpty()) {
            startActivity(Intent(this@MainActivity,InitialActivity::class.java))
            this.finish()
        }
        else{
            val adRequest = AdRequest.Builder().build()
            adView.loadAd(adRequest)
            startService(Intent(this@MainActivity,StreamService::class.java))
           includeMe.isChecked= preference.getBoolean("includeMe",true)
            toMe_checkbox.isChecked= preference.getBoolean("toMe",true)
            power_text.text = preference.getString("tweetText","")

        apply_button.setOnClickListener {
            preference.edit().putBoolean("includeMe",includeMe.isChecked).apply()
            preference.edit().putBoolean("toMe",toMe_checkbox.isChecked).apply()
            preference.edit().putString("tweetText",tweet_text.text.toString()).apply()
            power_text.text = preference.getString("tweetText","")
            tweet_text.text.clear()
            Toast.makeText(this@MainActivity,"保存しました",Toast.LENGTH_LONG).show()
        }

        }


    }

    override fun onDestroy() {
        super.onDestroy()
        val mNotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        mNotificationManager.cancel(0)
    }


}
