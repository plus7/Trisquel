package net.tnose.app.trisquel;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;

import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView;

/**
 * Created by user on 2018/02/04.
 */

public class ImmediateAutoCompleteTextView extends MaterialAutoCompleteTextView {
    public ImmediateAutoCompleteTextView(Context context, AttributeSet attrs, int defStyle) {
            super(context, attrs, defStyle);
            this.setThreshold(0);
    }

    public ImmediateAutoCompleteTextView(Context context, AttributeSet attrs) {
            super(context, attrs);
            this.setThreshold(0);
    }

    public ImmediateAutoCompleteTextView(Context context) {
            super(context);
            this.setThreshold(0);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
            showDropDown();
            return super.onTouchEvent(event);
    }
}
