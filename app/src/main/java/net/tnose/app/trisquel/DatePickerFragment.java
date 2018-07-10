package net.tnose.app.trisquel;

/**
 * Based on https://github.com/nukka123/DialogFragmentDemo
 */

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.widget.DatePicker;

import java.util.Calendar;

public class DatePickerFragment extends AbstractDialogFragment implements DatePickerDialog.OnDateSetListener {

    public static final String EXTRA_YEAR = "year";
    public static final String EXTRA_MONTH = "month";
    public static final String EXTRA_DAY = "dayOfMonth";

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(getContext(),this, year, month, dayOfMonth);

        return datePickerDialog;
    }

    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
        Intent data = new Intent();
        data.putExtra(EXTRA_YEAR, year);
        data.putExtra(EXTRA_MONTH, month);
        data.putExtra(EXTRA_DAY, dayOfMonth);
        notifyDialogResult(DialogInterface.BUTTON_POSITIVE, data);
    }

    public static class Builder extends AbstractDialogFragment.Builder {
        @NonNull
        @Override
        protected AbstractDialogFragment build() {//build()から呼ぶとcheckArgumentsで死ぬと思う
            return new DatePickerFragment();
        }
    }
}