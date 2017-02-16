package com.example.bxy.arduino_bt_android;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.LogRecord;

public class MainActivity extends AppCompatActivity {

    private  String TAG="看我";

    private BluetoothAdapter mBtAdapter;
    private boolean mBtFlag;
    private Thread mThread;
    private boolean mThreadFlag;
    private BluetoothSocket mBtSocket;
    private BluetoothDevice mBtDevice;
    private static final UUID HC_UUID =
            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private static final String HC_MAC="98:D3:31:FD:28:6E";
    private  InputStream inStream;
    private  OutputStream outStream;
    private EditText Message;
    private Button BtSend;
    private Button Btopen;
    private Button Btclose;
    private TextView Tv;
    private TextView Tvdevice_name;
    private TextView Tvdevice_address;
    private TextView Tvstate;

    private final int Message_Arduino = 1;
    private final int Message_Android = 2;
    private final int Message_ConnectError = 3;

    //用来更新UI
    private Handler UIHandler;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Message=(EditText) findViewById(R.id.edit_message);
        BtSend= (Button) findViewById(R.id.BtSend);
        BtSend.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v){
                writeSerial(Integer.valueOf(Message.getText().toString()));
                //Tv.append("Android:"+Message.getText()+"\n");
                Message.setText("");
            }
        });
        Btopen = (Button) findViewById(R.id.Btopen);
        Btopen.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v){
                if(!mBtFlag){ //如果此时未连接
                    myStartService();
                }else{
                    showToast("已连接，请勿重复点击");
                }
            }
        });
        Btclose = (Button) findViewById(R.id.Btclose);
        Btclose.setOnClickListener(new Button.OnClickListener(){
            @Override
            public void onClick(View v){
                if(!mBtFlag){ //如果此时未连接
                    showToast("当前未连接");
                }else{
                    myStopService();
                }
            }
        });

        Tv=(TextView) findViewById(R.id.TV);
        Tvdevice_address= (TextView)findViewById(R.id.device_address);
        Tvdevice_address.setText(HC_MAC);
        Tvdevice_name=(TextView)findViewById(R.id.device_name);
        Tvstate=(TextView)findViewById(R.id.connection_state);
        //创建属于主线程的handler
        //创建属于主线程的handler
        UIHandler=new Handler();
    }

    @Override
    protected void onStart(){
        super.onStart();
        myStartService();
    }
    @Override
    protected void onDestroy(){
        super.onDestroy();
        myStopService();
    }

    /**
     * Called by onStartCommand, initialize and start runtime thread
     */
    private void myStartService() {
        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if ( mBtAdapter == null ) {
            showToast("Bluetooth unused.");
            mBtFlag  = false;
            return;
        }
        if ( !mBtAdapter.isEnabled() ) {
            mBtFlag  = false;
            myStopService();
            showToast("Open bluetoooth then restart program!!");
            return;
        }

        showToast("Start searching!!");
        mThreadFlag = true;
        mThread = new MyThread();
        mThread.start();
    }

    /**
     * Called by onDestroyCommand
     */
    private void myStopService(){
        if(mThreadFlag==true){
            mThread.destroy();
            mThreadFlag = false;
        }
        mBtSocket = null;
        mBtDevice = null;
    }
    /**
     * Thread runtime
     */
    public class MyThread extends Thread {
        @Override
        public void run() {
            super.run();
            myBtConnect();
            while( mThreadFlag ) {
                readSerial();
                try{
                    Thread.sleep(30);
                }catch(Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * device control
     */
    public void myBtConnect() {
        showToast("Connecting...");

        /* Discovery device */
         mBtDevice = mBtAdapter.getRemoteDevice(HC_MAC);
        /*BluetoothDevice mBtDevice = null;
        Set<BluetoothDevice> mBtDevices = mBtAdapter.getBondedDevices();
        if ( mBtDevices.size() > 0 ) {
            for (Iterator<BluetoothDevice> iterator = mBtDevices.iterator();
                 iterator.hasNext(); ) {
                mBtDevice = (BluetoothDevice)iterator.next();
                showToast(mBtDevice.getName() + "|" + mBtDevice.getAddress());
            }
        }*/

        try {
            mBtSocket = mBtDevice.createRfcommSocketToServiceRecord(HC_UUID);
            showToast("Connecting to "+mBtSocket.getRemoteDevice().getName());
        } catch (IOException e) {
            e.printStackTrace();
            mBtFlag = false;
            showToast("Create bluetooth socket error");
        }

        mBtAdapter.cancelDiscovery();

    /* Setup connection */
        try {
            mBtSocket.connect();
            updateUI(true);
            showToast("Connect bluetooth success");
            Log.i(TAG, "Connect " + HC_MAC + " Success!");
            mBtFlag=true;
        } catch (IOException e) {
            e.printStackTrace();
            try {
                updateUI(false);
                showToast("Connect error, close");
                mHandler.obtainMessage(Message_ConnectError,"Connect error, Please try again").sendToTarget();
                mBtFlag = false;
                mBtSocket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

    /* I/O initialize */
        if ( mBtFlag ) {
            try {
                inStream  = mBtSocket.getInputStream();
                outStream = mBtSocket.getOutputStream();
                showToast("Bluetooth Connection is ready! Start Chatting");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    /**
     * Read serial data from HC06
     */
    public int readSerial() {
        int ret = 0;
        byte[] rsp = null;

        if ( !mBtFlag ) {
            return -1;
        }
        try {
            rsp = new byte[inStream.available()];
            ret = inStream.read(rsp);
            String con = new String(rsp);
            showToast(con);
            if(!con.isEmpty())
                mHandler.obtainMessage(Message_Arduino,con).sendToTarget();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return ret;
    }

    /**
     * Write serial data to HC06
     * @param value - command
     */
    public void writeSerial(int value) {
        showToast("点击Send");
        String ha = "" + value;
        mHandler.obtainMessage(Message_Android,ha).sendToTarget();
        try {
            outStream.write(ha.getBytes());
            outStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showToast(String str){
        Log.d(TAG,str);
    }

    //更新界面
    public void updateUI(boolean flag){
        if(flag) {
            // 构建Runnable对象，在runnable中更新界面
            final Runnable runnableUi = new Runnable() {
                @Override
                public void run() {
                    Tvdevice_name.setText(mBtDevice.getName());
                    Tvdevice_address.setText(mBtDevice.getAddress());
                    Tvstate.setText("Connected");
                    Btopen.setEnabled(false);
                    Btclose.setEnabled(true);
                    BtSend.setEnabled(true);
                }
            };
            new Thread(){
                public void run(){
                    UIHandler.post(runnableUi);
                }
            }.start();
        }else{
            // 构建Runnable对象，在runnable中更新界面
            final Runnable runnableUi = new Runnable() {
                @Override
                public void run() {
                    Tvdevice_name.setText("");
                    Tvdevice_address.setText("");
                    Tvstate.setText("Disconnected");
                    Btopen.setEnabled(true);
                    Btclose.setEnabled(false);
                    BtSend.setEnabled(false);
                }
            };
            new Thread(){
                public void run(){
                    UIHandler.post(runnableUi);
                }
            }.start();
        }
    }


    private final Handler mHandler = new Handler() {

        @Override
        public void handleMessage(Message msg){
            switch(msg.what){
                case Message_Arduino:
                    showToast("Arduino:"+msg.obj);
                    Tv.append("Arduino:"+msg.obj+"\n");
                    break;
                case Message_Android:
                    showToast("Android:"+msg.obj);
                    Tv.append("Android:"+msg.obj+"\n");
                    break;
                case Message_ConnectError:
                    Tv.append(msg.obj.toString());
            }
        }
    };

}
