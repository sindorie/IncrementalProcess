package PlanBModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import components.system.Configuration;

import PlanBModule.AbstractManager.Decision;
import analysis.StaticInfo;
import staticFamily.StaticApp;
import support.CommandLine;
import support.Logger;
import support.Logger.CurrentThreadInfo;
import support.Logger.InformationFilter;

public class EntryPoint {

	public static void main(String[] args) {
		
		String prefix = "/home/zhenxu/workspace/APK/";
		String path = 
				"Dragon_interface2.apk";
//				"Dragon_interface.apk";
//				"Dragon-v1.apk";	
//				"Dragon.apk";	
//				"TestProcedure.apk";
//				"Beta1.apk";
//				"CalcA.apk";
//		"backupHelper.apk";
//		"net.mandaria.tippytipper.apk";
//			"TestField.apk";
//		"Dragon_double.apk";
		
		boolean force = true;
//		boolean force = false;
		
		String[] targets = {
				/*Dragon_interface2.apk*/
				"com.example.dragon.MainActivity:51",
				
				/*Dragon_interface.apk*/
//				"com.example.dragon.MainActivity:37",
				
				/*Dragon.apk*/
//				"com.example.dragon.SecondLayout:86",
				
				/*Dragon-v1.apk*/
//				"com.example.dragon.MainActivity:119",
//				"com.example.dragon.SecondLayout:72",
//				"com.example.dragon.SecondLayout:84",
//				"com.example.dragon.SecondLayout:79",
//				"com.example.dragon.SecondLayout:90",
				
				/*net.mandaria.tippytipper.apk*/
//				"net.mandaria.tippytipperlibrary.activities.Total:259",
//				"net.mandaria.tippytipperlibrary.activities.Total:344",
//				"net.mandaria.tippytipperlibrary.activities.Total:467",
//				"net.mandaria.tippytipperlibrary.activities.Total:307",
//				"net.mandaria.tippytipperlibrary.activities.Total:377",
//				"net.mandaria.tippytipperlibrary.activities.TippyTipper:254",
//				"net.mandaria.tippytipperlibrary.activities.TippyTipper:258",
		};

		
		
		
		String[] serials = getDeviceId();
		if(serials == null){ System.out.println("Need two serial devices"); return;
		}else{ System.out.println("Serial: "+Arrays.toString(serials)); }

		setupLogger();
		StaticApp app = StaticInfo.initAnalysis(prefix+path, force);
		UIModel model = new UIModel();
		DualDeviceOperation operater = new DualDeviceOperation(app, model, serials[0], serials[1]);
		DepthFirstManager manager = new DepthFirstManager(app, model);
		manager.setTargets(targets);
		
		final ExuectionLoop loop = new ExuectionLoop(manager, operater, model);
		
		loop.setMaxIteration(100);
		
//		loop.setCheckCallBack(new ExuectionLoop.CheckCallBack() {
//			boolean cycleBreak = false;
//			@Override
//			public void onOperationFinish(ExuectionLoop loop) {
//				if(cycleBreak){
//					String line = CommandLine.requestInput();
//					if(line.equals("1")){
//						cycleBreak = !cycleBreak;
//					}else if(line.equals("9")){
//						
//					}
//				}else if(CommandLine.hasNextLine()){
//					String line = CommandLine.requestInput();
//					if(line.equals("1")){
//						cycleBreak = !cycleBreak;
//					}else if(line.equals("9")){
//						
//					}
//				}
//			}
//			@Override
//			public boolean onIterationStart(ExuectionLoop loop) {
//				return false;
//			}
//			
//			@Override
//			public void onDecisionMade(ExuectionLoop loop, Decision nextOperation) {}
//		});
		
		loop.run();
	}
	
	
	private static  String[] getDeviceId(){
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
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return null;
	}

	public static void setupLogger(){
		File logFolder = new File("LogFolder");
		if(!logFolder.exists()){
			logFolder.mkdirs();
		}

		final JTabbedPane outputPane = new JTabbedPane();
		Logger.registerJPanel("Class category", outputPane);
		final Map<String,JTextArea> classToArea = new HashMap<String,JTextArea>();
//		final Map<String, DefaultTableModel> classToModel = new HashMap<String,DefaultTableModel>();
		final Map<String, PrintWriter> classToWriter = new HashMap<String, PrintWriter>();
		Logger log = new Logger(null); 
		
		final String[] columnId = new String[]{
				"Time", "tName","tId","Class","Method","Line","Tag","Level","Message"
		};
		
		log.addLocalFilter(new InformationFilter(){
			@Override
			public boolean filtered(CurrentThreadInfo info, String tag, String message, int level) {
				String className = info.getCallerRecord().getClassName(); 
				/*Wrtie to text area*/
				String result = Logger.StandardConstructor.construct(info, tag, message, level);
				
				JTextArea area = new JTextArea();
				if(classToArea.containsKey(className)){
					area = classToArea.get(className);
				}else{
					area = new JTextArea();
					JScrollPane jsp = new JScrollPane();
					jsp.setViewportView(area);
					outputPane.add(className, jsp);
					classToArea.put(className, area);
				}
				area.append(result);
				
				/*Write to file*/
				if(!result.endsWith("\n")) result+="\n";
				PrintWriter writer = null;
				if(classToWriter.containsKey(className)){
					 writer = classToWriter.get(className);
				}else{
					File logFile = new File("LogFolder/"+className+".txt");
					try {
						writer = new PrintWriter(logFile);
					} catch (FileNotFoundException e) { 
						e.printStackTrace();
					}
					classToWriter.put(className, writer);
				}
				writer.print(result);
				writer.flush();
				
				return true; //on purpose which means all message is filtered
			}
		});
		
		Logger.addWorker(log);
		Logger.ConsoleLogger.addLocalFilter(new InformationFilter(){
			String filterList = 
					  "components.ViewDeviceInfo "
					+ "support.CommandLine "
					+ "components.system.WindowOverview "
//					+ "components.Executer "
					+ "components.BreakPointReader "
//					+ "components.EventDeposit"
//					+ "PlanBModule.AnchorSolver"
//					+ "components.solver.YicesProcessInterface"
					; 
			
			@Override
			public boolean filtered(CurrentThreadInfo info, String tag,
					String message, int level) {
				String className = info.getCallerRecord().getClassName().trim();
				if(filterList.contains(className)){	
					//level <= Logger.LEVEl_DEBUG && 
					return true;
				}
				return false;
			}
		});
	}


	

}
