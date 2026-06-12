package com.iterable.iterableapi.ui.inbox;

/**
 * Implement on the host Activity or parent Fragment of {@link IterableInboxFragment}
 * to handle back-navigation taps from the opt-in inbox toolbar.
 *
 * <p>Relevant when toolbar option is {@code InboxToolbarOption.WithBackButton} or a
 * {@code InboxToolbarOption.Custom} layout includes a view with id
 * {@code @id/iterable_reserved_inbox_toolbar_action}. If no host implements this
 * interface, the fragment falls back to the host activity's
 * {@code OnBackPressedDispatcher}.
 *
 * <p>The listener is discovered during {@code onAttach()}, so it survives process
 * death - recreated fragments re-bind to the restored host automatically.
 */
public interface IterableInboxToolbarBackListener {
    void onInboxToolbarBackClick();
}
