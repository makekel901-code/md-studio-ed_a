package com.amd.mdstudio.legacy;

import android.app.Activity;
import android.app.Application;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;

/** Helper class for MD Studio diagnostic event reporting. */
public final class Diagnostics extends ContentProvider {
    private static volatile Context appContext;
    private static volatile Uri destination;
    private static boolean handlerInstalled;
    private static long activatedAt;
    private static final String EXTRA = "com.pocketbuilder.runtime.DIAGNOSTICS_URI";

    @Override
    public boolean onCreate() {
        Context context = getContext();
        if (context != null) {
            appContext = context.getApplicationContext();
            if (appContext instanceof Application) {
                ((Application) appContext).registerActivityLifecycleCallbacks(new Application.ActivityLifecycleCallbacks() {
                    @Override public void onActivityPreCreated(Activity activity, Bundle state) { activate(activity); }
                    @Override public void onActivityCreated(Activity activity, Bundle state) { activate(activity); lifecycle("created", activity); }
                    @Override public void onActivityStarted(Activity activity) { lifecycle("started", activity); }
                    @Override public void onActivityResumed(Activity activity) { lifecycle("resumed", activity); }
                    @Override public void onActivityPaused(Activity activity) { lifecycle("paused", activity); }
                    @Override public void onActivityStopped(Activity activity) { lifecycle("stopped", activity); }
                    @Override public void onActivitySaveInstanceState(Activity activity, Bundle state) {}
                    @Override public void onActivityDestroyed(Activity activity) { lifecycle("destroyed", activity); }
                });
            }
        }
        return true;
    }

    private static synchronized void activate(Activity activity) {
        if (activity == null || activity.getIntent() == null) return;
        String raw = activity.getIntent().getStringExtra(EXTRA);
        if (raw == null) {
            if (android.content.Intent.ACTION_MAIN.equals(activity.getIntent().getAction())) destination = null;
            return;
        }
        Uri uri = Uri.parse(raw);
        if (!"content".equals(uri.getScheme()) || !"com.pocketbuilder.diagnostics".equals(uri.getAuthority())) return;
        if (uri.equals(destination)) return;
        destination = uri;
        activatedAt = System.currentTimeMillis();
        if (!handlerInstalled) {
            final Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
            Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread thread, Throwable error) {
                    try { report("crash", trace(error)); }
                    finally {
                        if (previous != null) previous.uncaughtException(thread, error);
                        else { android.os.Process.killProcess(android.os.Process.myPid()); System.exit(10); }
                    }
                }
            });
            handlerInstalled = true;
        }
        report("event", "Diagnostic test session activated (helper v1)");
    }

    private static void lifecycle(String event, Activity activity) {
        report("lifecycle", activity.getClass().getName() + " " + event);
    }

    public static void event(String message) { report("event", message); }
    public static void error(Throwable error) { report("event", trace(error)); }

    private static void report(String type, String message) {
        if (destination == null || appContext == null) return;
        try {
            ContentValues values = new ContentValues();
            values.put("type", type);
            values.put("message", message);
            values.put("timestamp", System.currentTimeMillis());
            appContext.getContentResolver().insert(destination, values);
        } catch (Exception ignored) {}
    }

    private static String trace(Throwable error) {
        if (error == null) return "";
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.PrintWriter pw = new java.io.PrintWriter(sw);
        error.printStackTrace(pw);
        return sw.toString();
    }

    @Override public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) { return null; }
    @Override public String getType(Uri uri) { return null; }
    @Override public Uri insert(Uri uri, ContentValues values) { return null; }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
