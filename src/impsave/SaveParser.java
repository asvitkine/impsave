package impsave;

import java.security.*;
import java.util.ArrayList;

import crockford.CrockfordBase32;

public class SaveParser {
	public static final int MAX_OFFSET = 0x2000;
	public static final int SAVE_NAME_OFFSET = 0xC;
	public static final int SAVE_NAME_LENGTH = 28;

	private byte[] data;

	public SaveParser(byte[] data) {
		this.data = data;
	}

	public String getCountryName() {
		return readCString(0x1980);
	}

	public String getSavedGameName() {
		return readCString(SAVE_NAME_OFFSET);
	}

	public int getTurnNumber() {
		int value = data[0x19a4];
		return value < 0 ? 256 + value : value;
	}

	public String getGameId() {
		byte[] idBytes = new byte[] { data[8], data[9], data[10], data[11] };
		CrockfordBase32 encoder = new CrockfordBase32();
		String gameId = encoder.encodeToString(idBytes);
		// Single player games don't have an id, instead hash list of countries.
		if (gameId.equals("0000000")) {
			byte[] longId = computeSoloGameId();
			System.arraycopy(longId, 0, idBytes, 0, 4);
			gameId = encoder.encodeToString(idBytes);
		}
		return gameId;
	}

	private byte[] computeSoloGameId() {
		ArrayList<String> countries = new ArrayList<String>();
		// Read list of countries (this only works for solo games as they
		// are stored differently in multiplayer).
		int offset = 0x1a2d;
		while (data[offset + 1] != 0) {
			String country = readPascalString(offset);
			countries.add(country);
			offset += country.length() + 1;
		}
		return computeStringListHash(countries);
	}

	private static byte[] computeStringListHash(ArrayList<String> list) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("MD5");
			for (String str : list)
				md.update(str.getBytes());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return md.digest();
	}

	private String readCString(int start) {
		int end = start;
		while (end < data.length && data[end] != 0) {
			end++;
		}
		return new String(data, start, end - start);
	}

	private String readPascalString(int start) {
		int length = data[start];
		return new String(data, start + 1, length);
	}

	public SaveDb.GameFileContents getGameFileContents() {
		SaveDb.GameFileContents contents = new SaveDb.GameFileContents();
		contents.setId(getGameId());
		contents.setCountry(getCountryName());
		contents.setTurn(getTurnNumber());
		contents.setSavedGameName(getSavedGameName());
		return contents;
	}
}
