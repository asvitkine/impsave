package impsave;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

public class MainPanel extends JFrame implements ChangeListener {
	private JTabbedPane tabs;
	private RestoreTreePanel restoreTreePanel;

	public MainPanel(String title, String footer, File saveFolder, File imperialismApp) {
		super(title);

		tabs = new JTabbedPane();
		tabs.addTab("Log", new Console());
		tabs.addChangeListener(this);
		restoreTreePanel = new RestoreTreePanel(saveFolder);
		tabs.add("Restore", restoreTreePanel);

		getContentPane().add(new LaunchPanel(imperialismApp), BorderLayout.NORTH);
		getContentPane().add(tabs);
		JLabel label = new JLabel(footer, SwingConstants.CENTER);
		label.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
		getContentPane().add(label, BorderLayout.SOUTH);

		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setSize(620, 440);
		setMinimumSize(new Dimension(620, 440));
		setLocationRelativeTo(null);
	}

	@Override
	public void stateChanged(ChangeEvent arg0) {
		if (tabs.getSelectedComponent() == restoreTreePanel) {
			restoreTreePanel.load();
		} else {
			restoreTreePanel.clear();
		}
	}

	public SaveDb getSaveDb() {
		return restoreTreePanel.getSaveDb();
	}
}
