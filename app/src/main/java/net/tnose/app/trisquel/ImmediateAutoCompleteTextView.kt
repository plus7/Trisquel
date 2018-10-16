package net.tnose.app.trisquel

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent

import com.rengwuxian.materialedittext.MaterialAutoCompleteTextView

/**
 * Created by user on 2018/02/04.
 */

class ImmediateAutoCompleteTextView : MaterialAutoCompleteTextView {
    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs, defStyle) {
        this.threshold = 0
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        this.threshold = 0
    }

    constructor(context: Context) : super(context) {
        this.threshold = 0
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        showDropDown()
        return super.onTouchEvent(event)
    }
}
