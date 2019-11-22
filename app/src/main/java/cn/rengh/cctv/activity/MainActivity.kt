package cn.rengh.cctv.activity

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import cn.rengh.cctv.R
import com.r.library.common.util.AppInfo
import com.r.library.common.util.LogUtils
import com.r.library.common.util.UIUtils
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.functions.Consumer
import io.reactivex.schedulers.Schedulers
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private var tvName: TextView? = null
    private var SHOW_TIME: Long = 1
    private var disposable: Disposable? = null

    private var mBackKeyPressed = false // 记录是否有首次按键

    private val mBackKeyRunnable =
        Runnable { mBackKeyPressed = false }
    private var weakHandler: MyHandler? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        UIUtils.setFullStateBar(this, true)
        setContentView(R.layout.activity_main)

        val appInfo = AppInfo(this)
        weakHandler = MyHandler(this)

        tvName = findViewById(R.id.tv_name)
        tvName?.text = appInfo.appName + appInfo.versionName

        val observable = Observable
            .timer(this.SHOW_TIME, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())

        val consumer = Consumer<Long> {
            startActivity(Intent(this, CCTVActivity::class.java))
        }

        disposable = observable.subscribe(consumer)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if (KeyEvent.KEYCODE_BACK == keyCode) {
            if (doubleClickBackToFinish()) {
                Toast.makeText(this, "连按两次【返回】键退出", Toast.LENGTH_LONG).show()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    override fun onStop() {
        super.onStop()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!this.disposable!!.isDisposed) {
            this.disposable!!.dispose()
            this.disposable = null
        }
    }

    private fun doubleClickBackToFinish(): Boolean {
        return if (!mBackKeyPressed) {
            mBackKeyPressed = true
            weakHandler!!.postDelayed(mBackKeyRunnable, 600)
            true
        } else {
            mBackKeyPressed = false
            weakHandler!!.removeCallbacks(mBackKeyRunnable)
            false
        }
    }

    private class MyHandler(activity: MainActivity) : Handler() {
        private val weakReference: WeakReference<MainActivity> = WeakReference(activity)
        override fun handleMessage(msg: Message) {
            val activity = weakReference.get()
            if (null == activity) {
                LogUtils.i("MyHandler", "activity is null.")
                return
            }
        }

    }
}
