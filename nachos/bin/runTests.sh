#/bin/bash
tests=(
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
totalTests=0
passedTests=0
for test in $tests
do
	echo "Running Test: $test"
	totalTests=$((totalTests + 1))
	/Users/ujjwalj/Dropbox/Projects/OS/CS162/nachos/bin/nachos -- nachos.ag.$test | grep "success"
	if [[ $? == 0 ]] ; then
		passedTests=$((passedTests + 1))
	fi
done
echo "------------------------------------------"
echo "Total: $totalTests\t Passed: $passedTests"
echo "------------------------------------------"
