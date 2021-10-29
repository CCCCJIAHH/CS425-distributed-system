import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;

public class ServerRun {
    public static void main(String[] args) throws UnknownHostException {
        String nodeId = args[0];
        int port = Integer.parseInt(args[1]);
        String configFilePath = args[2];
        InetAddress addr = InetAddress.getLocalHost();
        try {
            Thread t = new ServerNodeImpl(nodeId, addr.getHostAddress(), port, configFilePath);
            t.start();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
