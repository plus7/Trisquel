package net.tnose.app.trisquel
import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import net.tnose.app.trisquel.databinding.CustomImageViewBinding
import java.io.File


class CustomImageView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : FrameLayout(context, attrs) {
    var mPath : String = ""
    private var imgListener: OnClickListener? = null
    private var closeButtonListener: OnClickListener? = null
    private var _binding: CustomImageViewBinding? = null
    private val binding get() = _binding!!

    init {
        initme()
    }

    override fun setOnClickListener(l: OnClickListener?) {
        this.imgListener = l
    }

    fun setOnCloseClickListener(l: OnClickListener?) {
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
        val inflater =LayoutInflater.from(context)
        _binding = CustomImageViewBinding.inflate(inflater, this, true)
        this.addOnLayoutChangeListener { v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            // 大きさが確定してから描画しないといけない。
            // 当初はonCreateで追加されたCustomImageViewにpathが設定された瞬間に描画していたが、
            // それだと妙に小さくなる。なおこの半端な実装はgitの履歴には残していない。
            if(mPath.isNotEmpty() &&
                    oldBottom - oldTop != bottom - top &&
                    oldRight - oldLeft != right - left) {
                val rb = if (mPath.startsWith("/")) {
                    Glide.with(binding.civImageView.context).load(File(mPath))
                }else{
                    Glide.with(binding.civImageView.context).load(Uri.parse(mPath))
                }
                rb.apply(RequestOptions()
                         .placeholder(R.drawable.general_image_gray)
                         .centerCrop()
                         .error(R.drawable.ic_error_circle)
                         .timeout(5000)).into(binding.civImageView)
            }
        }
        binding.civCloseButton.setOnClickListener {
            this@CustomImageView.closeButtonListener?.onClick(this@CustomImageView)
        }
        binding.civImageView.setOnClickListener{
            this@CustomImageView.imgListener?.onClick(this@CustomImageView)
        }
    }
}