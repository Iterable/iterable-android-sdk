package com.iterable.iterableapi.ui.inbox

import androidx.annotation.LayoutRes
import java.io.Serializable

/**
 * Controls how [IterableInboxToolbarView] renders. Passed as a fragment argument or
 * intent extra on [IterableInboxFragment] / [IterableInboxActivity].
 */
sealed interface InboxToolbarOption : Serializable {

    /** No toolbar. The fragment renders identically to prior SDK versions. */
    data object None : InboxToolbarOption {
        private fun readResolve(): Any = None
    }

    /** A title-only toolbar above the inbox list. */
    data object Default : InboxToolbarOption {
        private fun readResolve(): Any = Default
    }

    /** A toolbar with the configured title plus a back navigation icon. */
    data object WithBackButton : InboxToolbarOption {
        private fun readResolve(): Any = WithBackButton
    }

    /**
     * Inflates a fully custom toolbar layout supplied by the integrator. The integrator
     * owns all wiring for their own views (menus, clicks, icons, etc.).
     *
     * Reserved opt-in ids - the SDK looks up these ids via `findViewById` and, if
     * present, auto-wires them. The names are deliberately namespaced; do not reuse
     * them on unrelated views in the custom layout. Omitting either id keeps the SDK
     * from touching that view.
     *
     *   - `@id/iterable_reserved_inbox_toolbar_action` - auto-wired to the SDK's default
     *     back handler. Override the action by implementing
     *     [IterableInboxToolbarBackListener] on the host Activity or parent Fragment.
     *   - `@id/iterable_reserved_inbox_toolbar_title` - if the view is a `TextView`, the
     *     SDK sets its text to the `toolbarTitle` argument (or the default "Inbox"
     *     string when null).
     */
    data class Custom(@LayoutRes val layoutRes: Int) : InboxToolbarOption
}
