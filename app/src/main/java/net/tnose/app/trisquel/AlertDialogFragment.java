package net.tnose.app.trisquel;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

public class AlertDialogFragment extends AbstractDialogFragment  {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString("title",""))
                .setMessage(getArguments().getString("message",""))
                .setPositiveButton(android.R.string.yes,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {}
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
            return new AlertDialogFragment();
        }
    }
}
