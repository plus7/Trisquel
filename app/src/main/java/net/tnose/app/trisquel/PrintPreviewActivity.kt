package net.tnose.app.trisquel

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.print.PrintManager
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.print.PrintHelper
import net.tnose.app.trisquel.databinding.ActivityPrintPreviewBinding

class PrintPreviewActivity : AppCompatActivity() {
    var name: String = ""
    private lateinit var binding: ActivityPrintPreviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPrintPreviewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.includeContentPrintPreview.webview.settings.builtInZoomControls = true
        binding.includeContentPrintPreview.webview.settings.useWideViewPort = true

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
        sb.append("<style>" +
                "div.filminfo {\n" +
                "  margin-bottom: 1em;\n" +
                "}\n" +
                "h2 {\n" +
                "    margin:0px;\n" +
                "    padding-top: 0.83em;\n" +
                "    padding-bottom: 0.83em;\n" +
                "}\n" +
                "table {\n" +
                "  border-collapse: collapse;\n" +
                "}\n" +
                "table th, table td {\n" +
                "  border: solid 1px black;\n" +
                "}\n" +
                "div.photo {\n" +
                "   page-break-inside: avoid;\n"+
                "}\n"+
                "div.content {\n" +
                "   column-count: 2;\n"+
                "}\n"+
                "</style>")
        //
//         +
        sb.append("</head><body>")

        sb.append("<div class=\"filminfo\"><h1>${name}</h1>")
        sb.append("<table width=\"100%\">")
        sb.append("<tr><td>${getString(R.string.label_camera)}</td>      <td>${filmroll.camera.manufacturer} ${filmroll.camera.modelName}</td></tr>")
        sb.append("<tr><td>${getString(R.string.label_manufacturer)}</td> <td>${filmroll.manufacturer}</td></tr>")
        sb.append("<tr><td>${getString(R.string.label_brand)}</td>       <td>${filmroll.brand}</td></tr>")
        sb.append("<tr><td>${getString(R.string.label_iso)}</td>         <td>${filmroll.iso}</td></tr>")
        sb.append("</table></div><div class=\"content\">")

        for (p in photos) {
            val accessories = p.accessories.map{a -> dao.getAccessory(a)!!.name }.joinToString(", ")
            sb.append("<div class=\"photo\">")
            sb.append("<h2>#${p.frameIndex+1}</h2>")
            sb.append("<table width=\"100%\">")
            val lens = dao.getLens(p.lensid)
            sb.append("<tr><td>${getString(R.string.label_date)}</td>          <td>${p.date}</td></tr>")
            sb.append("<tr><td>${getString(R.string.label_lens)}</td>          <td>${lens!!.manufacturer} ${lens.modelName}</td></tr>")
            if(p.aperture > 0.0)
                sb.append("<tr><td>${getString(R.string.label_aperture)}</td>      <td>${p.aperture}</td></tr>")
            if(p.shutterSpeed > 0.0)
                sb.append("<tr><td>${getString(R.string.label_shutter_speed)}</td>  <td>${Util.doubleToStringShutterSpeed(p.shutterSpeed)}</td></tr>")
            sb.append("<tr><td>${getString(R.string.label_ttl_light_meter)}</td><td>${p.ttlLightMeter}</td></tr>")
            if(p.location.isNotEmpty())
                sb.append("<tr><td>${getString(R.string.label_location)}</td>      <td>${p.location}</td></tr>")
            if(p.isValidLatLng)
                sb.append("<tr><td>${getString(R.string.label_coordinate)}</td>      <td>${p.latitude}, ${p.longitude}</td></tr>")
            if(p.memo.isNotEmpty())
                sb.append("<tr><td>${getString(R.string.label_memo)}</td>          <td>${p.memo}</td></tr>")
            if(accessories.isNotEmpty())
                sb.append("<tr><td>${getString(R.string.label_accessories)}</td>    <td>${accessories}</td></tr>")
            sb.append("</table>")
            sb.append("</div>")
        }

        sb.append("</div></body></html>")
        dao.close()

        binding.includeContentPrintPreview.webview.loadDataWithBaseURL(null, sb.toString(), "text/html", "UTF8", null)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_printpreview, menu)
        return true
    }

    private fun printHtml(fileName: String) {
        if (PrintHelper.systemSupportsPrint()) {
            val adapter = binding.includeContentPrintPreview.webview.createPrintDocumentAdapter(name)
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
