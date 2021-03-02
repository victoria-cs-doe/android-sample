package com.deezer.sdk.sample;

import android.annotation.TargetApi;
import android.media.audiofx.Visualizer;
import android.media.audiofx.Visualizer.OnDataCaptureListener;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import com.deezer.sdk.model.Track;
import com.deezer.sdk.network.connect.SessionStore;
import com.deezer.sdk.network.request.event.DeezerError;
import com.deezer.sdk.network.request.event.OAuthException;
import com.deezer.sdk.player.RadioPlayer;
import com.deezer.sdk.player.RadioPlayer.RadioType;
import com.deezer.sdk.player.event.RadioPlayerListener;
import com.deezer.sdk.player.exception.TooManyPlayersExceptions;
import com.deezer.sdk.player.networkcheck.WifiAndMobileNetworkStateChecker;

import java.util.Arrays;

@TargetApi(Build.VERSION_CODES.GINGERBREAD)
public class VisualizerActivity extends PlayerActivity implements RadioPlayerListener, OnDataCaptureListener {

    private Visualizer mVisualizer;

    private static final long RADIO_SOUNDTRACKS = 30701L;
    private RadioPlayer mRadioPlayer;

    public static final String EXTRA_DISPLAY = "display";
    private int mDisplay = 0;
    public static final int DISPLAY_WAVEFORM = 1;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // restore existing deezer Connection
        new SessionStore().restore(mDeezerConnect, this);

        mDisplay = getIntent().getIntExtra(EXTRA_DISPLAY, DISPLAY_WAVEFORM);

        // setup UI
        setContentView(R.layout.activity_visualizer);
        setupPlayerUI();
        //build the player
        createPlayer();
    }

    @Override
    protected void onResume() {
        super.onResume();

        mRadioPlayer.playRadio(RadioType.RADIO, RADIO_SOUNDTRACKS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mRadioPlayer.stop();
        stopVisualizer();
    }

    /**
     * Sets up the player UI (mostly remove unnecessary buttons)
     */
    private void setupPlayerUI() {
        // for now hide the player
        setPlayerVisible(true);

        // disable unnecesary buttons
        setButtonEnabled(mButtonPlayerSeekBackward, false);
        setButtonEnabled(mButtonPlayerSeekForward, false);
        setButtonEnabled(mButtonPlayerSkipBackward, false);
        setButtonEnabled(mButtonPlayerSkipForward, false);
        setButtonEnabled(mButtonPlayerStop, false);
        setButtonEnabled(mButtonPlayerPause, false);

        setButtonEnabled(mButtonPlayerRepeat, false);
    }

    private void stopVisualizer() {
        if (mVisualizer != null) {
            mVisualizer.setEnabled(false);
            mVisualizer.setDataCaptureListener(null, 0, false, false);
        }
    }

    /**
     * Creates the Radio Player
     */
    private void createPlayer() {
        try {
            mRadioPlayer = new RadioPlayer(getApplication(), mDeezerConnect,
                    new WifiAndMobileNetworkStateChecker());
            mRadioPlayer.addPlayerListener(this);
            setAttachedPlayer(mRadioPlayer);
        } catch (OAuthException e) {
            handleError(e);
        } catch (DeezerError e) {
            handleError(e);
        } catch (TooManyPlayersExceptions e) {
            handleError(e);
        }
    }


    //////////////////////////////////////////////////////////////////////////////////////
    // Radio Player Callbacks
    //////////////////////////////////////////////////////////////////////////////////////


    @Override
    public void onPlayTrack(final Track track) {
        displayTrack(track);

        stopVisualizer();

        // update the visualizer
        int sessionId = mRadioPlayer.getAudioSessionId();
        Log.i("Visualizer", "Session Id : " + sessionId);

        int[] captureSizeRange = Visualizer.getCaptureSizeRange();
        Log.i("Visualizer", "Capture size range : " + Arrays.toString(captureSizeRange));

        int maxRate = Visualizer.getMaxCaptureRate();
        Log.i("Visualizer", "Max Capture rate : " + maxRate);

        mVisualizer = new Visualizer(sessionId);

        int size = (captureSizeRange[0] + captureSizeRange[1]) / 2;
        mVisualizer.setCaptureSize(size);
        mVisualizer.setDataCaptureListener(this, maxRate, true, true);
        mVisualizer.setEnabled(true);
    }

    @Override
    public void onTrackEnded(final Track track) {
        stopVisualizer();
    }

    @Override
    public void onAllTracksEnded() {
        stopVisualizer();
    }


    @Override
    public void onRequestException(final Exception e, final Object requestId) {
        handleError(e);
        stopVisualizer();
    }

    @Override
    public void onTooManySkipsException() {
        Toast.makeText(this, R.string.deezer_too_many_skips,
                Toast.LENGTH_LONG).show();
    }


    @Override
    public void onWaveFormDataCapture(final Visualizer visualizer, final byte[] waveform, final int samplingRate) {
        // Log.i("Visualizer", "Got wf : " + Arrays.toString(waveform));

    }

    @Override
    public void onFftDataCapture(Visualizer visualizer, byte[] bytes, int i) {

    }
}
