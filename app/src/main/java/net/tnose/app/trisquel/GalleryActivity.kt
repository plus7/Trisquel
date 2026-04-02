package net.tnose.app.trisquel

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.IntentCompat
import net.tnose.app.trisquel.ui.theme.TrisquelTheme

class GalleryActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val photo = IntentCompat.getParcelableExtra(intent, "photo", Photo::class.java)
        val favList = IntentCompat.getParcelableArrayListExtra(intent, "favList", Photo::class.java)

        setContent {
            TrisquelTheme {
                if (photo != null && favList != null) {
                    GalleryScreen(initialPhoto = photo, favList = favList)
                }
            }
        }
    }
}
