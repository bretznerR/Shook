package v.shook.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Looper;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.deezer.sdk.model.Permissions;
import com.deezer.sdk.network.connect.event.DialogListener;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.spotify.sdk.android.authentication.AuthenticationClient;
import com.spotify.sdk.android.authentication.AuthenticationRequest;
import com.spotify.sdk.android.authentication.AuthenticationResponse;
import retrofit.RetrofitError;
import v.shook.R;
import v.shook.library.LibraryService;
import v.shook.library.Source;

import javax.net.ssl.HttpsURLConnection;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class SourcesActivity extends AppCompatActivity
{
    private static int SPOTIFY_REQUEST_CODE = 1337;

    private SourceAdapter adapter;
    private DragSortListView.DropListener dropListener = new DragSortListView.DropListener()
    {
        @Override
        public void drop(int from, int to)
        {
            Source toSwap = adapter.sources.get(from);

            // we reduced this source priority ; priority.set(to), increase all priorities on the way by 1
            if(from < to)
            {
                toSwap.setPriority(adapter.sources.get(to).getPriority());
                from++;
                for(;from<=to;from++)
                {
                    adapter.sources.get(from).setPriority(adapter.sources.get(from).getPriority()+1);
                }
            }
            // we increased this source priority ; priority.set(to), reduce all priorities on the way by 1
            else
            {
                toSwap.setPriority(adapter.sources.get(to).getPriority());
                from--;
                for(;from>=to;from--)
                {
                    adapter.sources.get(from).setPriority(adapter.sources.get(from).getPriority()-1);
                }
            }

            Collections.sort(adapter.sources, new Comparator<Source>()
            {
                @Override
                public int compare(Source o1, Source o2)
                {
                    return o2.getPriority() - o1.getPriority();
                }
            });
            adapter.notifyDataSetChanged();

            //reload songs from source
            SharedPreferences accountsPrefs = getSharedPreferences(SettingsActivity.PREFERENCES_ACCOUNT_FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = accountsPrefs.edit();
            editor.putInt("spotify_prior", Source.SOURCE_SPOTIFY.getPriority());
            editor.putInt("deezer_prior", Source.SOURCE_DEEZER.getPriority());
            editor.apply();

            Toast.makeText(SourcesActivity.this, getText(R.string.pls_resync), Toast.LENGTH_SHORT).show();
        }
    };

    ListView.OnItemClickListener onItemClickListener = new AdapterView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            Source source = adapter.sources.get(position);
            if(source.isAvailable())
            {
                //disconnect
                source.disconnect();

                Collections.sort(adapter.sources, new Comparator<Source>() {
                    @Override
                    public int compare(Source o1, Source o2) {
                        return o2.getPriority() - o1.getPriority();
                    }
                });
                adapter.notifyDataSetChanged();

                Toast.makeText(SourcesActivity.this, getText(R.string.disconnect_ok), Toast.LENGTH_SHORT).show();

                return;
            }

            if(source == Source.SOURCE_SPOTIFY)
            {
                AuthenticationRequest.Builder builder =
                        new AuthenticationRequest.Builder(Source.SOURCE_SPOTIFY.SPOTIFY_CLIENT_ID, AuthenticationResponse.Type.CODE,
                                Source.SOURCE_SPOTIFY.SPOTIFY_REDIRECT_URI).setShowDialog(true);
                builder.setScopes(new String[]{"user-read-private", "streaming", "user-read-email", "user-follow-read",
                        "playlist-read-private", "playlist-read-collaborative", "user-library-read", "user-library-modify",
                        "playlist-modify-public", "playlist-modify-private", "user-follow-modify"});
                AuthenticationRequest request = builder.build();
                AuthenticationClient.openLoginActivity(SourcesActivity.this, SPOTIFY_REQUEST_CODE, request);
            }
            else if(source == Source.SOURCE_DEEZER)
            {
                String[] permissions = new String[] {Permissions.BASIC_ACCESS, Permissions.MANAGE_LIBRARY,
                        Permissions.EMAIL, Permissions.OFFLINE_ACCESS, Permissions.DELETE_LIBRARY,
                Permissions.MANAGE_COMMUNITY};

                //test that
                LibraryService.configureLibrary(getApplicationContext());

                Source.SOURCE_DEEZER.deezerApi.authorize(SourcesActivity.this, permissions, new DialogListener()
                {
                    @Override
                    public void onComplete(Bundle bundle)
                    {
                        Source.SOURCE_DEEZER.DEEZER_USER_SESSION.save(Source.SOURCE_DEEZER.deezerApi, SourcesActivity.this.getApplicationContext());
                        Source.SOURCE_DEEZER.setAvailable(true);
                        Source.SOURCE_DEEZER.setPriority(adapter.sources.get(0).getPriority()+1);
                        Source.SOURCE_DEEZER.me = Source.SOURCE_DEEZER.deezerApi.getCurrentUser();
                        SharedPreferences pref = getSharedPreferences(SettingsActivity.PREFERENCES_ACCOUNT_FILE_NAME, Context.MODE_PRIVATE);
                        SharedPreferences.Editor editor = pref.edit();
                        editor.putInt("deezer_prior", Source.SOURCE_DEEZER.getPriority());
                        editor.apply();
                        Collections.sort(adapter.sources, new Comparator<Source>() {
                            @Override
                            public int compare(Source o1, Source o2) {
                                return o2.getPriority() - o1.getPriority();
                            }
                        });
                        adapter.notifyDataSetChanged();
                        Source.SOURCE_DEEZER.getPlayer().init();

                        Toast.makeText(SourcesActivity.this, getText(R.string.pls_resync), Toast.LENGTH_SHORT).show();
                    }

                    @Override
                    public void onCancel() {}

                    @Override
                    public void onException(Exception e) {}
                });
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sources);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        DragSortListView sourcesListView = findViewById(R.id.sources_listview);
        adapter = new SourceAdapter(this);
        sourcesListView.setAdapter(adapter);
        DragSortController controller = new DragSortController(sourcesListView);
        controller.setDragHandleId(R.id.element_more);
        sourcesListView.setFloatViewManager(controller);
        sourcesListView.setOnTouchListener(controller);
        sourcesListView.setDropListener(dropListener);
        sourcesListView.setOnItemClickListener(onItemClickListener);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent)
    {
        super.onActivityResult(requestCode, resultCode, intent);

        if(requestCode == SPOTIFY_REQUEST_CODE)
        {
            AuthenticationResponse response = AuthenticationClient.getResponse(resultCode, intent);
            if(response.getType() == AuthenticationResponse.Type.CODE)
            {
                final String code = response.getCode();
                Thread t = new Thread()
                {
                    public void run()
                    {
                        Looper.prepare();
                        try
                        {
                            URL apiUrl = new URL("https://accounts.spotify.com/api/token");
                            HttpsURLConnection urlConnection = (HttpsURLConnection) apiUrl.openConnection();
                            urlConnection.setDoInput(true);
                            urlConnection.setDoOutput(true);
                            urlConnection.setRequestMethod("POST");

                            //write POST parameters
                            OutputStream out = new BufferedOutputStream(urlConnection.getOutputStream());
                            BufferedWriter writer = new BufferedWriter (new OutputStreamWriter(out, "UTF-8"));
                            writer.write("grant_type=authorization_code&");
                            writer.write("code=" + code + "&");
                            writer.write("redirect_uri=" + Source.SOURCE_SPOTIFY.SPOTIFY_REDIRECT_URI + "&");
                            writer.write("client_id=" + Source.SOURCE_SPOTIFY.SPOTIFY_CLIENT_ID + "&");
                            writer.write("client_secret=" + "3166d3b40ff74582b03cb23d6701c297");
                            writer.flush();
                            writer.close();
                            out.close();

                            urlConnection.connect();

                            System.out.println("[Shook] [AUTH] Result : " + urlConnection.getResponseCode() + " " + urlConnection.getResponseMessage());

                            BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                            String result = reader.readLine();
                            reader.close();
                            result = result.substring(1);
                            result = result.substring(0, result.length()-1);
                            String[] results = result.split(",");
                            for(String param : results)
                            {
                                if(param.startsWith("\"access_token\":\""))
                                {
                                    param = param.replaceFirst("\"access_token\":\"", "");
                                    param = param.replaceFirst("\"", "");
                                    Source.SOURCE_SPOTIFY.SPOTIFY_USER_TOKEN = param;
                                    SharedPreferences pref = getSharedPreferences(SettingsActivity.PREFERENCES_ACCOUNT_FILE_NAME, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = pref.edit();
                                    editor.putString("spotify_token", Source.SOURCE_SPOTIFY.SPOTIFY_USER_TOKEN);
                                    editor.commit();
                                }
                                else if(param.startsWith("\"refresh_token\":\""))
                                {
                                    param = param.replaceFirst("\"refresh_token\":\"", "");
                                    param = param.replaceFirst("\"", "");
                                    Source.SOURCE_SPOTIFY.SPOTIFY_REFRESH_TOKEN = param;
                                    SharedPreferences pref = getSharedPreferences(SettingsActivity.PREFERENCES_ACCOUNT_FILE_NAME, Context.MODE_PRIVATE);
                                    SharedPreferences.Editor editor = pref.edit();
                                    editor.putString("spotify_refresh_token", Source.SOURCE_SPOTIFY.SPOTIFY_REFRESH_TOKEN);
                                    editor.commit();
                                }
                            }

                            Source.SOURCE_SPOTIFY.setAvailable(true);
                            Source.SOURCE_SPOTIFY.setPriority(adapter.sources.get(0).getPriority()+1);
                            SharedPreferences pref = getSharedPreferences(SettingsActivity.PREFERENCES_ACCOUNT_FILE_NAME, Context.MODE_PRIVATE);
                            SharedPreferences.Editor editor = pref.edit();
                            editor.putInt("spotify_prior", Source.SOURCE_SPOTIFY.getPriority());
                            editor.apply();
                            Collections.sort(adapter.sources, new Comparator<Source>() {
                                @Override
                                public int compare(Source o1, Source o2) {
                                    return o2.getPriority() - o1.getPriority();
                                }
                            });
                            adapter.notifyDataSetChanged();
                            Source.SOURCE_SPOTIFY.spotifyApi.setAccessToken(Source.SOURCE_SPOTIFY.SPOTIFY_USER_TOKEN);

                            try
                            {
                                Source.SOURCE_SPOTIFY.mePrivate = Source.SOURCE_SPOTIFY.spotifyApi.getService().getMe();
                            }
                            catch(RetrofitError e)
                            {
                                e.printStackTrace();
                            }

                            Source.SOURCE_SPOTIFY.getPlayer().init();
                            Toast.makeText(SourcesActivity.this, getText(R.string.pls_resync), Toast.LENGTH_SHORT).show();
                        }
                        catch(Exception e)
                        {
                            e.printStackTrace();
                        }
                    }
                };
                t.start();
            }
            else
            {
                System.err.println("Wrong reponse received.\n");
                System.err.println("Error : " + response.getError());
            }
        }
    }

    public static class SourceAdapter extends BaseAdapter
    {
        public ArrayList<Source> sources;
        private Context context;

        class ViewHolder
        {
            ImageView source;
            TextView status;
            ImageView more;
        }

        public SourceAdapter(Context context)
        {
            this.context = context;

            sources = new ArrayList<>();
            sources.add(Source.SOURCE_SPOTIFY);
            sources.add(Source.SOURCE_DEEZER);
            Collections.sort(sources, new Comparator<Source>() {
                @Override
                public int compare(Source o1, Source o2) {
                    return o2.getPriority() - o1.getPriority();
                }
            });
        }

        @Override
        public int getCount() {return sources.size();}
        @Override
        public Object getItem(int position) {return sources.get(position);}
        @Override
        public long getItemId(int position) {return position;}

        @Override
        public View getView(int position, View convertView, ViewGroup parent)
        {
            ViewHolder mViewHolder;

            if(convertView == null)
            {
                mViewHolder = new ViewHolder();

                //map to layout
                convertView = LayoutInflater.from(context).inflate(R.layout.sources_list_layout, parent, false);

                //get imageview
                mViewHolder.source = convertView.findViewById(R.id.element_image);
                mViewHolder.status = convertView.findViewById(R.id.element_subtitle);

                convertView.setTag(mViewHolder);
            }
            else mViewHolder = (ViewHolder) convertView.getTag();

            Source source = sources.get(position);
            mViewHolder.source.setImageResource(source.getLogoImage());
            mViewHolder.status.setText(source.isAvailable() ? context.getString(R.string.connected) + " (" + source.getUserName() + ")" : context.getString(R.string.disconnected));
            return convertView;
        }
    }
}
