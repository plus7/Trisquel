package net.tnose.app.trisquel

import android.app.Activity
import android.content.*
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.appcompat.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_tabbed.*
import java.util.*

class TabbedActivity : AppCompatActivity(),
        AbstractDialogFragment.Callback,
        ShootingInfoEditFragment.OnFragmentInteractionListener,
        TagEditFragment.OnFragmentInteractionListener {

    val DIALOG_SAVE_OR_DISCARD = 100
    val DIALOG_CONTINUE_OR_DISCARD = 101

    private var mSectionsPagerAdapter: SectionsPagerAdapter? = null
    private var mShootingInfoEditFragment: ShootingInfoEditFragment? = null
    private var mTagEditFragment: TagEditFragment? = null
    private var mId: Int = -1
    private var mFrameIndex: Int = -1
    private var mFilmRollId: Int = -1

    override fun onFragmentInteraction(uri: Uri){

    }

    override fun onFragmentAttached(f: ShootingInfoEditFragment){
        mShootingInfoEditFragment = f
    }

    override fun onFragmentAttached(f: TagEditFragment){
        mTagEditFragment = f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_tabbed)
        setSupportActionBar(toolbar)
        val actionBar = supportActionBar
        actionBar!!.setDisplayHomeAsUpEnabled(true)

        val data = intent
        mId = data.getIntExtra("id", -1)
        mFrameIndex = data.getIntExtra("frameIndex", -1)
        mFilmRollId = data.getIntExtra("filmroll", -1)

        mSectionsPagerAdapter = SectionsPagerAdapter(supportFragmentManager)
        container.offscreenPageLimit = 2
        container.adapter = mSectionsPagerAdapter
        tabLayout.setupWithViewPager(container)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_photo, menu)
        menu.findItem(R.id.menu_save)?.isEnabled = mShootingInfoEditFragment?.canSave() ?: false
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val sf = mShootingInfoEditFragment
        val tf = mTagEditFragment
        when (item.itemId) {
            android.R.id.home -> {
                val sf_dirty = sf?.isDirty ?: false
                val tf_dirty = tf?.isDirty ?: false
                if (!(sf_dirty || tf_dirty)) {
                    setResult(Activity.RESULT_CANCELED, Intent())
                    finish()
                } else {
                    if (sf?.canSave() == true) {
                        val fragment = YesNoDialogFragment.Builder()
                                .build(DIALOG_SAVE_OR_DISCARD)
                        fragment.arguments?.putString("message", getString(R.string.msg_save_or_discard_data))
                        fragment.arguments?.putString("positive", getString(R.string.save))
                        fragment.arguments?.putString("negative", getString(R.string.discard))
                        fragment.showOn(this, "dialog")
                    } else {
                        val fragment = YesNoDialogFragment.Builder()
                                .build(DIALOG_CONTINUE_OR_DISCARD)
                        fragment.arguments?.putString("message", getString(R.string.msg_continue_editing_or_discard_data))
                        fragment.arguments?.putString("positive", getString(R.string.continue_editing))
                        fragment.arguments?.putString("negative", getString(R.string.discard))
                        fragment.showOn(this, "dialog")
                    }
                }
                return true
            }
            R.id.menu_save -> {
                val result = Intent()
                result.putExtra("photo", sf?.newphoto)
                val tags = ArrayList<String>()
                if(tf != null) for((i,v) in tf.checkState.withIndex()){
                    if(v) tags.add(tf.allTags[i])
                }
                result.putExtra("tags", tags)
                setResult(Activity.RESULT_OK, result)
                finish()
                return true
            }
            R.id.menu_copy -> {
                val s = sf?.photoText ?: ""
                if (s.isEmpty()) {
                    return true
                }
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.primaryClip = ClipData.newPlainText("", s)
                Toast.makeText(this, getString(R.string.notify_copied), Toast.LENGTH_SHORT).show()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onBackPressed() {
        val sf_dirty = mShootingInfoEditFragment?.isDirty ?: false
        val tf_dirty = mTagEditFragment?.isDirty ?: false
        if (sf_dirty || tf_dirty) {
            if (mShootingInfoEditFragment?.canSave() == true) {
                val fragment = YesNoDialogFragment.Builder()
                        .build(DIALOG_SAVE_OR_DISCARD)
                fragment.arguments?.putString("message", getString(R.string.msg_save_or_discard_data))
                fragment.arguments?.putString("positive", getString(R.string.save))
                fragment.arguments?.putString("negative", getString(R.string.discard))
                fragment.showOn(this, "dialog")
            } else {
                val fragment = YesNoDialogFragment.Builder()
                        .build(DIALOG_CONTINUE_OR_DISCARD)
                fragment.arguments?.putString("message", getString(R.string.msg_continue_editing_or_discard_data))
                fragment.arguments?.putString("positive", getString(R.string.continue_editing))
                fragment.arguments?.putString("negative", getString(R.string.discard))
                fragment.showOn(this, "dialog")
            }
        } else {
            setResult(Activity.RESULT_CANCELED, Intent())
            super.onBackPressed()
        }
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            DIALOG_SAVE_OR_DISCARD -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                val result = Intent()
                result.putExtra("photo", mShootingInfoEditFragment?.newphoto)
                val tags = ArrayList<String>()
                val tf = mTagEditFragment
                if(tf != null) for((i,v) in tf.checkState.withIndex()){
                    if(v) tags.add(tf.allTags[i])
                }
                result.putExtra("tags", tags)
                setResult(Activity.RESULT_OK, result)
                finish()
            } else if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
            DIALOG_CONTINUE_OR_DISCARD -> if (resultCode == DialogInterface.BUTTON_POSITIVE) {
                /* do nothing and continue editing */
            } else if (resultCode == DialogInterface.BUTTON_NEGATIVE) {
                setResult(Activity.RESULT_CANCELED, Intent())
                finish()
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }

    inner class SectionsPagerAdapter(fm: androidx.fragment.app.FragmentManager) : androidx.fragment.app.FragmentPagerAdapter(fm) {

        override fun getItem(position: Int): androidx.fragment.app.Fragment {
            return when(position){
                0 -> ShootingInfoEditFragment.newInstance(mFilmRollId, mId, mFrameIndex)
                else -> TagEditFragment.newInstance(mFilmRollId, mId)
            }
        }

        override fun getPageTitle(position: Int): CharSequence? {
            return when(position){
                0 -> "基本情報"
                else -> "タグ"
            }
        }

        override fun getCount(): Int {
            return 2
        }
    }
}
