package cuoldvr.daemon;

// Record stream on schedule for duration seconds
final class RecordTask {
	final String name;
	final String stream;
	final int duration;
	final Schedule schedule;
	
	RecordTask(String name, String stream, int duration, Schedule schedule) {
		this.name = name;
		this.stream = stream;
		this.duration = duration;
		this.schedule = schedule;
	}
}
