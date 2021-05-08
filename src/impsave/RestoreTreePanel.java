package impsave;

import java.awt.BorderLayout;
import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Set;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class RestoreTreePanel extends JPanel implements TreeSelectionListener {
	private SaveDb saveDb;

	private InfoPanel infoPanel;
	private JTree tree;

	public RestoreTreePanel(File saveFolder) {
		this.saveDb = new SaveDb(saveFolder);
		infoPanel = new InfoPanel(saveDb);
	}

	public void clear() {
		removeAll();
	}

	public void load() {
		saveDb.scan();
		saveDb.save();

		DefaultMutableTreeNode root = new DefaultMutableTreeNode("Saved Games");
		DefaultMutableTreeNode soloNode = createNodes(saveDb.getSoloGames(), "Solo Games");
		root.add(soloNode);
		DefaultMutableTreeNode hostedNode = createNodes(saveDb.getHostedGames(), "Hosted Games");
		root.add(hostedNode);
		DefaultMutableTreeNode joinedNode = createNodes(saveDb.getJoinedGames(), "Joined Games");
		root.add(joinedNode);

		tree = new JTree(root);
		tree.expandPath(new TreePath(soloNode.getPath()));
		tree.expandPath(new TreePath(hostedNode.getPath()));
		tree.expandPath(new TreePath(joinedNode.getPath()));
		tree.setRootVisible(false);
		tree.setShowsRootHandles(true);

		tree.addTreeSelectionListener(this);

		setLayout(new BorderLayout());
		add(new JScrollPane(tree), BorderLayout.WEST);
		add(infoPanel, BorderLayout.CENTER);
	}

	private DefaultMutableTreeNode createNodes(HashMap<String, GameInfo> games, String name) {
		DefaultMutableTreeNode gamesNode = new DefaultMutableTreeNode(name);
		gamesNode.setAllowsChildren(true);
		Set<String> keySet = games.keySet();
		if (keySet.isEmpty()) {
			gamesNode.add(new DefaultMutableTreeNode("(none)"));
		} else {
			String[] keys = keySet.toArray(new String[keySet.size()]);
			Arrays.sort(keys);
			for (String gameId : keys) {
				DefaultMutableTreeNode node = new DefaultMutableTreeNode(gameId);
				node.setUserObject(games.get(gameId));
				gamesNode.add(node);
			}
		}
		return gamesNode;
	}

	@Override
	public void valueChanged(TreeSelectionEvent e) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
		if (node == null)
			return;

		Object info = node.getUserObject();
		if (info instanceof GameInfo) {
			infoPanel.setGameInfo((GameInfo) info);
		} else {
			infoPanel.setGameInfo(null);
		}
	}
}
