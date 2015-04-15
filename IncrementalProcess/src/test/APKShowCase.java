package test;

import java.awt.Dimension;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import staticFamily.StaticApp;
import support.UIUtility;
import analysis.StaticInfo;

public class APKShowCase {

	public static void main(String[] args) {
		String name = "/home/zhenxu/AndroidTestAPKPlayGround/APK2/CalcA.apk";
		StaticApp app = StaticInfo.initAnalysis(name, false);
		
		JFrame frame = new JFrame();
		frame.setSize(500,400);
		
		JComponent component = UIUtility.createShowCaseForApk(app, true);
		JScrollPane jsp = new JScrollPane();
		component.setMinimumSize(new Dimension(800,600));
		jsp.setViewportView(component);
		
		frame.add(jsp);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

}
