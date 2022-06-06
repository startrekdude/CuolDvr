package cuoldvr;

import java.io.File;

import cuoldvr.daemon.Daemon;
import cuoldvr.mux.Remuxer;
import cuoldvr.utility.LogLevel;
import cuoldvr.utility.Logger;

// The entrypoint, cmdline parser, task dispatch
public final class CuolDvrProgram {
	private CuolDvrProgram() {} // static class
	
	public static void main(String[] args) {
		// Start the logger
		Logger.filter = LogLevel.INFO;
		
 		// We must have ffmpeg and ffprobe
		if (!Remuxer.testFFmpeg()) {
			System.out.println("No ffmpeg/ffprobe");
			System.exit(0);
		}
		
		// Parse out global options (just the logger atm)
		int i = 0;
		while (i < args.length && args[i].charAt(0) == '-') {
			if (args[i].startsWith("-loglevel:")) {
				String logLevel = args[i].substring(10);
				try {
					Logger.filter = LogLevel.valueOf(logLevel);
				} catch(IllegalArgumentException e) {
					System.out.printf("Unknown log level: %s\n", logLevel);
					System.exit(0);
				}
			} else {
				System.out.printf("Unknown option: %s\n", args[i]);
				System.exit(0);
			}
			i++;
		}
		
		// If no/not enough args, print usage
		if (i == args.length) {
			System.out.println("Usage: CuolDvr.jar [-loglevel:LOGLEVEL] <daemon|remux>");
			System.exit(0);
		}
		
		// Amazing. Now pull out the action and action arguments
		String action = args[i];
		String[] actionArgs = new String[args.length-i-1];
		System.arraycopy(args, i+1, actionArgs, 0, args.length-i-1);
		
		// Dispatch the action
		if (action.equals("daemon")) {
			daemon(actionArgs);
		} else if (action.equals("remux")) {
			remux(actionArgs);
		} else {
			System.out.printf("Unknown action: %s", action);
			System.exit(0);
		}
	}
	
	private static void daemon(String[] args) {
		// Start CuolDvr - ignore args
		Daemon daemon = new Daemon();
		if (!daemon.init()) {
			System.out.println("FAIL Daemon Init");
			System.exit(0);
		}
		daemon.run(); // all yours
	}
	
	private static void remux(String[] args) {
		// Run just the Remuxer, don't worry about the rest
		if (args.length != 2) {
			System.out.println("Usage: CuolDvr.jar [...] remux <directory> <dst.mp4>");
			System.exit(0);
		}
		
		File directory = new File(args[0]);
		File dst = new File(args[1]);
		
		// Make sure the directory actually exists
		if (!directory.isDirectory()) {
			System.out.printf("Not a directory: %s", args[0]);
			System.exit(0);
		}
		
		// Alright, everything checks out, start the remuxer
		Remuxer mux = new Remuxer(directory, dst);
		mux.go();
	}
}