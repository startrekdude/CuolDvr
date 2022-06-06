package cuoldvr.mux;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cuoldvr.utility.InputStreamUtils;
import cuoldvr.utility.Logger;

/*
 * Accepts transport streams and builds them into segments
 * New segments are created when:
 *  - The sample rate changes
 *  - Immediately after a stream where the audio is longer than the video
 *  - Discontinuities
 */
final class Segmenter {
	// Allowed variance of audio vs video duration
	private static final double epsilon = 0.5;
	
	// The directory to store segments in
	private final File directory;
	private final List<Segment> segments;
	
	private OutputStream os;
	private boolean splitOnNextStream;
	
	Segmenter(File directory) {
		this.directory = directory;
		segments = new ArrayList<Segment>();
		splitOnNextStream = false;
	}
	
	private void newSegment(int sampleRate) {
		// Close the last segment
		if (os != null)
			try { os.close(); } catch(IOException e) { /* ... how */ }
		
		// Make the new segment
		File segmentFile = directory.toPath().
			resolve(String.format("segment%s.ts", segments.size())).toFile();
		Segment segment = new Segment(segmentFile, sampleRate);
		segments.add(segment);
		
		// Open it for writing
		try {
			os = new FileOutputStream(segmentFile);
		} catch(IOException e) {
			Logger.errorf("CRITICAL: cannot open %s, IOException %s. Muxer will fail!",
				segmentFile, e.getMessage());
		}
	}
	
	void accept(File stream) {
		// Read the stats
		TsStats stats = FFmpeg.probe(stream);
		
		// Reject corrupt streams
		if (stats.corrupt) {
			Logger.debugf("Rejecting %s, corrupt", stream);
			return;
		}
		
		// Do we need to split?
		if (segments.size() == 0) {
			Logger.infof("Split (first stream) %s", stream);
			newSegment(stats.sampleRate);
		} else if (segments.get(segments.size()-1).sampleRate != stats.sampleRate) {
			Logger.infof("Split (new sample rate) %s", stream);
			newSegment(stats.sampleRate);
		} else if (splitOnNextStream) {
			Logger.infof("Split (requested) %s", stream);
			splitOnNextStream = false;
			newSegment(stats.sampleRate);
		} else if (stream.getName().charAt(7) == 'd') {
			Logger.infof("Split (discontinuity) %s", stream);
			newSegment(stats.sampleRate);
		}
		
		// Fix this *new and fun* variant of stream corruption
		if ((stats.audioDuration - stats.videoDuration) > epsilon) {
			splitOnNextStream = true;
			Logger.infof("Requesting split (bad length): %s", stream);
		}
		
		// Ayeet, let's do this
		try {
			InputStream is = new FileInputStream(stream);
			InputStreamUtils.copy(is, os);
		} catch(IOException e) {
			Logger.warnf("%s: IOException, %s", stream, e.getMessage());
		}
	}
	
	List<Segment> finish() {
		try { os.close(); } catch(IOException e) { /* ... how */ }
		
		return Collections.unmodifiableList(segments);
	}
}
