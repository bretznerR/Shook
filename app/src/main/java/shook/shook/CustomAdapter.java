package shook.shook;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

import v.shook.R;

/**
 * Created by Adrien on 06/03/2018.
 */

public class CustomAdapter extends RecyclerView.Adapter<CustomAdapter.MyViewHolder> {

    private LayoutInflater inflater;
    private Context context;
    private ArrayList<Music> musics;

    public CustomAdapter(ArrayList<Music> list,Context context) {
        inflater = LayoutInflater.from(context);
        this.context = context;
        this.musics=list;
        System.out.println(this.musics);
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.activity_search_result_item, parent, false);
        System.out.println("AHAHHA : "+view.findViewById(R.id.music_artistalbum));
        return new MyViewHolder(view);
    }


    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        Music music = this.musics.get(position);
        holder.display(music);
    }

    @Override
    public int getItemCount() {
        return this.musics.size();
    }

    class MyViewHolder extends RecyclerView.ViewHolder {

        private TextView title;
        private TextView artist_album;
        private Music currentMusic;

        public MyViewHolder(View itemView) {
            super(itemView);
            this.title = ((TextView) itemView.findViewById(R.id.music_title));
            this.artist_album = ((TextView) itemView.findViewById(R.id.music_artistalbum));
        }

        public void display(Music music) {
            this.currentMusic = music;
            this.title.setText(music.getTitle());
            this.artist_album.setText(music.getArtist().getName()+" - "+music.getAlbum());

        }
    }

}