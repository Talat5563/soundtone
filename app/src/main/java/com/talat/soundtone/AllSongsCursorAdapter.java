package com.talat.soundtone;

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by talat on 15/09/16
 * .
 */
public class AllSongsCursorAdapter extends CursorAdapter {

    private HashSet<Integer> selectedItemsHash = new HashSet<>();
    private ArrayList<Integer> selectedItemsVector = new ArrayList<>();

    public AllSongsCursorAdapter(Context context, Cursor c, boolean autoRequery) {
        super(context, c, autoRequery);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.list_item,parent,false);
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        RelativeLayout layout = (RelativeLayout) view.findViewById(R.id.list_item_layout);
        TextView title = (TextView) view.findViewById(R.id.title);
        TextView artist = (TextView) view.findViewById(R.id.artist);
        TextView duration = (TextView) view.findViewById(R.id.duration);
        ImageView thumbnail = (ImageView) view.findViewById(R.id.list_image);
        Integer position = cursor.getPosition();

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


        boolean isPressed = selectedItemsHash.contains(position);
        ChangeItemColor(context,layout,isPressed);

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

    public Integer getNumberOfPressedItems()
    {
        return selectedItemsHash.size();
    }

    public boolean AddSelectedPosition(int position)
    {
        if(selectedItemsHash.add(position))
        {
            selectedItemsVector.add(position);
            return true;
        }

        return false;
    }

    public void clearSelected()
    {
        selectedItemsVector.clear();
        selectedItemsHash.clear();
    }

    public boolean removeSelectedPosition(Integer position)
    {
        if(selectedItemsHash.remove(position))
        {
            selectedItemsVector.remove(position);
            return true;
        }

        return false;
    }

    public ArrayList<Integer> getSelectedVector()
    {
        return selectedItemsVector.isEmpty() ? null : selectedItemsVector;
    }

    private void ChangeItemColor(Context context, View view,boolean pressed)
    {
        if(pressed)
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.setBackgroundColor(ContextCompat.getColor(context,R.color.listItemColorPressed));
            }else{
                view.setBackgroundColor(context.getResources().getColor(R.color.listItemColorPressed));
            }
        }
        else
        {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                view.setBackgroundColor(ContextCompat.getColor(context,R.color.listItemColor));
            }else{
                view.setBackgroundColor(context.getResources().getColor(R.color.listItemColor));
            }
        }
    }
}
