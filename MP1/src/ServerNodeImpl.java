import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;


public class ServerNodeImpl extends ServerNode {
    private ServerSocket serverSocket;

    public ServerNodeImpl(String nodeName, String ip, int port, String configFilePath) throws IOException {
        super(nodeName, ip, port, 0, configFilePath);
        serverSocket = new ServerSocket(port);
        // load config file to group
        try {
            setGroup(Utils.loadConfig(configFilePath));
        } catch (Exception e) {
            e.printStackTrace();
        }
        this.getGroup().put(nodeName, this);
    }

    public ServerNodeImpl(String nodeName, String ip, int port) {
        super(nodeName, ip, port);
    }

    /**
     * send message to all nodes
     */
    @Override
    public void send(Message message) {
//        for (ServerNode node : this.getGroup().values()) {
//            String ip = node.getIp();
//            int port = node.getPort();
//            sendMsg(message, ip, port);
//        }
    }

    /**
     * socket listen on the given port, get the message, go to different paths due to the state of message
     * <p>
     * message.isReturned == false : the server receive the message for the first time
     * message.isReturned() && !message.isDeliverable() : the server receive the message it multicast
     * message.isDeliverable() : receive the final message
     */
    @Override
    public void run() {
        System.out.println("start listening ...");
        while (true) {
            try {
                Socket server = serverSocket.accept();
                DataInputStream in = new DataInputStream(server.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
                String input = reader.readLine();
                long len = input.length();
                if (input.startsWith("fail")) {
                    System.out.println("node " + input.split(" ")[1] + " failed");
                    deleteNode(input.split(" ")[1]);
                    if (getGroup().size() == 0) {
                        System.out.println("all other nodes failed");
                        break;
                    }
                    continue;
                }
                Message message = Utils.parseInput(input);
                message.setLen(len);
                if (!getGroup().containsKey(message.getSenderNodeId())) continue;
                if (!message.isReturned()) { // receive the msg for the first time
//                    System.out.println("receive first:" + message.toString());

                    long receiveTime = System.currentTimeMillis() % 10000000;
                    message.setReceiveTime(receiveTime);
                    receiveFirst(message);
                } else if (message.isReturned() && !message.isDeliverable()) { // receive the back message
//                    System.out.println("receive back:" + message.toString());

                    receiveBack(message);
                } else if (message.isDeliverable()) { // receive the final message
//                    System.out.println("receive final:" + message.toString());

                    receiveFinal(message);
                }
                server.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private synchronized void deleteNode(String nodeId) {
        if (!getGroup().containsKey(nodeId)) return;
        this.getGroup().remove(nodeId);
//        System.out.println("delete " + nodeId);
//        System.out.println(getGroup().keySet());
        for (Integer msgId : this.getReplyMsgs().keySet()) {
            List<Message> messages = getReplyMsgs().get(msgId);
            messages.removeIf(msg -> msg.getReceiverNodeId().equals(nodeId));
            getReplyMsgs().put(msgId, messages);
        }
        for (String key : this.getMessages().keySet()) {
            if (getMessages().get(key).getSenderNodeId().equals(nodeId)) {
                getMessages().remove(key);
            }
        }
//        System.out.println("delete " + nodeId + " messasge:");
//        for (String s : getMessages().keySet()) {
//            System.out.println(getMessages().get(s));
//        }
    }

    /**
     * receive the message for the first time, find a priority id > prev priority id,
     * save the message, mark it as undeliverable, return the priority to the sender
     */
    @Override
    public synchronized void receiveFirst(Message message) {
        // priority id ++
        int priorityId = this.getPriorityId() + 1;
        this.setPriorityId(priorityId);
        message.setDeliverable(false);
        message.setPriority(priorityId + ":" + this.getNodeName());
        message.setPriorityId(priorityId);
        message.setReturned(true);
        // save the message
        this.getMessages().put(message.getSenderNodeId() + ":" + message.getMsgId(), message);
        // send back the msg to sender
        sendBack(message);
    }

    /**
     * send the msg back to its sender
     */
    @Override
    public synchronized void sendBack(Message message) {
        // get the sender ip and port
        String senderNodeId = message.getSenderNodeId();
        if (this.getGroup().containsKey(senderNodeId)) {
            String ip = this.getGroup().get(senderNodeId).getIp();
            int port = this.getGroup().get(senderNodeId).getPort();
            sendMsg(message, ip, port, senderNodeId);
        }
//        System.out.println("send back message: " + message.toString());
    }

    /**
     * receive the returned msg from other servers, put the msg in replyMsgs
     * if receive msgs from all servers, sort the msg by priority, get the largest msg, multicast to other nodes
     * remove prev msg in messages, add the max msg to messages
     *
     * @param message returned msg
     */
    @Override
    public synchronized void receiveBack(Message message) {
        List<Message> messages = getReplyMsgs().getOrDefault(message.getMsgId(), new ArrayList<>());
        messages.add(message);
        getReplyMsgs().put(message.getMsgId(), messages);
        for (Integer msgId : getReplyMsgs().keySet()) {
            List<Message> messageList = getReplyMsgs().get(msgId);
//            System.out.println("check message list size:" + messageList.size() + ", msg id :" + msgId + ", group size:" + getGroup().size());
            // if collect all the return message
            if (messageList.size() >= this.getGroup().size()) {
                sortMessages(messageList);
//                System.out.println("receive all back msgs, sort result: ");
//                Utils.printMsgList(messageList);
                // get max msg
                Message maxMsg = messageList.get(messageList.size() - 1);
                maxMsg.setDeliverable(true);
                // remove the msg before, add max msg
                this.getMessages().remove(message.getSenderNodeId() + ":" + message.getMsgId());
                this.getMessages().put(message.getSenderNodeId() + ":" + message.getMsgId(), maxMsg);
                this.getReplyMsgs().remove(msgId);
                // multicast to other nodes
                sendFinal(maxMsg);
            }
        }

    }

    /**
     * use tcp connection to send message to other server
     *
     * @param message message item
     * @param ip      receiver ip
     * @param port    receiver port
     */
    private synchronized void sendMsg(Message message, String ip, int port, String nodeId) {
        try {
            Socket client = new Socket(ip, port);
            if (!this.getGroup().containsKey(nodeId)) return;
            String msg = message.toString();
            OutputStream outputStream = client.getOutputStream();
            outputStream.write(msg.getBytes());
            client.close();
        } catch (IOException e) {
            System.out.println("connection failed");
//            this.getGroup().remove(nodeId);
        }
    }

    /**
     * check if connection failed
     */
    public Boolean isConnectionClose(Socket socket) {
        try {
            socket.sendUrgentData(0);
            return false;
        } catch (Exception se) {
            return true;
        }
    }

    /**
     * send final msg to all other nodes
     */
    @Override
    public synchronized void sendFinal(Message message) {
//        System.out.println("send final msg: " + message.toString());
        for (ServerNode node : this.getGroup().values()) {
            sendMsg(message, node.getIp(), node.getPort(), node.getNodeName());
        }
    }

    /**
     * receive the final msg, remove prev msg, add the final msg to messages
     * sort the messageList, if the first msg is deliverable, deliver it and remove it from messages
     *
     * @param message the final msg
     */
    @Override
    public synchronized void receiveFinal(Message message) {
        // update the msg
        this.getMessages().remove(message.getSenderNodeId() + ":" + message.getMsgId());
        this.getMessages().put(message.getSenderNodeId() + ":" + message.getMsgId(), message);
        if (message.getPriorityId() > this.getPriorityId()) {
            this.setPriorityId(message.getPriorityId());
        }
        List<Message> messages = new ArrayList<>(this.getMessages().values());
        // sort the received msgs
        sortMessages(messages);

//        System.out.println("receive the final msg and sort list : ");
//        Utils.printMsgList(messages);

        // if the smallest msg is deliverable deliver it and remove it
        if (messages.get(0).isDeliverable()) {
            Message firstMsg = messages.remove(0);
            deliver(firstMsg);
            this.getMessages().remove(firstMsg.getSenderNodeId() + ":" + firstMsg.getMsgId());
        }
    }

    /**
     * sort msg list by priority
     */
    private synchronized void sortMessages(List<Message> messages) {
        messages.sort((a, b) -> {
            String pA = a.getPriority(), pB = b.getPriority();
            String[] splitA = pA.split(":"), splitB = pB.split(":");
            int priorityA = Integer.parseInt(splitA[0]), priorityB = Integer.parseInt(splitB[0]);
            String nodeA = splitA[1], nodeB = splitB[1];
            return priorityA == priorityB ? nodeA.compareTo(nodeB) : priorityA - priorityB;
        });
    }

    /**
     * deliver the message
     */
    @Override
    public synchronized void deliver(Message message) {
        String content = message.getContent();
        long deliverTime = System.currentTimeMillis() % 10000000;
        message.setDeliverTime(deliverTime);
        String[] split = content.split(" ");
        if (split[0].equals(Constant.DEPOSIT)) {
            String name = split[1];
            int amount = Integer.parseInt(split[2]);
            getAccountMap().putIfAbsent(name, new Account(name, 0));
            Account account = getAccountMap().get(name);
            account.deposit(amount);
            getAccountMap().put(name, account);
        } else if (split[0].equals(Constant.TRANSFER)) {
            String name1 = split[1], name2 = split[3];
            getAccountMap().putIfAbsent(name1, new Account(name1, 0));
            getAccountMap().putIfAbsent(name2, new Account(name2, 0));
            int amount = Integer.parseInt(split[4]);
            if (amount <= this.getAccountMap().get(name1).getMoney()) {
                Account account1 = getAccountMap().get(name1);
                Account account2 = getAccountMap().get(name2);
                account1.transfer(amount);
                account2.deposit(amount);
                getAccountMap().put(name1, account1);
                getAccountMap().put(name2, account2);
            }
        }
        Utils.printBalance(getAccountMap());
        long SendTime = message.getSendTime();
        long receiveTime = message.getReceiveTime();
        long len = message.getLen();
        int messageId = message.getMsgId();
        String recerverId = this.getNodeName();

        String nodeid = message.getSenderNodeId();
        String path = nodeid + ".csv";
        String data = recerverId + "," + messageId + "," + len + "," + SendTime + "," + receiveTime + "," + deliverTime;
//        System.out.println(data);
        writeData(path, data);
    }


    public synchronized void writeData(String path, String data) {
        File file = new File(path);
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(file, true));
            bw.write(data);
            bw.newLine();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
