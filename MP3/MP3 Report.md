NetID siyuren2

NetID jiahanc2

We have 5 servers, 1 coordinator in total. We use two phase locking to ensure the functionality of system.

## How to build and run the code

#### How to build

The project building is powered by [Apache Ant](http://ant.apache.org/). You can rebuild the project on your machine by simply running the following command:

```bash
$ ant
```

For Ant installation on Linux, try:

```bash
$ yum install ant
```

 This will generate a directory named `dist/`. The complied and packed `.jar` packages are inside.

#### How to run the code

Since the project is based on Java RPC service. The intialization includes registering servers. So please strictly follow the order.

1. Run server. Server name can be A/B/C/D/E

    ```bash
    $ ./server A config.txt
    ```

    You should make sure you run all the servers first.

2. Run coordinator. Coordinator should be the last line of config file. The name is F by default.

    ```bash
    $ ./coordinator F config.txt
    ```

3. After running server, you will see the following in the console:

    ```shell
    Server Ready!
    Coordinator ready? (y/n)
    >> 
    ```

    If you have started the coordinator, you can type `y` to start this server.

4. After starting the servers, you can run clients.

    ```bash
    $ ./client asdadd config.txt // type commands user console
    $ ./client asdadd config.txt < in.txt // use text file
    ```

## Roles in a simple transaction

#### Server

Each server has a local hash table to store the accounts on it. The account is described by the following properties.

```java
private int value;
private boolean readLock; // whether its read lock is occupied
private boolean writeLock; // whether its write lock is occupied
protected HashSet<String> readLockOwner; // transaction set that occupy read lock
protected String writeLockOwner; // transaction that occupies read lock
```

Server will receive commands from clients and check whether the accounts' lock is occupied and reply according messages to clients. 

#### Client

Each client has a hash table to store the uncommited changes. Once a transaction is commited, it will send all commands to the server and wait for server to check the commands and commit or abort the changes. Also, it will constantly get user input as commands.

#### Coordinator

Coordinator has two functions: detect dead lock(talk about later) and store aborted transactions to make sure any transactions depending on aborted transactions will be aborted.

#### A simple transaction

Consider a simple transaction:

```
BEGIN
DEPOSIT A.foo 20
WITHDRAW A.foo 10
COMMIT
BEGIN 
BALANCE A.foo
COMMIT
```

Clients sends DEPOSIT message to server to check whether A.foo's lock is aquired. The server finds that A.foo does not exist and return "NOT FOUND" to the client. The client will creathe the account in local hash table and set balance to 20. 

Then it sends WITHDRAW message to server, the server replies  NOT FOUND. The client will check its local storage to check whether the account is created in the transaction. It found the account and make the balance minus 10.

After send COMMIT, the client clear its local storage and release all locks. The server check whether there is a account has balance lower than 0 and store the changes in its local hash table.

In the next transaction, the client send BALANCE message to server. The server check the read/write lock of A.foo and return SUCCESS to client. 

After send COMMIT, the client release all locks. 

## Explanation of concurrency control approach

Below is the pseudo code of implementation.

For client:

```java
Get DEPOSIT:
	if not BEGIN or ABORTED: // if not in transaction or should be aborted
        abortTransaction();
        System.out.println("ABORTED");
    else:
    	localStorage.put(server,(account, value)) // save in local storage
    	msg = tryPut(server, account) // check if locked
    	if msg == 'ABORT':
    		abortTransaction();
        	System.out.println("ABORTED");
    	else if msg == 'SUCCESS' || 'NOT FOUND': // account not exist, create a new one
    		continue // wait for next command
    	else:
    		while msg == 'FAIL': // locked by other transaction
    			wait(500ms)
    			msg = tryPut(server, account)

Get WITHDRAW:
	if not BEGIN or ABORTED:
        abortTransaction();
        System.out.println("ABORTED");
    else:
    	localStorage.put(server,(account, value)) 
    	msg = tryPut(server, account) // check if locked
    	if msg == 'ABORT':
    		abortTransaction();
        	System.out.println("ABORTED");
    	else if msg == 'SUCCESS':
    		continue // wait for next command
    	else if msg == "NOT FOUND" && !localStorage.containsKey(account): // the accont does not exist
    		abortTransaction();
        	System.out.println("ABORTED");
    	else:
    		while msg == 'FAIL': // locked by other transaction
    			wait(500ms)
    			msg = tryPut(server, account)

GET BALANCE:
	if not BEGIN or ABORTED:
        abortTransaction();
        System.out.println("ABORTED");
    else:
    	value = localStorage.get(server, account) // first check local storage
    	if value == null:
    		msg = tryGet(server, account)
    		if msg == 'ABORT':
                abortTransaction();
                System.out.println("ABORTED");
            else if msg == 'SUCCESS':
                continue // wait for next command
            else if msg == "NOT FOUND": // the accont does not exist
                abortTransaction();
                System.out.println("ABORTED");
            else:
                while msg == 'FAIL': // locked by other transaction
                    wait(500ms)
                    msg = tryGet(server, account)

GET COMMIT:
	canCommit = tryCommit(localStorage) // check if any account balance < 0
	if !canCommit:
		abortTransaction();
	else:
    	commit();
    	releaseAllLocks();
    	localStorage.clear();

GET ABORT:
	releaseAllLocks();
    localStorage.clear();
```

For Client:

```java
tryPut:
	if transactionId in abortSet: // should abort
		return 'ABORT'
		
	if account not in storage: // account not exist
		return "NOT FOUND"
		
	if !account.readLock() && !account.writeLock(): // not locked
    	account.setWriteLock(transactionId)
    	return "SUCCESS"
    else if account.readLock() && !account.writeLock(): // read locked, not write lock
    	if account.readLockOwner == transactionId: // the client is the only read lock owner, promote the lock
    		account.setWriteLock(transactionId)
    		return "SUCCESS"
    	else: // other client has read lock
        	return "FAIL"
    else if account.writeLockOwner == transactionId: // the client has write lock
    	return "SUCCESS"
    else: // locked
    	return "FAIL"

tryGet:
	if transactionId in abortSet:
		return 'ABORT'
		
	if account not in storage:
		return "NOT FOUND"
		
	if account.writeLock(): // locked. We do not check whether the lock owner is itself because it will first read from its local storage
		return "FAIL"
	else: // share read lock with others
    	account.readLockOwners.add(transactionId)
    	return "SUCCESS"

tryCommit:
	tempStorage = storage
	tempStorage(account, value)
	if tempStorage.get(account) < 0: // check consistency
		return false
	else:
    	return true
```

## Abort and Roll back

Because the commands are only stored at client before commiting, to roll back, we only need to clear local storage of clients. To ensure other transactions do not use results from aborted transactions, we store aborted transaction ids in the coordinator. Before the server put or get something, it will check the aborted transaction set and reply 'Abort' if id is in the set.

Below is some code about it:

```java
// server check if it is already aborted
HashSet<String> abortingIdSet = getCoordinator().getAbortingTransactionSet();
if (abortingIdSet.contains(transactionId)) {
    return "ABORT";
}

// for client
GET ABORT:
	releaseAllLocks();
    localStorage.clear();
```

## Deadlock Detection

We use coordinator to detect deadlocks. We use DefaultDirectedGraph in java. Once a transaction's command is locked by other transactions, we add a edge from this transaction to the lock owner in the graph. If there is a cycle in the graph, it means a deadlock has occured. We will abort the transaction with the largest timestamp to break the circle.

Below is the pseudo code of implementation.

For client:

```java
WITHDRAW/DEPOSIT:
	msg = tryput(server, account, value)
	if msg == 'FAIL':
		while msg == 'FAIL': // locked by other transaction
    		wait(500ms)
    		msg = tryPut(server, account, value)
    if msg == 'ABORT':
    	abortTransation()
    	
BALANCE:
	msg = tryGet(server, account)
	if msg == 'FAIL':
		while msg == 'FAIL': // locked by other transaction
    		wait(500ms)
    		msg = tryGet(server, account)
    if msg == 'ABORT':
    	abortTransation()	

ABORT/COMMIT:
	releaseAllLocks()
	localStorage.clear()
	coordinator.removeEdge(transactionId)
```

For server:

```java
if locked:
	coordinator.addEdge(transactionId, lockOwners); // add edge to coordinator graph
```

For coordinator:

```java
detectorTimer.schedule(detector, 0, 500); // detect every 500 ms
DeadlockDetector:
	if findCycles(): // find deadlock, abort the latest transaction
		find the maximum time stamp of transaction
		abortSet.add(transactionId)
```

