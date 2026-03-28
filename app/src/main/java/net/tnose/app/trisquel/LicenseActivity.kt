package net.tnose.app.trisquel

import android.content.res.Resources.NotFoundException
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import net.tnose.app.trisquel.databinding.ActivityLicenseBinding
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

class LicenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLicenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.license_root)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }
        setSupportActionBar(binding.toolbar)
        try {
            binding.includeContentLicense.licenseview.setLicenses(R.xml.licenses)
        } catch (e1: NotFoundException) {
        } catch (e1: XmlPullParserException) {
        } catch (e1: IOException) {
        }
    }

}
