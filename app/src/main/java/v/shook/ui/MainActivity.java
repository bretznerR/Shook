/*
 *    Shook - Android music player
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package v.shook.ui;

import android.Manifest;
import android.app.Activity;
import android.app.SearchManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.*;
import android.widget.*;
import v.shook.R;
import v.shook.library.*;
import v.shook.player.PlayerService;
import v.shook.ui.adapters.LibraryObjectAdapter;
import v.shook.ui.settings.SettingsActivity;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener
{
    private static final int EXT_PERM_REQUEST_CODE = 0x42;

    /* music controller and callbacks */
    private PlayerService musicPlayer;
    private boolean musicCallbacksRegistered = false;
    private MediaControllerCompat.Callback musicCallbacks = new MediaControllerCompat.Callback()
    {
        @Override
        public void onPlaybackStateChanged(PlaybackStateCompat state)
        {
            showCurrentPlay(PlayerConnection.getService().getCurrentSong(), PlayerConnection.getService().isPlaying());
        }

        @Override
        public void onMetadataChanged(MediaMetadataCompat metadata)
        {
            showCurrentPlay(PlayerConnection.getService().getCurrentSong(), PlayerConnection.getService().isPlaying());
        }
    };
    private PlayerConnection.Callback connectionCallbacks = new PlayerConnection.Callback()
    {
        @Override
        public void onConnected()
        {
            musicPlayer = PlayerConnection.getService();

            if(!musicCallbacksRegistered)
            {
                PlayerConnection.musicController.registerCallback(musicCallbacks);
                musicCallbacksRegistered = true;
            }
        }

        @Override
        public void onDisconnected()
        {
            musicPlayer = null;
            hideCurrentPlay();
        }
    };

    /* current activity context (instanceState) */
    private static final int CONTEXT_NONE = 0;
    private static final int CONTEXT_ARTISTS = 1;
    private static final int CONTEXT_ALBUMS = 2;
    private static final int CONTEXT_SONGS = 3;
    private static final int CONTEXT_PLAYLISTS = 4;
    private static final int CONTEXT_SEARCH = 5;
    private int currentContext = CONTEXT_NONE;

    /* specific context (back button) handling */
    private static Bundle backBundle, back2Bundle;
    private static LibraryObject backObject, back2Object;
    private static boolean fromPlaylists;
    private static boolean globalSearch = false;
    private static LibraryObject currentObject = null;

    /* currently playing display */
    private RelativeLayout currentPlay;
    private TextView currentPlayTitle;
    private TextView currentPlaySubtitle;
    private ImageView currentPlayImage;
    private ImageView currentPlayAction;
    private boolean currentPlayShown = false;
    private boolean needShowCurrentPlay = false;
    private SearchView searchView;
    private MenuItem syncButton;

    /* main list view */
    private ListView mainListView;
    private ListView.OnItemClickListener mainListViewListener = new ListView.OnItemClickListener()
    {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id)
        {
            switch(currentContext)
            {
                case CONTEXT_SONGS:
                    ArrayList<Song> songs = new ArrayList<Song>(((LibraryObjectAdapter)mainListView.getAdapter()).getObjectList());
                    setPlaylist(songs, position);
                    break;
                case CONTEXT_ARTISTS:
                    backBundle = new Bundle(); saveInstanceState(backBundle); backObject = null;
                    Artist currentArtist = (Artist) ((LibraryObjectAdapter)mainListView.getAdapter()).getObjects().get(position);
                    ArrayList<Album> albums = currentArtist.getAlbums();
                    currentObject = currentArtist;
                    setContentToAlbums(albums, currentArtist.getName());
                    break;
                case CONTEXT_ALBUMS:
                    if(backBundle == null) {backBundle = new Bundle(); saveInstanceState(backBundle); backObject = currentObject;}
                    else {back2Bundle = new Bundle(); saveInstanceState(back2Bundle); back2Object = currentObject;}
                    Album currentAlbum = (Album) ((LibraryObjectAdapter)mainListView.getAdapter()).getObjects().get(position);
                    ArrayList<Song> asongs = currentAlbum.getSongs();
                    currentObject = currentAlbum;
                    setContentToSongs(asongs, currentAlbum.getName());
                    break;
                case CONTEXT_PLAYLISTS:
                    fromPlaylists = true;
                    backBundle = new Bundle(); saveInstanceState(backBundle); backObject = currentObject;
                    Playlist currentPlaylist = (Playlist) ((LibraryObjectAdapter)mainListView.getAdapter()).getObjects().get(position);
                    ArrayList<Song> psongs = currentPlaylist.getContent();
                    currentObject = currentPlaylist;
                    setContentToSongs(psongs, currentPlaylist.getName());
                    break;
                case CONTEXT_SEARCH:
                    currentObject = null;
                    LibraryObject selected = ((LibraryObjectAdapter)mainListView.getAdapter()).getObjects().get(position);
                    if(selected instanceof Artist)
                        setContentToAlbums(((Artist) selected).getAlbums(), selected.getName());
                    else if(selected instanceof Album)
                        setContentToSongs(((Album) selected).getSongs(), selected.getName());
                    else if(selected instanceof Playlist)
                        setContentToSongs(((Playlist) selected).getContent(), selected.getName());
                    else if(selected instanceof Song)
                    {
                        ArrayList<Song> playlist = new ArrayList<Song>();
                        playlist.add((Song) selected);
                        setPlaylist(playlist, 0);
                    }
                    break;
            }
        }
    };
    private ImageView.OnClickListener mainListViewMoreListener = new View.OnClickListener()
    {
        @Override
        public void onClick(View v)
        {
            final LibraryObject object = (LibraryObject) v.getTag();

            PopupMenu popupMenu = new PopupMenu(MainActivity.this, v);

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener()
            {
                @Override
                public boolean onMenuItemClick(MenuItem item)
                {
                    switch(item.getItemId())
                    {
                        case R.id.action_play:
                            ArrayList<Song> playlist = new ArrayList<Song>();
                            if(object instanceof Song) playlist.add((Song) object);
                            else if(object instanceof Album) playlist.addAll(((Album) object).getSongs());
                            else if(object instanceof Artist) for(Album a : ((Artist) object).getAlbums()) playlist.addAll(a.getSongs());
                            else if(object instanceof Playlist) playlist.addAll(((Playlist) object).getContent());
                            setPlaylist(playlist, 0);
                            break;

                        case R.id.action_play_next:
                            ArrayList<Song> playlist1 = new ArrayList<Song>();
                            if(object instanceof Song) playlist1.add((Song) object);
                            else if(object instanceof Album) playlist1.addAll(((Album) object).getSongs());
                            else if(object instanceof Artist) for(Album a : ((Artist) object).getAlbums()) playlist1.addAll(a.getSongs());
                            else if(object instanceof Playlist) playlist1.addAll(((Playlist) object).getContent());
                            playNext(playlist1);
                            Toast.makeText(MainActivity.this, playlist1.size() + " " + getString(R.string.added_next_ok), Toast.LENGTH_SHORT).show();
                            break;

                        case R.id.action_add_to_playlist:
                            ArrayList<Song> playlist2 = new ArrayList<Song>();
                            if(object instanceof Song) playlist2.add((Song) object);
                            else if(object instanceof Album) playlist2.addAll(((Album) object).getSongs());
                            else if(object instanceof Artist) for(Album a : ((Artist) object).getAlbums()) playlist2.addAll(a.getSongs());
                            else if(object instanceof Playlist) playlist2.addAll(((Playlist) object).getContent());
                            addToPlaylist(playlist2);
                            Toast.makeText(MainActivity.this, playlist2.size() + " " + getString(R.string.added_ok), Toast.LENGTH_SHORT).show();
                            break;

                        case R.id.action_add_to_list:
                            if(currentContext == CONTEXT_SONGS && fromPlaylists)
                            {
                                Playlist p = ((Playlist) currentObject);
                                p.getSources().getSourceByPriority(0).getSource()
                                        .removeSongFromPlaylist((Song) object, p, new Source.OperationCallback()
                                        {
                                            @Override
                                            public void onSucess(LibraryObject result)
                                            {
                                                runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        Toast.makeText(MainActivity.this, ((Song) object).getTitle() + " " + getString(R.string.delete_from_playlist_ok) + " " + p.getName(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure()
                                            {
                                                runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        Toast.makeText(MainActivity.this, ((Song) object).getTitle() + " " + getString(R.string.delete_from_playlist_fail) + " " + p.getName(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        });
                            }
                            else showAddToPlaylist(MainActivity.this, object);
                            break;

                        case R.id.action_manage_libraries:
                            if(currentContext == CONTEXT_PLAYLISTS)
                            {
                                Playlist p = ((Playlist) object);
                                AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this)
                                        .setTitle(getString(R.string.delete))
                                        .setMessage(R.string.are_you_sure_delete)
                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                dialog.cancel();
                                            }
                                        })
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                p.getSources().getSourceByPriority(0).getSource().
                                                        removePlaylist(p, new Source.OperationCallback()
                                                {
                                                    @Override
                                                    public void onSucess(LibraryObject result)
                                                    {
                                                        runOnUiThread(new Runnable()
                                                        {
                                                            @Override
                                                            public void run()
                                                            {
                                                                Toast.makeText(MainActivity.this, ((Playlist) object).getName() + " " + getString(R.string.delete_ok), Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }

                                                    @Override
                                                    public void onFailure()
                                                    {
                                                        runOnUiThread(new Runnable()
                                                        {
                                                            @Override
                                                            public void run()
                                                            {
                                                                Toast.makeText(MainActivity.this, ((Playlist) object).getName() + " " + getString(R.string.delete_fail), Toast.LENGTH_SHORT).show();
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        });
                                AlertDialog dialog = builder.create();
                                dialog.setOnShowListener(new DialogInterface.OnShowListener()
                                {
                                    @Override
                                    public void onShow(DialogInterface arg0)
                                    {
                                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                                    }
                                });
                                dialog.show();
                            }
                            else showManageLibraries(MainActivity.this, object);
                            break;
                    }
                    return false;
                }
            });
            getMenuInflater().inflate(R.menu.menu_object_more, popupMenu.getMenu());

            if(currentContext == CONTEXT_PLAYLISTS)
            {
                popupMenu.getMenu().findItem(R.id.action_add_to_list).setVisible(false);
                popupMenu.getMenu().findItem(R.id.action_manage_libraries).setTitle(R.string.delete);
            }
            else if(currentContext == CONTEXT_SONGS && fromPlaylists)
            {
                popupMenu.getMenu().findItem(R.id.action_add_to_list).setTitle(getString(R.string.remove_from_playlist));
            }

            if(currentContext != CONTEXT_SONGS && currentContext != CONTEXT_PLAYLISTS && currentContext != CONTEXT_SEARCH)
                popupMenu.getMenu().findItem(R.id.action_manage_libraries).setVisible(false);

            popupMenu.show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        mainListView = (ListView) findViewById(R.id.libraryList);
        mainListView.setOnItemClickListener(mainListViewListener);

        currentPlay = (RelativeLayout) findViewById(R.id.currentPlay);
        currentPlay.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, PlayActivity.class);
                startActivity(intent);
            }
        });
        currentPlayTitle = currentPlay.findViewById(R.id.element_title);
        currentPlaySubtitle = currentPlay.findViewById(R.id.element_subtitle);
        currentPlayImage = currentPlay.findViewById(R.id.element_image);
        currentPlayAction = currentPlay.findViewById(R.id.element_action);
        currentPlayAction.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if(musicPlayer.isPlaying()) PlayerConnection.musicController.getTransportControls().pause();
                else PlayerConnection.musicController.getTransportControls().play();
            }
        });

        restoreInstanceState(savedInstanceState, currentObject);

        //delay currentPlay showing
        mainListView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener()
        {
            @Override
            public void onGlobalLayout()
            {
                if(needShowCurrentPlay)
                {
                    showCurrentPlay(musicPlayer.getCurrentSong(), musicPlayer.isPlaying());
                    needShowCurrentPlay = false;
                    mainListView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                }
            }
        });
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        PlayerConnection.init(connectionCallbacks, getApplicationContext());
        LibraryService.configureLibrary(getApplicationContext());
        checkPermission();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();
        musicCallbacksRegistered = false;
    }

    @Override
    public void onBackPressed()
    {
        // Handle drawer close
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START))
        {
            drawer.closeDrawer(GravityCompat.START);
        }
        else if(back2Bundle != null)
        {
            restoreInstanceState(back2Bundle, back2Object);
            back2Bundle = null;
        }
        else if(backBundle != null)
        {
            restoreInstanceState(backBundle, backObject);
            backBundle = null;
        }
        else
        {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();
        searchView.setSearchableInfo(searchManager.getSearchableInfo(getComponentName()));
        searchView.setQueryHint(getString(R.string.search_lib));

        syncButton = menu.findItem(R.id.action_sync);

        return true;
    }

    @Override
    protected void onNewIntent(Intent intent)
    {
        if(Intent.ACTION_SEARCH.equals(intent.getAction()))
        {
            final String query = intent.getStringExtra(SearchManager.QUERY);
            if(globalSearch)
            {
                new Thread()
                {
                    public void run()
                    {
                        final ArrayList<LibraryObject> objects = LibraryService.queryWeb(query);
                        runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                setContentToSearch(objects);
                            }
                        });
                    }
                }.start();
            }
            else setContentToSearch(LibraryService.query(query));
        }
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings)
        {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        }
        else if(id == R.id.action_sync)
        {
            if(LibraryService.synchronization)
            {
                LibraryService.syncThread.interrupt();
                syncButton.setIcon(R.drawable.ic_sync);
                LibraryService.registerInit();
                return true;
            }

            syncButton.setIcon(R.drawable.ic_cancel);
            //devices with little screens : change name
            syncButton.setTitle(R.string.cancel);

            LibraryService.synchronizeLibrary(new LibraryService.SynchronizeCallback()
            {
                @Override
                public void synchronizeDone()
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            syncButton.setIcon(R.drawable.ic_sync);
                            syncButton.setTitle("Synchronisation");
                        }
                    });
                }
                @Override
                public void synchronizeFail(int error)
                {
                    runOnUiThread(new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            syncButton.setIcon(R.drawable.ic_sync);
                            syncButton.setTitle("Synchronisation");
                            switch (error)
                            {
                                case LibraryService.ERROR_LOADING_NOT_DONE:
                                    Toast.makeText(MainActivity.this, getText(R.string.sync_fail_load), Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        }
                    });
                }
            });
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item)
    {
        // Handle navigation drawer action
        int id = item.getItemId();

        switch(id)
        {
            case R.id.nav_search:
                // Enable web search
                globalSearch = true;
                // Request SearchView focus
                searchView.setIconified(false);
                searchView.setQueryHint(getString(R.string.search_web));
                // Set to empty activity
                fromPlaylists = false; currentObject = null; backBundle = null; back2Bundle = null;
                setContentToSearch(new ArrayList<LibraryObject>());
                break;

            case R.id.nav_artists:
                globalSearch = false; searchView.setQueryHint(getString(R.string.search_lib));
                fromPlaylists = false; currentObject = null; backBundle = null; back2Bundle = null;
                // Replace current activity content with artist list
                setContentToArtists();
                break;

            case R.id.nav_albums:
                globalSearch = false; searchView.setQueryHint(getString(R.string.search_lib));
                fromPlaylists = false; currentObject = null; backBundle = null; back2Bundle = null;
                // Replace current activity content with album view
                setContentToAlbums(LibraryService.getAlbums(), getResources().getString(R.string.albums));
                break;

            case R.id.nav_songs:
                globalSearch = false; searchView.setQueryHint(getString(R.string.search_lib));
                fromPlaylists = false; currentObject = null; backBundle = null; back2Bundle = null;
                // Replace current activity content with song list
                setContentToSongs(LibraryService.getSongs(), getResources().getString(R.string.songs));
                break;

            case R.id.nav_playlists:
                globalSearch = false; searchView.setQueryHint(getString(R.string.search_lib));
                fromPlaylists = false; currentObject = null; backBundle = null; back2Bundle = null;
                // Replace current activity content with playlist list
                setContentToPlaylists();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    /* Perform permission check and read library */
    private void checkPermission()
    {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M)
        {
            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED)
            {
                if(shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE))
                {
                    // Show an alert dialog
                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage(getString(R.string.please_grant_permission_msg));
                    builder.setTitle(getString(R.string.please_grant_permission_title));
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener()
                    {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i)
                        {
                            ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXT_PERM_REQUEST_CODE);
                        }
                    });
                    builder.setNeutralButton("Cancel",null);
                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                else
                {
                    // Request permission
                    ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, EXT_PERM_REQUEST_CODE);
                }
            }
            else startLibService();
        }
        else startLibService();
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == EXT_PERM_REQUEST_CODE) startLibService();
    }
    public void startLibService()
    {
        if(LibraryService.getArtists().size() == 0)
        {
            Intent service = new Intent(this, LibraryService.class);
            startService(service);

            LibraryService.registerInit();
        }
        if(currentContext == CONTEXT_NONE) setContentToArtists();
    }

    /* UI Change methods (Artists/Albums/Songs/Playlists...) */
    private void setContentToArtists()
    {
        this.setTitle(getResources().getString(R.string.artists));
        currentContext = CONTEXT_ARTISTS;

        LibraryObjectAdapter adapter = new LibraryObjectAdapter(this, LibraryService.getArtists());
        adapter.registerMoreClickListener(mainListViewMoreListener);
        mainListView.setAdapter(adapter);
    }
    private void setContentToAlbums(List<Album> albums, String title)
    {
        this.setTitle(title);
        currentContext = CONTEXT_ALBUMS;
        LibraryObjectAdapter adapter = new LibraryObjectAdapter(this, albums);
        adapter.registerMoreClickListener(mainListViewMoreListener);
        mainListView.setAdapter(adapter);
    }
    private void setContentToSongs(List<Song> songs, String title)
    {
        this.setTitle(title);
        currentContext = CONTEXT_SONGS;
        LibraryObjectAdapter adapter = new LibraryObjectAdapter(this, songs);
        adapter.registerMoreClickListener(mainListViewMoreListener);
        mainListView.setAdapter(adapter);
    }
    private void setContentToPlaylists()
    {
        this.setTitle(getResources().getString(R.string.playlists));
        currentContext = CONTEXT_PLAYLISTS;
        LibraryObjectAdapter adapter = new LibraryObjectAdapter(this, LibraryService.getPlaylists());
        adapter.registerMoreClickListener(mainListViewMoreListener);
        mainListView.setAdapter(adapter);
    }
    private void setContentToSearch(ArrayList<LibraryObject> searchResult)
    {
        currentObject = null; fromPlaylists = false;
        this.setTitle(getResources().getString(R.string.action_search));
        currentContext = CONTEXT_SEARCH;
        LibraryObjectAdapter adapter = new LibraryObjectAdapter(this, searchResult);
        adapter.registerMoreClickListener(mainListViewMoreListener);
        mainListView.setAdapter(adapter);
    }

    /* currently playing */
    private void showCurrentPlay(Song song, boolean play)
    {
        if(!currentPlayShown)
        {
            mainListView.getLayoutParams().height = mainListView.getHeight() - currentPlay.getHeight();
            mainListView.requestLayout();

            //show
            currentPlay.setVisibility(View.VISIBLE);
            currentPlayShown = true;
        }

        // update informations
        currentPlayTitle.setText(song.getTitle());
        currentPlaySubtitle.setText(song.getArtist().getName() + " - " + song.getAlbum().getName());
        if(song.getAlbum().hasAlbumArt()) currentPlayImage.setImageBitmap(song.getAlbum().getAlbumArtMiniature());
        else currentPlayImage.setImageResource(R.drawable.ic_albums);

        if(play) currentPlayAction.setImageResource(R.drawable.ic_action_pause);
        else currentPlayAction.setImageResource(R.drawable.ic_play_action);
    }
    private void hideCurrentPlay()
    {
        if(currentPlayShown)
        {
            currentPlay.setVisibility(View.INVISIBLE);
            mainListView.getLayoutParams().height = LinearLayout.LayoutParams.MATCH_PARENT;
            mainListView.requestLayout();
            currentPlayShown = false;
        }
    }

    /* actions */
    private void setPlaylist(ArrayList<Song> songs, int currentPos)
    {
        if(songs.size() == 0)
        {
            Toast.makeText(this, getText(R.string.empty), Toast.LENGTH_SHORT).show();
            return;
        }

        if(musicPlayer == null) PlayerConnection.start(songs, currentPos);
        else musicPlayer.setCurrentPlaylist(songs, currentPos);
    }
    private void playNext(ArrayList<Song> songs)
    {
        if(musicPlayer == null) PlayerConnection.start(songs, 0);
        else musicPlayer.addNextToPlaylist(songs);
    }
    private void addToPlaylist(ArrayList<Song> songs)
    {
        if(musicPlayer == null) PlayerConnection.start(songs, 0);
        else musicPlayer.addToPlaylist(songs);
    }

    @Override
    public void onSaveInstanceState(Bundle outState)
    {
        saveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    private void saveInstanceState(Bundle bundle)
    {
        if(bundle == null) return;

        Log.println(Log.WARN, "Shook-DEBUG", "SaveInstanceState : " + currentObject);

        bundle.putInt("currentContext", currentContext);
        bundle.putBoolean("fromPlaylists", fromPlaylists);
        bundle.putInt("listSelection", mainListView.getFirstVisiblePosition());
        bundle.putBoolean("currentPlayShown", currentPlayShown);
    }
    private void restoreInstanceState(Bundle bundle, LibraryObject currentObject)
    {
        if(bundle == null) return;

        int restoreContext = bundle.getInt("currentContext");
        fromPlaylists = bundle.getBoolean("fromPlaylists");

        MainActivity.currentObject = currentObject;

        switch(restoreContext)
        {
            case CONTEXT_ARTISTS:
                setContentToArtists();
                break;

            case CONTEXT_ALBUMS:
                if(currentObject == null) setContentToAlbums(LibraryService.getAlbums(), getString(R.string.albums));
                else setContentToAlbums(((Artist) currentObject).getAlbums(), ((Artist) currentObject).getName());
                break;

            case CONTEXT_SONGS:
                if(currentObject == null) setContentToSongs(LibraryService.getSongs(), getString(R.string.songs));
                else if(fromPlaylists) setContentToSongs(((Playlist) currentObject).getContent(), ((Playlist) currentObject).getName());
                else setContentToSongs(((Album) currentObject).getSongs(), ((Album) currentObject).getName());
                break;

            case CONTEXT_PLAYLISTS:
                setContentToPlaylists();
                break;
        }

        mainListView.setSelection(bundle.getInt("listSelection"));

        if(bundle.getBoolean("currentPlayShown") && PlayerConnection.getService() != null)
        {
            needShowCurrentPlay = true;
        }
    }

    /* shared dialogs */
    static void showAddToPlaylist(Activity context, LibraryObject object)
    {
        List<Song> toAdd = new ArrayList<>();
        if(object instanceof Song) toAdd.add((Song) object);
        else if(object instanceof Album) toAdd.addAll(((Album) object).getSongs());
        else if(object instanceof Artist) for(Album a : ((Artist) object).getAlbums()) toAdd.addAll(a.getSongs());
        else if(object instanceof Playlist) toAdd.addAll(((Playlist) object).getContent());

        List<Playlist> list = new ArrayList<>(LibraryService.getPlaylists());
        for(int i = 0;i<list.size();i++)
        {Playlist p = list.get(i); if(!p.isMine() && !p.isCollaborative()) list.remove(i);}
        list.add(0, new Playlist(context.getString(R.string.new_playlist), null));

        LibraryObjectAdapter adapter = new LibraryObjectAdapter(context, list);
        adapter.setHideMore(true);
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(context.getString(R.string.add_to_playlist))
                .setAdapter(adapter,
                        new DialogInterface.OnClickListener()
                        {
                            @Override
                            public void onClick(DialogInterface dialog, int which)
                            {
                                if(which == 0) //new playlist
                                {
                                    showAddPlaylist(context, new AddPlaylistCallback()
                                    {
                                        @Override
                                        public void onSuccess(Playlist result)
                                        {
                                            result.getSources().getSourceByPriority(0).getSource().
                                                    addSongsToPlaylist(toAdd, result, new Source.OperationCallback()
                                                    {
                                                        @Override
                                                        public void onSucess(LibraryObject result0)
                                                        {
                                                            context.runOnUiThread(new Runnable()
                                                            {
                                                                @Override
                                                                public void run()
                                                                {
                                                                    Toast.makeText(context, toAdd.size() + " " + context.getString(R.string.added_ok) + " " + result.getName(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }
                                                        @Override
                                                        public void onFailure()
                                                        {
                                                            context.runOnUiThread(new Runnable()
                                                            {
                                                                @Override
                                                                public void run()
                                                                {
                                                                    Toast.makeText(context, context.getString(R.string.added_fail) + " " + result.getName(), Toast.LENGTH_SHORT).show();
                                                                }
                                                            });
                                                        }
                                                    });
                                        }
                                    });
                                    return;
                                }

                                Playlist clicked = list.get(which);
                                clicked.getSources().getSourceByPriority(0).getSource()
                                        .addSongsToPlaylist(toAdd, clicked, new Source.OperationCallback() {
                                            @Override
                                            public void onSucess(LibraryObject result)
                                            {
                                                context.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        Toast.makeText(context, toAdd.size() + " " + context.getString(R.string.added_ok) + " " + clicked.getName(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }

                                            @Override
                                            public void onFailure()
                                            {
                                                context.runOnUiThread(new Runnable()
                                                {
                                                    @Override
                                                    public void run()
                                                    {
                                                        Toast.makeText(context, context.getString(R.string.added_fail) + " " + clicked.getName(), Toast.LENGTH_SHORT).show();
                                                    }
                                                });
                                            }
                                        });
                            }
                        })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.cancel();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface arg0)
            {
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
            }
        });
        dialog.show();
    }
    static void showManageLibraries(Activity context, LibraryObject object)
    {
        if(!(object instanceof Song)) return;

        /* create special source adapter */
        BaseAdapter sourceAdapter = new BaseAdapter()
        {
            class ViewHolder
            {
                SwitchCompat checkBox;
            }

            @Override
            public int getCount() {return Source.SOURCES.length;}
            @Override
            public Object getItem(int position) {return Source.SOURCES[position];}
            @Override
            public long getItemId(int position) {return position;}

            @Override
            public View getView(int position, View convertView, ViewGroup parent)
            {
                ViewHolder viewHolder;

                if(convertView == null)
                {
                    viewHolder = new ViewHolder();

                    //map to layout
                    convertView = LayoutInflater.from(context).inflate(R.layout.library_list_layout, parent, false);

                    //get imageview
                    viewHolder.checkBox = convertView.findViewById(R.id.element_check);

                    convertView.setTag(viewHolder);
                }
                else viewHolder = (ViewHolder) convertView.getTag();

                viewHolder.checkBox.setText(Source.SOURCES[position].getName());
                SongSources.SongSource thisSource = ((Song) object).getSources().getSourceByAbsolutePriority(position);
                viewHolder.checkBox.setChecked(thisSource != null && thisSource.getLibrary());

                //disable 'add to library' on local context (only allow to remove from local)
                if(position == 0 && !viewHolder.checkBox.isChecked())
                    viewHolder.checkBox.setEnabled(false);

                //handle actions
                viewHolder.checkBox.setOnClickListener(new View.OnClickListener()
                {
                    @Override
                    public void onClick(View view)
                    {
                        Source source = Source.SOURCES[position];
                        if(viewHolder.checkBox.isChecked())
                            source.addSongToLibrary((Song) object, new Source.OperationCallback()
                            {
                                @Override
                                public void onSucess(LibraryObject result)
                                {
                                    context.runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Toast.makeText(context, object.getName() + " " + context.getString(R.string.library_added), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                @Override
                                public void onFailure()
                                {
                                    context.runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Toast.makeText(context, object.getName() + " " + context.getString(R.string.library_add_fail), Toast.LENGTH_SHORT).show();
                                            viewHolder.checkBox.setChecked(false);
                                        }
                                    });
                                }
                            });
                        else
                            if(source == Source.SOURCE_LOCAL_LIB)
                            {
                                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                                        .setTitle(context.getString(R.string.delete))
                                        .setMessage(R.string.are_you_sure_delete)
                                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                viewHolder.checkBox.setChecked(true);
                                                dialog.cancel();
                                            }
                                        })
                                        .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener()
                                        {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which)
                                            {
                                                source.removeSongFromLibrary((Song) object, new Source.OperationCallback()
                                                {
                                                    @Override
                                                    public void onSucess(LibraryObject result)
                                                    {
                                                        context.runOnUiThread(new Runnable()
                                                        {
                                                            @Override
                                                            public void run()
                                                            {
                                                                Toast.makeText(context, object.getName() + " " + context.getString(R.string.library_removed), Toast.LENGTH_SHORT).show();
                                                                if(source == Source.SOURCE_LOCAL_LIB) viewHolder.checkBox.setEnabled(false);
                                                            }
                                                        });
                                                    }
                                                    @Override
                                                    public void onFailure()
                                                    {
                                                        context.runOnUiThread(new Runnable()
                                                        {
                                                            @Override
                                                            public void run()
                                                            {
                                                                Toast.makeText(context, object.getName() + " " + context.getString(R.string.library_remove_fail), Toast.LENGTH_SHORT).show();
                                                                viewHolder.checkBox.setChecked(true);
                                                            }
                                                        });
                                                    }
                                                });
                                            }
                                        });
                                AlertDialog dialog = builder.create();
                                dialog.setOnShowListener(new DialogInterface.OnShowListener()
                                {
                                    @Override
                                    public void onShow(DialogInterface arg0)
                                    {
                                        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                                        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                                    }
                                });
                                dialog.show();
                            }
                            else
                            source.removeSongFromLibrary((Song) object, new Source.OperationCallback()
                            {
                                @Override
                                public void onSucess(LibraryObject result)
                                {
                                    context.runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Toast.makeText(context, object.getName() + " " + context.getString(R.string.library_removed), Toast.LENGTH_SHORT).show();
                                            if(source == Source.SOURCE_LOCAL_LIB) viewHolder.checkBox.setEnabled(false);
                                        }
                                    });
                                }
                                @Override
                                public void onFailure()
                                {
                                    context.runOnUiThread(new Runnable()
                                    {
                                        @Override
                                        public void run()
                                        {
                                            Toast.makeText(context, object.getName() + " " + context.getString(R.string.library_remove_fail), Toast.LENGTH_SHORT).show();
                                            viewHolder.checkBox.setChecked(true);
                                        }
                                    });
                                }
                            });
                    }
                });

                return convertView;
            }
        };

        /* create dialog */
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setTitle(R.string.manage_libraries)
                .setAdapter(sourceAdapter, new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {}
                })
                .setPositiveButton("OK", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which)
                    {
                        dialog.dismiss();
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface arg0)
            {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
            }
        });
        dialog.show();
    }

    interface AddPlaylistCallback {void onSuccess(Playlist result);}
    static void showAddPlaylist(Activity context, AddPlaylistCallback callback)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.new_playlist);
        builder.setView(R.layout.add_playlist_dialog);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface dialog, int which) {dialog.cancel();}
        });

        AlertDialog dialog = builder.create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener()
        {
            @Override
            public void onShow(DialogInterface arg0)
            {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(Color.BLACK);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.BLACK);
                Spinner listView = dialog.findViewById(R.id.playlist_source);
                listView.setAdapter(new BaseAdapter()
                {
                    class ViewHolder
                    {
                        ImageView image;
                        TextView title;
                    }

                    @Override
                    public int getCount() {return Source.SOURCES.length;}
                    @Override
                    public Object getItem(int position) {return Source.SOURCES[position];}
                    @Override
                    public long getItemId(int position) {return position;}
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent)
                    {
                        ViewHolder viewHolder;

                        if(convertView == null)
                        {
                            viewHolder = new ViewHolder();

                            //map to layout
                            convertView = LayoutInflater.from(context).inflate(R.layout.mini_sources_list_layout, parent, false);

                            //get imageview
                            viewHolder.title = convertView.findViewById(R.id.element_title);
                            viewHolder.image = convertView.findViewById(R.id.element_image);

                            convertView.setTag(viewHolder);
                        }
                        else viewHolder = (ViewHolder) convertView.getTag();

                        viewHolder.title.setText(Source.SOURCES[position].getName());
                        viewHolder.image.setImageResource(Source.SOURCES[position].getIconImage());

                        return convertView;
                    }
                });
            }
        });
        dialog.setButton(DialogInterface.BUTTON_POSITIVE, "OK", new DialogInterface.OnClickListener()
        {
            @Override
            public void onClick(DialogInterface d, int which)
            {
                //create playlist
                Spinner listView = dialog.findViewById(R.id.playlist_source);
                Source source = Source.SOURCES[listView.getSelectedItemPosition()];
                EditText editText = dialog.findViewById(R.id.playlist_name);
                source.addPlaylist(editText.getText().toString(), new Source.OperationCallback()
                {
                    @Override
                    public void onSucess(LibraryObject result)
                    {
                        if(callback != null) callback.onSuccess((Playlist) result);
                    }

                    @Override
                    public void onFailure()
                    {
                        context.runOnUiThread(new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                Toast.makeText(context, context.getString(R.string.playlist_add_fail), Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }, false, false);
            }
        });
        dialog.show();
    }
}
