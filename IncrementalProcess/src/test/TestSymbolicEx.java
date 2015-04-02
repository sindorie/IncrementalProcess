package test;

import java.util.List;

import staticFamily.StaticApp;
import symbolic.PathSummary;
import symbolic.SymbolicExecution;
import analysis.StaticInfo;

public class TestSymbolicEx {

	public static void main(String[] args){
		String prefix = "/home/zhenxu/workspace/APK/";
		String path 
//			= "CalcA.apk";
			= "backupHelper.apk";
			
		String sig 
//			= "Lcom/bae/drape/gui/calculator/CalculatorActivity$10;->onClick(Landroid/view/View;)V";
			= "Lcom/example/backupHelper/BackupActivity;->onCreate(Landroid/os/Bundle;)V";
		
		test(prefix+path, sig);
	}

	static void test(String path, String sig) {

		StaticApp app = StaticInfo.initAnalysis(path, false);

		SymbolicExecution se = new SymbolicExecution(app);
		List<PathSummary> sum = se
				.doFullSymbolic(sig);

	}
}
