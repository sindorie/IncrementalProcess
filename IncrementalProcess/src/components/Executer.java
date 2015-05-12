package components;

import java.util.Arrays;

import android.view.KeyEvent;
import support.CommandLine;
import support.Logger;

public class Executer { 
	private String serial;
	private EventDeposit deposit;
	private boolean recordEvent = true;
	private double[] corrdinatesRatio;
	private boolean noReinstall = false;
	
	public Executer(String serial){
		this(serial,null);
	}
	
	public Executer(String serial,EventDeposit deposit){
		this.serial = serial;
		this.deposit = deposit;
	}

	public void applyEvent(Event event){
		this.applyEvent(event, true);
	}
	
	public void setForceNotReinstall(boolean noReinstall){
		this.noReinstall = noReinstall;
	}
	
	public void applyEvent(Event event, boolean sleep){ 
		Logger.trace(""+event);
		CommandLine.clear();
		int type = event.getEventType();
		switch(type){
		case EventFactory.iLAUNCH:{
			String packageName = (String) event.getAttribute(EventFactory.pkgName);
			String actName = (String) event.getAttribute(EventFactory.actName);
			String shellCommand = "am start -f 32768 -W -n " + packageName + "/" + actName;
			CommandLine.executeShellCommand(shellCommand, serial);
			Logger.trace(CommandLine.getLatestStdoutMessage());
			Logger.trace(CommandLine.getLatestStdoutMessage());
			if(deposit != null){ deposit.addEvent(event); }
		}break;
		case EventFactory.iREINSTALL:{
			String packageName = (String) event.getAttribute(EventFactory.pkgName);
			String path = (String)event.getAttribute(EventFactory.pkgPath);
			if(noReinstall){
				CommandLine.executeADBCommand("shell pm clear "+packageName, serial);
				Logger.trace(CommandLine.getLatestStdoutMessage());
				Logger.trace(CommandLine.getLatestStdoutMessage());
				CommandLine.executeADBCommand("shell am force-stop "+packageName, serial);
				Logger.trace(CommandLine.getLatestStdoutMessage());
				Logger.trace(CommandLine.getLatestStdoutMessage());
//				CommandLine.executeADBCommand(" input keyevent "+KeyEvent.KEYCODE_HO1ME , serial);
			}else{
				//clear the data first 
				CommandLine.executeADBCommand("shell pm clear "+packageName, serial);
				Logger.trace(CommandLine.getLatestStdoutMessage());
				Logger.trace(CommandLine.getLatestStdoutMessage());
				CommandLine.executeADBCommand("uninstall "+packageName, serial);
				Logger.trace(CommandLine.getLatestStdoutMessage());
				Logger.trace(CommandLine.getLatestStdoutMessage());
				String installCommand = "install "+ path;
				CommandLine.executeADBCommand(installCommand, serial);
				Logger.trace(CommandLine.getLatestStdoutMessage());
				Logger.trace(CommandLine.getLatestStdoutMessage());
			}
			if(deposit != null){ deposit.hasReinstalled(); }
		}break;
		case EventFactory.iPRESS:{
			String keycode = (String)event.getAttribute(EventFactory.keyCode);
			String inputCommand = "input keyevent " + keycode;
			CommandLine.executeShellCommand(inputCommand, serial);
			Logger.trace(CommandLine.getLatestStdoutMessage());
			Logger.trace(CommandLine.getLatestStdoutMessage());
			if(deposit != null){ deposit.addEvent(event); }
		}break;
		case EventFactory.iONCLICK:{
			String x = event.getAttribute(EventFactory.xCoordinate).toString();
			String y = event.getAttribute(EventFactory.yCoordinate).toString();
			int iX = Integer.parseInt(x);
			int iY = Integer.parseInt(y);
			if(corrdinatesRatio != null){
				iX = (int) (corrdinatesRatio[0] * iX);
				iY = (int) (corrdinatesRatio[0] * iY);
			}
			String inputCommand = "input tap " + iX + " " + iY;
			CommandLine.executeShellCommand(inputCommand, serial);
			Logger.trace(CommandLine.getLatestStdoutMessage());
			Logger.trace(CommandLine.getLatestStdoutMessage());
			if(deposit != null){  deposit.addEvent(event); }
		}break;
		case EventFactory.iUNDEFINED:
		default: throw new IllegalArgumentException();
		}

		if(sleep){
			try { Thread.sleep(EventFactory.getNeededSleepDuration(type));
			} catch (InterruptedException e) { }
		}
		Logger.trace();
	}

	public void setRatio(double[] corrdinatesRatio){
		this.corrdinatesRatio = Arrays.copyOf(corrdinatesRatio, 2);
	}
	
	public String getSerial() {
		return serial;
	}
	
	public boolean isRecordingEvent() {
		return recordEvent;
	}

	public void enableRecordingEvent(boolean recordEvent) {
		this.recordEvent = recordEvent;
	}	
	public void setEventDeposit(EventDeposit deposit){
		this.deposit = deposit;
	}
}

