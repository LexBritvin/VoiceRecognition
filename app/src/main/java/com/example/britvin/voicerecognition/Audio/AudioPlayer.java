package com.example.britvin.voicerecognition.Audio;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Process;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;

public class AudioPlayer implements Runnable {
    Thread thread;
    Handler handler;
    Handler graphHandler;
    private short[] audioData;
    private AudioFormatInfo format;
    private AudioTrack audioTrack;

    private long length;
    private boolean isRunning;
    private String serverIP;

    public AudioPlayer(short[] audioData, AudioFormatInfo format,
                       String serverIP, Handler handler, Handler graphHandler) {
        this.audioData = audioData;
        this.format = format;
        this.isRunning = true;
        this.handler = handler;
        this.serverIP = serverIP;
        this.graphHandler = graphHandler;
    }

    public AudioPlayer(short[] audioData, AudioFormatInfo format, Handler handler, Handler graphHandler) {
        this.audioData = audioData;
        this.format = format;
        this.isRunning = true;
        this.handler = handler;
        this.graphHandler = graphHandler;
    }

    public void HttpSend() {
        try {
            URI address = new URI("http://" + serverIP + "/");
            HttpClient client = new DefaultHttpClient();
            HttpPost post = new HttpPost(address);
            byte[] audioDataByteArray = shortToByteArray(audioData);
            post.setEntity(new ByteArrayEntity(audioDataByteArray));
            try {
                HttpResponse response = client.execute(post);

                InputStream contentStream = response.getEntity().getContent();
                long contentLength = response.getEntity().getContentLength();
                audioDataByteArray = new byte[(int) contentLength];
                for (int i = 0; i < contentLength; i++) {
                    audioDataByteArray[i] = (byte) contentStream.read();
                }
                short[] handledAudioData = ByteToShortArray(audioDataByteArray);
                audioData = handledAudioData;
                length = audioData.length;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (URISyntaxException eURI) {
            eURI.printStackTrace();

        }
    }

    private byte[] shortToByteArray(short[] shortArray) {
        byte[] byteArray = new byte[shortArray.length * 2];
        for (int i = 0; i < shortArray.length; i++) {
            byteArray[2 * i] = (byte) (shortArray[i] >> 8);
            byteArray[2 * i + 1] = (byte) shortArray[i];
        }
        return byteArray;
    }

    private short[] ByteToShortArray(byte[] byteArray) {
        short[] shortArray = new short[byteArray.length / 2];
        for (int i = 0; i < byteArray.length / 2; i++) {
            int MSB = (int) byteArray[2 * i];
            int LSB = (int) byteArray[2 * i + 1];
            shortArray[i] = (short) (MSB << 8 | (255 & LSB));
        }
        return shortArray;
    }

    private void updateGraph() {
        Message msg = new Message();
        Bundle bundle = new Bundle();
        bundle.putShortArray("audioData", audioData);
        msg.setData(bundle);
    }

    public void start() {
        thread = new Thread(this);
        thread.setName("Playing");
        thread.start();
    }

    public void run() {
        Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO);
        int buffsize = AudioTrack.getMinBufferSize(format.getSampleRateInHz(),
                AudioFormat.CHANNEL_OUT_MONO, format.getAudioFormat());
        AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                format.getSampleRateInHz(),
                AudioFormat.CHANNEL_OUT_MONO,
                format.getAudioFormat(),
                buffsize,
                AudioTrack.MODE_STREAM);
        short samples[] = new short[buffsize];
        audioTrack.play();
        int offset = 0;
        while (isRunning && offset < audioData.length) {
            for (int i = 0; i < buffsize; i++) {
                if (offset + i >= audioData.length) {
                    samples[i] = 0;
                } else {
                    samples[i] = audioData[offset + i];
                }
            }
            offset += buffsize;
            audioTrack.write(samples, 0, buffsize);
        }

        if (isRunning) {
            Message msg = new Message();
            msg.obj = "Play";
            handler.sendMessage(msg);
        }

        audioTrack.stop();
        audioTrack.release();
    }

    public void stop() {
        isRunning = false;
    }

}
