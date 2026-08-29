package com.attendai.core.notification.entity;

/** Notification delivery channel. Stored as VARCHAR — not a DB ENUM type. */
public enum Channel {
    EMAIL,
    IN_APP,
    PUSH
}
