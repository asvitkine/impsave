package impsave;

import java.io.File;
import java.util.HashMap;

public class GameInfo implements Comparable<GameInfo> {
	final String savedGameName;
	final String gameId;
	final String countryName;
	final String fileNamePrefix;
	int maxTurn;
	final HashMap<String, File> turnToFile;

	public GameInfo(String gameId, String countryName, String savedGameName, String fileNamePrefix) {
		this.gameId = gameId;
		this.countryName = countryName;
		this.savedGameName = savedGameName;
		this.fileNamePrefix = fileNamePrefix;
		this.turnToFile = new HashMap<String, File>();			
	}

	public String toString() {
		return gameId + " " + countryName + " (Turn " + maxTurn + ")";
	}

	@Override
	public int compareTo(GameInfo info) {
		return gameId.compareTo(info.gameId);
	}
}