package nachos.threads;
import nachos.ag.BoatGrader;
import java.util.Set;
import java.util.HashSet;

public class Boat
{
    static BoatGrader bg;
	static Condition readyToLeaveOahu, readyToLeaveMolokai;
	static Condition boatRiders;

	static Lock boatLock;
	static Lock lock;
	static int childrenOnOahu, childrenOnMolokai;
	static int adultsOnOahu, adultsOnMolokai;
	static boolean lastPersonLeft = false;
	static int boatIsInUse = 0;		// 0 not used, 1 half used, 2 full used.
	static int boatEmpty = 0, boatHalfFull = 1, boatFull = 2; 
	static int boatLocation = 0;	// Oahu
    
    public static void selfTest()
    {
		BoatGrader b = new BoatGrader();
	
		System.out.println("\n ***Testing Boats with only 2 children***");
		begin(0, 2, b);

		//	System.out.println("\n ***Testing Boats with 2 children, 1 adult***");
		//  begin(1, 2, b);

		//  System.out.println("\n ***Testing Boats with 3 children, 3 adults***");
		//  begin(3, 3, b);
    }

    public static void begin( int adults, int children, BoatGrader b )
    {
		// Store the externally generated autograder in a class
		// variable to be accessible by children.
		bg = b;
		Set<KThread> set = new HashSet<KThread>();

		// Instantiate global variables here
		boatLock = new Lock();
		lock = new Lock();
		readyToLeaveMolokai = new Condition(lock);
		readyToLeaveOahu = new Condition(lock);
		boatRiders = new Condition(boatLock);

		// Create threads here.
		for (int i = 0; i < adults; i++) {
			KThread adultThread = new KThread(new Runnable() {
				public void run() {
					AdultItinerary();
				}
			});
			adultThread.setName("Adult " + i);
			adultThread.fork();
			set.add(adultThread);
		}

		for (int i = 0; i < children; i++) {
			KThread childThread = new KThread(new Runnable() {
				public void run() {
					ChildItinerary();
				}
			});
			childThread.setName("Child " + i);
			childThread.fork();
			set.add(childThread);
		}

		// wait for all threads to complete.
		for (KThread t : set) {
			t.join();
		}
    }

    static void AdultItinerary()
    {
		bg.initializeAdult(); //Required for autograder interface. Must be the first thing called.
		//DO NOT PUT ANYTHING ABOVE THIS LINE. 

		// same code as childrenItinerary
		lock.acquire();
		adultsOnOahu++;
		readyToLeaveOahu.wake();
		readyToLeaveOahu.sleep();

		while (boatLocation != 0 || boatIsInUse == boatFull || childrenOnOahu > 1) {
			readyToLeaveOahu.sleep();
		}
		boatLock.acquire();
		boatIsInUse = boatFull;
		bg.AdultRowToMolokai();
		adultsOnOahu--;
		adultsOnMolokai++;
		boatLocation = 1;
		boatIsInUse = boatEmpty;
		boatLock.release();

		readyToLeaveMolokai.wake();
		lock.release();
    }

    static void ChildItinerary()
    {
		bg.initializeChild(); //Required for autograder interface. Must be the first thing called.

		// let everyone count first.
		lock.acquire();
		childrenOnOahu++;
		readyToLeaveOahu.wake(); 	// wake up sleeping threads.
		readyToLeaveOahu.sleep();

		// child is ready to board on boat.
		while (childrenOnOahu > 0) {
			// wait for boat to come on Oahu
			while (boatLocation != 0 || boatIsInUse == boatFull || childrenOnOahu == 1) {
				readyToLeaveOahu.sleep();
			}
			/* Boat is on Oahu and leave now, but if there are more children,
			 * carry them off too.
			 */
			boatLock.acquire();
			if (boatIsInUse == boatEmpty) {
				if (childrenOnOahu > 1) {
					// more children that can board on this trip.
					boatIsInUse = boatHalfFull;
					readyToLeaveOahu.wake();
					lock.release();
					boatRiders.sleep();
					lock.acquire();
				} else {
					// only one to row.
					if (adultsOnOahu == 0) lastPersonLeft = true;
					boatIsInUse = boatFull;
				}
				// ready to row the boat.
				bg.ChildRowToMolokai();
			} else if (boatIsInUse == boatHalfFull) {
				boatIsInUse = boatFull;
				if (childrenOnOahu == 2 && adultsOnOahu == 0) {
					lastPersonLeft = true;
				}
				boatRiders.wake();
				bg.ChildRideToMolokai();
			}

			childrenOnOahu--;
			childrenOnMolokai++;
			boatLocation = 1;
			boatIsInUse = boatEmpty;
			boatLock.release();

			if (lastPersonLeft) {
				readyToLeaveMolokai.wakeAll();
				lock.release();
				return;
			}

			readyToLeaveMolokai.wake();
			readyToLeaveMolokai.sleep();

			while (boatLocation != 1) {
				readyToLeaveMolokai.sleep();
			}

			if (lastPersonLeft) {
				lock.release();
				return;
			}

			// ready to board to Oahu.
			boatLock.acquire();
			boatIsInUse = boatFull;
			bg.ChildRowToOahu();
			childrenOnOahu++;
			childrenOnMolokai--;
			boatLocation = 0;
			boatIsInUse = boatEmpty;
			boatLock.release();
			lock.release();

			//Reached Oahu.
			lock.acquire();
			readyToLeaveOahu.wakeAll();
		}
    }
}
