package net.tnose.app.trisquel

import android.content.Context
import androidx.viewpager.widget.ViewPager
import android.util.AttributeSet
import android.view.View
import it.sephiroth.android.library.imagezoom.ImageViewTouch

class CustomViewPager(context: Context, attrs: AttributeSet) : androidx.viewpager.widget.ViewPager(context, attrs) {

    override fun canScroll(v: View, checkV: Boolean, dx: Int, x: Int, y: Int): Boolean {
        return if (v is ImageViewTouch) v.canScroll(dx) else super.canScroll(v, checkV, dx, x, y)
    }
}
