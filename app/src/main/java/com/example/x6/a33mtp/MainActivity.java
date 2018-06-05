package com.example.x6.a33mtp;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.example.x6.serial.SerialPort;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity {
    private final String TAG = "MainActivity";
    private TextView display;
    private SerialPort serialPort;
    private InputStream inputStream;
    private OutputStream outputStream;
    private boolean isStart = true;
//    private byte[] bytes = new byte[]{0x01,0x02,0x03,0x04,0x05,0x01,1,2,3,1,5,1,32,5,1,23,5,2,1,3,4,21,3,4,1,32,1,34,14,2,13,4,1,4,3,2,4,1};
    private byte[] bytes = new byte[126];
    private long lastTimeM ;
    private long timeOut = 200;
    private int recvNum = 0;
    private int recvWrongNum = 0;
    private int sendNum = 0;

    public final int SEND_START = 0;
    public final int SEND_ONE_FPS = 1;
    public final int SEND_END = 2;
    public final int RECV_START = 3;
    public final int RECV_ONE_FPS = 4;
    public final int RECV_END = 5;
    public final int RECV_OUT_TIME = 6;
    public final int RECV_WRONG_FPS = 7;

    private final String FLAG_KEY = "FLAG_KEY";

    private void sendMsgFlag(int what, int flag){
        Message message = new Message();
        message.what = what;
        Bundle bundle = new Bundle();
        bundle.putInt(FLAG_KEY,flag);
        message.setData(bundle);
        handler.sendMessage(message);
    }

    Handler handler = new Handler(){
        Bundle bundle = null;
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case 3:
                    bundle = msg.getData();
                    switch (bundle.getInt(FLAG_KEY)){
                        case SEND_ONE_FPS:
                            sendNum += 1;
                            display.setText("发送了 "+String.valueOf(sendNum)+ "帧数据\n"
                                    +"接收了 " + String.valueOf(recvNum)+"帧正确数据\n"
                                    +"接收了 " + String.valueOf(recvWrongNum)+"帧错误数据\n");
                            break;
                        case RECV_ONE_FPS:
                            recvNum += 1;
                            display.setText("发送了 "+String.valueOf(sendNum)+ "帧数据\n"
                                    +"接收了 " + String.valueOf(recvNum)+"帧正确数据\n"
                                    +"接收了 " + String.valueOf(recvWrongNum)+"帧错误数据\n");
                            break;
                        case RECV_WRONG_FPS:
                            recvWrongNum += 1;
                            display.setText("发送了 "+String.valueOf(sendNum)+ "帧数据\n"
                                    +"接收了 " + String.valueOf(recvNum)+"帧正确数据\n"
                                    +"接收了 " + String.valueOf(recvWrongNum)+"帧错误数据\n");
                            break;
                        case RECV_OUT_TIME:
                            break;
                    }
                    break;
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        for(byte i = 0; i < bytes.length; i++){
            bytes[i] = (byte) (i+1);
        }
        String str = "";
        for(int j =0; j < bytes.length; j++){
            str += String.format("%c ",j);
        }
        display = (TextView) findViewById(R.id.display);
        display.setText("发送了 "+String.valueOf(sendNum)+ "帧数据\n"
                +"接收了 " + String.valueOf(recvNum)+"帧正确数据\n"
                +"接收了 " + String.valueOf(recvWrongNum)+"帧错误数据\n");
        try {
            serialPort = new SerialPort(new File("/dev/ttyS3"),115200,0,50);
            inputStream = serialPort.getInputStream();
            outputStream = serialPort.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
        start();
    }

    private void start(){
        new Thread(){
            @Override
            public void run() {
                Looper.prepare();
                byte[] recvbyte0 = new byte[1024];
                while(isStart){
                    int length = 0;
                    lastTimeM = System.currentTimeMillis();
                    int j = 0;
                    int size = 0;
                    String str = "";
                    while ( isStart && System.currentTimeMillis() - lastTimeM <= timeOut && inputStream != null){
                        try {
                            if(inputStream.available()>0 == false){
                                continue;
                            }else{
                                Thread.sleep(20);
                            }
                            size = inputStream.read(recvbyte0);
                            if(size > 0){
                                length += size;
                            }else{
                                Log.e(TAG,"serial port 3 receive time out");
                                sendMsgFlag(3,RECV_OUT_TIME);
                                continue;
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                    boolean isRight = true;
                    if(length > 0) {
                        for (int i = 0; i < bytes.length; i++) { // 验证收到的这一帧信息是否正确
                            str += String.format("0x%x ", recvbyte0[i]);
                            if (recvbyte0[i] != bytes[i])
                                isRight = false;
                        }
                        Log.i(TAG, "Serial port 3 RECV:<--" + str);
                        if (isRight) {
                            Log.e(TAG, "串口接收数据正常");
                            sendMsgFlag(3,RECV_ONE_FPS);
                            try {
                                outputStream.write(bytes);
                                outputStream.flush();
                                sendMsgFlag(3,SEND_ONE_FPS);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }else{
                            sendMsgFlag(3,RECV_WRONG_FPS);
                        }
                    }
                }
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        isStart = false;
    }
}
