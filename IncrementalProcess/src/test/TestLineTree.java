package test;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import javax.swing.tree.TreeNode;
 
import support.TreeUtility;
import support.UIUtility;

public class TestLineTree {

	public static void main(String[] args) throws FileNotFoundException {
		
		String path = "/home/zhenxu/toRead";
		File f = new File(path);
		
		TreeNode tree = TreeUtility.buildLineTree(new Scanner(f));		
		
		UIUtility.showTree(tree);
	}

}
