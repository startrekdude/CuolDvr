package cuoldvr.daemon;

import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cuoldvr.hls.Recorder;
import cuoldvr.mux.Remuxer;
import cuoldvr.utility.Logger;
import cuoldvr.utility.RandomUtils;
import cuoldvr.utility.ThreadUtils;
import cuoldvr.utility.TzConstants;

// The CuolDvr Daemon
// Manages Record tasks, starting and ending them (on different threads) as appropriate
// (also kicks off the muxer)
public final class Daemon {
	// The types of events we can handle
	private abstract static class Event {
		final Instant time;
		private Event(Instant time) { this.time = time; }
	}
	private static class BeginEvent extends Event {
		final RecordTask task;
		private BeginEvent(Instant time, RecordTask task) {
			super(time);
			this.task = task;
		}
	}
	private static class EndEvent extends BeginEvent {
		private final Recorder recorder;
		private EndEvent(Instant time, RecordTask task, Recorder recorder) {
			super(time, task);
			this.recorder = recorder;
		}
	}
	
	// Where are the config files?
	private final static File streamsConfig = new File("streams.cfg");
	private final static File tasksConfig = new File("record.cfg");
	
	// The streams we know about, and when we need to record them
	private boolean initialized = false;
	private Map<String, StreamDescriptor> streams;
	
	// The actual task details are just inside the event stream, turns out they don't need to be anywhere else
	// The class is built around this structure
	private final List<Event> eventStream;
	
	public Daemon() {
		// Insertion performance, but, like, meh. Lists won't ever have more than a few hundred, even that's a lot
		eventStream = new LinkedList<Event>();
	}
	
	// Boot the daemon, true for success, false for failure
	public boolean init() {
		boolean success = true;
		
		// Alias the instance variables to use a more rich type for now
		StreamConfig streams = new StreamConfig();
		success &= streams.load(streamsConfig);
		RecordConfig tasks = new RecordConfig();
		success &= tasks.load(tasksConfig);
		this.streams = streams;
		
		// Done, maybe. Now check that every stream actually exists
		for (RecordTask task : tasks) {
			if (!streams.containsKey(task.stream)) {
				Logger.errorf("Stream %s doesn't exist", task.stream);
				success = false;
			}
		}
		
		// Do we have at least one task? If not, this is bad!
		if (tasks.isEmpty()) {
			Logger.error("No tasks");
			return false;
		}
		
		// Make every event for now (just begin events)
		for (RecordTask task : tasks) {
			Instant time = task.schedule.next();
			if (time == null) continue; // Fair enough - user can kill dead configs
			if (eventStream.isEmpty()) eventStream.add(new BeginEvent(time.minusSeconds(RandomUtils.inRange(0, 23)), task)); // Get the ball rolling
			else insertEvent(new BeginEvent(time.minusSeconds(RandomUtils.inRange(0, 23)), task));
		}
		logEventStream();
		
		initialized = success;
		return success;
	}
	
	public void run() {
		if (!initialized)
			throw new IllegalStateException("Daemon must be initialized");
		// Let's do this
		while (!eventStream.isEmpty()) {
			Event evt = eventStream.remove(0);
			waitForEvent(evt);
			// Do EndEvent /first/ - it's a subclass of BeginEvent (go figure)
			if (evt instanceof EndEvent) handleEndEvent((EndEvent)evt);
			else if (evt instanceof BeginEvent) handleBeginEvent((BeginEvent)evt);
		}
		Logger.info("No more events, exiting");
	}
	
	private boolean available(File f) {
		// Can we use f, or is it taken?
		File[] siblings = f.getParentFile().listFiles();
		if (siblings == null) return true;
		for (File sibling : siblings) {
			if (sibling.getName().startsWith(f.getName())) {
				return false;
			}
		}
		return true;
	}
	
	private File nextFile(String format) {
		// What's the next available file? 01, 02, 03, etc
		File f = null;
		int i = 1; // for humans :(
		do {
			f = new File(String.format(format, i));
			i++;
		} while(!available(f));
		return f;
	}
	
	private void handleBeginEvent(BeginEvent be) {
		// Start recording - and make sure to end recording at some point, also
		StreamDescriptor sd = streams.get(be.task.stream);
		File directory = nextFile(be.task.name + "-scratch/" + "%02d");
		LocalDateTime now = LocalDateTime.now();
		directory = new File(String.format("%s-%s,%s,%s,%s",
			directory.getPath(), now.getMonth().getValue(), now.getDayOfMonth(), now.getHour(), now.getMinute()));
		Recorder recorder = new Recorder(directory.getPath(), sd.stream, sd.origin, sd.referrer);
		recorder.begin(); // Start recording - now, when should we end
		
		// Make the end event
		Instant endTime = be.time.plusSeconds(be.task.duration);
		EndEvent ee = new EndEvent(endTime.plusSeconds(23), be.task, recorder);
		insertEvent(ee);
		logEventStream();
	}
	
	private void handleEndEvent(EndEvent ee) {
		// Stop recording, before anything else
		ee.recorder.end();
		
		// Now, we mux
		File directory = new File(ee.task.name);
		directory.mkdir();
		File out = nextFile(ee.task.name + "/" + ee.task.name + "-%02d.mp4");
		Remuxer mux = new Remuxer(ee.recorder.directory, out);
		mux.go();
		
		// Potentially make another begin event
		Instant next = ee.task.schedule.next();
		if (next == null) return;
		BeginEvent be = new BeginEvent(next.minusSeconds(RandomUtils.inRange(0, 23)), ee.task);
		insertEvent(be);
		logEventStream();
	}
	
	private void waitForEvent(Event e) {
		// Wait until it's time to handle the event, then return
		/*long*/ long time = //ago, in a galaxy far far away
			e.time.toEpochMilli();
		// We have to worry about precision here - don't sleep in too large chunks
		while (true) {
			long now = Instant.now().toEpochMilli();
			long diff = time - now;
			if (diff <= 0) break; 
			else if (diff > 1800000) ThreadUtils.sleep(300000);
			else if (diff > 600000) ThreadUtils.sleep(60000);
			else if (diff > 300000) ThreadUtils.sleep(15000);
			else if (diff > 60000) ThreadUtils.sleep(5000);
			else ThreadUtils.sleep(10);
		}
	}
	
	private void insertEvent(Event e) {
		// Insert the event at the appropriate time in sequence
		int i = 0;
		for (Event evt : eventStream) {
			if (evt.time.compareTo(e.time) > 0) break;
			i++;
		}
		eventStream.add(i, e);
	}
	
	private void logEventStream() {
		// Logs the event stream (go figure)
		for (Event evt : eventStream) {
			if (evt instanceof EndEvent){
				EndEvent endEvt = (EndEvent) evt;
				Logger.infof("Event: End %s for %s at %s", endEvt.recorder, endEvt.task.name, evt.time.atZone(TzConstants.tz));
			} else if (evt instanceof BeginEvent) {
				BeginEvent beginEvt = (BeginEvent) evt;
				Logger.infof("Event: Begin %s at %s", beginEvt.task.name, evt.time.atZone(TzConstants.tz));
			}
		}
	}
}