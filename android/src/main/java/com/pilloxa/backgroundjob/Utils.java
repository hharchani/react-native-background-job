package com.pilloxa.backgroundjob;

import android.app.ActivityManager;
import android.content.Context;
import android.support.annotation.NonNull;
import com.facebook.react.ReactNativeHost;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.common.LifecycleState;

public class Utils {
  private Utils() {
    //no instance
  }

  /** Check whether on not the React Native application is in foreground. */
  public static boolean isReactNativeAppInForeground(@NonNull ReactNativeHost reactNativeHost) {
    if (!reactNativeHost.hasInstance()) {
      // If the app was force-stopped the instace will be destroyed. The instance can't be created from a background thread.
      return false;
    }
    ReactContext reactContext = reactNativeHost.getReactInstanceManager().getCurrentReactContext();
    return reactContext != null && reactContext.getLifecycleState() == LifecycleState.RESUMED;
  }

  public static boolean isMyServiceRunning(Class<?> serviceClass, Context context) {
    ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
    for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
      if (serviceClass.getName().equals(service.service.getClassName())) {
        return true;
      }
    }
    return false;
  }
}

