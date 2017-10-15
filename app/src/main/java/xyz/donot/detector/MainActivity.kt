package xyz.donot.detector

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import com.google.android.gms.ads.AdRequest
import com.kobakei.ratethisapp.RateThisApp
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.android.UI
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch
import xyz.donot.detector.model.AppDatabase


class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        val dao = AppDatabase.getInstance(this)
        val preference = PreferenceManager.getDefaultSharedPreferences(this)
        super.onCreate(savedInstanceState)
        if (preference.getBoolean("initialize", true)) {
            startActivity(Intent(this@MainActivity, InitialActivity::class.java))
            this.finish()
        } else {
            setContentView(R.layout.activity_main)
            RateThisApp.onCreate(this)
            RateThisApp.showRateDialogIfNeeded(this)
            RateThisApp.init(RateThisApp.Config().also {
                it.setTitle(R.string.rate)
                it.setMessage(R.string.my_own_message)
                it.setUrl(getString(R.string.store_url))
            })
            setSupportActionBar(toolbar)
            switchButtontxt()
            adView.loadAd(AdRequest.Builder().build())
            includeMe.isChecked = preference.getBoolean("includeMe", true)
            message_checkbox.isChecked = preference.getBoolean("direct_message", true)
            power_text.text = preference.getString("tweetText", "")
            delete_button.setOnClickListener {
                launch(UI) {
                    async(CommonPool) { dao.statusDao().deleteAll() }.await()
                    Toast.makeText(this@MainActivity, "削除しました", Toast.LENGTH_SHORT).show()
                }
            }
            message_checkbox.setOnCheckedChangeListener({ x, b ->
                preference.edit().putBoolean("direct_message", b).apply()
            })
            includeMe.setOnCheckedChangeListener({ _, b ->
                preference.edit().putBoolean("includeMe", b).apply()
            })

            apply_button.setOnClickListener {
                preference.edit().putString("tweetText", tweet_text.text.toString()).apply()
                power_text.text = preference.getString("tweetText", "")
                tweet_text.text.clear()
                Toast.makeText(this@MainActivity, "保存しました", Toast.LENGTH_LONG).show()
            }


            start_service.setOnClickListener {
                val app = application as Detector
                if (app.isConnected) {
                    stopService(Intent(this@MainActivity, StreamingService::class.java))
                    app.isConnected = false
                } else {
                    if (Build.VERSION.SDK_INT >= 26) {
                        startForegroundService(Intent(this@MainActivity, StreamingService::class.java))
                    } else {
                        startService(Intent(this@MainActivity, StreamingService::class.java))
                    }
                    app.isConnected = true
                }
                switchButtontxt()
            }
        }
    }

    private fun switchButtontxt() {
        val app = application as Detector
        if (app.isConnected) {
            start_service.text = "感知停止"
        } else {
            start_service.text = "感知開始"
        }
    }


}
