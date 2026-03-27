package net.tnose.app.trisquel

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ListView
import androidx.appcompat.widget.AppCompatAutoCompleteTextView

// Based on https://github.com/Lesilva/BetterSpinner

class Material3Spinner : AppCompatAutoCompleteTextView, AdapterView.OnItemClickListener {

    private var mPosition = ListView.INVALID_POSITION
    constructor(context: Context) : super(context){
        onItemClickListener = this
    }

    constructor(context: Context, attr: AttributeSet?) : super(context, attr){
        onItemClickListener = this
    }

    constructor(context: Context, attr: AttributeSet?, arg2: Int) : super(context, attr, arg2){
        onItemClickListener = this
    }

    var position: Int
        get() = mPosition
        set(pos) {
            mPosition = pos
            Log.d("Material3Spinner", "pos is $pos")
            var obj: Any? = null
            if (pos >= 0 && pos < this.adapter.count) {
                obj = this.adapter.getItem(pos)
            }
            if (obj == null) {
                mPosition = ListView.INVALID_POSITION
                text = null
            } else {
                setText(obj.toString())
            }
        }

    override fun onItemClick(adapterView: AdapterView<*>?, view: View?, position: Int, id: Long) {
        mPosition = position
    }


}