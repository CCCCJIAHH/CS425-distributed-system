import java.util.HashSet;

public class ServerObject {
    private int value;
    private boolean readLock;
    private boolean writeLock;
    protected HashSet<String> readLockOwner;
    protected String writeLockOwner;

    ServerObject(int value) {
        this.value = value;
        this.readLock = false;
        this.writeLock = false;
        this.readLockOwner = new HashSet<>();
        this.writeLockOwner = null;
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public boolean getReadLock() {
        return this.readLock;
    }

    public void setReadLock(boolean flag) {
        this.readLock = flag;
    }

    public boolean getWriteLock() {
        return this.writeLock;
    }

    public void setWriteLock(boolean flag) {
        this.writeLock = flag;
    }
}
