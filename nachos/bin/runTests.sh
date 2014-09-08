#/bin/bash
RoundRobinTests=(
ThreadGrader1
ThreadGrader2
ThreadGrader3
ThreadGrader4
JoinGrader1
JoinGrader2
AlarmGrader1
CommunicatorGrader1
CommunicatorGrader2
)
PrioritySchedulerTests=(
ThreadGrader5
ThreadGrader6
SchedulerGrader1
SchedulerGrader2
SchedulerGrader3
)

totalTests=0
passedTests=0

runTest() {
	for test in ${@}
	do
		echo "Running Test: $test"
		totalTests=$((totalTests + 1))
		/Users/ujjwalj/Dropbox/Projects/OS/CS162/nachos/bin/nachos -- nachos.ag.$test | grep "success"
		if [[ $? == 0 ]] ; then
			passedTests=$((passedTests + 1))
		fi
	done
}

# Run roundrobin tests
runTest $RoundRobinTests

# Run PriorityScheduler tests
cp nachos.conf.ps nachos.conf
runTest $PrioritySchedulerTests
cp nachos.conf.rs nachos.conf

echo "------------------------------------------"
echo "Total: $totalTests\t Passed: $passedTests"
echo "------------------------------------------"
