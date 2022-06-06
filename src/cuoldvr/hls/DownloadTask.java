package cuoldvr.hls;

import java.io.File;
import java.io.FileOutputStream;
import java.net.URL;

import cuoldvr.http.HttpUtils;
import cuoldvr.utility.Logger;
import cuoldvr.utility.RandomUtils;
import cuoldvr.utility.ThreadUtils;

// Download src to dst, on a /different/ thread
final class DownloadTask implements Runnable {
	private final URL src;
	private final String origin;
	private final String referrer;
	private final File dst;
	
	DownloadTask(URL src, String origin, String referrer, File dst) {
		this.src = src;
		this.origin = origin;
		this.referrer = referrer;
		this.dst = dst;
	}
	
	void execute() {
		// Fairly basic
		Thread thread = new Thread(this);
		thread.setName(String.format("%s -> %s", src, dst));
		thread.start();
	}

	@Override
	public void run() {
		// NOTE: download to a temp file, then move over the real file
		File tmp = null;
		for (int i = 1; i < 15; i++) {
			try {
				if (tmp == null) tmp = File.createTempFile("dltmp", ".tmp", dst.getParentFile());
				byte[] data = HttpUtils.fetch(src, origin, referrer);
				FileOutputStream os = new FileOutputStream(tmp);
				os.write(data);
				os.close();
				tmp.renameTo(dst);
				Logger.infof("Completed: %s (%s)", dst, tmp);
				return;
			} catch(Throwable e) {
				// I suppose this can fail - intelligent backoff
				Logger.warnf("%s -> %s: %s: %s", src, dst, e.getClass().getName(), e.getMessage());
				int sleepLen = RandomUtils.inRange(i*100, 1500+i*100) * i;
				Logger.warnf("Sleeping for: %s, %s", i, sleepLen);
				ThreadUtils.sleep(sleepLen);
			}
		}
		Logger.errorf("Failure: %s", dst);
	}
}
