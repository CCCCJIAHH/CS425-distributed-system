import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws UnknownHostException {
        String nodeId = args[0];
        int port = Integer.parseInt(args[1]);
        String configFilePath = args[2];
        InetAddress addr = InetAddress.getLocalHost();
        try {
            Thread t = new ServerNodeImpl(nodeId, addr.getHostAddress(), port, configFilePath);
            t.start();
            Map<String, ServerNode> groups = Utils.loadConfig(configFilePath);
            Thread.sleep(10000);
            groups.put(nodeId, new ServerNodeImpl(nodeId, addr.getHostAddress(), port));
            Thread sender = new Sender(groups, nodeId, addr.getHostAddress(), port, 1);
            sender.start();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
