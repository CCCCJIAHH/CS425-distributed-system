import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

public class Utils {
    /**
     * read config file, load nodes config to group map
     *
     * @param filePath config file path
     */
    public static ConcurrentHashMap<String, ServerNode> loadConfig(String filePath) throws IOException {
        ConcurrentHashMap<String, ServerNode> group = new ConcurrentHashMap<>();
        FileInputStream fin = new FileInputStream(filePath);
        InputStreamReader reader = new InputStreamReader(fin);
        BufferedReader buffReader = new BufferedReader(reader);
        String strTmp = buffReader.readLine();
        int nodeNum = Integer.parseInt(strTmp);
        while ((strTmp = buffReader.readLine()) != null) {
            String[] split = strTmp.split(" ");
            ServerNode node = new ServerNodeImpl(split[0], split[1], Integer.parseInt(split[2]));
            group.put(node.getNodeName(), node);
        }
        if (nodeNum != group.size()) throw new IOException();
        buffReader.close();
        return group;
    }


    /**
     * parse input string to Message object
     */
    public static Message parseInput(String input) {
        String[] split = input.split(",");
        return new Message(split[0], split[1], split[2], Integer.parseInt(split[3]),
                Boolean.parseBoolean(split[4]), Boolean.parseBoolean(split[5]), split[6], Integer.parseInt(split[7]),Long.parseLong(split[8]), Long.parseLong(split[9]),Long.parseLong(split[10]),Integer.parseInt(split[11]));
    }

    public static void printBalance(TreeMap<String, Account> map) {
        StringBuilder builder = new StringBuilder();
        builder.append("BALANCES ");
        for (Account account : map.values()) {
            builder.append(account.getName()).append(":").append(account.getMoney()).append(" ");
        }
        System.out.println(builder);
    }

    public static void printMsgList(List<Message> messages) {
        System.out.println("======================================================");
        for (Message message : messages) {
            System.out.println(message.toString());
        }
        System.out.println("======================================================");
    }
}
