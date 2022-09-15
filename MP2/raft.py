#!/usr/local/bin/python3.10
import asyncio
import collections
import secrets
import sys
import threading
import time
from enum import Enum

TIMEOUT_BASE = 2
HEARTBEAT = 1
TIMEOUT_INF = 1000

if len(sys.argv) != 3:
    raise IndexError("wrong command line format\n")

try:
    pid, total = int(sys.argv[1]), int(sys.argv[2])
except:
    raise TypeError("please input Integer\n")

secret_generator = secrets.SystemRandom()


class StateType(Enum):
    FOLLOWER = 0
    LEADER = 1
    CANDIDATE = 2


class StateMachine(object):
    electionAlarm = secret_generator.random() * 4 + TIMEOUT_BASE  # make sure timeout > heartbeat

    def __init__(self):
        self.term = 1
        self.state = StateType.FOLLOWER
        self.commitIndex = 0
        self.timestamp = time.time()
        self.voteFor = None
        self.quorum = 0
        self.peers = {}
        self.ready4print = True
        self.logs = ['']
        self.logsQuorum = collections.defaultdict(set)
        for i in range(total):  # store other processes' states
            if i == pid:
                continue
            self.peers[str(i)] = {
                'matchIndex': 0,
            }

    def election_start(self):
        self.term += 1  # new term
        self.state = StateType.CANDIDATE  # become CANDIDATE
        self.quorum = 1  # quorum ++
        self.voteFor = None
        self.ready4print = True

        for i in range(self.commitIndex, len(self.logs)):
            self.logsQuorum[i].add(pid)

        # send STATE information and send RequestVote to peers
        print(f"STATE leader=null", flush=True)
        print(f"STATE term={self.term}", flush=True)
        print(f'STATE state="{self.state.name}"', flush=True)
        for p in self.peers.keys():
            print(f"SEND {p} RequestVotes {self.term}", flush=True)

    def request_vote(self, message_term, sender):
        if message_term > self.term:
            self.term = message_term
            self.voteFor = sender
            self.quorum = 0
            self.state = StateType.FOLLOWER
            self.ready4print = True
            print(
                f"SEND {sender} RequestVotesResponse {self.term} true {self.commitIndex}",
                flush=True)
        # elif message term == current term
        elif message_term == self.term and self.state != StateType.LEADER:
            # if stateMachine.state is CANDIDATE, reject the request
            if self.state == StateType.CANDIDATE or self.voteFor is not None:
                print(
                    f"SEND {sender} RequestVotesResponse {self.term} false {self.commitIndex}",
                    flush=True)
            # else
            else:
                self.voteFor = sender
                self.quorum = 0
                self.state = StateType.FOLLOWER
                print(f"SEND {sender} RequestVotesResponse {self.term} true {self.commitIndex}",
                      flush=True)
        # else message term < current term
        else:
            pass
        StateMachine.electionAlarm = secret_generator.random() * 4 + TIMEOUT_BASE

    def receive_votes(self, message_term, elements, message_sender):
        is_agree = elements[0]
        # if message term is higher
        if message_term > self.term:
            self.quorum = 0
            self.term = message_term
            self.state = StateType.FOLLOWER
            return False
        elif message_term == self.term and self.state == StateType.CANDIDATE:
            # if reply "true"
            if is_agree == "true":
                self.quorum += 1
            self.peers[message_sender]['matchIndex'] = int(elements[1])
            # if quorum exceeds half, can be selected as leader
            if self.quorum >= total // 2 + 1:
                self.state = StateType.LEADER
                StateMachine.electionAlarm = TIMEOUT_INF  # leader doesn't need timeout
                print(f'STATE state="{self.state.name}"', flush=True)
                print(f'STATE leader="{pid}"', flush=True)
        else:
            pass
        if self.state != StateType.LEADER:
            StateMachine.electionAlarm = secret_generator.random() * 4 + TIMEOUT_BASE

    def append_entries(self, message_term, message_sender, elements):
        # if message term is higher
        if message_term > self.term:
            self.quorum = 0
            self.term = message_term
            self.voteFor = message_sender
            self.state = StateType.FOLLOWER
            print(f'STATE leader=null', flush=True)
            print(f"STATE term={self.term}", flush=True)
            print(f'STATE leader="{message_sender}"', flush=True)
            print(f'STATE state="{self.state.name}"', flush=True)
            print(f"SEND {message_sender} AppendEntriesResponse {self.term} {self.commitIndex}",
                  flush=True)
        elif message_term == self.term:
            if self.ready4print:
                self.voteFor = message_sender
                self.state = StateType.FOLLOWER
                self.ready4print = False
                print(f"STATE leader=null", flush=True)
                print(f"STATE term={self.term}", flush=True)
                print(f'STATE leader="{message_sender}"', flush=True)
                print(f'STATE state="{self.state.name}"', flush=True)

            if len(elements) < 2:
                print(f"SEND {message_sender} AppendEntriesResponse {self.term} {self.commitIndex}",
                      flush=True)
            # get log message
            else:
                leader_log_index = int(elements[0])
                log_content = elements[1].strip('["').strip('"]')
                if log_content not in self.logs:
                    self.logs.append(log_content)
                    print(f'STATE log[{self.commitIndex + 1}]=[{self.term}, "{log_content}"]')
                    print(f"SEND {message_sender} AppendEntriesResponse {self.term} {self.commitIndex} true",
                          flush=True)

                elif leader_log_index >= self.commitIndex + 1:
                    self.commitIndex += 1
                    print(f'STATE commitIndex={self.commitIndex}', flush=True)
                    print(
                        f"SEND {message_sender} AppendEntriesResponse {self.term} {self.commitIndex} false",
                        flush=True)
                else:
                    print(
                        f"SEND {message_sender} AppendEntriesResponse {self.term} {self.commitIndex}",
                        flush=True)
        else:
            pass
        StateMachine.electionAlarm = secret_generator.random() * 4 + TIMEOUT_BASE

    def receive_append_entries(self, elements, message_sender):
        agree_index = int(elements[0])
        if len(elements) >= 2:
            is_agree = elements[1]
            # isAgree == "true", which means follower support this log
            if is_agree == "true":
                # if this log is uncommitted log
                if agree_index == self.commitIndex and len(
                        self.logs) - 1 > self.commitIndex:
                    self.logsQuorum[self.commitIndex].add(int(message_sender))
            # isAgree == "false", which means follower know this has been committed, just reply for update
            else:
                self.peers[message_sender]["matchIndex"] += 1
        else:
            self.peers[message_sender]["matchIndex"] = agree_index


async def stdin_reader():
    loop = asyncio.get_event_loop()
    reader = asyncio.StreamReader()
    protocol = asyncio.StreamReaderProtocol(reader)
    await loop.connect_read_pipe(lambda: protocol, sys.stdin)
    return reader


async def waiting_message(reader):
    line = await reader.readline()
    line = line.decode('utf-8')
    return line


stateMachine = StateMachine()


async def processThreading():
    reader = await stdin_reader()
    while True:
        try:
            line = await asyncio.wait_for(waiting_message(reader),
                                          timeout=StateMachine.electionAlarm)  # if don't read data within timeout duration
            line = line.strip().split(' ')
            if line[0] != "RECEIVE" and line[0] != "LOG":
                continue

            if line[0] == "LOG":
                if stateMachine.state != StateType.LEADER:
                    print(f"Only Leader can init LOG", flush=True)
                else:
                    log_content = line[1]
                    print(f"LOG {log_content}", flush=True)
                    print(f'STATE log[{len(stateMachine.logs)}]=[{stateMachine.term},"{log_content}"]', flush=True)
                    stateMachine.logsQuorum[len(stateMachine.logs)].add(pid)
                    stateMachine.logs.append(log_content)
                continue

            message_sender, message_type, message_term, elements = line[1], line[2], int(line[3]), line[4:]

            if message_type == "RequestVotes":
                stateMachine.request_vote(message_term, message_sender)


            elif message_type == "RequestVotesResponse":
                reset = stateMachine.receive_votes(message_term, elements, message_sender)
                if reset is not True:
                    continue

            elif message_type == "AppendEntries":
                stateMachine.append_entries(message_term, message_sender, elements)

            # receive appendEntriesResponse -> is leader -> send appendEntries periodically
            elif message_type == "AppendEntriesResponse":
                stateMachine.receive_append_entries(elements, message_sender)

            else:
                print(f"wrong message type", flush=True)
                break

        except asyncio.TimeoutError:
            stateMachine.election_start()

        except KeyboardInterrupt:
            print(f"{pid} stopped")
            break


async def heartBeatThreading():
    while True:
        if stateMachine.state == StateType.LEADER:
            # judge whether to commit
            if stateMachine.commitIndex != len(stateMachine.logs) - 1 and len(
                    stateMachine.logsQuorum[stateMachine.commitIndex]) >= total // 2 + 1:
                stateMachine.commitIndex += 1
                print(f'STATE commitIndex={stateMachine.commitIndex}', flush=True)
                print(f'COMMITTED {stateMachine.logs[stateMachine.commitIndex]} {stateMachine.commitIndex}', flush=True)
            for p in stateMachine.peers.keys():
                # everything ok, all logs are committed
                if len(stateMachine.logs) - 1 == stateMachine.commitIndex and stateMachine.commitIndex == \
                        stateMachine.peers[p]["matchIndex"]:
                    print(f"SEND {p} AppendEntries {stateMachine.term}", flush=True)
                # leader has uncommitted log
                elif stateMachine.peers[p]['matchIndex'] == stateMachine.commitIndex:
                    log = stateMachine.logs[stateMachine.commitIndex + 1]
                    print(f'SEND {p} AppendEntries {stateMachine.term} {stateMachine.commitIndex} ["{log}"]',
                          flush=True)
                # leader committed, but follower is uncommitted
                elif stateMachine.peers[p]['matchIndex'] < stateMachine.commitIndex:
                    next_index = stateMachine.peers[p]["matchIndex"] + 1
                    new_log = stateMachine.logs[next_index]
                    print(f'SEND {p} AppendEntries {stateMachine.term} {next_index} ["{new_log}"]', flush=True)
        await asyncio.sleep(HEARTBEAT)


if __name__ == "__main__":
    loop = asyncio.get_event_loop()
    tasks = [
        loop.create_task(heartBeatThreading()),
        loop.create_task(processThreading())
    ]
    asyncTasks = asyncio.gather(*tasks)
    loop.run_until_complete(asyncTasks)

    loop.close()
