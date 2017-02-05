package com.talat.soundtone;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.FileDescriptor;

/**
 * Created by talat on 15/09/16
 * .
 */
public class AllSongsCursorAdapter extends CursorAdapter {


    public AllSongsCursorAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item,parent,false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        TextView title = (TextView) view.findViewById(R.id.title);
        TextView artist = (TextView) view.findViewById(R.id.artist);
        TextView duration = (TextView) view.findViewById(R.id.duration);
        ImageView thumbnail = (ImageView) view.findViewById(R.id.list_image);

        String sTitle = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
        String sArtist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
        long sDuration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));

        long albumID = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID));
        Bitmap thumbnailBitmap = getAlbumart(context,albumID);

        long seconds = sDuration/1000;
        long minutes = seconds/60;
        seconds = seconds % 60;

        String sParsedDuration = Long.toString(minutes) +":" +
                (seconds<10 ? "0"+Long.toString(seconds):Long.toString(seconds));

        title.setText(sTitle);
        artist.setText(sArtist);
        duration.setText(sParsedDuration);
        if(thumbnail != null && thumbnailBitmap!=null)
        {
            thumbnail.setImageBitmap(thumbnailBitmap);
        }
    }

    public Bitmap getAlbumart(Context context,Long album_id)
    {
        Bitmap bm = null;
        try
        {
            final Uri sArtworkUri = Uri
                    .parse("content://media/external/audio/albumart");

            Uri uri = ContentUris.withAppendedId(sArtworkUri, album_id);

            ParcelFileDescriptor pfd = context.getContentResolver()
                    .openFileDescriptor(uri, "r");

            if (pfd != null) {
                FileDescriptor fd = pfd.getFileDescriptor();
                bm = BitmapFactory.decodeFileDescriptor(fd);
            }

        } catch (Exception e) {
            return null;
        }
        return bm;
    }
}
