package com.talat.soundtone;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.util.Log;

import java.util.ArrayList;


class PlayListManager {

    private static final String TAG = "SOUNDTONE-PLAYLISTER";

    private PlayListManager() {}

    static Cursor getAllMedia(Context context)
    {
        ContentResolver contentResolver= context.getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        String[] projection = new String[] {
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
        };
        String order = MediaStore.Audio.Media.TITLE + " ASC";
        return contentResolver.query(uri,projection,selection,null,order);
    }


    static Cursor getPlaylist(Context context, Integer PlaylistID)
    {
        Uri playListsUri  = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATE_MODIFIED,
                MediaStore.Audio.Playlists.DATE_ADDED,
        };

        String select =  null;
        String sortOrder = MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER;
        if(PlaylistID != -1)
        {
            select = MediaStore.Audio.Playlists._ID+ " = "+PlaylistID+"";
        }

        return context.getContentResolver().query(playListsUri,projection,select,null, sortOrder);
    }

    static Cursor getPlaylist(Context context, String PlaylistName)
    {
        Uri playListsUri  = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATE_MODIFIED,
                MediaStore.Audio.Playlists.DATE_ADDED,
        };

        String select =  null;
        String[] selectArgs = {PlaylistName};
        String sortOrder = MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER;
        if(PlaylistName != null)
        {
            select = MediaStore.Audio.Playlists.NAME+ "=?";

        }else{
            selectArgs = null;
        }

        Cursor playListCursor = context.getContentResolver().query(playListsUri,projection,select,selectArgs, sortOrder);
        playListCursor.moveToFirst();

        return playListCursor;
    }


    static Cursor getPlaylistSongs(Context context, Integer PlaylistID)
    {
        Log.i(TAG,"geting Playlist Songs- id = " + PlaylistID);
        Uri playListUri = MediaStore.Audio.Playlists.Members.getContentUri("external",PlaylistID);
        Log.i(TAG,"Got Playlist URI" + playListUri);
        String selection = MediaStore.Audio.Media.IS_MUSIC + "!=0";
        String[] projection = new String[] {
                MediaStore.Audio.Playlists.Members._ID,
                MediaStore.Audio.Playlists.Members.TITLE,
                MediaStore.Audio.Playlists.Members.AUDIO_ID,
                MediaStore.Audio.Playlists.Members.ALBUM,
                MediaStore.Audio.Playlists.Members.ALBUM_ID,
                MediaStore.Audio.Playlists.Members.ARTIST,
                MediaStore.Audio.Playlists.Members.ARTIST_ID,
                MediaStore.Audio.Playlists.Members.DURATION,
                MediaStore.Audio.Playlists.Members.DATA
        };

        Log.i(TAG,"querying");
        return context.getContentResolver().query(playListUri,projection,selection,null,
                null);
    }


    /**
     * Creates a New Play lList
     * @param context app-context
     * @param playListName Play List Name
     * @return the PlayList ID.
     */
    public static Integer createPlayList(Context context, String playListName)
    {
        Integer playlistID = -1;
        Uri playListsUri  = MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI;
        String[] projection = {
                MediaStore.Audio.Playlists._ID,
                MediaStore.Audio.Playlists.NAME,
                MediaStore.Audio.Playlists.DATE_MODIFIED,
                MediaStore.Audio.Playlists.DATE_ADDED
        };

        String select = MediaStore.Audio.Playlists.NAME + "=?" ;
        String[] selectArgs = {playListName};
        //Check if playlist exist if so return its id
        Cursor playlist = context.getContentResolver().query(playListsUri,projection,select,selectArgs,null);
        if (playlist==null || playlist.getCount() < 1)
        {
            if (playlist!= null)
            {
                playlist.close();
            }
            //create a playlist ContentValues
            ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Audio.Playlists.NAME,playListName);
            contentValues.put(MediaStore.Audio.Playlists.DATE_ADDED, System.currentTimeMillis());
            contentValues.put(MediaStore.Audio.Playlists.DATE_MODIFIED, System.currentTimeMillis());
            //add values to mediaStore.Audio.Playlist
            context.getContentResolver().insert(playListsUri,contentValues);
            //get playlist Cursor
            playlist = context.getContentResolver().query(playListsUri,projection,select,selectArgs,null);
        }

        if (playlist!= null && playlist.getCount() >= 1)
        {
            playlist.moveToFirst();
            playlistID = playlist.getInt(playlist.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
            playlist.close();
        }


        return playlistID;
    }

    static void deletePlaylist(Context context, Integer playlistId)
    {
        ContentResolver resolver = context.getContentResolver();
        String where = MediaStore.Audio.Playlists._ID + "=?";
        String[] whereVal = {playlistId.toString()};
        resolver.delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, whereVal);
    }

    static void addToPlayList(Context context, Integer playListID, Cursor song, @Nullable ArrayList<Integer> positionsVec)
    {
        //get playlist Uri
        Uri playListUri = MediaStore.Audio.Playlists.Members.getContentUri("external", playListID);
        String[] projection = new String[] {
                "count(*)"
        };

        Cursor playList = context.getContentResolver().query(playListUri,projection,null,null,null);
        assert playList != null;
        playList.moveToFirst();
        final int base = playList.getInt(0);
        playList.close();

        if(positionsVec != null)
        {
            for(Integer i :positionsVec){
                if(song.moveToPosition(i))
                {
                    //get song AudioID
                    int audioId = song.getInt(song.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));

                    //create song contentValue Audio_ID=songID
                    ContentValues values = new ContentValues();
                    values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + audioId);
                    //add song to playlist
                    values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
                    context.getContentResolver().insert(playListUri, values);
                }
            }
        }else{
            //get song AudioID
            int audioId = song.getInt(song.getColumnIndexOrThrow(MediaStore.Audio.Media._ID));

            //create song contentValue Audio_ID=songID
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, base + audioId);
            //add song to playlist
            values.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, audioId);
            context.getContentResolver().insert(playListUri, values);
        }


    }

    static void removeFromPlayList(Context context, Integer playListID, Cursor song, @Nullable ArrayList<Integer> positionsVec)
    {
        //get playlist Uri
        Uri playListUri = MediaStore.Audio.Playlists.Members.getContentUri("external", playListID);

        if(positionsVec != null)
        {
            for(Integer i :positionsVec) {
                if (song.moveToPosition(i)) {
                    //get song AudioID
                    int audioId = song.getInt(song.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
                    //remove song by Audio_ID
                    context.getContentResolver().delete(playListUri,MediaStore.Audio.Playlists.Members.AUDIO_ID +" = ?",new String[] {String.valueOf(audioId)});

                }

            }
        }else{
            //get song AudioID
            int audioId = song.getInt(song.getColumnIndex(MediaStore.Audio.Playlists.Members.AUDIO_ID));
            //remove song by Audio_ID
            context.getContentResolver().delete(playListUri,MediaStore.Audio.Playlists.Members.AUDIO_ID +" = ?",new String[] {String.valueOf(audioId)});

        }
    }


}
