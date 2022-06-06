package cuoldvr.http;

class HttpCacheEntry {
	final String lastModified;
	final String etag;
	final byte[] data;
	
	HttpCacheEntry(String lastModified, String etag, byte[] data) {
		this.lastModified = lastModified;
		this.etag = etag;
		this.data = data;
	}
}