package cuoldvr.hls;

import java.net.URL;

//Fairly self-explanatory
final class PlaylistEntry {
	URL url;
	String duration;
	boolean discontinous;
	
	PlaylistEntry(URL url, String duration, boolean discontinous) {
		this.url = url;
		this.duration = duration;
		this.discontinous = discontinous;
	}
	
	@Override
	public boolean equals(Object o) {
		if (!(o instanceof PlaylistEntry)) return false;
		PlaylistEntry other = (PlaylistEntry) o;
		return other.url.equals(url) &&
			other.duration.equals(duration) &&
			other.discontinous == discontinous;
	}
}
