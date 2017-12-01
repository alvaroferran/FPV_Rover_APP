package com.alvaroferran.fpv;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.TextView;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;

public class MainActivity extends Activity{

    String IP="192.168.42.1";
    int PORT= 3005;
    Socket mysocket;
    PrintWriter out;

    private WebView webView1;
    private String url="http://192.168.42.1:8080/stream_full.html";

    private SensorManager mSensorManager;
    private Sensor mAccelerometer;
    private Sensor mMagnetometer;
    private final float[] mAccelerometerReading = new float[3];
    private final float[] mMagnetometerReading = new float[3];
    private final float[] mRotationMatrix = new float[9];
    private final float[] mOrientationAngles = new float[3];

//    public TextView text;
    Thread clientThread = new Thread(new ClientThread());
    Thread sensorsThread = new Thread(new UpdateThread());
    private double angleAxes[] = new double[2];

    /********ON CREATE**************************************************************************************/

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        clientThread.start();
        sensorsThread.start();

        webView1 = (WebView) findViewById(R.id.webView1);
        webView1.getSettings().setJavaScriptEnabled(true);
        webView1.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        webView1.setWebViewClient(new WebViewer());

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMagnetometer = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

//        text = (TextView) findViewById(R.id.textView1);

    }



    /********ON RESUME*************************************************************************/

    @Override
    public void onResume() {
        super.onResume();
        webView1.loadUrl(url);

        mSensorManager.registerListener(_SensorEventListener , mAccelerometer,
                SensorManager.SENSOR_DELAY_UI);
        mSensorManager.registerListener(_SensorEventListener , mMagnetometer,
                SensorManager.SENSOR_DELAY_UI);
    }



    /********ON PAUSE**************************************************************************/

    @Override
    protected void onPause() {
        mSensorManager.unregisterListener(_SensorEventListener);
        super.onPause();
    }



    /********ON STOP***************************************************************************/

    @Override
    public void onStop() {
        try {
            out.write("quit");
            out.flush();
            mysocket.close();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        clientThread.interrupt();
        sensorsThread.interrupt();
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        mSensorManager.unregisterListener(_SensorEventListener);
        super.onDestroy();
    }



    /********NORMALIZE ANGLES******************************************************************/

    void normalizeAngles(){
        int sensorMin=10, sensorMax=30;
        double aX=angleAxes[0];
        double aY=angleAxes[1];
        //Resting zone
        if(aX<sensorMin && aX>-sensorMin) aX=0;
        if(aY<sensorMin && aY>-sensorMin) aY=0;
        //Map from angles to aX values between -1 and 1
        if(aX<-sensorMin) aX=map(aX,-sensorMin,-sensorMax,0,1);
        if(aX>sensorMin)  aX=map(aX,sensorMin,sensorMax,0,-1);
        if(aY<-sensorMin) aY=map(aY,-sensorMin,-sensorMax,0,1);
        if(aY>sensorMin)  aY=map(aY,sensorMin,sensorMax,0,-1);
        aY=map(aY,-1,1,1,-1);
        //Limit maximum values in all axes
        if(aX<-1) aX=-1;
        if(aX>1)  aX=1;
        if(aY<-1) aY=-1;
        if(aY>1)  aY=1;

        angleAxes[0]=aX;
        angleAxes[1]=aY;
    }

    double map(double vx, double v1, double v2, double n1, double n2){
        // v1 start of range, v2 end of range, vx the starting number between the range
        double percentage = (vx-v1)/(v2-v1);
        // n1 start of new range, n2 end of new range
        return (n2-n1)*percentage+n1;
    }



    /********UPDATE ORIENTATION ANGLES*********************************************************/

    public void updateOrientationAngles() {
        // Update rotation matrix, which is needed to update orientation angles.
        mSensorManager.getRotationMatrix(mRotationMatrix, null,
                mAccelerometerReading, mMagnetometerReading);
        mSensorManager.getOrientation(mRotationMatrix, mOrientationAngles);
        // "mOrientationAngles" now has up-to-date information.

        // double yaw = Math.toDegrees(mOrientationAngles[0]);
        double pitch = Math.toDegrees(mOrientationAngles[1]);
        double roll = Math.toDegrees(mOrientationAngles[2]);

        angleAxes[0] = pitch;
        angleAxes[1] = roll;
        normalizeAngles();
        pitch=angleAxes[0];
        roll=angleAxes[1];

        String Pitch = String.format("%.2f", pitch);
        if (pitch >= 0){
            Pitch = "+" + Pitch;
        }
        String Roll = String.format("%.2f", roll);
        if (roll >= 0){
            Roll = "+" + Roll;
        }
        String dataOut = Pitch + " " + Roll + ";" ;

//        text.setText(dataOut);

        try {
            out.write(dataOut);
            out.flush();
        }catch (java.lang.NullPointerException e1){
            e1.printStackTrace();
        }
    }



    /********SENSOR EVENT LISTENER*************************************************************/

    SensorEventListener _SensorEventListener=   new SensorEventListener() {
        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // Do something here if sensor accuracy changes.
            // Function MUST be declared for it to work.
        }

        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor == mAccelerometer) {
                System.arraycopy(event.values, 0, mAccelerometerReading,
                        0, mAccelerometerReading.length);
            }
            else if (event.sensor == mMagnetometer) {
                System.arraycopy(event.values, 0, mMagnetometerReading,
                        0, mMagnetometerReading.length);
            }
        }
    };



    /********SOCKET CLIENT THREAD**************************************************************/

    class ClientThread implements Runnable {

        @Override
        public void run() {
            try {
                InetAddress serverAddr = InetAddress.getByName(IP);
                mysocket = new Socket(serverAddr, PORT);
                out = new PrintWriter(mysocket.getOutputStream(),true);
            } catch (UnknownHostException e1) {
                e1.printStackTrace();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }
    }



    /********ANGLE SENDER THREAD***************************************************************/

    public class UpdateThread implements Runnable {

        @Override
        public void run() {
            while (true) {
                try {
                    updateOrientationAngles();
                    sensorsThread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (java.lang.NullPointerException e){
                    e.printStackTrace();

                }
            }
        }

    }

}
