package shook.shook;

import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Adrien on 06/03/2018.
 */

public class CustomAdapter extends ArrayAdapter {

    private ArrayList dataSet;
    Context mContext;

    // View lookup cache
    private static class ViewHolder {
        TextView txtName;

    }

    public CustomAdapter(ArrayList data, Context context) {
        super(context, R.layout.activity_search_result_item, data);
        this.dataSet = data;
        this.mContext = context;

    }

    @Nullable
    @Override
    public String getItem(int position) {
        return (String) dataSet.get(position);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        ViewHolder viewHolder; // view lookup cache stored in tag

        if (convertView == null) {

            viewHolder = new ViewHolder();
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.activity_search_result_item, parent, false);
//            viewHolder.txtName = (TextView) convertView.findViewById(R.id.name);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        viewHolder.txtName.setText(getItem(position));
        // Return the completed view to render on screen
        return convertView;
    }
}