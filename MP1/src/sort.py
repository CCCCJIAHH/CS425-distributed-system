import numpy as np
import matplotlib.pyplot as plt
import pandas as pd




def breakrange(file):
    datatemp = pd.read_csv(file, header=None)
    data = np.array(datatemp)
    len = data[:, 2]
    sendtime1 = data[:, 3]
    receivetime1 = data[:, 4]
    delivertime= data[:, 5]
    transferTime = np.subtract(receivetime1 + 0.1, sendtime1)
    delay = np.subtract(delivertime, sendtime1)
    bandwidth = np.divide(len * 8, transferTime * 1000)

    return bandwidth,delay

if __name__ == '__main__':
    breakrange('node1.csv')
