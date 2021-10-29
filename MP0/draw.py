# -*- coding = utf-8 -*-

import numpy as np
import matplotlib.pyplot as plt
import pandas as pd

file = 'log2.csv' #if want to read log1.csv,change here to read, log1.csv is small scenario and log2.csv is large scenario
data = pd.read_csv(file,  usecols=['event_timestamp', 'log_timestamp', 'message_length', 'log_time'])
np_data=np.array(data)
start_time = np_data[:,0]
end_time = np_data[:,1]
mess_len=np_data[:,2]
cur_time=np_data[:,3]
transmit_time=np.subtract(end_time*1000,start_time*1000)
bandwidth=np.divide(mess_len*8,transmit_time)

pre=0
maxD=[]
minD=[]
medianD=[]
ninetythT=[]
avBW=[]

for i in range(cur_time.size-1):
    if cur_time[i]!=cur_time[i+1]:
        cur=i+1
        maxT=np.max(transmit_time[pre:cur])
        minT=np.min(transmit_time[pre:cur])
        medianT=np.median(transmit_time[pre:cur])
        sortT=np.sort(transmit_time[pre:cur])
        index=int((cur-pre)*0.9-0.5)
        ninetyth=sortT[index]
        averageBW=np.average(bandwidth[pre:cur])
        maxD.append(maxT)
        minD.append(minT)
        medianD.append(medianT)
        ninetythT.append(ninetyth)
        avBW.append(averageBW)
        pre=cur



# plot delay graph
x=np.arange(1,len(maxD)+1)
x_tick=np.arange(1,len(maxD)+1,5)
plt.title('Delay_graph')
plt.xlabel('Time-s')
plt.ylabel('Delay-ms')
plt.xticks(x_tick)

plt.plot(x,maxD,marker='o', markersize=2)
plt.plot(x,minD,marker='o', markersize=2)
plt.plot(x,medianD,marker='o', markersize=2)
plt.plot(x,ninetythT,marker='o', markersize=2)

plt.legend(['max_daley', 'min_delay', 'median_delay', '90th percentile delay'])
plt.show()


#plot bandwidth
plt.title('Bandwidth_graph')
plt.xlabel('Time-s')
plt.ylabel('Bandwidth-bps')
plt.xticks(x_tick)
plt.plot(x,avBW,marker='o', markersize=2)
plt.legend(['Average_Bandwidth'])
plt.show()

