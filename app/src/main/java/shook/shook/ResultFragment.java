package shook.shook;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;

/**
 * Created by Adrien on 06/03/2018.
 */

public class ResultFragment extends Fragment {

    RecyclerView list;

    public ResultFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_search_results, container, false);
        list = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        ArrayList stringList= new ArrayList();
        CustomAdapter adapter = new CustomAdapter(stringList,getActivity());
        list.setAdapter(adapter);
        return rootView;
    }
}
