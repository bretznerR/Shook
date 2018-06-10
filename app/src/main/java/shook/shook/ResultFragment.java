package shook.shook;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
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
        ArrayList listMusic= new ArrayList<Music>();
        /*TEST*/
        listMusic.add(new Music("Ghost Town",new Artist("Kanye West"),null,"Ye"));
        /*FIN TEST*/
        CustomAdapter adapter = new CustomAdapter(listMusic,getActivity());
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setAdapter(adapter);
        return rootView;
    }
}
