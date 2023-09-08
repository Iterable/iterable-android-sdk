package com.iterable.androidsdk;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Message;
import android.os.Messenger;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.util.concurrent.Callable;

public class CardView extends androidx.cardview.widget.CardView {

    private androidx.cardview.widget.CardView cardView;
    private TextView titleText;
    private TextView descriptionText;
    private Button primaryButton;
    private Button secondaryButton;
    private ImageView image;

    private Callable<Void> callable;

    public CardView(Context context) {
        super(context);
        init();
    }

    public CardView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CardView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.card_view, this);
        cardView = findViewById(R.id.card_view);
        titleText = findViewById(R.id.titleText);
        descriptionText = findViewById(R.id.descriptionText);
        primaryButton = findViewById(R.id.primaryButton);
        secondaryButton = findViewById(R.id.secondaryButton);
        image = findViewById(R.id.image);
        this.updateValues();
    }

    public void updateValues() {

        // Retrieve the passed values from the intent
        Intent intent = ((Activity) getContext()).getIntent();

        //card config
        cardView.setRadius(intent.getIntExtra("cardRadius", 4));
        cardView.setCardBackgroundColor(intent.getIntExtra("cardBackgroundColor", Color.parseColor("#ffffff")));

        //title of the card
        titleText.setText(intent.getStringExtra("titleText"));
        titleText.setTextSize(intent.getIntExtra("titleTextSize", 20));
        titleText.setTextColor(intent.getIntExtra("titleTextColor", Color.parseColor("#000000")));

        //description of the card
        descriptionText.setText(intent.getStringExtra("descriptionText"));
        descriptionText.setTextSize(intent.getIntExtra("descriptionTextSize", 16));
        descriptionText.setTextColor(intent.getIntExtra("descriptionTextColor", Color.parseColor("#000000")));

        //primary button of the card
        primaryButton.setText(intent.getStringExtra("primaryButtonText"));
        primaryButton.setTextColor(intent.getIntExtra("primaryButtonTextColor", Color.parseColor("#000000")));

        //secondary button of the card
        secondaryButton.setText(intent.getStringExtra("secondaryButtonText"));
        secondaryButton.setTextColor(intent.getIntExtra("secondaryButtonTextColor", Color.parseColor("#000000")));

        //secondary button visibility
        if (intent.getBooleanExtra("secondaryButtonVisible", true)) {
            secondaryButton.setVisibility(View.VISIBLE);
        } else {
            secondaryButton.setVisibility(View.GONE);
        }

        //card image
        if (!intent.getStringExtra("imageUrl").isEmpty()) {
            Picasso.get().load(intent.getStringExtra("imageUrl")).into(image);
        }

        primaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the messenger from the intent
                Messenger messenger = intent.getParcelableExtra("messenger");

                // Send a message to the main activity
                Message message = new Message();
                message.what = 1;
                message.obj = "Primary Button clicked";
                try {
                    messenger.send(message);
                } catch (Exception e) {
                    // Handle the exception
                }
            }
        });

        secondaryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Get the messenger from the intent
                Messenger messenger = intent.getParcelableExtra("messenger");

                // Send a message to the main activity
                Message message = new Message();
                message.what = 1;
                message.obj = "Secondary Button clicked";
                try {
                    messenger.send(message);
                } catch (Exception e) {
                    // Handle the exception
                }
            }
        });


    }
}
