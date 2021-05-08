package impsave;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileAutoSaver implements Runnable {
	private File file;
	private String ext;
	private FileMonitor monitor;
	private byte[] content;
	private Sleeper idler;

	public FileAutoSaver(File file, String ext) throws IOException {
		this.file = file;
		this.ext = ext;
		this.monitor = new FileMonitor(file);
		this.content = Utils.readFile(file);
		this.idler = new Sleeper(50);
	}

	@Override
	public void run() {
		if (!monitor.hasChanged())
			return;

		int numReads = -1;
		try {
			numReads = readUpdatedContent();
			if (numReads < 0)
				return;
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		// TODO: Write the very first one.
		// TODO: Read what's in the folder, so that relaunching doesn't confuse things.
		File historyDir = getBackupDir(file);
		historyDir.mkdir();
		File autosave = chooseAutosaveName(historyDir);
		System.out.printf("Saved [%s/%s] (%d reads)\n", historyDir.getName(), autosave.getName(), numReads);
		saveFile(file.getName(), autosave, content);
		autosave.setLastModified(monitor.lastModifiedDate().getTime());
	}

	private static void saveFile(String fileName, File zipFile, byte[] content) {
		try {
			FileOutputStream fos = new FileOutputStream(zipFile);
			ZipOutputStream zos = new ZipOutputStream(fos);
			ZipEntry ze = new ZipEntry(fileName);
			zos.putNextEntry(ze);
			zos.write(content);
			zos.closeEntry();
			zos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static String formatTurn(int turnNumber) {
		return String.format("%03d", turnNumber);
	}
	public static String formatName(String countryName, int turnNumber) {
		return String.format("%s-%03d", countryName, turnNumber);
	}

	public static String getTurnString(String name, String countryName) {
		// May include the sub turn, like 005-2.
		return name.substring(countryName.length() + 1);
	}

	private File chooseAutosaveName(File historyDir) {
		String filename = null;
		try {
			SaveParser p = new SaveParser(content);
			filename = formatName(p.getCountryName(), p.getTurnNumber());
		} catch (Exception e) {
			e.printStackTrace();
			filename = monitor.lastModifiedDate().toString();
		}
		int tries = 2;
		File autosave = getAutosaveFile(historyDir, filename);
		while (autosave == null) {
			autosave = getAutosaveFile(historyDir, filename + "-" + tries);
			tries++;
		}
		return autosave;
	}

	// Returns null if the specified name is already taken.
	private File getAutosaveFile(File historyDir, String name) {
		String filename = name + ext;
		// Check that the uncompressed version doesn't exist, to avoid name re-use.
		if (new File(historyDir, filename).exists())
			return null;
		File file = new File(historyDir, filename + ".zip");
		return file.exists() ? null : file;
	}

	public static File getBackupDir(File file) {
		return new File(file.getAbsolutePath() + " autosaves");
	}

	private int readUpdatedContent() throws IOException {
		// Find a stable state.
		byte[][] newContent = new byte[2][];
		int index = 0;
		newContent[index++] = Utils.readFile(file);
		if (!content.equals(newContent[0])) {
			do {
				idler.run();
				// index 0 or 1
				newContent[index++ & 1] = Utils.readFile(file);
			} while (!Arrays.equals(newContent[0], newContent[1]));
			content = newContent[0];
			return index;
		}
		return -1;
	}
}
