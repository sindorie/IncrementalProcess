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
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.table.DefaultTableModel;

import components.EventSummaryPair;
import components.system.Configuration;
import PlanBModule.AbstractManager.Decision;
import PlanBModule.DepthFirstManager.CallBack;
import analysis.StaticInfo;
import staticFamily.StaticApp;
import support.CommandLine;
import support.Logger;
import support.Logger.CurrentThreadInfo;
import support.Logger.InformationFilter;
import tools.CoverageStats;

public class EntryPoint {

	public static void main(String[] args) {
		
		String prefix = "/home/zhenxu/workspace/APK/";
		String path = 
//				"Dragon_interface2.apk";
//				"Dragon_interface.apk";
//				"Dragon-v1.apk";	
//				"Dragon.apk";	
//				"TestProcedure.apk";
//				"Beta1.apk";
				"info.bpace.munchlife.apk";
//				"CalcA.apk";
//		"backupHelper.apk";
//		"net.mandaria.tippytipper.apk";
//			"TestField.apk";
//		"Dragon_double.apk";
		
		boolean force = true;
//		boolean force = false;
		
		String[] targets = {
				/*CalcA.apk*/
				
				
				
				/*Dragon_interface2.apk*/
//				"com.example.dragon.MainActivity:51",
				
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
		manager.setMaxIndividualValidationTry(3);
		
		
		manager.setCallBack(new CallBack(){
			@Override
			public void checkExecutionLog(Set<String> log) { // dont modify the data
				// TODO Auto-generated method stub
			}
		});
		
		
		final ExuectionLoop loop = new ExuectionLoop(manager, operater, model);
		loop.setMaxDuration(TimeUnit.MINUTES.toMillis(20));
		loop.setMaxIteration(200);
		loop.run();
		
		 
		Set<String> hits = manager.getAllHitList();
		CoverageStats statistic = new CoverageStats();
		Map<String, Set<String>> clsLineMis = statistic.getMissingLines(hits, app);
		Map<String, List<EventSummaryPair>> esData = operater.getESDeposit().data;
		
		
		System.out.println("Line coverage");
		int hitTotalSize = hits.size();
		System.out.println("Hit size: "+hitTotalSize);
		int totalMiseed = 0;
		for(Entry<String,Set<String>>  missed : clsLineMis.entrySet()){
			totalMiseed += missed.getValue().size();
			System.out.println(missed.getKey()+" missed line amount: "+missed.getValue().size());
		}
		System.out.println("Missed line amount: "+totalMiseed);
		System.out.println("Cumulative line coverage: "+hitTotalSize+"/"+(totalMiseed+hitTotalSize)+"(hit/total)");
		
		System.out.println("Path coverage");
		int totalES = 0;
		int concrete = 0;
		for(Entry<String, List<EventSummaryPair>> entry : esData.entrySet()){
			int localTotal = entry.getValue().size();
			int localConcrete = 0;
			for(EventSummaryPair esPair :entry.getValue()){
				if(esPair.isConcreateExecuted()) localConcrete+=1;
			}
			concrete += localConcrete;
			totalES += localTotal;
			System.out.println(entry.getKey()+" : "+localConcrete+"/"+localTotal);
		}
		System.out.println("Cumulative: "+ concrete+"/"+totalES);
		
//		manager.getDumpData();
//		operater.getDumpData();
//		try {
//			FileOutputStream fout = new FileOutputStream("generated/statistic.dump");
//			ObjectOutputStream oos = new ObjectOutputStream(fout);
//			List<Object> list = new ArrayList<Object>();
//			list.add(hits);
//			list.add(esData);
//			list.add(clsLineMis);
//			oos.writeObject(list);
//			
//			oos.flush();
//			oos.close();
//			fout.close();
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch(Exception e){
//			e.printStackTrace();
//		}
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
