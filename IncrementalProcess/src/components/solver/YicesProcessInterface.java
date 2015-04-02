package components.solver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import components.system.Configuration;

import support.Logger;

public class YicesProcessInterface implements ProblemSolver {
	public static String yicesLocation = Configuration.getValue(Configuration.attYics);
	private Process yicesProcess;
	private InputStream readChannel, errorChannel;
	private OutputStream writeChannel;
	private long maxtime = 1000 * 20; // 20s
	private String path;
	private boolean newProcess = true;
	
	public YicesProcessInterface(String path) throws IOException {
		this.path = path;
	}

	public boolean solve(String... statements) {	
		if(statements != null){
			for(String st : statements){
				Logger.trace(st);
			}
		}
		
		if(newProcess){
			this.terminateProcess();
		}
		
		Logger.trace();
		
		if(this.yicesProcess == null){
			try {
				startProcess();
			} catch (IOException e) {
				System.out.println("start process fails");
				throw new AssertionError();
			}
		}
		
		try {
			long end = System.currentTimeMillis() + maxtime;
			boolean result = false, timeout = false;
			int count;

			
			
			count = readChannel.available();

			if (count > 0) {
				byte[] buf = new byte[count];
				readChannel.read(buf);
			}
			for (String stat : statements) {
				writeChannel.write(stat.getBytes());
			}
			writeChannel.flush();

			Logger.trace();
			
			while (true) {
				try { Thread.sleep(100); } catch (InterruptedException e) { }
				
				long current = System.currentTimeMillis();
				count = readChannel.available();
				if (count > 0) {
					byte[] buf = new byte[count];
					readChannel.read(buf);
					String msg = new String(buf).trim();
					if (msg.equalsIgnoreCase("sat")) {
						result = true;
						break;
					} else if (msg.equalsIgnoreCase("unsat")) {
						result = false;
						break;
					} else {
						System.out.println("unidentified: " + msg);
						result = false;
						break;
					}
				}

				if (current >= end) {
					timeout = true;
					break;
				}
				
			}

			if (timeout) {
				writeChannel.write("(exit)\n".getBytes());
				writeChannel.flush();
				this.yicesProcess.destroy();
				try { Thread.sleep(50);
				} catch (InterruptedException e) { }
				startProcess();
			}

			
			int errorCount = errorChannel.available();
			if(errorCount > 0){
				byte[] buf = new byte[errorCount];
				errorChannel.read(buf);
				String er = new String(buf).trim();
				Logger.debug(er);
			}
			Logger.debug("Sat: "+result);
			if(newProcess) terminateProcess();
			return result;
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		return false;
	}

	private void terminateProcess(){
		if(yicesProcess!= null) yicesProcess.destroy();
		yicesProcess = null;
	}
	
	private void startProcess() throws IOException {
		yicesProcess = Runtime.getRuntime().exec(path);
		readChannel = yicesProcess.getInputStream();
		errorChannel = yicesProcess.getErrorStream();
		writeChannel = yicesProcess.getOutputStream();
	}
}
