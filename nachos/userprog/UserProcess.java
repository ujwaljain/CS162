package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
        int numPhysPages = Machine.processor().getNumPhysPages();
        pageTable = new TranslationEntry[numPhysPages];

        fds = new FileDescriptor[MAXFD];
        for (int i=0; i<MAXFD; i++) {
            fds[i] = new FileDescriptor();
            if (isStandardFileDescriptor(i))
                fds[i].openStandardFileDescriptor(i);
        }
    }
    
    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
	public int readVirtualMemory(int vaddr, byte[] data, int offset,
	int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();

	int vpn = Processor.pageFromAddress(vaddr);
	int off = Processor.offsetFromAddress(vaddr);
	TranslationEntry entry = pageTable[vpn];
	int ppn = entry.ppn;
	if (entry == null || !entry.valid)
		return -1;
	if (ppn < 0 || ppn >= Machine.processor().getNumPhysPages())
		return -1;
	entry.used = true;
	int paddr = Processor.makeAddress(ppn, off);
	int amount = Math.min(length, memory.length-paddr);
	System.arraycopy(memory, paddr, data, offset, amount);

	return amount;
	}

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
	public int writeVirtualMemory(int vaddr, byte[] data, int offset,
	int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	int vpn = Processor.pageFromAddress(vaddr);
	int off = Processor.offsetFromAddress(vaddr);
	TranslationEntry entry = pageTable[vpn];
	int ppn = entry.ppn;
	if (entry == null || !entry.valid)
		return -1;
	if (ppn < 0 || ppn >= Machine.processor().getNumPhysPages())
		return -1;
	entry.used = true;
	int paddr = Processor.makeAddress(ppn, off);
	int amount = Math.min(length, memory.length-paddr);
	System.arraycopy(data, offset, memory, paddr, amount);

	return amount;
	}

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}
	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
	numPages += stackPages;
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
	numPages++;

	// before loading sections, allocate physical memory
	for (int i = 0; i < numPages; i++)
	    getPage(i);

	if (!loadSections())
	    return false;

	// store arguments in last page
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {
	if (numPages > Machine.processor().getNumPhysPages()) {
	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}

	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;

		/* get free pages from kernel */
		TranslationEntry page = getPage(vpn);
		page.readOnly = section.isReadOnly();
		Lib.debug(dbgProcess, "loading section with vpn: " + vpn + " and spn: " + page.ppn);
		section.loadPage(i, page.ppn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
    }    

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Create a page table entry for vpn if it doesn't exists, otherwise
     * gets that page.
     */
	private TranslationEntry getPage(int vpn) {
		if (pageTable[vpn] == null) {
			int ppn = UserKernel.getFreePage();
			if (ppn == -1) {
				Lib.debug(dbgProcess, "Error: Can't create page");
				// TODO: handle this error case.
			}
			pageTable[vpn] = new TranslationEntry(vpn, ppn, true,
				false, false, false);
		}
		return pageTable[vpn];
	}

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

	Machine.halt();
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }

    /**
     * Handle the creat(char* name) system call
     */
    private int handleCreate(int a0) {
        String filename = readVirtualMemoryString(a0, MAX_ARG_LENGTH);
        if (isNullOrEmpty(filename))
            return -1;
        int fd = openFileForUser(filename, true);
        Lib.debug(dbgProcess, "handleCreate with fd: " + fd);
        return fd;
    }

    /**
     * Handle open(char* name) system call
     */
    private int handleOpen(int a0) {
        String filename = readVirtualMemoryString(a0, MAX_ARG_LENGTH);
        if (isNullOrEmpty(filename))
            return -1;
        int fd = openFileForUser(filename, false);
        Lib.debug(dbgProcess, "handleOpen with fd: " + fd);
        return fd;
    }

    private int handleClose(int fdIndex) {
        if (fdIndex == FD_STANDARD_INPUT || fdIndex == FD_STANDARD_OUTPUT) {
            // don't do anything, and return successfully.
            return 0;
        }
        if (fdIndex >= 0 && fdIndex < MAXFD) {
            FileDescriptor fd = fds[fdIndex];
            return fd.closeFile();
        }
        Lib.debug(dbgProcess, "handleClose unsuccessful");
        return -1;
    }

    private int handleRead(int fd, int b, int size) {
        if (!isValidFileDescriptor(fd))
            return -1;

        FileDescriptor fdesc = fds[fd];
        byte[] buf = new byte[size];
        if (fdesc.file == null)
            return -1;
        if (fdesc.file.read(buf, 0, size) == -1)
            return -1;
        return writeVirtualMemory(b, buf);
    }

    private int handleWrite(int fd, int b, int size) {
        if (!isValidFileDescriptor(fd))
            return -1;

        byte[] buf = new byte[size];
        int readbytes = readVirtualMemory(b, buf);
        if (readbytes != size)
            return -1;
        
        FileDescriptor fdesc = fds[fd];
        if (fdesc.file != null) {
            return fdesc.file.write(buf, 0, size);
        }
        return -1;
    }

    private int handleUnlink(int a0) {
        String filename = readVirtualMemoryString(a0, MAX_ARG_LENGTH);
        if (isNullOrEmpty(filename))
            return -1;
        int fdIndex = findFileDescriptor(filename);
        if (fdIndex == -1)
            return -1;

        // this index is either existing or new free fd.
        FileDescriptor fd = fds[fdIndex];
        if (!fd.isFree) {
            // matching fd is not close.
            fd.removeWhenClosed = true;
            return 0;
        }

        // fd is free that means, this file is already closed or didn't get
        // opened by this process.
        boolean r = Machine.stubFileSystem().remove(filename);
        return (r) ? 0 : -1;
    }

    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	Lib.debug(dbgProcess, "calling system call: " + syscall);
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
        case syscallCreate:
            return handleCreate(a0);
        case syscallOpen:
            return handleOpen(a0);
        case syscallClose:
            return handleClose(a0);
        case syscallRead:
            return handleRead(a0, a1, a2);
        case syscallWrite:
            return handleWrite(a0, a1, a2);
        case syscallUnlink:
            return handleUnlink(a0);
        default:
	        Lib.debug(dbgProcess, "Unknown syscall " + syscall);
            Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;
	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");
	}
    }

    /** 
     * This returns a file descriptor for name file. If the file,
     * hasn't be opened before, then get an unused file descriptor,
     * otherwise reuse file descriptor.
     */
    private int findFileDescriptor(String name) {
        if (name == null)
            return -1;

        // search for matching file descriptor.
        for (int i = 0; i < MAXFD; i++) {
            FileDescriptor fd = fds[i];
            if (fd.file != null && name.equals(fd.file.getName())) {
                return i;
            }
        }

        // find empty file descriptor
        return findEmptyFileDescriptor();
    }

    private int findEmptyFileDescriptor() {
        for (int i = 0; i < MAXFD; i++) {
            FileDescriptor fd = fds[i];
            if (fd.isFree) {
                return i;
            }
        }
        return -1;
    }

    private int openFileForUser(String name, boolean create) {
        /* find an free file descriptor for the process */
        int fdIndex = findFileDescriptor(name);
        if (fdIndex != -1) {
            /* if already opened then don't do anything otherwise open file
             * and attach this file descriptor to it
             */
            FileDescriptor fd = fds[fdIndex];
            if (fd.isFree) {
                FileSystem fs = Machine.stubFileSystem();
                OpenFile file = fs.open(name, create);
                if (file != null) {
                    fd.openFile(file);
                    return fdIndex;
                }
            } else {
                /* already opened */
                return fdIndex;
            }
        }
        return -1;
    }

    private boolean isValidFileDescriptor(int fdIndex) {
        if (fdIndex >= 0 && fdIndex < MAXFD)
            return true;
        return false;
    }

    private  boolean isStandardFileDescriptor(int fdIndex) {
        if (fdIndex == FD_STANDARD_INPUT || fdIndex == FD_STANDARD_OUTPUT)
            return true;
        return false;
    }

    private boolean isNullOrEmpty(String s) {
        return (s == null || s.trim().equals(""));
    }

    private class FileDescriptor {
        OpenFile file;
        boolean isFree;
        boolean removeWhenClosed;

        public FileDescriptor() {
            isFree = true;
            removeWhenClosed = false;
        }

        // create a file descriptor with standard io
        public void openStandardFileDescriptor(int i) {
            OpenFile f = null;
            if (i == FD_STANDARD_INPUT)
                f = UserKernel.console.openForReading();
            else if (i == FD_STANDARD_OUTPUT)
                f = UserKernel.console.openForWriting();
            
            if (f != null)
                this.openFile(f);
        }

        public void openFile(OpenFile file) {
            this.file = file;
            this.isFree = false;
            this.removeWhenClosed = false;
        }

        public int closeFile() {
            boolean success = false;
            if (this.file != null) {
                file.close();
                isFree = true;
                success = true;
                if (removeWhenClosed)
                    success = Machine.stubFileSystem().remove(file.getName());
                removeWhenClosed = false;
            }
            return (success) ? 0 : -1;
        }
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;

    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
    
    /** This process's used file descriptor. */
    protected FileDescriptor[] fds;
    private int initialPC, initialSP;
    private int argc, argv;
	
    /** Number of max file descriptors supported per process */
    private static final int MAXFD = 16;
    /** Max length of arguments of syscall */
    private static final int MAX_ARG_LENGTH = 256;
    private static final int FD_STANDARD_INPUT = 0;
    private static final int FD_STANDARD_OUTPUT = 1;
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}
