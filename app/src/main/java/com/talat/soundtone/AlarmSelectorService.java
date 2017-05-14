package com.talat.soundtone;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import java.util.Random;
import java.util.logging.LogRecord;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class AlarmSelectorService extends IntentService {

    private static final String TAG = "SOUNDTONE-AlarmSelector";

    // IntentService can perform, e.g. ACTION_FETCH_NEW_ITEMS
    private static final String ACTION_SELECT_ALARM = "com.talat.soundtone.action.SELECT_ALARM";

    private static final String EXTRA_PLAYLIST = "com.talat.soundtone.extra.PARAM1";

    private boolean isPlaylistSong = true;
    int mId = 739;


    public AlarmSelectorService() {
        super("AlarmSelectorService");
    }

    /**
     * Starts this service to perform action ActionNextAlarm with the given parameters. If
     * the service is already performing a task this action will be queued.
     *
     * @see IntentService
     */
    // TODO: Customize helper method
    public static void startActionNextAlarm(Context context, Integer playlistID) {
        Intent intent = new Intent(context, AlarmSelectorService.class);
        intent.setAction(ACTION_SELECT_ALARM);
        intent.putExtra(EXTRA_PLAYLIST, playlistID);
        context.startService(intent);
    }



    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_SELECT_ALARM.equals(action)) {
                final Integer PlaylistID = intent.getIntExtra(EXTRA_PLAYLIST,-1);
                handleActionNextAlarm(PlaylistID);
            }
        }
    }

    /**
     * Handle action NextAlarm in the provided background thread with the provided
     * parameters.
     */
    private void handleActionNextAlarm(Integer playList) {
        Cursor songCursor = getRandomizedPlaylist(playList);

        if(songCursor != null) {
            if (songCursor.moveToFirst()) {
                Log.i(TAG,"Setting songCursor as default media");
//                if(SettingsManager.getOrder() == SettingsManager.RANDOM)
//                {

                Random rand = new Random();
                int randomNamber = rand.nextInt(songCursor.getCount());
                if(songCursor.moveToPosition(randomNamber))
                {
                    Log.i(TAG,"Song at random position" + randomNamber + " success");
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                    {
                        if(Settings.System.canWrite(this))
                            setMediaAsDefaultRingtone(this,songCursor,isPlaylistSong);
                        else
                            notifyUserDefaultToneChanged("Can't Change default ringtone.",
                                    "please start SoundTone and approve all permissions");
                    }else{
                        setMediaAsDefaultRingtone(this,songCursor,isPlaylistSong);
                    }

                }else{
                    Log.i(TAG,"Song at random position" + randomNamber + " failed!!!!!!");
                }
//                }
            }
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                if (Settings.canDrawOverlays(this))
//                    addViewToScreen();
//            }
        }
    }

    private Cursor getRandomizedPlaylist(Integer playListID) {
        if(playListID == -1)
        {
            Log.i(TAG,"getiing AllMedia");
            isPlaylistSong = false;
            return getAllMedia();
        }
        Log.i(TAG,"getiing playlistSongs");
        isPlaylistSong = true;
        return PlayListManager.getPlaylistSongs(this,playListID);
    }


    public Cursor getAllMedia()
    {
        ContentResolver contentResolver= getContentResolver();
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
        String order ="RANDOM()";//MediaStore.Audio.Media.TITLE + " ASC";
        return contentResolver.query(uri,projection,selection,null,order);
    }

    public static void setMediaAsDefaultRingtone(Context context, Cursor MediaCursor,boolean isPlaylistSong)
    {
        //Get newMediaPath
        String newMediaPath = MediaCursor.getString(MediaCursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
        Log.i(TAG,"new Media Path is: " + newMediaPath);
        SharedPreferences sharedPreferences = context.getSharedPreferences(context.getPackageName(), Context.MODE_PRIVATE);
        //get latest Media Path from SharedPreferences
        String oldMediaFilePath = sharedPreferences.getString("OLD_FILE_PATH",null);
        Log.i(TAG,"old Media Path is: " + oldMediaFilePath);
        //update old media path value MediaStore.Audio.Media.IS_ALARM false
        if(oldMediaFilePath!= null)
        {
            ContentValues values = new ContentValues();
            values.put(MediaStore.Audio.Media.IS_ALARM,false);
            context.getContentResolver().update(MediaStore.Audio.Media.getContentUriForPath(oldMediaFilePath),
                    values,
                    MediaStore.MediaColumns.DATA + "=\"" +oldMediaFilePath +"\"", null);

            //Toast.makeText(context,"Changed " + oldMediaFilePath + " to not be an alarm",Toast.LENGTH_SHORT).show();
        }

        //update new media path value MediaStore.Audio.Media.IS_ALARM true
        ContentValues values = new ContentValues();
        values.put(MediaStore.Audio.Media.IS_ALARM,true);
        int num_rows = context.getContentResolver().update(MediaStore.Audio.Media.getContentUriForPath(newMediaPath),
                values,
                MediaStore.MediaColumns.DATA + "= ?",new String[]{newMediaPath});
        if(num_rows != 1)
        {
            Log.i(TAG,"Updated more then one row - check your WHERE clause : num_rows = " + num_rows);
        }

        Uri newMediaUri;
        if (isPlaylistSong)
        {
            newMediaUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    MediaCursor.getInt(MediaCursor.getColumnIndexOrThrow(MediaStore.Audio.Playlists.Members.AUDIO_ID)));
        }else{
            newMediaUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    MediaCursor.getInt(MediaCursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)));
        }
        //make uri

        Log.i(TAG,"the new media URI: " + newMediaUri);

        //set new media path as default ringtone
        RingtoneManager.setActualDefaultRingtoneUri(context,RingtoneManager.TYPE_ALARM,newMediaUri);
        //Toast.makeText(context,newMediaPath + " has been set as default ringtone",Toast.LENGTH_SHORT).show();
        String Title = "Default AlarmTone changed";
        String Text = "SoundTone has changed your default alarm tone to " +newMediaPath
                + "\nCreate more playlists";
        if(context instanceof AlarmSelectorService)
        {
            ((AlarmSelectorService)context).notifyUserDefaultToneChanged(Title,Text);
        }
        sharedPreferences.edit().putString("OLD_FILE_PATH",newMediaPath).apply();
    }

    void addViewToScreen()
    {
        LayoutInflater li = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        final WindowManager wm = (WindowManager) getSystemService(WINDOW_SERVICE);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                //WindowManager.LayoutParams.TYPE_INPUT_METHOD |
                WindowManager.LayoutParams.TYPE_SYSTEM_OVERLAY,// | WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH //| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                ,PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.CENTER;
        final View adView = li.inflate(R.layout.adoverlay_layout, null);

        wm.addView(adView, params);

        try {
            Thread.sleep(10000);
        } catch (InterruptedException e) {
            Log.d(TAG,"Sleep was interrupted");
        }

        wm.removeView(adView);

    }

    void notifyUserDefaultToneChanged(String Title ,String text)
    {
        NotificationCompat.Builder mBuilder =
            new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.logo_orange_transparen)
                    .setContentTitle(Title)
                    .setContentText(text)
                    .setStyle(new NotificationCompat.BigTextStyle().bigText(text));

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, MainActivity.class);

        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(MainActivity.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(
                        0,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );
        mBuilder.setContentIntent(resultPendingIntent);
        NotificationManager mNotificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        // mId allows you to update the notification later on.
        mNotificationManager.cancel(mId);
        mNotificationManager.notify(mId, mBuilder.build());
    }

}
