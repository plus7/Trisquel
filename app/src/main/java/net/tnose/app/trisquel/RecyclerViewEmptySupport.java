package net.tnose.app.trisquel;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.TextView;

/**
 * Created by user on 2018/02/17.
 */
// based on https://stackoverflow.com/questions/28217436/how-to-show-an-empty-view-with-a-recyclerview
//  - added event handler for onItemRangeInserted and onItemRangeRemoved
//  - added setEmptyMessage setter function
public class RecyclerViewEmptySupport extends RecyclerView {
    private View emptyView;
    private String emptyMessage;

    private AdapterDataObserver emptyObserver = new AdapterDataObserver() {

        private void doEmptyCheck(){
            Adapter<?> adapter =  getAdapter();
            if(adapter != null && emptyView != null) {
                if(adapter.getItemCount() == 0) {
                    if(emptyView != null && emptyView instanceof TextView){
                        TextView tv = (TextView) emptyView;
                        tv.setText(emptyMessage);
                    }
                    emptyView.setVisibility(View.VISIBLE);
                    RecyclerViewEmptySupport.this.setVisibility(View.GONE);
                }
                else {
                    emptyView.setVisibility(View.GONE);
                    RecyclerViewEmptySupport.this.setVisibility(View.VISIBLE);
                }
            }
        }

        @Override
        public void onItemRangeInserted(int positionStart, int itemCount){
            doEmptyCheck();
        }

        @Override
        public void onItemRangeRemoved(int positionStart, int itemCount) {
            doEmptyCheck();
        }

        @Override
        public void onChanged() {
            doEmptyCheck();
        }
    };

    public RecyclerViewEmptySupport(Context context) {
        super(context);
    }

    public RecyclerViewEmptySupport(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public RecyclerViewEmptySupport(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void setAdapter(Adapter adapter) {
        super.setAdapter(adapter);

        if(adapter != null) {
            adapter.registerAdapterDataObserver(emptyObserver);
        }

        emptyObserver.onChanged();
    }

    public void setEmptyView(View emptyView) {
        this.emptyView = emptyView;
    }

    public void setEmptyMessage(String s) {
        this.emptyMessage = s;
        if(this.emptyView != null && this.emptyView instanceof TextView){
            TextView tv = (TextView) this.emptyView;
            tv.setText(s);
        }
    }
}