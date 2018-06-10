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

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Build;
import android.os.IBinder;
import android.support.v4.media.session.MediaControllerCompat;
import android.util.Log;

import v.shook.library.Song;
import v.shook.player.PlayerService;

import java.util.ArrayList;

import static java.lang.System.exit;

public class PlayerConnection
{
    private static Context applicationContext;

    /* connection to player and callbacks */
    private static ArrayList<Song> playOnConnect; private static int positionOnConnect;
    private static volatile PlayerService musicPlayer = null;
    public static MediaControllerCompat musicController;
    private static ServiceConnection musicConnection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service)
        {
            musicPlayer = ((PlayerService.PlayerBinder) service).getService();

            try{musicController = new MediaControllerCompat(applicationContext, musicPlayer.getSessionToken());}
            catch(Exception e) {
                Log.d("shookMusic", "test exception ");
                exit(1);
            }

            if(playOnConnect != null) {musicPlayer.setCurrentPlaylist(playOnConnect, positionOnConnect); playOnConnect = null;}

            if(connectionCallback != null) connectionCallback.onConnected();
        }

        @Override
        public void onServiceDisconnected(ComponentName name)
        {
            musicPlayer = null;
            if(connectionCallback != null) connectionCallback.onDisconnected();
        }
    };

    /* connection callbacks */
    public interface Callback {void onConnected(); void onDisconnected();}
    private static Callback connectionCallback;

    public static boolean init(Callback connectionCallback, Context applicationContext)
    {
        if(musicPlayer != null) {if(connectionCallback != null) connectionCallback.onConnected(); return true;}

        Log.d("shookMusic", "MUSIC PLAYER");
        PlayerConnection.applicationContext = applicationContext;
        Intent serv = new Intent(applicationContext, PlayerService.class);
        PlayerConnection.connectionCallback = connectionCallback;
        return applicationContext.bindService(serv, musicConnection, Context.BIND_ABOVE_CLIENT);
    }

    public static void start(ArrayList<Song> songs, int currentPos)
    {
        playOnConnect = songs;
        positionOnConnect = currentPos;

        Intent serv = new Intent(applicationContext, PlayerService.class);

        if(Build.VERSION.SDK_INT >= 26) applicationContext.startForegroundService(serv);
        else applicationContext.startService(serv);

        applicationContext.bindService(serv, musicConnection, Context.BIND_ABOVE_CLIENT);
    }

    public static PlayerService getService()
    {
        return musicPlayer;
    }
}
