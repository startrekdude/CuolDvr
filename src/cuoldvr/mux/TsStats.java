package cuoldvr.mux;

// Grab bag of interesting TS stats
// The idea is, ffprobe /once/
final class TsStats {
	final boolean corrupt;
	final int sampleRate;
	final double audioDuration;
	final double videoDuration;
	
	TsStats(boolean corrupt, int sampleRate, double audioDuration, double videoDuration) {
		this.corrupt = corrupt;
		this.sampleRate = sampleRate;
		this.audioDuration = audioDuration;
		this.videoDuration = videoDuration;
	}
}
