package test;

import java.util.List;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultTreeModel;

import staticFamily.StaticApp;
import symbolic.PathSummary;
import symbolic.SymbolicExecution;
import analysis.StaticInfo;
import components.LayoutNode;
import components.ViewDeviceInfo;

public class PreviousTestBundle {

	
	
	static void testDeviceInfo(){
		String serial = 
//				"08563207";
			"192.168.56.102:5555";
		ViewDeviceInfo view = new ViewDeviceInfo(serial);
		LayoutNode data = view.loadWindowData();
//		System.out.println(data.getChildAt(0).getChildAt(0).att);

		
		final DefaultTreeModel model = new DefaultTreeModel(data);
		
		JTree tree = new JTree();
		tree.setModel(model);
		JFrame frame = new JFrame();
		frame.add(tree);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		
		
		Scanner sc = new Scanner(System.in);
		while(true){
			if(sc.hasNextLine()){
				
				String line = sc.nextLine().trim();
				if(line.isEmpty()) continue;
				if(line.equals("1")){
//					view = new ViewDeviceInfo(serial);
					data = view.loadWindowData();
					model.setRoot(data);
					tree.revalidate();
				}else if(line.equalsIgnoreCase("0")){
					break;
				}
			}
		}
		sc.close();
	}
	
	static void testSymbolic(String path, String sig) {
		StaticApp app = StaticInfo.initAnalysis(path, false);
		SymbolicExecution se = new SymbolicExecution(app);
		List<PathSummary> sum = se.doFullSymbolic(sig);
	}
}
