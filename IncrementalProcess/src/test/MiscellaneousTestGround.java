package test;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JTabbedPane;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import support.UIUtility;
import symbolic.PathSummary;
import symbolic.SymbolicExecution;
import analysis.StaticInfo;

public class MiscellaneousTestGround {

	public static void main(String[] args) {
		
//		System.out.println(String.format("%1$-20s", "hello")+"asdf");
//		System.out.println(TimeUnit.HOURS.toMillis(1));
//		String prefix = "/home/zhenxu/workspace/APK/";
//		String name = "TestArray.apk";
//		StaticApp app = StaticInfo.initAnalysis(prefix + name, false);
//		
////		for(StaticClass cls : app.getClasses()){
////			if(cls.getJavaName().contains("MainActivity")){
////				System.out.println("Class:   "+cls.getJavaName());
////				for(StaticMethod method :  cls.getMethods()){
////					System.out.println("method: "+method.getSignature());
////				}
////			}
////		}
//		
//		SymbolicExecution se = new SymbolicExecution(app);
//		List<PathSummary> initMethodSummary = se.doFullSymbolic("Lcom/example/testarray/MainActivity;-><init>()V");
//		List<PathSummary> assignMethodSummary = se.doFullSymbolic("Lcom/example/testarray/MainActivity;->assign(Landroid/view/View;)V");
//		List<PathSummary> changeMethodSummary = se.doFullSymbolic("Lcom/example/testarray/MainActivity;->change(Landroid/view/View;)V");
//		List<PathSummary> falterMethodSummary = se.doFullSymbolic("Lcom/example/testarray/MainActivity;->fixAlter(Landroid/view/View;)V");
//		List<PathSummary> createMethodSummary = se.doFullSymbolic("Lcom/example/testarray/MainActivity;->onCreate(Landroid/os/Bundle;)V");
//		
//		JFrame frame = new JFrame();
//		frame.setSize(500,400);
//		JTabbedPane jtp = new JTabbedPane();
//		
//		jtp.add("init", UIUtility.createRawSummaryPanel(initMethodSummary));
//		jtp.add("assign", UIUtility.createRawSummaryPanel(assignMethodSummary));
//		jtp.add("change", UIUtility.createRawSummaryPanel(changeMethodSummary));
//		jtp.add("fixAlter", UIUtility.createRawSummaryPanel(falterMethodSummary));
//		jtp.add("onCreate", UIUtility.createRawSummaryPanel(createMethodSummary));
//		
//		frame.add(jtp);
//		
//		frame.setVisible(true);
//		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
//		System.out.println("0123456->123".lastIndexOf("->"));
	}
 
	

}

/**
method: Lcom/example/testarray/MainActivity;-><init>()V
method: Lcom/example/testarray/MainActivity;->assign(Landroid/view/View;)V
method: Lcom/example/testarray/MainActivity;->change(Landroid/view/View;)V
method: Lcom/example/testarray/MainActivity;->fixAlter(Landroid/view/View;)V
method: Lcom/example/testarray/MainActivity;->onCreate(Landroid/os/Bundle;)V

**/