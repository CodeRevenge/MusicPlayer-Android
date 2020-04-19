package com.coderevenge.musicplayer;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

// Map the songs list to the ListView
public class SongAdapter extends BaseAdapter {
    private ArrayList<Song> songs;
    // Map the title and artist strings to the TextView
    private LayoutInflater songInf;

    public SongAdapter(Context c, ArrayList<Song> theSongs) {
        songs = theSongs;
        songInf = LayoutInflater.from(c);
    }

    @Override
    public int getCount() {
        return songs.size();
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Map to song layout
        LinearLayout songLay = (LinearLayout) songInf.inflate(R.layout.song, parent, false);

        // Get title and artist views
        TextView songView = (TextView)songLay.findViewById(R.id.song_title);
        TextView artistView = (TextView)songLay.findViewById(R.id.song_artist);

        // Get song using position
        Song currSong = songs.get(position);

        // Get title and artist strings
        songView.setText(currSong.getTitle());
        artistView.setText(currSong.getArtist());

        // Set position as tag
        songLay.setTag(position);

        return songLay;
    }
}