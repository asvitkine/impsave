package impsave;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import impsave.SaveDb.GameFileContents;
import impsave.SaveDb.SavedGames;

/**
 * XML serialization/deserialization logic. Uses plain old Java DOM API to avoid
 * a dependency on a third party library (which bloats the jar and requires keeping
 * up to date with security vulnerabilities).
 *
 * If we used XStream, this could be significantly simplified, with something like:
 *
 *    XStream xstream = new XStream();
 *    xstream.addPermission(NoTypePermission.NONE);
 *    xstream.addPermission(NullPermission.NULL);
 *    xstream.addPermission(PrimitiveTypePermission.PRIMITIVES);
 *    xstream.allowTypeHierarchy(String.class);
 *    xstream.allowTypeHierarchy(Collection.class);
 *    xstream.allowTypesByWildcard(new String[] { XmlSerializer.class.getPackage().getName() + ".*" });
 *    xstream.alias("game", GameFileContents.class);
 *    xstream.alias("imp-save-db", SavedGames.class);
 *
 *    (SavedGames) xstream.fromXML(file);
 *    xstream.toXML(savedGames, out);
 *
 * But this adds 700kb to the jar (which is very tiny otherwise) and introduces the need to update the
 * software when security vulnerabilities get patched.
 *
 * Note: Originally, this used JAXB which came built-in with the JRE, but which got removed in Java 11+
 * (and its dependency as an external library is even bigger than XStream).
 */
public class XmlSerializer {
	public SavedGames readSavedGames(File file) throws Exception {
		// Note: With xstream, this would just be: (SavedGames) xstream.fromXML(file);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = factory.newDocumentBuilder();
		Document doc = dBuilder.parse(file);
		doc.getDocumentElement().normalize();

		SavedGames games = new SavedGames();
		NodeList gameNodes = doc.getElementsByTagName("game");
		for (int i = 0; i < gameNodes.getLength(); i++) {
			Element node = (Element) gameNodes.item(i);
			GameFileContents game = new GameFileContents();
			game.setFile(getChildText(node, "filePath"));
			game.setId(getChildText(node, "gameId"));
			game.setTurn(Integer.parseInt(getChildText(node, "turnNumber")));
			game.setCountry(getChildText(node, "countryName"));
			game.setSavedGameName(getChildText(node, "savedGameName"));
			games.getGames().add(game);
		}
		NodeList commentNodes = doc.getElementsByTagName("entry");
		for (int i = 0; i < commentNodes.getLength(); i++) {
			Element node = (Element) commentNodes.item(i);
			NodeList pairs = node.getElementsByTagName("string");
			String key = pairs.item(0).getTextContent();
			String value = pairs.item(1).getTextContent();
			games.getComments().put(key, value);
		}
		return games;
	}

	private static String getChildText(Element node, String tag) {
		return node.getElementsByTagName(tag).item(0).getTextContent();
	}

	public void writeSavedGames(SavedGames savedGames, OutputStream out) throws Exception {
		// Note: With xstream, this would just be: xstream.toXML(savedGames, out);

		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = factory.newDocumentBuilder();
		Document doc = builder.newDocument();

		Element root = doc.createElement("imp-save-db");
		doc.appendChild(root);

		Element games = doc.createElement("games");
		root.appendChild(games);
		for (GameFileContents game : savedGames.getGames()) {
			games.appendChild(createGameNode(doc, game));
		}

		Element comments = doc.createElement("comments");
		root.appendChild(comments);
		for (Map.Entry<String, String> entry : savedGames.getComments().entrySet()) {
			Element node = doc.createElement("entry");
			node.appendChild(createStringNode(doc, "string", entry.getKey()));
			node.appendChild(createStringNode(doc, "string", entry.getValue()));
			comments.appendChild(node);
		}

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transf = transformerFactory.newTransformer();

		transf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
		transf.setOutputProperty(OutputKeys.INDENT, "yes");
		transf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

		DOMSource source = new DOMSource(doc);
		StreamResult console = new StreamResult(out);
		transf.transform(source, console);
	}

	private static Node createGameNode(Document doc, GameFileContents game) {
		Element node = doc.createElement("game");
		node.appendChild(createStringNode(doc, "filePath", game.getFile()));
		node.appendChild(createStringNode(doc, "gameId", game.getId()));
		node.appendChild(createStringNode(doc, "turnNumber", "" + game.getTurn()));
		node.appendChild(createStringNode(doc, "countryName", "" + game.getCountry()));
		node.appendChild(createStringNode(doc, "savedGameName", "" + game.getSavedGameName()));
		return node;
	}

	private static Node createStringNode(Document doc, String tag, String value) {
		Element node = doc.createElement(tag);
		node.appendChild(doc.createTextNode(value));
		return node;
	}
}
