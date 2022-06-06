package cuoldvr.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.zip.GZIPOutputStream;

// Handles writing to log files, and rotating them as neccessary
final class LogRotate {
	private LogRotate() {} // static class
	
	// Write to writer until day ticks over, then make a new writer
	private static PrintWriter writer;
	private static LocalDate day;
	
	private static final File directory = new File("logs");
	private static final File logFile = new File("logs/latest.log");
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'.log.gz'");
	
	static {
		// Make the directory, and also, what time is it?
		directory.mkdir();
		day = LocalDate.now();
		
		// Possibly archive an old log file, if needed
		LocalDate oldDate = LocalDate.ofInstant(Instant.ofEpochMilli(logFile.lastModified()), ZoneId.of("UTC"));
		if (logFile.exists() && oldDate.compareTo(day) < 0) {
			archiveLogFile(oldDate);
		}
		
		try { writer = new PrintWriter(new FileWriter(logFile, true)); }
		catch(IOException e) { e.printStackTrace(); }
		Runtime.getRuntime().addShutdownHook(new Thread(writer::close));
	}
	
	// GZIP and archive an old log file
	private static void archiveLogFile(LocalDate date) {
		// What name should the file use?
		File archive = directory.toPath().resolve(date.format(formatter)).toFile();
		
		// Copy over the file, compressing it
		try {
			OutputStream os = new GZIPOutputStream(new FileOutputStream(archive));
			InputStream is = new FileInputStream(logFile);
			InputStreamUtils.copy(is, os);
			os.close();
		} catch(IOException e) { e.printStackTrace(); }
		
		// Done with this one
		logFile.delete();
	}
	
	// Synchronized: only one thread can write to the file at any given time
	synchronized static void write(String s) {
		LocalDate now = LocalDate.now();
		if (now.compareTo(day) > 0) {
			// Rotate the log file
			writer.close();
			archiveLogFile(day);
			day = now;
			try { writer = new PrintWriter(new FileWriter(logFile)); }
			catch(IOException e) { e.printStackTrace(); }
		}
		
		writer.println(s);
		writer.flush();
	}
}