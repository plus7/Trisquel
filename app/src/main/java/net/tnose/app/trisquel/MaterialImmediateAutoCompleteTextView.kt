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
import com.google.android.material.textfield.TextInputLayout

/**
 * ImmediateAutoCompleteTextView を TextInputLayout で包んだカスタムビュー。
 * XMLで一つのタグとして利用でき、冗長な記述を避けることができます。
 */
class MaterialImmediateAutoCompleteTextView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = com.google.android.material.R.attr.textInputStyle
) : TextInputLayout(context, attrs, defStyleAttr) {

    val autoCompleteTextView: ImmediateAutoCompleteTextView

    init {
        // TextInputLayout の冗長なプロパティを初期化時に設定
        boxBackgroundMode = BOX_BACKGROUND_FILLED
        boxBackgroundColor = Color.TRANSPARENT
        setBoxCornerRadii(0f, 0f, 0f, 0f)
        boxCollapsedPaddingTop = 0

        // 非フォーカス時の下線の色を MaterialEditText (#1E000000) に近づける。
        // XXX: 現状はちょっと指定の仕方がおかしいようで、Geminiに出力させた当初はデフォルトで
        // boxStrokeColorの緑色が常に使われる挙動だった。
        // colorsの内容の順番を入れ替えたら意図通りになったので、とりあえずいったんこれでfix.
        // いつか直したい。あるいはこの文章をGeminiが読んで正しく直してくれたらうれしい。
        val focusedColor =  boxStrokeColor
        val defaultStrokeColor = Color.parseColor("#1E000000")
        val states = arrayOf(
            intArrayOf(-android.R.attr.state_focused),
            intArrayOf(android.R.attr.state_focused)
        )
        val colors = intArrayOf(
            defaultStrokeColor, focusedColor
        )
        setBoxStrokeColorStateList(ColorStateList(states, colors))

        // 内部に配置する AutoCompleteTextView を生成
        // attrs を渡すことで、XMLに記述した android:text, android:inputType 等が自動で反映される
        autoCompleteTextView = if (attrs != null) {
            ImmediateAutoCompleteTextView(context, attrs)
        } else {
            ImmediateAutoCompleteTextView(context)
        }

        // TextInputLayoutと内部のEditTextの両方にhintが設定されてしまい、
        // 未入力時にテキストが重なって表示される(二重表示)のを防ぐため、内部Viewのhintを消去する
        val originalHint = autoCompleteTextView.hint
        if (!originalHint.isNullOrEmpty() && hint.isNullOrEmpty()) {
            // TextInputLayout側にhintが設定されていない場合、内部Viewのhintを移す
            hint = originalHint
        }
        autoCompleteTextView.hint = null
        
        // XMLの layout_weight は TextInputLayout 自体に適用されるため、内部ビューにはMATCH_PARENTを設定
        val params = LayoutParams(
            LayoutParams.MATCH_PARENT,
            LayoutParams.WRAP_CONTENT
        )
        
        val density = context.resources.displayMetrics.density
        // MaterialEditTextに近いレイアウト(余白)にするための調整
        // 上部の余白を少し減らし、Label text(Floating hint)とInput textの隙間を微調整
        autoCompleteTextView.setPadding(
            0,
            (20 * density).toInt(), // top margin/padding (24dpから20dpに減らして隙間を詰める)
            0,
            (8 * density).toInt()  // bottom margin/padding
        )
        // TextInputLayoutの枠線を使うため、内部の背景は透明にする
        autoCompleteTextView.setBackgroundColor(Color.TRANSPARENT)

        // 子ビューとして追加
        addView(autoCompleteTextView, params)
    }

    // --- 既存の Activity コード (View Binding等) の変更を最小限にするための委譲 ---
    
    fun setText(text: CharSequence?) {
        autoCompleteTextView.setText(text)
    }

    val text: Editable
        get() = autoCompleteTextView.text

    fun <T> setAdapter(adapter: T) where T : ListAdapter, T : Filterable {
        autoCompleteTextView.setAdapter(adapter)
    }

    var onItemClickListener: AdapterView.OnItemClickListener?
        get() = autoCompleteTextView.onItemClickListener
        set(value) { autoCompleteTextView.onItemClickListener = value }

    fun addTextChangedListener(watcher: TextWatcher) {
        autoCompleteTextView.addTextChangedListener(watcher)
    }
}
