package cuoldvr.mux;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.json.JsonString;
import javax.json.JsonValue;

import cuoldvr.utility.FileUtils;
import cuoldvr.utility.InputStreamUtils;
import cuoldvr.utility.Logger;
import cuoldvr.utility.ParserUtils;

// All the FFmpeg stuff goes here
// Tries to present an API that pretends it's not using a command-line program
final class FFmpeg {
	private FFmpeg() {} // static class
	
	// Keep a reference to the Runtime - should /for sure/ be a static class, ugh
	private final static Runtime runtime = Runtime.getRuntime();
	private final static TsStats corruptStat = new TsStats(true, 0, 0, 0);
	
	// What? I'm interested.
	static int invocationCount = 0;
	
	// Concatenate a bunch of parts to a dst file using the concat demuxer
	// Parts /should/ be the same stream layout, /must/ be the same codecs (duh)
	static void concatToMp4(List<File> parts, File dst) {
		// Make the concat file input, use a temp file
		File concat = FileUtils.tempSibling(dst, "txt");
		try {
			PrintWriter writer = new PrintWriter(new FileWriter(concat));
			parts.forEach(f -> writer.printf("file '%s'\n", f.getAbsolutePath()));
			writer.close();
		} catch(IOException e) {
			Logger.errorf("Writing concat file, IOException, %s", e.getMessage());
		}
		
		// Prep and execute the command
		String[] cmd = new String[] {
			"ffmpeg", "-f", "concat", "-safe", "0", "-i", concat.getPath(),
			"-map", "0", "-c", "copy", "-fflags", "+genpts", "-movflags", "+faststart",
			dst.getPath()
		};
		
		try {
			Process coder = runtime.exec(cmd);
			invocationCount++;
			InputStreamUtils.readBytes(coder.getErrorStream());
			InputStreamUtils.readBytes(coder.getInputStream());
			coder.waitFor();
		} catch(Exception e) {
			Logger.errorf("Concat: %s, %s, %s", dst, e.getClass().getName(), e.getMessage());
		}
		
		// Clean up
		concat.delete();
	}
	
	// Honestly, this is just magic
	// Okay, no, but it's complex
	// TS to TS, -shortest, and add anullsrc streams for any non-present sample rates
	// The ordering is also a thing - set /should/ have a stable iteration order
	static File normalize(Segment ts, Set<Integer> sampleRates) {
		File out = ts.file.getParentFile().toPath()
			.resolve(String.format("%s.ts", UUID.randomUUID())).toFile();
		
		// Video input
		List<String> cmd = new ArrayList<String>();
		cmd.addAll(Arrays.asList("ffmpeg", "-f", "mpegts", "-i", ts.file.getPath()));
		
		// Make the audio inputs
		for (Integer sampleRate : sampleRates) {
			if (sampleRate == ts.sampleRate)
				cmd.addAll(Arrays.asList("-f", "mpegts", "-i", ts.file.getPath()));
			else
				cmd.addAll(Arrays.asList("-f", "lavfi", "-i", String.format("anullsrc=r=%s", sampleRate)));
		}
		
		// Make the mappings
		cmd.addAll(Arrays.asList("-map", "0:v"));
		for (int i = 0; i < sampleRates.size(); i++) {
			cmd.addAll(Arrays.asList("-map", String.format("%s:a", i+1)));
		}
		
		// Make the codecs
		cmd.addAll(Arrays.asList("-c:v:0", "copy"));
		int i = 0;
		for (Integer sr : sampleRates) {
			if (sr == ts.sampleRate)
				cmd.addAll(Arrays.asList(String.format("-c:a:%s", i), "copy"));
			else
				cmd.addAll(Arrays.asList(String.format("-c:a:%s", i), "aac"));
			i++;
		}
		
		cmd.addAll(Arrays.asList("-shortest", "-f", "mpegts", out.getPath()));
		
		// Run the command
		try {
			Process coder = runtime.exec(cmd.toArray(new String[0]));
			invocationCount++;
			InputStreamUtils.readString(coder.getErrorStream());
			InputStreamUtils.readString(coder.getInputStream());
			coder.waitFor();
		} catch(Exception e) {
			Logger.errorf("Normalize %s, %s, %s", ts, e.getClass().getName(), e.getMessage());
		}
		
		return out;
	}
	
	// Probe durations, corruption, sample rate
	static TsStats probe(File ts) {
		// Fill a complete TsStats structure
		String[] cmd = new String[] {
			"ffprobe", "-hide_banner", "-f", "mpegts", "-i", ts.getPath(), "-show_entries",
			"program_stream=codec_type,duration,sample_rate", "-of", "json"
		};
		
		// Read the error and output from the process
		JsonObject output;
		String error;
		try {
			Process prober = runtime.exec(cmd);
			invocationCount++;
			
			error = InputStreamUtils.readString(prober.getErrorStream());
			JsonReader jr = Json.createReader(prober.getInputStream());
			output = jr.readObject();
			jr.close();
		} catch(IOException e) {
			Logger.warnf("Stat file %s, IOException %s", ts, e.getMessage());
			return corruptStat;
		}
		
		// Is the file corrupt? This is a bit..ugly.
		boolean corrupt = error.indexOf("non-existing PPS 0 referenced") != -1
			|| error.indexOf("Could not find codec parameters") != -1
			|| error.indexOf("decoding for stream 0 failed") != -1;
		
		// Parse out the streams array
		JsonArray programs = output.getJsonArray("programs");
		if (programs == null || programs.isEmpty()) return corruptStat;
		JsonValue firstProgramValue = programs.get(0);
		if (!(firstProgramValue instanceof JsonObject)) return corruptStat;
		JsonObject firstProgram = (JsonObject) firstProgramValue;
		JsonArray streams = firstProgram.getJsonArray("streams");
		if (streams == null) return corruptStat;
		
		// Pull out the interesting stats
		int sampleRate = 0;
		double audioDuration = 0;
		double videoDuration = 0;
		for (JsonValue jv : streams) {
			if (!(jv instanceof JsonObject)) continue;
			JsonObject stream = (JsonObject) jv;
			String codecType = getStringOrNull(stream, "codec_type");
			if ("audio".equals(codecType)) {
				audioDuration = ParserUtils.parseDoubleOrDefault(getStringOrNull(stream, "duration"), 0);
				sampleRate = ParserUtils.parseIntOrDefault(getStringOrNull(stream, "sample_rate"), 0);
			} else if ("video".equals(codecType)) {
				String vd = getStringOrNull(stream, "duration");
				// Sometimes, streams are missing duration
				if (vd == null) videoDuration = -1;
				else videoDuration = ParserUtils.parseDoubleOrDefault(vd, 0);
			}
		}
		
		return new TsStats(corrupt, sampleRate, audioDuration, videoDuration);
	}
	
	// Because the JsonObject methods are inconsistent when dealing with nonexistent keys
	private static String getStringOrNull(JsonObject jo, String key) {
		JsonString js = jo.getJsonString(key);
		if (js != null) return js.getString();
		return null;
	}
}