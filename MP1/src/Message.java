import java.util.Comparator;
import java.util.Objects;

public class Message {
    private String content;
    private String senderNodeId;
    private String receiverNodeId;
    private int MsgId;
    private boolean deliverable;
    private boolean isReturned;
    private int priorityId;
    private String priority; // priorityId : senderNodeId, compare priorityId first then compare senderNodeId
    private long sendTime;
    private long receiveTime;
    private long deliverTime;
    private long len;



    public Message() {
    }

    public Message(String content, String senderNodeId, String receiverNodeId, int msgId, boolean deliverable, boolean isReturned, String priority, int priorityId, long sendTime, long receiveTime, long deliverTime,long len){
        this.content = content;
        this.senderNodeId = senderNodeId;
        this.receiverNodeId = receiverNodeId;
        this.MsgId = msgId;
        this.deliverable = deliverable;
        this.isReturned = isReturned;
        this.priority = priority;
        this.priorityId = priorityId;
        this.sendTime = sendTime;
        this.receiveTime=receiveTime;
        this.deliverTime=deliverTime;
        this.len=len;
    }

    public int getPriorityId() {
        return priorityId;
    }

    public void setPriorityId(int priorityId) {
        this.priorityId = priorityId;
    }

    public String getReceiverNodeId() {
        return receiverNodeId;
    }

    public void setReceiverNodeId(String receiverNodeId) {
        this.receiverNodeId = receiverNodeId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSenderNodeId() {
        return senderNodeId;
    }

    public void setSenderNodeId(String senderNodeId) {
        this.senderNodeId = senderNodeId;
    }

    public int getMsgId() {
        return MsgId;
    }

    public void setMsgId(int msgId) {
        MsgId = msgId;
    }

    public boolean isDeliverable() {
        return deliverable;
    }

    public void setDeliverable(boolean deliverable) {
        this.deliverable = deliverable;
    }

    public boolean isReturned() {
        return isReturned;
    }

    public void setReturned(boolean returned) {
        isReturned = returned;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void setSendTime(long sendTime) {this.sendTime = sendTime;}

    public long getSendTime() { return sendTime;}

    public void setReceiveTime(long receiveTime) {this.receiveTime=receiveTime;}

    public long getReceiveTime(){return receiveTime;}

    public void setDeliverTime(long deliverTime){this.deliverTime=deliverTime;}

    public  long getDeliverTime(){return  deliverTime;}

    public long getLen() {
        return len;
    }
    public void setLen(long len) {
        this.len = len;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return senderNodeId.equals(message.senderNodeId) &&
                MsgId == message.MsgId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(senderNodeId, MsgId);
    }

    @Override
    public String toString() {
        return content + "," + senderNodeId +
                "," + receiverNodeId +
                "," + MsgId +
                "," + deliverable +
                "," + isReturned +
                "," + priority +
                "," + priorityId +
                "," + sendTime +
                "," + receiveTime +
                "," + deliverTime +
                "," + len;
    }
}
