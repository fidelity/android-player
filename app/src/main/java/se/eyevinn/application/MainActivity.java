package se.eyevinn.application;

import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Bundle;
import android.os.Process;
import android.system.Os;
import android.system.OsConstants;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.ui.AspectRatioFrameLayout;
import com.google.android.exoplayer2.ui.PlayerView;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.video.VideoRendererEventListener;
import com.google.android.flexbox.FlexDirection;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.FlexboxLayout;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.net.URI;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity implements VideoRendererEventListener {

    public static final String HLS_VOD = "https://f53accc45b7aded64ed8085068f31881.egress.mediapackage-vod.eu-north-1.amazonaws.com/out/v1/1c63bf88e2664639a6c293b4d055e6bb/ade303f83e8444d69b7658f988abb054/2a647c0cf9b7409598770b9f11799178/manifest.m3u8";
    public static final String MPD_VOD = "https://f53accc45b7aded64ed8085068f31881.egress.mediapackage-vod.eu-north-1.amazonaws.com/out/v1/1c63bf88e2664639a6c293b4d055e6bb/64651f16da554640930b7ce2cd9f758b/66d211307b7d43d3bd515a3bfb654e1c/manifest.mpd";
    public static final String HLS_LIVE = "https://d2fz24s2fts31b.cloudfront.net/out/v1/6484d7c664924b77893f9b4f63080e5d/manifest.m3u8";
    public static final String MPD_LIVE = "https://d2fz24s2fts31b.cloudfront.net/out/v1/3b6879c0836346c2a44c9b4b33520f4e/manifest.mpd";
    public static final String HLS_LIVE_SSAI = "https://edfaeed9c7154a20828a30a26878ade0.mediatailor.eu-west-1.amazonaws.com/v1/master/1b8a07d9a44fe90e52d5698704c72270d177ae74/AdTest/master.m3u8";

    private static final String TAG = "MainActivity";
    private SimpleExoPlayer player;
    private Timer timer;
    private static long numCores = Os.sysconf(OsConstants._SC_NPROCESSORS_CONF);
    private static long clockSpeedHz = Os.sysconf(OsConstants._SC_CLK_TCK);
    private static int appPID = Process.myPid();
    private static CpuMetrics cpuMetrics = new CpuMetrics();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setupPlayer();
        loadButtonListener();
        hlsVodButtonListener();
        mpdVodButtonListener();
        hlsLiveButtonListener();
        mpdLiveButtonListener();
        hlsLiveSsaiButtonListener();
        showDataButtonListener();
    }

    private void setupPlayer() {
        if (player == null) {
            DefaultTrackSelector trackSelector = new DefaultTrackSelector(this);
            trackSelector.setParameters(
                    trackSelector.buildUponParameters().setMaxVideoSizeSd());
            player = new SimpleExoPlayer.Builder(this)
                    .setTrackSelector(trackSelector)
                    .build();
        }
        PlayerView playerView = findViewById(R.id.exo_player_view);
        playerView.setPlayer(player);
        playerView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        EditText edtView = (EditText) findViewById(R.id.inputtext);
        View.OnFocusChangeListener ofcListener = new MyFocusChangeListener();
        edtView.setOnFocusChangeListener(ofcListener);
        player.setPlayWhenReady(true); //run file/link when ready to play.
        player.addListener(new Player.Listener(){
            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                if (isPlaying) {
                    findViewById(R.id.streamControls).setVisibility(View.GONE);
                    getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    // Active playback.
                } else {
                    // Not playing because playback is paused, ended, suppressed, or the player
                    // is buffering, stopped or failed. Check player.getPlayWhenReady,
                    // player.getPlaybackState, player.getPlaybackSuppressionReason and
                    // player.getPlaybackError for details.
                    findViewById(R.id.streamControls).setVisibility(View.VISIBLE);
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                //(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                }
            }
        });
    }

    private void hlsVodButtonListener() {
        Button hlsButton = (Button) findViewById(R.id.hlsVodButton);
        hlsButton.setOnClickListener(view -> {
            EditText editText = (EditText) findViewById(R.id.inputtext);
            editText.setText(HLS_VOD, TextView.BufferType.EDITABLE);
            editText.clearFocus();
        });
    }

    private void hlsLiveButtonListener() {
        Button hlsButton = (Button) findViewById(R.id.hlsLiveButton);
        hlsButton.setOnClickListener(view -> {
            EditText editText = (EditText) findViewById(R.id.inputtext);
            editText.setText(HLS_LIVE, TextView.BufferType.EDITABLE);
            editText.clearFocus();
        });
    }

    private void mpdVodButtonListener() {
        Button hlsButton = (Button) findViewById(R.id.mpdVodButton);
        hlsButton.setOnClickListener(view -> {
            EditText editText = (EditText) findViewById(R.id.inputtext);
            editText.setText(MPD_VOD, TextView.BufferType.EDITABLE);
            editText.clearFocus();
        });
    }

    private void mpdLiveButtonListener() {
        Button hlsButton = (Button) findViewById(R.id.mpdLiveButton);
        hlsButton.setOnClickListener(view -> {
            EditText editText = (EditText) findViewById(R.id.inputtext);
            editText.setText(MPD_LIVE, TextView.BufferType.EDITABLE);
            editText.clearFocus();
        });
    }

    private void hlsLiveSsaiButtonListener() {
        Button hlsButton = (Button) findViewById(R.id.hlsLiveSsaiButton);
        hlsButton.setOnClickListener(view -> {
            EditText editText = (EditText) findViewById(R.id.inputtext);
            editText.setText(HLS_LIVE_SSAI, TextView.BufferType.EDITABLE);
            editText.clearFocus();
        });
    }

    private void loadButtonListener() {
        Button button = (Button) findViewById(R.id.loadButton);
        button.setOnClickListener(view -> {
            PlayerView pv = (PlayerView) findViewById(R.id.exo_player_view);
            pv.setVisibility(View.VISIBLE);
            EditText editText = (EditText) findViewById(R.id.inputtext);
            editText.clearFocus();
            EditText inputText = (EditText) findViewById(R.id.inputtext);
            String message = inputText.getText().toString();
            if (isVideoUrl(message)) {
                playStreamInPlayer(message);
            } else {
                new TaskGetSourceList((sourceList -> { onSourcesLoaded(sourceList); }))
                        .execute(message);
            }
        });
    }

    private void onSourcesLoaded(TaskGetSourceList.SourceList sourceList) {
        LinearLayout buttonPanel1 = findViewById(R.id.buttonpanels);
        buttonPanel1.removeAllViews();
        FlexboxLayout flexbox = new FlexboxLayout(getApplicationContext());
        flexbox.setFlexDirection(FlexDirection.ROW);
        flexbox.setFlexWrap(FlexWrap.WRAP);
        for(TaskGetSourceList.Source s : sourceList.getSourceList()) {
            Button b = new Button(this);
            b.setText(s.getName());
            b.setTooltipText(s.getUrl());
            b.setOnClickListener(view -> {
                playStreamInPlayer(s.getUrl());
            });
           flexbox.addView(b);
        }
        buttonPanel1.addView(flexbox);
    }

    private boolean isVideoUrl(String url) {
        URI uri = URI.create(url);
        String path = uri.getPath();
        return path.endsWith(".m3u8") || path.endsWith(".mpd") || path.endsWith(".mp4");
    }

    private void showDataButtonListener() {
        Button button = (Button) findViewById(R.id.showDataButton);
        RelativeLayout layout = (RelativeLayout) findViewById(R.id.hwMetrics);
        button.setOnClickListener(view -> {
            if(layout.getVisibility() == View.VISIBLE) {
                System.out.println("Hiding metrics");
                layout.setVisibility(View.GONE);
                timer.cancel();
            } else {
//                startTime = System.currentTimeMillis();
                cpuMetrics.setStartTime(System.currentTimeMillis());
                timer = new Timer();
                layout.setVisibility(View.VISIBLE);
                System.out.println("Showing metrics");
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        getHardwareMetrics();
                    }
                },0, 1000);
            }
        });
    }

    private void getHardwareMetrics() {
        updateMemoryMetrics();
        updateBatteryMetrics();
        updateCpuMetrics();
    }

    private void updateCpuMetrics() {
        TextView cpuText = findViewById(R.id.cpuText);
        try {
            File file = new File(String.format("/proc/%s/stat", appPID));
            Scanner reader = new Scanner(file);
            String statResult = "";
            while(reader.hasNextLine()) {
                statResult += reader.nextLine();
            }
            long currTime = System.currentTimeMillis();
            String[] splitStatResult = statResult.split(" ");

            float cpuTimeSec = calcCpuTime(splitStatResult, cpuMetrics);
            float avgCpuUsage = ((100 * (cpuTimeSec - cpuMetrics.getCpuTimeSec() / (currTime - cpuMetrics.getStartTime()))) / numCores);
            cpuText.setText(String.format("CPU: %.2f%%", (double) Math.abs(avgCpuUsage)));
            cpuMetrics.updateCpuMetrics(currTime, cpuTimeSec);
            cpuMetrics.updateStatMetrics(splitStatResult);
            reader.close();
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private float calcCpuTime(String[] stat, CpuMetrics cpuMetrics) {
        int dUTime = Integer.parseInt(stat[14]) - cpuMetrics.getUtime();
        int dSTime = Integer.parseInt(stat[15]) - cpuMetrics.getStime();
        int dCuTime = Integer.parseInt(stat[16]) - cpuMetrics.getCutime();
        int dCsTime = Integer.parseInt(stat[17]) - cpuMetrics.getCstime();

        return (float)(dUTime + dSTime + dCuTime + dCsTime) / clockSpeedHz;
    }

    private void updateBatteryMetrics() {
        ProgressBar batteryBar = findViewById(R.id.batteryBar);
        TextView batteryText = findViewById(R.id.batteryText);
        Intent batteryStatus = this.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if(checkIfPluggedIn(batteryStatus)) {
            this.runOnUiThread(() -> {
                batteryBar.setVisibility(View.GONE);
                batteryText.setVisibility(View.GONE);
            });
        } else {
            int batteryLvl = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int batteryScale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPercentage = batteryLvl * 100 / (float) batteryScale;
            this.runOnUiThread(() -> {
                if(batteryBar.getVisibility() != View.VISIBLE) {
                    batteryBar.setVisibility(View.VISIBLE);
                    batteryText.setVisibility(View.VISIBLE);
                }
                batteryBar.setProgress(batteryLvl);
                batteryText.setText(String.format("%.2f%%", (double) batteryPercentage));
            });
        }
    }

    private boolean checkIfPluggedIn(Intent intent) {
        int isPlugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        return isPlugged != 0;
    }

    private void updateMemoryMetrics() {
        ActivityManager activityManager = (ActivityManager) this.getSystemService(ACTIVITY_SERVICE);
        ProgressBar memBar = findViewById(R.id.memBar);
        TextView memText = findViewById(R.id.memText);
        Runtime info = Runtime.getRuntime();
        ActivityManager.MemoryInfo mem = new ActivityManager.MemoryInfo();
        activityManager.getMemoryInfo(mem);
        int percent = (int)Math.ceil((1.0 - ((double)mem.availMem / mem.totalMem)) * 100);
        this.runOnUiThread(() -> {
            memBar.setProgress(percent);
            memText.setText(String.format("%.2fGB", ((double)mem.availMem / 1024 / 1024 / 1024)));
        });
    }

    private void playStreamInPlayer(String url) {
        if (url.endsWith("m3u8")) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_M3U8)
                    .build();
            player.setMediaItem(mediaItem);

        } else if (url.endsWith("mpd")) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_MPD)
                    .build();
            player.setMediaItem(mediaItem);
        } else if (url.endsWith("mp4")) {
            MediaItem mediaItem = new MediaItem.Builder()
                    .setUri(url)
                    .setMimeType(MimeTypes.APPLICATION_MP4)
                    .build();
            player.setMediaItem(mediaItem);
        } else {
            Log.v(TAG, "Invalid url");
        }
        player.setPlayWhenReady(true);
        player.seekTo(0, 0);
        player.prepare();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.v(TAG, "onDestroy()...");
        player.release();
    }




    private class MyFocusChangeListener implements View.OnFocusChangeListener {
        public void onFocusChange(View v, boolean hasFocus) {
            if (v.getId() == R.id.inputtext && !hasFocus) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
            }
        }
    }
}