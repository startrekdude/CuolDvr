package cuoldvr.utility;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

// Simple logger class: messages >= log level to stdout, everything to a file
public final class Logger {
	private Logger() {} // static class
	
	private final static DateFormat dateFormat
		= new SimpleDateFormat("yyyy/MM/dd hh:mm:ss");
	
	// Messages lower than filter will not be printed to standard out
	public static LogLevel filter = LogLevel.DEBUG;
	
	public static void log(LogLevel level, String msg) {
		// Prepare the message; include date, verbosity, class/method
		String s = String.format("[%s][%s][%s] %s",
			dateFormat.format(new Date()),
			level,
			getCallerInfo(),
			msg
		);
		
		if (level.compareTo(filter) >= 0) System.out.println(s);
		
		LogRotate.write(s);
	}
	
	// Walk the stack; pick the earliest outside of Logger
	// Slow, but I don't actually care
	private static String getCallerInfo() {
		StackTraceElement[] st = Thread.currentThread().getStackTrace();
		StackTraceElement frame = null;
		for (StackTraceElement ste : st) {
			String clazz = ste.getClassName();
			// Skip Thread, Logger
			if (clazz.equals("cuoldvr.utility.Logger")
				|| clazz.equals("java.lang.Thread")) continue;
			frame = ste; break;
		}
		return String.format("%s::%s", frame.getClassName(), frame.getMethodName());
	}
	
	// These are all fairly straightforward
	public static void logf(LogLevel level, String fmt, Object ... args) {
		log(level, String.format(fmt, args));
	}
	
	public static void debug(String msg) {
		log(LogLevel.DEBUG, msg);
	}
	
	public static void debugf(String fmt, Object ... args) {
		logf(LogLevel.DEBUG, fmt, args);
	}
	
	public static void verbose(String msg) {
		log(LogLevel.VERBOSE, msg);
	}
	
	public static void verbosef(String fmt, Object ... args) {
		logf(LogLevel.VERBOSE, fmt, args);
	}
	
	public static void info(String msg) {
		log(LogLevel.INFO, msg);
	}
	
	public static void infof(String fmt, Object ... args) {
		logf(LogLevel.INFO, fmt, args);
	}
	
	public static void warn(String msg) {
		log(LogLevel.WARN, msg);
	}
	
	public static void warnf(String fmt, Object ... args) {
		logf(LogLevel.WARN, fmt, args);
	}
	
	public static void error(String msg) {
		log(LogLevel.ERROR, msg);
	}
	
	public static void errorf(String fmt, Object ... args) {
		logf(LogLevel.ERROR, fmt, args);
	}
}
