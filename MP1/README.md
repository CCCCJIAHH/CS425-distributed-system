### This is MP1 readme to instruct how to run our program. The program are in the src folder

Make sure the java version is at least 1.8 to promise the javac.   

To compile, javac  *.java.  
There are one sender and one receiver.  

For the receiver, run like this:   
java ServerRun node1 8081 config1.txt  
java ServerRun node2 8082 config2.txt  
java ServerRun node3 8083 config3.txt  

The receiver is the node that this MP requires to implement. So open the exact numbers of recerivers as you want to test.   

For the sender, run like this:

 python3 -u gentx.py 0.5 | java SenderRun node1 8081 config1.txt  
python3 -u gentx.py 0.5 | java SenderRun node2 8082 config2.txt  
python3 -u gentx.py 0.5 | java SenderRun node3 8083 config3.txt

Sender is what we implement to send messages. It is not necessary to open the same number as receivers. For example, 8 node 5 Hz, if you use 8 senders, it will actually make the receiver receives at 40 Hz. But feel free to open more than one sender to test our multi-sender situation.

