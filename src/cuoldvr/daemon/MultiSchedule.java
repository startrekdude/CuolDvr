package cuoldvr.daemon;

import java.time.Instant;
import java.util.List;

final class MultiSchedule implements Schedule {
	private final Schedule[] schedules;
	
	MultiSchedule(List<Schedule> schedules) {
		this.schedules = schedules.toArray(new Schedule[0]);
	}

	@Override
	public Instant next() {
		Instant next = null;
		for (Schedule schedule : schedules) {
			Instant is = schedule.next();
			if (is == null) continue;
			if (next == null || is.compareTo(next) < 0) next = is;
		}
		return next;
	}
}
