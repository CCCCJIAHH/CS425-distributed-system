# -*- coding = utf-8 -*-
import socket
import sys
import threading
import time
import csv


def main():
    port = int(sys.argv[1], 10)
    # build a server
    server = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
    server.bind(('172.22.156.131', port))  # bind port
    server.listen(8)
    print("Waiting for connection... \n")
    csvfile = open('log2.csv', 'w')
    writer = csv.writer(csvfile)
    header = ['event_timestamp', 'node_name', 'event', 'log_timestamp', 'message_length', 'log_time']
    writer.writerow(header)
    while True:
        conn, addr = server.accept()
        t = threading.Thread(target=tcp_link, args=(conn, writer))
        t.start()


def tcp_link(sock, writer):
    while True:
        data = sock.recv(1024)
        if not data:
            break
        # get current time
        cur_time = time.time()
        cur_min = time.localtime().tm_min
        cur_sec = time.localtime().tm_sec
        # print event message
        data_str = data.decode("utf-8")
        log_line = "{0}".format(data_str)
        print(log_line)
        # write into log.csv
        split = log_line.split(" ")
        event_timestamp = split[0]
        node_name = split[1]
        event = split[2]
        log_timestamp = cur_time
        message_length = len(log_line)
        log_time = "{0}:{1}".format(cur_min, cur_sec)
        data = [event_timestamp, node_name, event, log_timestamp, message_length, log_time]
        writer.writerow(data)
    sock.close()


if __name__ == '__main__':
    main()
