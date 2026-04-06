package net.tnose.app.trisquel

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import net.tnose.app.trisquel.ui.theme.TrisquelTheme

// Wrapper activity for legacy intents
class EditFilmRollActivity : AppCompatActivity() {
    private val addCameraLauncher = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult().let {
        registerForActivityResult(it) { result ->
            // If we needed to handle camera result, we could, but the ViewModel handles reloading automatically via Room flows.
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val id = intent.getIntExtra("id", 0)
        
        setContent {
            TrisquelTheme {
                EditFilmRollRoute(
                    id = id,
                    onCancel = {
                        setResult(RESULT_OK, Intent())
                        finish()
                    },
                    onNavigateToEditCamera = {
                        val nextIntent = Intent(this, EditCameraActivity::class.java)
                        nextIntent.putExtra("type", 0)
                        addCameraLauncher.launch(nextIntent)
                    }
                )
            }
        }
    }
}
