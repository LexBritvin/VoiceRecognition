package com.example.britvin.voicerecognition.Audio;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;


public class AudioGraph extends SurfaceView implements SurfaceHolder.Callback {

    private DrawThread drawThread;
    private ReDrawThread reDrawThread;

    public AudioGraph(Context context) {
        super(context);
        getHolder().addCallback(this);
    }
    public AudioGraph(Context context, AttributeSet attrs) {
        super(context, attrs);
        getHolder().addCallback(this);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TO DO добавить инициализацию аудио данных
        drawThread = new DrawThread(getHolder());
        drawThread.setRunning(true);
        drawThread.start();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        boolean retry = true;
        // завершаем работу потока
        drawThread.setRunning(false);
        while (retry) {
            try {
                drawThread.join();
                retry = false;
            } catch (InterruptedException e) {
                // если не получилось, то будем пытаться еще и еще
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        final int height = getMeasuredHeight();    // высота
        final int width = getMeasuredWidth();    // ширина

        setMeasuredDimension(width, height / 3);
    }

    public void updateGraph(short[] audioData) {
        reDrawThread = new ReDrawThread(getHolder(), audioData);
        reDrawThread.start();
    }

    class DrawThread extends Thread {
        private boolean runFlag = false;
        private SurfaceHolder surfaceHolder;

        public DrawThread(SurfaceHolder surfaceHolder) {
            this.surfaceHolder = surfaceHolder;

        }

        public void setRunning(boolean run) {
            runFlag = run;
        }

        @Override
        public void run() {
            Canvas canvas;
            canvas = null;

            try {
                // получаем объект Canvas и выполняем отрисовку
                canvas = surfaceHolder.lockCanvas(null);
                int height = canvas.getHeight();
                int width = canvas.getWidth();
                synchronized (surfaceHolder) {
                    canvas.drawColor(Color.WHITE);
                    Paint paint = new Paint(Color.BLACK);

                    canvas.drawLine(0, height / 2, width, height / 2, paint);

                }
            } finally {
                if (canvas != null) {
                    // отрисовка выполнена. выводим результат на экран
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }

        }
    }

    class ReDrawThread extends Thread {
        short[] audioData;
        private SurfaceHolder surfaceHolder;

        public ReDrawThread(SurfaceHolder surfaceHolder, short[] audioData) {
            this.surfaceHolder = surfaceHolder;
            this.audioData = audioData;
        }

        @Override
        public void run() {
            Canvas canvas;
            canvas = null;
            try {
                // получаем объект Canvas и выполняем отрисовку
                canvas = surfaceHolder.lockCanvas(null);
                int height = canvas.getHeight();
                int width = canvas.getWidth();

                int pointsPerPixel = audioData.length / width;
                float[] audioDataGraph = new float[2 * width];
                Path _path = new Path();
                _path.moveTo(0, height / 2);
                for (int i = 0; i < width; i++) {
                    float x = i;
                    float y = 0;
                    int step = pointsPerPixel / 8;
                    for (int j = 0; j < pointsPerPixel; j += step) {
                        y = ((float) audioData[i * pointsPerPixel + j] / 32768) * height + height / 2;
                        _path.lineTo(x, y);
                    }
                }
                synchronized (surfaceHolder) {
                    canvas.drawColor(Color.WHITE);
                    Paint paint = new Paint(Color.BLACK);
                    canvas.drawLine(0, height / 2, width, height / 2, paint);
                    canvas.drawPath(_path, paint);
                }
            } finally {
                if (canvas != null) {
                    // отрисовка выполнена. выводим результат на экран
                    surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }

    }
}



