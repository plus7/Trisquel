package net.tnose.app.trisquel

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import net.tnose.app.trisquel.ui.theme.TrisquelTheme

// Wrapper activity for legacy intents
class EditLensActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra("id", -1)

        setContent {
            TrisquelTheme {
                EditLensRoute(
                    id = id,
                    onSaveSuccess = {
                        setResult(RESULT_OK, Intent())
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_CANCELED, Intent())
                        finish()
                    }
                )
            }
        }
    }
}
