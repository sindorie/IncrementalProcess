package test;

import staticFamily.StaticApp;
import staticFamily.StaticMethod;
import analysis.StaticInfo;

public class TestStaticAnalysis {

	public static void main(String[] args){
		String methodRoot = "Lcom/example/beta1/MainActivity$3;->onClick(Landroid/view/View;)V";
		
		String prefix = "/home/zhenxu/workspace/APK/";
		String path = 
				"Beta1.apk";
		StaticApp app = StaticInfo.initAnalysis(prefix+path, false);
		
		StaticMethod m = app.findMethod(methodRoot);
		String className = m.getDeclaringClass(app).getJavaName();
		for (int lineNumber : m.getSourceLineNumbers()){
			System.out.println("setting breakpoint on "+className+", "+lineNumber);
		}
	}
}
