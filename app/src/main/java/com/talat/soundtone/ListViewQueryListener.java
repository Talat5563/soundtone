package com.talat.soundtone;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.ListView;
import android.widget.SearchView;

/**
 * Created by talat on 05/04/17.
 */

public class ListViewQueryListener implements SearchView.OnQueryTextListener {

    private static final String TAG = "SOUNDTONE-QueryListener";

    private ListView aListView;
    private Context context;


    ListViewQueryListener(Context context,ListView listView)
    {
        this.context = context;
        this.aListView = listView;
    }


    @Override
    public boolean onQueryTextSubmit(String s) {
        return false;
    }

    @Override
    public boolean onQueryTextChange(String s) {

        Cursor playlist;
        String playListName = ((MainActivity)context).getCurrentPlayListName();

        if(playListName.equals(context.getString(R.string.all_songs)))
        {
            playlist = PlayListManager.getAllMedia(context,s);
        }else{
            playlist = PlayListManager.getPlaylist(context,playListName);
            int id = playlist.getInt(playlist.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
            Log.i(TAG,"Playlist is: " + playListName);
            Log.i(TAG,"Playlist ID: " + id);
            playlist = PlayListManager.getPlaylistSongs(context,id,s);
        }

        aListView.setAdapter(new AllSongsCursorAdapter(context,playlist,true));

        return true;
    }
}
