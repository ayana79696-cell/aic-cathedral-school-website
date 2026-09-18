package ke.aiccathedral.schoolmessenger;

import android.app.*;
import android.content.*;
import android.telephony.SmsManager;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class SmsSentReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        String recipientId = intent.getStringExtra("recipient_id");
        String token = intent.getStringExtra("token");
        int result = getResultCode();
        boolean ok = result == Activity.RESULT_OK;
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject()
                    .put("action","complete")
                    .put("recipient_id",recipientId)
                    .put("status", ok ? "sent" : "failed")
                    .put("error_message", ok ? JSONObject.NULL : "Android SMS result " + result);
                HttpURLConnection c = (HttpURLConnection) new URL(MainActivity.ENDPOINT).openConnection();
                c.setRequestMethod("POST");
                c.setConnectTimeout(15000);
                c.setReadTimeout(30000);
                c.setDoOutput(true);
                c.setRequestProperty("Content-Type","application/json");
                c.setRequestProperty("Authorization","Bearer " + token);
                try (OutputStream os = c.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }
                c.getResponseCode();
            } catch (Exception ignored) {}
        }).start();
    }
}
