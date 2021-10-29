import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Map;
import java.util.Scanner;

public class Sender extends Thread {
    private Map<String, ServerNode> group;
    private final String nodeName;
    private final String ip;
    private final int port;
    private int msgId; // message id that is multicasted by self

    public Sender(Map<String, ServerNode> group, String nodeName, String ip, int port, int msgId) {
        this.group = group;
        this.nodeName = nodeName;
        this.ip = ip;
        this.port = port;
        this.msgId = msgId;
    }

    public Map<String, ServerNode> getGroup() {
        return group;
    }

    public void setGroup(Map<String, ServerNode> group) {
        this.group = group;
    }

    @Override
    public void run() {
        System.out.println("start sending msg ...");
        Scanner scanner = new Scanner(System.in);
        while (true) {
            try {
                // read the output of python script, multicast the message to group
                if (scanner.hasNextLine()) {
                    String content = scanner.nextLine();
                    System.out.println("scanner read:" + content);
                    multicast(content);
                }
            } catch (Exception e) {
                e.printStackTrace();
                break;
            }
        }
    }

    public void multicast(String content) {
        for (ServerNode node : this.getGroup().values()) {
            String ip = node.getIp();
            int port = node.getPort();
            long sendTime = System.currentTimeMillis()%10000000;
            System.out.println(sendTime + "");
            Message message = new Message(content, nodeName, node.getNodeName(), msgId, false, false, "", 0, sendTime, 0, 0, 0);
            sendMsg(message, ip, port);
        }
        msgId++;
    }

    private void sendMsg(Message message, String ip, int port) {
        try {
            Socket client = new Socket(ip, port);
            String msg = message.toString();
            System.out.println("sender send msg : " + msg);
            OutputStream outputStream = client.getOutputStream();
            outputStream.write(msg.getBytes());
            client.close();
        } catch (IOException e) {
            System.out.println("connection failed: " + message.getReceiverNodeId());
            this.getGroup().remove(message.getReceiverNodeId());
            multiCastFailure(message.getReceiverNodeId());
        }
    }

    private void multiCastFailure(String receiverNodeId) {
        for (ServerNode node : getGroup().values()) {
            try {
                Socket client = new Socket(node.getIp(), node.getPort());
                OutputStream outputStream = client.getOutputStream();
                String msg = "fail " + receiverNodeId;
                outputStream.write(msg.getBytes());
                client.close();
            } catch (IOException e) {
                this.getGroup().remove(node.getNodeName());
                multiCastFailure(node.getNodeName());
            }
        }
    }
}
