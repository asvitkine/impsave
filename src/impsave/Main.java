package impsave;

import java.io.File;
import java.io.IOException;

import javax.swing.SwingUtilities;

public class Main implements Runnable {
	private static final String VERSION = "0.24";

	public void run() {
		Context context = new Context();
		String appName = computeAppTitle(context.getJarOrAppFile().getName());
		File folder = context.getSaveDirectory();
		MainPanel c = new MainPanel(appName, folder, context.getImperialismApp());
		c.setVisible(true);
		if (!folder.exists() || !folder.isDirectory()) {
			System.out.println("ERROR: Can't find 'Save' folder.");
			return;
		}
		FolderMonitor monitor = new FolderMonitor(folder, ".imp", new Sleeper(250));
		System.out.println("Monitoring files in: " + folder.getAbsolutePath());
		new Thread(monitor).start();
	}

	private static String computeAppTitle(String appName) {
		String appTitle = "ImpSave " + VERSION;
		if (appName.endsWith(".jar")) {
			appTitle += " (" + appName + ")";
		}
		return appTitle;
	}

	public static void main(String[] args) throws IOException {
		SwingUtilities.invokeLater(new Main());
	}
}