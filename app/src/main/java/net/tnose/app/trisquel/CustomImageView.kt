package net.tnose.app.trisquel
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import kotlinx.android.synthetic.main.custom_image_view.view.*
import java.io.File


class CustomImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    var mPath : String = ""
    private var imgListener: View.OnClickListener? = null
    private var closeButtonListener: View.OnClickListener? = null

    init {
        initme()
    }

    override fun setOnClickListener(l: View.OnClickListener?) {
        this.imgListener = l
    }

    fun setOnCloseClickListener(l: View.OnClickListener?) {
        this.closeButtonListener = l
    }

    var path: String
        get() {
            return mPath
        }
        set(value: String){
            mPath = value
        }

    private fun initme() {
        LayoutInflater.from(context).inflate(R.layout.custom_image_view, this)
        this.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            // 大きさが確定してから描画しないといけない。
            // 当初はonCreateで追加されたCustomImageViewにpathが設定された瞬間に描画していたが、
            // それだと妙に小さくなる。なおこの半端な実装はgitの履歴には残していない。
            if(mPath.isNotEmpty() &&
                    oldBottom - oldTop != bottom - top &&
                    oldRight - oldLeft != right - left){
                val file = File(mPath)
                Glide.with(civ_image_view.context)
                        .load(file)
                        .apply(RequestOptions()
                                .placeholder(R.drawable.general_image_gray)
                                .centerCrop()
                                .error(R.drawable.ic_error_circle)
                                .timeout(5000)
                        )
                        .into(civ_image_view)
            }
        }
        civ_close_button.setOnClickListener {
            this@CustomImageView.closeButtonListener?.onClick(this@CustomImageView)
        }
        civ_image_view.setOnClickListener{
            this@CustomImageView.imgListener?.onClick(this@CustomImageView)
        }
    }
}