package components;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import staticFamily.StaticStmt;
import support.ADBUtility;
import support.CommandLine;
import support.Logger; 
import symbolic.Blacklist;

public class BreakPointReader {
//	private JavaDebugBridge bridge;
	private StaticApp staticApp;
//	private List<String> previousMethodRoots;
	private String serial, packageName;
	private int localPort = 15975;
	private Process process; 
	private OutputStream out;
	private BufferedReader stdReader, errReader;
	private InputStream stdin, stderr;
	private List<String> breakpointsLog; 
	private List<StaticMethod> previousMethods; 
	
	public BreakPointReader(String serial){
		this.serial = serial;
	}
	
	/**
	 * Setup the JavaDebugBridge for all method roots and beneth
	 * @param staticApp
	 * @param methodRoots
	 */
	public boolean setup(StaticApp staticApp, List<String> methodRoots){
		Logger.trace("methodRoots: "+methodRoots);
		this.staticApp = staticApp;
		this.packageName = staticApp.getPackageName();
		breakpointsLog = new ArrayList<String>();
		if(setupBridge() == false){
			Logger.debug("Setup failure");
			return false; 	
		} 
		setBreakPoints(methodRoots);
		Logger.trace("ends");
		return true;
	}
	
	
	public List<List<String>> readExecLog(){
		Logger.trace();
		int tryCount = 0;
		final List<String> readLog = new ArrayList<String>(); //used to store whatever is read. 
		final List<Exception> eList = new ArrayList<Exception>(); // used to store exception.
		final Stack<StaticMethod> stack =new Stack<StaticMethod>();
		List<List<String>> result = new ArrayList<List<String>>();
		List<String> executionLog = new ArrayList<String>();
		int lineCount = 0, byteAvailiable , methodIndex = 1;
		stack.push(this.previousMethods.get(0));
		result.add(executionLog);
		
		while(true){			
			try {
				String line = readLine(stdin);
				Logger.trace("Read: "+line);
				if(line != null && !line.isEmpty()){
					readLog.add(line);
				}
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			if(eList.size() > 0){ Logger.debug("Exception occured"); break; }
			
			if(readLog.size() > lineCount){ //there is new reading
				String line = readLog.get(lineCount);
				if(line.trim().endsWith("Nothing suspended.")){ 
					Logger.trace("Nothing suspended");
					if(tryCount > 3){
						break;
					}else{
						tryCount += 1;
					}
				}
				lineCount += 1;
				
				if(line.contains("Breakpoint hit: ")){
//					Logger.trace("Hit: "+line);
					String bpInfo = line.substring(line.indexOf("Breakpoint hit: "));
					String methodInfo = bpInfo.split(", ")[1];
					String lineInfo = bpInfo.split(", ")[2].replace(",", "");
					String className = methodInfo.substring(0, methodInfo.lastIndexOf("."));
					int lineNumber = Integer.parseInt(lineInfo.substring(lineInfo.indexOf("=")+1, lineInfo.indexOf(" ")));
					executionLog.add(className + ":" + lineNumber);
					
					Logger.debug("Stack Peek: "+stack.peek().getSignature());
					StaticStmt statement = findStaticStmt(stack.peek(), bpInfo);
					if(statement == null){
						Logger.debug("Null statement: "+bpInfo);
					}else if(statement.endsMethod()){
						StaticMethod poped = stack.pop();
						Logger.trace("Pop: "+poped.getSignature());
					}else if(statement.invokesMethod()){
						String targetSig = (String)statement.getData();
						Logger.trace("Invokation: "+targetSig);
						if(targetSig != null){
							StaticMethod targetM = staticApp.findMethod(targetSig);
							if(targetM != null){
								Logger.debug(targetM.getSignature());
								StaticClass targetC = targetM.getDeclaringClass(staticApp);
								if(targetC != null){
									Logger.debug(targetC.getJavaName());
									boolean inBlackList = blacklistCheck(targetM);
									Logger.trace("inBlackList: "+inBlackList);
									if(!inBlackList){
										for (int i : targetM.getSourceLineNumbers()){
											this.setBreakPointAtLine(targetC.getJavaName(), i);
										}
										stack.push(targetM);
										Logger.trace("Push: "+stack.peek().getSignature());
									}
								}else{
									Logger.debug("targetC is null");
								}
							}else{
								Logger.debug("targetM is null");
							}
						}
					}else{
						Logger.trace("Normal line");
					}
					
					if(stack.isEmpty()){
						if(methodIndex < this.previousMethods.size()){
							stack.push(previousMethods.get(methodIndex));
							executionLog = new ArrayList<String>();
							result.add(executionLog);
							methodIndex += 1;
						}else{ 
							Logger.trace("Stack empty and no more method root ");
							break; 
						}
					}
					
					Logger.trace("continue");
					try {  out.write("cont\n".getBytes()); out.flush();
					} catch (IOException e) { }
				}
				
				
			}else{ //timeout for reading
				Logger.trace("continue");
				try {  out.write("cont\n".getBytes()); out.flush();
				} catch (IOException e) { }
//				break; 
			}
		}
		
		String[] channels = this.instantFlush();
		if(channels[0] != null){
			Logger.trace("Stdout: "+channels[0]);
		}
		if(channels[1] != null){
			Logger.trace("Stderr: "+channels[1]);
		}
		
		this.terminateProcess();
		return result;
	}
	
	private StaticStmt findStaticStmt(StaticMethod m, String bpInfo) {
		String methodInfo = bpInfo.split(", ")[1];
		String cN = methodInfo.substring(0, methodInfo.lastIndexOf("."));
		String lineInfo = bpInfo.split(", ")[2].replace(",", "");
		int newHitLine = Integer.parseInt(lineInfo.substring(lineInfo.indexOf("=")+1, lineInfo.indexOf(" ")));
		StaticClass c = staticApp.findClassByJavaName(cN);
		if (c == null){
			return null;
		}
		return m.getStmtByLineNumber(newHitLine);
	}
	
	private boolean blacklistCheck(StaticMethod m) {
		Blacklist bl = new Blacklist();
		StaticClass c = m.getDeclaringClass(staticApp);
		if (m == null || c == null)
			return false;
		return (bl.classInBlackList(c.getDexName()) || bl.methodInBlackList(m.getSignature()));
	}
	
	
	private int setBreakPoints(List<String> methodRoots){
		int count = 0;
		Logger.trace(methodRoots);
		List<StaticMethod> foundMethod = new ArrayList<StaticMethod>();
		for(String method: methodRoots){
			StaticMethod m = staticApp.findMethod(method);
			foundMethod.add(m);
			String className = m.getDeclaringClass(staticApp).getJavaName();
			for (int lineNumber : m.getSourceLineNumbers()){
				Logger.trace("setting breakpoint on "+className+", "+lineNumber);
				if(this.setBreakPointAtLine(className, lineNumber)){
					count += 1;
				}
			}
		}
		Logger.trace("Total of "+count+" lines.");
		previousMethods = foundMethod;
		return count;
	}
	
	private boolean setBreakPointAtLine(String className, int line) {
		//check if the break point was set. Return if so. 
		if (breakpointsLog.contains(className + ":" + line)){return true;}
		//send the message 
		String mes = "stop at "+className+":"+line+"\n";
		try { 
			out.write(mes.getBytes());
			out.flush();
		} catch (IOException e) { 
			e.printStackTrace(); 
			return false;
		}
		
		/*
		 * deterministically wait and read right amount of feed back
		 * The known feedback includes: 
		 * 1. Set break point ...  	//successful
		 * 2. Unable to set ...		//failure
		 * 3. Deferring ...			//successful but takes two lines
		 */
		try {
			while(true){
				String reading = stdReader.readLine().replace(">", "").trim();
				if(reading.contains("Set breakpoint ")){
					Logger.trace("Set on "+line+" is Sucessful");
					break;
				}else if(reading.contains("Unable ")){
					Logger.trace("Set on "+line+" is failure");
					Logger.trace("Read: "+reading);
					break;
				}else if(reading.startsWith("Deferring")){
					reading = stdReader.readLine();
					Logger.trace("Set on "+line+" is Sucessful but deffered");
					break;
				}else{
					Logger.trace("Unknown result: "+reading);
				}
			}
		} catch (IOException e) { e.printStackTrace(); }
		breakpointsLog.add(className + ":" + line);
		return true;
	}
	
	
	private void terminateProcess(){
		try { out.write("exit\n".getBytes());
			out.flush();
		} catch (IOException e) { }
	}
	
	private boolean setupBridge(){
		String pid = ADBUtility.getPID(packageName, serial);
		Logger.trace("PID: "+pid);
		while(pid == null){
			System.out.println("fail to get pid");
			CommandLine.requestInput();
			pid = ADBUtility.getPID(packageName, serial);
			Logger.trace("PID: "+pid);
		}
		
		CommandLine.executeADBCommand(" forward tcp:" + localPort + " jdwp:" + pid, serial);
		String errMes = CommandLine.getLatestStderrMessage();
		String stdMes = CommandLine.getLatestStdoutMessage();
		if(errMes != null && !errMes.isEmpty()){Logger.trace("Stderr: "+errMes); }
		if(stdMes != null && !stdMes.isEmpty()){Logger.trace("Stdout: "+stdMes); }
		
		try {
			process = Runtime.getRuntime().exec("jdb -sourcepath " + "src" + " -attach localhost:" + localPort);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		
		try { Thread.sleep(20);} catch (InterruptedException e) { }
		out = process.getOutputStream();
		stdin = process.getInputStream();
		stderr = process.getErrorStream();
		stdReader = new BufferedReader(new InputStreamReader(stdin));
		errReader = new BufferedReader(new InputStreamReader(stderr));	
		
		//Read until "Initializing jdb ..." show up
		Logger.trace();
		boolean once = false;
		long stt = System.currentTimeMillis();
		while(true){
			try { Thread.sleep(100);
			} catch (InterruptedException e) { }
			try {
				String line = stdReader.readLine();
				if(line == null) continue;
				Logger.trace(line);
				if(line.startsWith("Initializing jdb ...")) {break;}
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if(once && System.currentTimeMillis() - stt > 1000*5){
				once = false;
				System.out.println("JDB initialization taking too long.");
			}
		}
		return true;
	}

	private String[] instantFlush(){
		String[] reading = new String[2];
		try {
			if(stdin.available() > 0){
				int count = stdin.available();
				byte[] buf = new byte[count];
				stdin.read(buf);
				reading[0] = new String(buf).trim();
			}
			if(stderr.available() > 0){
				int count = stderr.available();
				byte[] buf = new byte[count];
				stdin.read(buf);
				reading[1] = new String(buf).trim();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return reading;
	}
	
	private String readLine(InputStream in) throws IOException{
		StringBuilder sb = new StringBuilder();
		byte[] buf = new byte[256];
		int index = 0;
		
		long startTime = System.currentTimeMillis();
		while(true){
			if(in.available() > 0){
				int read = in.read();
				if(read < 0){
					break;
				}else if(read == '\n'){
					index += 1;
					break;
				}else{
					buf[index] = (byte) read;
					index += 1;
					if(index == 256){
						sb.append(new String(buf).trim());
						index = 0;
						buf = new byte[256];
					}
				}
			}else{
				try { Thread.sleep(10);
				} catch (InterruptedException e) { }
				long currentTime = System.currentTimeMillis();
				if(currentTime - startTime > 300){ 
					break; 
				}
			}
		}
		if(index > 0){
			sb.append(new String(Arrays.copyOf(buf, index)));
		}
		return sb.toString();
	}
}
