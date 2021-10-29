from sort import sort,rangesort,breakrange
import numpy as np
import matplotlib.pyplot as plt
import pandas as pd


file1 ='node1.csv'
bandwidth1,delay1=breakrange(file1)

file2='node2.csv'
bandwidth2,delay2=breakrange(file2)

file3='node3.csv'
bandwidth3,delay3=breakrange(file3)


x=np.arange(1,len(bandwidth1)+1)
x_tick=np.arange(1,len(bandwidth1)+1,5)
plt.title('node1')
plt.xlabel('message-ID')
plt.ylabel('bandwidth-Mbps')
plt.xticks(x_tick)

plt.plot(x,bandwidth1,marker='o', markersize=2)
plt.show()


#node2 send message
x=np.arange(1,len(bandwidth2)+1)
x_tick=np.arange(1,len(bandwidth2)+1,5)
plt.title('node2')
plt.xlabel('message-ID')
plt.ylabel('bandwidth-Mbps')
plt.xticks(x_tick)

plt.plot(x,bandwidth2,marker='o', markersize=2)
plt.show()

#plot node3 sned message
x=np.arange(1,len(bandwidth3)+1)
x_tick=np.arange(1,len(bandwidth3)+1,5)
plt.title('node3')
plt.xlabel('message-ID')
plt.ylabel('bandwidth-Mbps')
plt.xticks(x_tick)

plt.plot(x,bandwidth3,marker='o', markersize=2)
plt.show()

#draw delay
x=np.arange(1,len(bandwidth1)+1)
x_tick=np.arange(1,len(bandwidth1)+1,5)
plt.title('node1')
plt.xlabel('message-ID')
plt.ylabel('delay-ms')
plt.xticks(x_tick)

plt.plot(x,delay1,marker='o', markersize=2)
plt.show()


#draw delay2
x=np.arange(1,len(bandwidth2)+1)
x_tick=np.arange(1,len(bandwidth2)+1,5)
plt.title('node2')
plt.xlabel('message-ID')
plt.ylabel('delay-ms')
plt.xticks(x_tick)

plt.plot(x,delay2,marker='o', markersize=2)
plt.show()


#draw delay 3
x=np.arange(1,len(bandwidth3)+1)
x_tick=np.arange(1,len(bandwidth3)+1,5)
plt.title('node3')
plt.xlabel('message-ID')
plt.ylabel('delay-ms')
plt.xticks(x_tick)

plt.plot(x,delay3,marker='o', markersize=2)
plt.show()




