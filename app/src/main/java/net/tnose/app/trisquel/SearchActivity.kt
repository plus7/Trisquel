package net.tnose.app.trisquel

import android.os.Bundle
import android.support.v7.app.AppCompatActivity

class SearchActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
    }

    override fun finish() {
        super.finish()

        overridePendingTransition(R.anim.abc_fade_in, R.anim.abc_fade_out)
    }
}
