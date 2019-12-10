package com.example.securityapplication;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class SpeechToTextService extends Service{

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    protected static AudioManager mAudioManager;
    protected SpeechRecognizer mSpeechRecognizer;
    protected Intent mSpeechRecognizerIntent;
    protected Messenger mServerMessenger =  new Messenger(new IncomingHandler(this));

    protected boolean mIsListening;
    protected volatile boolean mIsCountDownOn;
    private static boolean mIsStreamSolo;
    private boolean mRunning;

    static final int MSG_RECOGNIZER_START_LISTENING = 1;
    static final int MSG_RECOGNIZER_CANCEL = 2;

    public static final String TAG = "SpeechToTextService";
    @Override
    public void onCreate() {
        super.onCreate();
        mRunning = false;
        Log.d("SpeechToTextService", "Inside onCreate");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(!mRunning){
            mRunning = true;
            Log.d("SpeechToTextService", "Inside onStartCommand");
            mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

            mSpeechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);

            mSpeechRecognizer.setRecognitionListener(new SpeechRecognitionListener());

            mSpeechRecognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);

            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE,
                    this.getPackageName());

            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);

            mSpeechRecognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 100);

        }
        return super.onStartCommand(intent, flags, startId);
    }


    protected static class IncomingHandler extends Handler  {

        private WeakReference<SpeechToTextService> mtarget;

        public IncomingHandler(SpeechToTextService speechToTextService) {//Service target
            Log.d("SpeechToTextService", "Inside IncomingHandler");
            mtarget = new WeakReference<SpeechToTextService>(speechToTextService);
        }

        @Override
        public void handleMessage(Message msg) {
            Log.d("SpeechToTextService", "Inside handleMessage");

            final SpeechToTextService target = mtarget.get();
            super.handleMessage(msg);

            switch (msg.what){
                case MSG_RECOGNIZER_START_LISTENING:

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    {
                        // turn off beep sound
                        if (!mIsStreamSolo)
                        {
                            mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, true);
                            mIsStreamSolo = true;
                        }
                    }
                    if (!target.mIsListening)
                    {
                        target.mSpeechRecognizer.startListening(target.mSpeechRecognizerIntent);
                        target.mIsListening = true;
                        Log.d(TAG, "message start listening"); //$NON-NLS-1$
                    }
                    break;

                case MSG_RECOGNIZER_CANCEL:
                    if (mIsStreamSolo)
                    {
                        mAudioManager.setStreamSolo(AudioManager.STREAM_VOICE_CALL, false);
                        mIsStreamSolo = false;
                    }
                    target.mSpeechRecognizer.cancel();
                    target.mIsListening = false;
                    Log.d(TAG, "message canceled recognizer");
                    break;
            }
        }
    }

    protected CountDownTimer mNoSpeechCountDown = new CountDownTimer(5000,5000) {
        @Override
        public void onTick(long l) {

        }

        @Override
        public void onFinish() {
            mIsCountDownOn = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_CANCEL);
            try
            {
                mServerMessenger.send(message);
                message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {

            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (mIsCountDownOn)
        {
            mNoSpeechCountDown.cancel();
        }
        if (mSpeechRecognizer != null)
        {
            mSpeechRecognizer.destroy();
        }
    }

    private class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle bundle) {
            //Called when the endpointer is ready for the user to start speaking.


            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
            {
                mIsCountDownOn = true;
                mNoSpeechCountDown.start();

            }
            Log.d("SpeechRecognitionL", "onReadyForSpeech");
        }

        @Override
        public void onBeginningOfSpeech() {
            //The user has started to speak
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
        }

        @Override
        public void onRmsChanged(float v) {
            //The sound level in the audio stream has changed.
        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            //More sound has been received.


        }

        @Override
        public void onEndOfSpeech() {
            //Called after the user stops speaking.
        }

        @Override
        public void onError(int i) {
            //A network or recognition error occurred.
            if (mIsCountDownOn)
            {
                mIsCountDownOn = false;
                mNoSpeechCountDown.cancel();
            }
            mIsListening = false;
            Message message = Message.obtain(null, MSG_RECOGNIZER_START_LISTENING);
            try
            {
                mServerMessenger.send(message);
            }
            catch (RemoteException e)
            {

            }
        }

        @Override
        public void onResults(Bundle bundle) {
            //Called when recognition results are ready.
            Log.d("Log", "onResults");
            ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String text = "";
            for(String result : matches){
                text += result + "\n";
                Toast.makeText(getApplicationContext(),"Listening..."+result,Toast.LENGTH_LONG).show();
            }

        }

        @Override
        public void onPartialResults(Bundle bundle) {
            //Called when partial recognition results are available.
            Log.d("Log", "onPartialResults");
            ArrayList<String> matches = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String text = "";
            for(String result : matches){
                text += result + "\n";
                Toast.makeText(getApplicationContext(),"Listening..."+result,Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onEvent(int i, Bundle bundle) {

        }
    }
}
