package cuoldvr.utility;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

// Various utitlies for reading from an InputStream - consumes (closes) the stream
public final class InputStreamUtils {
	private InputStreamUtils() {} // static class
	
	public static String readString(InputStream is) throws IOException {
		return new String(readBytes(is), StandardCharsets.UTF_8); // UTF-8, why not?
	}

	// Copy an input stream to a byte array
	public static byte[] readBytes(InputStream is) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		
		int len;
		byte[] scratch = new byte[1024];
		while ((len = is.read(scratch, 0, scratch.length)) != -1) {
			out.write(scratch, 0, len);
		}
		
		out.flush();
		is.close(); // Close the stream
		return out.toByteArray();
	}
	
	public static void copy(InputStream is, OutputStream os) throws IOException {
		byte[] buf = new byte[8192];
		int len;
		
		while ((len = is.read(buf)) != -1)
			os.write(buf, 0, len);
		
		is.close();
	}
}
