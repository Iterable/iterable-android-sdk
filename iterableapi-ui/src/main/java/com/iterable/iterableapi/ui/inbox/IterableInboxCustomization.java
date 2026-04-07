package com.iterable.iterableapi.ui.inbox;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Configuration class for customizing the inbox toolbar/header appearance and behavior.
 * <p>
 * Use the {@link Builder} to create an instance with the desired customization options.
 * <p>
 * Example usage:
 * <pre>
 * IterableInboxCustomization customization = new IterableInboxCustomization.Builder()
 *     .setTitle("My Inbox")
 *     .setTitleCenterAligned(true)
 *     .setShowCloseButton(true)
 *     .setCloseButtonListener(v -&gt; finish())
 *     .build();
 * </pre>
 */
public class IterableInboxCustomization {

    @Nullable
    private final String title;

    private final boolean titleCenterAligned;

    private final boolean showCloseButton;

    @Nullable
    private final View.OnClickListener closeButtonListener;

    private IterableInboxCustomization(@NonNull Builder builder) {
        this.title = builder.title;
        this.titleCenterAligned = builder.titleCenterAligned;
        this.showCloseButton = builder.showCloseButton;
        this.closeButtonListener = builder.closeButtonListener;
    }

    /**
     * @return The toolbar title, or null to use the default
     */
    @Nullable
    public String getTitle() {
        return title;
    }

    /**
     * @return Whether the title should be center-aligned
     */
    public boolean isTitleCenterAligned() {
        return titleCenterAligned;
    }

    /**
     * @return Whether the close button should be shown
     */
    public boolean isShowCloseButton() {
        return showCloseButton;
    }

    /**
     * @return The click listener for the close button, or null for default behavior
     */
    @Nullable
    public View.OnClickListener getCloseButtonListener() {
        return closeButtonListener;
    }

    /**
     * Builder for creating {@link IterableInboxCustomization} instances.
     */
    public static class Builder {
        @Nullable
        private String title;
        private boolean titleCenterAligned = false;
        private boolean showCloseButton = false;
        @Nullable
        private View.OnClickListener closeButtonListener;

        /**
         * Set the toolbar title text.
         *
         * @param title The title to display
         * @return This builder for chaining
         */
        @NonNull
        public Builder setTitle(@Nullable String title) {
            this.title = title;
            return this;
        }

        /**
         * Set whether the title should be center-aligned in the toolbar.
         *
         * @param centerAligned true to center-align the title
         * @return This builder for chaining
         */
        @NonNull
        public Builder setTitleCenterAligned(boolean centerAligned) {
            this.titleCenterAligned = centerAligned;
            return this;
        }

        /**
         * Set whether a close button (X) should be shown in the toolbar.
         *
         * @param showCloseButton true to show the close button
         * @return This builder for chaining
         */
        @NonNull
        public Builder setShowCloseButton(boolean showCloseButton) {
            this.showCloseButton = showCloseButton;
            return this;
        }

        /**
         * Set a custom click listener for the close button.
         * If not set and the close button is shown, clicking it will finish the activity.
         *
         * @param listener The click listener
         * @return This builder for chaining
         */
        @NonNull
        public Builder setCloseButtonListener(@Nullable View.OnClickListener listener) {
            this.closeButtonListener = listener;
            return this;
        }

        /**
         * Build the customization instance.
         *
         * @return A new {@link IterableInboxCustomization} instance
         */
        @NonNull
        public IterableInboxCustomization build() {
            return new IterableInboxCustomization(this);
        }
    }
}
