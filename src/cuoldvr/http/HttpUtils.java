package cuoldvr.http;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.ResponseCache;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import cuoldvr.utility.InputStreamUtils;
import cuoldvr.utility.Logger;

// Talks HTTP - sets appropriate headers, redirects, compression
// Gives back byte[] or UTF-8 strings
// Impersonates IE11, as best it can
public final class HttpUtils {
	private HttpUtils() {} // static class
	
	static {
		ResponseCache.setDefault(null); // Disable inbuilt client-side caching
		System.setProperty("sun.net.http.allowRestrictedHeaders", "true"); // THIS ISN'T AN APPLET, USE SANE DEFAULTS
	}
	
	private static final String userAgent = "Mozilla/5.0 (Windows NT 6.3; WOW64; Trident/7.0; rv:11.0) like Gecko";
	
	public static byte[] fetch(URL resource, String origin, String referrer) throws IOException {
		// For logging
		String strUrl = resource.toExternalForm();
		
		// Check the protocol
		String protocol = resource.getProtocol();
		if (!protocol.equals("http") && !protocol.equals("https")) {
			Logger.warnf("URL %s not http, https", strUrl);
			throw new IllegalArgumentException("URL must be http or https");
		}
		
		// Open a connection
		HttpURLConnection conn = (HttpURLConnection)resource.openConnection();
		conn.setRequestMethod("GET");
		conn.setDoInput(true);
		conn.setDoOutput(false);
		
		// Setup the headers
		conn.setRequestProperty("Accept", "*/*");
		conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
		conn.setRequestProperty("Accept-Language", "en-US");
		conn.setRequestProperty("DNT", "1");
		
		// Cache headers go after DNT in real IE
		Optional<HttpCacheEntry> cacheEntry = HttpCache.retrieve(conn);
		
		conn.setRequestProperty("Origin", origin);
		conn.setRequestProperty("Referer", referrer); // No, that /isn't/ the right spelling. 
		conn.setRequestProperty("User-Agent", userAgent);
		conn.setInstanceFollowRedirects(false);
		
		conn.connect();
		
		// Do we redirect?
		int status = conn.getResponseCode();
		if (status == HttpURLConnection.HTTP_MOVED_PERM ||
				status == HttpURLConnection.HTTP_MOVED_TEMP ||
				status == HttpURLConnection.HTTP_SEE_OTHER) {
			URL newUrl = new URL(conn.getHeaderField("Location"));
			Logger.infof("Redirect %s -> %s", strUrl, newUrl.toExternalForm());
			return fetch(newUrl, origin, referrer);
		}
		
		// Is the data available in cache?
		if (status == HttpURLConnection.HTTP_NOT_MODIFIED) {
			Logger.verbosef("URL %s -> Retrieving from cache", strUrl);
			if (cacheEntry.isEmpty()) {
				Logger.errorf("URL %s not in cache", strUrl);
				throw new IllegalStateException("Server sent 304; not requested.");
			}
			return cacheEntry.get().data;
		}
		
		// Compression: gzip has been tested, deflate has not
		String enc = conn.getHeaderField("Content-Encoding");
		Logger.debugf("%s compressed with %s", strUrl, enc);
		byte[] data;
		if ("gzip".equals(enc))
			data = InputStreamUtils.readBytes(new GZIPInputStream(conn.getInputStream()));
		else if ("deflate".equals(enc))
			data = InputStreamUtils.readBytes(new InflaterInputStream(conn.getInputStream(), new Inflater(true)));
		else if (enc == null)
			data = InputStreamUtils.readBytes(conn.getInputStream());
		else
			throw new IOException(String.format("Unsupported encoding: %s", enc));
		
		// Cache the data if needed
		HttpCache.put(conn, data);
		
		return data;
	}	
	
	// Always decodes as UTF-8
	public static String fetchString(URL resource, String origin, String referrer) throws IOException {
		return new String(fetch(resource, origin, referrer), StandardCharsets.UTF_8);
	}
}