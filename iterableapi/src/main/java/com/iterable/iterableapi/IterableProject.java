package com.iterable.iterableapi;

import androidx.annotation.NonNull;

/**
 * An Iterable project to run the SDK against: a Mobile API key together with the
 * {@link IterableConfig} that belongs with it.
 *
 * The two halves are paired in one object on purpose. {@link IterableConfig} carries the data region
 * and the {@link IterableAuthHandler}, so supplying a key and a config separately allows one
 * project's key to be paired with another project's region or auth handler. That mismatch produces an
 * authentication failure, or a request sent to the wrong region, that looks unrelated to the project
 * it came from. Requiring the pair makes it unrepresentable.
 *
 * Build one per project or region, for example:
 *
 * <pre>{@code
 * IterableProject euProject = new IterableProject(euApiKey, new IterableConfig.Builder()
 *         .setAuthHandler(new RegionAuthHandler(Region.EU))
 *         .setDataRegion(IterableDataRegion.EU)
 *         .build());
 * }</pre>
 *
 * Instances are immutable and safe to hold for the lifetime of the app, because
 * {@link IterableConfig}'s fields are final. Note that the config holds strong references to your
 * handlers, so a long-lived project keeps its handlers alive.
 *
 * @see IterableApi#switchProject(android.content.Context, IterableProject, IterableProjectSwitchCallback)
 */
public final class IterableProject {
    private final String apiKey;
    private final IterableConfig config;

    /**
     * @param apiKey the project's Iterable Mobile API key. Must not be null, empty, or whitespace
     *               only. Validated here rather than at the call site so an unusable project cannot
     *               be constructed: a blank key would otherwise tear down the project the SDK is on
     *               and leave it initialized against nothing.
     * @param config the configuration to run this project with. Must not be null. Pass
     *               {@code new IterableConfig.Builder().build()} for defaults.
     * @throws IllegalArgumentException if {@code apiKey} is blank or either argument is null
     */
    public IterableProject(@NonNull String apiKey, @NonNull IterableConfig config) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("IterableProject: apiKey must not be null or blank");
        }
        if (config == null) {
            throw new IllegalArgumentException("IterableProject: config must not be null. Pass "
                    + "new IterableConfig.Builder().build() for defaults.");
        }
        this.apiKey = apiKey;
        this.config = config;
    }

    @NonNull
    public String getApiKey() {
        return apiKey;
    }

    @NonNull
    public IterableConfig getConfig() {
        return config;
    }

    /** Masks the key, so a project can be logged without leaking it. */
    @NonNull
    @Override
    public String toString() {
        return "IterableProject{apiKey=" + apiKey.charAt(0) + "***, dataRegion="
                + config.dataRegion + "}";
    }
}
