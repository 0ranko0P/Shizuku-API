package rikka.shizuku.server;

import static rikka.shizuku.ShizukuApiConstants.REQUEST_PERMISSION_REPLY_ALLOWED;

import android.os.Bundle;

import moe.shizuku.server.IShizukuApplication;
import rikka.shizuku.server.util.Logger;

public class ClientRecord {

    private static final Logger LOGGER = new Logger("ClientRecord");

    public final int uid;
    public final int pid;
    public final IShizukuApplication client;
    public final String packageName;
    public boolean allowed;

    public ClientRecord(int uid, int pid, IShizukuApplication client, String packageName) {
        this.uid = uid;
        this.pid = pid;
        this.client = client;
        this.packageName = packageName;
        this.allowed = false;
    }

    public void dispatchRequestPermissionResult(int requestCode, boolean allowed) {
        Bundle reply = new Bundle();
        reply.putBoolean(REQUEST_PERMISSION_REPLY_ALLOWED, allowed);
        try {
            client.dispatchRequestPermissionResult(requestCode, reply);
        } catch (Throwable e) {
            LOGGER.w(e, "dispatchRequestPermissionResult failed for client (uid=%d, pid=%d, package=%s)", uid, pid, packageName);
        }
    }
}
