package com.example.websocket;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.os.Handler;
import android.os.Looper;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.net.URISyntaxException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private WebSocketClient webSocketClient;
    private TextView statusText, messageText;
    private Button connectButton, ledOnButton, ledOffButton;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Initialize UI elements
        statusText = findViewById(R.id.statusText);
        messageText = findViewById(R.id.messageText);
        connectButton = findViewById(R.id.connectButton);
        ledOnButton = findViewById(R.id.ledOnButton);
        ledOffButton = findViewById(R.id.ledOffButton);

        // Set up button click listeners
        connectButton.setOnClickListener(v -> toggleConnection());
        ledOnButton.setOnClickListener(v -> sendCommand(17, "on"));
        ledOffButton.setOnClickListener(v -> sendCommand(17, "off"));

        // Initial connection
        createWebSocketClient();
    }

    private void createWebSocketClient() {
        URI uri;
        try {
            // Replace with your WebSocket server address
            uri = new URI("ws://192.168.1.11:8765");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return;
        }

        webSocketClient = new WebSocketClient(uri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                mainHandler.post(() -> {
                    statusText.setText("Connected");
                    connectButton.setText("Disconnect");
                });
            }

            @Override
            public void onMessage(String message) {
                mainHandler.post(() -> messageText.setText("Received: " + message));
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                mainHandler.post(() -> {
                    statusText.setText("Disconnected");
                    connectButton.setText("Connect");
                });
            }

            @Override
            public void onError(Exception ex) {
                mainHandler.post(() -> statusText.setText("Error: " + ex.getMessage()));
            }
        };
    }

    private void toggleConnection() {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            webSocketClient.close();
        } else {
            createWebSocketClient();
            webSocketClient.connect();
        }
    }

    private void sendCommand(int pin, String state) {
        if (webSocketClient != null && webSocketClient.isOpen()) {
            try {
                JSONObject command = new JSONObject();
                command.put("pin", pin);
                command.put("state", state);
                webSocketClient.send(command.toString());
                mainHandler.post(() -> messageText.setText("Sent: " + command.toString()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }
}