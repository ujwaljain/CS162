package nachos.ag;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.threads.Lock;
import nachos.threads.KThread;
import nachos.threads.PriorityScheduler;
import nachos.threads.ThreadedKernel;

public class SchedulerGrader3 extends BasicTestGrader
{
	Lock lock;

	public void run () {
		assertTrue(ThreadedKernel.scheduler instanceof PriorityScheduler,
	  		"this test requires priority scheduler");

		lock = new Lock();
		// parent threads acquires the lock with priority 1
		lock.acquire();

		ThreadHogger th = new ThreadHogger();
		forkNewThread(new NormalThread(), 3);
		forkNewThread(th, 2);

		// main thread can't get back past without priority donation.
		KThread.yield();
		lock.release();
		th.d = 1;

		// run same test again with different priorities
		lock.acquire();
		th.d = 0;
		forkNewThread(new NormalThread(), 2);
		forkNewThread(th, 3);

		KThread.yield();
		lock.release();
		th.d = 1;
		done();
	}
  
	private class NormalThread implements Runnable {
		public void run () {
			lock.acquire();
			lock.release();
		}
	}

	private class ThreadHogger implements Runnable {
		public int d = 0;
		public void run() {
			while (d == 0) {
				KThread.yield();
			}
		}
	}
}