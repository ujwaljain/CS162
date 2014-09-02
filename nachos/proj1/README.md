## Task 1 (Join)

##### Correctness constraints
* If joined thread already finished then return immediately.
* Wait for the joined-thread to finish and then start running the thread.

##### Psuedo Code
Add a private variable on joined-thread on which it is joined. 
```java
join()
	if joined-thread already finished then return
	else put the current thread to sleep

finish()
	when joined-thread finished
	wake up sleeping thread.
```
## Task 2 (Condition Variable)

##### Correctness constraints 
* sleep should put the currrent thread into waiting queue
* wake should wakeup the next thread in the waiting queue
* these operations should be done atomically using interrupts.

## Task 3 (Alarm)

##### Correctness constraints
* Alarm should work with any number of threads.
* Once waitUntil called, currentThread should go to sleep
* Thread should be woken up by timerInterrupt when time elapsed is more than x

```java
waitUntil()
	create a object with currentThread, and wakeUpTime
	insert this object into priority queue
	currentThread.sleep()

timerInterrupt()
	check if the head of priority queue can be woken up
	if yes then put this thread into ready
	if not then don't do anything.
	follow for other remaining threads in queue
```

## Task 4 (Communicator)

##### Corectness constraints
* Speaking thread should wait until that word is listened.
* Listening thread should wait until it listens some word.
* Listening thread only wakes up the speaker whom it listened to.

## Task 5 (Priority Scheduler)

##### Correctness constraints
* You must all dequeue a thread with highest priority.
* If multiple threads have highest priority then dequeue the oldest one.
* 
[Reference](https://github.com/thinkhy/CS162/wiki/Note-for-Project-1#task-v-35-125-lines-priority-schedulin)
