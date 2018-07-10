package net.tnose.app.trisquel;

/**
 * Based on https://github.com/nukka123/DialogFragmentDemo
 */

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/**
 * コールバックによる通知機能を備えたダイアログを実装するための抽象ダイアログ・フラグメント.
 * <p>
 * このクラスを実装したダイアログ・フラグメントを生成するためには、
 * {@link Builder#build(int)} を使用する必要があります.
 * <p>
 * 実装クラスによるダイアログの結果は、{@link Callback}を実装したホストへ通知されます.
 * 通知対象のホストは、このクラスが提供する表示用メソッドで指定する事ができます.
 * 詳しくは {@link #showOn(Activity, String)}、  {@link #showOn(Fragment, String)}、
 * {@link #showChildOn(Fragment, String)} のドキュメントを確認してください.
 * <p>
 * NOTE: 通知対象のホストを明確にするため、実装クラスのダイアログを表示するメソッドとして
 * {@link DialogFragment#show(FragmentManager, String)}、
 * {@link DialogFragment#show(FragmentTransaction, String)}
 * を使用することは推奨しません.
 */
public abstract class AbstractDialogFragment extends DialogFragment {

    /**
     * ダイアログの結果を通知するためのコールバック・インタフェース.
     */
    public interface Callback {
        /**
         * このメソッドは、ダイアログのボタンが押された時に呼び出されます.
         *
         * @param requestCode ダイアログのリクエストコード.
         * @param resultCode  ダイアログの結果コード. {@link DialogInterface} が定義する値に従います.
         * @param data        ダイアログの結果データ. このパラメータは補助的に使用されます.
         *                    結果データの具体的な仕様は、実装クラスのドキュメントを確認してください.
         *                    実装によってはnullである場合があります.
         */
        void onDialogResult(int requestCode, int resultCode, Intent data);

        /**
         * このメソッドは、ダイアログがキャンセルされた時に呼び出されます.
         *
         * @param requestCode ダイアログのリクエストコード.
         */
        void onDialogCancelled(int requestCode);
    }

    /**
     * ダイアログのホストの種別
     */
    private enum HostType {
        UNSPECIFIED,
        ACTIVITY,
        TARGET_FRAGMENT,
        PARENT_FRAGMENT
    }

    private static final String ARG_PREFIX = AbstractDialogFragment.class.getName() + ".";
    private static final String ARG_REQUEST_CODE = ARG_PREFIX + "requestCode";
    private static final String ARG_CALLBACK_HOST = ARG_PREFIX + "callbackHostSpec";

    private int requestCode;
    private HostType callbackHostSpec;


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        checkArguments(getArguments());

        Bundle args = getArguments();
        requestCode = args.getInt(ARG_REQUEST_CODE);
        callbackHostSpec = (HostType) args.getSerializable(ARG_CALLBACK_HOST);
    }


    @Override
    public void onCancel(DialogInterface dialog) {
        super.onCancel(dialog);
        notifyDialogCancelled();
    }


    /**
     * {@link #callbackHostSpec} の状態別に、target が コールバックすべき対象であるかを確認します.
     * callbackHostSpec が UNSPECIFIED である場合は、全てのtargetがコールバック対象です.
     * callbackHostSpec が UNSPECIFIED以外 である場合は、callbackHostSpec と一致する target がコールバック対象です.
     *
     * @param target 検査するコールバック対象の種別
     * @return 確認結果: true=コールバックすべき対象である場合. false=それ以外.
     */
    private boolean shouldCallback(HostType target) {
        return callbackHostSpec == HostType.UNSPECIFIED || callbackHostSpec == target;
    }

    /**
     * ダイアログの結果を通知します.
     * <p>
     * 実装クラスは、ダイアログの終了を伴うボタンのクリック・イベントにおいて、
     * このメソッドを使用して結果をホストに通知するようにしてください.
     *
     * @param resultCode 結果コード.
     *                   例で示すような {@link DialogInterface} の定義値を設定してください.
     *                   1) {@link DialogInterface.OnClickListener#onClick(DialogInterface, int)}
     *                   から取得したwitchの値.
     *                   2) ボタンの役割に応じた次の値; {@link DialogInterface#BUTTON_POSITIVE}、
     *                   {@link DialogInterface#BUTTON_NEUTRAL}、{@link DialogInterface#BUTTON_NEGATIVE}
     * @param data       結果データ. ダイアログの結果として結果コード以外のデータを渡したい場合に、
     *                   このパラメータを使用してください.
     *                   このパラメータを使用する必要がない場合はnullを与えてください.
     */
    protected final void notifyDialogResult(int resultCode, Intent data) {
        Activity activity = getActivity();
        if (shouldCallback(HostType.ACTIVITY) && activity instanceof Callback) {
            Callback callback = (Callback) activity;
            callback.onDialogResult(requestCode, resultCode, data);
        }

        Fragment target = getTargetFragment();
        if (shouldCallback(HostType.TARGET_FRAGMENT) && target instanceof Callback) {
            Callback callback = (Callback) target;
            callback.onDialogResult(requestCode, resultCode, data);
        }

        Fragment parent = getParentFragment();
        if (shouldCallback(HostType.PARENT_FRAGMENT) && parent instanceof Callback) {
            Callback callback = (Callback) parent;
            callback.onDialogResult(requestCode, resultCode, data);
        }
    }

    /**
     * ダイアログの(領域外のタップ等による)キャンセルが発生した事を通知します.
     * <p>
     * NOTE: 抽象クラスは {@link #onCancel(DialogInterface)} の処理として、
     * このメソッドによる通知を実施しています.
     * そのため実際には、実装クラスが、このメソッドを使用する必要はありません.
     */
    protected final void notifyDialogCancelled() {
        Activity activity = getActivity();
        if (shouldCallback(HostType.ACTIVITY) && activity instanceof Callback) {
            Callback callback = (Callback) activity;
            callback.onDialogCancelled(requestCode);
        }

        Fragment target = getTargetFragment();
        if (shouldCallback(HostType.TARGET_FRAGMENT) && target instanceof Callback) {
            Callback callback = (Callback) target;
            callback.onDialogCancelled(requestCode);
        }

        Fragment parent = getParentFragment();
        if (shouldCallback(HostType.PARENT_FRAGMENT) && parent instanceof Callback) {
            Callback callback = (Callback) parent;
            callback.onDialogCancelled(requestCode);
        }
    }


    /**
     * ダイアログ・フラグメントのビルダ.
     * <p>
     * 実装クラスは、このビルダを継承して、各自のダイアログ・フラグメントを構築する処理を実装します.
     * <p>
     * ダイアログの利用者は、実装クラスのビルダを使用して、ダイアログ・フラグメントを生成してください.
     */
    public abstract static class Builder {

        private boolean cancelable = true;

        /**
                    * ダイアログのキャンセルが可能か否かを設定します.
                    *
         * @param cancelable キャンセル可能か否か.
         * @return 自身のビルダ.
                    */
            @NonNull
            public Builder setCancelable(boolean cancelable) {
                this.cancelable = cancelable;
            return this;
        }

        /**
         * ダイアログ・フラグメントを生成するための抽象メソッド.
         * <p>
         * 実装クラスは、このメソッドをオーバーライドしてダイアログ・フラグメントを生成してください.
         *
         * @return 生成したダイアログ・フラグメント.
         */
        @NonNull
        protected abstract AbstractDialogFragment build();

        /**
         * ダイアログ・フラグメントを生成する.
         *
         * @param requestCode リクエスト・コード.
         * @return 生成したダイアログ・フラグメント.
         */
        @NonNull
        public final AbstractDialogFragment build(int requestCode) {
            AbstractDialogFragment dialog = build();
            Bundle args = (dialog.getArguments() != null) ? dialog.getArguments() : new Bundle();

            args.putInt(ARG_REQUEST_CODE, requestCode);

            // デフォルトのホスト種別を設定する.
            // 明示的な設定は、ダイアログの表示メソッドで更新する.
            args.putSerializable(ARG_CALLBACK_HOST, HostType.UNSPECIFIED);

            dialog.setArguments(args);
            dialog.setCancelable(cancelable);

            return dialog;
        }
    }

    /**
     * ダイアログを表示します。
     * ダイアログの結果は {@link Callback} を実装したホスト
     * (アクティビティ、ターゲット指定されたフラグメント、親フラグメント)に通知します.
     *
     * @param transaction ダイアログを追加するトランザクション
     * @param tag         ダイアログを識別するタグ
     * @deprecated 本メソッドの使用は推奨しません。
     * 可能な限り {@link #showOn(Activity, String)}、  {@link #showOn(Fragment, String)}、
     * {@link #showChildOn(Fragment, String)} の何れかのメソッドを使用して表示させてください。
     */
    @Override
    public int show(FragmentTransaction transaction, String tag) {
        return super.show(transaction, tag);
    }

    /**
     * ダイアログを表示します。
     * ダイアログの結果は {@link Callback} を実装したホスト
     * (アクティビティ、ターゲット指定されたフラグメント、親フラグメント)に通知します.
     *
     * @param manager ダイアログを追加するフラグメント・マネージャ
     * @param tag     ダイアログを識別するタグ
     * @deprecated 本メソッドの使用は推奨しません。
     * 可能な限り {@link #showOn(Activity, String)}、  {@link #showOn(Fragment, String)}、
     * {@link #showChildOn(Fragment, String)} の何れかのメソッドを使用して表示させてください。
     */
    @Override
    public void show(FragmentManager manager, String tag) {
        super.show(manager, tag);
    }

    /**
     * ダイアログを表示します.
     * ダイアログの結果は {@link Callback} を実装したホスト(アクティビティ)に通知します.
     * <p>
     * ダイアログ・フラグメントは {@link AppCompatActivity#getSupportFragmentManager()}
     * に追加されます.
     *
     * @param host ダイアログの結果を通知するホスト
     *             NOTE: 引数に与えるアクティビティは AppCompatActivity を継承したオブジェクトである必要があります。
     * @param tag  ダイアログを識別するタグ
     */
    public void showOn(@NonNull Activity host, String tag) {
        checkAppCompatActivity(host);
        Log.d("getArguments", getArguments().toString());
        checkArguments(getArguments());

        getArguments().putSerializable(ARG_CALLBACK_HOST, HostType.ACTIVITY);

        AppCompatActivity hostCompat = (AppCompatActivity) host;
        FragmentManager manager = hostCompat.getSupportFragmentManager();
        super.show(manager, tag);
    }

    /**
     * ダイアログを表示します.
     * ダイアログの結果は {@link Callback} を実装したホスト(フラグメント)に通知します.
     * <p>
     * ダイアログ・フラグメントは {@link Fragment#getFragmentManager()}
     * に追加されます.
     *
     * @param host ダイアログの結果を通知するホスト
     * @param tag  ダイアログを識別するタグ
     */
    public void showOn(@NonNull Fragment host, String tag) {
        checkArguments(getArguments());

        getArguments().putSerializable(ARG_CALLBACK_HOST, HostType.TARGET_FRAGMENT);

        setTargetFragment(host, getArguments().getInt(ARG_REQUEST_CODE));

        FragmentManager manager = host.getFragmentManager();
        super.show(manager, tag);
    }

    /**
     * ダイアログを子フラグメントとして表示します.
     * ダイアログの結果は {@link Callback} を実装したホスト(フラグメント)に通知します.
     * <p>
     * ダイアログ・フラグメントは {@link Fragment#getChildFragmentManager()}
     * に追加されます.
     *
     * @param host ダイアログの結果を通知するホスト
     * @param tag  ダイアログを識別するタグ
     */
    public void showChildOn(@NonNull Fragment host, String tag) {
        checkArguments(getArguments());

        getArguments().putSerializable(ARG_CALLBACK_HOST, HostType.PARENT_FRAGMENT);

        FragmentManager manager = host.getChildFragmentManager();
        super.show(manager, tag);
    }


    private static void checkAppCompatActivity(Activity activity) {
        if (!(activity instanceof AppCompatActivity)) {
            throw new IllegalArgumentException("host activity only supports AppCompatActivity.");
        }
    }

    private static void checkArguments(Bundle bundle) {
        if (bundle == null) {
            throw new IllegalStateException("Don't clear setArguments()");
        }
    }
}
