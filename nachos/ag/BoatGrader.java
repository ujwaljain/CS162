package nachos.ag;

import nachos.threads.Boat;
import nachos.threads.KThread;
import nachos.machine.Lib;

public class BoatGrader extends BasicTestGrader {

    /**
     * BoatGrader consists of functions to be called to show that
     * your solution is properly synchronized. This version simply
     * prints messages to standard out, so that you can watch it.
     * You cannot submit this file, as we will be using our own
     * version of it during grading.
     *
     * Note that this file includes all possible variants of how
     * someone can get from one island to another. Inclusion in
     * this class does not imply that any of the indicated actions
     * are a good idea or even allowed.
     */
	private int adultsOahu, childrenOahu;
    private int adultsMolokai, childrenMolokai;

    void run() {
        final int adults = getIntegerArgument("adults");
        final int children = getIntegerArgument("children");

        Lib.assertTrue(adults >= 0 && children >= 1, 
            "number can't be negative");
        this.startTest(adults, children);
        checkAllCrossed(adults, children);
        done();
    }

    private void startTest(int adults, int children) {
        this.adultsOahu = adults;
        this.childrenOahu = children;
        this.adultsMolokai = this.childrenMolokai = 0;

        Boat.begin(adults, children, this);
    }

    private void checkAllCrossed(int adults, int children) {
        Lib.assertTrue(adultsOahu == 0, adultsOahu +
            " adults are still in Oahu");
        Lib.assertTrue(childrenOahu == 0, childrenOahu +
            " children are still in Oahu");

        Lib.assertTrue(adultsMolokai == adults, adultsMolokai +
            " adults reached Molokai");
        Lib.assertTrue(childrenMolokai == children, childrenMolokai +
            " children reached Molokai");
    }

	//NEW ADDITION FOR 2014
	//MUST BE CALLED AT THE START OF CHILDITINERARY!
	public void initializeChild(){
		System.out.println("A child has forked.");
	}
	
	//NEW ADDITION FOR 2014
	//MUST BE CALLED AT THE START OF ADULTITINERARY!
	public void initializeAdult(){
		System.out.println("An adult as forked.");
	}

    /* ChildRowToMolokai should be called when a child pilots the boat
       from Oahu to Molokai */
    public void ChildRowToMolokai() {
        Lib.assertTrue(childrenOahu > 0, "no children in Oahu.");
        childrenOahu--;
        childrenMolokai++;
        System.out.println("**" + KThread.currentThread().getName() + 
            " rowing to Molokai.");
    }

    /* ChildRowToOahu should be called when a child pilots the boat
       from Molokai to Oahu*/
    public void ChildRowToOahu() {
        Lib.assertTrue(childrenMolokai > 0, "no children in Molokai");
        childrenOahu++;
        childrenMolokai--;
        System.out.println("**" + KThread.currentThread().getName() + 
            " rowing to Oahu.");
    }

    /* ChildRideToMolokai should be called when a child not piloting
       the boat disembarks on Molokai */
    public void ChildRideToMolokai() {
        Lib.assertTrue(childrenOahu > 0, "no children in Oahu.");
        childrenOahu--;
        childrenMolokai++;
	   System.out.println("**" + KThread.currentThread().getName() + 
            " arrived on Molokai as a passenger.");
    }

    /* ChildRideToOahu should be called when a child not piloting
       the boat disembarks on Oahu */
    public void ChildRideToOahu() {
        Lib.assertTrue(childrenMolokai > 0, "no children in Molokai");
        childrenOahu++;
        childrenMolokai--;
        System.out.println("**" + KThread.currentThread().getName() + 
            " arrived on Oahu as a passenger.");
    }

    /* AdultRowToMolokai should be called when a adult pilots the boat
       from Oahu to Molokai */
    public void AdultRowToMolokai() {
        Lib.assertTrue(adultsOahu > 0, "no adults in Oahu");
        adultsOahu--;
        adultsMolokai++;
        System.out.println("**Adult rowing to Molokai.");
    }

    /* AdultRowToOahu should be called when a adult pilots the boat
       from Molokai to Oahu */
    public void AdultRowToOahu() {
        Lib.assertTrue(adultsMolokai > 0, "no adults in Molokai");
        adultsMolokai--;
        adultsOahu++;
        System.out.println("**Adult rowing to Oahu.");
    }

    /* AdultRideToMolokai should be called when an adult not piloting
       the boat disembarks on Molokai */
    public void AdultRideToMolokai() {
        Lib.assertNotReached("Invalid Operation: AdultRideToMolokai");
        System.out.println("**Adult arrived on Molokai as a passenger.");
    }

    /* AdultRideToOahu should be called when an adult not piloting
       the boat disembarks on Oahu */
    public void AdultRideToOahu() {
        Lib.assertNotReached("Invalid Operation: AdultRideToOahu");
        System.out.println("**Adult arrived on Oahu as a passenger.");
    }
}




