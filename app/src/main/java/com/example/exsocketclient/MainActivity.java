package com.example.exsocketclient;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    private EditText et_send_msg;
    private Button btn_send;

    private TextView tv_message_body;

    private EditText et_server_ip;
    private EditText et_server_port;
    private Button btn_connect;
    private Button btn_disconnect;

    private AlertDialog alertDialog;

    private Handler handler;

    private Socket socket;

    private BufferedWriter networkWriter;
    private BufferedReader networkReader;

    private String readline = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        handler = new Handler();

        et_send_msg = findViewById(R.id.et_send_msg);
        btn_send = findViewById(R.id.btn_send);

        tv_message_body = findViewById(R.id.tv_message_body);

        et_server_ip = findViewById(R.id.et_server_ip);
        et_server_port = findViewById(R.id.et_server_port);
        btn_connect = findViewById(R.id.btn_connect);
        btn_disconnect = findViewById(R.id.btn_disconnect);

        btn_send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        btn_connect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ip = et_server_ip.getText().toString();
                String port = et_server_port.getText().toString();

                if(!ip.equals("") && !port.equals("")){
                    setSocket(ip, Integer.parseInt(port));
                }else{
                    showDialog("ip 또는 port를 입력해주세요.");
                }
            }
        });

        btn_disconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                disConnect();
            }
        });

        et_send_msg.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if(event.getAction() == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_ENTER){
                    sendMessage();
                    return true;
                }
                return false;
            }
        });
    }

    private void showDialog(String message){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("알림");
        builder.setMessage(message);
        builder.setNegativeButton("확인", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                alertDialog.dismiss();
            }
        });

        if(alertDialog == null) alertDialog = builder.create();
        alertDialog.show();

        alertDialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                alertDialog = null;
            }
        });
    }

    private void setSocket(String ip, int port){
        if(socket == null) new SocketTask(ip, port).execute();
    }

    private Runnable showUpdate = new Runnable() {

        public void run() {
            tv_message_body.setText(tv_message_body.getText().toString() + readline + "\n");
        }

    };

    private void disConnect(){
        if(socket != null && socket.isConnected() && !socket.isClosed()) {
            try {
                socket.close();
                socket = null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    class CheckUpdate extends Thread {
        @Override
        public void run() {
            try{
                while (true){
                    if(socket == null || socket.isClosed()) break;
                    readline = networkReader.readLine();
                    handler.post(showUpdate);
                }
            }catch (Exception e){
                e.printStackTrace();
                socket = null;
                tv_message_body.setText(tv_message_body.getText() + "서버 연결 종료\n");
            }
        }
    }

    class SendMessage extends Thread{
        @Override
        public void run() {
            if(socket == null || socket.isClosed()) return;
            String message = et_send_msg.getText().toString();

            PrintWriter out = new PrintWriter(networkWriter, true);
            out.print(message);
            out.flush();

            readline = ("[나]: " + message);
            handler.post(showUpdate);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    et_send_msg.setText("");
                    et_send_msg.requestFocus();
                }
            });
        }
    }

    class SocketTask extends AsyncTask<String, String, String>{
        String ip;
        int port;

        SocketTask(String ip, int port){
            this.ip = ip;
            this.port = port;
        }

        @Override
        protected String doInBackground(String... strings) {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(ip, port), 5000);
                networkWriter = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), "EUC-KR"));
                networkReader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "EUC-KR"));

                CheckUpdate thread = new CheckUpdate();
                thread.start();
            }catch (IOException e){
                socket = null;
                e.printStackTrace();
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        showDialog("소켓 서버를 확인해주세요.");
                    }
                });
            }

            return "";
        }
    }

    private void sendMessage(){
        if(socket != null && socket.isConnected()) {
            String msg = et_send_msg.getText().toString();

            if (!msg.equals("")) {
                SendMessage sendMessageThread = new SendMessage();
                sendMessageThread.start();
            } else {
                showDialog("전송할 메세지를 입력하세요.");
            }
        }else{
            showDialog("서버 연결을 확인하세요.");
        }
    }
}