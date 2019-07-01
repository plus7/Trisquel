package net.tnose.app.trisquel

import android.content.res.Resources.NotFoundException
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_license.*
import kotlinx.android.synthetic.main.content_license.*
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

class LicenseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_license)
        setSupportActionBar(toolbar)
        try {
            licenseview.setLicenses(R.xml.licenses)
        } catch (e1: NotFoundException) {
        } catch (e1: XmlPullParserException) {
        } catch (e1: IOException) {
        }
    }

}
