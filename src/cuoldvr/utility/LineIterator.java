package cuoldvr.utility;

import java.util.Iterator;

// Allows foreach iteration over the lines in a String
public final class LineIterator implements Iterable<String>, Iterator<String> {
	// Keep a reference to the String being iterated and the index
	private final String s;
	private int i;
	
	public LineIterator(String s) {
		this.s = s;
		i = 0;
	}
	
	@Override
	public boolean hasNext() {
		// Use <= to allow for a potential final empty line
		return i <= s.length();
	}

	@Override
	public String next() {
		int nextNewline = s.indexOf('\n', i);
		
		String result;
		if (nextNewline == -1) { // Last line
			result = s.substring(i);
			i = s.length() + 1;
		} else {
			if (s.charAt(nextNewline-1) == '\r') // Strip off a carriage return, if present
				result = s.substring(i, nextNewline-1);
			else
				result = s.substring(i, nextNewline);
			i = nextNewline+1;
		}
		
		return result;
	}

	@Override
	public Iterator<String> iterator() {
		return this;
	}

}