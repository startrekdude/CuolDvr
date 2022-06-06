package cuoldvr.daemon;

import java.time.Instant;

// Returns the next time a record should happen, or null if none should happen
interface Schedule {
	Instant next();
}
