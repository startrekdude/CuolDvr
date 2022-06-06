package cuoldvr.hls;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import cuoldvr.utility.LineIterator;
import cuoldvr.utility.Logger;

// Provides M3U parsing services
abstract class HLSPlaylist {
	// All tags that apply to the entry, not the playlist
	private static final Set<String> entryKeys = Set.of(
		"EXTINF", "EXT-X-BYTERANGE", "EXT-X-DISCONTINUITY", "EXT-X-KEY", "EXT-X-MAP", "EXT-X-PROGRAM-DATE-TIME",
		"EXT-X-DATERANGE", "EXT-X-STREAM-INF"
	);
	
	// Parses an HLS playlist from a String; returns data via callbacks
	protected final void parse(String s) {
		checkSignature(s); // potentially throws
		
		// Map for the metadata
		Map<String, String> entryMetadata = new HashMap<String, String>();
		
		// Let's parse this playlist
		for (String line : new LineIterator(s)) {
			if (line.isEmpty()) continue; // Skip empty lines
			
			if (line.startsWith("#EXT")) {
				if (line.equals("#EXTM3U")) continue; // skip header
				
				// Parse out the key and value
				int valueIndex = line.indexOf(':');
				String key = line.substring(1, valueIndex == -1 ? line.length() : valueIndex);
				String value = valueIndex == -1 ? null : line.substring(valueIndex+1);
				
				if (entryKeys.contains(key))
					entryMetadata.put(key, value);
				else
					acceptMetadata(key, value);
			} else if (line.charAt(0) == '#') continue; // Skip misc comments
			else {
				acceptEntry(line, entryMetadata);
				entryMetadata.clear();
			}
		}
	}
	
	// Checks if the string is a HLS playlist; if not, throws
	private static void checkSignature(String s) {
		boolean valid = s.startsWith("#EXTM3U") &&
				(s.charAt(7) == '\n' || s.regionMatches(7, "\r\n", 0, 2));
		if (!valid) {
			Logger.error("Not an M3U file");
			throw new IllegalArgumentException("s: not an HLS playlist");
		}
	}
	
	// Subclasses override these
	protected void acceptMetadata(String key, String value) {}
	protected void acceptEntry(String entry, Map<String, String> metadata) {}
}