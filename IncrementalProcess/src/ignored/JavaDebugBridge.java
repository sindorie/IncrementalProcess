package ignored;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import support.ADBUtility;
import support.CommandLine;
import support.Logger;

public class JavaDebugBridge {
	private int localPort =7772;
	private String srcPath = "src";
	private Process pc;
	private OutputStream out;
	private BufferedReader in;
	private InputStream err;
	private ArrayList<String> breakpointsLog = new ArrayList<String>();
	private String serial;
	
	public JavaDebugBridge(String serial){
		this.serial = serial;
	}
	
	public void init(String packageName) {
		String pID = ADBUtility.getPID(packageName, serial);
		Logger.trace("PID: "+pID);
		
		try {
			CommandLine.executeADBCommand(" forward tcp:" + localPort + " jdwp:" + pID, serial);
			Logger.trace(CommandLine.getLatestStdoutMessage());
			Logger.trace(CommandLine.getLatestStderrMessage());
			
			pc = Runtime.getRuntime().exec("jdb -sourcepath " + srcPath + " -attach localhost:" + localPort);
			out = pc.getOutputStream();
			in = new BufferedReader(new InputStreamReader(pc.getInputStream()));
			err = pc.getErrorStream();
			String line = "";
			while(!line.equals("TIMEOUT")){
				line = this.readLine();
				Logger.trace(line);
			}
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public void flush(){
		String line = "";
		while(!line.equals("TIMEOUT")){
			line = this.readLine();
			Logger.trace(line);
		}
	}
	
	public String readLine() {
		String result = "";
		ExecutorService executor = Executors.newFixedThreadPool(1);
		Future<String> newestLine = executor.submit(
				new Callable<String>(){
					@Override
					public String call() throws Exception {
						String result = "";
						try {
							result = in.readLine();
						} catch (IOException e) { 
							Logger.trace(e.getMessage());
						}
						return result;
					}
				});
		
		try {
			result = newestLine.get(300, TimeUnit.MILLISECONDS);
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
		} catch (TimeoutException e) {
			result = "TIMEOUT";
		}
		
//		Logger.trace("Reading: "+result);

		try {
			int amount = err.available();
			if(amount > 0){
				byte[] buf = new byte[amount];
				err.read(buf);
				String s = new String(buf).trim();
				if(s!= null && s.isEmpty()){
					Logger.debug("err: "+s);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		executor.shutdown();
		return result;
	}
	
	public void setBreakPointAtLine(String className, int line) {
		try {
			if (!breakpointsLog.contains(className + ":" + line)) {
				out.write(("stop at " + className + ":" + line + "\n").getBytes());
				out.flush();
				breakpointsLog.add(className + ":" + line);
				String s = "";
				s = readLine();
				Logger.trace(s);
				if(s.startsWith("Deferring")){
					s = readLine();
					Logger.trace(s);
				}
			}else {
				System.out.println("breakpoints already set, no need to set again.");
			}
		}	catch (Exception e) { e.printStackTrace(); }
	}
	
	public void cont() {
		try {
			Logger.trace("JDB continued");
			out.write("cont\n".getBytes());
			out.flush();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	public ArrayList<String> getLocals() {
		ArrayList<String> result = new ArrayList<String>();
		try {
			out.write("locals\n".getBytes());
			out.flush();
			String line = "";
			while (!line.equals("TIMEOUT")) {
				line = readLine();
				if (line.equals("Local variables:") || line.equals("Method arguments:") || line.equals("TIMEOUT"))
					continue;
				result.add(line);
			}
		}	catch (Exception e) {e.printStackTrace();}
		return result;
	}
	
	public void setMonitorCont(boolean flag) {
		try {
			if (flag) 	out.write("monitor cont\n".getBytes());
			else 		out.write("unmonitor 1\n".getBytes());
			out.flush();
		}	catch (Exception e) {e.printStackTrace();}
	}
	
	public void exit() {
		try {
			out.write("exit\n".getBytes());
			out.flush();
		}	catch (Exception e) { e.printStackTrace(); }
	}
	
	public Process getProcess() {
		return pc;
	}

	public int getLocalPort() {
		return localPort;
	}
	
	public void setLocalPort(int localPort) {
		this.localPort = localPort;
	} 
	
	public String getSrcPath() {
		return srcPath;
	}

	public void setSrcPath(String srcPath) {
		this.srcPath = srcPath;
	}
	
}
