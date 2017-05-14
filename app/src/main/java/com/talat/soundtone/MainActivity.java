package com.talat.soundtone;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.SearchManager;
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
import android.os.StrictMode;
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
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;


public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "SOUNDTONE-Main";
    static final String CHOSEN_PLAYLIST = "SOUNDTONE-CHOSEN_PLAYLIST";
    static final String AUTO_STAT_PERMISSION_GRANTED = "soundtone_autostart_perm";
    static final int OVERLAY_PERMISSION_REQ_CODE = 144;

    ListView listView;
    NavigationView navigationView;

    //icons
    private MenuItem mDeleteFromPlayListIcon;
    private MenuItem mAddToPlayListIcon;

    private TextView playListNameTag;

    private AdView mAdView;

    private Dialogs appDialogs;

    private HashMap<String,Integer> playlistNames = new HashMap<>();
    private String CurrentVisiblePlaylist;

    private boolean gotPermissions = false;
    private boolean isPlaylistSong = false;

    //fast scroll hack
    private int mCurrentState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close){
            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                setPlayListsAsMenuItem(navigationView);
            }
        };
        drawer.addDrawerListener(toggle);
        toggle.syncState();


        //init views and items.
        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        CurrentVisiblePlaylist = getString(R.string.all_songs);

        listView = (ListView) findViewById(R.id.list_view);
        playListNameTag = (TextView) findViewById(R.id.playlist_name_tag);
        mAdView = (AdView) findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);


        setOnClickListiners(listView);
        setOnScrollStateChanged(listView);
        appDialogs = new Dialogs(this);
        requestPermission();
    }



    @Override
    protected void onStart() {
        if(gotPermissions)
        {
            setPlayListsAsMenuItem(navigationView);
            AppRater.app_launched(this);
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
                    appDialogs.showSoundToneCantFunctionWithoutPermissions();
                }
                break;
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
            if(((AllSongsCursorAdapter)listView.getAdapter()).getNumberOfPressedItems() > 0)
            {
                ((AllSongsCursorAdapter)listView.getAdapter()).clearSelected();
                ((AllSongsCursorAdapter)listView.getAdapter()).notifyDataSetChanged();
                //restart Option Menu
                MainActivity.this.invalidateOptionsMenu();
            }
            else
            {
                super.onBackPressed();
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if(listView !=null &&  listView.getAdapter() != null)
        {
            setAppBarIconsVisibility(((AllSongsCursorAdapter)listView.getAdapter()).getNumberOfPressedItems() > 0);
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        mAddToPlayListIcon = menu.findItem(R.id.action_add_to_playlist);
        mDeleteFromPlayListIcon = menu.findItem(R.id.action_delete_from_playlist);

        SearchManager searchManager = (SearchManager)
                getSystemService(Context.SEARCH_SERVICE);

        MenuItem searchMenuItem = menu.findItem(R.id.search);
        SearchView searchView = (SearchView) searchMenuItem.getActionView();

        searchView.setSearchableInfo(searchManager.
                getSearchableInfo(getComponentName()));
        searchView.setSubmitButtonEnabled(false);
        searchView.setIconifiedByDefault(false);

        searchView.setOnQueryTextListener(new ListViewQueryListener(this,listView));

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
            appDialogs.showNoSettingsPage();
            return true;
        }
        if (id == R.id.action_create_playlist){
            appDialogs.showCreatePlayListDialog();
        }
        if (id == R.id.help_button){
            Intent intent = new Intent(this,HelpActivity.class);
            startActivity(intent);
        }
        if (id == R.id.action_add_to_playlist || id == R.id.action_delete_from_playlist){
            AllSongsCursorAdapter cursorAdapter = (AllSongsCursorAdapter)listView.getAdapter();
            Cursor songCursor = cursorAdapter.getCursor();
            songCursor.moveToFirst();

            ArrayList<Integer> selectedItemsVector = new ArrayList<Integer>(((AllSongsCursorAdapter)listView.getAdapter()).getSelectedVector());

            if(!selectedItemsVector.isEmpty())
            {
                if (id == R.id.action_add_to_playlist) {
                    appDialogs.showAddToPlayList(playlistNames, songCursor,selectedItemsVector);
                } else {
                    appDialogs.showAreYouSureDialog("", CurrentVisiblePlaylist.toString(), songCursor, playlistNames,selectedItemsVector);
                }

                for (Integer i : selectedItemsVector) {
                    ((AllSongsCursorAdapter)listView.getAdapter()).removeSelectedPosition(i);
                }

                ((AllSongsCursorAdapter)listView.getAdapter()).notifyDataSetChanged();
                MainActivity.this.invalidateOptionsMenu();
            }
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
                ((AllSongsCursorAdapter)listView.getAdapter()).clearSelected();
                MainActivity.this.invalidateOptionsMenu();

            } catch ( IllegalArgumentException e){
                Log.e(TAG, getString(R.string.cant_find_playlists),e);
                Toast.makeText(this,getString(R.string.cant_find_playlists),Toast.LENGTH_SHORT).show();
            }
        }

        playListNameTag.setText(CurrentVisiblePlaylist + " Playlist");

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
        Toast.makeText(this,newMediaPath + " has been set as default AlarmTone",Toast.LENGTH_SHORT).show();
        sharedPreferences.edit().putString("OLD_FILE_PATH",newMediaPath).apply();
    }

    public void requestPermission() {

        // permission to read an write to external storage
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

        // permission to write settings
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            if(!Settings.System.canWrite(this))
            {
                appDialogs.showPleaseGrantChangeSettingsPermission();
            }

            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.MEDIA_CONTENT_CONTROL)
                    != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.MEDIA_CONTENT_CONTROL}, 112);
            }

            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.CHANGE_CONFIGURATION)
                    != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.CHANGE_CONFIGURATION}, 113);
            }

            if(ContextCompat.checkSelfPermission(this,
                    Manifest.permission.MODIFY_AUDIO_SETTINGS)
                    != PackageManager.PERMISSION_GRANTED)
            {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.MODIFY_AUDIO_SETTINGS}, 114);
            }
        }

        String manufacturer = "xiaomi";
        if(manufacturer.equalsIgnoreCase(android.os.Build.MANUFACTURER))
        {
            SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
            if(!sharedPreferences.getBoolean(AUTO_STAT_PERMISSION_GRANTED,false))
                appDialogs.showPleaseGrantAutoStartPermission();
        }

//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//            if (!Settings.canDrawOverlays(this)) {
//                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                        Uri.parse("package:" + getPackageName()));
//                startActivityForResult(intent, OVERLAY_PERMISSION_REQ_CODE);
//            }
//        }
    }


    public void setPlayListsAsMenuItem(NavigationView navigationView)
    {
        Menu menu = navigationView.getMenu();

        //get ChosenPlaylist id
        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName(), Context.MODE_PRIVATE);
        int chosenPlaylistId = sharedPreferences.getInt(CHOSEN_PLAYLIST,-1);

        SubMenu subMenu = menu.findItem(R.id.playlists).getSubMenu();
        subMenu.clear();
        playlistNames.clear();
        subMenu.add(getString(R.string.all_songs))
                .setIcon(R.drawable.playlist1).setChecked(chosenPlaylistId == -1);
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
                MenuItem item = subMenu.add(playlistName).setIcon(R.drawable.playlist1).setChecked(playListId == chosenPlaylistId);

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
        }catch(Exception e) {
            Log.e(TAG,"Cant set playlist as menu items ",e);
        }
    }

    public void onColonClick(final View theView)
    {
        // Start an alpha animation for clicked item
        Animation animation1 = new AlphaAnimation(0.3f, 1.0f);
        animation1.setDuration(2000);
        theView.startAnimation(animation1);
        //get Cursor from view
        final View parentRow = (View) theView.getParent();
        final ListView listView = (ListView) parentRow.getParent();
        final int position = listView.getPositionForView(parentRow);
        AllSongsCursorAdapter cursorAdapter = (AllSongsCursorAdapter)listView.getAdapter();
        final Cursor songCursor = cursorAdapter.getCursor();
        songCursor.moveToPosition(position);

        final PopupMenu popupMenu = new PopupMenu(this,theView);
        Menu menu = popupMenu.getMenu();

        //menu.add(R.string.play_song);
//        menu.add(R.string.add_to_playlist);
//        menu.add(R.string.delete_from_playlist);
        menu.add(R.string.set_as_def_ring);
        menu.add(R.string.choose_this_song);

        setPlayListsAsMenuItem(navigationView);

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                String itemName = item.getTitle().toString();
                /*if(itemName.equals(getString(R.string.delete_from_playlist))) {
                    TextView songName = (TextView) parentRow.findViewById(R.id.title);
                    String songNameS = songName.getText().toString();
                    appDialogs.showAreYouSureDialog(songNameS,CurrentVisiblePlaylist,songCursor,playlistNames);

                }else if(itemName.equals(getString(R.string.add_to_playlist))) {
                    appDialogs.showAddToPlayList(playlistNames,songCursor);
                }else */


                if(itemName.equals(getString(R.string.set_as_def_ring))){
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                            !Settings.System.canWrite(getApplicationContext())){
                        Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + getApplicationContext().getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                    } else {
                        AlarmSelectorService.setMediaAsDefaultRingtone(getApplicationContext(),songCursor,isPlaylistSong);
                    }
                }else if(itemName.equals(getString(R.string.choose_this_song)))
                {
                    chooseAListItem(parentRow,position);
                }

                popupMenu.dismiss();
                return true;
            }
        });

        popupMenu.show();
    }


//    public void onAddToPlayListClick(final View theView)
//    {
//        //get Cursor from view
//        View parentRow = (View) theView.getParent();
//        ListView listView = (ListView) parentRow.getParent();
//        int position = listView.getPositionForView(parentRow);
//        AllSongsCursorAdapter cursorAdapter = (AllSongsCursorAdapter)listView.getAdapter();
//        Cursor songCursor = cursorAdapter.getCursor();
//        songCursor.moveToPosition(position);
//
//        appDialogs.showAddToPlayList(playlistNames,songCursor);
//    }

        public void onAlbumImageClick(final View theView)
    {
        //get Cursor from view
        View parentRow = (View) theView.getParent();
        ListView listView = (ListView) parentRow.getParent();
        int position = listView.getPositionForView(parentRow);
        AllSongsCursorAdapter cursorAdapter = (AllSongsCursorAdapter)listView.getAdapter();
        Cursor songCursor = cursorAdapter.getCursor();

        if(songCursor != null && !songCursor.isNull(position))
        {
            songCursor.moveToPosition(position);
            playWithDefaultMediaPlayer(songCursor);
        }else{
            Toast.makeText(this,"Can't play this song - ERROR",Toast.LENGTH_SHORT).show();
        }

    }

    public void onAddToPlayLIstFabClick(final View view)
    {
        appDialogs.showCreatePlayListDialog();
    }

    public void playWithDefaultMediaPlayer(Cursor MediaCursor)
    {
        try{
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    Method m = StrictMode.class.getMethod("disableDeathOnFileUriExposure");
                    m.invoke(null);
                } catch (Exception e) {
                    Log.e(TAG,"can't disable DeathOnFileUriExposure",e);
                }
            }

            //Get newMediaPath
            String newMediaPath = MediaCursor.getString(MediaCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));

            Intent intent = new Intent();
            intent.setAction(android.content.Intent.ACTION_VIEW);
            File file = new File(newMediaPath);
            intent.setDataAndType(Uri.fromFile(file), "audio/*");
            startActivity(intent);
        }catch (Exception e) {
            Log.e(TAG,"can't find an app to play default ringtone with",e);
            Toast.makeText(this,"Sorry Can't play this Song",Toast.LENGTH_SHORT).show();
        }
    }

    public void setAppBarIconsVisibility(boolean visible)
    {
        mAddToPlayListIcon.setVisible(visible);
        mDeleteFromPlayListIcon.setVisible(visible);
    }


    void setOnScrollStateChanged(final ListView mList)
    {
        mList.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView absListView, int state) {

                if (state == SCROLL_STATE_IDLE && mCurrentState != state && mList.isFastScrollEnabled()){

                    mList.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mList.setFastScrollEnabled(false);
                        }
                    },800);
                }


                mCurrentState = state;
            }

            @Override
            public void onScroll(AbsListView absListView, int i, int i1, int i2) {
                if (mCurrentState == SCROLL_STATE_TOUCH_SCROLL) {

                    if (!mList.isFastScrollEnabled())
                        mList.setFastScrollEnabled(true);

                    if(playListNameTag.getVisibility() == View.GONE)
                    {
                        showViewAnimatedTopToButtom(playListNameTag);

                        mList.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                dismissViewAnimatedTopToButtom(playListNameTag);
                            }
                        },2000);
                    }

                    if(mAdView.getVisibility() == View.GONE)
                    {
                        mAdView.setVisibility(View.VISIBLE);
                        slideToTop(mAdView);
                        mList.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                slideToBottom(mAdView);
                                mAdView.setVisibility(View.GONE);
                            }
                        },5000);
                    }
                }
            }
        });
    }

    public void setOnClickListiners(final ListView listView)
    {
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if(((AllSongsCursorAdapter)listView.getAdapter()).getNumberOfPressedItems() > 0)
                {
                    chooseAListItem(view,position);
                }else{
                    Cursor MediaDB = (Cursor) parent.getItemAtPosition(position);
                    playWithDefaultMediaPlayer(MediaDB);
                }
            }
        });

        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                chooseAListItem(view,position);
                return true;
            }
        });
    }

    public void chooseAListItem(View view,int position)
    {
        // Start an alpha animation for clicked item
        Animation animation1 = new AlphaAnimation(0.3f, 1.0f);
        animation1.setDuration(600);
        view.startAnimation(animation1);

        // add\delete item position to selected items position array
        if(!((AllSongsCursorAdapter)listView.getAdapter()).AddSelectedPosition(position))
        {
            ((AllSongsCursorAdapter)listView.getAdapter()).removeSelectedPosition(position);
        }
        // update list
        ((AllSongsCursorAdapter)listView.getAdapter()).notifyDataSetChanged();

        //restart Option Menu
        MainActivity.this.invalidateOptionsMenu();

    }

    public String getCurrentPlayListName()
    {
        return CurrentVisiblePlaylist;
    }


    public void showViewAnimatedTopToButtom(View view)
    {
        if(view.getVisibility() != View.VISIBLE)
        {
            // Prepare the View for the animation
            view.setVisibility(View.VISIBLE);
            view.setAlpha(0.0f);

            // Start the animation
            view.animate()
                    .translationY(view.getHeight())
                    .alpha(1.0f);
        }
    }

    public void dismissViewAnimatedTopToButtom(final View view){

        if(view.getVisibility() == View.VISIBLE)
        {
            view.animate()
                    .translationY(0)
                    .alpha(0.0f)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            super.onAnimationEnd(animation);
                            if(view.getTranslationY() == 0)
                            {
                                view.setVisibility(View.GONE);
                            }
                        }
                    });
        }
    }

    public void slideToBottom(View view){
        TranslateAnimation animate = new TranslateAnimation(0,0,0,view.getHeight());
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
        view.setVisibility(View.GONE);
    }

    public void slideToTop(View view){
        TranslateAnimation animate = new TranslateAnimation(0,0,view.getHeight(),0);
        animate.setDuration(500);
        animate.setFillAfter(true);
        view.startAnimation(animate);
        view.setVisibility(View.VISIBLE);
    }

}
