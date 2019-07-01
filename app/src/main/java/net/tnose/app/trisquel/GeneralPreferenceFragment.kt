package net.tnose.app.trisquel

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import android.view.MenuItem
import android.widget.Toast

/**
 * Created by user on 2018/07/12.
 */

class GeneralPreferenceFragment : PreferenceFragmentCompat(), AbstractDialogFragment.Callback {
    val REQCODE_DELETE_SUGGEST = 100

    protected var mListener: OnFragmentInteractionListener? = null
    override fun onCreatePreferences(bundle: Bundle?, s: String?) {
        addPreferencesFromResource(R.xml.pref_general)
        setHasOptionsMenu(false)
        val pref = findPreference("reset_autocomplete")
        pref.onPreferenceClickListener = Preference.OnPreferenceClickListener {
            val fragment = YesNoDialogFragment.Builder()
                    .build(REQCODE_DELETE_SUGGEST)
            fragment.arguments?.putString("message", getString(R.string.msg_reset_autocomplete))
            fragment.showChildOn(this@GeneralPreferenceFragment, "dialog")
            false
        }
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnFragmentInteractionListener

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val id = item!!.itemId
        if (id == android.R.id.home) {
            startActivity(Intent(activity, SettingsActivity::class.java))
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent) {
        when (requestCode) {
            REQCODE_DELETE_SUGGEST -> if (resultCode == RESULT_OK) {
                val pref = PreferenceManager.getDefaultSharedPreferences(activity)
                val e = pref.edit()
                e.putString("lens_manufacturer", "[]")
                e.putString("camera_manufacturer", "[]")
                e.putString("camera_mounts", "[]")
                e.putString("film_manufacturer", "[]")
                e.putString("film_brand", "{}")
                e.apply()
                Toast.makeText(context, getString(R.string.msg_reset_autocomplete_done), Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDialogCancelled(requestCode: Int) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }
}
