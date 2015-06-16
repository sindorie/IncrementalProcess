package PlanBModule;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import components.Event;
import components.EventSummaryPair;
import components.system.Configuration;
import analysis.StaticInfo;
import staticFamily.StaticApp;
import support.Logger;
import support.Logger.CurrentThreadInfo;
import support.Logger.InformationFilter;
import tools.CoverageStats;

public class EntryPoint {
	static boolean force = false;
	
	static int maxIteration = Integer.MAX_VALUE;
	static long maxTime = TimeUnit.MINUTES.toMillis(30);
	static int maxValidationTry = 20;
	
	public static void main(String[] args) {
		
		String name = "/home/zhenxu/AndroidTestAPKPlayGround/APK/Dragon.apk";
		try{
			String[] targets = {

			};
			startTest(name, targets);
		 }catch(Exception e){
			 e.printStackTrace();
		 }
		
		
	}
	
	static void startTest(String path, String[] targets){
		String[] serials = getDeviceId();
		if(serials == null){ System.out.println("Need two serial devices"); return;
		}else{ System.out.println("Serial: "+Arrays.toString(serials)); }
//		Logger.ConsoleLogger.setEnableLocal(false);
		setupLogger(); 
		
		StaticApp app = StaticInfo.initAnalysis(path, force);
		UIModel model = new UIModel();
		DualDeviceOperation operater = new DualDeviceOperation(app, model, serials[0], serials[1]);
		DepthFirstManager manager = new DepthFirstManager(app, model);
		manager.setTargets(targets);
		manager.setMaxIndividualValidationTry(maxValidationTry);
		
		DepthFirstManager.CallBack cheker = new DepthFirstManager.CallBack(){
			int totalNewEventCount = 0, validationCount = 0;
			Set<String> totalLog = new HashSet<String>();
			@Override
			public void check(List<Event> newEvents,
					List<EventSummaryPair> list, 
					Set<String> log,
					EventSummaryPair executed) {
				System.out.println("New events size: "+(newEvents == null?0:newEvents.size()));
				System.out.println("New validation size: "+(list == null?0:list.size()));
				
				totalNewEventCount += (newEvents == null?0:newEvents.size());
				validationCount += (list == null?0:list.size());
				if(log != null) totalLog.addAll(log);
				
//				System.out.println("Waiting for input:");
//				String line = CommandLine.requestInput();
			}
		};
		manager.setCallBack(cheker);
		
		ExuectionLoop loop = new ExuectionLoop(manager, operater, model);
		loop.setMaxDuration(maxTime);
		loop.setMaxIteration(maxIteration);
		loop.run();
		
		 
		Set<String> hits = manager.getAllHitList();
		CoverageStats statistic = new CoverageStats();
		Map<String, Set<String>> clsLineMis = statistic.getMissingLines(hits, app);
		Map<String, List<EventSummaryPair>> esData = operater.getESDeposit().data;
		
		/* Statistic */
		
		System.out.println("Line coverage");
		int hitTotalSize = hits.size();
		System.out.println("Hit size: "+hitTotalSize);
		int totalMiseed = 0;
		
		for(Entry<String,Set<String>>  missed : clsLineMis.entrySet()){
			System.out.println(String.join("\n", missed.getValue()));
		}
		
		
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
		
		
		Map<String,String> targetDetail = manager.getTargetReachDetail();
		File f = new File("hitLines");
		f.mkdir();
		String[] chunk = path.split("\\/");
		File f1 =  new File("hitLines/"+chunk[chunk.length-1].replace(".", ""));
		try {
			PrintWriter pw = new PrintWriter(f1);
			pw.println("Hit Lines:");
			for(String lines : hits){
				pw.println(lines);
			}
			pw.println("Coverage: "+hitTotalSize+"/"+(totalMiseed+hitTotalSize));
			
			pw.println("Reached targets:");
			for(Entry<String, String> entry : targetDetail.entrySet()){
				pw.println(entry.getKey()+" hit via: ");
				pw.println(entry.getValue());
			}
			pw.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
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
