package impsave;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Calendar;

import javax.swing.*;

public class LaunchPanel extends JPanel implements ActionListener {
	private File imperialismApp;
	private JLabel statusLabel;
	private JButton playButton;
	private JButton updateButton;
	private Patcher patcher;

	public LaunchPanel(File imperialismApp) {
		this.imperialismApp = imperialismApp;
		statusLabel = new JLabel("Checking Imperialism version...");
		playButton = new JButton("Play");
		playButton.addActionListener(this);
		playButton.setEnabled(false);
		updateButton = new JButton("Update");
		updateButton.addActionListener(this);
		updateButton.setEnabled(false);

		add(statusLabel);
		add(playButton);
		add(updateButton);

		checkVersion();
	}

	private void checkVersion() {
		patcher = new Patcher(imperialismApp);
		switch (patcher.getState()) {
		case Patcher.FILE_MISSING:
			statusLabel.setText("Imperialism not found!");
			break;
		case Patcher.FILE_OLD:
			statusLabel.setText("Imperialism needs to be updated.");
			updateButton.setEnabled(true);
			break;
		case Patcher.FILE_UNKNOWN:
			statusLabel.setText("Warning: Unknown Imperialism version.");
			break;
		case Patcher.FILE_UP_TO_DATE:
			statusLabel.setText("Imperialism is up to date!");
			playButton.setEnabled(true);
			break;
		case Patcher.FILE_ERROR:
		default:
			statusLabel.setText("Error reading Imperialism file.");
		}
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == updateButton) {
			updateButton.setEnabled(false);
			if (!patcher.patch()) {
				JOptionPane.showMessageDialog(this, "Patching failed!");
			} else {
				JOptionPane.showMessageDialog(this, "Imperialism patched!");
			}
			checkVersion();
			return;
		}

		int year = Calendar.getInstance().get(Calendar.YEAR);
		// Note: Check for > 2036 instead of 2037 to avoid the case where there may
		// be a year roll-over after launching Imperialism.
		if (year > 2036) {
			JOptionPane.showMessageDialog(this, "Please change your system to date to below 2037, or Imperialism will crash!");
			return;
		}

		playButton.setEnabled(false);
		new Thread() {
			public void run() {
				ProcessBuilder pb = new ProcessBuilder();
				pb.command(imperialismApp.getAbsolutePath());
				pb.directory(imperialismApp.getParentFile());
				pb.redirectErrorStream(true);
				try {
					Process p = pb.start();
					BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
					while ((reader.readLine()) != null) {
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				playButton.setEnabled(true);
			}
		}.start();
	}
}
