package net.tnose.app.trisquel;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.List;

/**
 * Created by user on 2018/02/12.
 */

public class CameraAdapter extends ArrayAdapter<CameraSpec> {
    public CameraAdapter(Context context, int resource){
        super(context, resource);
    }
    public CameraAdapter(Context context, int resource, List<CameraSpec> items){
        super(context, resource, items);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView) super.getView(position, convertView, parent);
        CameraSpec c = getItem(position);
        view.setText(c.manufacturer + " " + c.modelName);
        return view;
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        TextView view = (TextView) super.getDropDownView(position, convertView, parent);
        CameraSpec c = getItem(position);
        view.setText(c.manufacturer + " " + c.modelName);
        return view;
    }

    public int getPosition(int id){
        int position = -1;
        for (int i = 0 ; i < this.getCount(); i++){
            if (this.getItem(i).id == id) {
                position = i;
                break;
            }
        }
        return position;
    }
}