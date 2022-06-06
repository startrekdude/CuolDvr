package cuoldvr.hls;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import cuoldvr.utility.Logger;

final class Playlist extends HLSPlaylist {
	// Entries calculated relative to this
	private final URL baseUrl;
	private final List<PlaylistEntry> entries;
	
	Playlist(String s, URL baseUrl) {
		this.baseUrl = baseUrl;
		entries = new ArrayList<PlaylistEntry>();
		parse(s);
	}
	
	List<PlaylistEntry> entriesFrom(PlaylistEntry e) {
		// Return all entries appearing after e, or just the last entry if e can't be found
		int entryLen = entries.size();
		for (int i = 0; i < entryLen; i++) {
			PlaylistEntry entry = entries.get(i);
			if (entry.equals(e)) {
				return Collections.unmodifiableList(entries.subList(i+1, entryLen));
			}
		}
		
		// If we're here, just return the last element
		return Collections.unmodifiableList(entries.subList(entryLen-1, entryLen));
	}
	
	@Override
	protected void acceptEntry(String entry, Map<String, String> metadata) {
		URL url;
		try {
			url = new URL(baseUrl, entry);
		} catch(MalformedURLException e) {
			// Maybe one of the other ones works, let's be tolerant of issues
			Logger.warnf("Unable to parse url: %s", entry);
			return;
		}
		
		boolean discontinous = metadata.containsKey("EXT-X-DISCONTINUITY");
		String duration = "unknown";
		if (metadata.containsKey("EXTINF")) {
			String extInf = metadata.get("EXTINF");
			int firstComma = extInf.indexOf(',');
			if (firstComma != -1) {
				duration = extInf.substring(0, firstComma);
			}
		}
		
		entries.add(new PlaylistEntry(url, duration, discontinous));
	}
}
