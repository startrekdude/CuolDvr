package cuoldvr.daemon;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Map;

import cuoldvr.utility.Logger;
import cuoldvr.utility.ParserUtils;
import cuoldvr.utility.TzConstants;

final class RepeatSchedule implements Schedule {
	private final static Map<String, DayOfWeek> days;
	static {
		days = new HashMap<String, DayOfWeek>();
		days.put("Mon", DayOfWeek.MONDAY);
		days.put("Tue", DayOfWeek.TUESDAY);
		days.put("Wed", DayOfWeek.WEDNESDAY);
		days.put("Thu", DayOfWeek.THURSDAY);
		days.put("Fri", DayOfWeek.FRIDAY);
		days.put("Sat", DayOfWeek.SATURDAY);
		days.put("Sun", DayOfWeek.SUNDAY);
	}
	
	private final DayOfWeek day;
	private final int hour;
	private final int minute;
	
	// Spec: "Thu 00:25"
	RepeatSchedule(String desc) {
		DayOfWeek day = null;
		int hour = 0;
		int minute = 0;
		
		// Split on the first space
		int i = desc.indexOf(' ');
		if (i == -1) throwInvalidDesc(desc);
		day = days.getOrDefault(desc.substring(0, i), null);
		if (day == null) throwInvalidDesc(desc);
		
		int[] hm = null;
		try {
			hm = ParserUtils.parseHMSpec(desc.substring(i+1));
		} catch(IllegalArgumentException e) { throwInvalidDesc(desc); }
		hour = hm[0];
		minute = hm[1];
		
		this.day = day;
		this.hour = hour;
		this.minute = minute;
	}
	
	private void throwInvalidDesc(String desc) {
		String msg = String.format("Invalid repeat schedule desc %s", desc);
		Logger.error(msg);
		throw new IllegalArgumentException(msg);
	}
	
	@Override
	public Instant next() {
		// Ayeet, the meat of this class
		ZonedDateTime next = ZonedDateTime.now(TzConstants.tz);
		if (next.getHour() < hour || (next.getHour() == hour && next.getMinute() < minute)) {
			next = next.with(TemporalAdjusters.nextOrSame(day));
		} else {
			next = next.with(TemporalAdjusters.next(day));
		}
		
		return next.with(LocalTime.of(hour, minute))
			.withEarlierOffsetAtOverlap().toInstant();
	}

}
