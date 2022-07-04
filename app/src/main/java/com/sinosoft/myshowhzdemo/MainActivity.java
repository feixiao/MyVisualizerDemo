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
                visualizer.setEnabled(false);   //  歌曲流完毕，则不再采样
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
        // 这一底层实现的方法来返回一个采样值的范围数组，0为最小值128,1为最大值1024！采样值都为2的n次幂！
        visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);

        Log.e("CaptureSizeRange",Visualizer.getCaptureSizeRange()[1]+"");//0为128；1为1024

        // 设置监听器 setDataCaptureListener(Visualizer.OnDataCaptureListener listener, int rate, boolean waveform, boolean fft)
        // 如果waveform为true，fft为false则会回调onWaveFormDataCapture方法
        // 如果waveform为false，fft为true则会回调onFftDataCapture方法。
        visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener(){

            // waveform是波形采样的字节数组，它包含一系列的8位（无符号）的PCM单声道样本
            @Override
            public void onWaveFormDataCapture(Visualizer visualizer, byte[] waveform, int samplingRate) {
                Log.e("onWaveFormDataCapture","调用了！");
                visualizerView.updateVisualizer(waveform);
            }


            // 数字信号处理相关的知识，FFT（Fast Fourier Transformation），即快速傅里叶转换，它用于把时域上连续的信号(波形)强度转换成离散的频域信号(频谱)
            // fft是经过FFT转换后频率采样的字节数组，频率范围为0（直流）到采样值的一半
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
        } , Visualizer.getMaxCaptureRate()/2, true, false );

        Log.e("采样频率",Visualizer.getMaxCaptureRate()/2+"");//10000mHz=10Hz

        // 这个设置必须在参数设置之后，表示开始采样
        visualizer.setEnabled(true);
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
