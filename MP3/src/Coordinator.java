import java.io.BufferedReader;
import java.io.FileReader;
import java.net.UnknownHostException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;

import org.jgraph.graph.DefaultEdge;
import org.jgrapht.graph.DefaultDirectedGraph;

public class Coordinator extends UnicastRemoteObject implements CoordinatorInterface {
    private static final long serialVersionUID = 1L;

    private String name;
    private List<ServerInterface> serverInterfaceList;
    private HashMap<String, Long> transactionTimeMap;
    private HashSet<String> abortingTransactions;
    private DefaultDirectedGraph<String, DefaultEdge> graph;

    Coordinator(ArrayList<String> addressList, String name) throws RemoteException, UnknownHostException {
        this.name = name;
        this.serverInterfaceList = new ArrayList<>();
        this.transactionTimeMap = new HashMap<>();
        this.abortingTransactions = new HashSet<>();
        this.graph = new DefaultDirectedGraph<>(DefaultEdge.class);
        int port = Integer.parseInt(addressList.get(addressList.size() - 1).split(" ")[2]);
        // The name of the coordinator can be set to F
        Registry registry;
        registry = LocateRegistry.createRegistry(port);
        registry.rebind(this.name, this);

        System.err.println("Coordinator Ready!");

        // Set up connections with servers
        for (int i = 0; i < addressList.size() - 1; i++) {
            String s = addressList.get(i);
            String remoteIp = s.split(" ")[1];

            int remotePort = Integer.parseInt(s.split(" ")[2]);
            String remoteName = s.split(" ")[0];

            try {
                Registry registry_server = LocateRegistry.getRegistry(remoteIp, remotePort);
                ServerInterface remoteServer = (ServerInterface) registry_server.lookup(remoteName);

                this.serverInterfaceList.add(remoteServer);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // Set up deadlock detector
        Timer detectorTimer = new Timer(true);
        DeadlockDetector detector = new DeadlockDetector(this);
        detectorTimer.schedule(detector, 0, 500);
    }

    @Override
    public void addEdge(String transactionId, HashSet<String> lockOwners) throws RemoteException {
        graph.addVertex(transactionId);
        for (String lockOwnerId : lockOwners) {
            graph.addVertex(lockOwnerId);
            graph.addEdge(transactionId, lockOwnerId);
        }
    }

    @Override
    public DefaultDirectedGraph<String, DefaultEdge> getGraph() throws RemoteException {
        return graph;
    }

    @Override
    public void putIntoTransactionTimeMap(String transactionId, long TimeStamp) throws RemoteException {
        transactionTimeMap.put(transactionId, TimeStamp);
    }

    @Override
    public void removeFromTransactionTimeMap(String transactionId) throws RemoteException {
        transactionTimeMap.remove(transactionId);
    }

    @Override
    public HashMap<String, Long> getTransactionTimeMap() throws RemoteException {
        return transactionTimeMap;
    }

    @Override
    public HashSet<String> getAbortingTransactionSet() throws RemoteException {
        return abortingTransactions;
    }

    @Override
    public void removeFromGraph(String transactionId) throws RemoteException {
        graph.removeVertex(transactionId);
    }

    @Override
    public void removeFromAbortingTransactionSet(String transactionId) throws RemoteException {
        abortingTransactions.remove(transactionId);
    }

    @Override
    public boolean containsVertex(String transactionId) throws RemoteException {
        return graph.containsVertex(transactionId);
    }

    @Override
    public void addAbortingTransaction(String transactionId) throws RemoteException {
        abortingTransactions.add(transactionId);
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            String name = args[0];
            String configFile = args[1];
            try {
                BufferedReader br = new BufferedReader(new FileReader(configFile));
                String line = br.readLine();
                ArrayList<String> addressList = new ArrayList<>();
                while (line != null) {
                    addressList.add(line);
                    line = br.readLine();
                }
                br.close();

                // Initialize the coordinator
                new Coordinator(addressList, name);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }
        } else {
            System.err.println("Incorrect arguments!");
            System.err.println("Expected arguments: [coordinator name]");
            System.exit(0);
        }
    }
}
