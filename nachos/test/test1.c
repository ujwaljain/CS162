#include "syscall.h"
#include "stdio.h"
#include "stdlib.h"

#define BUFSIZE 64
char buf[BUFSIZE] = "HELLO WORLD";
char file[10] = "test1.txt";

void clear_buf() {
    int i;
    for (i = 0; i < BUFSIZE; i++)
        buf[i] = '\0';
    return;
}

// tests create/open
void test1() {
    int f = creat(file);
    if (f == -1) {
        printf("1.1: file creation failed\n");
    }
    f = open(file);
    if (f == -1) {
        printf("1.1: file opening failed\n");
    }
    close(f);
    unlink(file);
    f = open(file);
    if (f != -1) {
        printf("1.1: failure: non-existent file opened\n");
    }
}

// tests create/open
void test2() {
    int f = creat("");
    if (f != -1) {
        printf("1.3: failure: empty filename created\n");
    }
    f = open("");
    if (f != -1) {
        printf("1.3: failure: empty filename opened\n");
    }
}

// tests close/unlink
void test3() {
    int f = close(0);
    if (f == -1) 
        printf("1.3: Failure: couldn't close stdin\n");

    f = close(1);
    if (f == -1)
        printf("1.3: Failure: couldn't close stdout\n");

    f = close(100);
    if (f != -1)
        printf("1.3: Failure: successfully close unsupported file descriptor\n");

    f = close(-1);
    if (f != -1)
        printf("1.3: Failure: successfully close unsupported file descriptor\n");

    f = close(10);
    if (f != -1)
        printf("1.3: Failure: successfully close non-existent file descriptor\n");

    f = unlink("");
    if (f != -1)
        printf("1.3: Failure: successfully unlinked empty filename\n");

    f = unlink("blahblah");
    if (f != -1)
        printf("1.3: Failure: successfully unlinked non-existent file\n");
}

// tests creat/open/close/unlink
void test4() {
    int f = creat(file);
    if (f == -1)
        printf("1.4: Failure: file creation\n");

    if (close(f) == -1)
        printf("1.4: Failure: file closing\n");
    
    if (unlink(file) == -1)
        printf("1.4: Failure: file deletion\n");

    if (open(file) != -1)
        printf("1.4: Failure: successfully opened deleted file.\n");
}

// tests creat/open/close/unlink
void test5() {
    int f = creat(file);
    if (f == -1)
        printf("1.5: Failure: file creation\n");

    if (close(f) == -1)
        printf("1.5: Failure: file closing\n");
    
    if (open(file) == -1)
        printf("1.5: Failure: couldn't open closed file.\n");

    if (close(f) == -1)
        printf("1.5: Failure: file closing\n");
    
    if (unlink(file) == -1)
        printf("1.5: Failure: file deletion\n");

    if (open(file) != -1)
        printf("1.5: Failure: successfully opened deleted file.\n");
}

//tests read/write
void test6() {
    int f = creat(file);
    char temp[BUFSIZE];
    if (f == -1) {
        printf("1.6: Failure: couldn't open empty file \n");
    }
    if (read(f, temp, 10) == -1) {
        printf("1.6: Failure: couldn't read empty file \n");
    }

    if (write(f, buf, 11) == -1) {
        printf("1.6: Failure: couldn't write empty file \n");
    }

    if (read(f, temp, 12) == -1 && temp[0] != '\0') {
        printf("1.6: Failure: couldn't read an already read file \n");
    }
    close(f);
    f = open(file);
    if (read(f, temp, 12) == -1)
        printf("1.6: Failure: couldn't read written file\n");

    int i;
    for(i = 0; i < BUFSIZE; i++)
        if (temp[i] != buf[i]) {
            printf("1.6: Failure: read characters different than written characters\n");
            break;
        }
    close(f);
}
int main() {
    test1();
    test2();
    test3();
    test4();
    test5();
    test6();
    // cleanup
    unlink(file);
}
