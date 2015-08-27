package com.example.britvin.voicerecognition.Audio;

/**
 * Created by Александр on 25.06.2014.
 */
public class AudioFormatInfo {

    public int sampleRateInHz;
    public int channelConfig;
    public int audioFormat;

    public AudioFormatInfo(int sampleRateInHz, int channelConfig, int audioFormat) {
        this.sampleRateInHz = sampleRateInHz;
        this.channelConfig = channelConfig;
        this.audioFormat = audioFormat;
    }

    public int getSampleRateInHz() {
        return sampleRateInHz;
    }

    public int getChannelConfig() {
        return channelConfig;
    }

    public int getAudioFormat() {
        return audioFormat;
    }

}
