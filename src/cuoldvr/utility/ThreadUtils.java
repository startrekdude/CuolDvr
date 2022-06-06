package cuoldvr.utility;

public final class ThreadUtils {
	private ThreadUtils() {} // static class
	
	public static void sleep(long millis) {
		// I don't care that I can theoretically be interrupted, shut up, Java
		try { Thread.sleep(millis); } catch (InterruptedException e) {}
	}
}
