package net.tnose.app.trisquel

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Process
import androidx.appcompat.app.AppCompatActivity

// https://github.com/hshiozawa/AndroidAppRestart
// Apache license 2.0
class RestartActivity : AppCompatActivity() {

    companion object{
        var EXTRA_MAIN_PID = "RestartActivity.main_pid"

        fun createIntent(context: Context): Intent {
            val intent = Intent()
            intent.setClassName(context.packageName, RestartActivity::class.java.name)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            // メインプロセスの PID を Intent に保存しておく
            intent.putExtra(EXTRA_MAIN_PID, Process.myPid())
            return intent
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. メインプロセスを Kill する
        val intent = intent
        val mainPid = intent.getIntExtra(EXTRA_MAIN_PID, -1)
        Process.killProcess(mainPid)

        // 2. MainActivity を再起動する
        val context = applicationContext
        val restartIntent = Intent(Intent.ACTION_MAIN)
        restartIntent.setClassName(context.packageName, MainActivity::class.java.name)
        restartIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        context.startActivity(restartIntent)

        // 3. RestartActivity を終了する
        finish()
        Process.killProcess(Process.myPid())
    }
}