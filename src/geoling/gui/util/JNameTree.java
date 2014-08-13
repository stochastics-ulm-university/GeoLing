package geoling.gui.util;

import geoling.gui.vendor.CheckTreeManager;
import geoling.models.Group;
import geoling.models.GroupsMaps;
import geoling.models.Map;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import org.javalite.activejdbc.LazyList;
import org.javalite.activejdbc.Model;

/**
 * <code>JNameTree</code> is a subclass <code>JTree</code>, but it overrides the method
 * <code>convertValueToText</code>, such that the names of <code>Model</code> objects are returned.
 * 
 * @author Raphael Wimmer, Institute of Stochastics, Ulm University
 */
@SuppressWarnings("serial")
public class JNameTree extends JTree {

	public String convertValueToText(Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
		DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
		if (node.getUserObject() instanceof Model) {
			Model model = (Model) node.getUserObject();
			String name = model.getString("name");
			if (name == null) {
				return "ID " + model.getId();
			} else {
				return name;
			}
		} else {
			return super.convertValueToText(value, selected, expanded, leaf, row, hasFocus);
		}
	}

	/**
	 * 
	 * @param group
	 *            The currently selected <code>Group</code>.
	 * @param checkTreeManager
	 *            The <code>CheckTreeManager</code> of
	 *            <code>this. JNameTree</code>
	 */
	@SuppressWarnings("unchecked")
	public void setCheckTreeSelected(Group group, CheckTreeManager checkTreeManager) {
		LazyList<GroupsMaps> groupsMaps = group.getAll(GroupsMaps.class);

		DefaultMutableTreeNode node = null;

		// Get the enumeration
		DefaultMutableTreeNode m_rootNode = (DefaultMutableTreeNode) this.getModel().getRoot();
		Enumeration<DefaultMutableTreeNode> num = m_rootNode.breadthFirstEnumeration();

		// iterate through the enumeration
		while (num.hasMoreElements()) {
			// get the node
			node = (DefaultMutableTreeNode) num.nextElement();
			if (node.getUserObject() instanceof Map) {
				Map map = (Map) node.getUserObject();
				int nodeId = map.getLongId().intValue();
				for (GroupsMaps groupsMap : groupsMaps) {
					int gm_id = groupsMap.getLong("map_id").intValue();
					if (gm_id == nodeId) {
						checkTreeManager.getSelectionModel().addSelectionPath(this.getPath(node));
					}
				}
			}
		}
	}

	/**
	 * 
	 * @param checkTreeManager
	 *            The <code>CheckTreeManager</code> of
	 *            <code>this. JNameTree</code>
	 * @return maps A <code>Vector</code> with all selected maps.
	 */
	@SuppressWarnings("unchecked")
	public Vector<Map> getCheckTreeSelected(CheckTreeManager checkTreeManager) {
		DefaultMutableTreeNode node = null;
		Vector<Map> maps = new Vector<Map>();

		// Get the enumeration
		DefaultMutableTreeNode m_rootNode = (DefaultMutableTreeNode) this.getModel().getRoot();
		Enumeration<DefaultMutableTreeNode> num = m_rootNode.breadthFirstEnumeration();

		// iterate through the enumeration
		while (num.hasMoreElements()) {
			// get the node
			node = (DefaultMutableTreeNode) num.nextElement();
			if (node.getUserObject() instanceof Map) {
				if (checkTreeManager.getSelectionModel().isPathSelected(this.getPath(node), true)) {
					maps.add((Map) node.getUserObject());
				}
			}
		}
		return maps;
	}

	public TreePath getPath(TreeNode node) {
		List<TreeNode> list = new ArrayList<TreeNode>();

		// Add all nodes to list
		while (node != null) {
			list.add(node);
			node = node.getParent();
		}
		Collections.reverse(list);

		// Convert array of nodes to TreePath
		return new TreePath(list.toArray());
	}

}