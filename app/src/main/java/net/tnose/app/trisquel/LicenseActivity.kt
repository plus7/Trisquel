package net.tnose.app.trisquel

import android.content.res.Resources.NotFoundException
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.tnose.app.trisquel.databinding.ActivityLicenseBinding
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

class LicenseActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLicenseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLicenseBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        try {
            binding.includeContentLicense.licenseview.setLicenses(R.xml.licenses)
        } catch (e1: NotFoundException) {
        } catch (e1: XmlPullParserException) {
        } catch (e1: IOException) {
        }
    }

}
