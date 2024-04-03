package com.iterable.androidsdk;

import android.os.Build;
import android.os.Bundle;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

import com.iterable.iterableapi.IterableApi;
import com.iterable.iterableapi.IterableAuthHandler;
import com.iterable.iterableapi.IterableConfig;
import com.iterable.iterableapi.RetryPolicy;
import com.iterable.iterableapi.testapp.R;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Duration;
import java.util.Objects;

@RequiresApi(api = Build.VERSION_CODES.O)
public class MainActivity extends AppCompatActivity implements IterableAuthHandler {

    private static final String secret = "34992609011249b410db9e1a568db9b65063c73e618bdb0229a674aeed7db7fba1bdc06e9b42d021120b9c88f795a734c18ab88ff7b6ecbccc50a945899d3666";
    private static final String email = "harrymash2006@gmail.com";
    private static final String userId = "harrymash2006";
    private static final String apiKey = "4236278428294f04be8443007d3daf89";
    private static final Duration days7 = Duration.ofDays(7);
    private static final Duration days366 = Duration.ofDays(366);
    private static final int issuedAt = 1516239022;
    private static final int expiration = 1516239023;
    private static final String payload = String.format(
            "{ \"email\": \"%s\", \"iat\": %d, \"exp\": %d }",
            email, issuedAt, expiration
    );


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
        IterableConfig config = new IterableConfig.Builder()
                .setAuthHandler(this)
                .setMaxRetries(4)
                .setRetryBackoff(RetryPolicy.LINEAR)
                .setRetryInterval(2L)
                .build();

        IterableApi.initialize(this, apiKey, config);

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
        findViewById(R.id.btn_pauseAuthRetry).setOnClickListener(view -> IterableApi.getInstance().pauseAuthRetries(true));
        findViewById(R.id.btn_resumeAuthRetry).setOnClickListener(view -> IterableApi.getInstance().pauseAuthRetries(false));
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

    @Override
    public String onAuthTokenRequested() {
        String token = IterableJWTGenerator.generateToken(secret, days366, null, "hani7");
        Log.i("jwt token::", token);
        return token;
    }

    @Override
    public void onTokenRegistrationSuccessful(String authToken) {
        Log.i("success:", "token registration successful.");
    }

    @Override
    public void onTokenRegistrationFailed(Throwable object) {
        Log.e("failed:", Objects.requireNonNull(object.getMessage()));
    }
}
