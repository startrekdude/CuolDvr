package cuoldvr.daemon;

import java.time.Instant;
import java.time.Month;
import java.time.Year;
import java.util.HashMap;
import java.util.Map;

import cuoldvr.utility.Logger;
import cuoldvr.utility.ParserUtils;
import cuoldvr.utility.TzConstants;

final class OneOffSchedule implements Schedule {
	private final static Map<String, Month> months;
	static {
		months = new HashMap<String, Month>();
		months.put("Jan", Month.JANUARY);
		months.put("Feb", Month.FEBRUARY);
		months.put("Mar", Month.MARCH);
		months.put("Apr", Month.APRIL);
		months.put("May", Month.MAY);
		months.put("Jun", Month.JUNE);
		months.put("Jul", Month.JULY);
		months.put("Aug", Month.AUGUST);
		months.put("Sep", Month.SEPTEMBER);
		months.put("Oct", Month.OCTOBER);
		months.put("Nov", Month.NOVEMBER);
		months.put("Dec", Month.DECEMBER);
	}
	
	private final Month month;
	private final int year;
	private final int day;
	private final int hour;
	private final int minute;
	
	// Spec "Feb 21 '20 00:20"
	OneOffSchedule(String desc) {
		Month month = null;
		int year = 0;
		int day = 0;
		int hour = 0;
		int minute = 0;
		
		// Month comes first
		int i = desc.indexOf(' ');
		if (i == -1) throwInvalidDesc(desc);
		month = months.getOrDefault(desc.substring(0, i), null);
		if (month == null) throwInvalidDesc(desc);
		
		// Next is day of month
		int j = desc.indexOf(' ', i+1);
		if (j == -1) throwInvalidDesc(desc);
		try {
			day = Integer.parseInt(desc.substring(i+1, j));
		} catch(NumberFormatException e) { throwInvalidDesc(desc); }
		
		// Next is year
		int k = desc.indexOf(' ', j+1);
		if (k == -1) throwInvalidDesc(desc);
		if (desc.charAt(j+1) != '\'') throwInvalidDesc(desc);
		try {
			year = Integer.parseInt(desc.substring(j+2, k))+2000;
		} catch(NumberFormatException e) { throwInvalidDesc(desc); }
		
		// Validate the day of month
		if (Year.of(year).atMonth(month).lengthOfMonth() < day || day < 0)
			throwInvalidDesc(desc);
		
		int[] hm = null;
		try {
			hm = ParserUtils.parseHMSpec(desc.substring(k+1));
		} catch(IllegalArgumentException e) { throwInvalidDesc(desc); }
		hour = hm[0];
		minute = hm[1];
		
		this.month = month;
		this.year = year;
		this.day = day;
		this.hour = hour;
		this.minute = minute;
	}
	
	private void throwInvalidDesc(String desc) {
		String msg = String.format("Invalid one-off desc %s", desc);
		Logger.error(msg);
		throw new IllegalArgumentException(desc);
	}
	
	@Override
	public Instant next() {
		Instant next = Year.of(year)
			.atMonth(month)
			.atDay(day)
			.atTime(hour, minute)
			.atZone(TzConstants.tz)
			.withEarlierOffsetAtOverlap()
			.toInstant();
		if (next.compareTo(Instant.now()) < 0) return null;
		return next;
	}
}