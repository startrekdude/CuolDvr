package cuoldvr.http;

import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import cuoldvr.utility.Logger;

// A simple HTTP cache, supporting Last-Modified and ETags
// Handles storing responses, and sending/receiving appropriate headers
final class HttpCache {
	private HttpCache() {} // static class
	
	// Keyed by the URL
	private static Map<String, SoftReference<HttpCacheEntry>> cache =
			Collections.synchronizedMap(new HashMap<String, SoftReference<HttpCacheEntry>>());
	
	// Return a strong reference to the entry to prevent garbage collection until no longer needed
	// Enrich the connection with the headers
	static Optional<HttpCacheEntry> retrieve(HttpURLConnection conn) {
		String key = conn.getURL().toExternalForm();
		if (!cache.containsKey(key)) return Optional.empty();
		
		// Create a strong reference here
		HttpCacheEntry data = cache.get(key).get();
		if (data == null) return Optional.empty(); // Garbage collected
		
		Logger.debugf("URL %s in cache", key);
		conn.setRequestProperty("If-Modified-Since", data.lastModified);
		conn.setRequestProperty("If-None-Match", data.etag);
		return Optional.of(data);
	}
	
	static void put(HttpURLConnection conn, byte[] data) {
		String etag = conn.getHeaderField("ETag");
		String lastModified = conn.getHeaderField("Last-Modified");
		
		if (etag == null && lastModified == null) return; // Nothing to do
		
		HttpCacheEntry entry = new HttpCacheEntry(
			lastModified == null ? "" : lastModified,
			etag == null ? "" : etag,
			data);
		SoftReference<HttpCacheEntry> reference = new SoftReference<HttpCacheEntry>(entry);
		String key = conn.getURL().toExternalForm();
		
		Logger.debugf("Adding %s to cache", key);
		cache.put(key, reference);
	}
}
