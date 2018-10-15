package net.tnose.app.trisquel;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;

import java.util.ArrayList;

/**
 * Created by user on 2018/06/24.
 */

public class CheckListDialogFragment extends AbstractDialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final String[] items = getArguments().getStringArray("items");
        ArrayList<Integer> checked_indices = getArguments().getIntegerArrayList("checked_indices");

        final boolean[] checkedItems = new boolean[items.length];
        for(int i = 0; i < checked_indices.size(); i++){
            checkedItems[checked_indices.get(i)] = true;
        }

        return new AlertDialog.Builder(getActivity())
                .setTitle(getArguments().getString("title",""))
                .setMultiChoiceItems(items, checkedItems, new DialogInterface.OnMultiChoiceClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which, boolean isChecked) {
                        checkedItems[which] = isChecked ;
                    }
                })
                .setPositiveButton(getArguments().getString("positive", getString(android.R.string.yes)),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Intent data = new Intent();
                                data.putExtra("id", getArguments().getInt("id"));
                                data.putExtra("items", getArguments().getStringArray("items"));
                                ArrayList<String> strArray = new ArrayList<>();
                                ArrayList<Integer> idxArray = new ArrayList<>();
                                ArrayList<Integer> tagArray = new ArrayList<>();
                                for(int i = 0; i < checkedItems.length; i++) {
                                    if(checkedItems[i]) {
                                        strArray.add(getArguments().getStringArray("items")[i]);
                                        idxArray.add(i);
                                        if(getArguments().getIntegerArrayList("tags") != null) {
                                            tagArray.add(getArguments().getIntegerArrayList("tags").get(i));
                                        }
                                    }
                                }
                                data.putExtra("checked_items", strArray);
                                data.putExtra("checked_indices", idxArray);
                                data.putExtra("checked_tags", tagArray);
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
            return new CheckListDialogFragment();
        }
    }
}
