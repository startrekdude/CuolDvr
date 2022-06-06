package cuoldvr.utility;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Various misc parser utils
public final class ParserUtils {
	private ParserUtils() {} // static class
	
	private final static Pattern hmSpec = Pattern.compile("^([\\d]{1,2}):([\\d]{2})");
	
	// Spec like hh:mm or possibly h:mm
	// We use 24hr, less ambiguous
	public static int[] parseHMSpec(String spec) {
		Matcher matcher = hmSpec.matcher(spec);
		if (!matcher.matches()) throw new IllegalArgumentException("not a hm spec");
		
		int h = Integer.parseInt(matcher.group(1));
		int m = Integer.parseInt(matcher.group(2));
		if (h < 0 || h > 23) throw new IllegalArgumentException("h out of range");
		if (m < 0 || m > 59) throw new IllegalArgumentException("m out of range");
		return new int[] { h, m };
	}
	
	public static int parseIntOrDefault(String s, int d) {
		try { return Integer.parseInt(s); }
		catch(NumberFormatException nfe) { return d; }
	}
	
	// Double.parseDouble(null) returns NPE, Integer.parseInt(null) returns NFE
	// Just an odd inconsistency I found
	public static double parseDoubleOrDefault(String s, double d) {
		if (s == null) return d;
		try { return Double.parseDouble(s); }
		catch(NumberFormatException nfe) { return d; }
	}
}
