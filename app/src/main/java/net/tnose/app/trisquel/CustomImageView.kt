package net.tnose.app.trisquel
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.support.v4.content.ContextCompat
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import kotlinx.android.synthetic.main.custom_image_view.view.*


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
            try {
                var bmp: Bitmap?
                val options = BitmapFactory.Options()
                options.inJustDecodeBounds = true
                bmp = BitmapFactory.decodeFile(path, options)
                var (bmp_x, bmp_y) = Pair(options.outWidth, options.outHeight)
                val scale = resources.displayMetrics.density
                val inSampleSize = kotlin.math.max((bmp_y / (150 * scale)).toInt(), 1) // 横に極めて長い嫌がらせ画像が来たら無理だけど手抜き
                options.inJustDecodeBounds = false
                options.inSampleSize = inSampleSize
                bmp = BitmapFactory.decodeFile(path, options)
                if(bmp == null){
                    val drawable = ContextCompat.getDrawable(context, R.drawable.ic_error_circle)
                    bmp = Bitmap.createBitmap(
                            drawable?.intrinsicWidth ?: 150,
                            drawable?.intrinsicHeight ?: 150,
                            Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    drawable?.setBounds(0, 0, canvas.width, canvas.height)
                    drawable?.draw(canvas)
                }
                civ_image_view.setImageBitmap(bmp)
            }catch (e: Exception){
                e.printStackTrace()
            }
        }

    private fun initme() {
        LayoutInflater.from(context).inflate(R.layout.custom_image_view, this)
        civ_close_button.setOnClickListener {
            this@CustomImageView.closeButtonListener?.onClick(this@CustomImageView)
        }
        civ_image_view.setOnClickListener{
            this@CustomImageView.imgListener?.onClick(this@CustomImageView)
        }
    }
}