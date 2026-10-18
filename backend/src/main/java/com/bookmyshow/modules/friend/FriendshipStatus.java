package com.bookmyshow.modules.friend;

/** Friendship states. Stored as text in friendships.status. */
public final class FriendshipStatus {

    public static final String PENDING = "PENDING";
    public static final String ACCEPTED = "ACCEPTED";
    public static final String DECLINED = "DECLINED";
    public static final String REMOVED = "REMOVED";

    private FriendshipStatus() {}
}
