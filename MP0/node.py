# -*- coding = utf-8 -*-
import os
import subprocess
import sys
import socket
import time


def main():
    # check the No. of input parameter
    if len(sys.argv) != 4:
        print("invalid parameter #")
        return -1
    # read the parameters
    node_name = sys.argv[1]
    ip = sys.argv[2]
    port = int(sys.argv[3], 10)

    # read output of generator.py and send to logger
    s = socket.socket(socket.AF_INET, socket.SOCK_STREAM)  # TCP and ipv4
    s.connect((ip, port))
    s.send("{0} - {1} connected \n".format(time.time(), node_name).encode("utf-8"))
    while True:
        data = input()
        print(data + "\n")
        split = data.split(" ")
        time_stamp = split[0]
        event = split[1]
        s.send("{0} {1} {2}".format(time_stamp, node_name, event).encode("utf-8"))


if __name__ == '__main__':
    main()
