package cuoldvr.daemon;

import java.net.URL;

// Describes a recordable stream
final class StreamDescriptor {
	final URL stream;
	final String origin;
	final String referrer;
	
	StreamDescriptor(URL stream, String origin, String referrer) {
		this.stream = stream;
		this.origin = origin;
		this.referrer = referrer;
	}
}
