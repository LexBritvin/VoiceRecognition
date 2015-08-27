package com.example.britvin.voicerecognition.Audio;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Created by initlab on 27.04.15.
 */
public class AudioProcessor {
    public static double durationInSeconds(short[] audioData, AudioFormatInfo format) {
        return (double) audioData.length / format.sampleRateInHz;
    }

    public static short[] centerAudio(short[] audioData, AudioFormatInfo format, int divisibility) {
        int start = 0;
        int stop = 0;
        for (start = 0; (Math.abs(audioData[start]) < 1500); start++) ;
        for (stop = audioData.length - 1; (Math.abs(audioData[stop]) < 500); stop--) ;
        int seconds = 1;
        int audioSampleDuration = format.sampleRateInHz * 3 / 8;
        int startSilence = 0;
        short newAudioData[] = new short[audioSampleDuration];
        for (int i = 0; i < newAudioData.length; i++) {
            newAudioData[i] = 0;
        }
        for (int i = 0; (i <= stop - start) && (startSilence + i < newAudioData.length); i++) {
            newAudioData[startSilence + i] = audioData[start + i];
        }
        return newAudioData;
    }

    public static double[] shortToDoubleArray(short[] shortData) {
        int size = shortData.length;
        double[] doubleData = new double[size];
        for (int i = 0; i < size; i++) {
            doubleData[i] = shortData[i];
        }
        return doubleData;
    }

    public static short[] doubleToShortArray(double[] doubleData) {
        int size = doubleData.length;
        short[] shortData = new short[size];
        for (int i = 0; i < size; i++) {
            shortData[i] = (short) Math.round(doubleData[i]);
        }
        return shortData;
    }

    public static short[] normalize(short[] audioData, int targetMax) {
        int max = audioData[0];
        for (int i = 0; i < audioData.length; i++)
            max = audioData[i] > max ? audioData[i] : max;

        // This is the maximum volume reduction
        double maxReduce = 1 - targetMax / (double) max;
        for (int i = 0; i < audioData.length; i++) {
            int abs = Math.abs(audioData[i]);
            double factor = (maxReduce * abs / (double) max);
            audioData[i] = (short) Math.round((1 - factor) * audioData[i]);
        }
        return audioData;
    }

    public static double standartDeviation(double audioData[]) {
        double average = 0;
        for (int i = 0; i < audioData.length; i++) {
            average += audioData[i];
        }
        average /= audioData.length;
        double result = 0;
        for (int i = 0; i < audioData.length; i++) {
            result += Math.pow(audioData[i] - average, 2);
        }
        result = Math.sqrt(result / audioData.length);

        return result;
    }

    public static double[] quantization(double audioData[]) {

        double stdDeviation = standartDeviation(audioData);
        double correct = 33;
        double noise = stdDeviation / correct * Math.sqrt(2 * Math.log(audioData.length));
        for (int i = 0; i < audioData.length; i++) {
            if (Math.abs(audioData[i]) >= noise) {
                audioData[i] = Math.signum(audioData[i]) * (Math.abs(audioData[i]) - noise);
            } else {
                audioData[i] = 0;
            }
        }
        return audioData;
    }

    public static double[] denoise(double audioData[]) {

        double hpf[] = getHPF(audioData);
        hpf = quantization(hpf);
        for (int i = 0; i < audioData.length / 2; i++) {
            audioData[2 * i + 1] = hpf[i];
        }
        return audioData;
    }

    public static double[] waveletProcess(short[] audioData) {
        double CL[] = D8_LowFilter();
        double CH[] = HPF(CL);
        double[] doubleData = normalizeDoubleArray(shortToDoubleArray(audioData), 10000);
        int level = 9;
        double[] lpf = evenArray(doubleData, level);
        for (int i = 0; i < level - 1; i++) {
            lpf = pconv(getLPF(lpf), CL, CH, 0);
        }

        return getLPF(lpf);
    }


    public static double[] getLPF(double audioData[]) {
        double LF[] = new double[audioData.length / 2];
        for (int i = 0; i < LF.length; i++)
            LF[i] = audioData[2 * i];
        return LF;
    }

    public static double[] getHPF(double audioData[]) {
        double HF[] = new double[audioData.length / 2];
        for (int i = 0; i < HF.length; i++)
            HF[i] = audioData[2 * i + 1];
        return HF;
    }

    public static double[] D4_LowFilter() {
        double LPF[] = {
                4.829629131445341433748715998644486838169524195042022752011715e-01,
                8.365163037378079055752937809168732034593703883484392934953414e-01,
                2.241438680420133810259727622404003554678835181842717613871683e-01,
                -1.294095225512603811744494188120241641745344506599652569070016e-01
        };
        return LPF;
    }

    public static double[] D8_LowFilter() {
        double LPF[] = {
                -0.0001174768,
                0.0006754494,
                -0.0003917404,
                -0.0048703530,
                0.0087460940,
                0.0139810279,
                -0.0440882539,
                -0.0173693010,
                0.1287474266,
                0.0004724846,
                -0.2840155430,
                -0.0158291053,
                0.5853546837,
                0.6756307363,
                0.3128715909,
                0.0544158422,
        };
        return LPF;
    }

    public static double[] D16_LowFilter() {
        double LPF[] = {
                -0.0000000211,
                0.0000002309,
                -0.0000007364,
                -0.0000010436,
                0.0000113366,
                -0.0000139457,
                -0.0000610360,
                0.0001747872,
                0.0001142415,
                -0.0009410217,
                0.0004078970,
                0.0031280234,
                -0.0036442796,
                -0.0069900146,
                0.0139937689,
                0.0102976596,
                -0.0368883977,
                -0.0075889744,
                0.0759242360,
                -0.0062397228,
                -0.1323883056,
                0.0273402638,
                0.2111906939,
                -0.0279182081,
                -0.3270633105,
                -0.0897510894,
                0.4402902569,
                0.6373563321,
                0.4303127228,
                0.1650642835,
                0.0349077143,
                0.0031892209,
        };
        return LPF;
    }

    public static double[] HPF(double[] LPF) {
        int n = LPF.length;
        double[] hpf = new double[n];
        int k = -1;
        for (int i = 0; i < n; i++) {
            hpf[i] = (k = -k) * LPF[n - i - 1];
        }
        return hpf;
    }

    public static double[] invLPF(double[] LPF, double[] HPF) {
        int n = LPF.length;
        double[] iCL = new double[n];

        for (int i = 0; i < n; i += 2) {
            int k = i - 2 < 0 ? n + i - 2 : i - 2;
            iCL[i] = LPF[k];
            iCL[i + 1] = HPF[k];
        }
        return iCL;
    }

    public static double[] invHPF(double[] LPF, double[] HPF) {
        int n = LPF.length;
        double[] iCH = new double[n];

        for (int i = 0; i < n; i += 2) {
            int k = i - 1 < 0 ? n + i - 1 : i - 1;
            iCH[i] = LPF[k];
            iCH[i + 1] = HPF[k];
        }
        return iCH;
    }

    public static double[] pconv(double[] data, double[] CL, double[] CH, int offset) {
        int n = CL.length;
        int m = data.length;
        int index;

        double[] result = new double[m];
        for (int k = 0; k < m; k += 2) {
            double sL = 0;
            double sH = 0;
            for (int i = 0; i < n; i++) {
                index = (k + i) % m;
                sL += data[index] * CL[i];
                sH += data[index] * CH[i];
            }
            index = k + offset > m - 1 ? k + offset - m : k + offset;
            result[index] = sL;
            result[index + 1] = sH;
        }
        return result;
    }

    public static byte[] shortArrayToByteArray(short[] shortArray) {
        byte[] byteArray = new byte[shortArray.length * 2];
        for (int i = 0; i < shortArray.length; i++) {
            byte[] byteNumber = shortToByteArray(shortArray[i]);
            byteArray[2 * i] = byteNumber[0];
            byteArray[2 * i + 1] = byteNumber[1];
        }
        return byteArray;
    }

    public static byte[] shortToByteArray(short number) {
        byte[] ret = new byte[2];
        ret[0] = (byte) (number & 0xFF);
        ret[1] = (byte) ((number >> 8) & 0xFF);
        return ret;
    }

    public static short byteArrayToShort(byte[] byteArray) {
        ByteBuffer bb = ByteBuffer.wrap(byteArray);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getShort();
    }

    public static int byteArrayToInt(byte[] byteArray) {
        ByteBuffer bb = ByteBuffer.wrap(byteArray);
        bb.order(ByteOrder.LITTLE_ENDIAN);
        return bb.getInt();
    }

    public static byte[] intToByteArray(int number) {
        byte[] ret = new byte[4];
        ret[0] = (byte) (number & 0xFF);
        ret[1] = (byte) ((number >> 8) & 0xFF);
        ret[2] = (byte) ((number >> 16) & 0xFF);
        ret[3] = (byte) ((number >> 24) & 0xFF);
        return ret;
    }

    public static short[] bytesToShortArray(byte[] byteArray) {
        short[] shortArray = new short[byteArray.length / 2];
        for (int i = 0; i < byteArray.length / 2; i++) {
            byte[] number = {byteArray[2 * i], byteArray[2 * i + 1]};
            shortArray[i] = byteArrayToShort(number);
        }
        return shortArray;
    }

    public static double[] normalizeDoubleArray(double[] arr, double max) {
        double[] result = new double[arr.length];
        for (int i = 0; i < arr.length; i++) {
            result[i] = arr[i] / max;
        }
        return result;
    }

    public static double[] evenArray(double[] arr, int level) {
        int ddd = (int) (Math.pow(2, level));
        int difference = arr.length / ddd + 1;
        double newArr[] = new double[ddd * difference];
        difference = newArr.length - arr.length;
        for (int i = 0; i < arr.length; i++)
            newArr[i] = arr[i];
        for (int i = 0; i < difference; i++)
            newArr[arr.length + i] = 0;
        return newArr;
    }
}
