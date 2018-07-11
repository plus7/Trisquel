package net.tnose.app.trisquel;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.view.MenuItem;
import android.widget.Toast;

import static android.app.Activity.RESULT_OK;

/**
 * Created by user on 2018/07/12.
 */

public class GeneralPreferenceFragment extends PreferenceFragmentCompat implements AbstractDialogFragment.Callback {
    public final int REQCODE_DELETE_SUGGEST = 100;
    @Override
    public void onCreatePreferences(Bundle bundle, String s) {
        addPreferencesFromResource(R.xml.pref_general);
        setHasOptionsMenu(false);
        Preference pref = findPreference("reset_autocomplete");
        pref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                AbstractDialogFragment fragment = new YesNoDialogFragment.Builder()
                        .build(REQCODE_DELETE_SUGGEST);
                fragment.getArguments().putString("message", getString(R.string.msg_reset_autocomplete));
                fragment.showChildOn(GeneralPreferenceFragment.this, "dialog");
                return false;
            }
        });
    }

    protected OnFragmentInteractionListener mListener;
    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    public interface OnFragmentInteractionListener {}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            startActivity(new Intent(getActivity(), SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDialogResult(int requestCode, int resultCode, @Nullable Intent data) {
        switch (requestCode) {
            case REQCODE_DELETE_SUGGEST:
                if(resultCode == RESULT_OK){
                    SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                    SharedPreferences.Editor e = pref.edit();
                    e.putString("lens_manufacturer", "[]");
                    e.putString("camera_manufacturer", "[]");
                    e.putString("camera_mounts", "[]");
                    e.putString("film_manufacturer", "[]");
                    e.putString("film_brand", "{}");
                    e.apply();
                    Toast.makeText(getContext(), getString(R.string.msg_reset_autocomplete_done), Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

    @Override
    public void onDialogCancelled(int requestCode) {
        // onDialogResult(requestCode, DialogInterface.BUTTON_NEUTRAL, null);
    }
}
