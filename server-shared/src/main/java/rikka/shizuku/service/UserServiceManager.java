package rikka.shizuku.service;

import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_COMPONENT;
import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_DEBUGGABLE;
import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_PROCESS_NAME;
import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_TAG;
import static rikka.shizuku.ShizukuApiConstants.USER_SERVICE_ARG_VERSION_CODE;

import android.content.ComponentName;
import android.content.pm.PackageInfo;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.ArrayMap;

import java.io.OutputStream;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Executor;

import moe.shizuku.server.IShizukuServiceConnection;
import rikka.shizuku.ShizukuApiConstants;
import rikka.shizuku.service.api.SystemService;
import rikka.shizuku.service.util.Logger;
import rikka.shizuku.service.util.UserHandleCompat;

public abstract class UserServiceManager {

    private static final Logger LOGGER = new Logger("UserServiceManager");

    private final Executor executor;
    private final Map<String, UserServiceRecord> userServiceRecords = Collections.synchronizedMap(new ArrayMap<>());

    public UserServiceManager(Executor executor) {
        this.executor = executor;
    }

    public PackageInfo ensureCallingPackageForUserService(String packageName, int appId, int userId) {
        PackageInfo packageInfo = SystemService.getPackageInfoNoThrow(packageName, 0x00002000 /*PackageManager.MATCH_UNINSTALLED_PACKAGES*/, userId);
        if (packageInfo == null || packageInfo.applicationInfo == null) {
            throw new SecurityException("unable to find package " + packageName);
        }

        if (UserHandleCompat.getAppId(packageInfo.applicationInfo.uid) != appId) {
            throw new SecurityException("package " + packageName + " is not owned by " + appId);
        }
        return packageInfo;
    }

    public int removeUserService(IShizukuServiceConnection conn, Bundle options) {
        ComponentName componentName = Objects.requireNonNull(options.getParcelable(USER_SERVICE_ARG_COMPONENT), "component is null");

        int uid = Binder.getCallingUid();
        int appId = UserHandleCompat.getAppId(uid);
        int userId = UserHandleCompat.getUserId(uid);

        String packageName = componentName.getPackageName();
        ensureCallingPackageForUserService(packageName, appId, userId);

        String className = Objects.requireNonNull(componentName.getClassName(), "class is null");
        String tag = options.getString(USER_SERVICE_ARG_TAG);
        String key = packageName + ":" + (tag != null ? tag : className);

        synchronized (this) {
            UserServiceRecord record = getUserServiceRecordLocked(key);
            if (record == null) return 1;
            removeUserServiceLocked(record);
        }
        return 0;
    }

    private void removeUserServiceLocked(UserServiceRecord record) {
        if (userServiceRecords.values().remove(record)) {
            record.destroy();
            onUserServiceRecordRemoved(record);
        }
    }

    public int addUserService(IShizukuServiceConnection conn, Bundle options) {
        Objects.requireNonNull(conn, "connection is null");
        Objects.requireNonNull(options, "options is null");

        int uid = Binder.getCallingUid();
        int appId = UserHandleCompat.getAppId(uid);
        int userId = UserHandleCompat.getUserId(uid);

        ComponentName componentName = Objects.requireNonNull(options.getParcelable(USER_SERVICE_ARG_COMPONENT), "component is null");
        String packageName = Objects.requireNonNull(componentName.getPackageName(), "package is null");
        PackageInfo packageInfo = ensureCallingPackageForUserService(packageName, appId, userId);

        String className = Objects.requireNonNull(componentName.getClassName(), "class is null");
        String sourceDir = Objects.requireNonNull(packageInfo.applicationInfo.sourceDir, "apk path is null");

        int versionCode = options.getInt(USER_SERVICE_ARG_VERSION_CODE, 1);
        String tag = options.getString(USER_SERVICE_ARG_TAG);
        String processNameSuffix = options.getString(USER_SERVICE_ARG_PROCESS_NAME);
        boolean debug = options.getBoolean(USER_SERVICE_ARG_DEBUGGABLE, false);
        String key = packageName + ":" + (tag != null ? tag : className);

        synchronized (this) {
            UserServiceRecord record = getOrCreateUserServiceRecordLocked(key, versionCode, sourceDir);
            record.callbacks.register(conn);

            if (record.service != null && record.service.pingBinder()) {
                record.broadcastBinderReceived();
            } else {
                Runnable runnable = () -> startUserService(record, key, record.token, packageName, className, processNameSuffix, uid, debug);
                executor.execute(runnable);
            }
            return 0;
        }
    }

    private UserServiceRecord getUserServiceRecordLocked(String key) {
        return userServiceRecords.get(key);
    }

    private UserServiceRecord getOrCreateUserServiceRecordLocked(String key, int versionCode, String apkPath) {
        UserServiceRecord record = getUserServiceRecordLocked(key);
        if (record != null) {
            if (record.versionCode != versionCode) {
                LOGGER.v("Remove service record %s (%s) because version code not matched (old=%d, new=%d)", key, record.token, record.versionCode, versionCode);
            } else if (record.service == null || !record.service.pingBinder()) {
                LOGGER.v("Service in record %s (%s) is dead", key, record.token);
            } else {
                LOGGER.i("Found existing service record %s (%s)", key, record.token);
                return record;
            }

            removeUserServiceLocked(record);
        }

        record = new UserServiceRecord(versionCode) {

            @Override
            public void removeSelf() {
                synchronized (UserServiceManager.this) {
                    removeUserServiceLocked(this);
                }
            }
        };

        onUserServiceRecordCreated(record, apkPath);

        userServiceRecords.put(key, record);
        LOGGER.i("New service record %s (%s): version=%d, apk=%s", key, record.token, versionCode, apkPath);
        return record;
    }

    private void startUserService(UserServiceRecord record, String key, String token, String packageName, String classname, String processNameSuffix, int callingUid, boolean debug) {
        LOGGER.v("Starting process for service record %s (%s)...", key, token);

        String cmd = getUserServiceStartCmd(record, key, token, packageName, classname, processNameSuffix, callingUid, debug);
        int exitCode;
        try {
            java.lang.Process process = Runtime.getRuntime().exec("sh");
            OutputStream os = process.getOutputStream();
            os.write(cmd.getBytes());
            os.flush();
            os.close();

            exitCode = process.waitFor();
        } catch (Throwable e) {
            throw new IllegalStateException(e.getMessage());
        }
        if (exitCode != 0) {
            throw new IllegalStateException("sh exited with " + exitCode);
        }
    }

    public abstract String getUserServiceStartCmd(UserServiceRecord record, String key, String token, String packageName, String classname, String processNameSuffix, int callingUid, boolean debug);

    private void sendUserServiceLocked(IBinder binder, String token) {
        Map.Entry<String, UserServiceRecord> entry = null;
        for (Map.Entry<String, UserServiceRecord> e : userServiceRecords.entrySet()) {
            if (e.getValue().token.equals(token)) {
                entry = e;
                break;
            }
        }

        if (entry == null) {
            throw new IllegalArgumentException("unable to find token " + token);
        }

        LOGGER.v("Received binder for service record %s", token);

        UserServiceRecord record = entry.getValue();
        record.setBinder(binder);
    }

    public void attachUserService(IBinder binder, Bundle options) {
        Objects.requireNonNull(binder, "binder is null");
        String token = Objects.requireNonNull(options.getString(ShizukuApiConstants.USER_SERVICE_ARG_TOKEN), "token is null");

        synchronized (this) {
            sendUserServiceLocked(binder, token);
        }
    }

    public void onUserServiceRecordCreated(UserServiceRecord record, String apkPath) {

    }

    public void onUserServiceRecordRemoved(UserServiceRecord record) {

    }

}
