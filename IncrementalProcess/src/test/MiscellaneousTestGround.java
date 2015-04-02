package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JFrame;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import components.EventFactory;
import components.Executer;
import components.GraphicalLayout;
import analysis.StaticInfo;
import staticFamily.StaticApp;
import support.ADBUtility;
import support.CommandLine;
import support.UIUtility;

public class MiscellaneousTestGround {

	public static void main(String[] args) {
		int[] a = {1,2,3};
		
		System.out.println(Arrays.toString(Arrays.copyOf(a, 2)));
		
		
	}

	
//	/*Test find the called*/
//	new Thread(new Runnable(){
//		@Override
//		public void run() {
//			find();
//			
//		}
//	}).start();
//	public static void find(){
//		Thread t = Thread.currentThread();
//		StackTraceElement element = t.getStackTrace()[1];
//		
//		
//		
//		System.out.println(element.getClassName());
//		System.out.println(element.getMethodName());
//	}
	
	
	/*Test re-installation of an application*/
//	String path = "/home/zhenxu/workspace/APK/TheApp.apk";
//	StaticApp testApp = StaticInfo.initAnalysis(path, false);
//	System.out.println(testApp.getPackageName());
//	System.out.println(testApp.getSmaliAppPath());
//
//	String serial = "192.168.56.101:5555";
//	
//	Executer ex = new Executer(serial);
//	ex.applyEvent(EventFactory.createReinstallEvent(
//			testApp.getPackageName(), 
//			testApp.getSmaliAppPath()  ));
//	System.out.println(CommandLine.getLatestStdoutMessage());
}
