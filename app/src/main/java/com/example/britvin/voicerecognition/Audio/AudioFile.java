package com.example.britvin.voicerecognition.Audio;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import static com.example.britvin.voicerecognition.Audio.AudioProcessor.byteArrayToInt;
import static com.example.britvin.voicerecognition.Audio.AudioProcessor.byteArrayToShort;
import static com.example.britvin.voicerecognition.Audio.AudioProcessor.intToByteArray;
import static com.example.britvin.voicerecognition.Audio.AudioProcessor.shortArrayToByteArray;
import static com.example.britvin.voicerecognition.Audio.AudioProcessor.shortToByteArray;

/**
 * Created by initlab on 28.04.15.
 */
public class AudioFile {
    public static void saveAudioFile(File file, short[] audioData, AudioFormatInfo format) {
        byte[] data = shortArrayToByteArray(audioData);
        if (file.exists()) {
            file.delete();
        }
        try {
            FileOutputStream outputStream = new FileOutputStream(file);

            int mySubChunk1Size = 16; // 16 для формата PCM. Это оставшийся размер подцепочки, начиная с этой позиции.
            short myBitsPerSample = 16;
            short myFormat = 1; // 1 для PCM
            short myChannels = 1;
            int mySampleRate = format.getSampleRateInHz();
            int myByteRate = mySampleRate * myChannels * myBitsPerSample / 8;
            short myBlockAlign = (short) (myChannels * myBitsPerSample / 8);

            int myDataSize = data.length;
            int myChunk2Size = myDataSize * myChannels * myBitsPerSample / 8;
            int myChunkSize = 36 + myChunk2Size;

            BufferedOutputStream bos = new BufferedOutputStream(outputStream);
            DataOutputStream outFile = new DataOutputStream(bos);
            // Записываем header WAV файла
            outFile.writeBytes("RIFF");                                 // 00 - RIFF
            outFile.write(intToByteArray(myChunkSize), 0, 4);      // 04 - how big is the rest of this file?
            outFile.writeBytes("WAVE");                                 // 08 - WAVE
            outFile.writeBytes("fmt ");                                 // 12 - fmt
            outFile.write(intToByteArray(mySubChunk1Size), 0, 4);  // 16 - size of this chunk
            outFile.write(shortToByteArray(myFormat), 0, 2);     // 20 - what is the audio format? 1 for PCM = Pulse Code Modulation
            outFile.write(shortToByteArray(myChannels), 0, 2);   // 22 - mono or stereo? 1 or 2?  (or 5 or ???)
            outFile.write(intToByteArray(mySampleRate), 0, 4);     // 24 - samples per second (numbers per second)
            outFile.write(intToByteArray(myByteRate), 0, 4);       // 28 - bytes per second
            outFile.write(shortToByteArray(myBlockAlign), 0, 2); // 32 - # of bytes in one sample, for all channels
            outFile.write(shortToByteArray(myBitsPerSample), 0, 2);  // 34 - how many bits in a sample(number)?  usually 16 or 24
            outFile.writeBytes("data");                                 // 36 - data
            outFile.write(intToByteArray(myDataSize), 0, 4);       // 40 - how big is this data chunk

            outFile.write(data);                                    // 44 - the actual data itself - just a long string of numbers

            outFile.flush();
            outFile.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static short[] loadAudioFile(File file) {
        short[] audioData = null;
        int size = (int) file.length();
        byte[] header = new byte[44];
        byte[] audioBytes = new byte[size - 44];
        AudioFormatInfo format;
        try {
            FileInputStream inputStream = new FileInputStream(file);
            BufferedInputStream buf = new BufferedInputStream(inputStream);
            buf.read(header, 0, 44);
            buf.read(audioBytes);
            buf.close();
            format = readWaveHeader(header);
            audioData = AudioProcessor.bytesToShortArray(audioBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return audioData;
    }

    public static AudioFormatInfo readWaveHeader(byte[] header) {
        byte[] channelBytes = {header[22], header[23]};
        int channels = byteArrayToShort(channelBytes);

        byte[] sampleRateBytes = {header[24], header[25], header[26], header[27]};
        int sampleRate = byteArrayToInt(sampleRateBytes);

        byte[] bitsPerSampleBytes = {header[35], header[36]};
        int bitsPerSample = byteArrayToShort(bitsPerSampleBytes);
        return new AudioFormatInfo(sampleRate, channels, bitsPerSample);
    }
}
