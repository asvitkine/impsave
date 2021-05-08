package impsave;

import java.awt.FileDialog;
import java.io.File;
import java.io.IOException;

public class SaveInfoPrinter {
	private static String chooseFile() {
		FileDialog dialog = new FileDialog((FileDialog)null, "", FileDialog.LOAD);
		dialog.setVisible(true); 
		String loc = dialog.getFile();
		if (loc == null)
			return null;
		return dialog.getDirectory() + loc;
	}

	public static void main(String[] args) throws IOException {
		String file = chooseFile();
		if (file == null)
			System.exit(0);
		byte[] data = Utils.readFileN(new File(file), SaveParser.MAX_OFFSET);
		SaveParser sp = new SaveParser(data);
		System.out.println("GameId: " + sp.getGameId());
		System.out.println("Country: " + sp.getCountryName());
		System.out.println("Turn: " + sp.getTurnNumber());
	}
}