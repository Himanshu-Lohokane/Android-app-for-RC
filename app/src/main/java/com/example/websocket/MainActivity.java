package com.example.websocket;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AlertDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";
    private WebSocketClient webSocketClient;
    private TextView statusText, messageText;
    private EditText ipAddressEditText;
    private Button connectButton, submitIpButton, buttonForward, buttonBackward, buttonLeft, buttonRight;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String serverIpAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Set the activity to full-screen mode
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS);
        // Hide the navigation and status bars
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        // Hide the action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        setContentView(R.layout.activity_main);

        // Initialize UI elements
        statusText = findViewById(R.id.statusText);
        messageText = findViewById(R.id.messageText);
        connectButton = findViewById(R.id.connectButton);
        ipAddressEditText = findViewById(R.id.ipAddressEditText);
        submitIpButton = findViewById(R.id.submitIpButton);
        buttonForward = findViewById(R.id.button_forward);
        buttonBackward = findViewById(R.id.button_backward);
        buttonLeft = findViewById(R.id.button_left);
        buttonRight = findViewById(R.id.button_right);

        // Set up button click listeners
        connectButton.setOnClickListener(v -> showIpAddressDialog());

        // Set up touch listeners for control buttons
        buttonForward.setOnTouchListener(motorTouchListener);
        buttonBackward.setOnTouchListener(motorTouchListener);
        buttonLeft.setOnTouchListener(servoTouchListener);
        buttonRight.setOnTouchListener(servoTouchListener);
    }

    private void showIpAddressDialog() {
        // Create an AlertDialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("WebSocket Connection");

        // Set up the input field
        final EditText input = new EditText(this);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT);
        input.setHint("Enter IP Address");
        if (serverIpAddress != null && !serverIpAddress.isEmpty()) {
            input.setText(serverIpAddress);
        }
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton("Connect", (dialog, which) -> {
            serverIpAddress = input.getText().toString();
            if (!serverIpAddress.isEmpty()) {
                createWebSocketClient();
                webSocketClient.connect();
            } else {
                statusText.setText("Please enter a valid IP address.");
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        // Show the dialog
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private final View.OnTouchListener motorTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (v.getId() == R.id.button_forward) {
                        sendPinCommand(23, "ON");
                        sendPinCommand(27, "ON");
                    } else if (v.getId() == R.id.button_backward) {
                        sendPinCommand(24, "ON");
                        sendPinCommand(22, "ON");
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (v.getId() == R.id.button_forward) {
                        sendPinCommand(23, "OFF");
                        sendPinCommand(27, "OFF");
                    } else if (v.getId() == R.id.button_backward) {
                        sendPinCommand(24, "OFF");
                        sendPinCommand(22, "OFF");
                    }
                    return true;
            }
            return false;
        }
    };

    private final View.OnTouchListener servoTouchListener = new View.OnTouchListener() {
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (v.getId() == R.id.button_left) {
                        sendServoCommand(5);    // Left position (5°)
                    } else if (v.getId() == R.id.button_right) {
                        sendServoCommand(180);  // Right position (180°)
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    sendServoCommand(90);       // Center position (90°)
                    return true;
            }
            return false;
        }
    };

    private void sendPinCommand(int pin, String state) {
        try {
            if (webSocketClient != null && webSocketClient.isOpen()) {
                JSONObject command = new JSONObject();
                command.put("pin", pin);
                command.put("state", state);
                webSocketClient.send(command.toString());
                mainHandler.post(() -> messageText.setText("Sent: " + command.toString()));
                Log.d(TAG, "Sent motor command: " + command.toString());
            } else {
                Log.e(TAG, "WebSocket not connected when trying to send motor command");
                mainHandler.post(() -> messageText.setText("Error: Not connected"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending motor command", e);
            mainHandler.post(() -> messageText.setText("Error: " + e.getMessage()));
        }
    }

    private void sendServoCommand(int angle) {
        try {
            if (webSocketClient != null) {
                if (webSocketClient.isOpen()) {
                    JSONObject command = new JSONObject();
                    command.put("servo", angle);
                    String commandStr = command.toString();
                    webSocketClient.send(commandStr);
                    mainHandler.post(() -> {
                        messageText.setText("Servo: " + angle + "°");
                        Log.d(TAG, "Sent servo command: " + commandStr);
                    });
                } else {
                    Log.e(TAG, "WebSocket not open when trying to send servo command");
                    mainHandler.post(() -> messageText.setText("Error: Not connected"));
                }
            } else {
                Log.e(TAG, "WebSocket client null when trying to send servo command");
                mainHandler.post(() -> messageText.setText("Error: No connection"));
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending servo command", e);
            mainHandler.post(() -> messageText.setText("Error: " + e.getMessage()));
        }
    }

    private void createWebSocketClient() {
        URI uri;
        try {
            uri = new URI("ws://" + serverIpAddress + ":8765");
            Log.d(TAG, "WebSocket URI: " + uri.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.e(TAG, "Invalid WebSocket URI", e);
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                mainHandler.post(() -> {
                    statusText.setText("Connected");
                    connectButton.setText("Disconnect");
                    Log.d(TAG, "WebSocket connected");
                });
            }

            @Override
            public void onMessage(String message) {
                mainHandler.post(() -> {
                    messageText.setText("Received: " + message);
                    Log.d(TAG, "WebSocket message: " + message);
                });
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                mainHandler.post(() -> {
                    statusText.setText("Disconnected");
                    connectButton.setText("Connect");
                    Log.d(TAG, "WebSocket closed: " + reason);
                });
            }

            @Override
            public void onError(Exception ex) {
                mainHandler.post(() -> {
                    statusText.setText("Error: " + ex.getMessage());
                    Log.e(TAG, "WebSocket error", ex);
                });
            }
        };
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Clean up WebSocket connection if it exists
        if (webSocketClient != null) {
            if (webSocketClient.isOpen()) {
                webSocketClient.close();
            }
        }
    }
}