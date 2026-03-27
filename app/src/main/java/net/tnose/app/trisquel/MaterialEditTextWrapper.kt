package net.tnose.app.trisquel

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout

/**
 * MaterialEditText を OS標準のコンポーネントで置き換えるためのカスタムビュー。
 * TextInputLayout + TextInputEditText の組み合わせで、冗長な記述を避けます。
 */
class MaterialEditTextWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {

    val internalEditText: TextInputEditText

    init {
        // TextInputLayout の冗長なプロパティを初期化時に設定
        boxBackgroundMode = BOX_BACKGROUND_FILLED
        boxBackgroundColor = Color.TRANSPARENT
        setBoxCornerRadii(0f, 0f, 0f, 0f)
        boxCollapsedPaddingTop = 0

        // 非フォーカス時の下線の色を MaterialEditText (#1E000000) に近づけるための調整
        val focusedColor = boxStrokeColor
        val defaultStrokeColor = Color.parseColor("#1E000000")
        //val hoveredColor = Color.parseColor("#42000000") // hover/focused以外の色
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_focused),
            intArrayOf(android.R.attr.state_focused),
            //intArrayOf(android.R.attr.state_hovered)
        )
        val colors = intArrayOf(
            defaultStrokeColor,
            focusedColor,
            //hoveredColor,
        )
        setBoxStrokeColorStateList(ColorStateList(states, colors))

        // 内部に配置する TextInputEditText を生成
        // attrs を渡すことで、XMLに記述した android:text, android:inputType 等が自動で反映される
        internalEditText = if (attrs != null) {
            TextInputEditText(context, attrs)
        } else {
            TextInputEditText(context)
        }

        // TextInputLayoutと内部のEditTextの両方にhintが設定されてしまい、
        // 未入力時にテキストが重なって表示される(二重表示)のを防ぐため、内部Viewのhintを消去する
        val originalHint = internalEditText.hint
        if (!originalHint.isNullOrEmpty() && hint.isNullOrEmpty()) {
            hint = originalHint
        }
        internalEditText.hint = null
        
        // XMLの layout_weight は TextInputLayout 自体に適用されるため、内部ビューにはMATCH_PARENTを設定
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        
        val density = context.resources.displayMetrics.density
        // MaterialEditTextに近いレイアウト(余白)にするための調整
        internalEditText.setPadding(
            0,
            (20 * density).toInt(), // top margin/padding
            0,
            (8 * density).toInt()  // bottom margin/padding
        )
        // TextInputLayoutの枠線を使うため、内部の背景は透明にする
        internalEditText.setBackgroundColor(Color.TRANSPARENT)

        // 子ビューとして追加
        addView(internalEditText, params)
    }

    // --- 既存の Activity コード (View Binding等) の変更を最小限にするための委譲 ---
    
    fun setText(text: CharSequence?) {
        internalEditText.setText(text)
    }

    val text: Editable?
        get() = internalEditText.text

    fun addTextChangedListener(watcher: TextWatcher) {
        internalEditText.addTextChangedListener(watcher)
    }

    /**
     * MaterialEditText の helperTextColor への互換性のためのプロパティ。
     */
    var helperTextColor: Int
        get() = helperTextCurrentTextColor
        set(value) {
            // value がリソースID (R.color.xxx) か、実際の色 (Color Int) か判定して設定する
            val color = try {
                ContextCompat.getColor(context, value)
            } catch (e: Exception) {
                value // リソースが見つからない場合などは、そのまま色として扱う
            }
            setHelperTextColor(ColorStateList.valueOf(color))
        }

    /**
     * MaterialEditText の isHelperTextAlwaysShown への互換性のためのプロパティ。
     * TextInputLayout では `isHelperTextEnabled` を ON にすることで近い挙動になるため、それに委譲する。
     */
    var isHelperTextAlwaysShown: Boolean
        get() = isHelperTextEnabled
        set(value) {
            isHelperTextEnabled = value
        }
}