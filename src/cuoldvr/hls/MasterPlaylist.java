package cuoldvr.hls;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cuoldvr.utility.Logger;

// Represents an http live streaming master playlist
// Keeps track of the sub playlists, and has a method to select them
final class MasterPlaylist extends HLSPlaylist {
	// Keep track of the sub-playlists URL and bandwidth
	private static class SubPlaylistDescriptor {
		private final int bandwidth;
		private final URL url;
		
		private SubPlaylistDescriptor(int bandwidth, URL url) {
			this.bandwidth = bandwidth;
			this.url = url;
		}
	}
	
	// Use a Regex to pull out the bandwidth
	private static final Pattern bandwidthMatch = Pattern.compile("BANDWIDTH=([\\d]+)");
	
	private final URL baseUrl; // Sub-playlists are relative to this
	private final List<SubPlaylistDescriptor> subPlaylists;
	
	MasterPlaylist(String s, URL baseUrl) {
		this.baseUrl = baseUrl;
		subPlaylists = new ArrayList<SubPlaylistDescriptor>();
		parse(s);
	}
	
	URL selectPlaylist(int targetBandwidth) {
		if (subPlaylists.isEmpty()) {
			Logger.errorf("%s: no sub playlists", baseUrl);
			throw new IllegalStateException("No sub playlists");
		}
		
		// Rule: find the closest higher or equal to the target
		Comparator<SubPlaylistDescriptor> byBandwidth =	
			(d1, d2) -> Integer.compare(d1.bandwidth, d2.bandwidth);
		Optional<SubPlaylistDescriptor> spd = subPlaylists.stream().
			filter(d -> d.bandwidth >= targetBandwidth).
			sorted(byBandwidth).
			findFirst();
		if (spd.isPresent()) return spd.get().url;
		
		return subPlaylists.stream().sorted(byBandwidth.reversed()).findFirst().get().url;
	}
	
	private static int parseStreamInf(String s) {
		Matcher ma = bandwidthMatch.matcher(s);
		if (ma.find()) {
			try {
				return Integer.parseInt(ma.group(1));
			} catch(Throwable e) {
				return 0;
			}
		} else return 0; // Unfortunate
	}
	
	@Override
	protected void acceptEntry(String entry, Map<String, String> metadata) {
		URL url;
		try {
			// This works okay for fully-qualified and not-so-fully qualified urls
			url = new URL(baseUrl, entry);
		} catch(MalformedURLException e) {
			Logger.warnf("Malformed URL in MasterPlaylist: %s", entry);
			return; // Better luck next entry
		}
		
		int bandwidth;
		if (metadata.containsKey("EXT-X-STREAM-INF")) {
			String streamInf = metadata.get("EXT-X-STREAM-INF");
			bandwidth = parseStreamInf(streamInf);
			if (bandwidth == 0) Logger.warnf("Unable to parse stream inf: %s", streamInf);
		} else {
			bandwidth = 0; // In this case, be pessimistic for now
		}
		
		subPlaylists.add(new SubPlaylistDescriptor(bandwidth, url));
	}
}
