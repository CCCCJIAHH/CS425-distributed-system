import java.io.BufferedReader;
import java.io.FileReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.sql.Timestamp;
import java.util.*;

public class Client {
    /**
     * Transaction Id for this Client
     */
    private String transactionId;
    /**
     * Transaction flag: true for in a transaction, false for not
     */
    private boolean transactionFlag;
    /**
     * Aborted flag: true for aborted transaction
     */
    private boolean abortedFlag;
    /**
     * Map server name to remote interface
     */
    private HashMap<String, ServerInterface> serverInterfaceHashMap;
    /**
     * Temporary local storage for write operations
     */
    private HashMap<String, HashMap<String, Integer>> tentativeStorage;
    /**
     * Record all the servers we took read operation. Use for release readLocks
     */
    private HashSet<String> readLockOccupiedServerSet;

    /**
     * Constructor. Set up connections with servers and initialize user input console
     *
     * @param addressList
     * @param transactionId
     */
    Client(ArrayList<String> addressList, String transactionId) {
        this.transactionId = transactionId;
        this.serverInterfaceHashMap = new HashMap<>();
        this.transactionFlag = false;
        this.abortedFlag = false;
        this.tentativeStorage = new HashMap<>();
        this.readLockOccupiedServerSet = new HashSet<>();

        // Set up connections with servers
        for (int i = 0; i < addressList.size() - 1; i++) {
            String s = addressList.get(i);
            String remoteIp = s.split(" ")[1];

            int remotePort = Integer.parseInt(s.split(" ")[2]);
            String remoteName = s.split(" ")[0];

            try {
                Registry registry = LocateRegistry.getRegistry(remoteIp, remotePort);
                ServerInterface remoteServer = (ServerInterface) registry.lookup(remoteName);

                this.serverInterfaceHashMap.put(remoteName, remoteServer);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // User input
        userConsole();
    }

    /**
     * Commit a transaction.
     * 1. Send all tentative changes to servers.
     * 2. Release occupied read and write locks
     * 3. Clear temporary storage
     *
     * @throws RemoteException
     */
    private boolean commitTransaction() throws RemoteException {
        transactionFlag = false;
        // check if balance lower than 0
        for (String serverName : tentativeStorage.keySet()) {
            ServerInterface targetServer = serverInterfaceHashMap.get(serverName);
            HashMap<String, Integer> fakeServerStorage = tentativeStorage.get(serverName);
            for (String key : fakeServerStorage.keySet()) {
                boolean check = targetServer.putCheck(key, fakeServerStorage.get(key));
                if (!check) {
                    abortTransaction();
                    System.out.println("ABORTED");
                    return false;
                }
            }
        }
        // Send all the tentative changes to corresponding servers
        for (String serverName : tentativeStorage.keySet()) {
            ServerInterface targetServer = serverInterfaceHashMap.get(serverName);
            HashMap<String, Integer> fakeServerStorage = tentativeStorage.get(serverName);

            for (String key : fakeServerStorage.keySet()) {
                try {
                    targetServer.put(key, fakeServerStorage.get(key));
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            try {
                targetServer.releaseLocks(transactionId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        // Some server we make only read operation. And readLocks on these servers need to be released
        releaseAllReadLocks();

        // Finished a transaction, clear tentative local storage and read server set
        tentativeStorage.clear();
        readLockOccupiedServerSet.clear();
        for (ServerInterface server : serverInterfaceHashMap.values()) {

            server.getCoordinator().removeFromTransactionTimeMap(transactionId);

            if (server.getCoordinator().containsVertex(transactionId)) {
                server.getCoordinator().removeFromGraph(transactionId);
            }
            break;
        }
        return true;
    }

    /**
     * Abort a transaction.
     * 1. Release occupied read and write locks
     * 2. Clear temporary storage
     *
     * @throws RemoteException
     */
    private void abortTransaction() throws RemoteException {
        transactionFlag = false;
        abortedFlag = true;

        // Release writeLocks
        for (String serverName : tentativeStorage.keySet()) {
            ServerInterface targetServer = serverInterfaceHashMap.get(serverName);
            try {
                targetServer.releaseLocks(transactionId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        // Release readLocks
        releaseAllReadLocks();

        // Finished a transaction, clear tentative local storage and read server set
        tentativeStorage.clear();
        readLockOccupiedServerSet.clear();
        for (ServerInterface server : serverInterfaceHashMap.values()) {

            server.getCoordinator().removeFromTransactionTimeMap(transactionId);

            if (server.getCoordinator().getAbortingTransactionSet().contains(transactionId)) {
                server.getCoordinator().removeFromAbortingTransactionSet(transactionId);
            }

            if (server.getCoordinator().containsVertex(transactionId)) {
                server.getCoordinator().removeFromGraph(transactionId);
            }

            break;
        }
    }

    private void releaseAllReadLocks() {
        for (String serverName : readLockOccupiedServerSet) {
            ServerInterface targetServer = serverInterfaceHashMap.get(serverName);
            try {
                targetServer.releaseLocks(transactionId);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private void userConsole() {
        Scanner scan = new Scanner(System.in);
        String input = scan.nextLine();
        while (input != null) {
            String[] inputs = input.split(" ");
            try {
                switch (inputs[0]) {
                    case "BEGIN":
                        abortedFlag = false;
                        transactionFlag = true;
                        Date date = new Date();
                        Timestamp date_ts = new Timestamp(date.getTime());
                        long l = date_ts.getTime();
                        for (ServerInterface server : serverInterfaceHashMap.values()) {
                            server.getCoordinator().putIntoTransactionTimeMap(transactionId, l);
                            break;
                        }
                        System.out.println("OK");
                        break;
                    case "DEPOSIT":
                        if (abortedFlag) {

                        } else if (!transactionFlag) {
                            System.err.println("Please BEGIN before SET");
                        } else if (inputs.length != 3) {
                            System.err.println("Invalid command");
                        } else {
                            String serverKey = inputs[1];
                            int value = Integer.parseInt(inputs[2]);
                            String serverName = serverKey.split("\\.")[0];
                            String key = serverKey.split("\\.")[1];
                            if (!serverInterfaceHashMap.containsKey(serverName)) {
                                System.err.println("Server [" + serverName + "] doesn't exist");
                            } else {
                                ServerInterface targetServer = serverInterfaceHashMap.get(serverName);
                                // Make write attempts until "Success"
                                String successFlag = targetServer.tryPut(transactionId, key);
                                while (successFlag.equals("FAIL")) {
                                    try {
                                        Thread.sleep(500);
                                        successFlag = targetServer.tryPut(transactionId, key);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                HashMap<String, Integer> fakeServerStorage = tentativeStorage.getOrDefault(serverName, new HashMap<>());
                                if (successFlag.equals("ABORT")) {
                                    abortTransaction();
                                    System.out.println("ABORTED");
                                } else if (successFlag.equals("NOT FOUND")) {
                                    fakeServerStorage.put(key, 0);
                                    System.out.println("OK");
                                } else {
                                    fakeServerStorage.putIfAbsent(key, 0);
                                    System.out.println("OK");
                                }
                                // Save tentative result locally
                                fakeServerStorage.put(key, fakeServerStorage.get(key) + value);
                                tentativeStorage.put(serverName, fakeServerStorage);
                            }
                        }
                        break;
                    case "WITHDRAW":
                        if (abortedFlag) {

                        } else if (!transactionFlag) {
                            System.err.println("Please BEGIN before SET");
                        } else if (inputs.length != 3) {
                            System.err.println("Invalid command");
                        } else {
                            String serverKey = inputs[1];
                            int value = -Integer.parseInt(inputs[2]);
                            String serverName = serverKey.split("\\.")[0];
                            String key = serverKey.split("\\.")[1];
                            if (!serverInterfaceHashMap.containsKey(serverName)) {
                                System.err.println("Server [" + serverName + "] doesn't exist");
                            } else {
                                ServerInterface targetServer = serverInterfaceHashMap.get(serverName);
                                // Make write attempts until "Success"
                                String successFlag = targetServer.tryPut(transactionId, key);
                                while (successFlag.equals("FAIL")) {
                                    try {
                                        Thread.sleep(500);
                                        successFlag = targetServer.tryPut(transactionId, key);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                // Save tentative result locally
                                HashMap<String, Integer> fakeServerStorage = tentativeStorage.getOrDefault(serverName, new HashMap<>());
                                boolean createHere = false;
                                if (fakeServerStorage.containsKey(key)) createHere = true;
                                if (successFlag.equals("ABORT")) {
                                    abortTransaction();
                                    System.out.println("ABORTED");
                                } else if (successFlag.equals("NOT FOUND") && !createHere) {
                                    abortTransaction();
                                    System.out.println("NOT FOUND, ABORTED");
                                } else {
                                    fakeServerStorage.putIfAbsent(key, 0);
                                    System.out.println("OK");
                                    // Save tentative result locally
                                    fakeServerStorage.put(key, fakeServerStorage.get(key) + value);
                                    tentativeStorage.put(serverName, fakeServerStorage);
                                }
                            }
                        }
                        break;
                    case "BALANCE":
                        if (abortedFlag) {

                        } else if (!transactionFlag) {
                            System.err.println("Please BEGIN before GET");
                        } else if (inputs.length != 2) {
                            System.err.println("Invalid command");
                        } else {
                            String serverKey = inputs[1];
                            String serverName = serverKey.split("\\.")[0];
                            String key = serverKey.split("\\.")[1];
                            if (!serverInterfaceHashMap.containsKey(serverName)) {
                                System.err.println("Server [" + serverName + "] doesn't exist");
                            } else {
                                ServerInterface targetServer = serverInterfaceHashMap.get(serverName);

                                HashMap<String, Integer> fakeServerStorage = tentativeStorage.get(serverName);
                                Integer curValue = null;
                                if (fakeServerStorage != null && fakeServerStorage.containsKey(key)) {
                                    // If local tentative object has the key, just print
                                    curValue = fakeServerStorage.get(key);

                                }
                                // We do not have this key locally, we need to query servers
                                // Make read attempts until "Success"
                                String successFlag = targetServer.tryGet(transactionId, key);
                                while (successFlag.equals("FAIL")) {
                                    try {
                                        Thread.sleep(500);
                                        successFlag = targetServer.tryGet(transactionId, key);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                }
                                if (successFlag.equals("ABORT")) {
                                    abortTransaction();
                                    System.out.println("ABORTED");
                                } else if (successFlag.equals("NOT FOUND")) {
                                    if (curValue != null) {
                                        System.out.println(serverName + "." + key + " = " + curValue);
                                    } else {
                                        abortTransaction();
                                        System.out.println("NOT FOUND, ABORTED");
                                    }
                                } else {
                                    // We can read the object
                                    readLockOccupiedServerSet.add(serverName);
                                    int value = targetServer.get(key);
                                    if (curValue != null) value += curValue;
                                    System.out.println(serverName + "." + key + " = " + value);
                                }
                            }
                        }
                        break;
                    case "COMMIT":
                        if (!abortedFlag) {
                            boolean commit = commitTransaction();
                            if (commit) System.out.println("COMMIT OK");
                        }
                        break;
                    case "ABORT":
                        abortTransaction();
                        System.out.println("ABORT");
                        break;
                    default:
                        System.err.println("Invalid command");
                }
            } catch (RemoteException e) {
                e.printStackTrace();
            }
            try {
                input = scan.nextLine();
            } catch (NoSuchElementException e) {
                System.out.println("No input, exit program");
                scan.close();
                System.exit(0);
            }
        }
        scan.close();
        System.exit(0);
    }

    public static void main(String[] args) {
        if (args.length == 2) {
            String transactionId = args[0];
            String configFile = args[1];
            try {
                // Read address book from file to an ArrayList
                BufferedReader br = new BufferedReader(new FileReader(configFile));
                String line = br.readLine();
                ArrayList<String> addressList = new ArrayList<>();
                while (line != null) {
                    addressList.add(line);
                    line = br.readLine();
                }
                br.close();

                // Initialize the client
                new Client(addressList, transactionId);
            } catch (Exception e) {
                e.printStackTrace();
                System.exit(0);
            }

        } else {
            System.err.println("Incorrect arguments!");
            System.err.println("Expected arguments: [server name]");
            System.exit(0);
        }
    }
}
