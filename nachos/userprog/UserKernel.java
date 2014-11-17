package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A kernel that can support multiple user processes.
 */
public class UserKernel extends ThreadedKernel {
    /**
     * Allocate a new user kernel.
     */
    public UserKernel() {
	super();
    }

    /**
     * Initialize this kernel. Creates a synchronized console and sets the
     * processor's exception handler.
     */
    public void initialize(String[] args) {
	super.initialize(args);

	console = new SynchConsole(Machine.console());
	freePages = new ArrayList<Integer>();
	pageLock = new Lock();
    processLock = new Lock();
	// initialize all user memory is free
	for (int i = 0; i < Machine.processor().getNumPhysPages(); i++)
		freePages.add(i);

    processMap = new HashMap<Integer, UserProcess>();
	Machine.processor().setExceptionHandler(new Runnable() {
		public void run() { exceptionHandler(); }
	    });
    }

    /**
     * Test the console device.
     */	
    public void selfTest() {
	super.selfTest();

	System.out.println("Testing the console device. Typed characters");
	System.out.println("will be echoed until q is typed.");

	char c;

	do {
	    c = (char) console.readByte(true);
	    console.writeByte(c);
	}
	while (c != 'q');

	System.out.println("");
    }

    /**
     * Returns the current process.
     *
     * @return	the current process, or <tt>null</tt> if no process is current.
     */
    public static UserProcess currentProcess() {
	if (!(KThread.currentThread() instanceof UThread))
	    return null;
	
	return ((UThread) KThread.currentThread()).process;
    }

	/**
	 * @return an available page to be added in pageTable
	 */
	public static int getFreePage() {
		pageLock.acquire();
		int page = -1;
		if (freePages != null || !freePages.isEmpty())
			page = freePages.remove(0);
		pageLock.release();
		return page;
	}

	/**
	 * Called when userprocess releases page to be added to free memory pool
	 */
	public static void putFreePage(int page) {
		pageLock.acquire();
		freePages.add(page);
		pageLock.release();
	}

    /**
     * The exception handler. This handler is called by the processor whenever
     * a user instruction causes a processor exception.
     *
     * <p>
     * When the exception handler is invoked, interrupts are enabled, and the
     * processor's cause register contains an integer identifying the cause of
     * the exception (see the <tt>exceptionZZZ</tt> constants in the
     * <tt>Processor</tt> class). If the exception involves a bad virtual
     * address (e.g. page fault, TLB miss, read-only, bus error, or address
     * error), the processor's BadVAddr register identifies the virtual address
     * that caused the exception.
     */
    public void exceptionHandler() {
	Lib.assertTrue(KThread.currentThread() instanceof UThread);

	UserProcess process = ((UThread) KThread.currentThread()).process;
	int cause = Machine.processor().readRegister(Processor.regCause);
	process.handleException(cause);
    }

    /**
     * Start running user programs, by creating a process and running a shell
     * program in it. The name of the shell program it must run is returned by
     * <tt>Machine.getShellProgramName()</tt>.
     *
     * @see	nachos.machine.Machine#getShellProgramName
     */
    public void run() {
	super.run();

	UserProcess process = UserProcess.newUserProcess();
	
	String shellProgram = Machine.getShellProgramName();
	Lib.assertTrue(process.execute(shellProgram, new String[] { }));

	KThread.currentThread().finish();
    }

    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }

    /**
     * Assign processId to a userProcess
     */ 
    public static int assignProcessId(UserProcess p) {
        processLock.acquire();
        processMap.put(processIdCounter, p);
        int tmp = processIdCounter++;
        processLock.release();
        return tmp;
    }

    public static UserProcess getProcessUsingPid(int pid) {
        return processMap.get(pid);
    }

    /**
     * Remove process from the kernel, process finished.
     */
    public static void removeProcessId(int processId) {
        processMap.remove(processId);
    }

    /** Globally accessible reference to the synchronized console. */
    public static SynchConsole console;

    private static Lock pageLock;
    private static Lock processLock;
    /** Gobal list of available pages in the processor */
    private static List<Integer> freePages;
    // dummy variables to make javac smarter
    private static Coff dummy1 = null;
    /* process id counter */
    private static int processIdCounter = 1;
    /* Map of processId and user process */
    private static Map<Integer, UserProcess> processMap;

}
