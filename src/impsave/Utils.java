package impsave;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Utils {
	public static byte[] readFile(File file) throws IOException {
		return readFileN(file, (int) file.length());
	}

	public static byte[] readFileN(File file, int n) throws IOException {
		byte[] data = new byte[n];
		FileInputStream f = new FileInputStream(file);
		f.read(data);
		f.close();
		return data;
	}

	public static boolean writeToFileIfNotSame(File file, byte[] data) throws IOException {
		if (file.exists()) {
			byte[] existingData = Utils.readFile(file);
			if (Arrays.equals(data, existingData)) {
				return false;
			}
		}
		FileOutputStream out = new FileOutputStream(file);
		try {
			out.write(data);
			out.flush();
		} finally {
			out.close();
		}
		return false;
	}

	public static String readZipComment(File f) throws IOException {
		String comment = null;
		ZipFile zf = new ZipFile(f);
		try {
			Enumeration<? extends ZipEntry> entries = zf.entries();
			if (!entries.hasMoreElements())
				throw new IOException("empty zip file");
			comment = entries.nextElement().getComment();
		} finally {
			zf.close();
		}
		return comment;
	}

	public static String getRelativePath(File f, File relativeTo) {
		return relativeTo.toURI().relativize(f.toURI()).getPath();
	}

	public static void copy(InputStream in, OutputStream out) throws IOException {
		byte[] buffer = new byte[1024];
        int len = in.read(buffer);
        while (len > 0) {
        	out.write(buffer, 0, len);
        	len = in.read(buffer);
        }
	}

	public static File addFileNameSuffix(File file, String suffix) {
		String name = file.getName();
		int dotIndex = name.lastIndexOf('.');
		if (dotIndex == -1) {
			dotIndex = name.length();
		}
		String newName = name.substring(0, dotIndex) + suffix + name.substring(dotIndex, name.length());
		return new File(file.getParent(), newName);
	}

	public static byte[] encodeMD5(byte[] data) {
		byte[] md5 = null;
		try {
			MessageDigest d = MessageDigest.getInstance("MD5");
			d.reset();
			d.update(data);
			md5 = d.digest();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return md5;
	}

	private static char[] HEX_CHARS =
		{ '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };
	public static String bytesToHex(byte bytes[]) {
		StringBuilder buf = new StringBuilder(bytes.length * 2);
		for (int i = 0; i < bytes.length; i++) {
			byte b = bytes[i];
			buf.append(HEX_CHARS[(b & 0xF0) >> 4]);
			buf.append(HEX_CHARS[b & 0x0F]);
		}
		return buf.toString();
	}

	public static byte[] hexToBytes(String hex) {
		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < bytes.length; i++) {
			bytes[i] = (byte) Integer.parseInt(hex.substring(2*i, 2*i+2), 16);
		}
		return bytes;
	}

	public static int byteSwapInt(int value) {
		int result = 0;
		for (int i = 0; i <= 24; i += 8) {
			int oneByte = (value >> i) & 0xff;
			result |= (oneByte << (24 - i));
		}
		return result;
	}

	public static String truncateAtChar(String str, char c) {
		int index = str.indexOf(c);
		if (index != -1) {
			str = str.substring(0, index);
		}
		return str;
	}

	public static boolean endsWithOneOf(String str, String... suffixes) {
		for (String suffix : suffixes) {
			if (str.endsWith(suffix)) {
				return true;
			}
		}
		return false;
	}

	public static boolean startsWithOneOf(String str, String... prefixes) {
		for (String prefix : prefixes) {
			if (str.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}
}
