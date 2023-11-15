package top.nino.mydb.backend.tm;

import top.nino.mydb.backend.utils.Panic;
import top.nino.mydb.common.Error;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

/**
 * 事务管理
 * 三种事务状态：激活、已提交、已丢弃
 * 行为包含：开启事务，提交事务，丢弃事务，关闭事务管理器
 * 定义了一个.xid文件，头八个字节代表 事务的数量，随后每个事务分配一个字节用来存储状态，0代表超级事务
 *
 * @Author：zengzhj
 * @Date：2023/11/15 16:02
 */
public interface TransactionManager {

    /**
     * 开启事务
     * @return 返回该事务被分配的xid
     */
    long begin();

    /**
     * 提交事务
     * @param xid 该事务的 xid
     */
    void commit(long xid);

    /**
     * 丢弃事务das
     * @param xid 该事务的xid
     */
    void abort(long xid);

    /**
     * 判断当前事务是该状态
     * @param xid 该事务的xid
     * @return
     */
    boolean isActive(long xid);
    boolean isCommitted(long xid);
    boolean isAborted(long xid);

    /**
     * 关闭该事务管理器, 该XID文件流也会被关闭
     */
    void close();

    /**
     * 根据path位置创建一个xid文件，并且返回事务管理器
     * @param path
     * @return
     */
    static TransactionManagerImpl create(String path) {
        File file = new File(path + TransactionManagerImpl.XID_SUFFIX);

        try {
            if(!file.createNewFile()) {
                Panic.panic(Error.FILE_EXISTS_EXCEPTION);
            }
        } catch (Exception e) {
            Panic.panic(e);
        }

        // 必须保证文件 可读可写
        if(!file.canRead() || !file.canWrite()) {
            Panic.panic(Error.FILE_CANNOT_RW_EXCEPTION);
        }

        FileChannel fileChannel = null;
        RandomAccessFile randomAccessFile = null;

        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileChannel = randomAccessFile.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }

        // 写空XID文件头8个字节
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[TransactionManagerImpl.LEN_XID_HEADER_LENGTH]);

        try {
            fileChannel.position(0);
            fileChannel.write(byteBuffer);
        } catch (IOException e) {
            Panic.panic(e);
        }
        return new TransactionManagerImpl(randomAccessFile, fileChannel);
    }


    /**
     * 根据path位置 打开一个xid文件，并且返回事务管理器
     * @param path
     * @return
     */
    static TransactionManagerImpl open(String path) {
        File file = new File(path + TransactionManagerImpl.XID_SUFFIX);
        if(!file.exists()) {
            Panic.panic(Error.FILE_NOT_EXISTS_EXCEPTION);
        }

        if(!file.canRead() || !file.canWrite()) {
            Panic.panic(Error.FILE_CANNOT_RW_EXCEPTION);
        }

        RandomAccessFile randomAccessFile = null;
        FileChannel fileChannel = null;

        try {
            randomAccessFile = new RandomAccessFile(file, "rw");
            fileChannel = randomAccessFile.getChannel();
        } catch (FileNotFoundException e) {
            Panic.panic(e);
        }
        return new TransactionManagerImpl(randomAccessFile, fileChannel);
    }
}
