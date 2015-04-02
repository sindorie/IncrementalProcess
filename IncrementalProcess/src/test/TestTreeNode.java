package test;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeModel;

public class TestTreeNode {

	public static void main(String[] args) {
		DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
		DefaultMutableTreeNode n1 = new DefaultMutableTreeNode("n1");
		DefaultMutableTreeNode n2 = new DefaultMutableTreeNode("n2");
		root.add(n1);
		root.add(n2);

		final JTree tree = new JTree(root);


		final JFrame frame = new JFrame("Demo");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().add(tree);
		frame.pack();
		frame.setVisible(true);
		final TreeModel model = tree.getModel();
		
		
		tree.addTreeSelectionListener(new TreeSelectionListener() {
			boolean b = true;

			@Override
			public void valueChanged(TreeSelectionEvent e) {
				System.out.println(e.getSource());
				System.out.println(((DefaultMutableTreeNode) e
						.getNewLeadSelectionPath().getLastPathComponent())
						.getUserObject());
				
				
			}
		});
	}

}
