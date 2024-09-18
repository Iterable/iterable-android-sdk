package com.iterable.androidsdk;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.EditText;
import com.iterable.iterableapi.CommerceItem;
import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.IterableConfig;
import com.iterable.iterableapi.IterableConstants;
import com.iterable.iterableapi.IterableIdentityResolution;
import com.iterable.iterableapi.testapp.R;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AnonTrackingTestActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableAnonTracking(true).build();

        // clear data for testing
        SharedPreferences sharedPref = getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(IterableConstants.SHARED_PREFS_ANON_SESSIONS, "");
        editor.putString(IterableConstants.SHARED_PREFS_EVENT_LIST_KEY, "");
        editor.apply();

        new Handler().postDelayed(() -> {
            IterableApi.initialize(getBaseContext(), "18845050c4774b7c9dc48beece2f6d8b", iterableConfig);
            IterableApi.getInstance().setUserId(null);
            IterableApi.getInstance().setEmail(null);
            printAllSharedPreferencesData(this);

        }, 1000);

        findViewById(R.id.updateCart).setOnClickListener(view -> {
            EditText updateCart_edit = findViewById(R.id.updateCart_edit);
            if(updateCart_edit == null) return;
            Log.d("TEST_USER", String.valueOf(updateCart_edit.getText()));
            try {
                JSONArray cartJSOnItems = new JSONArray(String.valueOf(updateCart_edit.getText()));
                List<CommerceItem> items = new ArrayList<>();
                for(int i = 0; i < cartJSOnItems.length(); i++) {
                    final JSONObject item = cartJSOnItems.getJSONObject(i);
                    items.add(new CommerceItem(item.getString("id"), item.getString("name"), item.getDouble("price"), item.getInt("quantity")));
                }
                IterableApi.getInstance().updateCart(items);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        findViewById(R.id.trackPurchase).setOnClickListener(view -> {
            EditText purchase_items = findViewById(R.id.trackPurchase_edit);
            if(purchase_items == null) return;
            Log.d("TEST_USER", String.valueOf(purchase_items.getText()));

            int total;

            try {
                JSONObject jsonData = new JSONObject(String.valueOf(purchase_items.getText()));
                total = (int) jsonData.get("total");
                  JSONArray items_array = jsonData.getJSONArray("items");
                  List<CommerceItem> items = new ArrayList<>();
                  for(int i = 0; i < items_array.length(); i++) {
                      final JSONObject item = items_array.getJSONObject(i);
                      items.add(new CommerceItem(item.getString("id"), item.getString("name"), item.getDouble("price"), item.getInt("quantity")));
                  }
                IterableApi.getInstance().trackPurchase(total, items);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        findViewById(R.id.customEvent).setOnClickListener(view -> {
            EditText customEvent_edit = findViewById(R.id.customEvent_edit);
            if(customEvent_edit == null) return;
            Log.d("TEST_USER", String.valueOf(customEvent_edit.getText()));

            try {
              JSONObject customEventItem = new JSONObject(String.valueOf(customEvent_edit.getText()));
              JSONObject items = new JSONObject(customEventItem.get("dataFields").toString());
              if(customEventItem.has("eventName")) {
                  items.put("eventName", customEventItem.getString("eventName"));
              }
              IterableApi.getInstance().track("customEvent", 0, 0, items);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        findViewById(R.id.updateUser).setOnClickListener(view -> {
            EditText updateUser_edit = findViewById(R.id.updateUser_edit);
            if(updateUser_edit == null) return;
            Log.d("TEST_USER", String.valueOf(updateUser_edit.getText()));

            try {
                JSONObject updateUserItem = new JSONObject(String.valueOf(updateUser_edit.getText()));
                IterableApi.getInstance().updateUser(updateUserItem);

            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });
        findViewById(R.id.setUser).setOnClickListener(view -> {
            EditText setUser_edit = findViewById(R.id.setUser_edit);
            if(setUser_edit == null) return;;
            final IterableIdentityResolution identityResolution = new IterableIdentityResolution();
            IterableApi.getInstance().setUserId(String.valueOf(setUser_edit.getText()), identityResolution);
        });
        findViewById(R.id.setEmail).setOnClickListener(view -> {
            EditText setEmail_edit = findViewById(R.id.setEmail_edit);
            if(setEmail_edit == null) return;
            final IterableIdentityResolution identityResolution = new IterableIdentityResolution();
            IterableApi.getInstance().setEmail(String.valueOf(setEmail_edit.getText()), identityResolution);
        });

        findViewById(R.id.btn_logout).setOnClickListener(view -> {
            IterableApi.getInstance().setUserId(null);
            IterableApi.getInstance().setEmail(null);
        });

    }
    public void printAllSharedPreferencesData(Context context) {
        SharedPreferences sharedPref = context.getSharedPreferences(IterableConstants.SHARED_PREFS_FILE, Context.MODE_PRIVATE);
        Map<String, ?> allEntries = sharedPref.getAll();

        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("SharedPref", entry.getKey() + ": " + entry.getValue().toString());
        }
    }
}