package de.hdu.pvs.crashfinder.instrument;

public class InstrumentStats {

	static int numOfInsertedInstructions = 0;

	public static void addInsertedInstructions(int num) {
		numOfInsertedInstructions += num;
	}

	public static void showInstrumentationStats() {
		System.out.println(numOfInsertedInstructions);
	}

	public static int get() {
		return numOfInsertedInstructions;
	}

	public static void reset() {
		numOfInsertedInstructions = 0;
	}
}