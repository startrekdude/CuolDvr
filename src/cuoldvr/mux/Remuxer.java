package cuoldvr.mux;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import cuoldvr.utility.Logger;

// Remuxes a folder full of transport streams into a mp4 file
// Operates on its own thread, uses FFmpeg
public final class Remuxer implements Runnable {
	private final static Runtime runtime = Runtime.getRuntime();
	
	public static boolean testFFmpeg() {
		// Can we run ffmpeg? yes/no
		try {
			runtime.exec("ffmpeg");
			runtime.exec("ffprobe");
			return true;
		} catch (Throwable e) {
			Logger.errorf("Unable to run ffmpeg: %s, %s", e.getClass().getName(), e.getMessage());
			return false;
		}
	}
	
	// Remux all the transport streams in directory to dst
	// Performs sorting
	private final File directory;
	private final File dst;
	
	public Remuxer(File directory, File dst) {
		if (!directory.isDirectory())
			throw new IllegalArgumentException("Directory must be a directory");
		this.directory = directory;
		this.dst = dst;
	}
	
	public void go() {
		// Kick off the thread
		Logger.infof("Starting remux: %s -> %s", directory, dst);
		Thread thread = new Thread(this);
		thread.setName(String.format("Remux %s %s", directory, dst));
		try { thread.start(); }
		catch (Throwable e) {
			Logger.errorf("Mux thread: %s, %s", e.getClass().getName(), e.getMessage());
		}
	}
	
	@Override
	public void run() {
		// Pull out just the transport streams
		List<File> transportStreams = new ArrayList<File>();
		for (File file : directory.listFiles())
			if (file.getName().endsWith(".ts"))
				transportStreams.add(file);
		
		// Sort them, 1 to n
		Collections.sort(transportStreams);
		
		// Start by segmeting the streams
		Logger.infof("Pass 1: segmenting %s streams", transportStreams.size());
		
		Segmenter segmenter = new Segmenter(directory);
		int logFrequency = transportStreams.size()/15;
		for (int i = 0; i < transportStreams.size(); i++) {
			File ts = transportStreams.get(i);
			
			if (logFrequency != 0 && i != 0 && i%logFrequency == 0)
				Logger.infof("Processing stream: %s (%s)", ts, i);
			
			segmenter.accept(ts);
		}
		
		// Log the segments
		List<Segment> segments = segmenter.finish();
		for (Segment s : segments) {
			Logger.infof("%s %s", s.file, s.sampleRate);
		}
		
		// Pass 2: Normalize every segment
		Logger.infof("Pass 2: Normalizing %s segments", segments.size());
		Set<Integer> rates = new LinkedHashSet<Integer>();
		segments.forEach(s -> rates.add(s.sampleRate));
		Logger.infof("Sample rates: %s, normalizing", rates);
		
		for (Segment s : segments) {
			Logger.infof("Normalizing %s", s.file);
			File newFile = FFmpeg.normalize(s, rates);
			if (newFile.exists()) {
				s.file.delete();
				newFile.renameTo(s.file);
			} else {
				Logger.errorf("Error normalizing %s", s.file);
			}
		}
		
		// Pass 3:, concat everything (hopefully this all works :/)
		Logger.info("Final concatenation");
		List<File> parts = segments.stream()
			.map(s -> s.file)
			.collect(Collectors.toList());
		FFmpeg.concatToMp4(parts, dst);
		
		parts.forEach(File::delete);
		
		Logger.infof("FF*: %s", FFmpeg.invocationCount);
	}
}
