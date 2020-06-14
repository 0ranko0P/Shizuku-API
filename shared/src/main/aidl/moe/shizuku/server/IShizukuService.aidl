package moe.shizuku.server;

import moe.shizuku.server.IRemoteProcess;

interface IShizukuService {

    void exit() = 100;

    int getVersion() = 2;

    int getUid() = 3;

    int checkPermission(String permission) = 4;

    IRemoteProcess newProcess(in String[] cmd, in String[] env, in String dir) = 7;

    String getSELinuxContext() = 8;
}
