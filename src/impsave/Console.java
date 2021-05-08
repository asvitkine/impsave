package impsave;

import java.awt.BorderLayout;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.Scanner;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingWorker;

public class Console extends JPanel {
	private JTextArea log;

	private final PipedInputStream inPipe = new PipedInputStream();
	private final PipedInputStream outPipe = new PipedInputStream();

	public Console() {
		System.setIn(inPipe);

		try {
			System.setOut(new PrintStream(new PipedOutputStream(outPipe), true));
		} catch (IOException e) {
			System.out.println("Error: " + e);
			return;
		}

		log = new JTextArea();
		log.setEditable(false);
		setLayout(new BorderLayout());
		add(new JScrollPane(log));

		(new SwingWorker<Void, String>() {
			protected Void doInBackground() throws Exception {
				Scanner s = new Scanner(outPipe);
				while (s.hasNextLine()) {
					String line = s.nextLine();
					publish(line);
				}
				s.close();
				return null;
			}

			@Override
			protected void process(java.util.List<String> chunks) {
				for (String line : chunks) {
					if (line.length() < 1)
						continue;
					log.append(line.trim() + "\n");
				}
			}
		}).execute();
	}
}
