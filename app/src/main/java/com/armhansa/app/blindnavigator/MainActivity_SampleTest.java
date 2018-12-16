package com.armhansa.app.blindnavigator;

import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.VideoView;

import org.opencv.videoio.VideoCapture;

public class MainActivity_SampleTest extends AppCompatActivity
        implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener {

    private static final String TAG = "MainActivity_SampleTest";

    VideoCapture videoCapture;
    VideoView videoView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_test);

        videoView = findViewById(R.id.show_video);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/"
                + R.raw.dataset);
        videoView.setVideoURI(videoUri);
        videoView.start();
        videoView.setOnPreparedListener(this);
        videoView.setOnCompletionListener(this);

    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onPrepared: ");
    }

    @Override
    public void onCompletion(MediaPlayer mediaPlayer) {
        Log.d(TAG, "onCompletion: ");
    }

}
