package com.google.devrel.training.conference;

import java.text.SimpleDateFormat;

import com.google.api.server.spi.Constant;

/**
 * Contains the client IDs and scopes for allowed clients consuming the conference API.
 */
public class Constants {
    public static final String WEB_CLIENT_ID = "894248967662-4qcv22pcf1f5ksiac7t9srjnimd3engl.apps.googleusercontent.com";
    public static final String ANDROID_CLIENT_ID = "894248967662-01bcbl0eavmjq6duc9q7tuaffn9ocutc.apps.googleusercontent.com";
    public static final String IOS_CLIENT_ID = "replace this with your iOS client ID";
    public static final String ANDROID_AUDIENCE = WEB_CLIENT_ID;
    public static final String EMAIL_SCOPE = Constant.API_EMAIL_SCOPE;
    public static final String API_EXPLORER_CLIENT_ID = Constant.API_EXPLORER_CLIENT_ID;
    public static final String MEMCACHE_ANNOUNCEMENTS_KEY = "RECENT_ANNOUNCEMENTS";
    public static final String MEMCACHE_FEATUREDSPEAKER_KEY = "RECENT_FEATUREDSPEAKER";
    public static final String TIME_FORMAT = "HH:mm";   
    public static final String TIME_FORMAT_PATTERN = "\\d{1,2}:\\d{2}";
}