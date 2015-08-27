package com.example.britvin.voicerecognition;

import android.content.Context;
import android.content.res.AssetManager;
import android.media.AudioFormat;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import com.example.britvin.voicerecognition.Audio.AudioFile;
import com.example.britvin.voicerecognition.Audio.AudioFormatInfo;
import com.example.britvin.voicerecognition.Audio.AudioGraph;
import com.example.britvin.voicerecognition.Audio.AudioPlayer;
import com.example.britvin.voicerecognition.Audio.AudioProcessor;
import com.example.britvin.voicerecognition.Audio.AudioReceiver;

import org.apache.commons.io.FilenameUtils;
import org.encog.ml.data.MLData;
import org.encog.ml.data.basic.BasicMLData;
import org.encog.neural.networks.BasicNetwork;
import org.encog.persist.EncogDirectoryPersistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;


public class MainActivity extends ActionBarActivity {

    public static BasicNetwork network;
    public static String[] words;
    short[] audioData = null;
    double[] audioDoubleData = null;
    boolean mStartRecording = true;
    boolean mStartPlaying = true;
    Button btnRecord;
    Button btnPlay;
    Button btnNormalize;
    Button btnAudioSave;
    Button btnAudioLoad;
    Button btnRecognize;
    Spinner spinnerWord;
    Handler handler;
    Handler graphHandler;
    TextView lblAudioSize;
    TextView lblAudioDuration;
    TextView lblWord;
    Context context;
    private AudioReceiver mRecorder = null;
    private AudioPlayer mPlayer = null;
    private AudioFormatInfo format;
    private AudioGraph audioGraph;
    private File APPLICATION_PUBLIC_FOLDER;

    public BasicNetwork loadNetwork() throws IOException {
        AssetManager am = this.getApplicationContext().getAssets();
        InputStream is = am.open("voicerecognition.eg");
        BasicNetwork network = (BasicNetwork) EncogDirectoryPersistence.loadObject(is);
        return network;
    }

    public static int getMaxIndex(double[] arr) {
        double max = arr[0];
        int max_i = 0;
        for (int j = 0; j < arr.length; j++) {
            if (arr[j] > max) {
                max_i = j;
                max = arr[j];
            }
        }
        return max_i;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
//        setupUI(findViewById(R.id.parent));
        APPLICATION_PUBLIC_FOLDER = new File(Environment.getExternalStorageDirectory(), "voicerecognition");
        if (!APPLICATION_PUBLIC_FOLDER.exists()) {
            APPLICATION_PUBLIC_FOLDER.mkdir();
        }
        context = getApplicationContext();

        audioGraph = (AudioGraph) findViewById(R.id.audioGraph);
        btnRecord = (Button) findViewById(R.id.btnRecord);
        btnPlay = (Button) findViewById(R.id.btnPlay);
        btnNormalize = (Button) findViewById(R.id.btnNormalize);
        btnAudioSave = (Button) findViewById(R.id.btnAudioSave);
        btnAudioLoad = (Button) findViewById(R.id.btnAudioLoad);
        btnRecognize = (Button) findViewById(R.id.btnRecognize);
        lblAudioSize = (TextView) findViewById(R.id.lblAudioSize);
        lblAudioDuration = (TextView) findViewById(R.id.lblAudioDuration);
        spinnerWord = (Spinner) findViewById(R.id.spinnerWords);
        lblWord = (TextView) findViewById(R.id.lblWord);

        String[] wordsInit = {"стоп", "вперёд", "назад", "вправо", "влево"};
        words = wordsInit;

        handler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                String text = (String) msg.obj;
                btnPlay.setText(text);
                mStartPlaying = !mStartPlaying;
            }
        };
        graphHandler = new Handler() {
            @Override
            public void handleMessage(Message msg) {
                audioData = msg.getData().getShortArray("audioData");
                btnPlay.setEnabled(true);
                updateActivityData();
            }
        };
        try {
            network = loadNetwork();
        } catch (IOException ex) { }
        View.OnClickListener oclBtn = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File file;
                String category;
                File categoryFolder;
                int count;
                switch (v.getId()) {
                    case R.id.btnRecord:
                        onRecord(mStartRecording);
                        mStartRecording = !mStartRecording;
                        break;

                    case R.id.btnPlay:
                        onPlay(mStartPlaying);
                        mStartPlaying = !mStartPlaying;
                        break;

                    case R.id.btnNormalize:
                        audioData = AudioProcessor.centerAudio(audioData, format, 2);
                        audioData = AudioProcessor.normalize(audioData, 8000);
//                        audioDoubleData = AudioProcessor.waveletProcess(audioData);
                        updateActivityData();
                        break;

                    case R.id.btnAudioLoad:
                        category = spinnerWord.getSelectedItem().toString();

                        categoryFolder = new File(APPLICATION_PUBLIC_FOLDER.getAbsolutePath() + "/" + category);
                        if (!categoryFolder.exists()) {
                            categoryFolder.mkdir();
                        }
                        count = getCategoryCount(categoryFolder);
                        if (count > 0) {
                            file = new File(categoryFolder, count + ".wav");
                            audioData = AudioFile.loadAudioFile(file);
                        }
                        updateActivityData();
                        break;

                    case R.id.btnAudioSave:
                        category = spinnerWord.getSelectedItem().toString();

                        categoryFolder = new File(APPLICATION_PUBLIC_FOLDER.getAbsolutePath() + "/" + category);
                        if (!categoryFolder.exists()) {
                            categoryFolder.mkdir();
                        }
                        count = getCategoryCount(categoryFolder);
                        count++;
                        file = new File(categoryFolder, count + ".wav");
                        saveAudioFile(file);
                        break;

                    case R.id.btnRecognize:
                        testNetwork();
                        break;
                }
            }
        };
        btnPlay.setOnClickListener(oclBtn);
        btnRecord.setOnClickListener(oclBtn);
        btnNormalize.setOnClickListener(oclBtn);
        btnAudioLoad.setOnClickListener(oclBtn);
        btnAudioSave.setOnClickListener(oclBtn);
        btnRecognize.setOnClickListener(oclBtn);
    }

    private void onRecord(boolean start) {
        if (start) {
            this.format = new AudioFormatInfo(22050, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
            mRecorder = new AudioReceiver(format, graphHandler);
            mRecorder.start();
            btnRecord.setText("Stop");
            btnPlay.setEnabled(false);
        } else {
            mRecorder.stop();
            btnRecord.setText("Record");
            btnPlay.setEnabled(true);
            mRecorder = null;
        }
    }

    private void onPlay(boolean start) {
        if (start) {
            btnPlay.setText("Stop");
            mPlayer = new AudioPlayer(audioData, format, handler, graphHandler);
            mPlayer.start();
        } else {
            mPlayer.stop();
            mPlayer = null;
            btnPlay.setText("Play");
        }
    }

    private void updateActivityData() {
        this.format = new AudioFormatInfo(22050, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        audioGraph.updateGraph(audioData);
        lblAudioSize.setText(Integer.toString(audioData.length));
        DecimalFormat df = new DecimalFormat("#.00");
        lblAudioDuration.setText(df.format(AudioProcessor.durationInSeconds(audioData, format)));
        btnPlay.setEnabled(true);
    }

    public ArrayList<File> getAudioFilesList(File directoryName) {
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = directoryName.listFiles();
        for (File file : files) {
            if (!file.isDirectory()) {
                if (file.getName().endsWith(".wav")) {
                    inFiles.add(file);
                }
            }
        }
        return inFiles;
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public int getCategoryCount(File categoryFolder) {
        ArrayList<File> audioFilesList = getAudioFilesList(categoryFolder);
        int count = 0;
        for (File file : audioFilesList) {
            String filename = FilenameUtils.getBaseName(file.getName());
            try {
                int filenameCount = Integer.parseInt(filename);
                count = filenameCount > count ? filenameCount : count;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return count;
    }

    public void saveAudioFile(File file) {
        if (audioData != null) {
            audioData = AudioProcessor.centerAudio(audioData, format, 2);
            audioData = AudioProcessor.normalize(audioData, 8000);
            AudioFile.saveAudioFile(file, audioData, format);
        }
    }

    public void testNetwork() {
        // test the neural network.
        double[] audioDataDouble = AudioProcessor.waveletProcess(AudioProcessor.normalize(audioData, 8000));
        MLData mlData = new BasicMLData(audioDataDouble);
        final MLData output = network.compute(mlData);
        int max_i = getMaxIndex(output.getData());
        lblWord.setText(words[max_i]);
    }
}
