package nachos.threads;

import nachos.machine.*;

import java.util.PriorityQueue;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
	/**
	 * Allocate a new Alarm. Set the machine's timer interrupt handler to this
	 * alarm's callback.
	 *
	 * <p><b>Note</b>: Nachos will not function correctly with more than one
	 * alarm.
	 */
	public Alarm() {
		Machine.timer().setInterruptHandler(new Runnable() {
			public void run() { timerInterrupt(); }
		});
		waitQueue = new PriorityQueue<WaitingThread>();
	}

	/**
	 * The timer interrupt handler. This is called by the machine's timer
	 * periodically (approximately every 500 clock ticks). Causes the current
	 * thread to yield, forcing a context switch if there is another thread
	 * that should be run.
	 */
	public void timerInterrupt() {
		long currentTime = Machine.timer().getTime();

		while (waitQueue.size() > 0) {
			WaitingThread waitingThread = waitQueue.peek();
			if (waitingThread.wakeTime < currentTime) {
				waitQueue.poll();
				waitingThread.thread.ready();
				Lib.debug(dbgInt, waitingThread.thread.getName() + 
					" waking up from timer interrupt.");
			} else
				break;
		}
	}

	/**
	 * Put the current thread to sleep for at least <i>x</i> ticks,
	 * waking it up in the timer interrupt handler. The thread must be
	 * woken up (placed in the scheduler ready set) during the first timer
	 * interrupt where
	 *
	 * <p><blockquote>
	 * (current time) >= (WaitUntil called time)+(x)
	 * </blockquote>
	 *
	 * @param	x	the minimum number of clock ticks to wait.
	 *
	 * @see	nachos.machine.Timer#getTime()
	 */
	public void waitUntil(long x) {
		// for now, cheat just to get something working (busy waiting is bad)
		boolean intStatus = Machine.interrupt().disable();
		long wakeTime = Machine.timer().getTime() + x;
		
		KThread currentThread = KThread.currentThread();
		WaitingThread waitThread = new WaitingThread(wakeTime, currentThread);

		waitQueue.add(waitThread);
		Lib.debug(dbgInt, "Wait thread " + currentThread.getName() +
		 		" until " + wakeTime);
		KThread.sleep();
		Machine.interrupt().restore(intStatus);
	}

	private class WaitingThread implements Comparable<WaitingThread> {
		long wakeTime;
		KThread thread;

		public WaitingThread(long wakeTime, KThread thread) {
			this.wakeTime = wakeTime;
			this.thread = thread;
		}

		@Override
		public int compareTo(WaitingThread that) {
			if (this.wakeTime == that.wakeTime) return 0;
			return (this.wakeTime > that.wakeTime) ? 1 : -1;
		}
	}
	private PriorityQueue<WaitingThread> waitQueue;
	private static final char dbgInt = 'i';
}