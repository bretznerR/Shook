package shook.shook;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.TextView;

import com.deezer.sdk.model.Album;
import com.deezer.sdk.model.Permissions;
import com.deezer.sdk.model.Track;
import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.connect.event.DialogListener;
import com.deezer.sdk.network.request.DeezerRequest;
import com.deezer.sdk.network.request.DeezerRequestFactory;
import com.deezer.sdk.network.request.SearchResultOrder;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.network.request.event.JsonRequestListener;
import com.deezer.sdk.network.request.event.RequestListener;
import com.deezer.sdk.player.AlbumPlayer;
import com.deezer.sdk.player.exception.TooManyPlayersExceptions;
import com.deezer.sdk.player.networkcheck.WifiAndMobileNetworkStateChecker;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private TextView mTextMessage;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    showFragment(new SearchFragment());
                    return true;
                case R.id.navigation_search:
                    showFragment(new SearchFragment());
                    return true;
                case R.id.navigation_notifications:
                    showFragment(new SearchFragment());
                    return true;
            }
            return false;
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*
        *
        * Deezer
        * */
        // replace with your own Application ID
        String applicationID = "276402";
        final DeezerConnect deezerConnect = new DeezerConnect(this, applicationID);

        // The set of Deezer Permissions needed by the app
        String[] permissions = new String[] {
                Permissions.BASIC_ACCESS,
                Permissions.MANAGE_LIBRARY,
                Permissions.LISTENING_HISTORY };

        // restore any saved session
        SessionStore sessionStore = new SessionStore();
        if (sessionStore.restore(deezerConnect, this)) {
            // The restored session is valid, navigate to the Home Activity
            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
        }
        DialogListener listener = new DialogListener() {

            public void onComplete(Bundle values) {
                // store the current authentication info
                SessionStore sessionStore = new SessionStore();
                sessionStore.save(deezerConnect, MainActivity.this);
            }

            public void onCancel() {}

            public void onException(Exception e) {}
        };

        // Launches the authentication process
        deezerConnect.authorize(this, permissions, listener);


        mTextMessage = (TextView) findViewById(R.id.message);
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);
    }

    private void showFragment(Fragment fragment) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.content, fragment)
                .commit();
    }

    public void getAlbum(DeezerConnect deezerConnect, long artistId) {
        // the request listener
        RequestListener listener = new JsonRequestListener() {

            public void onResult(Object result, Object requestId) {
                List<Album> albums = (List<Album>) result;

                // do something with the albums
            }

            public void onUnparsedResult(String requestResponse, Object requestId) {}

            public void onException(Exception e, Object requestId) {}
        };

        // create the request
        //long artistId = 11472;
        DeezerRequest request = DeezerRequestFactory.requestArtistAlbums(artistId);

        // set a requestId, that will be passed on the listener's callback methods
        request.setId("myRequest");

        // launch the request asynchronously
        deezerConnect.requestAsync(request, listener);
    }

    public void playMusic(DeezerConnect deezerConnect) throws DeezerError, TooManyPlayersExceptions {
        // create the player

        AlbumPlayer albumPlayer = new AlbumPlayer(getApplication(), deezerConnect, new WifiAndMobileNetworkStateChecker());

        // start playing music
        long albumId = 89142;
        albumPlayer.playAlbum(albumId);

        // ...

        // to make sure the player is stopped (for instance when the activity is closed)
        albumPlayer.stop();
        albumPlayer.release();
    }

    public void searchBarDeezer(DeezerConnect deezerConnect, String query) {
        // the request listener
        RequestListener listener = new JsonRequestListener() {

            public void onResult(Object result, Object requestId) {
                List<Track> albums = (List<Track>) result;

                Log.d("Deezer", albums.toString());
            }

            public void onUnparsedResult(String requestResponse, Object requestId) {}

            public void onException(Exception e, Object requestId) {}
        };

        // create the request
        //long artistId = 11472;
        DeezerRequest request = DeezerRequestFactory.requestSearchTracks(query, SearchResultOrder.Ranking);

        // set a requestId, that will be passed on the listener's callback methods
        request.setId("searchQuery");

        // launch the request asynchronously
        deezerConnect.requestAsync(request, listener);
    }
}
