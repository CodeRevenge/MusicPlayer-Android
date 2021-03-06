package com.coderevenge.musicplayer;

import android.Manifest;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;
import android.widget.MediaController;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

// ActionBarActivity is deprecated since v22.1.0
public class MainActivity extends AppCompatActivity implements MediaController.MediaPlayerControl {

    private ArrayList<Song> songList;
    private ListView songListView;
    // We play music in the Service class, but control it from the Activity class, where the application's user interface operates
    private MusicService musicService;
    // Intent is used for a wide variety of tasks, but most often they're used to start another activity
    private Intent playIntent;
    // A flag to keep track of whether the Activity class is bound to the Service class or not
    private boolean musicBound = false;
    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;

    private View lastSongPicked;

    private ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            musicService.setList(songList);

            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };
    private MusicController musicController;
    private boolean paused = false, playbackPaused = false;

    @Override
    protected void onStop() {
        musicController.hide();
        super.onStop();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (paused) {
            setMusicController();
            paused = false;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        paused = true;
    }

    private void setMusicController() {
        if (musicController == null) {
            musicController = new MusicController(this);

            musicController.setPrevNextListeners(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    playNext();
                }
            }, new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    playPrev();
                }
            });

            musicController.setMediaPlayer(this);
            musicController.setAnchorView(findViewById(R.id.principal));
            musicController.setEnabled(true);

        }
    }

    private void playNext() {
        musicService.playNext();
        if (playbackPaused) {
            setMusicController();
            playbackPaused = false;
        }

        musicController.show(0);

        updatePlaySelection();
    }

    private void playPrev() {
        musicService.playPrev();
        if (playbackPaused) {
            setMusicController();
            playbackPaused = false;
        }

        musicController.show(0);
        updatePlaySelection();
    }

    private void updatePlaySelection() {
        lastSongPicked.setBackgroundColor(0x00FFFFFF);
        lastSongPicked = songListView.getChildAt(musicService.getCurrentSongPosition());
        lastSongPicked.setBackgroundColor(0xFF516D8F);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        songListView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<>();
        requestRead();

        // Sort the title of songs in songList alphabetically
        Collections.sort(songList, new Comparator<Song>() {
            @Override
            public int compare(Song lhs, Song rhs) {
                return lhs.getTitle().compareTo(rhs.getTitle());
            }
        });

        // Map the songList to the songListView
        SongAdapter songAdapter = new SongAdapter(this, songList);
        songListView.setAdapter(songAdapter);

        setMusicController();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (playIntent == null) {
            // Connect to the service
            playIntent = new Intent(this, MusicService.class);
            playIntent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            bindService(playIntent, serviceConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicService = null;
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        switch (id) {
            case R.id.action_shuffle:
                musicService.setShuffle();
                if (musicService.isShuffle()) item.setIcon(R.drawable.shuffle_on);
                else item.setIcon(R.drawable.shuffle_off);
                break;

            case R.id.action_end:
                stopService(playIntent);
                musicService = null;
                System.exit(0);
                break;

            case R.id.action_repeat:
                musicService.setRepeat();
                if (musicService.isRepeat()) item.setIcon(R.drawable.repeat_on);
                else item.setIcon(R.drawable.repeat_off);
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    public void requestRead() {
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
        } else {
            getSongList();
        }
    }


    public void getSongList() {
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            // get columns
            int titleColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

            // add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new Song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }
    }

    public void songPicked(View view) {
        musicService.setCurrentSongPosition(Integer.parseInt(view.getTag().toString()));
        musicService.playSong();


        if (lastSongPicked != null) {
            lastSongPicked.setBackgroundColor(0x00FFFFFF);
        }
        view.setBackgroundColor(0xFF516D8F);
        lastSongPicked = view;

        if (playbackPaused) {
            setMusicController();
            playbackPaused = false;
        }
        musicController.show(0);
    }

    @Override
    public void start() {
        musicService.go();
    }

    @Override
    public void pause() {
        playbackPaused = true;
        musicService.pausePlayer();
    }

    @Override
    public int getDuration() {
        if (musicService != null && musicBound && musicService.isPlaying()) {
            return musicService.getDuration();
        }

        return 0;
    }

    @Override
    public int getCurrentPosition() {
        if (musicService != null && musicBound && musicService.isPlaying()) {
            return musicService.getCurrentPosition();
        }

        return 0;
    }

    @Override
    public void seekTo(int pos) {
        musicService.seek(pos);
    }

    @Override
    public boolean isPlaying() {
        if (musicService != null && musicBound) {
            return musicService.isPlaying();
        }

        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}