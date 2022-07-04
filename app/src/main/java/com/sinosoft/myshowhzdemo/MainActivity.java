package com.sinosoft.myshowhzdemo;

import android.Manifest;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.media.audiofx.Visualizer;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private Visualizer visualizer;
    private VisualizerView visualizerView;



    private static final int PERM_REQ_CODE = 23;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        visualizerView= (VisualizerView) findViewById(R.id.myview);
        mediaPlayer=MediaPlayer.create(this,R.raw.goodbye_my_lover);
        init();
        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            public void onCompletion(MediaPlayer mediaPlayer) {
                visualizer.setEnabled(false);//歌曲流完毕，则不再采样
            }
        });
        mediaPlayer.start();
    }

    private boolean checkAudioPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private void requestAudioPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERM_REQ_CODE);
    }


    private void init(){

        if (!checkAudioPermission()){
            requestAudioPermission();
        }

        visualizer = new Visualizer(mediaPlayer.getAudioSessionId());
        visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
        Log.e("CaptureSizeRange",Visualizer.getCaptureSizeRange()[1]+"");//0为128；1为1024
        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener(){

            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                Log.e("onWaveFormDataCapture","调用了！");
                visualizerView.updateVisualizer(waveform);
            }

            @Override
            public void onFftDataCapture(Visualizer visualizer, byte[] fft, int samplingRate) {
                Log.e("onFftDataCapture","调用了！");
                byte[] model = new byte[fft.length / 2 + 1];
                //Log.e("fft",fft.length+"");
                Log.e("samplingRate",samplingRate+"");
                model[0] = (byte) Math.abs(fft[1]);
                int j = 1;

                for (int i = 2; i < 18;) {
                    model[j] = (byte) Math.hypot(fft[i], fft[i + 1]);
                    i += 2;
                    j++;
                }
                visualizerView.updateVisualizer(model);
            }
        } , Visualizer.getMaxCaptureRate()/2, false, true );
        Log.e("采样频率",Visualizer.getMaxCaptureRate()/2+"");//10000mHz=10Hz
        visualizer.setEnabled(true);//这个设置必须在参数设置之后，表示开始采样
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (isFinishing() && mediaPlayer != null) {
            visualizer.release();
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}
