package APKDetailViewer3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import analysis.StaticInfo;
import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticField;
import staticFamily.StaticMethod;
import staticFamily.StaticStmt;
import symbolic.PathSummary;
import symbolic.SymbolicExecution;

public class DataCache {

	Map<String,DataSection> dataCache = new HashMap<String,DataSection>();
	DataSection currentData;
	
	StaticApp retrieveApp(String path, boolean force){
		try{
			if(force){
				StaticApp app = StaticInfo.initAnalysis(path, force);
				if(app == null){
					System.out.println("Static Anlaysis failure.");
					return null;
				}
				currentData = new DataSection(app);
				dataCache.put(path, currentData);
			}else{
				DataSection data = dataCache.get(path);
				if(data == null){
					StaticApp app = StaticInfo.initAnalysis(path, force);
					if(app == null){ 
						System.out.println("Static Anlaysis failure.");
						return null; 
					}
					data = new DataSection(app);
					dataCache.put(path, data);
				}
				currentData = data;
			}
			return currentData.app;
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	List<StaticClass> retrieveFilteredClasses(){
		if(currentData != null){
			List<StaticClass> list = currentData.app.getClasses();
			List<StaticClass> result = new ArrayList<StaticClass>();
			for(StaticClass clz : list){
				if(!symbolic.Blacklist.classInBlackList(clz.getDexName())){
					result.add(clz);
				}
			}
			return result;
		}
		return new ArrayList<StaticClass>();
	}
	
	List<PathSummary> retrievePathSummary(String sig){
		if(currentData != null){
			return currentData.retrieveSummary(sig);
		}
		return null;
	}
	
	void clearData(){
		dataCache.clear();
	}
	
	static class DataSection{
		public DataSection(StaticApp app){
			this.app = app;
			this.se = new SymbolicExecution(app);
		}
		
		StaticApp app;
		SymbolicExecution se;
		Map<String, List<PathSummary>> summaryCache = new HashMap<String, List<PathSummary>>();
		Map<String, String> mapToClzText = new HashMap<String,String>();
		
		public List<PathSummary> retrieveSummary(String sig){
			List<PathSummary> result = summaryCache.get(sig);
			if(result == null){
				result = se.doFullSymbolic(sig);
				summaryCache.put(sig, result);
			}
			return result;
		}
		
	}
	
	String retrieveClzText(StaticClass clz){
		String sig = clz.getJavaName();
		if(currentData.mapToClzText.containsKey(sig)){
			return currentData.mapToClzText.get(sig);
		}else{
			String result = staticClassToString(clz);
			currentData.mapToClzText.put(sig, result);
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
