package impsave;

import java.io.*;
import java.util.HashSet;

public class Patcher {
	private static final int EXPECTED_SIZE = 3133440;

	private static String LATEST_MD5;
	private static HashSet<String> OLD_MD5S = new HashSet<String>();

	private static final void addMD5(String md5) {
		if (LATEST_MD5 == null) {
			LATEST_MD5 = md5;
			return;
		}
		OLD_MD5S.add(LATEST_MD5);
		LATEST_MD5 = md5;
	}

	static {
		addMD5("7dc20de2f44b9425be072f360e614157"); // r13 (gog)
		addMD5("c8908f3422cd0c2955cb46c826ebb3be"); // r14
		addMD5("7c1d66101b37bc0770b0ecf7de3dba1e"); // r15
		addMD5("f8a60d317cc9d148ab609c71095cf2a3"); // r33
		addMD5("5fd25232c0b4b440663e3370c3a1db6b"); // r39
		addMD5("28340af850a8b28faa2b71a16a775261"); // r50
		addMD5("749786f6e552245a88a2defbebb8409b"); // r51
		addMD5("4f65156ea95ff0329dcd90e62ed7db07"); // r52
		addMD5("5c46a85303a104d991a924062952a865"); // r53
		addMD5("ebe5f3037fba6ea2e3833b480672c47c"); // r54
		addMD5("1ee3ccd610801161db799382cb0af599"); // r55
	}

	// States:
	public static final int FILE_ERROR = -1;
	public static final int FILE_MISSING = 0;
	public static final int FILE_OLD = 1;
	public static final int FILE_UNKNOWN = 2;
	public static final int FILE_UP_TO_DATE = 3;

	private File file;
	private byte[] data;
	private int state;

	public Patcher(File file) {
		this.file = file;
		if (!file.exists()) {
			state = FILE_MISSING;
		} else if (file.length() != EXPECTED_SIZE) {
			state = FILE_UNKNOWN;
		} else try {
			data = Utils.readFile(file);
			String md5 = Utils.bytesToHex(Utils.encodeMD5(data));
			if (LATEST_MD5.equals(md5)) {
				state = FILE_UP_TO_DATE;
			} else if (OLD_MD5S.contains(md5)) {
				state = FILE_OLD;
			} else {
				state = FILE_UNKNOWN;
			}
		} catch (IOException e) {
			e.printStackTrace();
			state = FILE_ERROR;
		}
	}

	private static PatchSet getPatches() {
		PatchSet patches = new PatchSet();

		// Uninline call to CDib::AttachMemory() from CDib::AttachResource(),
		// to free up some space in the binary we can use for patches.
		patches.addPatch(0x47c0ab, "6A006A0057B8A0A84700FFD05F5EC20800");

		// Make CDib::AttachResource() own a copy of the buffer to fix a
		// multiplayer crash when playing a scenario on Win2K where
		// (apparently?) the underlying data goes away.
		//
		// The original code essentially does:
		//   r = LoadResource(FindResource(...))
		//   AttachMemory(r)
		//
		// But |r| is not owned and apparently can be deallocated.
		patches.addPatch(0x47a8a0,
			"56578BF1E8266BF8FF8B5424148B7C24" +
			"0C8956148B471485C0894620751E0FB7" +
			"470E0FAF47048BC8C1E905A81F740141" +
			"8B47080FAFC1C1E002894620837F2000" +
			"6A025975280FB7470E48741C83E80374" +
			"0E83E8047520C7462400010000EB17C7" +
			"462410000000EB0E894E24EB098B4610" +
			"8B4020894624837C241000C746180100" +
			"000075228B46248B4E20538D5C812853" +
			"E83EC61800535750894610E8E0DA1600" +
			"83C4105BEB0A85D27406894E18897E10" +
			"8B46108B4E2483C0288946048D04888B" +
			"CE89460CE8F076F8FF33C05F40");

		// Fix crash at 0x4eaecc during "next turn".
		patches.addJmpPatch(0x4eaec7, 0x47c0bc, "E8C5CDF8FF85C07501C20400E9FFED0600");
		// Fix null deref at 0x4d6f81.
		patches.addJmpPatch(0x4d6f7e, 0x47c0cd, "FF524C85C00F84B1AE05008B10E9A4AE0500");
		// Fix null deref at 0x61d89b.
		patches.addJmpPatch(0x61D89B, 0x47c0df, "85C9750331C0C38B8198000000C3");
		// Fix a couple calls to 0x1.
		patches.addJmpBackPatch(0x489d94, 0x47c0ed, 0x489d9a, "85D2740F83BA9C010000017406FF929C010000");
		patches.addJmpBackPatch(0x489cd5, 0x47c105, 0x489cdb, "85C0740F83B89C010000017406FF909C010000");
		// Fix null deref at 0x607AA3 (in CWnd::Default).
		patches.addJmpPatch(0x607AA3, 0x47c11d, "83BAA8000000007406FF92A80000005EC3");
		// Fix for possible null jmp at 0x549858.
		patches.addJmpBackPatch(0x549858, 0x47c12e, 0x54985E, "837874007406FF5074668907");
		// Fix null dereference at 0x4803dd when a player disconnects.
		patches.addJmpBackPatch(0x408F8F, 0x47c13f, 0x4803d0, "83790400750331C0C3");
		return patches;
	}

	public boolean patch() {
		try {
			writeFile(file.getAbsolutePath() + ".old", data);
			getPatches().apply(data);
			writeFile(file.getAbsolutePath(), data);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		return true;
	}

	private static void writeFile(String path, byte[] data) throws IOException {
		FileOutputStream fos = new FileOutputStream(path);
		fos.write(data);
		fos.close();
	}

	public int getState() {
		return state;
	}
}
