package impsave;

import java.util.HashMap;

public class PatchSet {
	private HashMap<Integer, String> patches;

	public PatchSet() {
		patches = new HashMap<Integer, String>();
	}

	public void apply(byte[] data) {
		System.out.println("Patching file of size " + data.length);
		for (int addr : patches.keySet()) {
			byte[] patchData = Utils.hexToBytes(patches.get(addr));
			int convertedAddr = addr - 0x400C00;
			for (int i = 0; i < patchData.length; i++) {
				data[convertedAddr + i] = patchData[i];
			}
		}
	}

	private static String calcJmpOrCall(int instrAddr, int destAddr) {
		int x = destAddr - instrAddr - 5;
		return String.format("%x", Utils.byteSwapInt(x));
	}

	private static String jmpInstr(int instrAddr, int destAddr) {
		// e8 for call
		return "e9" + calcJmpOrCall(instrAddr, destAddr);
	}

	public void addPatch(int addr, String hex) {
		patches.put(addr, hex);
		int len = hex.length() / 2;
		System.out.printf("Patching 0x%x - 0x%x (%d bytes)\n", addr, addr + len, len);
	}

	public void addJmpPatch(int instrAddr, int patchAddr, String patchHex) {
		addPatch(instrAddr, jmpInstr(instrAddr, patchAddr));
		addPatch(patchAddr, patchHex);
	}

	public void addJmpBackPatch(int instrAddr, int patchAddr, int backAddr, String patchHex) {
		int jmpBackFromAddr = patchAddr + patchHex.length() / 2;
		addJmpPatch(instrAddr, patchAddr, patchHex + jmpInstr(jmpBackFromAddr, backAddr));
		if (backAddr - instrAddr < 10) {
			// Pad with NOPs to not confuse disassemblers.
			for (int jmpEnd = instrAddr + 5; jmpEnd < backAddr; jmpEnd++) {
				addPatch(jmpEnd, "90");
			}
		}
	}
}
