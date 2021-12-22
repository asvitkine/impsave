package impsave;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JOptionPane;

import crockford.CrockfordBase32;

// TODO: Update game names as we find copies.
public class SaveDb {
	private static final String EXT = ".imp";
	private static final String[] EXTS = new String[] { EXT, EXT + ".zip" };

	public static final String SOLO_GAME_PREFIX = "slot";
	public static final String HOSTED_GAME_PREFIX = "mult";
	public static final String JOINED_GAME_PREFIX = "cli_";

	private File saveFolder;
	private SavedGames savedGames;
	private HashMap<File, GameFileContents> gameFileCache;
	private XmlSerializer xml;
	private HashMap<String, GameInfo> soloGames;
	private HashMap<String, GameInfo> hostedGames;
	private HashMap<String, GameInfo> joinedGames;

	public SaveDb(File saveFolder) {
		this.saveFolder = saveFolder;
		savedGames = new SavedGames();
		gameFileCache = new HashMap<File, GameFileContents>();
		xml = new XmlSerializer();
		load();
	}

	public HashMap<String, GameInfo> getSoloGames() {
		return soloGames;
	}

	public HashMap<String, GameInfo> getHostedGames() {
		return hostedGames;
	}

	public HashMap<String, GameInfo> getJoinedGames() {
		return joinedGames;
	}

	private File getFile() {
		return new File(saveFolder, "imp_save_db.xml");
	}

	public GameFileContents getGameInfo(File f) {
		return gameFileCache.get(f);
	}

	public void putGameInfo(GameFileContents game) {
		gameFileCache.put(new File(saveFolder + "/" + game.filePath), game);
		savedGames.games.add(game);
	}

	public void scan() {
		System.out.println("Scanning...");
		soloGames = new HashMap<String, GameInfo>();
		hostedGames = new HashMap<String, GameInfo>();
		joinedGames = new HashMap<String, GameInfo>();
		for (File f : saveFolder.listFiles()) {
			if (ignoreFile(f))
				continue;

			// Joined games have format "cli_1407716399.imp".
			if (f.getName().startsWith(JOINED_GAME_PREFIX))
				updateGamesFromBackups(joinedGames, f, JOINED_GAME_PREFIX);

			// Solo games are auto-saved to slotA.imp and manually saved to slot0.imp to slot7.imp.
			if (f.getName().startsWith(SOLO_GAME_PREFIX))
				updateGamesFromBackups(soloGames, f, SOLO_GAME_PREFIX);
			// Hosted games are auto-saved to multA.imp and manually saved to mult0.imp to mult7.imp.
			if (f.getName().startsWith(HOSTED_GAME_PREFIX))
				updateGamesFromBackups(hostedGames, f, HOSTED_GAME_PREFIX);
		}
	}

	private boolean ignoreFile(File f) {
		return !f.isFile() || f.isHidden() || !f.getName().endsWith(EXT);
	}

	private void updateGamesFromBackups(HashMap<String, GameInfo> games, File sourceFile, String prefix) {
		processGameFile(games, sourceFile, prefix);
		File dir = FileAutoSaver.getBackupDir(sourceFile);
		if (!dir.isDirectory())
			return;
		for (File f : dir.listFiles()) {
			if (!Utils.endsWithOneOf(f.getName(), EXTS))
				continue;
			processGameFile(games, f, prefix);
		}
	}

	private void processGameFile(HashMap<String, GameInfo> games, File f, String prefix) {
		try {
			SaveDb.GameFileContents contents = getGameFileContents(f);

			GameInfo info = games.get(contents.getId());
			if (info == null) {
				info = new GameInfo(contents.getId(), contents.getCountry(), contents.getSavedGameName(), prefix);
				games.put(contents.getId(), info);
			}
			info.maxTurn = Math.max(info.maxTurn, contents.getTurn());

			String turnStr = FileAutoSaver.formatTurn(contents.getTurn());
			String filename = f.getName();
			// If it's an autosave file, get the full turn string (e.g. 050-2).
			String expectedFormat = FileAutoSaver.formatName(contents.getCountry(), contents.getTurn());
			if (filename.startsWith(expectedFormat)) {
				for (String ext : EXTS) {
					if (filename.endsWith(ext)) {
						String nameNoExt = filename.substring(0, filename.length() - ext.length());
						turnStr = FileAutoSaver.getTurnString(nameNoExt, contents.getCountry());
						break;
					}
				}
			}

			info.turnToFile.put(turnStr, f);
		} catch (Exception e) {
			System.out.println("ERROR reading " + f);
			e.printStackTrace();
		}
	}

	public String getFilename(String prefix, int index) {
		return prefix + index + EXT;
	}

	public String getSoloFileName(int index) {
		return getFilename(SOLO_GAME_PREFIX, index);
	}

	public String getSoloSaveGameName(int index) {
		return getSaveGameName(getSoloFileName(index));
	}

	public String getMultiFileName(int index) {
		return getFilename(HOSTED_GAME_PREFIX, index);
	}

	public String getHostedSaveGameName(int index) {
		return getSaveGameName(getMultiFileName(index));
	}

	public String getOriginalFileName(File file) {
		String parentDirName = file.getParentFile().getName();
		String suffix = " autosaves";
		if (parentDirName.endsWith(suffix) &&
			Utils.startsWithOneOf(parentDirName, SOLO_GAME_PREFIX, HOSTED_GAME_PREFIX)) {
			return parentDirName.substring(0, parentDirName.length() - suffix.length());
		}
		return null;
	}

	private void copyFileOrZipContents(File file, OutputStream out, String gameName) throws IOException {
		FileInputStream in = new FileInputStream(file);
		InputStream source;

		ZipInputStream zin = null;
		if (file.getName().endsWith(".zip")) {
			zin = new ZipInputStream(in);
			ZipEntry entry = zin.getNextEntry();
			if (entry == null) {
				zin.close();
				in.close();
				throw new IOException("null ZipEntry");
			}
			System.out.println("Extracting inner file [" + entry.getName() + "]");
			source = zin;
		} else {
			source = in;
		}

		if (gameName != null) {
			byte[] header = new byte[SaveParser.SAVE_NAME_OFFSET + SaveParser.SAVE_NAME_LENGTH];
			if (source.read(header) != header.length) {
				in.close();
				throw new IOException("Couldn't read header!");
			}

			byte[] gameNameBytes = gameName.getBytes("US-ASCII");
			for (int i = SaveParser.SAVE_NAME_OFFSET; i < header.length; i++) {
				int fromIndex = i - SaveParser.SAVE_NAME_OFFSET;
				if (fromIndex < gameNameBytes.length) {
					header[i] = gameNameBytes[fromIndex];
				} else {
					header[i] = 0;
				}
			}
			header[header.length - 1] = '\0';

			out.write(header);
		}
		Utils.copy(source, out);
		if (zin != null) {
	        zin.closeEntry();
	    	zin.close();
		}
		in.close();
	}

	public void activateFile(File file, File dest, String gameName) throws IOException {
		FileOutputStream out = new FileOutputStream(dest);
		try {
			copyFileOrZipContents(file, out, gameName);
			getGameFileContents(dest).savedGameName = gameName;
		} catch (IOException e) {
			dest.delete();
		} finally {
			out.close();
		}
	}

	private String getSaveGameName(String filename) {
		File file = new File(saveFolder, filename);
		if (!file.exists()) {
			return "(Empty)";
		}
		try {
			GameFileContents contents = getGameFileContents(file);
			return contents.getSavedGameName();
		} catch (IOException e) {
			e.printStackTrace();
			return "(Error)";
		}
	}

	private byte[] readFileOrZipContents(File file) throws IOException {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		copyFileOrZipContents(file, out, null);
		return out.toByteArray();
	}

	private SaveDb.GameFileContents getGameFileContents(File f) throws IOException {
		SaveDb.GameFileContents contents = getGameInfo(f);
		if (contents != null)
			return contents;
		byte[] data;
		if (f.getName().endsWith(".zip")) {
			data = readFileOrZipContents(f);
		} else {
			data = Utils.readFileN(f, SaveParser.MAX_OFFSET);
		}
		SaveParser sp = new SaveParser(data);
		contents = sp.getGameFileContents();
		contents.setFile(Utils.getRelativePath(f, saveFolder));
		putGameInfo(contents);
		return contents;
	}

	private String generateBackupSuffix() {
	      byte[] arr = new byte[3];
	      new Random().nextBytes(arr);
	      return ".old" + new CrockfordBase32().encodeToString(arr);
	}

	private static void renameFileOrDie(File from, File to) {
		try {
			from.renameTo(to);
		} catch (Exception e) {
			e.printStackTrace();
			JOptionPane.showMessageDialog(null, "Could not rename file: " + from + " to: " + to);
			System.exit(-1);
		}
	}

	private void load() {
		File file = getFile();
		if (!file.exists())
			return;

		System.out.println("Loading " + file);
		try {
			savedGames = xml.readSavedGames(file);
		} catch (Exception e) {
			System.out.println("Error loading file: " + file);
			File backupFile = Utils.addFileNameSuffix(file, generateBackupSuffix());
			System.out.println("Renaming existing file to: " + backupFile);
			renameFileOrDie(file, backupFile);
			savedGames = new SavedGames();
		}

		if (savedGames.comments == null)
			savedGames.comments = new HashMap<String, String>();
		for (GameFileContents game : savedGames.games) {
			gameFileCache.put(new File(saveFolder + "/" + game.filePath), game);
		}
	}

	public void save() {
		File file = getFile();
		System.out.println("Writing to " + file);
		try {
			xml.writeSavedGames(savedGames, new FileOutputStream(file));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}

	public String getComment(String gameId) {
		return savedGames.comments.get(gameId);
	}

	public void setComment(String gameId, String text) {
		if (text.isEmpty()) {
			savedGames.comments.remove(gameId);
		} else {
			savedGames.comments.put(gameId, text);
		}
	}

	public static class SavedGames {
		private List<GameFileContents> games = new ArrayList<GameFileContents>();
		private Map<String, String> comments = new HashMap<String, String>();

	    public void setGames(List<GameFileContents> games) {
	        this.games = games;
	    }

	    public List<GameFileContents> getGames() {
	    	return games;
	    }

	    public void setComments(Map<String, String> comments) {
	        this.comments = comments;
	    }

	    public Map<String, String> getComments() {
	    	return comments;
	    }
	}

	public static class GameFileContents {
		private String filePath;
		private String gameId;
		private int turnNumber;
		private String countryName;
		private String savedGameName;

		public GameFileContents() {
		}

		public GameFileContents(String data, String file) {
		}

		public String getFile() {
			return filePath;
		}

		public void setFile(String file) {
			this.filePath = file;
		}

		public String getId() {
			return gameId;
		}

		public void setId(String id) {
			gameId = id;
		}

		public int getTurn() {
			return turnNumber;
		}

		public void setTurn(int turn) {
			turnNumber = turn;
		}

		public String getCountry() {
			return countryName;
		}

		public void setCountry(String country) {
			countryName = country;
		}

		public void setSavedGameName(String name) {
			savedGameName = name;
		}

		public String getSavedGameName() {
			return savedGameName;
		}
	}
}
