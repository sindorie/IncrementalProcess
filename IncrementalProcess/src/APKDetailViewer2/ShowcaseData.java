package APKDetailViewer2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticField;
import staticFamily.StaticMethod;
import staticFamily.StaticStmt;
import symbolic.PathSummary;
import symbolic.SymbolicExecution;
import analysis.StaticInfo;

public class ShowcaseData {

	Map<String, StaticApp> map = new HashMap<String, StaticApp>();
	Map<String, SymbolicExecution> mapSym = new HashMap<String, SymbolicExecution>();
	Map<String, List<PathSummary>> summaryMap = new HashMap<String,List<PathSummary>>();
	Map<String, String> mapToClzText = new HashMap<String,String>();
	
	StaticApp retrieveApp(String path) {
		StaticApp app = null;
		if(map.containsKey(path)){ app  = map.get(path);
		}else{ 
			app = StaticInfo.initAnalysis(path, false);
			SymbolicExecution ex = new SymbolicExecution(app);
			ex.blackListOn = true;
			mapSym.put(path, ex);
			map.put(path, app);
		}
		return app;
	}
	
	List<PathSummary> retrieveSummaries(String currentPath, String signature){
		if(summaryMap.containsKey(signature)){
			return summaryMap.get(signature);
		}else{
			SymbolicExecution ex = mapSym.get(currentPath);
			List<PathSummary> result =  ex.doFullSymbolic(signature);
			summaryMap.put(signature, result);
			return result;
		}
	}
	
	String retrieveClzText(StaticClass clz){
		String sig = clz.getJavaName();
		if(mapToClzText.containsKey(sig)){
			return mapToClzText.get(sig);
		}else{
			String result = staticClassToString(clz);
			mapToClzText.put(sig, result);
			return result;
		}
	}
	static String TAB = "       ";
	static String staticClassToString(StaticClass clz){
		StringBuilder sb = new StringBuilder();
		if(clz.isEnum()) sb.append(".enum ");
		else if(clz.isInterface()) sb.append(".interface ");
		else sb.append(".class ");
		if(clz.isFinal())sb.append("final ");
		if(clz.isAbstract())sb.append("abstract ");
		sb.append(clz.getJavaName()).append("\n");
		sb.append(".extends ").append(clz.getSuperClass()).append("\n");
		sb.append(".implements ").append(clz.getInterfaces()).append("\n");
		sb.append(".source ").append(clz.getSourceFileName()).append("\n");
		if(clz.isInnerClass())sb.append(".outter ").append(clz.getOuterClass()).append("\n");
		sb.append("\n");
		
		for(StaticField field : clz.getFields()){
			sb.append(TAB).append(".field ").append(field.getDeclaration()).append("\n");
		}
		sb.append("\n");
		
		for(StaticMethod method : clz.getMethods()){
			sb.append(staticMethodtoString(method)).append("\n");
		} 
		sb.append(".end class ");
		return sb.toString();
	}
	static String staticMethodtoString(StaticMethod method){
		StringBuilder sb = new StringBuilder();
		sb.append(TAB).append(".method ");
		if(method.isAbstract()) sb.append("abstract ");
		if(method.isNative())sb.append("native ");
		if(method.isPrivate())sb.append("private ");
		if(method.isProtected())sb.append("protected ");
		if(method.isStatic())sb.append("static ");
		sb.append(method.getSignature()).append("\n");
		for( StaticStmt stmt : method.getSmaliStmts()){
			sb.append(TAB).append(TAB).append(".line "+stmt.getSourceLineNumber()).append("\n");
			sb.append(TAB).append(TAB).append(stmt.getSmaliStmt()).append("\n").append("\n");
		}
		sb.append(TAB).append(".end method ").append("\n");
		return sb.toString();
	}
}
