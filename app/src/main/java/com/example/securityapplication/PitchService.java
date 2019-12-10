package com.example.securityapplication;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.pitch.AMDF;
import be.tarsos.dsp.pitch.DynamicWavelet;
import be.tarsos.dsp.pitch.FFTPitch;
import be.tarsos.dsp.pitch.FastYin;
import be.tarsos.dsp.pitch.McLeodPitchMethod;
import be.tarsos.dsp.pitch.PitchDetectionHandler;
import be.tarsos.dsp.pitch.PitchDetectionResult;
import be.tarsos.dsp.pitch.PitchDetector;
import be.tarsos.dsp.pitch.PitchProcessor.PitchEstimationAlgorithm;
import be.tarsos.dsp.pitch.Yin;

public class PitchService extends Service implements AudioProcessor {

    public enum PitchEstimationalgorithm {
        AMDF, MPM, FFT_YIN, DYNAMIC_WAVELET, FFT_PITCH;

        public PitchDetector getDetector(float sampleRate, int bufferSize) {
            PitchDetector detector;
            if (this == MPM) {
                detector = new McLeodPitchMethod(sampleRate, bufferSize);
            } else if (this == DYNAMIC_WAVELET) {
                detector = new DynamicWavelet(sampleRate, bufferSize);
            } else if (this == FFT_YIN) {
                detector = new FastYin(sampleRate, bufferSize);
            } else if (this == AMDF) {
                detector = new AMDF(sampleRate, bufferSize);
            } else if (this == FFT_PITCH) {
                detector = new FFTPitch(Math.round(sampleRate), bufferSize);
            } else {
                detector = new Yin(sampleRate, bufferSize);
            }
            return detector;
        }
    }


    private PitchDetector detector;
    private PitchDetectionHandler pitchDetectionHandler;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public PitchService(PitchEstimationAlgorithm algorithm, float sampleRate,
                        int bufferSize,
                        PitchDetectionHandler handler) {
        this.detector = algorithm.getDetector(sampleRate, bufferSize);
        this.pitchDetectionHandler = handler;

    }


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public boolean process(AudioEvent audioEvent) {
        Intent i = new Intent("Pitch_inHz");
        float[] audioFloatBuffer = audioEvent.getFloatBuffer();
        PitchDetectionResult result = detector.getPitch(audioFloatBuffer);
        pitchDetectionHandler.handlePitch(result,audioEvent);
        float pitch = result.getPitch();
        Log.d("PitchService", "pitchInHz"+pitch);
        Toast.makeText(getApplicationContext(), "pitch"+pitch, Toast.LENGTH_SHORT).show();
        sendBroadcast(i);
        return true;
    }

    @Override
    public void processingFinished() {

    }
}
