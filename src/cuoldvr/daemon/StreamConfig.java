package cuoldvr.daemon;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;

import cuoldvr.utility.Logger;

final class StreamConfig extends HashMap<String, StreamDescriptor> {
	// Meh, why not - but maybe don't try to serialize this
	private static final long serialVersionUID = -523937882558904111L;
	// Various state
	private boolean errorFlag = false;
	private String name = null;
	private URL url;
	private String origin;
	private String referrer;
	
	boolean load(File config) {
		boolean parseSuccess = DvrConfig.readConfigFile(config, this::acceptEntry);
		finishStream();
		// GC intermediate state
		name = origin = referrer = null;
		url = null;
		return parseSuccess && !errorFlag;
	}
	
	private void finishStream() {
		if (name == null) return;
		if (url == null || origin == null || referrer == null) {
			errorFlag = true;
			Logger.errorf("Incomplete stream %s", name);
		}
		StreamDescriptor sd = new StreamDescriptor(url, origin, referrer);
		url = null;
		origin = referrer = null;
		put(name, sd);
	}
	
	private void acceptEntry(String key, String value) {
		// Finish/start a stream
		if (key.equals("Stream")) {
			finishStream();
			name = value;
			return;
		}
		
		// No keys without a stream
		if (name == null) {
			Logger.errorf("%s outside stream", key);
			errorFlag = true;
			return;
		}
		
		// Accept a origin
		if (key.equals("Origin")) {
			origin = value;
			if (!origin.startsWith("http"))
				Logger.warnf("Origin for %s doesn't start with http", name);
		}
		
		// Accept a referrer
		if (key.equals("Referrer")) {
			referrer = value;
			if (!referrer.startsWith("http"))
				Logger.warnf("Referrer for %s doesn't start with http", name);
		}
		
		// Accept a URL
		if (key.equals("Url")) {
			try {
				url = new URL(value);
				String protocol = url.getProtocol();
				if (!protocol.equals("http") && !protocol.equals("https")) {
					Logger.errorf("Bad protocol %s", protocol);
					errorFlag = true;
				}
			} catch(MalformedURLException e) {
				Logger.errorf("Malformed url: %s", value);
				errorFlag = true;
			}
		}
	}
}