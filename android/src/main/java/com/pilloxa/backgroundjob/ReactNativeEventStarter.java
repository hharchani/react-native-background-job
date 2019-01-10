package com.pilloxa.backgroundjob;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;

import com.facebook.react.HeadlessJsTaskService;
import com.facebook.react.ReactApplication;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.jstasks.HeadlessJsTaskConfig;

import javax.annotation.Nullable;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

class ReactNativeEventStarter {
  private static final String LOG_TAG = ReactNativeEventStarter.class.getSimpleName();
  private final ReactNativeHost reactNativeHost;
  private final Context context;
  private static final String CONTEXT_TITLE_SETTING = "CONTEXT_TITLE_SETTING";
  private static final String CONTEXT_TEXT_SETTING = "CONTEXT_TEXT_SETTING";
  private static final String SETTINGS_KEY = "Background_Job_Settings";

  ReactNativeEventStarter(@NonNull Context context) {
    this.context = context;
    reactNativeHost = ((ReactApplication) context.getApplicationContext()).getReactNativeHost();
  }

  public void trigger(@NonNull Bundle jobBundle) {
    Log.d(LOG_TAG, "trigger() called with: jobBundle = [" + jobBundle + "]");
    boolean appInForeground = Utils.isReactNativeAppInForeground(reactNativeHost);
    boolean allowExecutionInForeground = jobBundle.getBoolean("allowExecutionInForeground", false);
    SharedPreferences.Editor editor = this.context.getSharedPreferences(SETTINGS_KEY, Context.MODE_PRIVATE).edit();
    editor.putString(CONTEXT_TEXT_SETTING, jobBundle.getString("notificationText"));
    editor.putString(CONTEXT_TITLE_SETTING, jobBundle.getString("notificationTitle"));
    editor.apply();
    if (!appInForeground || allowExecutionInForeground) {
      // Will execute if the app is in background, or in forground but it has permision to do so
      MyHeadlessJsTaskService.start(context, jobBundle);
    }
  }

  public static class MyHeadlessJsTaskService extends HeadlessJsTaskService {
    private static final String LOG_TAG = MyHeadlessJsTaskService.class.getSimpleName();

    @Override
    @SuppressLint("WrongConstant")
    public void onCreate() {
      super.onCreate();
      Context mContext = this.getApplicationContext();
      SharedPreferences preferences = mContext.getSharedPreferences(SETTINGS_KEY, MODE_PRIVATE);
      String contextTitle = preferences.getString(CONTEXT_TITLE_SETTING, "Running in background...");
      String contextText = preferences.getString(CONTEXT_TEXT_SETTING, "Background job");
      String CHANNEL_ID = "Background job";
      Notification notification = null;
      NotificationManager notificationManager =
        (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

      MyHeadlessJsTaskService jsTaskService = new MyHeadlessJsTaskService();

      Intent intent = new Intent();
      PendingIntent pendingIntent = PendingIntent.getService(mContext, 0, intent, 0);

      if (Utils.isMyServiceRunning(jsTaskService.getClass(), mContext)) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
          NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_ID, NotificationManager.IMPORTANCE_LOW);
          ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).createNotificationChannel(channel);
          notification = new Notification.Builder(mContext, CHANNEL_ID)
            .setContentTitle(contextTitle)
            .setContentText(contextText)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setTicker(contextTitle)
            .build();
        } else {
          notification = new Notification.Builder(mContext)
            .setContentTitle(contextTitle)
            .setContentText(contextText)
            .setSmallIcon(R.mipmap.ic_launcher_round)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setTicker(contextTitle)
            .build();
        }
        notification.flags = notification.flags
          | Notification.FLAG_ONGOING_EVENT;
        notification.flags |= Notification.FLAG_AUTO_CANCEL;

        notificationManager.notify(1, notification);
//        startForeground(1, notification);
      } else {
        stopForeground(true);
        notificationManager.cancel(1);
      }
    }

    @Nullable
    @Override
    protected HeadlessJsTaskConfig getTaskConfig(Intent intent) {
      Log.d(LOG_TAG, "getTaskConfig() called with: intent = [" + intent + "]");
      Bundle extras = intent.getExtras();
      boolean allowExecutionInForeground = extras.getBoolean("allowExecutionInForeground", false);
      long timeout = extras.getLong("timeout", 2000);
      // For task with quick execution period additional check is required
      ReactNativeHost reactNativeHost =
        ((ReactApplication) getApplicationContext()).getReactNativeHost();
      boolean appInForeground = Utils.isReactNativeAppInForeground(reactNativeHost);
      if (appInForeground && !allowExecutionInForeground) {
        return null;
      }
      return new HeadlessJsTaskConfig(intent.getStringExtra("jobKey"), Arguments.fromBundle(extras),
        timeout, allowExecutionInForeground);
    }

    public static void start(Context context, Bundle jobBundle) {
      Log.d(LOG_TAG,
        "start() called with: context = [" + context + "], jobBundle = [" + jobBundle + "]");
      Intent starter = new Intent(context, MyHeadlessJsTaskService.class);
      starter.putExtras(jobBundle);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        context.startForegroundService(starter);
      } else {
        context.startService(starter);
      }
    }
  }
}

