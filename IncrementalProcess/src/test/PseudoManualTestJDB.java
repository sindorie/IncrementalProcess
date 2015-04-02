package test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import support.ADBUtility;
import support.CommandLine;
import support.Logger;

public class PseudoManualTestJDB {

	/**
	 * 1. setup JDB 
	 * 2. set break points
	 * 3. wait for command line input
	 * 4. read breakpoints 
	 * 
	 * @param args
	 */
	public static void main(String[] args){
		Logger.setEnableGlobal(false);
		
		int[] internalList = { 522, 1438 };
		int[] mainList = { 25,34,35,26,36,37,38,39,28 };
		String internalString = "android.support.v7.internal.widget.ActionBarOverlayLayout";
		String mainString = "com.example.testprocedure.MainActivity";
		
		PseudoManualTestJDB test = new PseudoManualTestJDB();
		boolean sucessful = test.setup("015d3f1936080c05", "com.example.testprocedure");
		if(sucessful == false){
			System.out.println("Failure");
			return;
		}
		
		test.setBreakpoint(internalString, internalList);
		test.setBreakpoint(mainString, mainList);

		
		System.out.println("Waiting for any key");
		CommandLine.requestInput();
		List<String> list = test.readHitLines();
		
		
//		System.out.println("Finish reading:\n"+list);
		
		test.instantFlush();
		test.terminateProcess();
		System.out.println("Terminated");
	}
	
	String serial;
	String packageName;
	int localPort = 15975;
	Process process; 
	OutputStream out;
	BufferedReader stdReader, errReader;
	InputStream stdin, stderr;
	
	boolean setup(String serial, String packageName){
		this.serial = serial;
		this.packageName = packageName;
		String pid = ADBUtility.getPID(packageName, serial);
		System.out.println("PID: "+pid);
		if(pid == null || !pid.matches("\\d*")){
			System.out.println("Get pid failure");
			return false;
		}
		CommandLine.executeADBCommand(" forward tcp:" + localPort + " jdwp:" + pid, serial);
		String errMes = CommandLine.getLatestStderrMessage();
		String stdMes = CommandLine.getLatestStdoutMessage();
		if(errMes != null && !errMes.isEmpty()){System.out.println("Stderr: "+errMes); }
		if(stdMes != null && !stdMes.isEmpty()){ System.out.println("Stdout: "+stdMes); }
		
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
		while(true){
			try {
				String line = stdReader.readLine().trim();
				System.out.println(line);
				if(line.equals("Initializing jdb ...")) {break;}
			} catch (IOException e) {
				e.printStackTrace();
			}
			try { Thread.sleep(5);
			} catch (InterruptedException e) { }
		}
		return true;
	}
	
	/**
	 * If time after last reading exceed a limit than treat as the end of reading
	 * @return
	 */
	List<String> readHitLines(){
		final List<String> exLog = new ArrayList<String>();
		final List<Exception> exc = new ArrayList<Exception>();
		int count = 0;
		int tries = 0;
		while(true){
			try { Thread.sleep(50); } catch (InterruptedException e1) { }
			final Thread readThread = new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						String result = stdReader.readLine();
						if(result == null) return;
						System.out.println("Thread reading: "+result);
						exLog.add(result);
					} catch (IOException e) {
						e.printStackTrace();
						exc.add(e);
					} catch (Exception e1){
						e1.printStackTrace();
					}
				}
			});
			readThread.setPriority(Thread.MAX_PRIORITY);
			readThread.start();
			try { readThread.join(400);
			} catch (InterruptedException e) {  e.printStackTrace(); }
			if(readThread.isAlive()){ readThread.interrupt(); }
			
			if(exc.size() > 0){
				System.out.println("Exception occur");
				break;
			}
			if(exLog.size() > count){ 
				String lastestReading = exLog.get(count);
				if(lastestReading.endsWith("Nothing suspended.")){
					break;
				}
				tries = 0;
				count += 1; // there is reading
				try { jdbContinue();
				} catch (IOException e) { }
			}else{
				if(tries < 3){
					System.out.println("Give one more try");
					tries += 1;
				}else{ 
					tries += 1;
					try {
						int aval = stdin.available();
						if(aval > 0){
							byte[] buf = new byte[aval];
							stdin.read(buf);
							String lastMessage = new String(buf).trim();
							exLog.add(lastMessage);
							System.out.println(lastMessage);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
					System.out.println("No more reading");
					break; 
				}
			}
		}
		return exLog;
	}
	
	void jdbContinue() throws IOException{
		out.write("cont\n".getBytes());
		out.flush();
		try { Thread.sleep(10); } catch (InterruptedException e) { }
	}
	
	int setBreakpoint(String className , int[] lines){
		System.out.println("Setting for: "+className);
		int count = 0;
		for(int line : lines){
			String mes = "stop at "+className+":"+line+"\n";
			try { 
				out.write(mes.getBytes());
				out.flush();
			} catch (IOException e) { 
				e.printStackTrace(); 
				return count;
			}
		
			try {
				
				while(true){
					String reading = stdReader.readLine().replace(">", "").trim();
					if(reading.startsWith("Set")){
						System.out.println("Set on "+line+" is Sucessful");
						count += 1;
						break;
					}else if(reading.startsWith("Unable")){
						System.out.println("Set on "+line+" is failure");
						break;
					}else if(reading.startsWith("Deferring")){
						reading = stdReader.readLine();
						System.out.println("Set on "+line+" is Sucessful but deffered");
						break;
					}else{
						System.out.println("Unknown result: "+reading);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return count;
	}
	
	void instantFlush(){
		try {
			if(stdin.available() > 0){
				int count = stdin.available();
				byte[] buf = new byte[count];
				stdin.read(buf);
				System.out.println(new String(buf));
			}
			if(stderr.available() > 0){
				int count = stderr.available();
				byte[] buf = new byte[count];
				stdin.read(buf);
				System.out.println(new String(buf));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	
	List<String> readStream(final BufferedReader reader, final long timeout){
		final List<String> list = new ArrayList<String>();
		final List<Exception> eList = new ArrayList<Exception>();
		int count = 0;
		while(true){
			Thread thread = new Thread(new Runnable(){
				@Override
				public void run() {
					try {
						String line = reader.readLine();
						list.add(line);
					} catch (IOException e) {
						e.printStackTrace();
						eList.add(e);
					}
				}
			});
			
			thread.start();
			try { thread.join(timeout); } catch (InterruptedException e) {}
			if(eList.size() > 0){ break;}
			if(list.size() > count){ count += 1;
			}else{break; }
		}
		return list;
	}
	
	void terminateProcess(){
		try { out.write("exit\n".getBytes());
			out.flush();
		} catch (IOException e) { }
	}
	
}
