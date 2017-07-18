package com.mazouri.grpc_android_sample;

import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.google.common.util.concurrent.UncheckedExecutionException;

import java.util.concurrent.TimeUnit;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.examples.helloworld.GreeterGrpc;
import io.grpc.examples.helloworld.HelloRequest;
import io.grpc.examples.helloworld.HelloResponse;

public class MainActivity extends AppCompatActivity {

    @Bind(R.id.main_edit_server_host)
    EditText mServerHostEditText;

    @Bind(R.id.main_edit_server_port)
    EditText mServerPortEditText;

    @Bind(R.id.message)
    EditText mMessageToSend;

    @Bind(R.id.main_text_log)
    TextView mLogText;

    @Bind(R.id.main_button_send_request)
    Button mSendButton;

    private ManagedChannel mChannel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ButterKnife.bind(this);

        mLogText.setMovementMethod(new ScrollingMovementMethod());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ButterKnife.unbind(this);
        shutdownChannel();
    }

    @OnClick(R.id.main_button_send_request)
    public void sendRequestToServer () {
        new SendHelloTask().execute();
    }

    private void log ( String logMessage ) {
        mLogText.append("\n" + logMessage);
    }

    private void shutdownChannel () {
        if (mChannel != null) {
            try {
                mChannel.shutdown().awaitTermination(1, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // FIXME this call seems fishy as it interrupts the main thread
                Thread.currentThread().interrupt();
            }
        }
        mChannel = null;
    }


    private class SendHelloTask extends AsyncTask<Void, Void, String> {

        private String mHost = "";
        private int mPort = -1;
        private String mMessage;

        @Override
        protected void onPreExecute () {
            mSendButton.setEnabled(false);

            String newHost = mServerHostEditText.getText().toString();
            if (!mHost.equals(newHost)) {
                mHost = newHost;
                shutdownChannel();
            }
            if (TextUtils.isEmpty(mHost)) {
                log("ERROR: empty host name!");
                cancel(true);
                return;
            }

            String portString = mServerPortEditText.getText().toString();
            if (TextUtils.isEmpty(portString)) {
                log("ERROR: empty port");
                cancel(true);
                return;
            }

            mMessage = mMessageToSend.getText().toString();

            if (TextUtils.isEmpty(mMessage)) {
                mMessage = "Default message";
            }

            try {
                int newPort = Integer.parseInt(portString);
                if (mPort != newPort) {
                    mPort = newPort;
                    shutdownChannel();
                }
            } catch (NumberFormatException ex) {
                log("ERROR: invalid port");
                cancel(true);
                return;
            }

            log("Sending hello to server...");
        }

        @Override
        protected String doInBackground ( Void... params ) {
            try {
                if (mChannel == null) {
                    mChannel = ManagedChannelBuilder.forAddress(mHost, mPort).usePlaintext(true).build();
                }
                GreeterGrpc.GreeterBlockingStub greeterStub = GreeterGrpc.newBlockingStub(mChannel);
                HelloRequest helloRequest = HelloRequest.newBuilder().setName(mMessage).build();

                HelloResponse helloResponse = greeterStub.sayHello(helloRequest);
                return "SERVER: " + helloResponse.getMessage();
            } catch ( SecurityException | UncheckedExecutionException e ) {
                e.printStackTrace();
                return "ERROR: " + e.getMessage();
            }
        }

        @Override
        protected void onPostExecute ( String s ) {
            shutdownChannel();
            log(s);
            mSendButton.setEnabled(true);
        }

        @Override
        protected void onCancelled () {
            mSendButton.setEnabled(true);
        }
    }
}
