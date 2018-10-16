package net.tnose.app.trisquel

/**
 * Based on https://github.com/nukka123/DialogFragmentDemo
 */

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.DialogFragment
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentTransaction
import android.support.v7.app.AppCompatActivity
import android.util.Log
import net.tnose.app.trisquel.AbstractDialogFragment.Builder
import net.tnose.app.trisquel.AbstractDialogFragment.Callback

/**
 * コールバックによる通知機能を備えたダイアログを実装するための抽象ダイアログ・フラグメント.
 *
 *
 * このクラスを実装したダイアログ・フラグメントを生成するためには、
 * [Builder.build] を使用する必要があります.
 *
 *
 * 実装クラスによるダイアログの結果は、[Callback]を実装したホストへ通知されます.
 * 通知対象のホストは、このクラスが提供する表示用メソッドで指定する事ができます.
 * 詳しくは [.showOn]、  [.showOn]、
 * [.showChildOn] のドキュメントを確認してください.
 *
 *
 * NOTE: 通知対象のホストを明確にするため、実装クラスのダイアログを表示するメソッドとして
 * [DialogFragment.show]、
 * [DialogFragment.show]
 * を使用することは推奨しません.
 */
abstract class AbstractDialogFragment : DialogFragment() {

    private var requestCode: Int = 0
    private var callbackHostSpec: HostType? = null

    /**
     * ダイアログの結果を通知するためのコールバック・インタフェース.
     */
    interface Callback {
        /**
         * このメソッドは、ダイアログのボタンが押された時に呼び出されます.
         *
         * @param requestCode ダイアログのリクエストコード.
         * @param resultCode  ダイアログの結果コード. [DialogInterface] が定義する値に従います.
         * @param data        ダイアログの結果データ. このパラメータは補助的に使用されます.
         * 結果データの具体的な仕様は、実装クラスのドキュメントを確認してください.
         * 実装によってはnullである場合があります.
         */
        fun onDialogResult(requestCode: Int, resultCode: Int, data: Intent)

        /**
         * このメソッドは、ダイアログがキャンセルされた時に呼び出されます.
         *
         * @param requestCode ダイアログのリクエストコード.
         */
        fun onDialogCancelled(requestCode: Int)
    }

    /**
     * ダイアログのホストの種別
     */
    private enum class HostType {
        UNSPECIFIED,
        ACTIVITY,
        TARGET_FRAGMENT,
        PARENT_FRAGMENT
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkArguments(arguments)

        val args = arguments
        requestCode = args.getInt(ARG_REQUEST_CODE)
        callbackHostSpec = args.getSerializable(ARG_CALLBACK_HOST) as HostType
    }


    override fun onCancel(dialog: DialogInterface?) {
        super.onCancel(dialog)
        notifyDialogCancelled()
    }


    /**
     * [.callbackHostSpec] の状態別に、target が コールバックすべき対象であるかを確認します.
     * callbackHostSpec が UNSPECIFIED である場合は、全てのtargetがコールバック対象です.
     * callbackHostSpec が UNSPECIFIED以外 である場合は、callbackHostSpec と一致する target がコールバック対象です.
     *
     * @param target 検査するコールバック対象の種別
     * @return 確認結果: true=コールバックすべき対象である場合. false=それ以外.
     */
    private fun shouldCallback(target: HostType): Boolean {
        return callbackHostSpec == HostType.UNSPECIFIED || callbackHostSpec == target
    }

    /**
     * ダイアログの結果を通知します.
     *
     *
     * 実装クラスは、ダイアログの終了を伴うボタンのクリック・イベントにおいて、
     * このメソッドを使用して結果をホストに通知するようにしてください.
     *
     * @param resultCode 結果コード.
     * 例で示すような [DialogInterface] の定義値を設定してください.
     * 1) [DialogInterface.OnClickListener.onClick]
     * から取得したwitchの値.
     * 2) ボタンの役割に応じた次の値; [DialogInterface.BUTTON_POSITIVE]、
     * [DialogInterface.BUTTON_NEUTRAL]、[DialogInterface.BUTTON_NEGATIVE]
     * @param data       結果データ. ダイアログの結果として結果コード以外のデータを渡したい場合に、
     * このパラメータを使用してください.
     * このパラメータを使用する必要がない場合はnullを与えてください.
     */
    protected fun notifyDialogResult(resultCode: Int, data: Intent) {
        val activity = activity
        if (shouldCallback(HostType.ACTIVITY) && activity is Callback) {
            val callback = activity as Callback
            callback.onDialogResult(requestCode, resultCode, data)
        }

        val target = targetFragment
        if (shouldCallback(HostType.TARGET_FRAGMENT) && target is Callback) {
            val callback = target as Callback
            callback.onDialogResult(requestCode, resultCode, data)
        }

        val parent = parentFragment
        if (shouldCallback(HostType.PARENT_FRAGMENT) && parent is Callback) {
            val callback = parent as Callback
            callback.onDialogResult(requestCode, resultCode, data)
        }
    }

    /**
     * ダイアログの(領域外のタップ等による)キャンセルが発生した事を通知します.
     *
     *
     * NOTE: 抽象クラスは [.onCancel] の処理として、
     * このメソッドによる通知を実施しています.
     * そのため実際には、実装クラスが、このメソッドを使用する必要はありません.
     */
    protected fun notifyDialogCancelled() {
        val activity = activity
        if (shouldCallback(HostType.ACTIVITY) && activity is Callback) {
            val callback = activity as Callback
            callback.onDialogCancelled(requestCode)
        }

        val target = targetFragment
        if (shouldCallback(HostType.TARGET_FRAGMENT) && target is Callback) {
            val callback = target as Callback
            callback.onDialogCancelled(requestCode)
        }

        val parent = parentFragment
        if (shouldCallback(HostType.PARENT_FRAGMENT) && parent is Callback) {
            val callback = parent as Callback
            callback.onDialogCancelled(requestCode)
        }
    }


    /**
     * ダイアログ・フラグメントのビルダ.
     *
     *
     * 実装クラスは、このビルダを継承して、各自のダイアログ・フラグメントを構築する処理を実装します.
     *
     *
     * ダイアログの利用者は、実装クラスのビルダを使用して、ダイアログ・フラグメントを生成してください.
     */
    abstract class Builder {

        private var cancelable = true

        /**
         * ダイアログのキャンセルが可能か否かを設定します.
         *
         * @param cancelable キャンセル可能か否か.
         * @return 自身のビルダ.
         */
        fun setCancelable(cancelable: Boolean): Builder {
            this.cancelable = cancelable
            return this
        }

        /**
         * ダイアログ・フラグメントを生成するための抽象メソッド.
         *
         *
         * 実装クラスは、このメソッドをオーバーライドしてダイアログ・フラグメントを生成してください.
         *
         * @return 生成したダイアログ・フラグメント.
         */
        protected abstract fun build(): AbstractDialogFragment

        /**
         * ダイアログ・フラグメントを生成する.
         *
         * @param requestCode リクエスト・コード.
         * @return 生成したダイアログ・フラグメント.
         */
        fun build(requestCode: Int): AbstractDialogFragment {
            val dialog = build()
            val args = if (dialog.arguments != null) dialog.arguments else Bundle()

            args.putInt(ARG_REQUEST_CODE, requestCode)

            // デフォルトのホスト種別を設定する.
            // 明示的な設定は、ダイアログの表示メソッドで更新する.
            args.putSerializable(ARG_CALLBACK_HOST, HostType.UNSPECIFIED)

            dialog.arguments = args
            dialog.isCancelable = cancelable

            return dialog
        }
    }

    /**
     * ダイアログを表示します。
     * ダイアログの結果は [Callback] を実装したホスト
     * (アクティビティ、ターゲット指定されたフラグメント、親フラグメント)に通知します.
     *
     * @param transaction ダイアログを追加するトランザクション
     * @param tag         ダイアログを識別するタグ
     */
    @Deprecated("本メソッドの使用は推奨しません。\n" +
            "      可能な限り {@link #showOn(Activity, String)}、  {@link #showOn(Fragment, String)}、\n" +
            "      {@link #showChildOn(Fragment, String)} の何れかのメソッドを使用して表示させてください。")
    override fun show(transaction: FragmentTransaction, tag: String): Int {
        return super.show(transaction, tag)
    }

    /**
     * ダイアログを表示します。
     * ダイアログの結果は [Callback] を実装したホスト
     * (アクティビティ、ターゲット指定されたフラグメント、親フラグメント)に通知します.
     *
     * @param manager ダイアログを追加するフラグメント・マネージャ
     * @param tag     ダイアログを識別するタグ
     */
    @Deprecated("本メソッドの使用は推奨しません。\n" +
            "      可能な限り {@link #showOn(Activity, String)}、  {@link #showOn(Fragment, String)}、\n" +
            "      {@link #showChildOn(Fragment, String)} の何れかのメソッドを使用して表示させてください。")
    override fun show(manager: FragmentManager, tag: String) {
        super.show(manager, tag)
    }

    /**
     * ダイアログを表示します.
     * ダイアログの結果は [Callback] を実装したホスト(アクティビティ)に通知します.
     *
     *
     * ダイアログ・フラグメントは [AppCompatActivity.getSupportFragmentManager]
     * に追加されます.
     *
     * @param host ダイアログの結果を通知するホスト
     * NOTE: 引数に与えるアクティビティは AppCompatActivity を継承したオブジェクトである必要があります。
     * @param tag  ダイアログを識別するタグ
     */
    fun showOn(host: Activity, tag: String) {
        checkAppCompatActivity(host)
        Log.d("getArguments", arguments.toString())
        checkArguments(arguments)

        arguments.putSerializable(ARG_CALLBACK_HOST, HostType.ACTIVITY)

        val hostCompat = host as AppCompatActivity
        val manager = hostCompat.supportFragmentManager
        super.show(manager, tag)
    }

    /**
     * ダイアログを表示します.
     * ダイアログの結果は [Callback] を実装したホスト(フラグメント)に通知します.
     *
     *
     * ダイアログ・フラグメントは [Fragment.getFragmentManager]
     * に追加されます.
     *
     * @param host ダイアログの結果を通知するホスト
     * @param tag  ダイアログを識別するタグ
     */
    fun showOn(host: Fragment, tag: String) {
        checkArguments(arguments)

        arguments.putSerializable(ARG_CALLBACK_HOST, HostType.TARGET_FRAGMENT)

        setTargetFragment(host, arguments.getInt(ARG_REQUEST_CODE))

        val manager = host.fragmentManager
        super.show(manager, tag)
    }

    /**
     * ダイアログを子フラグメントとして表示します.
     * ダイアログの結果は [Callback] を実装したホスト(フラグメント)に通知します.
     *
     *
     * ダイアログ・フラグメントは [Fragment.getChildFragmentManager]
     * に追加されます.
     *
     * @param host ダイアログの結果を通知するホスト
     * @param tag  ダイアログを識別するタグ
     */
    fun showChildOn(host: Fragment, tag: String) {
        checkArguments(arguments)

        arguments.putSerializable(ARG_CALLBACK_HOST, HostType.PARENT_FRAGMENT)

        val manager = host.childFragmentManager
        super.show(manager, tag)
    }

    companion object {

        private val ARG_PREFIX = AbstractDialogFragment::class.java.name + "."
        private val ARG_REQUEST_CODE = ARG_PREFIX + "requestCode"
        private val ARG_CALLBACK_HOST = ARG_PREFIX + "callbackHostSpec"


        private fun checkAppCompatActivity(activity: Activity) {
            if (activity !is AppCompatActivity) {
                throw IllegalArgumentException("host activity only supports AppCompatActivity.")
            }
        }

        private fun checkArguments(bundle: Bundle?) {
            if (bundle == null) {
                throw IllegalStateException("Don't clear setArguments()")
            }
        }
    }
}
