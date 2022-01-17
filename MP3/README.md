# MP3

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

