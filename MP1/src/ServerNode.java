import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public abstract class ServerNode extends Thread {
    private String nodeName;
    private String ip;
    private int port;
    private int MsgId; // message id that is multicasted by self
    private int priorityId; // the max priority so far
    private ConcurrentHashMap<String, ServerNode> group; // all nodes in system, <NodeName, Node>
    private ConcurrentHashMap<String, Message> messages; // all messages received from other nodes <key, Message>
    private ConcurrentHashMap<Integer, List<Message>> replyMsgs; // messages returned from other nodes
    private TreeMap<String, Account> accountMap;

    public ServerNode(String nodeName, String ip, int port, int msgId, String configFilePath) {
        this.nodeName = nodeName;
        this.ip = ip;
        this.port = port;
        this.MsgId = 0;
        this.priorityId = 0;
        this.messages = new ConcurrentHashMap<>();
        this.replyMsgs = new ConcurrentHashMap<>();
        this.accountMap = new TreeMap<>();
    }

    public ServerNode(String nodeName, String ip, int port) {
        this.nodeName = nodeName;
        this.ip = ip;
        this.port = port;
    }

    public ConcurrentHashMap<Integer, List<Message>> getReplyMsgs() {
        return replyMsgs;
    }

    public TreeMap<String, Account> getAccountMap() {
        return accountMap;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getMsgId() {
        return MsgId;
    }

    public void setMsgId(int msgId) {
        MsgId = msgId;
    }

    public int getPriorityId() {
        return priorityId;
    }

    public void setPriorityId(int priorityId) {
        this.priorityId = priorityId;
    }

    public ConcurrentHashMap<String, ServerNode> getGroup() {
        return group;
    }

    public void setGroup(ConcurrentHashMap<String, ServerNode> group) {
        this.group = group;
    }

    public Map<String, Message> getMessages() {
        return messages;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ServerNode that = (ServerNode) o;
        return Objects.equals(nodeName, that.nodeName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(nodeName);
    }

    abstract public void send(Message message);

    abstract public void receiveFirst(Message message);

    abstract public void sendBack(Message message);

    abstract public void receiveBack(Message message);

    abstract public void sendFinal(Message message);

    abstract public void receiveFinal(Message message);

    abstract public void deliver(Message message);
}
