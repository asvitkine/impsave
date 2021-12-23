package impsave;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Date;

public class FileMonitor {
	private File file;
	private long lastModified;

	public FileMonitor(File file) throws FileNotFoundException {
		this.file = file;
		if (!file.exists())
			throw new FileNotFoundException(file.getAbsolutePath());
	}

	public Date lastModifiedDate() {
		return new Date(lastModified);
	}

	public boolean hasChanged() {
		long lastModified = file.lastModified();
		if (lastModified != this.lastModified) {
			this.lastModified = lastModified;
			return true;
		}
		return false;
	}
}
