package cuoldvr.hls;

import java.io.File;
import java.net.URL;
import java.util.List;

import cuoldvr.http.HttpUtils;
import cuoldvr.utility.Logger;
import cuoldvr.utility.RandomUtils;
import cuoldvr.utility.ThreadUtils;

// Record stream to directory from when begin is called to end
// Multithreaded!
public final class Recorder {
	public final File directory;
	
	private final URL stream;
	private final String origin;
	private final String referrer;
	
	// This signals to the working thread it's time to stop
	private volatile boolean stopSignal;
	private volatile boolean isRunning;
	
	public Recorder(String directory, URL stream, String origin, String referrer) {
		this.directory = new File(directory);
		this.stream = stream;
		this.origin = origin;
		this.referrer = referrer;
		stopSignal = false;
		isRunning = false;
	}
	
	private URL acquirePlaylist() {
		try {
			String streamData = HttpUtils.fetchString(stream, origin, referrer);
			MasterPlaylist mp = new MasterPlaylist(streamData, stream);
			return mp.selectPlaylist(720000);
		} catch(Throwable e) {
			// Not great; we can't proceed without this. Let's wait for a bit and continue.
			Logger.errorf("Fetching master playlist: %s %s", e.getClass().getName(), e.getMessage());
			int sleepLen = RandomUtils.inRange(2500, 7500);
			Logger.infof("Sleeping for: %s", sleepLen);
			ThreadUtils.sleep(sleepLen);
			return null;
		}
	}
	
	private Playlist fetchPlaylist(URL playlist) {
		// Okay, so: We retry up to 6 times, waiting more each time (intelligent backoff)
		for (int i = 1; i < 7; i++) {
			try {
				// Fingers crossed
				return new Playlist(HttpUtils.fetchString(playlist, origin, referrer), playlist);
			} catch(Throwable e) {
				// Damn. Intelligent backoff time
				int sleepLen = RandomUtils.inRange(i*100, 1100) * i;
				Logger.warnf("Could not fetch playlist (%s %s), sleeping %s",
					e.getClass().getName(), e.getMessage(), sleepLen);
				ThreadUtils.sleep(sleepLen);
			}
		}
		Logger.error("Could not fetch playlist, fall back to master");
		return null;
	}
	
	private File getPathForEntry(PlaylistEntry entry, int i) {
		// 0000001d-16.667.ts
		String name = String.format("%07d%s-%s.ts",
			i, entry.discontinous ? "d" : "", entry.duration);
		return directory.toPath().resolve(name).toFile();
	}
	
	private void loop() {
		URL playlistUrl = null;
		PlaylistEntry lastEntry = null; // The last entry we saw
		int i = 1; // Monotonic counter for file names
		int zeroCnt = 0;
		while (!stopSignal) {
			// Okay, first thing to do: acquire a playlist we can use, if we don't have one
			if (playlistUrl == null) {
				playlistUrl = acquirePlaylist();
				Logger.infof("Using playlist: %s", playlistUrl);
				if (playlistUrl == null) continue; // It failed, it does the waiting
			}
			
			// Wonderful. Now we go to fetch the playlist
			// Note: if a stop request is received at the start of fetching a playlist
			// and it is impossible at that time, the stop request will take some time
			// before it is fulfilled. I don't care.
			Playlist playlist = fetchPlaylist(playlistUrl);
			if (playlist == null) {
				// This is after retries; let's fall back to master and see if we can't pick this up again
				playlistUrl = null;
				continue;
			}
			
			// Alright: what do we have to download?
			List<PlaylistEntry> entries = playlist.entriesFrom(lastEntry);
			
			// Case: nothing to do
			if (entries.isEmpty()) {
				Logger.debug("No new entries, waiting");
				zeroCnt++;
				if (zeroCnt == 6) {
					Logger.warn("30 seconds w/o new entry, fall back to master");
					zeroCnt = 0;
					playlistUrl = null;
				}
				ThreadUtils.sleep(5000);
				continue;
			}
			zeroCnt = 0;
			
			// Pick out the new last entry
			lastEntry = entries.get(entries.size()-1);
			
			// Time to actually download the transport streams
			for (PlaylistEntry entry : entries) {
				File dest = getPathForEntry(entry, i);
				Logger.infof("%s -> %s", entry.url, dest);
				DownloadTask dl = new DownloadTask(entry.url, origin, referrer, dest);
				dl.execute();
				i++; // Next file
			}
			
			ThreadUtils.sleep(5000);
		}
		
		// Give any outstanding downloads time to finish - if they don't, too bad
		Logger.info("Giving download threads 20sec to finish up");
		ThreadUtils.sleep(20000);
		isRunning = false;
	}
	
	public void begin() {
		Logger.infof("Begin: %s -> %s", stream, directory);
		isRunning = true;
		
		Logger.infof("Creating directory: %s", directory);
		directory.mkdirs();
		
		// Let's go!
		Thread thread = new Thread(this::loop);
		thread.setName(String.format("%s -> %s", stream, directory));
		thread.start();	
	}
	
	public void end() {
		Logger.infof("End requested: %s -> %s", stream, directory);
		stopSignal = true;
		
		// Wait for the thread to pick this up
		while (isRunning) ThreadUtils.sleep(1000);
		Logger.infof("End complete: %s -> %s", stream, directory);
	}
}
