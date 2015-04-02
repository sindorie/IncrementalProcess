package support;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

public class UIUtility {

	public static void showTree(TreeNode node){
		showTree("tree",node,JFrame.EXIT_ON_CLOSE);
	}
	
	public static void showTree(String name, TreeNode node, int operationCode){
		JFrame frame = new JFrame(name);
		JTree tree = new JTree(node);
		frame.getContentPane().add(tree);
		frame.pack();
		frame.setVisible(true);
	}
	
}
