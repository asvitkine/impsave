package impsave;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;

public class InfoPanel extends JPanel implements ItemListener, KeyListener, ActionListener, Runnable {
	private SaveDb saveDb;
	private GameInfo info;
	private JTextField gameNameField;
	private JComboBox<TurnEntry> turnSelector;
	private JButton[] restoreButtons;
	private JTextArea comment;
	private Timer timer;

	public InfoPanel(SaveDb saveDb) {
		this.saveDb = saveDb;
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		// Save comments after 0.5s after last key type.
		timer = new Timer(500, this);
		timer.setRepeats(false);
		saveDb.addSaveGameNameListener(this);
	}

	@Override
	public void run() {
		itemStateChanged(null);
	}

	private void saveComments() {
		saveDb.setComment(info.gameId, comment.getText());
		try {
			saveDb.save();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public void keyPressed(KeyEvent event) {
	}

	@Override
	public void keyReleased(KeyEvent event) {
	}

	@Override
	public void keyTyped(KeyEvent event) {
		timer.restart();
	}

	@Override
	public void actionPerformed(ActionEvent event) {
		if (event.getSource() == timer) {
			saveComments();
			return;
		}

		String gameName = gameNameField.getText();
		if (!Charset.forName("US-ASCII").newEncoder().canEncode(gameName)) {
			JOptionPane.showMessageDialog(null, "Invalid save game name: " + gameName);
			return;
		}
		// Restore button clicked.

		// Copy over the selected file...
		JButton button = (JButton) event.getSource();
		String targetName = button.getName();
		File file = getFileToRestore();
		System.out.printf("Activating [%s/%s]\n", file.getParentFile().getName(), file.getName());
		try {
			saveDb.activateFile(file, targetName, gameName);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(this, "Could not restore game.");
			System.err.println("Error activating: " + e.getMessage());
			return;
		}
		System.out.println("Activated as: " + targetName);
		JOptionPane.showMessageDialog(this, "Game selected for restore.");
	}

	private void addRow(String label, JComponent right) {
		JPanel row = new JPanel();
		row.setLayout(new BorderLayout());
		JLabel left = new JLabel("<html><b>" + label + "</b>: ");
		row.add(left, BorderLayout.WEST);
		right.setPreferredSize(new Dimension(200, 8));
		row.add(right, BorderLayout.EAST);
		add(row);
		add(Box.createVerticalStrut(2));
	}

	private JTextField createReadOnlyField(String label) {
		JTextField field = new JTextField(label);
		field.setEditable(false);
		return field;
	}

	public void setGameInfo(GameInfo info) {
		// Save off comments if a timer is pending.
		if (timer.isRunning()) {
			timer.stop();
			saveComments();
		}

		this.info = info;
		removeAll();

		if (info == null) {
			revalidate();
			repaint();
			return;
		}

		addRow("Game ID", createReadOnlyField(info.gameId));
		addRow("Country", createReadOnlyField(info.countryName));
		gameNameField = new JTextField();
		addRow("Turn", createTurnComboBox());
		addRow("Game Name", gameNameField);

		add(new JLabel("<html><b>Comment</b>:"));
		comment = new JTextArea(saveDb.getComment(info.gameId));
		comment.addKeyListener(this);
		JScrollPane scroll = new JScrollPane(comment);
		scroll.setPreferredSize(new Dimension(1024, 5000));
		add(scroll);

		if (SaveDb.JOINED_GAME_PREFIX.equals(info.fileNamePrefix)) {
			add(Box.createVerticalStrut(20));
			restoreButtons = new JButton[] { new JButton() };
			restoreButtons[0].addActionListener(this);
			add(restoreButtons[0]);
		} else {
			add(Box.createVerticalStrut(5));
			JPanel grid = new JPanel();
			grid.setBorder(BorderFactory.createCompoundBorder(
					BorderFactory.createTitledBorder("Restore to:"),
					BorderFactory.createEmptyBorder(2, 2, 2, 2)));
			grid.setLayout(new GridLayout(5, 2));
			JButton button = new JButton("- Autosave -");
			String targetFile = info.fileNamePrefix + "A.imp";
			button.setName(targetFile);
			button.setToolTipText("Restore to " + targetFile);
			button.addActionListener(this);
			grid.add(button);
			grid.add(new JPanel());
			restoreButtons = new JButton[8];
			for (int i = 0; i < 8; i++) {
				button = new JButton();
				button.setHorizontalAlignment(SwingConstants.LEFT);
				button.addActionListener(this);
				restoreButtons[i] = button;
			}
			int[] order = {0,4,1,5,2,6,3,7};
			for (int i = 0; i < 8; i++) {
				grid.add(restoreButtons[order[i]]);
			}
			add(grid);
		}
		itemStateChanged(null);
		revalidate();
		repaint();
	}

	private String getRestoreButtonNamePrefix(int index) {
		return "Slot " + index + ": ";
	}

	@Override
	public void itemStateChanged(ItemEvent unused) {
		if (restoreButtons == null) {
			return;
		}
		File fileToRestore = getFileToRestore();
		int i = 0;
		for (JButton button : restoreButtons) {
			if (restoreButtons.length == 8) {
				if (SaveDb.HOSTED_GAME_PREFIX.equals(info.fileNamePrefix)) {
					button.setText(getRestoreButtonNamePrefix(i) + saveDb.getHostedSaveGameName(i));
				} else {
					button.setText(getRestoreButtonNamePrefix(i) + saveDb.getSoloSaveGameName(i));
				}
				String targetFile = saveDb.getFilename(info.fileNamePrefix, i);
				button.setName(targetFile);
				button.setToolTipText("Restore to " + targetFile);
				i++;
			} else {
				button.setText("  Select for Restore  ");
				String targetFile = Utils.truncateAtChar(fileToRestore.getParentFile().getName(), ' ');
				button.setName(targetFile);
				button.setToolTipText("Restore to " + targetFile);
			}
			button.setEnabled(true);
		}
		gameNameField.setText(saveDb.getGameInfo(fileToRestore).getSavedGameName());
	}

	private void add(JComponent c) {
		c.setAlignmentX(0f);
		super.add(c);
	}

	private class TurnEntry {
		public final String turn;
		public final File file;

		public TurnEntry(String turn, File file) {
			this.turn = turn;
			this.file = file;
		}
		@Override
		public String toString() {
			String label = turn;
			return label;
		}
	}

	private JComboBox<TurnEntry> createTurnComboBox() {
		turnSelector = new JComboBox<TurnEntry>();
		String[] turns = new String[info.turnToFile.size()];
		info.turnToFile.keySet().toArray(turns);
		Arrays.sort(turns);
		for (String turn : turns)
			turnSelector.addItem(new TurnEntry(turn, info.turnToFile.get(turn)));
		turnSelector.setSelectedItem(turnSelector.getItemAt(turns.length - 1));
		Dimension size = turnSelector.getPreferredSize();
		size.width = 120;
		turnSelector.setMinimumSize(size);
		turnSelector.setPreferredSize(size);
		turnSelector.setMaximumSize(size);
		turnSelector.addItemListener(this);
		return turnSelector;
	}

	public File getFileToRestore() {
		TurnEntry entry = (TurnEntry) turnSelector.getSelectedItem();
		return entry.file;
	}
}