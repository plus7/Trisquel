package net.tnose.app.trisquel

import android.app.Activity
import android.content.*
import android.graphics.Typeface
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import java.util.*

class EditPhotoListActivity : AppCompatActivity(), PhotoFragment.OnListFragmentInteractionListener, AbstractDialogFragment.Callback {
    internal val REQCODE_ADD_PHOTO = 100
    internal val REQCODE_EDIT_PHOTO = 101
    internal val REQCODE_EDIT_FILMROLL = 102

    private var toolbar: Toolbar? = null
    private var mFilmRoll: FilmRoll? = null
    private var photo_fragment: PhotoFragment? = null
    private var namelabel: TextView? = null
    private var cameralabel: TextView? = null
    private var brandlabel: TextView? = null

    private val filmRollText: String
        get() {
            val sb = StringBuilder()
            sb.append(mFilmRoll!!.name + "\n")
            if (mFilmRoll!!.manufacturer.length > 0) sb.append(getString(R.string.label_manufacturer) + ": " + mFilmRoll!!.manufacturer + "\n")
            if (mFilmRoll!!.brand.length > 0) sb.append(getString(R.string.label_brand) + ": " + mFilmRoll!!.brand + "\n")
            if (mFilmRoll!!.iso > 0) sb.append(getString(R.string.label_iso) + ": ")
            sb.append(mFilmRoll!!.iso)
            sb.append('\n')
            val c = mFilmRoll!!.camera
            sb.append(getString(R.string.label_camera) + ": " + c.manufacturer + " " + c.modelName + "\n")

            val dao = TrisquelDao(this)
            dao.connection()
            val ps = dao.getPhotosByFilmRollId(mFilmRoll!!.id)
            for (i in ps.indices) {
                val p = ps[i]
                val l = dao.getLens(p.lensid)
                sb.append("------[No. " + (p.index + 1) + "]------\n")
                sb.append(getString(R.string.label_date) + ": " + p.date + "\n")
                sb.append(getString(R.string.label_lens_name) + ": " + l!!.manufacturer + " " + l.modelName + "\n")
                if (p.aperture > 0) sb.append(getString(R.string.label_aperture) + ": " + p.aperture + "\n")
                if (p.shutterSpeed > 0) sb.append(getString(R.string.label_shutter_speed) + ": " + Util.doubleToStringShutterSpeed(p.shutterSpeed) + "\n")
                if (p.expCompensation != 0.0) sb.append(getString(R.string.label_exposure_compensation) + ": " + p.expCompensation + "\n")
                if (p.ttlLightMeter != 0.0) sb.append(getString(R.string.label_ttl_light_meter) + ": " + p.ttlLightMeter + "\n")
                if (p.location.length > 0) sb.append(getString(R.string.label_location) + ": " + p.location + "\n")
                if (p.latitude != 999.0 && p.longitude != 999.0) sb.append(getString(R.string.label_coordinate) + ": " + java.lang.Double.toString(p.latitude) + ", " + java.lang.Double.toString(p.longitude) + "\n")
                if (p.memo.length > 0) sb.append(getString(R.string.label_memo) + ": " + p.memo + "\n")
                if (p.accessories.size > 0) {
                    sb.append(getString(R.string.label_accessories) + ": ")
                    var first = true
                    for (a in p.accessories) {
                        if (!first) sb.append(", ")
                        sb.append(dao.getAccessory(a)!!.name)
                        first = false
                    }
                    sb.append("\n")
                }
            }
            dao.close()
            return sb.toString()
        }

    protected fun findViews() {
        namelabel = findViewById(R.id.label_name)
        cameralabel = findViewById(R.id.label_camera)
        brandlabel = findViewById(R.id.label_filmbrand)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_photo_list)
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        var id = -1
        val data = intent
        if (data != null) {
            id = data.getIntExtra("id", -1)
        }

        findViews()

        val dao = TrisquelDao(applicationContext)
        dao.connection()
        mFilmRoll = dao.getFilmRoll(id)
        dao.close()

        if (mFilmRoll!!.name.isEmpty()) {
            namelabel!!.setText(R.string.empty_name)
            namelabel!!.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC)
            setTitle(R.string.empty_name)
        } else {
            namelabel!!.text = mFilmRoll!!.name
            namelabel!!.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
            title = mFilmRoll!!.name
        }
        cameralabel!!.text = mFilmRoll!!.camera.manufacturer + " " + mFilmRoll!!.camera.modelName
        brandlabel!!.text = mFilmRoll!!.manufacturer + " " + mFilmRoll!!.brand
        toolbar!!.subtitle = mFilmRoll!!.camera.manufacturer + " " + mFilmRoll!!.camera.modelName + " / " +
                mFilmRoll!!.manufacturer + " " + mFilmRoll!!.brand

        val filmroll_layout = findViewById<LinearLayout>(R.id.layout_filmroll)
        filmroll_layout.setOnClickListener {
            val intent = Intent(application, EditFilmRollActivity::class.java)
            intent.putExtra("id", mFilmRoll!!.id)
            startActivityForResult(intent, REQCODE_EDIT_FILMROLL)
        }

        photo_fragment = PhotoFragment.newInstance(1, id)
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.container, photo_fragment)
        transaction.commit()

        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(application, EditPhotoActivity::class.java)
            intent.putExtra("filmroll", mFilmRoll!!.id)
            startActivityForResult(intent, REQCODE_ADD_PHOTO)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_filmroll, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val data: Intent
        when (item.itemId) {
            android.R.id.home -> {
                data = Intent()
                data.putExtra("filmroll", this.mFilmRoll!!.id)
                setResult(Activity.RESULT_OK, data)
                finish()
                return true
            }
            R.id.menu_edit_film -> {
                var intent = Intent(application, EditFilmRollActivity::class.java)
                intent.putExtra("id", mFilmRoll!!.id)
                startActivityForResult(intent, REQCODE_EDIT_FILMROLL)
                return true
            }
            R.id.menu_copy -> {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.primaryClip = ClipData.newPlainText("", filmRollText)
                Toast.makeText(this, getString(R.string.notify_copied), Toast.LENGTH_SHORT).show()
                return true
            }
            R.id.menu_export_pdf -> {
                intent = Intent(application, PrintPreviewActivity::class.java)
                intent.putExtra("filmroll", mFilmRoll!!.id)
                startActivity(intent)
                return true
            }
        }
        return false
    }

    override fun onBackPressed() {
        Log.d("onBackPressed", "java")
        val data = Intent()
        data.putExtra("filmroll", this.mFilmRoll!!.id)
        setResult(Activity.RESULT_OK, data)
        super.onBackPressed()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQCODE_ADD_PHOTO -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val p = Photo(
                        -1,
                        bundle!!.getInt("filmroll"),
                        bundle.getInt("index"),
                        bundle.getString("date")!!,
                        bundle.getInt("camera"),
                        bundle.getInt("lens"),
                        bundle.getDouble("focal_length"),
                        bundle.getDouble("aperture"),
                        bundle.getDouble("shutter_speed"),
                        bundle.getDouble("exp_compensation"),
                        bundle.getDouble("ttl_light_meter"),
                        bundle.getString("location")!!,
                        bundle.getDouble("latitude"),
                        bundle.getDouble("longitude"),
                        bundle.getString("memo")!!,
                        bundle.getString("accessories")!!)
                photo_fragment!!.insertPhoto(p)
            }
            REQCODE_EDIT_PHOTO -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                Log.d("ActivityResult: lens", Integer.toString(bundle!!.getInt("lens")))
                val p = Photo(
                        bundle.getInt("id"),
                        bundle.getInt("filmroll"),
                        bundle.getInt("index"),
                        bundle.getString("date")!!,
                        bundle.getInt("camera"),
                        bundle.getInt("lens"),
                        bundle.getDouble("focal_length"),
                        bundle.getDouble("aperture"),
                        bundle.getDouble("shutter_speed"),
                        bundle.getDouble("exp_compensation"),
                        bundle.getDouble("ttl_light_meter"),
                        bundle.getString("location")!!,
                        bundle.getDouble("latitude"),
                        bundle.getDouble("longitude"),
                        bundle.getString("memo")!!,
                        bundle.getString("accessories")!!)
                photo_fragment!!.updatePhoto(p)
            }
            REQCODE_EDIT_FILMROLL -> if (resultCode == Activity.RESULT_OK) {
                val bundle = data.extras
                val dao = TrisquelDao(this.applicationContext)
                dao.connection()
                val c = dao.getCamera(bundle!!.getInt("camera"))
                val f = FilmRoll(
                        bundle.getInt("id"),
                        bundle.getString("name")!!,
                        bundle.getString("created")!!,
                        Util.dateToStringUTC(Date()),
                        c!!,
                        bundle.getString("manufacturer")!!,
                        bundle.getString("brand")!!,
                        bundle.getInt("iso"),
                        36
                )

                if (f.name.isEmpty()) {
                    namelabel!!.setText(R.string.empty_name)
                    namelabel!!.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC)
                    setTitle(R.string.empty_name)
                } else {
                    namelabel!!.text = f.name
                    namelabel!!.setTypeface(Typeface.SANS_SERIF, Typeface.NORMAL)
                    title = f.name
                }
                cameralabel!!.text = f.camera.manufacturer + " " + f.camera.modelName
                brandlabel!!.text = f.manufacturer + " " + f.brand
                toolbar!!.subtitle = f.camera.manufacturer + " " + f.camera.modelName + " / " +
                        f.manufacturer + " " + f.brand
                //TODO:
                dao.updateFilmRoll(f)
                dao.close()
            } else if (resultCode == Activity.RESULT_CANCELED) {
            }
        }
    }

    override fun onListFragmentInteraction(item: Photo, isLong: Boolean) {
        if (isLong) {
            val fragment = SelectDialogFragment.Builder()
                    .build(200)
            fragment.arguments.putInt("id", item.id)
            fragment.arguments.putStringArray("items", arrayOf(getString(R.string.delete), getString(R.string.add_photo_above)))
            fragment.showOn(this, "dialog")
        } else {
            val intent = Intent(application, EditPhotoActivity::class.java)
            intent.putExtra("filmroll", mFilmRoll!!.id)
            intent.putExtra("id", item.id)
            intent.putExtra("index", item.index)
            startActivityForResult(intent, REQCODE_EDIT_PHOTO)
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            200 -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                if (data != null) {
                    val which: Int
                    val id: Int
                    val index: Int
                    which = data.getIntExtra("which", -1)
                    id = data.getIntExtra("id", -1)
                    val dao = TrisquelDao(this.applicationContext)
                    dao.connection()
                    val p = dao.getPhoto(id)
                    dao.close()
                    index = p!!.index
                    when (which) {
                        0 -> if (id != -1) photo_fragment!!.deletePhoto(id)
                        1 -> {
                            val intent = Intent(application, EditPhotoActivity::class.java)
                            intent.putExtra("filmroll", mFilmRoll!!.id)
                            intent.putExtra("index", index)
                            startActivityForResult(intent, REQCODE_ADD_PHOTO)
                        }
                    }
                    Log.d("PHOTOLIST_SELECTION", Integer.toString(which))
                }
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }
}
