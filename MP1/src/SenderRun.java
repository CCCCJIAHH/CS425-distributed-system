import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class SenderRun {
    public static void main(String[] args) throws UnknownHostException {
        String nodeId = args[0];
        int port = Integer.parseInt(args[1]);
        String configFilePath = args[2];
        InetAddress addr = InetAddress.getLocalHost();
        try {
            Map<String, ServerNode> groups = Utils.loadConfig(configFilePath);
            groups.put(nodeId, new ServerNodeImpl(nodeId, addr.getHostAddress(), port));
            Thread sender = new Sender(groups, nodeId, addr.getHostAddress(), port, 1);
            sender.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
