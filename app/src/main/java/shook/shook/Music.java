package shook.shook;

import java.util.ArrayList;

/**
 * Created by Adrien on 10/06/2018.
 */

public class Music {

    private String title;
    private Artist artist;
    private ArrayList<Artist> featuring;
    private String album;

    public Music(String title, Artist artist, ArrayList<Artist> featuring, String album) {
        this.title = title;
        this.artist = artist;
        this.featuring = featuring;
        this.album = album;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Artist getArtist() {
        return artist;
    }

    public void setArtist(Artist artist) {
        this.artist = artist;
    }

    public ArrayList<Artist> getFeaturing() {
        return featuring;
    }

    public void setFeaturing(ArrayList<Artist> featuring) {
        this.featuring = featuring;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }
}
