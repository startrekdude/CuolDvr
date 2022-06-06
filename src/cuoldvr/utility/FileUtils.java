package cuoldvr.utility;

import java.io.File;
import java.util.UUID;

// Get a temp file without making it
public final class FileUtils {
	public static File tempSibling(File file, String extension) {
		return file.getAbsoluteFile().getParentFile().toPath()
			.resolve(String.format("%s.%s", UUID.randomUUID(), extension)).toFile();
	}
}
