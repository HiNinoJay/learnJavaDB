package top.nino.mydb.common;

/**
 * @Author：zengzhj
 * @Date：2023/11/15 16:49
 */
public class Error {

    /**
     * 文件相关异常
     */
    public static final Exception FILE_NOT_EXISTS_EXCEPTION = new RuntimeException("File does not exists!");
    public static final Exception FILE_EXISTS_EXCEPTION = new RuntimeException("File already exists!");
    public static final Exception FILE_CANNOT_RW_EXCEPTION = new RuntimeException("File cannot read or write!");

    /**
     * TM
     */
    public static final Exception BAD_XID_FILE_EXCEPTION = new RuntimeException("Bad XID file!");
}
