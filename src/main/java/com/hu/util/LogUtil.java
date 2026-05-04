package com.hu.util;

/**
 * 日志打印工具
 */
public class LogUtil {
    /**
     * 用于打印标题信息，方便日志查看
     * @param title 标题
     */
    public static void logTitle(String title) {
        System.out.printf("-------------------- %s ---------------------\n", title);
    }
}
