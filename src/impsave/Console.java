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
	private final PipedInputStream errPipe = new PipedInputStream();

	public Console() {
		System.setIn(inPipe);

		try {
			System.setOut(new PrintStream(new PipedOutputStream(outPipe), true));
			System.setErr(new PrintStream(new PipedOutputStream(errPipe), true));
		} catch (IOException e) {
			System.out.println("Error: " + e);
			return;
		}

		log = new JTextArea();
		log.setEditable(false);
	    log.setTabSize(2);
		setLayout(new BorderLayout());
		add(new JScrollPane(log));

		new StreamProcessor(outPipe).execute();
		new StreamProcessor(errPipe).execute();
	}

	private final class StreamProcessor extends SwingWorker<Void, String> {
		private final PipedInputStream stream;

		public StreamProcessor(PipedInputStream stream) {
			this.stream = stream;
		}

		@Override
		protected Void doInBackground() throws Exception {
			Scanner s = new Scanner(stream);
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
				log.append(line + "\n");
			}
			log.setCaretPosition(log.getDocument().getLength());
		}
	}
}
