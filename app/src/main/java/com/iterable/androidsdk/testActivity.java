package com.iterable.androidsdk;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.iterable.iterableapi.CommerceItem;
import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.IterableConfig;
import com.iterable.iterableapi.testapp.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class testActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);
        getLocalData();
        IterableConfig iterableConfig = new IterableConfig.Builder().setEnableAnonTracking(true).build();
        IterableApi.initialize(this, "18845050c4774b7c9dc48beece2f6d8b", iterableConfig);

        ((Button) findViewById(R.id.updateCart)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText updateCart_edit = (EditText) findViewById(R.id.updateCart_edit);
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
                    updateCart_edit.setText("");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                getLocalData();
            }
        });
        ((Button) findViewById(R.id.trackPurchase)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText purchase_items = (EditText) findViewById(R.id.trackPurchase_edit);
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
                    purchase_items.setText("");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                getLocalData();
            }
        });
        ((Button) findViewById(R.id.customEvent)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText customEvent_edit = (EditText) findViewById(R.id.customEvent_edit);
                if(customEvent_edit == null) return;
                Log.d("TEST_USER", String.valueOf(customEvent_edit.getText()));

                try {
                  JSONObject customEventItem = new JSONObject(String.valueOf(customEvent_edit.getText()));
                  JSONObject items = new JSONObject(customEventItem.get("dataFields").toString());
                  if(customEventItem.has("eventName")) {
                      items.put("eventName", customEventItem.getString("eventName"));
                  }
                  IterableApi.getInstance().track("customEvent", 0, 0, items);
                  customEvent_edit.setText("");
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }

                getLocalData();
            }
        });
        ((Button) findViewById(R.id.updateUser)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText updateUser_edit = (EditText) findViewById(R.id.updateUser_edit);
                if(updateUser_edit == null) return;
                Log.d("TEST_USER", String.valueOf(updateUser_edit.getText()));

                try {
                    JSONObject updateUserItem = new JSONObject(String.valueOf(updateUser_edit.getText()));
                    if(updateUserItem.has("dataFields")) {
                        Log.d("TEST_USER", "udpate_user"+ String.valueOf(updateUserItem.getJSONObject("dataFields")));
                    IterableApi.getInstance().updateUser(updateUserItem.getJSONObject("dataFields"));
                    updateUser_edit.setText("");
                    }
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
                getLocalData();
            }
        });
        ((Button) findViewById(R.id.setUser)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText setUser_edit = (EditText) findViewById(R.id.setUser_edit);
                if(setUser_edit == null) return;

                IterableApi.getInstance().setUserId(String.valueOf(setUser_edit.getText()));
                setUser_edit.setText("");
                getLocalData();

            }
        });
        ((Button) findViewById(R.id.setEmail)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText setEmail_edit = (EditText) findViewById(R.id.setEmail_edit);
                if(setEmail_edit == null) return;

                IterableApi.getInstance().setEmail(String.valueOf(setEmail_edit.getText()));
                setEmail_edit.setText("");
                getLocalData();

            }
        });

        ((Button) findViewById(R.id.btn_logout)).setOnClickListener(view -> {
            IterableApi.getInstance().setUserId(null);
            getLocalData();
        });

    }
    public void getLocalData() {
        // Name of the SharedPreferences file
//        String SHARED_PREFS_FILE = "iterable-encrypted-shared-preferences";
        String SHARED_PREFS_FILE = "com.iterable.iterableapi";

        // Get the SharedPreferences object
        SharedPreferences sharedPreferences = getApplicationContext().getSharedPreferences(SHARED_PREFS_FILE, Context.MODE_PRIVATE);

        // Retrieve all key-value pairs
        Map<String, ?> allEntries = sharedPreferences.getAll();

        // Log all entries
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            Log.d("SharedPreferences_main", entry.getKey() + ": " + entry.getValue().toString());
        }
    }
}