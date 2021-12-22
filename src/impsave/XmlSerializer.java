package impsave;

import java.io.File;
import java.io.OutputStream;
import java.util.Collection;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.security.NoTypePermission;
import com.thoughtworks.xstream.security.NullPermission;
import com.thoughtworks.xstream.security.PrimitiveTypePermission;

import impsave.SaveDb.GameFileContents;
import impsave.SaveDb.SavedGames;

public class XmlSerializer {
	private XStream xstream;

	public XmlSerializer() {
		xstream = createXStream();
	}

	private static XStream createXStream() {
		XStream xstream = new XStream();
		xstream.addPermission(NoTypePermission.NONE);
		xstream.addPermission(NullPermission.NULL);
		xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);
		xstream.allowTypeHierarchy(String.class);
		xstream.allowTypeHierarchy(Collection.class);
		xstream.allowTypesByWildcard(new String[] {
			XmlSerializer.class.getPackage().getName() + ".*"
		});
		xstream.alias("game", GameFileContents.class);
		xstream.alias("imp-save-db", SavedGames.class);
		return xstream;
	}

	public SavedGames readSavedGames(File file) throws Exception {
		return (SavedGames) xstream.fromXML(file);
	}

	public void writeSavedGames(SavedGames savedGames, OutputStream out) {
		xstream.toXML(savedGames, out);
	}
}
