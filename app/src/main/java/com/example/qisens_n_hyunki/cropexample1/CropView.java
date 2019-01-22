/*
 * FreeHand Crop Example
 *  -> 출처: http://androidgeekcoder.blogspot.com/2016/07/android-freehand-image-crop_7.html
 *
 * <반응속도 향상을 위해 SurfaceView, Thread 사용>
 * SurfaceView는 canvas가 아닌 Surface를 가짐 (가상메모리 화면)
 * 메인 스레드가 surface의 변화를 감지해 쓰레드에게 그리기 허용 여부를 알려줘야함 -> SurfaceHolder.callback
 *  -> 출처: https://aroundck.tistory.com/202
 *
 */

package com.example.qisens_n_hyunki.cropexample1;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.List;


public class CropView extends SurfaceView implements SurfaceHolder.Callback {
    private Paint paint;
    public static List<Point> points;
    boolean flgPathDraw = true;

    Point mfirstpoint = null;
    boolean bfirstpoint = false;  // first point가 있으면 true, 없으면 false

    Point mlastpoint = null;

    Bitmap bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.test3);
    Context mContext;

    String TAG = "CropView";

    // canvas size (ImageCropActivity에 intent로 넘겨줄꺼)
    int canvasWidth;
    int canvasHeight;
    // bitmap size
    int bitmapWidth;
    int bitmapHeight;
    float rate;   // (bitmap / canvas)
    // canvas 크기에 맞게 줄인 bitmap image size
    int newWidth;
    int newHeight;
    // bitmap image 가운데 정렬을 위한 x, y값
    int canvasLeft;
    int canvasTop;
    // portrait = true / landscape = false
    public static boolean configOrientation = true;

    // 지정한 범위의 x, y의 min, max값
    public static float minx=0, miny=0, maxx=0, maxy=0;

    // 현재 좌표에서 바로 전의 좌표를 저장하기 위한 변수
    public static List<Point> previousPoint;

    Path path = new Path();

    // 쓰레드 변수
    SurfaceHolder mHolder;
    DrawThread mThread;
    final static int DELAY = 50;

    public CropView(Context c) {
        super(c);
        mContext = c;

        setFocusable(true);
        setFocusableInTouchMode(true);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);    // line의 계단 현상을 없애주어 부드럽게 해줌
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeCap(Paint.Cap.ROUND);         // line의 끝 부분을 둥글게 처리
        paint.setStrokeJoin(Paint.Join.ROUND);       // line이 만나는 부분을 둥글게 처리
        paint.setStrokeWidth(80);                   // 선 굵기
        paint.setARGB(90,255,255,255);   // 흰색선인데 약간 투명하게

//        this.setOnTouchListaener(this);
        points = new ArrayList<Point>();

        bfirstpoint = false;

        // ---------------------------------------------------------------------------------------------
        mHolder = getHolder();
        mHolder.addCallback(this);   // 시스템이 표면의 변화가 발생할 때마다 콜백 메서드 호출
        // ---------------------------------------------------------------------------------------------

        bitmapWidth = bitmap.getWidth();
        bitmapHeight = bitmap.getHeight();
        Log.d(TAG, String.valueOf(bitmapWidth) +" / " + String.valueOf(bitmapHeight));
    }


    // 원래 onTouch() 함수에 있던 내용
    public boolean onTouchEvent(MotionEvent event) {
        Point point = new Point();
        point.x = (int) event.getX();
        point.y = (int) event.getY();

        if (flgPathDraw) {

            if (bfirstpoint) {

                if (comparePoint(mfirstpoint, point)) {
                    points.add(mfirstpoint);
                    flgPathDraw = false;
                    showCropDialog();
                } else {
                    points.add(point);
                }
            } else {
                points.add(point);
            }

            if (!(bfirstpoint)) {

                mfirstpoint = point;
                bfirstpoint = true;

                // min, max값 초기화 추가-------------------------------------------------------------
                minx = mfirstpoint.x;
                miny = mfirstpoint.y;
                maxx = mfirstpoint.x;
                maxy = mfirstpoint.y;
                // ---------------------------------------------------------------------------------
            }
        }

        invalidate();   // onDraw()가 호출되면서 이미지를 계속해서 새로 그림

        if (event.getAction() == MotionEvent.ACTION_UP) {  // ACTION_UP: 누른걸 땠을때

            mlastpoint = point;
            if (flgPathDraw) {
                if (points.size() > 2) {   // 걍 떼면 거의 dialog 뜸
                    if (!comparePoint(mfirstpoint, mlastpoint)) {     // false
                        flgPathDraw = false;
                        points.add(mfirstpoint);
                        showCropDialog();
                    }
                }
            }
        }

        return true;
    }


    // Thread Class
    class DrawThread extends Thread {

        boolean bExit;
        int mWidth, mHeight;
        SurfaceHolder mHolder;

        DrawThread(SurfaceHolder Holder){
            mHolder = Holder;
            bExit = false;
        }

        public void SizeChange(int Width, int Height){
            mWidth = Width;
            mHeight= Height;
        }

        public void run(){
            Canvas canvas;

            while (bExit == false){

                synchronized(mHolder){

                    canvas = mHolder.lockCanvas();
                    if (canvas == null) break;

                    // 여기에 onDraw() 내용이 들어가야함
                    // 비트맵 이미지를 canvas 크기에 맞게 축소 --------------------------------------------
                    canvasWidth = canvas.getWidth();
                    canvasHeight = canvas.getHeight();

                    if (configOrientation == true) {
                        rate = (float) canvasWidth / (float) bitmapWidth;

                        newWidth = (int) (bitmapWidth * rate);
                        newHeight = (int) (bitmapHeight * rate);

                        canvasTop = (canvasHeight - newHeight) / 2;

                        Bitmap resizeBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                        canvas.drawBitmap(resizeBitmap, 0, canvasTop, null);
                    }
                    else {
                        rate = (float) canvasHeight / (float) bitmapHeight;

                        newWidth = (int) (bitmapWidth * rate);
                        newHeight = (int) (bitmapHeight * rate);

                        canvasLeft = (canvasWidth - newWidth) / 2;

                        Bitmap resizeBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
                        canvas.drawBitmap(resizeBitmap, canvasLeft, 0, null);
                    }
                    //------------------------------------------------------------------------------------------

                    boolean first = true;
                    for (int i = 0; i < points.size(); i += 1) {
                        Point point = points.get(i);
                        if (first) {
                            first = false;
                            path.moveTo(point.x, point.y);   // 기준점을 x, y로 이동시킴
                        } else if (i < points.size() - 1) {
                            Point next = points.get(i + 1);
                            path.quadTo(point.x, point.y, next.x, next.y);   // 점 x1, y1에서 x2, y2로 곡선을 그림
                        } else {
                            mlastpoint = points.get(i);
                            path.lineTo(point.x, point.y);   // path의 마지막에 경로 추가
                        }
                    }
                    canvas.drawPath(path, paint);


                    mHolder.unlockCanvasAndPost(canvas);
                }

                try {Thread.sleep(CropView.DELAY);} catch(Exception e){}
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration config) {
        super.onConfigurationChanged(config);

        if (config.orientation == Configuration.ORIENTATION_PORTRAIT) {
            configOrientation = true;
        } else if (config.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            configOrientation = false;
        }
    }

    // 표면이 처음 생성된 직후 호출. 이때부터 표면에 그리기 허용
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mThread = new DrawThread(mHolder);
        mThread.start();
    }

    // 표면의 색상이나 표면이 변경된 경우 호출. 여기서 표면의 크기 초기화
    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if(mThread != null) {
            mThread.SizeChange(width, height);
        }
    }

    // 표면이 파괴되기 직전에 호출. 이게 리턴
    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        mThread.bExit = true;
        for(;;) {
            try {
                mThread.join();
                break;
            } catch (Exception e) {

            }
        }
    }


    private boolean comparePoint(Point first, Point current) {
        int left_range_x = (int) (current.x - 3);
        int left_range_y = (int) (current.y - 3);

        int right_range_x = (int) (current.x + 3);
        int right_range_y = (int) (current.y + 3);

        // 현재 point값이랑 min, max값 비교하는거 추가 -------------------------------------------------
        if (current.x < minx) minx = current.x;
        if (current.y < miny) miny = current.y;
        if (current.x > maxx) maxx = current.x;
        if (current.y > maxy) maxy = current.y;
        // -----------------------------------------------------------------------------------------

        if ((left_range_x < first.x && first.x < right_range_x) && (left_range_y < first.y && first.y < right_range_y)) {  // first point하고 current point 차이가 작으면
            if (points.size() < 10) {  // 화면에 그려진게 별로 없으면(point가 10개 미만이면) crop 하지마셈
                return false;
            } else {                   // 그려진게 많으면(point가 10개 이상이면) crop 하셈 (여기 else문에 안들어가짐)
                return true;
            }
        } else {
            return false;
        }

    }

    private void showCropDialog() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override            public void onClick(DialogInterface dialog, int which) {
                Intent intent;
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:

                        intent = new Intent(mContext, ImageCropActivity.class);
                        intent.putExtra("crop", true);
                        intent.putExtra("width", newWidth);   // 추가
                        intent.putExtra("height", newHeight); // 추가
                        intent.putExtra("canvasWidth", canvasWidth);
                        intent.putExtra("canvasHeight", canvasHeight);
                        intent.putExtra("canvasLeft", canvasLeft);
                        intent.putExtra("canvasTop", canvasTop);
                        mContext.startActivity(intent);
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:

                        intent = new Intent(mContext, ImageCropActivity.class);
                        intent.putExtra("crop", false);
                        intent.putExtra("width", newWidth);   // 추가
                        intent.putExtra("height", newHeight); // 추가
                        mContext.startActivity(intent);

                        bfirstpoint = false;

                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        builder.setMessage("Do you Want to save Crop or Non-crop image?")
                .setPositiveButton("Crop", dialogClickListener)
                .setNegativeButton("취소", dialogClickListener).show()
                .setCancelable(false);
    }
}