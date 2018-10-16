package net.tnose.app.trisquel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.print.PrintManager
import android.support.v4.print.PrintHelper
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_print_preview.*
import kotlinx.android.synthetic.main.content_print_preview.*

class PrintPreviewActivity : AppCompatActivity() {
    var name: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_print_preview)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        webview.settings.builtInZoomControls = true
        webview.settings.useWideViewPort = true

        val id = intent.getIntExtra("filmroll", -1)

        val dao = TrisquelDao(this.applicationContext)
        dao.connection()
        val filmroll = dao.getFilmRoll(id)
        val photos = dao.getPhotosByFilmRollId(id)
        filmroll!!.photos = photos
        name = filmroll.name
        val sb = StringBuilder()
        sb.append("<!doctype><html><head>")
        sb.append("<title>${name}</title>")
        sb.append("</head><body>")

        sb.append("<h1>${name}</h1>")
        sb.append("<table>")
        sb.append("<tr><td>${getString(R.string.label_camera)}</td>      <td>${filmroll.camera.manufacturer} ${filmroll.camera.modelName}</td></tr>")
        sb.append("<tr><td>${getString(R.string.label_manufacturer)}</td> <td>${filmroll.manufacturer}</td></tr>")
        sb.append("<tr><td>${getString(R.string.label_brand)}</td>       <td>${filmroll.brand}</td></tr>")
        sb.append("<tr><td>${getString(R.string.label_iso)}</td>         <td>${filmroll.iso}</td></tr>")
        sb.append("</table>")

        for (p in photos) {
            sb.append("<h2>#${p.index}</h2>")
            sb.append("<table>")
            val lens = dao.getLens(p.lensid)
            sb.append("<tr><td>${getString(R.string.label_date)}</td>          <td>${p.date}</td></tr>")
            sb.append("<tr><td>${getString(R.string.label_lens)}</td>          <td>${lens!!.manufacturer} ${lens.modelName}</td></tr>")
            sb.append("<tr><td>${getString(R.string.label_aperture)}</td>      <td>${p.aperture}</td></tr>")
            sb.append("<tr><td>${getString(R.string.label_shutter_speed)}</td>  <td>${Util.doubleToStringShutterSpeed(p.shutterSpeed)}</td></tr>")
            sb.append("<tr><td>${getString(R.string.label_ttl_light_meter)}</td><td>${p.ttlLightMeter}</td></tr>")
            sb.append("<tr><td>${getString(R.string.label_location)}</td>      <td>${p.location}</td></tr>")
            sb.append("<tr><td>${getString(R.string.label_memo)}</td>          <td>${p.memo}</td></tr>")
            sb.append("</table>")
        }

        sb.append("</body></html>")
        dao.close()

        webview.loadDataWithBaseURL(null, sb.toString(), "text/html", "UTF8", null)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_printpreview, menu)
        return true
    }

    private fun printHtml(fileName: String) {
        if (PrintHelper.systemSupportsPrint()) {
            val adapter = webview.createPrintDocumentAdapter(name)
            val printManager: PrintManager = getSystemService(Context.PRINT_SERVICE) as PrintManager
            printManager.print(fileName, adapter, null)
        } else {
            Toast.makeText(this, getString(R.string.msg_no_printer_support), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val data: Intent
        when (item.itemId) {
            android.R.id.home -> {
                data = Intent()
                finish()
                return true
            }
            R.id.menu_print -> {
                printHtml("filmroll.pdf")
                return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        val data = Intent()
        setResult(Activity.RESULT_OK, data)
        super.onBackPressed()
    }
}
