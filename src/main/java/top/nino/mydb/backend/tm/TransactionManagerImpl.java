package top.nino.mydb.backend.tm;

import top.nino.mydb.backend.utils.Panic;
import top.nino.mydb.backend.utils.Parser;
import top.nino.mydb.common.Error;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @Author：zengzhj
 * @Date：2023/11/15 16:34
 */
public class TransactionManagerImpl implements TransactionManager{

    /**
     * XID文件的后缀名
     */
    public static final String XID_SUFFIX = ".xid";


    /**
     * 该XID文件的头8个字节 用来记录管理事务的个数
     */
    public static final int LEN_XID_HEADER_LENGTH = 8;

    /**
     * 每个事务的占用长度为一个字节：存储该事务的状态
     */
    private static final int XID_FIELD_SIZE = 1;

    /**
     * 默认超级事务的xid为0，永远为committed状态
     * 所以 获取其他xid事务的状态 的公式为 (xid-1)+8字节处
     */
    public static final long SUPER_XID = 0;

    /**
     * 事务的三种状态
     * 0-激活 1-已提交 2-丢弃
     */
    private static final byte FIELD_TRAN_ACTIVE = 0;
    private static final byte FIELD_TRAN_COMMITTED = 1;
    private static final byte FIELD_TRAN_ABORTED = 2;

    //--------------------------------------------------//

    private RandomAccessFile randomAccessFile;
    private FileChannel fileChannel;
    private long xidCounter;
    private Lock counterLock;


    public TransactionManagerImpl(RandomAccessFile randomAccessFile, FileChannel fileChannel) {
        this.randomAccessFile = randomAccessFile;
        this.fileChannel = fileChannel;
        this.counterLock = new ReentrantLock();
        checkXIDCounter();
    }

    /**
     * 检查XID文件是否合法
     * 读取XID_FILE_HEADER中的xidCounter,根据它计算文件的理论长度，对比实际长度
     */
    private void checkXIDCounter() {
        long readFileLength = 0;
        try {
            readFileLength = randomAccessFile.length();
        } catch (IOException e) {
            Panic.panic(Error.BAD_XID_FILE_EXCEPTION);
        }

        // 如果头部都没有8个子节空间（事务数量记录） 说明该XID不合法
        if(readFileLength < LEN_XID_HEADER_LENGTH) {
            Panic.panic(Error.BAD_XID_FILE_EXCEPTION);
        }

        // 将存储的数量 读出来
        ByteBuffer byteBuffer = ByteBuffer.allocate(LEN_XID_HEADER_LENGTH);
        try {
            fileChannel.position(0);
            fileChannel.read(byteBuffer);
        } catch (IOException e) {
            Panic.panic(e);
        }

        // 读出来过后，算理论上 该XID该有多大，再和真实长度 作比较
        this.xidCounter = Parser.parseLong(byteBuffer.array());
        long end = getXidPosition(this.xidCounter + 1);
        if(end != readFileLength) {
            Panic.panic(Error.BAD_XID_FILE_EXCEPTION);
        }
    }

    /**
     * 获得 xid 事务的 在文件中 存储数据的开始位置
     * @param xid
     * @return
     */
    private long getXidPosition(long xid) {
        return LEN_XID_HEADER_LENGTH + (xid - 1) * XID_FIELD_SIZE;
    }


    @Override
    public long begin() {
        counterLock.lock();
        try {
            long xid = this.xidCounter + 1;
            updateXID(xid, FIELD_TRAN_ACTIVE);
            incrXIDCounter();
            return xid;
        } finally {
            counterLock.unlock();
        }
    }

    private void incrXIDCounter() {
        this.xidCounter++;
        ByteBuffer byteBuffer = ByteBuffer.wrap(Parser.long2Byte(xidCounter));
        try {
            fileChannel.position(0);
            fileChannel.write(byteBuffer);
        } catch (IOException e) {
            Panic.panic(e);
        }

        try {
            fileChannel.force(false);
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    private void updateXID(long xid, byte status) {
        long offset = getXidPosition(xid);
        byte[] temp = new byte[XID_FIELD_SIZE];
        temp[0] = status;

        ByteBuffer byteBuffer = ByteBuffer.wrap(temp);
        try {
            fileChannel.position(offset);
            fileChannel.write(byteBuffer);
        } catch (IOException e) {
            Panic.panic(e);
        }

        try {
            fileChannel.force(false);
        } catch (IOException e) {
            Panic.panic(e);
        }
    }

    /**
     * 检测XID事务是否处于status状态
     * @param xid
     * @param status
     * @return
     */
    private boolean checkXID(long xid, byte status) {
        long offset = getXidPosition(xid);
        ByteBuffer byteBuffer = ByteBuffer.wrap(new byte[XID_FIELD_SIZE]);

        try {
            fileChannel.position(offset);
            fileChannel.read(byteBuffer);
        } catch (IOException e) {
            Panic.panic(e);
        }
        return byteBuffer.array()[0] == status;
    }

    @Override
    public void commit(long xid) {
        updateXID(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public void abort(long xid) {
        updateXID(xid, FIELD_TRAN_ABORTED);
    }

    @Override
    public boolean isActive(long xid) {
        return checkXID(xid, FIELD_TRAN_ACTIVE);
    }

    @Override
    public boolean isCommitted(long xid) {
        return checkXID(xid, FIELD_TRAN_COMMITTED);
    }

    @Override
    public boolean isAborted(long xid) {
        return checkXID(xid, FIELD_TRAN_ABORTED);
    }

    @Override
    public void close() {
        try {
            fileChannel.close();
            randomAccessFile.close();
        } catch (IOException e) {
            Panic.panic(e);
        }
    }
}
