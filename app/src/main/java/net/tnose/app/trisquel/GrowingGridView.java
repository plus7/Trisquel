package net.tnose.app.trisquel;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.GridView;

/**
 * Created by user on 2018/07/01.
 */

public class GrowingGridView extends GridView {
    public GrowingGridView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public GrowingGridView(Context context) {
        super(context);
    }

    public GrowingGridView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int expandSpec = MeasureSpec.makeMeasureSpec(Integer.MAX_VALUE >> 2,
                MeasureSpec.AT_MOST);
        super.onMeasure(widthMeasureSpec, expandSpec);
    }
}
