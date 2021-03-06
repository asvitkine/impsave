package impsave;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

		addWindowListener(new WindowAdapter() {
			@Override
		    public void windowDeactivated(WindowEvent e) {
				// On background, switch back to the log panel so that
				// switching to the restore panel triggers a reload.
				// Only do this when opposite window is null, so that message
				// boxes in our app don't trigger it.
				if (e.getOppositeWindow() == null) {
					tabs.setSelectedIndex(0);
				}
			}
		});
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
