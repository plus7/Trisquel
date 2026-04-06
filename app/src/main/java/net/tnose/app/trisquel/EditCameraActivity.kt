package net.tnose.app.trisquel

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import net.tnose.app.trisquel.ui.theme.TrisquelTheme

// Wrapper activity for legacy intents
class EditCameraActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra("id", -1)
        val type = intent.getIntExtra("type", 0)
        
        setContent {
            TrisquelTheme {
                EditCameraRoute(
                    id = id,
                    type = type,
                    onSaveSuccess = {
                        setResult(RESULT_OK, Intent())
                        finish()
                    },
                    onCancel = {
                        setResult(RESULT_OK, Intent())
                        finish()
                    }
                )
            }
        }
    }
}
