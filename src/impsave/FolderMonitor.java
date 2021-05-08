package impsave;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

public class FolderMonitor implements Runnable {
	private File folder;
	private String ext;
	private Runnable idler;

	public FolderMonitor(File folder, String ext, Runnable idler) {
		this.folder = folder;
		this.ext = ext;
		this.idler = idler;
	}
	
	public void run() {
		HashMap<File, FileAutoSaver> savers = new HashMap<File, FileAutoSaver>();
		while (true) {
			for (File file : folder.listFiles()) {
				if (!file.isFile() || file.isHidden() || !file.getName().endsWith(ext))
					continue;
				// FIXME: Don't throw exceptions from here.
				try {
					monitorFile(savers, file);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			idler.run();
		}
	}
	
	private void monitorFile(HashMap<File, FileAutoSaver> savers, File file) throws IOException {
		FileAutoSaver saver = savers.get(file);
		if (saver == null) {
			System.out.println("Found new file: " + file.getName());
			saver = new FileAutoSaver(file, ext);
			savers.put(file, saver);
		}

		saver.run();
	}
}
