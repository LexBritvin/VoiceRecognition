package com.example.britvin.voicerecognition.Audio;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;

import java.util.ArrayList;

public class AudioReceiver implements Runnable {
    private final int BUFF_COUNT = 32;
    Thread thread;
    Handler graphHandler;
    private boolean mIsRunning;
    private AudioFormatInfo format;
    private AudioRecord mRecord;
    private ArrayList<Integer> data;

    public AudioReceiver(AudioFormatInfo format, Handler graphHandler) {
        this.format = format;
        mIsRunning = true;
        mRecord = null;
        data = new ArrayList<Integer>();
        this.graphHandler = graphHandler;
    }

    public short[] getAudioData() {
        short audioData[] = new short[this.data.size()];
        for (int i = 0; i < this.data.size(); i++) {
            int value = data.get(i);
            audioData[i] = (short) value;
        }
        return audioData;
    }

    public void start() {
        thread = new Thread(this);
        thread.setName("Capture");
        thread.start();
    }

    public void stop() {
        mIsRunning = false;
        thread = null;
    }

    @Override
    public void run() {
        // приоритет для потока обработки аудио
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        mIsRunning = true;

        int buffSize = AudioRecord.getMinBufferSize(format.getSampleRateInHz(),
                format.getChannelConfig(), format.getAudioFormat());

        if (buffSize == AudioRecord.ERROR) {
            System.err.println("getMinBufferSize returned ERROR");
            return;
        }

        if (buffSize == AudioRecord.ERROR_BAD_VALUE) {
            System.err.println("getMinBufferSize returned ERROR_BAD_VALUE");
            return;
        }

        // здесь работаем с short, поэтому требуем 16-bit
        if (format.getAudioFormat() != AudioFormat.ENCODING_PCM_16BIT) {
            System.err.println("unknown format");
            return;
        }

        // циклический буфер буферов. Чтобы не затереть данные,
        // пока главный поток их обрабатывает
        short[] buffer = new short[buffSize];

        mRecord = new AudioRecord(MediaRecorder.AudioSource.MIC,
                format.getSampleRateInHz(),
                format.getChannelConfig(), format.getAudioFormat(),
                buffSize * 10);

        if (mRecord.getState() != AudioRecord.STATE_INITIALIZED) {
            System.err.println("getState() != STATE_INITIALIZED");
            return;
        }

        try {
            mRecord.startRecording();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            return;
        }

        int count = 0;

        while (thread != null) {
            int samplesRead = mRecord.read(buffer, 0, buffSize);

            if (samplesRead == AudioRecord.ERROR_INVALID_OPERATION) {
                System.err.println("read() returned ERROR_INVALID_OPERATION");
                return;
            }

            if (samplesRead == AudioRecord.ERROR_BAD_VALUE) {
                System.err.println("read() returned ERROR_BAD_VALUE");
                return;
            }
            for (int i = 0; i < buffSize; i++) {
                int value = (int) buffer[i];
                data.add(value);
            }
            // посылаем оповещение обработчикам
            // TO DO
        }
        Message msg = new Message();
        Bundle bundle = new Bundle();
        short[] temp = getAudioData();
        bundle.putShortArray("audioData", getAudioData());
        msg.setData(bundle);
        graphHandler.sendMessage(msg);
        try {
            try {
                mRecord.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
                return;
            }
        } finally {
            // освобождаем ресурсы
            mRecord.release();
            mRecord = null;
        }

    }

}