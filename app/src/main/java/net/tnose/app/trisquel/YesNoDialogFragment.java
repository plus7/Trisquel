package net.tnose.app.trisquel;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

public class YesNoDialogFragment extends AbstractDialogFragment  {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString("title",""))
                .setMessage(getArguments().getString("message",""))
                .setPositiveButton(getArguments().getString("positive", getString(android.R.string.yes)),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent data = new Intent();
                                data.putExtra("id", getArguments().getInt("id"));
                                notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data);
                            }
                        })
                .setNegativeButton(getArguments().getString("negative", getString(android.R.string.cancel)),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent data = new Intent();
                                data.putExtra("id", getArguments().getInt("id"));
                                notifyDialogResult(DialogInterface.BUTTON_NEGATIVE, data);
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
            return new YesNoDialogFragment();
        }
    }
}
