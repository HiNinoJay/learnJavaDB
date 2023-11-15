package top.nino.mydb.backend.utils;

/**
 * @Author：zengzhj
 * @Date：2023/11/15 16:47
 */
public class Panic {

    /**
     * 导致系统直接退出的情况
     * @param err
     */
    public static void panic(Exception err) {
        err.printStackTrace();
        System.exit(1);
    }
}
