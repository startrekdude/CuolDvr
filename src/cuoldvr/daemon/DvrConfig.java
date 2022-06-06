package cuoldvr.daemon;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.BiConsumer;

import cuoldvr.utility.InputStreamUtils;
import cuoldvr.utility.LineIterator;
import cuoldvr.utility.Logger;

// Generic config parser - returns k/v pairs via method calls
final class DvrConfig {
	private DvrConfig() {} // static class
	
	private static final boolean parseConfig(String s, BiConsumer<String, String> acceptEntry) {
		// Ayeet, let's do this
		for (String line : new LineIterator(s)) {
			// Skip empty line
			if (line.isEmpty()) continue;
			
			// Skip comments
			if (line.charAt(0) == '#') continue;
			
			// Skip leading spaces
			int i = 0;
			while (i < line.length() && line.charAt(i) == ' ') i++;
			if (i == line.length()) continue;
			
			// Parse out the key and value
			int sep = line.indexOf(' ', i);
			if (sep == -1) {
				Logger.warnf("Invalid config line: %s", line);
				return false;
			}
			String key = line.substring(i, sep);
			
			// Remove leading spaces from value
			i = sep;
			while (i < line.length() && line.charAt(i) == ' ') i++;
			String value = line.substring(i);
			if (value.isEmpty()) {
				Logger.warnf("Invalid config line: %s", line);
				return false;
			}
			
			acceptEntry.accept(key, value);
		}
		return true;
	}
	
	static final boolean readConfigFile(File f, BiConsumer<String, String> acceptEntry) {
		// Check the parameter
		if (!f.isFile()) {
			Logger.errorf("%s does not exist or is a directory", f);
			return false;
		}
		
		// Read in the config - always ASCII
		String config;
		try {
			InputStream is = new FileInputStream(f);
			config = new String(InputStreamUtils.readBytes(is), StandardCharsets.US_ASCII);
		} catch(IOException e) {
			Logger.errorf("Reading %s, IOException, %s", f, e.getMessage());
			return false;
		}
		
		return parseConfig(config, acceptEntry);
	}
}
