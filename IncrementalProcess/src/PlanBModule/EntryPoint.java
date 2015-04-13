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
import java.util.HashSet;
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

import components.Event;
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
	static boolean force = true;
	
	public static void main(String[] args) {
		
		String prefix = "/home/zhenxu/AndroidTestAPKPlayGround/APK2/";
		String path = "net.mandaria.tippytipper.apk";
		String[] targets = { 
				"com.actionbarsherlock.ActionBarSherlock:259",
				"com.actionbarsherlock.ActionBarSherlock:123",
				"com.actionbarsherlock.ActionBarSherlock:178",
				"com.actionbarsherlock.ActionBarSherlock:183",
				"com.actionbarsherlock.ActionBarSherlock:185",
				"com.actionbarsherlock.ActionBarSherlock:188",
				"com.actionbarsherlock.ActionBarSherlock:190",
				"com.actionbarsherlock.ActionBarSherlock:183",
				"com.actionbarsherlock.ActionBarSherlock:207",
				"com.actionbarsherlock.ActionBarSherlock:198",
				"com.actionbarsherlock.ActionBarSherlock:220",
				"com.actionbarsherlock.ActionBarSherlock:223",
				"com.actionbarsherlock.ActionBarSherlock:560",
				"com.actionbarsherlock.ActionBarSherlock:561",
				"com.actionbarsherlock.ActionBarSherlock:604",
				"com.actionbarsherlock.ActionBarSherlock:605",
				"com.actionbarsherlock.ActionBarSherlock:581",
				"com.actionbarsherlock.ActionBarSherlock:582",
				"com.actionbarsherlock.ActionBarSherlock:780",
				"com.actionbarsherlock.ActionBarSherlock:777",
				"com.actionbarsherlock.R$styleable:455",
				"com.actionbarsherlock.app.SherlockActivity:172",
				"com.actionbarsherlock.app.SherlockActivity:174",
				"com.actionbarsherlock.app.SherlockActivity:114",
				"com.actionbarsherlock.app.SherlockActivity:100",
				"com.actionbarsherlock.app.SherlockActivity:165",
				"com.actionbarsherlock.app.SherlockActivity:167",
				"com.actionbarsherlock.app.SherlockPreferenceActivity:172",
				"com.actionbarsherlock.app.SherlockPreferenceActivity:174",
				"com.actionbarsherlock.app.SherlockPreferenceActivity:114",
				"com.actionbarsherlock.app.SherlockPreferenceActivity:208",
				"com.actionbarsherlock.app.SherlockPreferenceActivity:100",
				"com.actionbarsherlock.app.SherlockPreferenceActivity:165",
				"com.actionbarsherlock.app.SherlockPreferenceActivity:167",
				"com.actionbarsherlock.internal.ActionBarSherlockNative:60",
				"com.actionbarsherlock.internal.ActionBarSherlockNative:190",
				"com.actionbarsherlock.internal.ActionBarSherlockNative:200",
				"com.actionbarsherlock.internal.ActionBarSherlockNative:203",
				"com.actionbarsherlock.internal.ActionBarSherlockNative:204",
				"com.actionbarsherlock.internal.ActionBarSherlockNative:212",
				"com.actionbarsherlock.internal.ActionBarSherlockNative:214",
				"com.actionbarsherlock.internal.ActionBarSherlockNative:215",
				"com.actionbarsherlock.internal.ActionBarSherlockNative:218",
				"com.actionbarsherlock.internal.app.ActionBarWrapper:30",
				"com.actionbarsherlock.internal.app.ActionBarWrapper:87",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:24",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:261",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:246",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:181",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:183",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:197",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:233",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:236",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:239",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:221",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:223",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:286",
				"com.actionbarsherlock.internal.view.menu.MenuItemWrapper:308",
				"com.actionbarsherlock.internal.view.menu.MenuWrapper:84",
				"com.actionbarsherlock.internal.view.menu.MenuWrapper:87",
				"com.actionbarsherlock.internal.view.menu.MenuWrapper:90",
				"com.actionbarsherlock.internal.view.menu.MenuWrapper:139",
				"com.actionbarsherlock.internal.view.menu.MenuWrapper:147",
				"com.actionbarsherlock.view.MenuInflater$MenuState:428",
				"com.actionbarsherlock.view.MenuInflater$MenuState:429",
				"com.actionbarsherlock.view.MenuInflater$MenuState:432",
				"com.actionbarsherlock.view.MenuInflater$MenuState:437",
				"com.actionbarsherlock.view.MenuInflater$MenuState:447",
				"com.actionbarsherlock.view.MenuInflater$MenuState:453",
				"com.actionbarsherlock.view.MenuInflater$MenuState:454",
				"com.actionbarsherlock.view.MenuInflater$MenuState:462",
				"com.actionbarsherlock.view.MenuInflater$MenuState:441",
				"com.actionbarsherlock.view.MenuInflater$MenuState:457",
				"com.actionbarsherlock.view.MenuInflater$MenuState:359",
				"com.actionbarsherlock.view.MenuInflater$MenuState:389",
				"com.actionbarsherlock.view.MenuInflater$MenuState:359",
				"com.actionbarsherlock.view.MenuInflater$MenuState:394",
				"com.actionbarsherlock.view.MenuInflater:145",
				"com.actionbarsherlock.view.MenuInflater:160",
				"com.actionbarsherlock.view.MenuInflater:163",
				"com.actionbarsherlock.view.MenuInflater:165",
				"com.actionbarsherlock.view.MenuInflater:171",
				"com.actionbarsherlock.view.MenuInflater:179",
				"com.actionbarsherlock.view.MenuInflater:181",
				"com.actionbarsherlock.view.MenuInflater:187",
				"com.actionbarsherlock.view.MenuInflater:188",
				"com.actionbarsherlock.view.MenuInflater:199",
				"com.actionbarsherlock.view.MenuInflater:120",
				"net.mandaria.tippytipperlibrary.R$styleable:608",
				"net.mandaria.tippytipperlibrary.TippyTipperApplication:65",
				"net.mandaria.tippytipperlibrary.activities.About:82",
				"net.mandaria.tippytipperlibrary.activities.Settings:96",
				"net.mandaria.tippytipperlibrary.activities.SplitBill:173",
				"net.mandaria.tippytipperlibrary.activities.SplitBill:169",
				"net.mandaria.tippytipperlibrary.activities.SplitBill:170",
				"net.mandaria.tippytipperlibrary.activities.SplitBill:110",
				"net.mandaria.tippytipperlibrary.activities.TippyTipper:251",
				"net.mandaria.tippytipperlibrary.activities.TippyTipper:253",
				"net.mandaria.tippytipperlibrary.activities.TippyTipper:257",
				"net.mandaria.tippytipperlibrary.activities.TippyTipper:258",
				"net.mandaria.tippytipperlibrary.activities.TippyTipper:262",
				"net.mandaria.tippytipperlibrary.activities.Total:464",
				"net.mandaria.tippytipperlibrary.activities.Total:267",
				"net.mandaria.tippytipperlibrary.errors.CustomExceptionHandler:117",
				"net.mandaria.tippytipperlibrary.errors.CustomExceptionHandler:132",
				"net.mandaria.tippytipperlibrary.preferences.DecimalPreference:68",
				"net.mandaria.tippytipperlibrary.preferences.DecimalPreference:104",
				"net.mandaria.tippytipperlibrary.preferences.DecimalPreference:141",
				"net.mandaria.tippytipperlibrary.preferences.DecimalPreference:149",
				"net.mandaria.tippytipperlibrary.preferences.NumberPickerPreference:71",
				"net.mandaria.tippytipperlibrary.preferences.NumberPickerPreference:100",
				"net.mandaria.tippytipperlibrary.preferences.NumberPickerPreference:132",
				"net.mandaria.tippytipperlibrary.preferences.NumberPickerPreference:140",
				"net.mandaria.tippytipperlibrary.preferences.SeekBarPreference:76",
				"net.mandaria.tippytipperlibrary.preferences.SeekBarPreference:93",
				"net.mandaria.tippytipperlibrary.preferences.SeekBarPreference:128",
				"net.mandaria.tippytipperlibrary.preferences.SeekBarPreference:111",
				"net.mandaria.tippytipperlibrary.preferences.SeekBarPreference:119",
				"net.mandaria.tippytipperlibrary.preferences.SeekBarPreference:142",
				"net.mandaria.tippytipperlibrary.preferences.SeekBarPreference:143",
				"net.mandaria.tippytipperlibrary.services.TipCalculatorService:158",
				"net.mandaria.tippytipperlibrary.services.TipCalculatorService:168",
				"net.mandaria.tippytipperlibrary.services.TipCalculatorService:175",
				"net.mandaria.tippytipperlibrary.services.TipCalculatorService:258",
				"net.mandaria.tippytipperlibrary.tasks.GetAdRefreshRateTask:110",
				"net.mandaria.tippytipperlibrary.tasks.GetAdRefreshRateTask:121",
				"net.mandaria.tippytipperlibrary.tasks.GetAdRefreshRateTask:39",
				"net.mandaria.tippytipperlibrary.tasks.GetInHouseAdsPercentageTask:110",
				"net.mandaria.tippytipperlibrary.tasks.GetInHouseAdsPercentageTask:38",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker$3:114",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker$3:120",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker$3:116",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker$3:117",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker$NumberPickerInputFilter:375",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker$NumberPickerInputFilter:383",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker$NumberPickerInputFilter:380",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker$NumberPickerInputFilter:386",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker$NumberRangeKeyListener:417",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:170",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:438",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:441",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:443",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:438",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:451",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:318",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:325",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:320",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:275",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:345",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:351",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:347",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:348",
				"net.mandaria.tippytipperlibrary.widgets.NumberPicker:288",
		};
		 
		startTest(prefix+path, targets);
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
		manager.setMaxIndividualValidationTry(5);
		
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
		loop.setMaxDuration(TimeUnit.MINUTES.toMillis(30));
		loop.setMaxIteration(300);
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
