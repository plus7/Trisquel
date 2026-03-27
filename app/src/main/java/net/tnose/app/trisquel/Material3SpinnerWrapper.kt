package net.tnose.app.trisquel

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.text.Editable
import android.text.TextWatcher
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Filterable
import android.widget.ListAdapter
import androidx.core.content.ContextCompat
import com.google.android.material.textfield.TextInputLayout

/**
 * MaterialBetterSpinner を OS標準のコンポーネントで置き換えるためのカスタムビュー。
 * TextInputLayout + Material3Spinner の組み合わせで、冗長な記述を避けます。
 */
class Material3SpinnerWrapper @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {

    val internalSpinner: Material3Spinner

    init {
        // TextInputLayout の冗長なプロパティを初期化時に設定
        boxBackgroundMode = BOX_BACKGROUND_FILLED
        boxBackgroundColor = Color.TRANSPARENT
        setBoxCornerRadii(0f, 0f, 0f, 0f)
        boxCollapsedPaddingTop = 0
        endIconMode = END_ICON_DROPDOWN_MENU

        // 非フォーカス時の下線の色を調整
        val focusedColor = boxStrokeColor
        val defaultStrokeColor = Color.parseColor("#1E000000")
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_focused),
            intArrayOf(android.R.attr.state_focused)
        )
        val colors = intArrayOf(
            defaultStrokeColor,
            focusedColor
        )
        setBoxStrokeColorStateList(ColorStateList(states, colors))

        // 内部に配置する Material3Spinner を生成
        internalSpinner = if (attrs != null) {
            Material3Spinner(context, attrs)
        } else {
            Material3Spinner(context)
        }

        val originalHint = internalSpinner.hint
        if (!originalHint.isNullOrEmpty() && hint.isNullOrEmpty()) {
            hint = originalHint
        }
        internalSpinner.hint = null
        
        // focusableInTouchMode を false にしてドロップダウンとして振る舞うようにする
        internalSpinner.isFocusableInTouchMode = false

        // XMLの layout_weight は TextInputLayout 自体に適用されるため、内部ビューにはMATCH_PARENTを設定
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        
        val density = context.resources.displayMetrics.density
        internalSpinner.setPadding(
            0,
            (20 * density).toInt(), // top margin/padding
            0,
            (8 * density).toInt()  // bottom margin/padding
        )
        internalSpinner.setBackgroundColor(Color.TRANSPARENT)

        addView(internalSpinner, params)
    }

    // --- 互換性のための委譲 ---

    fun <T> setAdapter(adapter: T?) where T : ListAdapter, T : Filterable {
        internalSpinner.setAdapter(adapter)
    }

    fun setText(text: CharSequence?) {
        internalSpinner.setText(text, false) // フィルターをかけないように false を渡すことが多い
    }

    val text: Editable?
        get() = internalSpinner.text

    val adapter: ListAdapter?
        get() = internalSpinner.adapter

    var position: Int
        get() = internalSpinner.position
        set(value) {
            internalSpinner.position = value
        }

    fun addTextChangedListener(watcher: TextWatcher) {
        internalSpinner.addTextChangedListener(watcher)
    }

    var onItemClickListener: AdapterView.OnItemClickListener?
        get() = internalSpinner.onItemClickListener
        set(value) {
            internalSpinner.onItemClickListener = value
        }

    var helperTextColor: Int
        get() = helperTextCurrentTextColor
        set(value) {
            val color = try {
                ContextCompat.getColor(context, value)
            } catch (e: Exception) {
                value
            }
            setHelperTextColor(ColorStateList.valueOf(color))
        }

    var isHelperTextAlwaysShown: Boolean
        get() = isHelperTextEnabled
        set(value) {
            isHelperTextEnabled = value
        }
}