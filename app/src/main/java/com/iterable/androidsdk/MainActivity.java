package com.iterable.androidsdk;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.iterable.iterableapi.CommerceItem;
import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.testapp.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //Below api key is used to display merge user feature
        IterableApi.initialize(this, "289895aa038648ee9e4ce60bd0a46e9c");

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        findViewById(R.id.mainLayout).setOnLongClickListener(v -> {
            Intent intent = new Intent(this, AnonTrackingTestActivity.class);
            startActivity(intent);
            return true;
        });

        findViewById(R.id.btn_track_event).setOnClickListener(v -> IterableApi.getInstance().track("Browse Mocha"));

        findViewById(R.id.btn_update_cart).setOnClickListener(v -> {
            List<CommerceItem> items = new ArrayList<>();
            items.add(new CommerceItem("123", "Mocha", 1, 1));
            IterableApi.getInstance().updateCart(items);
        });

        findViewById(R.id.btn_buy_mocha).setOnClickListener(v -> {
            List<CommerceItem> items = new ArrayList<>();
            items.add(new CommerceItem("456", "Black Coffee", 2, 1));
            IterableApi.getInstance().trackPurchase(4, items);
        });

        findViewById(R.id.btn_buy_coffee).setOnClickListener(v -> {
            List<CommerceItem> items = new ArrayList<>();
            items.add(new CommerceItem("456", "Black Coffee", 5, 1));
            IterableApi.getInstance().trackPurchase(5, items);
        });

        findViewById(R.id.btn_set_user).setOnClickListener(v -> IterableApi.getInstance().setUserId("hani7"));

        findViewById(R.id.btn_update_user).setOnClickListener(v -> {
            try {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("firstName", "Hani");
                IterableApi.getInstance().updateUser(jsonObject);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
        });

        findViewById(R.id.btn_logout).setOnClickListener(view -> IterableApi.getInstance().setUserId(null));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
