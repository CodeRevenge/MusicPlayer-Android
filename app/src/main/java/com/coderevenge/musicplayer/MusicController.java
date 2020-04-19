package com.coderevenge.musicplayer;

import android.content.Context;
import android.widget.MediaController;

// The MediaController class presents a standard widget with play/pause, rewind, fast-forward, skip(prev/next) buttons in it.
public class MusicController extends MediaController {
    public MusicController(Context context) {
        super(context);
    }

    public void hide() {
    }
}