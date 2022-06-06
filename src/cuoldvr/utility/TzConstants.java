package cuoldvr.utility;

import java.time.ZoneId;

// Yeah, for this particular app, we just hardcode the timezone to Toronto
// This will create a problem if:
//  1) Toronto ever has a different timezone than Ottawa
//  2) Carleton relocates
// Also, to be fair, not hard to change as long as only one zone is needed
public final class TzConstants {
	private TzConstants() {} // static class
	
	public final static ZoneId tz = ZoneId.of("America/Toronto");
}
