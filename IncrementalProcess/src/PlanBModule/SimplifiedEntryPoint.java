package PlanBModule;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;

import main.Paths;
import staticFamily.StaticApp;
import support.Logger;
import support.Utility;
import support.Logger.CurrentThreadInfo;
import support.Logger.InformationFilter;
import symbolic.Expression;
import symbolic.PathSummary;
import analysis.StaticInfo;
import components.ExpressionTranfomator;
import components.EventDeposit.InternalPair;
import components.WrappedSummary;
import components.system.Configuration;

public class SimplifiedEntryPoint {
	boolean force = false;
	int maxIteration = 300;
	long maxTime = TimeUnit.MINUTES.toMillis(30);
	int maxValidationTry = 3;
	String parentFolderName = "LogFolder"; // could be a path; debug purpose
	boolean enableConsoleOutput = true;
	StaticApp app;
	UIModel model;
	DualDeviceOperation operater;
	DepthFirstManager manager;
	ExuectionLoop loop;
	static String topFolder = Paths.AppDataDir.endsWith("/") ? Paths.AppDataDir : Paths.AppDataDir+"/";
	
	public static void main(String[] args) {
		boolean checkPrevious = true;
		String path = "/home/zhenxu/AndroidTestAPKPlayGround/APK2/net.mandaria.tippytipper.apk";
		String[] targets = {};
		
		//Be sure to unlock screen on the virual device
		SimplifiedEntryPoint entry = new SimplifiedEntryPoint(path);
		if(checkPrevious && entry.checkDumpDataExsitence()){
			boolean successful = entry.restore(); 
			if(!successful){
				System.out.println("Restoration failure");
				return;
			}
		}else{ 
			System.out.println("Start test");
			String[] serials = getDeviceId();
			if(serials == null){ System.out.println("Need two serial devices"); return;
			}else{ System.out.println("Serial: "+Arrays.toString(serials)); }
			entry.startTest(targets, serials);
		}
		
		Statistic.probe(entry.operater, entry.manager);
	}
	
	public SimplifiedEntryPoint(StaticApp app){
		this.app = app;
	}
	
	public SimplifiedEntryPoint(String path){
		app = StaticInfo.initAnalysis(path, force);
	}
	
	//do not put string like "" in targets
	void startTest(String[] targets, String[] serials){
		model = new UIModel();
		operater = new DualDeviceOperation(app, model, serials[0], serials[1]);
		manager = new DepthFirstManager(app, model);
		loop = new ExuectionLoop(manager, operater, model);
		
		String apkIdentity = getApkIdentityString();
		manager.setTargets(targets);
		setupLogger(apkIdentity);
//		loop.setDumpTag(getDumpTag(app));
		
		//set the limitation
		loop.setMaxDuration(maxTime);
		loop.setMaxIteration(maxIteration);
		manager.setMaxIndividualValidationTry(maxValidationTry);
		try{ loop.run();
		}catch(Exception e){ e.printStackTrace();
		}catch(Error e){ e.printStackTrace(); }
		
		dump(apkIdentity);
	}
	
	boolean restore(){
		String apkIdentity = this.getApkIdentityString();
		model = new UIModel();
		operater = new DualDeviceOperation(app, model, null,null);
		manager = new DepthFirstManager(app, model);
		File dumpFolder = new File(topFolder+"DumpFolder");
		if(dumpFolder.exists() == false) dumpFolder.mkdirs();
		
		Object managerData = Utility.readFromDisk( topFolder+"DumpFolder/"+apkIdentity+"_manager");
		Object operaterData = Utility.readFromDisk( topFolder+"DumpFolder/"+apkIdentity+"_operater");
//		Object uiModelData = Utility.readFromDisk( topFolder+"DumpFolder/"+apkIdentity+"_uimodel");
//	
		if(managerData == null 
				|| operaterData == null 
//				|| uiModelData == null	
				) return false;
		
		try{
//			this.model.restore(uiModelData);
			this.operater.restore(operaterData);
			this.manager.restore(managerData);
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}

	boolean checkDumpDataExsitence(){
		File f = new File(topFolder+"DumpFolder/");
		if(f.exists()){
			for(String fileName : f.list()){
				if(fileName.contains(this.getApkIdentityString()+"_manager")){
					return true;
				}
			}
		}
		return false;
	}
	
	void dump(String apkIdentity){
		Serializable manageData = manager.getDumpData();
		Serializable operaterData = operater.getDumpData();
//		Serializable modelData = model.getDumpObject();
		if(apkIdentity != null){
			File dumpFolder = new File(topFolder+"DumpFolder");
			if(dumpFolder.exists() == false) dumpFolder.mkdirs();
			Utility.writeToDisk(manageData, topFolder+"DumpFolder/"+apkIdentity+"_manager");
			Utility.writeToDisk(operaterData, topFolder+"DumpFolder/"+apkIdentity+"_operater");
//			Utility.writeToDisk(modelData, topFolder+"DumpFolder/"+apkIdentity+"_uimodel");
		}
	}
	
	String getApkIdentityString(){
		return app.getPackageName().replaceAll("\\.\\/\\\\", "");//delete '.', '/', '\'
	}
	
	
	
	void setupLogger(String logFileName){
		File logFolder = new File(parentFolderName);
		if(!logFolder.exists()){ logFolder.mkdirs(); }
		
		Logger fileLogging = new Logger(null); 
		File logFile = new File(parentFolderName+"/"+logFileName);
		try {
			PrintWriter pw = new PrintWriter(logFile);
			fileLogging.addLocalFilter(new InformationFilter(){
				@Override
				public boolean filtered(CurrentThreadInfo info, String tag,
						String message, int level) {
					String toPrint = Logger.StandardConstructor.construct(info, tag, message, level);
					pw.println(toPrint);
					pw.flush();
					return false;
				}
			});
		} catch (Exception e) {
			System.out.println("Log file initailization failure");
			e.printStackTrace();
		}
		Logger.addWorker(fileLogging);
		if(enableConsoleOutput){
			Logger.ConsoleLogger.addLocalFilter(new InformationFilter(){
				String filterList = 
						  "components.ViewDeviceInfo "
						+ "support.CommandLine "
						+ "components.system.WindowOverview "
//						+ "components.Executer "
						+ "components.BreakPointReader "
						+ "components.EventDeposit"
						+ "PlanBModule.AnchorSolver"
						+ "components.solver.YicesProcessInterface"
						; //can add more classes to the list
				@Override
				public boolean filtered(CurrentThreadInfo info, String tag,
						String message, int level) {
					String className = info.getCallerRecord().getClassName().trim();
					if(filterList.contains(className)){	
						return true;
					}
					return false;
				}
			});
		}else{
			Logger.ConsoleLogger.setEnableLocal(false);
		}
		
	}
	
	static String[] getDeviceId(){
		try {
			String adbVal = Configuration.getValue(Configuration.attADB);
			Process p = Runtime.getRuntime().exec(adbVal+" devices");
			InputStream input = p.getInputStream();
			Thread.sleep(200);
			byte[] buf = new byte[input.available()];
			input.read(buf);
			String read = new String(buf).trim();
			String[] parts = read.split("\n");
			if(parts.length < 3) return null;
			String[] result = new String[2];
			for(int i = 1; i<parts.length;i++){
				if(parts[i].contains(":")){
					result[0] = parts[i].trim().split(" |\\t")[0].trim();
					break;
				}
			}
			for(int i = 1; i<parts.length;i++){
				if(!parts[i].contains(":")){
					result[1] = parts[i].trim().split(" |\\t")[0].trim();
					break;
				}
			}
			return result;
		} catch (Exception e) { e.printStackTrace();}
		return null;
	}
}
