package net.tnose.app.trisquel;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

/**
 * Created by user on 2018/06/24.
 */

public class SelectDialogFragment extends AbstractDialogFragment {
    //final String[] items = {"item_0", "item_1", "item_2"};

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String[] items = getArguments().getStringArray("items");
        return new AlertDialog.Builder(getActivity())
                //.setTitle(getArguments().getString("title",""))
                .setItems(items, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Intent data = new Intent();
                        data.putExtra("id", getArguments().getInt("id"));
                        data.putExtra("which", which);
                        notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data);
                    }
                })
                .setCancelable(true)
                .create();
    }

    @Override
    public void onPause() {
        super.onPause();
        dismiss();
    }

    public static class Builder extends AbstractDialogFragment.Builder {
        @NonNull
        @Override
        protected AbstractDialogFragment build() {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return new SelectDialogFragment();
        }
    }
}
