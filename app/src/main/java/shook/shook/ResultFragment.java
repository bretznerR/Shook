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

import v.shook.R;

/**
 * Created by Adrien on 06/03/2018.
 */

public class ResultFragment extends Fragment {

    private RecyclerView list;

    public ResultFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_search_results, container, false);
        list = (RecyclerView) rootView.findViewById(R.id.recyclerView);
        ArrayList listMusic= new ArrayList<Music>();
        /*TEST*/
        listMusic.add(new Music("Ghost Town",new Artist("Kanye West"),null,"Ye"));
        listMusic.add(new Music("Voilà",new Artist("N.E.R.D"),null,"NO ONE EVER REALLY DIES"));
        listMusic.add(new Music("Ghost Town",new Artist("Kanye West"),null,"Ye"));
        listMusic.add(new Music("Voilà",new Artist("N.E.R.D"),null,"NO ONE EVER REALLY DIES"));
        listMusic.add(new Music("Ghost Town",new Artist("Kanye West"),null,"Ye"));
        listMusic.add(new Music("Voilà",new Artist("N.E.R.D"),null,"NO ONE EVER REALLY DIES"));
        /*FIN TEST*/
        list.setLayoutManager(new LinearLayoutManager(getActivity()));
        list.setAdapter(new CustomAdapter(listMusic,getActivity()));
        return rootView;
    }
}
