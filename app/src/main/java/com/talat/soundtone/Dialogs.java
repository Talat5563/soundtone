package com.talat.soundtone;


import android.app.Dialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.NativeExpressAdView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;

import static com.talat.soundtone.MainActivity.AUTO_STAT_PERMISSION_GRANTED;
import static com.talat.soundtone.MainActivity.CHOSEN_PLAYLIST;

class Dialogs {

    private final String TAG = "SOUNDTONE-Dialogs";

    private Dialog playListChooserDialog;
    private Dialog addToPlayListDialog;
    private Dialog areYouSureDialog;
    private Dialog createPlayList;
    private Dialog deletePlaylist;
    private Context context;

    private int chosenPlayList;
    private int chosen = 0;

    //Constructor
    Dialogs(Context context) {
        this.context = context;
        //Chosen PlayList initialization
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        //get chosen from SharedPreferences
        chosenPlayList = sharedPreferences.getInt(CHOSEN_PLAYLIST,-1);
        createPlayList = buildCreatePlayList();
        deletePlaylist = null;
    }

    void showPlayListChooser(HashMap<String, Integer> playlistNamesMap) {
        playListChooserDialog = buildPlaylistChooser(playlistNamesMap);
        playListChooserDialog.show();
    }

    void dismissPlaylistChooser() {
        playListChooserDialog.dismiss();
    }

    void showAddToPlayList(HashMap<String, Integer> playlistNamesMap, Cursor songCursor, @Nullable ArrayList<Integer> positionsVec)
    {
        addToPlayListDialog = buildAddToPlaylist(playlistNamesMap,songCursor,positionsVec);
        addToPlayListDialog.show();
    }

    void dismissAddToPlayList()
    {
        addToPlayListDialog.dismiss();
    }

    private Dialog buildPlaylistChooser(HashMap<String,Integer> playlistNamesMap)
    {
        final HashMap<String,Integer> playlistNames = playlistNamesMap;
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.choose_alarm_playlist).setIcon(R.drawable.ic_menu_slideshow)
                .setSingleChoiceItems(playlistNames.keySet().toArray(new CharSequence[playlistNames.keySet().size()]), chosen, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String name = playlistNames.keySet()
                                .toArray(new CharSequence[playlistNames.keySet().size()])[which].toString();
                        Log.i(TAG, "You Chose Playlist:" + name);
                        chosenPlayList = playlistNames.get(name);
                        Log.i(TAG, "With ID:" + chosenPlayList);
                        chosen = which;
                        Log.i(TAG, "which:" + which);
                    }
                });

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
                sharedPreferences.edit().putInt(CHOSEN_PLAYLIST,chosenPlayList).apply();
                dialog.dismiss();
                Toast.makeText(context, R.string.playlist_was_set,Toast.LENGTH_LONG).show();
                ((MainActivity)context).setPlayListsAsMenuItem(((MainActivity) context).navigationView);
                AlarmSelectorService.startActionNextAlarm(context,chosenPlayList);
                showMakesSureYoursUsingYourDefaultAlarm();
            }
        });

        return builder.create();
    }


    private Dialog buildAddToPlaylist(HashMap<String,Integer> playlistNamesMap, final Cursor songCursor,@Nullable final ArrayList<Integer> positionsVec)
    {
        final HashMap<String,Integer> playlistNames = playlistNamesMap;
        final int[] playListToAdd = new int[1];
        final ArrayList<String> keySet = new ArrayList<>(playlistNames.keySet());
        Collections.reverse(keySet);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Choose a PlayList:").setIcon(R.drawable.ic_menu_slideshow)
//                .setItems(playlistNames.keySet().toArray(new CharSequence[playlistNames.keySet().size()]),
//                        new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                String name = playlistNames.keySet()
//                                        .toArray(new CharSequence[playlistNames.keySet().size()])[which].toString();
//                                Log.i(TAG, "You Chose Playlist:" + name);
//                                if(!name.equals(context.getString(R.string.all_songs)))
//                                {
//                                    playListToAdd[0] = playlistNames.get(name);
//                                    PlayListManager.addToPlayList(context,playListToAdd[0],songCursor, positionsVec);
//                                }
//                            }
//                        })
        .setSingleChoiceItems(keySet.toArray(new CharSequence[keySet.size()]),0,
                new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String name = keySet.get(which);
                Log.i(TAG, "You Chose Playlist:" + name);
                if(!name.equals(context.getString(R.string.all_songs)))
                {
                    playListToAdd[0] = playlistNames.get(name);
                    PlayListManager.addToPlayList(context,playListToAdd[0],songCursor, positionsVec);
                }
                dialog.dismiss();
            }
        });



        AlertDialog ret = builder.create();
        ListView listView=ret.getListView();
        // set devider

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            listView.setDivider(context.getDrawable(R.drawable.list_divider));
        }else{
            listView.setDivider(context.getResources().getDrawable(R.drawable.list_divider));
        }

        listView.setDividerHeight(2); // set height
        listView.setFooterDividersEnabled(false);
        listView.setHeaderDividersEnabled(false);


        return ret;
    }

    void showAreYouSureDialog(String songName, String PlaylistName,
                                Cursor songCursor, HashMap<String,Integer> playlistNamesMap, ArrayList<Integer> positionVec)
    {
        areYouSureDialog = BuildAreYouSureDialog(songName,PlaylistName,songCursor,playlistNamesMap,positionVec);
        areYouSureDialog.show();
    }

    void dismissAreYouSureDialog()
    {
        areYouSureDialog.dismiss();
    }

    private Dialog BuildAreYouSureDialog(final String songName, final String PlaylistName,
                                         final Cursor songCursor, final HashMap<String,Integer> playlistNamesMap
                                            ,final ArrayList<Integer> positionVec)
    {
        String title = context.getString(R.string.deleting_song) + songName
                + context.getString(R.string.from_playlist) +PlaylistName;

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title).setIcon(R.drawable.rubbish_bin)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PlayListManager.removeFromPlayList(context,playlistNamesMap.get(PlaylistName),songCursor, positionVec);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        areYouSureDialog.dismiss();
                    }
                });

        return builder.create();
    }

    void showCreatePlayListDialog()
    {
        createPlayList.show();
    }

    void dismissCreatePlayListDialog()
    {
        createPlayList.dismiss();
    }

    private Dialog buildCreatePlayList()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        builder.setView(inflater.inflate(R.layout.create_playlist_layout, null))
                .setTitle("Enter Playlist Name").setIcon(R.drawable.ic_library_add_black_24dp)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        EditText playlistNameEditText = (EditText)createPlayList.findViewById(R.id.playlist_name);
                        String playListName = playlistNameEditText.getText().toString();

                        PlayListManager.createPlayList(context,playListName);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        createPlayList.dismiss();
                    }
                });

        return builder.create();
    }

    Dialog buildDeletePlayList(String playlistName,final int PlaylistId)
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        builder.setTitle("Do You Want To Delete Playlist - " + playlistName).setIcon(R.drawable.rubbish_bin)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PlayListManager.deletePlaylist(context,PlaylistId);
                        ((MainActivity)context).setPlayListsAsMenuItem(((MainActivity) context).navigationView);
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        deletePlaylist.dismiss();
                    }
                });
        return builder.create();
    }

    void showDeletePlayListDialog(String playlistName,int PlaylistId)
    {
        deletePlaylist = buildDeletePlayList(playlistName,PlaylistId);
        deletePlaylist.show();
    }

    void dismissDeletePlayListDialog()
    {
        if(deletePlaylist!=null){
            deletePlaylist.dismiss();
        }
    }

    void showNoSettingsPage()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View adParentView =  inflater.inflate(R.layout.no_settings_layout, null);

        NativeExpressAdView adView = (NativeExpressAdView) adParentView.findViewById(R.id.native_medium_ad);

        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        builder.setView(adParentView);
        builder.setTitle(R.string.no_settings_yet);
        builder.setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    void showPleaseGrantChangeSettingsPermission()
    {
        AlertDialog.Builder builder =new AlertDialog.Builder(context);

        builder.setTitle("Please grant Write-Settings Permission to SoundTone");
        builder.setMessage("SoundTone Can't preform without the right to change your settings");
        builder.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                openAndroidPermissionsMenu();
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    void showPleaseGrantAutoStartPermission()
    {
        AlertDialog.Builder builder =new AlertDialog.Builder(context);

        builder.setTitle("Please grant AutoStart Permission to SoundTone");
        builder.setMessage("SoundTone Can't preform without the right to AutoStart");
        builder.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                //this will open auto start screen where user can enable permission for your app
                Intent intent = new Intent();
                intent.setComponent(new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"));
                context.startActivity(intent);
            }
        });
        builder.setNegativeButton("I've Granted that Permission", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
                sharedPreferences.edit().putBoolean(AUTO_STAT_PERMISSION_GRANTED,true).apply();
                dialog.dismiss();
            }
        });

        builder.create().show();
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void openAndroidPermissionsMenu() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
        intent.setData(Uri.parse("package:" + context.getPackageName()));
        context.startActivity(intent);
    }

    void showMakesSureYoursUsingYourDefaultAlarm()
    {
        AlertDialog.Builder builder =new AlertDialog.Builder(context);

        builder.setTitle("Make sure you are using a DEFAULT alarm tone");
        builder.setMessage("SoundTone changes your default alarm tone.\nmake sure you are using it when setting an alarm or in your currently set alarms...");
        builder.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        builder.setNegativeButton("How?", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String how = "how to set alarm as default ringtone ";
                String deviceManufacturer = android.os.Build.MANUFACTURER;
                dialog.dismiss();
                issueGoogleSearch(how + deviceManufacturer);
            }
        });

        builder.create().show();
    }

    void issueGoogleSearch(String what)
    {
        Uri uri = Uri.parse("http://www.google.com/#q=" + what);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        context.startActivity(intent);
    }

    void showSoundToneCantFunctionWithoutPermissions()
    {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        builder.setTitle("SoundTone Can't Function Without These Permissions");
        builder.setMessage("To use SoundTone please restart the app and grant the requested permissions.");
        builder.setPositiveButton(context.getString(R.string.ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
                ((MainActivity)context).finish();
            }
        });

        builder.create().show();
    }

}
