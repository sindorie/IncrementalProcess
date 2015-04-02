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

import analysis.StaticInfo;
import staticFamily.StaticApp;
import support.Logger;
import support.Logger.CurrentThreadInfo;
import support.Logger.InformationFilter;

public class EntryPoint {

	public static void main(String[] args) {
		
		String prefix = "/home/zhenxu/workspace/APK/";
		String path = 
//				"Dragon.apk";
				"TestProcedure.apk";
//				"Beta1.apk";
//				"CalcA.apk";
//		"backupHelper.apk";
//		"net.mandaria.tippytipper.apk";
		
		String[] serials = getDeviceId();
		if(serials == null){ System.out.println("Need two serial devices"); return;
		}else{ System.out.println("Serial: "+Arrays.toString(serials)); }

		setupLogger();

		StaticApp app = StaticInfo.initAnalysis(prefix+path, false);
		UIModel model = new UIModel();
		DualDeviceOperation operater = new DualDeviceOperation(app, model, serials[0], serials[1]);
		DepthFirstManager manager = new DepthFirstManager(app, model);
		
		final ExuectionLoop loop = new ExuectionLoop(manager, operater, model);
		
		loop.enableCycleBreak(true);
		
		new Thread(new Runnable(){
			@Override
			public void run() {
				Scanner sc = new Scanner(System.in);
				while(true){
					if(sc.hasNextLine()){
						String line = sc.nextLine();
						if(line == null) continue;
						line = line.trim();
						if(line.equalsIgnoreCase("1")){
							loop.enableCycleBreak(true); //dont care synchronization
						}else if(line.equals("2")){
							loop.enableCycleBreak(false);
						}else if(line.equals("3") || line.isEmpty()){
							loop.nextCycle();
							
						}else if(line.equals("4") || line.isEmpty()){
							
							
							
							
						}else if(line.equals("9")){
							try {
								FileOutputStream fout = new FileOutputStream("generated/file");
								ObjectOutputStream oos = new ObjectOutputStream(fout);  
								synchronized(loop){
									oos.writeObject(loop);
									oos.close();
									System.out.println("Done");
								}
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							
						}else if(line.equalsIgnoreCase("0")){
							break;
						}
					}
				}	sc.close();
			}
		}).start();

		
//		System.out.println(Logger.getGlobalWokerAmount());
		loop.run();
	}
	
	private static  String[] getDeviceId(){
		try {
			Process p = Runtime.getRuntime().exec("adb devices");
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
				/*Write to table*/
//				DefaultTableModel model = classToModel.get(className);
//				if(model == null){
//					model = new DefaultTableModel();
//					model.setColumnIdentifiers(columnId);
//					JTable table = new JTable();
//					JScrollPane scroll = new JScrollPane();
//					scroll.setViewportView(table);
//					outputPane.addTab(className, scroll);
//					table.setModel(model);
//					
//					classToModel.put(className, model);
//				}
//				StackTraceElement element = info.getCallerRecord();
//				String[] row = new String[]{
//						info.time+"",
//						info.threadName,
//						info.threadId+"",
//						element.getClassName()+"",
//						element.getMethodName(),
//						element.getLineNumber()+"",
//						tag, level +"",
//						message==null?"":message
//				};
//				model.addRow(row);
				
				
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
			String filterList = "components.ViewDeviceInfo "
					+ "support.CommandLine"
					+ "components.system.WindowOverview"; 
			@Override
			public boolean filtered(CurrentThreadInfo info, String tag,
					String message, int level) {
				String className = info.getCallerRecord().getClassName();
				if(level < Logger.LEVEl_DEBUG && filterList.contains(className)){	
					return true;
				}
				return false;
			}
		});
		
		
//		JTextArea area = new JTextArea();
//		JScrollPane sc = new JScrollPane();
//		sc.setViewportView(area);
//		outputPane.add("Exception", sc);
//		
//		Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(){
//			@Override
//			public void uncaughtException(Thread t, Throwable e) {
//				StringBuilder sb = new StringBuilder();
//				sb.append(t.getName()+" : "+t.getName()+"\n");
//				sb.append(e.getMessage()+"\n");
//				for(StackTraceElement se : t.getStackTrace()){
//					sb.append(se.getClassName()+", ");
//					sb.append(se.getMethodName()+", ");
//					sb.append(se.getLineNumber()+"\n");
//				}
//				area.append(sb.toString());
//			}
//		});
	}


	

}
