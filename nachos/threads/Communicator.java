package nachos.threads;

import nachos.machine.*;
import java.util.LinkedList;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
	/**
	 * Allocate a new communicator.
	 */
	public Communicator() {
		lock = new Lock();
		sleepingSpeaker = new Condition(lock);
		sleepingListener = new Condition(lock);
		currentSpeaker = new Condition(lock);
		isFilled = false;
	}

	/**
	 * Wait for a thread to listen through this communicator, and then transfer
	 * <i>word</i> to the listener.
	 *
	 * <p>
	 * Does not return until this thread is paired up with a listening thread.
	 * Exactly one listener should receive <i>word</i>.
	 *
	 * @param	word	the integer to transfer.
	 */
	public void speak(int word) {
		lock.acquire();
		// no listener, then wait.
		while (isFilled) {
			sleepingSpeaker.sleep();
		}
		isFilled = true;
		transferMsg = word;
		sleepingListener.wake();
		currentSpeaker.sleep();
		lock.release();
	}

	/**
	 * Wait for a thread to speak through this communicator, and then return
	 * the <i>word</i> that thread passed to <tt>speak()</tt>.
	 *
	 * @return	the integer transferred.
	 */    
	public int listen() {
		lock.acquire();
		// no speaker yet.
		while (!isFilled) {
			sleepingListener.sleep();
		}
		isFilled = false;
		int temp = transferMsg;
		currentSpeaker.wake();
		sleepingSpeaker.wake();
		lock.release();
		return temp;
	}

	private Condition currentSpeaker, sleepingSpeaker, sleepingListener;
	private int transferMsg;
	private boolean isFilled;
	private Lock lock;
}
