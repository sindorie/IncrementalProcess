package ignored;

import ignored.DeviceManager.DeviceBundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.tree.DefaultMutableTreeNode;

import PlanBModule.AbstractManager;
import analysis.Expression;
import staticFamily.StaticApp;
import components.BasicMatcher;
import components.Event;
import components.EventDeposit;
import components.EventDeposit.InternalPair;
import components.system.InformationCollector;
import components.system.LogcatReader;
import components.system.WindowInformation;
import components.system.WindowOverview;
import components.EventFactory;
import components.EventResultBundle;
import components.EventSummaryPair;
import components.Executer;
import components.GraphicalLayout;
import components.LayoutNode;
import components.BreakPointReader;
import components.LinesSummaryMatcher;
import components.UISymbolicPair;
import components.ViewDeviceInfo;
import components.WrappedSummary;
import concolic.Execution;


/**
 * This implementation retrieve jdb information whenever an event take places.
 * This is due to the expectation of inaccurate symbolic states.
 * 
 * 
 * current implementation limitation:
 * Click back for the closing keyboard should not have side effect
 * 
 * 
 * @author zhenxu
 */
public class SimplifiedApproach implements Runnable{
	
	int levelOfChecking = 0; 
	
	PlanB_UIModel model;
	String virtualDevice_serial = null, realDevice_serial = null;
	
	boolean manualControl = true;
	
	Executer jdbDeviceExecuter,  viewDeviceExecuter;
	ViewDeviceInfo viewInfo, viewInfo_path;
	StaticApp app;
	
	SymbolicAnalysis ex = new SymbolicAnalysis(app);
	Map<String, List<WrappedSummary>> methodSigToSummaries = new HashMap<String,List<WrappedSummary>>();
	
	BreakPointReader bpReader = new BreakPointReader();
	LogcatReader logcatReader = new LogcatReader(realDevice_serial,"adb","System.out");//TODO
	LinesSummaryMatcher matcher = new BasicMatcher();
	
	Stack<Event> stack = new Stack<Event>();
	
	EventDeposit deposit = new EventDeposit();
	InformationCollector collector_viewDevice;
	InformationCollector collector_jdbDevice;
	String mainAct, pkgName;
	
	Event closeKeyboardEvent = EventFactory.createCloseKeyboardEvent();
	AbstractManager execution = null;//TODO
	
	GraphicalLayout currentLayout = null;
	
	public SimplifiedApproach(StaticApp app){
		this.app = app;
		model = new PlanB_UIModel();
	}
	
	void setup(){
		ex.blackListOn = true;
		ex.debug = false;
		ex.init();
		
		mainAct = app.getMainActivity().getJavaName();
		pkgName = app.getPackageName();	
		stack.push( EventFactory.createLaunchEvent(
				GraphicalLayout.Launcher, pkgName, mainAct)); 
		model.defineRootLayout(GraphicalLayout.Launcher);
		currentLayout = GraphicalLayout.Launcher;
		setupDevice();
		
		collector_viewDevice = new InformationCollector(virtualDevice_serial);
		collector_jdbDevice = new InformationCollector(realDevice_serial);
	}
	
	@Override
	public void run(){
		setup();
		
		while(true){
			if(execution.isInExplorationMode()){
				//TODO clear jdb information
				
				Event newEvent = execution.getNextExplorationEvent();
				if(newEvent.getSource() == null){
					//TODO
					continue;
				}
				if(!currentLayout.equals(newEvent.getSource())
					&& !repositionToLayout(newEvent.getSource()) ){
					continue;
				}
				
				//get the layout information
				this.viewDeviceExecuter.applyEvent(newEvent);
				WindowOverview winOverview = collector_viewDevice.getWindowOverview();
				WindowInformation focusedWin = winOverview.getFocusedWindow();
				int scope = focusedWin.isWithinApplciation(this.app);
				GraphicalLayout resultedLayout = null;
				boolean inputMethodVisible = winOverview.isKeyboardVisible();
				switch(scope){
				case WindowInformation.SCOPE_LAUNCHER:{
					resultedLayout = GraphicalLayout.Launcher;
				}break;
				case WindowInformation.SCOPE_WITHIN:{
					resultedLayout = new GraphicalLayout(focusedWin.actName, viewInfo.loadWindowData());
					if(inputMethodVisible){
						closeKeyboard();
					}
				}break;
				case WindowInformation.SCOPE_OUT:{
					resultedLayout = new GraphicalLayout(focusedWin.actName, null);
				}break;
				}
				
				//Read logcat for method call information
				List<String> feedBack = logcatReader.readLogcatFeedBack();
				List<DefaultMutableTreeNode> methodIOTrees = logcatReader.buildMethodCallTree(feedBack);
				List<String> methodRoots = logcatReader.getMethodRoots(methodIOTrees);
				int majorBranchIndex = logcatReader.findMajorBranch(methodIOTrees);
				
				List<List<WrappedSummary>> mappedSummaryCandidates = new ArrayList<List<WrappedSummary>>();
				int indexCount = 0;
				for(String methodSig: methodRoots){
					List<WrappedSummary> summaries = methodSigToSummaries.get(methodSig);
					if(summaries == null){
						ex.setTargetMethod(methodSig); 
						summaries = WrappedSummary.wrapSummaryList(ex.doFullSymbolic(false));
						methodSigToSummaries.put(methodSig, summaries);
					}
					mappedSummaryCandidates.add(summaries);
					indexCount += 1;
				}
				
				//get the path information 
				
				bpReader.setup(app, methodRoots);
				this.jdbDeviceExecuter.applyEvent(newEvent);
				if(inputMethodVisible){
					jdbDeviceExecuter.applyEvent(closeKeyboardEvent);
				}
				List<List<String>> methodRootIndex_hitline_pair = bpReader.readExecLog();
				List<WrappedSummary> mappedSummaries = new ArrayList<WrappedSummary>();
				for(int i = 0; i<indexCount ; i++){
					List<String> hitLines = methodRootIndex_hitline_pair.get(i);
					List<WrappedSummary> summaries = mappedSummaryCandidates.get(i);
					int index = matcher.matchSummary(hitLines, WrappedSummary.unwrapSummary(summaries));
					if(index <= 0){
						mappedSummaries.add(null);
					}else{
						mappedSummaries.add(summaries.get(index));
					}
				}
				
				EventSummaryPair esPair = new EventSummaryPair(newEvent, mappedSummaries, majorBranchIndex, methodRoots);
				List<Event> additionalEvents = this.model.update(esPair, resultedLayout);
				esPair.setConcreateExecuted();
				if(additionalEvents != null){
					this.execution.add(additionalEvents);
				}
			}else if(execution.isInExpansionMpde()){
				EventSummaryPair esPair = execution.getNextExpansionEvent();
				this.reinstallApplication();
				//only use the last effort
				List<EventSummaryPair> sequence = this.model.solveForEvent(esPair);
				if(sequence == null || sequence.isEmpty()) continue;
				
				reinstallApplication();
				int i = 0;
				WindowOverview winOverview  = null;
				for(;i<sequence.size()-1;i++){
					this.viewDeviceExecuter.applyEvent(sequence.get(i).getEvent());
					winOverview = collector_jdbDevice.getWindowOverview();
					//TODO needs to find a way to avoid closing keyboard 
					//when the next event is about enter text
					boolean inputVisible = winOverview.isKeyboardVisible();
					if(inputVisible){
						closeKeyboard();
					}
				}
				if(winOverview != null){
					winOverview.getFocusedWindow();
				}
				GraphicalLayout targetLayout = new GraphicalLayout(winOverview.getFocusedWindow().actName,
						viewInfo_path.loadWindowData());
				if(targetLayout.equals(esPair.getEvent().getSource()) == false){
					continue;
				}
				
				bpReader.setup(app, esPair.getMethodRoots());
				this.viewDeviceExecuter.applyEvent(esPair.getEvent());
				List<List<String>> methodRootIndex_hitline_pair = bpReader.readExecLog();
				//do linear comparison with the major branch.
				WrappedSummary majorBranch = esPair.getMajorBranch();
				boolean comparionResult = false;
				for(List<String> methodHits : methodRootIndex_hitline_pair){
					comparionResult |= matcher.compareBPRecords(
							methodHits, majorBranch.summaryReference.getExecutionLog());
					if(comparionResult) break;
				}
				
				if(comparionResult){
					WindowOverview overview = this.collector_jdbDevice.getWindowOverview();
					WindowInformation focusedWin = overview.getFocusedWindow();
					int scope = focusedWin.isWithinApplciation(this.app);
					GraphicalLayout resultGUI = null;
					switch(scope){
					case WindowInformation.SCOPE_LAUNCHER:{
						resultGUI = GraphicalLayout.Launcher;
					}break;
					case WindowInformation.SCOPE_WITHIN:{
						resultGUI = new GraphicalLayout(
								focusedWin.actName, this.viewInfo_path.loadWindowData());
					}break;
					case WindowInformation.SCOPE_OUT:{
						resultGUI = new GraphicalLayout(focusedWin.actName, null);
					}break;
					}
					this.model.update(esPair, resultGUI);
					esPair.setConcreateExecuted();
				}else{//fail
					
				}
			}else if(execution.isInReachTargetMode()){
				EventSummaryPair esPair = execution.getNextTargetSummary();
				
				
				
			}else if(execution.isFinished()) break;
		}
	}

	

	void closeKeyboard(){
		//TODO
	}
	

	void setupDevice(){
		DeviceManager dManager = new DeviceManager();
		while(true){
			int count = dManager.deviceList.size();
			if(count >= 2){
				//check any of them is virtual device
				DeviceBundle info1 = dManager.deviceList.get(0);
				DeviceBundle info2 = dManager.deviceList.get(1);
			
				boolean flag = false;
				String se1 = info1.rawDevice.getSerialNumber();
				String se2 = info2.rawDevice.getSerialNumber();
				if(se1.contains(":")){ //TODO might not work in future
					virtualDevice_serial = se1;
					realDevice_serial = se2;
					flag = true;
				}else if(se2.contains(":")){
					virtualDevice_serial = se2;
					realDevice_serial = se1;
					flag = true;
				}
				if(flag) break;
			}
			viewInfo = new ViewDeviceInfo(virtualDevice_serial);
			try { Thread.sleep(2000);
			} catch (InterruptedException e) {}
		}
		
		jdbDeviceExecuter = new Executer(realDevice_serial);
		viewDeviceExecuter = new Executer(virtualDevice_serial);
	}
	
	void onApplicationError(){
		//TODO
	}

	
	void reinstallApplication(){
		Event reinstall = EventFactory.createReinstallEvent(this.pkgName);
		jdbDeviceExecuter.applyEvent(reinstall);
		viewDeviceExecuter.applyEvent(reinstall);
	}
	

	boolean repositionToLayout(GraphicalLayout targetLayout){
		{//try to reposition from current screen-sym to dest layout
			List<EventSummaryPair> sequence = this.model.findPathToLayout(this.currentState, targetLayout);
			if(sequence != null && sequence.isEmpty() == false){
				
				
				
				
				
				
				if(resultFlag) return true;
			}
		}
		
		{//try to use the sequence from the event deposit
			List<InternalPair> ipList = this.deposit.findSequenceToLayout(targetLayout);
			if(ipList != null && ipList.isEmpty() == false){
				//re-install the application which result in empty symbolic 
				//assume that the effect of this sequence is exactly the same as before 
				//in terms of both layout and symbolic states. Therefore the checking 
				//other than the final one is ignored.
				UISymbolicPair lastUISymState = null;
				this.viewDeviceExecuter.enableRecordingEvent(false);
				Event toExecute = null;
				for(InternalPair ip : ipList){
					toExecute = ip.e;
					EventSummaryPair esPair = ip.esp;
					this.jdbDeviceExecuter.applyEvent(toExecute);
					this.viewDeviceExecuter.applyEvent(toExecute);
					if(esPair != null && esPair.getDest() != null){
						lastUISymState = esPair.getDest();
					}
//					if(levelOfChecking == 0){ }else{  }
				}
				
				WindowOverview winOverview = collector_viewDevice.getWindowOverview();
				if(winOverview.isKeyboardVisible()){
					Event back = EventFactory.createCloseKeyboardEvent();
					this.jdbDeviceExecuter.applyEvent(back);
					this.viewDeviceExecuter.applyEvent(back);
				}
				
				WindowInformation focusedInfo = winOverview.getFocusedWindow();
				String actName = focusedInfo.actName;
				GraphicalLayout layout = new GraphicalLayout(actName,this.viewInfo.loadWindowData());
				boolean result = false;
				if(toExecute.getDest().equals(layout)){
					this.currentState = lastUISymState;
					result = true;
				}else{
					//oh my god... assumption is broken 
					//TODO
				}
				this.viewDeviceExecuter.enableRecordingEvent(true);
				if(result) return true;
			}
		}
		return false;
	}

}
