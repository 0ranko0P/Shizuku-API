package rikka.shizuku;

import androidx.annotation.RestrictTo;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP_PREFIX;

public class ShizukuApiConstants {

    public static final int SERVER_VERSION = 11;

    // binder
    public static final String BINDER_DESCRIPTOR = "moe.shizuku.server.IShizukuService";
    public static final int BINDER_TRANSACTION_transact = 1;

    // user service
    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final int USER_SERVICE_TRANSACTION_destroy = 16777115;

    public static final String USER_SERVICE_ARG_TAG = "shizuku:user-service-arg-tag";
    public static final String USER_SERVICE_ARG_COMPONENT = "shizuku:user-service-arg-component";
    public static final String USER_SERVICE_ARG_DEBUGGABLE = "shizuku:user-service-arg-debuggable";
    public static final String USER_SERVICE_ARG_VERSION_CODE = "shizuku:user-service-arg-version-code";
    public static final String USER_SERVICE_ARG_PROCESS_NAME = "shizuku:user-service-arg-process-name";

    @RestrictTo(LIBRARY_GROUP_PREFIX)
    public static final String USER_SERVICE_ARG_TOKEN = "shizuku:user-service-arg-token";

    // attach client
    public static final String ATTACH_REPLY_SERVER_VERSION = "shizuku:attach-reply-version";
    public static final String ATTACH_REPLY_SERVER_UID = "shizuku:attach-reply-uid";
    public static final String ATTACH_REPLY_SERVER_SECONTEXT = "shizuku:attach-reply-secontext";
    public static final String ATTACH_REPLY_PERMISSION_GRANTED = "shizuku:attach-reply-permission-granted";
    public static final String ATTACH_REPLY_SHOULD_SHOW_REQUEST_PERMISSION_RATIONALE = "shizuku:attach-reply-should-show-request-permission-rationale";

    // request permission
    public static final String REQUEST_PERMISSION_REPLY_ALLOWED = "shizuku:request-permission-reply-allowed";
    public static final String REQUEST_PERMISSION_REPLY_IS_ONETIME = "shizuku:request-permission-reply-is-onetime";

}
