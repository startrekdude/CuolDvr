package cuoldvr.daemon;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cuoldvr.utility.Logger;

final class RecordConfig extends ArrayList<RecordTask> {
	// Yeah, alright
	private static final long serialVersionUID = 4162363135682669836L;
	
	// Parse "hh:mm:ss?"
	private static final Pattern durationPattern = Pattern.compile("^([\\d]{1,2}):([\\d]{2})(?::([\\d]{2}))?$");
	private static int parseDurationSpec(String spec) {
		Matcher matcher = durationPattern.matcher(spec);
		if (!matcher.matches()) {
			Logger.errorf("Duration spec does not match, %s", spec);
			return 0;
		}
		
		Integer h = Integer.parseInt(matcher.group(1));
		Integer m = Integer.parseInt(matcher.group(2));
		Integer s = matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0;
		
		if (h > 23) {
			Logger.errorf("%s: h not in range", spec);
			return 0;
		}
		if (m > 59) {
			Logger.errorf("%s: m not in range", spec);
			return 0;
		}
		if (s > 59) {
			Logger.errorf("%s: s not in range", spec);
			return 0;
		}
		
		return (h * 60 * 60) + (m * 60) + s;
	}
	
	// Intermediate state while loading - keep track of errors
	private boolean errorFlag = false;
	private List<Schedule> schedules;
	private String name;
	private String stream;
	private int duration;
	
	boolean load(File config) {
		boolean parseSuccess = DvrConfig.readConfigFile(config, this::acceptEntry);
		finishStream();
		// The following are now eligible for GC
		schedules = null;
		name = stream = null;
		return parseSuccess & !errorFlag;
	}
	
	// Start a new stream
	private void startNewStream(String name) {
		this.name = name;
		schedules = new ArrayList<Schedule>();
		stream = null;
		duration = 0;
	}
	
	// Finish a stream
	private void finishStream() {
		// Validate
		if (schedules == null || schedules.isEmpty() || duration == 0 || stream == null) {
			Logger.errorf("Incomplete record %s", name);
			errorFlag = true;
			return;
		}
		Schedule sched = null;
		if (schedules.size() == 1)
			sched = schedules.get(0);
		else
			sched = new MultiSchedule(schedules);
		
		add(new RecordTask(name, stream, duration, sched));
	}
	
	private void acceptEntry(String key, String value) {
		// Start/finish a record block
		if (key.equals("Record")) {
			if (name != null) finishStream();
			startNewStream(value);
			return;
		}
		
		// No keys outside a Record
		if (name == null) {
			Logger.errorf("Key %s outside of record", key);
			errorFlag = true;
			return;
		}
		
		// Accept a Stream
		if (key.equals("Stream")) {
			stream = value;
			return;
		}
		
		// Accept a Repeat
		if (key.equals("Repeat")) {
			try {
				schedules.add(new RepeatSchedule(value));
			} catch(IllegalArgumentException e) { errorFlag = true; return; }
		}
		
		// Accept a OneOff
		if (key.equals("OneOff")) {
			try {
				schedules.add(new OneOffSchedule(value));
			} catch(IllegalArgumentException e) { errorFlag = true; return; }
		}
		
		if (key.equals("Duration")) {
			duration = parseDurationSpec(value);
		}
	}
}
