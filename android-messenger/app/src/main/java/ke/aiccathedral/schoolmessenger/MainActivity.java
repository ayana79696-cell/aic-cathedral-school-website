package ke.aiccathedral.schoolmessenger;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telephony.SmsManager;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    static final String ENDPOINT = "https://rmarersocrwzqhnbuygi.supabase.co/functions/v1/school-phone-sms";
    static final String PREFS = "aic_sms";
    static final int SMS_PERMISSION = 44;

    EditText tokenInput;
    TextView statusText, broadcastText, logText;
    Button saveButton, checkButton, sendButton;
    ProgressBar progress;
    JSONObject pendingBroadcast;
    JSONArray pendingRecipients;
    final Handler handler = new Handler(Looper.getMainLooper());

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_main);
        tokenInput = findViewById(R.id.tokenInput);
        statusText = findViewById(R.id.statusText);
        broadcastText = findViewById(R.id.broadcastText);
        logText = findViewById(R.id.logText);
        saveButton = findViewById(R.id.saveTokenButton);
        checkButton = findViewById(R.id.checkButton);
        sendButton = findViewById(R.id.sendButton);
        progress = findViewById(R.id.progress);

        String saved = getPreferences(0).getString("token", "");
        tokenInput.setText(saved);
        saveButton.setOnClickListener(v -> testConnection());
        checkButton.setOnClickListener(v -> fetchPending());
        sendButton.setOnClickListener(v -> sendPending());

        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED)
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION);

        if (!saved.isEmpty()) testConnection();
        handler.postDelayed(new Runnable() {
            public void run() { if (!getToken().isEmpty()) fetchPending(); handler.postDelayed(this, 15000); }
        }, 15000);
    }

    String getToken() { return tokenInput.getText().toString().trim(); }

    void testConnection() {
        final String token = getToken();
        if (token.isEmpty()) { statusText.setText("Enter the device token."); return; }
        getPreferences(0).edit().putString("token", token).apply();
        new Thread(() -> {
            try {
                JSONObject result = post(token, new JSONObject().put("action","next"));
                runOnUiThread(() -> {
                    statusText.setText("Connected ✓");
                    log("Phone connected to AIC ERP.");
                    parsePending(result);
                });
            } catch (Exception e) {
                runOnUiThread(() -> statusText.setText("Connection failed: " + e.getMessage()));
            }
        }).start();
    }

    void fetchPending() {
        final String token = getToken();
        if (token.isEmpty()) return;
        new Thread(() -> {
            try {
                JSONObject result = post(token, new JSONObject().put("action","next"));
                runOnUiThread(() -> parsePending(result));
            } catch (Exception e) {
                runOnUiThread(() -> log("Check failed: " + e.getMessage()));
            }
        }).start();
    }

    void parsePending(JSONObject result) {
        try {
            pendingBroadcast = result.optJSONObject("broadcast");
            pendingRecipients = result.optJSONArray("recipients");
            if (pendingBroadcast == null || pendingRecipients == null || pendingRecipients.length() == 0) {
                broadcastText.setText("No pending broadcast");
                sendButton.setEnabled(false);
                return;
            }
            broadcastText.setText("Pending: " + pendingBroadcast.optString("title") +
                    "\nRecipients: " + pendingRecipients.length() +
                    "\nMessage: " + pendingBroadcast.optString("message"));
            sendButton.setEnabled(true);
        } catch (Exception e) { log("Invalid server response."); }
    }

    void sendPending() {
        if (pendingRecipients == null || pendingRecipients.length() == 0) return;
        if (checkSelfPermission(Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.SEND_SMS}, SMS_PERMISSION);
            return;
        }
        sendButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        progress.setMax(pendingRecipients.length());
        progress.setProgress(0);
        new Thread(() -> {
            for (int i = 0; i < pendingRecipients.length(); i++) {
                try {
                    JSONObject r = pendingRecipients.getJSONObject(i);
                    sendOne(r);
                    final int p = i + 1;
                    runOnUiThread(() -> { progress.setProgress(p); log("Queued " + p + " / " + pendingRecipients.length()); });
                    Thread.sleep(250);
                } catch (Exception e) {
                    log("Could not queue message: " + e.getMessage());
                }
            }
            runOnUiThread(() -> {
                progress.setVisibility(View.GONE);
                broadcastText.setText("Broadcast sent/processing. Check ERP history.");
            });
        }).start();
    }

    void sendOne(JSONObject r) {
        String phone = r.optString("phone");
        String message = r.optString("message", pendingBroadcast.optString("message"));
        String recipientId = r.optString("id");
        Intent intent = new Intent(this, SmsSentReceiver.class);
        intent.putExtra("recipient_id", recipientId);
        intent.putExtra("token", getToken());
        int requestCode = Math.abs(recipientId.hashCode());
        android.app.PendingIntent sent = android.app.PendingIntent.getBroadcast(
                this, requestCode, intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        SmsManager.getDefault().sendTextMessage(phone, null, message, sent, null);
    }

    JSONObject post(String token, JSONObject body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(ENDPOINT).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(15000);
        c.setReadTimeout(30000);
        c.setDoOutput(true);
        c.setRequestProperty("Content-Type","application/json");
        c.setRequestProperty("Authorization","Bearer " + token);
        try (OutputStream os = c.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = c.getResponseCode();
        InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
        String text = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines()
                .reduce("", (a,b) -> a + b);
        if (code >= 400) throw new IOException(text);
        return new JSONObject(text);
    }

    static void log(String s) {
        // receiver handles its own logging; UI logging is intentionally lightweight.
    }
}
