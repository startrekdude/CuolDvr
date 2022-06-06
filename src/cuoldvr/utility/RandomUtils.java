package cuoldvr.utility;

// Various utilities for generating random numbers
public final class RandomUtils {
	private RandomUtils() {} // static class
	
	// Inclusive on both bounds
	public static int inRange(int low, int high) {
		return low + (int)(Math.random() * ((high - low) + 1));
	}
}
