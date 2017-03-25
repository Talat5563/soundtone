package com.talat.soundtone;

import android.Manifest;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AdapterView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "SOUNDTONE-Main";
    static final String CHOSEN_PLAYLIST = "SOUNDTONE-CHOSEN_PLAYLIST";

    ListView listView;
    NavigationView navigationView;

    private Dialogs appDialogs;

    private HashMap<String,Integer> playlistNames = new HashMap<>();

    private String CurrentVisiblePlaylist;

    private boolean gotPermissions = false;
    private boolean isPlaylistSong = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                setPlayListsAsMenuItem(navigationView);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();



        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        CurrentVisiblePlaylist = getString(R.string.all_songs);

        listView = (ListView) findViewById(R.id.list_view);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Cursor MediaDB = (Cursor) parent.getItemAtPosition(position);
                playWithDefaultMediaPlayer(MediaDB);
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {

                // Start an alpha animation for clicked item
                Animation animation1 = new AlphaAnimation(0.3f, 1.0f);
                animation1.setDuration(4000);
                view.startAnimation(animation1);

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                        !Settings.System.canWrite(getApplicationContext())){
                    Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                }
                else
                {
                    Cursor MediaDB = (Cursor) parent.getItemAtPosition(position);
                    AlarmSelectorService.setMediaAsDefaultRingtone(getApplicationContext(),MediaDB,isPlaylistSong);
                }

                return true;
            }
        });


        appDialogs = new Dialogs(this);
        requestPermission();

    }

    @Override
    protected void onStart() {
        if(gotPermissions)
        {
            setPlayListsAsMenuItem(navigationView);
        }
        super.onStart();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case 111: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! set ListView Adapter
                    gotPermissions = true;
                    listView.setAdapter(new AllSongsCursorAdapter(this,PlayListManager.getAllMedia(this),true));
                    setPlayListsAsMenuItem(navigationView);
                } else {
                    //TODO put an explanation dialog
                    finish();
                }
            }

            // other 'case' lines to check for other
            // permissions this app might request
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        if (id == R.id.action_create_playlist){
            appDialogs.showCreatePlayListDialog();
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.

        String name = item.getTitle().toString();
        Cursor playlist;

        if(name.equals(getString(R.string.choose_alarm_playlist))){
            appDialogs.showPlayListChooser(playlistNames);
            return true;
        }else if(name.equals(getString(R.string.all_songs))) {
            playlist = PlayListManager.getAllMedia(this);
            isPlaylistSong = false;
            listView.setAdapter(new AllSongsCursorAdapter(this,playlist,true));
            CurrentVisiblePlaylist = name;
        }else{
            try
            {
                playlist = PlayListManager.getPlaylist(this,name);
                int id = playlist.getInt(playlist.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
                Log.i(TAG,"Playlist is: " + name);
                Log.i(TAG,"Playlist ID: " + id);
                playlist = PlayListManager.getPlaylistSongs(this,id);
                isPlaylistSong = true;

                listView.setAdapter(new AllSongsCursorAdapter(this,playlist,true));
                CurrentVisiblePlaylist = name;

            } catch ( IllegalArgumentException e){
                Log.e(TAG, getString(R.string.cant_find_playlists),e);
                Toast.makeText(this,getString(R.string.cant_find_playlists),Toast.LENGTH_SHORT).show();
            }
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    public void setMediaPathAsDefaultRingtone(Cursor MediaCursor) throws IllegalArgumentException
    {
        //Get newMediaPath
        String newMediaPath = MediaCursor.getString(MediaCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        //get latest Media Path from SharedPreferences
        String oldMediaFilePath = sharedPreferences.getString("OLD_FILE_PATH",null);

        //update old media path value MediaStore.Audio.Media.IS_ALARM false
        if(oldMediaFilePath!= null)
        {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.IS_ALARM,false);
            getContentResolver().update(MediaStore.Audio.Media.getContentUriForPath(oldMediaFilePath),
                    values,
                    MediaStore.MediaColumns.DATA + "='" +oldMediaFilePath +"'", null);

            Toast.makeText(this,"Changed " + oldMediaFilePath + " to not be an alarm",Toast.LENGTH_SHORT).show();
        }

        //update new media path value MediaStore.Audio.Media.IS_ALARM true
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.IS_ALARM,true);
        int num_rows = getContentResolver().update(MediaStore.Audio.Media.getContentUriForPath(newMediaPath),
                values,
                MediaStore.MediaColumns.DATA + "= ?",new String[]{newMediaPath});
        if(num_rows != 1)
        {
            Log.i(TAG,"Updated more then one row - check your WHERE clause : num_rows = " + num_rows);
        }

        //make uri
        Uri newMediaUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                MediaCursor.getInt(MediaCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));

        //set new media path as default ringtone
        RingtoneManager.setActualDefaultRingtoneUri(this,RingtoneManager.TYPE_ALARM,newMediaUri);
        Toast.makeText(this,newMediaPath + " has been set as default ringtone",Toast.LENGTH_SHORT).show();
        sharedPreferences.edit().putString("OLD_FILE_PATH",newMediaPath).apply();
    }

    public void requestPermission() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 111);
        }else {
            gotPermissions = true;
            listView.setAdapter(new AllSongsCursorAdapter(this,PlayListManager.getAllMedia(this),true));
            try
            {
                setPlayListsAsMenuItem(navigationView);
            }catch(IllegalArgumentException e)
            {
                Log.e(TAG, getString(R.string.cant_find_playlists),e);
                Toast.makeText(this,getString(R.string.cant_find_playlists),Toast.LENGTH_SHORT).show();
            }
        }
    }


    public void setPlayListsAsMenuItem(NavigationView navigationView) throws IllegalArgumentException
    {
        Menu menu = navigationView.getMenu();

        SubMenu subMenu = menu.findItem(R.id.playlists).getSubMenu();
        subMenu.clear();
        playlistNames.clear();
        subMenu.add(getString(R.string.all_songs))
                .setIcon(R.drawable.playlist1);
        playlistNames.put(getString(R.string.all_songs),-1);

        try(Cursor playlistsCursor = PlayListManager.getPlaylist(this,-1))
        {
            if(playlistsCursor == null || playlistsCursor.getCount() <=0)
            {
                Log.e(TAG,getString(R.string.no_playlists_to_show));
                Toast.makeText(this,R.string.no_playlists_to_show,Toast.LENGTH_SHORT).show();
                return;
            }

            playlistsCursor.moveToFirst();
            do {

                final String playlistName = playlistsCursor.
                        getString(playlistsCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.NAME));
                final int playListId = playlistsCursor.
                        getInt(playlistsCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists._ID));
                playlistNames.put(playlistName, playListId);
                MenuItem item = subMenu.add(playlistName).setIcon(R.drawable.playlist1);
                View LongClickView = new View(this);
                LongClickView.setOnLongClickListener(new View.OnLongClickListener() {
                    @Override
                    public boolean onLongClick(View v) {
                        appDialogs.showDeletePlayListDialog(playlistName,playListId);
                        return true;
                    }
                });
                item.setActionView(LongClickView);
            } while (playlistsCursor.moveToNext());
        }
    }

    public void onColonClick(final View theView)
    {
        //TODO add on click animation
        // Start an alpha animation for clicked item
        Animation animation1 = new AlphaAnimation(0.3f, 1.0f);
        animation1.setDuration(2000);
        theView.startAnimation(animation1);
        //get Cursor from view
        final View parentRow = (View) theView.getParent();
        ListView listView = (ListView) parentRow.getParent();
        final int position = listView.getPositionForView(parentRow);
        AllSongsCursorAdapter cursorAdapter = (AllSongsCursorAdapter)listView.getAdapter();
        final Cursor songCursor = cursorAdapter.getCursor();
        songCursor.moveToPosition(position);

        final PopupMenu popupMenu = new PopupMenu(this,theView);
        Menu menu = popupMenu.getMenu();

        menu.add(R.string.play_song);
        menu.add(R.string.add_to_playlist);
        menu.add(R.string.delete_from_playlist);

        setPlayListsAsMenuItem(navigationView);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String itemName = item.getTitle().toString();
                if(itemName.equals(getString(R.string.delete_from_playlist))) {
                    TextView songName = (TextView) parentRow.findViewById(R.id.title);
                    String songNameS = songName.getText().toString();
                    //TODO check if its not all songs
                    appDialogs.showAreYouSureDialog(songNameS,CurrentVisiblePlaylist,songCursor,playlistNames);

                }else if(itemName.equals(getString(R.string.add_to_playlist))) {
                    appDialogs.showAddToPlayList(playlistNames,songCursor);
                }else if(itemName.equals(getString(R.string.play_song))){
                    playWithDefaultMediaPlayer(songCursor);
                }

                popupMenu.dismiss();
                return true;
            }
        });

        popupMenu.show();
    }

    public void playWithDefaultMediaPlayer(Cursor MediaCursor)
    {
        //Get newMediaPath
        String newMediaPath = MediaCursor.getString(MediaCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

        Intent intent = new Intent();
        intent.setAction(android.content.Intent.ACTION_VIEW);
        File file = new File(newMediaPath);
        intent.setDataAndType(Uri.fromFile(file), "audio/*");
        startActivity(intent);
    }


}
