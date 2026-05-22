package com.example.codecompiler;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Remove the default Android 12+ splash icon as quickly as possible
        // Note: For full control, we use our custom activity.
        
        // Make the activity fullscreen
        Window window = getWindow();
        WindowCompat.setDecorFitsSystemWindows(window, false);
        WindowInsetsControllerCompat controller = new WindowInsetsControllerCompat(window, window.getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        setContentView(R.layout.activity_splash);

        VideoView videoView = findViewById(R.id.splashVideoView);

        try {
            Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.vac_animation);
            videoView.setVideoURI(videoUri);

            videoView.setOnPreparedListener(mp -> {
                // Adjust scaling to fill screen
                float videoWidth = mp.getVideoWidth();
                float videoHeight = mp.getVideoHeight();
                float viewWidth = videoView.getWidth();
                float viewHeight = videoView.getHeight();

                if (videoWidth > 0 && videoHeight > 0 && viewWidth > 0 && viewHeight > 0) {
                    float videoRatio = videoWidth / videoHeight;
                    float viewRatio = viewWidth / viewHeight;

                    if (videoRatio > viewRatio) {
                        videoView.setScaleX(videoRatio / viewRatio);
                    } else {
                        videoView.setScaleY(viewRatio / videoRatio);
                    }
                }
                videoView.start();
            });

            videoView.setOnCompletionListener(mp -> navigateToMain());
            videoView.setOnErrorListener((mp, what, extra) -> {
                navigateToMain();
                return true;
            });

        } catch (Exception e) {
            navigateToMain();
        }
    }

    private void navigateToMain() {
        if (!isFinishing()) {
            Intent intent = new Intent(SplashActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        }
    }
}
