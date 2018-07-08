package shook.shook;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import v.shook.R;


/**
 * Created by Adrien on 10/06/2018.
 */

public class PlaylistsFragment extends Fragment {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_playlists, container, false);
        Toolbar topToolBar = (Toolbar)rootView.findViewById(R.id.toolbar);
        topToolBar.setTitle("Playlists");
        return rootView;
    }
}

