#!/usr/local/bin/python3.10
"""
    @caution -> use "{}" to quote a str for json decode. It's "{}" not '{}'! very important
"""
import sys
import threading
import time
import timeout_decorator
from enum import Enum
import random

TIMEOUT_BASE = 2
HEARTBEAT = 1
TIMEOUT_INF = 1000

if len(sys.argv) != 3:
    raise IndexError("wrong command line format\n")

try:
    pid, total = int(sys.argv[1]), int(sys.argv[2])
except:
    raise TypeError("please input Integer\n")


class StateType(Enum):
    FOLLOWER = 0
    LEADER = 1
    CANDIDATE = 2


class StateMachine(object):
    electionAlarm = random.random() * 4 + TIMEOUT_BASE  # make sure timeout > heartbeat

    def __init__(self):
        self.lock = threading.Lock()
        self.term = 1
        self.pid = pid
        self.state = StateType.FOLLOWER
        self.commitIndex = 0
        self.voteFor = None
        self.quorum = 0
        self.peers = {}
        for i in range(total):  # store other processes' states
            if i == pid:
                continue
            self.peers[str(i)] = {
                'pid': int(i),
                'nextIndex': 1,
                'matchIndex': 0,
                'voteGranted': False,
                'timeout': 0
            }

    def election_start(self):
        self.term += 1  # new term
        self.state = StateType.CANDIDATE  # become CANDIDATE
        self.voteFor = pid  # vote for myself
        self.quorum = 1  # quorum ++
        self.voteFor = None

        # send STATE information and send RequestVote to peers
        print(f"STATE term={self.term}", flush=True)
        print(f'STATE state="{self.state.name}"', flush=True)
        for p in self.peers.keys():
            print(f"SEND {p} RequestVotes {self.term}", flush=True)

    def request_vote(self, messageTerm, sender):
        # if message term is higher
        #   -> FOLLOWER
        #   -> update term
        #   -> vote for sender
        #   -> quorum = 0
        #   -> send RequestVotesResponse
        #   -> reset timeout
        if messageTerm > self.term:
            self.term = messageTerm
            self.voteFor = sender
            self.quorum = 0
            self.state = StateType.FOLLOWER
            print(f"SEND {sender} RequestVotesResponse {self.term} true", flush=True)
        # elif message term == current term
        elif messageTerm == self.term and self.state != StateType.LEADER:
            # if stateMachine.state is CANDIDATE, reject the request
            if self.state == StateType.CANDIDATE:
                print(f"SEND {sender} RequestVotesResponse {self.term} false", flush=True)
            # else
            #   -> FOLLOWER
            #   -> vote for sender
            #   -> quorum = 0
            #   -> send RequestVotesResponse
            #   -> reset timeout
            else:
                self.voteFor = sender
                self.quorum = 0
                self.state = StateType.FOLLOWER
                print(f"SEND {sender} RequestVotesResponse {self.term} true", flush=True)
        # else message term < current term
        else:
            pass
        self.electionAlarm = random.random() * 4 + TIMEOUT_BASE

    def receive_votes(self, messageTerm, elements, messageSender):
        with self.lock:
            isAgree = elements[0]
            # if message term is higher
            #   -> FOLLOWER
            #   -> update term
            #   -> vote for sender
            #   -> quorum = 0
            # NOTICE: I don't know who is leader, don't reset timeout
            if messageTerm > self.term:
                self.quorum = 0
                self.term = messageTerm
                self.state = StateType.FOLLOWER
                return False
            elif messageTerm == self.term and self.state == StateType.CANDIDATE:
                # if reply "true"
                #   -> quorum += 1
                #   -> update peers
                if isAgree == "true":
                    self.quorum += 1
                    self.peers[str(messageSender)]['voteGranted'] = True
                    # if quorum exceeds half, can be selected as leader
                    #   -> state -> LEADER  -> send STATE
                    #   -> send AppendEntries
                    if self.quorum > total // 2 + 1:
                        self.state = StateType.LEADER
                        print(f'STATE state="{self.state.name}"', flush=True)
                        print(f'STATE leader="{pid}"', flush=True)
                        for p in self.peers.keys():
                            # TODO: no sure whether the last parameter is leader id
                            # TODO: add log info
                            print(f"SEND {p} AppendEntries {self.term} {pid}", flush=True)
                        # TODO: how to set real INF timeout
                        StateMachine.electionAlarm = TIMEOUT_INF  # leader doesn't need timeout
                        return False
                else:
                    self.peers[str(messageSender)]['voteGranted'] = False
            else:
                pass
            StateMachine.electionAlarm = random.random() * 4 + TIMEOUT_BASE

    def append_entries(self, messageTerm, messageSender):
        # if message term is higher
        #   -> FOLLOWER
        #   -> update term
        #   -> vote for sender
        #   -> quorum = 0
        # NOTICE: I know the leader is the sender
        if messageTerm > self.term:
            self.quorum = 0
            self.term = messageTerm
            self.voteFor = messageSender
            self.term = messageTerm
        elif messageTerm == self.term:
            print(f"STATE term={self.term}", flush=True)
            self.voteFor = messageSender
            print(f'STATE leader="{messageSender}"', flush=True)
            self.state = StateType.FOLLOWER
            print(f'STATE state="{self.state.name}"', flush=True)

            # TODO: when add log, we need to change status (true) according to log condition
            print(f"SEND {messageSender} AppendEntriesResponse {self.term} true", flush=True)
        else:
            pass
        StateMachine.electionAlarm = random.random() * 4 + TIMEOUT_BASE

    def receive_append_entries(self):
        with self.lock:
            time.sleep(HEARTBEAT)
            for p in self.peers.keys():
                # TODO: no sure whether the last parameter is leader id
                # TODO: add log info
                print(f"SEND {p} AppendEntries {self.term} {pid}", flush=True)


@timeout_decorator.timeout(StateMachine.electionAlarm)
def waitingMessage():
    line = sys.stdin.readline()
    return line


def main():
    stateMachine = StateMachine()

    while True:
        try:
            line = waitingMessage()
            line = line.strip().split(' ')
            if line[0] != "RECEIVE":
                print(f"wrong format", flush=True)
                break
            message_sender, message_type, message_term, elements = line[1], line[2], int(line[3]), line[4:]

            if message_type == "RequestVotes":
                # TODO: we need new election method according to log condition, not every CANDIDATE -> LEADER
                stateMachine.request_vote(message_term, message_sender)


            elif message_type == "RequestVotesResponse":
                reset = stateMachine.receive_votes(message_term, elements, message_sender)
                if reset is not True:
                    continue

            elif message_type == "AppendEntries":
                stateMachine.append_entries(message_term, message_sender)

            # receive appendEntriesResponse -> is leader -> send appendEntries periodically
            elif message_type == "AppendEntriesResponse":
                stateMachine.receive_append_entries()

            else:
                print(f"wrong message type", flush=True)
                break

        except timeout_decorator.TimeoutError:
            stateMachine.election_start()

        except KeyboardInterrupt:
            print(f"{pid} stopped")
            break


if __name__ == "__main__":
    main()
