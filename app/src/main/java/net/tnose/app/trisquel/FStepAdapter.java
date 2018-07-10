package net.tnose.app.trisquel;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.GridView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by user on 2018/07/01.
 */

public class FStepAdapter extends BaseAdapter {
    private Context mContext;
    private LayoutInflater mLayoutInflater;
    private Double[] mFArray = {
            0.95, 1.0, 1.1,  1.2,  1.3,  1.4,  1.5,  1.6,  1.7,  1.8,  1.9,  2.0, 2.2, 2.4,
            2.5,  2.8, 3.2,  3.5,  4.0,  4.5,  4.8,  5.0,  5.6,  6.3,  6.7,  7.1,
            8.0,  9.0, 9.5, 10.0, 11.0, 13.0, 14.0, 16.0, 18.0, 19.0, 20.0, 22.0
    };

    private boolean[] mFCheckedArray = {
        false, false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false,
            false, false, false, false, false, false, false, false, false, false, false, false, false
    };
    List<Boolean> mCheckedList;

    private static class ViewHolder {
        public CheckBox checkBox;
    }

    public FStepAdapter(Context context) {
        mContext = context;
        mLayoutInflater = LayoutInflater.from(context);
        mCheckedList = new ArrayList<Boolean>();
        for(int i = 0; i < mFArray.length; i++){
            mCheckedList.add(Boolean.FALSE);
        }
    }

    public int getCount() {
        return mFArray.length;
    }

    public Object getItem(int position) {
        return mFArray[position];
    }

    public long getItemId(int position) {
        return position;
    }

    public String getFStepsString(){
        StringBuilder sb = new StringBuilder();
        for(int i = 0; i < mFArray.length; i++){
            if(mCheckedList.get(i)){
                if (sb.length() > 0) sb.append(", ");
                sb.append(mFArray[i].toString());
            }
        }
        return sb.toString();
    }

    public void setCheckedState(String s){
        if(s.length() > 0) {
            String[] fsAsArray = s.split(", ");
            ArrayList<Double> list = new ArrayList<Double>();
            for (String speed : fsAsArray) {
                list.add(Double.parseDouble(speed));
            }
            setCheckedState(list.toArray(new Double[list.size()])); //無駄だけどしょうがない
        }
    }

    public void setCheckedState(Double[] ds){
        List<Double> fsList = Arrays.asList(mFArray);
        List<Double> inputList = Arrays.asList(ds);
        mCheckedList.clear();

        for(int i = 0; i < fsList.size(); i++){
            if(inputList.contains(fsList.get(i))){
                mCheckedList.add(Boolean.TRUE);
            }else{
                mCheckedList.add(Boolean.FALSE);
            }
        }
    }

    public View getView(final int position, View convertView, final ViewGroup parent) {

        ViewHolder holder;
        if (convertView == null) {
            convertView = mLayoutInflater.inflate(R.layout.grid_item_cb, null);
            holder = new ViewHolder();
            holder.checkBox = convertView.findViewById(R.id.fval_checkbox);
            holder.checkBox.setChecked(mCheckedList.get(position));
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder)convertView.getTag();
        }
        holder.checkBox.setText(mFArray[position].toString());
        //なんでこれでOKなのかよくわからないがとりあえず動いている
        //http://falco.sakura.ne.jp/tech/2012/11/android-listview-onitemclick-%E3%82%A4%E3%83%99%E3%83%B3%E3%83%88%E3%81%8C%E7%99%BA%E7%94%9F%E3%81%97%E3%81%AA%E3%81%84%EF%BC%81/
        holder.checkBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                 ((GridView) parent).performItemClick(v, position, (long)v.getId());
            }
        });
        holder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                Double d = Double.parseDouble(buttonView.getText().toString());
                int i;
                for(i=0; i<FStepAdapter.this.mFArray.length; i++){
                    if(FStepAdapter.this.mFArray[i].equals(d)) break;
                }
                if(i != FStepAdapter.this.mFArray.length){
                    FStepAdapter.this.mCheckedList.set(i, isChecked);
                }
            }
        });

        return convertView;
    }
}
