package rikka.shizuku.service.util;

public class Unsafe {
    @SuppressWarnings("unchecked")
    public static <T> T unsafeCast(Object object) {
        return (T) object;
    }
}
