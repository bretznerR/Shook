package shook.shook;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.TextView;

import com.deezer.sdk.model.Permissions;
import com.deezer.sdk.network.connect.DeezerConnect;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.connect.event.DialogListener;

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

}
