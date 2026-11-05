package com.iterable.iterableapi;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Data region determining which data center and endpoints the SDK sends data to.
 * Defaults to {@link #US}; EU-hosted projects must select {@link #EU}.
 */
public enum IterableDataRegion {
    US(0, "US", "https://api.iterable.com/api/"),
    EU(1, "EU", "https://api.eu.iterable.com/api/");

    private static final String TAG = "IterableDataRegion";

    private final int code;
    private final String regionCode;
    private final String endpoint;

    IterableDataRegion(int code, String regionCode, String endpoint) {
        this.code = code;
        this.regionCode = regionCode;
        this.endpoint = endpoint;
    }

    public String getEndpoint() {
        return this.endpoint;
    }

    /**
     * Stable numeric identifier for this region, matching the values used by the React Native and
     * Flutter SDKs. Unlike {@link #ordinal()} this is guaranteed not to shift if regions are added.
     */
    public int getCode() {
        return this.code;
    }

    /**
     * Stable short identifier for this region (for example {@code "EU"}), matching the values used
     * by Iterable's other SDKs. It currently matches {@link #name()}, but is stored separately for
     * the same reason {@link #getCode()} does not use {@link #ordinal()}: renaming an enum constant
     * must not change an identifier that wrappers and persisted configuration already rely on.
     */
    @NonNull
    public String getRegionCode() {
        return this.regionCode;
    }

    /**
     * Resolves a region from its short identifier (e.g. {@code "EU"}), accepting either case. Also
     * accepts a full API endpoint URL, so values from the iOS SDK's string-based data region can be
     * passed through unchanged.
     * <p>
     * Unrecognised values fall back to {@link #US} and are logged as an error, matching the
     * behaviour of Iterable's other SDKs. A null or blank value also falls back to {@link #US},
     * which is the documented default rather than a misconfiguration.
     *
     * @param value region identifier, or {@code null}
     * @return the matching region, or {@link #US} if the value is not recognised
     */
    @NonNull
    public static IterableDataRegion from(@Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            IterableLogger.w(TAG, "No data region specified, defaulting to " + US.regionCode);
            return US;
        }

        String normalized = value.trim();
        for (IterableDataRegion region : values()) {
            if (normalized.equalsIgnoreCase(region.regionCode) || normalized.equalsIgnoreCase(region.endpoint)) {
                return region;
            }
        }

        // Error level, not warning: this runs while the config is still being built, and
        // IterableLogger reads the pre-init config (ERROR) until initialize() swaps it in.
        IterableLogger.e(TAG, "Unsupported data region \"" + value + "\", defaulting to " + US.regionCode
                + ". Supported values: " + supportedRegionCodes());
        return US;
    }

    /**
     * Resolves a region from its numeric identifier, as used by the React Native and Flutter SDKs.
     * <p>
     * Unrecognised values fall back to {@link #US} and are logged as an error, matching the
     * behaviour of Iterable's other SDKs.
     *
     * @param code region identifier, see {@link #getCode()}
     * @return the matching region, or {@link #US} if the code is not recognised
     */
    @NonNull
    public static IterableDataRegion from(int code) {
        for (IterableDataRegion region : values()) {
            if (region.code == code) {
                return region;
            }
        }

        IterableLogger.e(TAG, "Unsupported data region code " + code + ", defaulting to " + US.regionCode
                + ". Supported values: " + supportedRegionCodes());
        return US;
    }

    private static String supportedRegionCodes() {
        StringBuilder builder = new StringBuilder();
        for (IterableDataRegion region : values()) {
            if (builder.length() > 0) {
                builder.append(", ");
            }
            builder.append(region.regionCode).append(" (").append(region.code).append(")");
        }
        return builder.toString();
    }
}
