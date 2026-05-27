package com.firstclub.membership.domain;

/**
 * Lifecycle status of a {@link Subscription}.
 *
 * <ul>
 *   <li>{@link #ACTIVE} - subscription is currently in effect.</li>
 *   <li>{@link #CANCELED} - subscription was explicitly canceled by the user; terminal state.</li>
 *   <li>{@link #EXPIRED} - subscription's billing period has ended; computed at read time
 *       when {@code endAt} is at or before the current instant.</li>
 * </ul>
 *
 * <p>Validates: Requirements 4.1, 5.3.
 */
public enum SubscriptionStatus {

    /** Subscription is currently active. */
    ACTIVE,

    /** Subscription was canceled by the user. Terminal. */
    CANCELED,

    /** Subscription's period has ended. Read-time projection. */
    EXPIRED
}
