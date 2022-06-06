package cuoldvr.mux;

import java.io.File;

// Describes a segment, as produced by the segmenter
// The largest possible region of the video that cannot
// be combined with another safely
final class Segment {
	final File file;
	final int sampleRate;
	
	Segment(File file, int sampleRate) {
		this.file = file;
		this.sampleRate = sampleRate;
	}
}
