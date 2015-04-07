package PlabAmodule;

import ignored.DeviceManager;
import ignored.DeviceManager.DeviceBundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import javax.swing.tree.DefaultMutableTreeNode;

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
 * @author zhenxu
 */
public class DetailOrientedProcedure implements Runnable{
	
	int levelOfChecking = 0; 
	
	UIModel model;
	UISymbolicPair currentState;
	String virtualDevice_serial = null, realDevice_serial = null;
	
	boolean manualControl = true;
	
	Executer executor_jdbDevice,  executor_viewDevice;
	ViewDeviceInfo viewInfo;
	StaticApp app;
	
	SymbolicAnalysis ex = new SymbolicAnalysis(app);
	Map<String, List<WrappedSummary>> methodSigToSummaries = new HashMap<String,List<WrappedSummary>>();
	
	BreakPointReader lineReader = new BreakPointReader();
	LogcatReader logcatReader = new LogcatReader(realDevice_serial,"adb","System.out");//TODO
	LinesSummaryMatcher matcher = new BasicMatcher();
	
	Stack<Event> stack = new Stack<Event>();
	List<EventSummaryPair> queue = new ArrayList<EventSummaryPair>();	
	
	EventDeposit deposit = new EventDeposit();
	InformationCollector collector_viewDevice;
	InformationCollector collector_jdbDevice;
	String mainAct, pkgName;
	
	Event closeKeyboardEvent = EventFactory.createCloseKeyboardEvent();
	
	public DetailOrientedProcedure(StaticApp app){
		this.app = app;
		model = new UIModel();
	}
	
	void setup(){
		ex.blackListOn = true;
		ex.debug = false;
		ex.init();
		
		mainAct = app.getMainActivity().getJavaName();
		pkgName = app.getPackageName();	
		stack.push( EventFactory.createLaunchEvent(
				GraphicalLayout.Launcher, pkgName, mainAct)); 
		currentState = new UISymbolicPair(GraphicalLayout.Launcher);
		model.defineRootState(currentState);
		setupDevice();
		
		collector_viewDevice = new InformationCollector(virtualDevice_serial);
		collector_jdbDevice = new InformationCollector(realDevice_serial);
	}
	
	
	/**
	 * Define:
	 * Cumulative Symbolic States -> CSS
	 * Layout CSS pair -> LSP
	 * Event PathSummary Pair - > EPP
	 * 
	 * Path Candidates:
	 * 1.	Known LSP for EPP
	 * 		If a LSP satisfies EPP and layout in LSP is the same as source
	 * 		of EPP, construct a sequence to LSP
	 * 
	 * 2.
	 * 		
	 * 
	 * A sequence from current layout-symbolic pair to targeted 
	 * 		layout-symbolic pair. 
	 * 2.	find a shortest path 
	 */
	@Override
	public void run(){
		setup();
		Major: while(true){
			if( !stack.isEmpty() ){//there are more new events to be executed -- expansion
				Event currentEvent = stack.pop();
				GraphicalLayout targetLayout = currentEvent.getSource();
				//reposition to the dest layout if the current one is not
				if(!targetLayout.equals(currentState.getLayout())  && !repositionToLayout(targetLayout) ){
					//somehow the event can no longer be triggered due to the failure
					//of reaching the dest layout.
					continue Major; 
				}
				
				//assume at this point, cumulative symbolic states are correct. 
				EventResultBundle executionResult = executeEvent(currentEvent);
				if(executionResult.hasCrashed){
					onApplicationError();
					continue Major;
				}
				
				
				currentEvent.setDest(executionResult.resultedGUI);
				EventSummaryPair esPair = new EventSummaryPair(executionResult);
				UISymbolicPair nextState = buildNextState(this.currentState, executionResult);
				
				if(executionResult.scope == EventResultBundle.SCOPE_WITHIN){
					List<Event> extra = model.createMoreEvents(executionResult.resultedGUI);
					if(extra != null){
						for(Event e : extra){
							this.stack.push(e);
						}
					}
				}
				
				model.update(this.currentState, esPair, nextState);
				this.currentState = nextState;
			}else{//no more new event
				EventSummaryPair esPair = queue.remove(0);
				if(esPair.getMajorBranch().isExecuted){
					//TODO either change the priority or ignored
					//may want to update the uiModel
					continue Major;
				}
								
				List<EventSummaryPair> path_fromCurrent = model.findConcretePathForEvent(esPair, currentState);
				if(path_fromCurrent != null && repositionSteps(path_fromCurrent)){ // cannot find suitable sequence
					EventResultBundle result = executeEvent(esPair.getEvent());
					if(result.getPrimaryBranch().equals(esPair.getMajorBranch())){
						//TODO successful 
						UISymbolicPair next = this.currentState.clone();
						next.overlaySymbolic(result.getPrimaryBranch().symbolicStates);
						next.setGraphicalLayout(result.resultedGUI);
						currentState = next;
						continue Major;
					}
				}
				esPair.increaseTryCount();
				
				List<List<EventSummaryPair>> path_solved = model.solveForEvent(esPair);
				int max = Math.min(path_solved.size(), 5);
				for(int i =0; i<max;i++){
					this.reinstallApplication();
					if(repositionSteps(path_fromCurrent)){
						EventResultBundle result = executeEvent(esPair.getEvent());
						if(result.getPrimaryBranch().equals(esPair.getMajorBranch())){
							//TODO successful 
							UISymbolicPair next = this.currentState.clone();
							next.overlaySymbolic(result.getPrimaryBranch().symbolicStates);
							next.setGraphicalLayout(result.resultedGUI);
							currentState = next;
							continue Major;
						}
					}
					esPair.increaseTryCount();
				}
			}
		}
	}

	
	/**
	 * Elements of the same index in
	 * 1. List<String> methodRoots,
	 * 2. List<List<PathSummary>> mappedSummaryCandidates,
	 * 3. List<PathSummary> mappedSummaries,
	 * are corresponding to each other.
	 * 
	 * methodRoot -> potential path summary candidates -> mapped path summary
	 * 
	 * 
	 * @param currentEvent
	 * @return
	 */
	public EventResultBundle executeEvent(Event currentEvent){
		EventResultBundle result = new EventResultBundle(currentEvent);
		//TODO modify the procedure so that error flag can be properly setup.
		
		executor_viewDevice.applyEvent(currentEvent);
		WindowOverview winOverview = collector_viewDevice.getWindowOverview();
		boolean inputMethodVisible = winOverview.isKeyboardVisible();
		if(inputMethodVisible){
			executor_jdbDevice.applyEvent(closeKeyboardEvent);
		}
		
		WindowInformation focusedInfo = winOverview.getFocusedWindow();
		GraphicalLayout resultedGUI = null;
		String actName = focusedInfo.actName;
		switch(checkWindowScope(focusedInfo)){
		case EventResultBundle.SCOPE_WITHIN:{
			resultedGUI = new GraphicalLayout(actName, viewInfo.loadWindowData());
		}break;
		case EventResultBundle.SCOPE_LAUNCHER:{
			resultedGUI = GraphicalLayout.Launcher;
		}break;
		case EventResultBundle.SCOPE_OUT:
		default: resultedGUI = new GraphicalLayout(actName, null);
		}
		
		//Read logcat for method call information
		List<String> feedBack = logcatReader.readLogcatFeedBack();
		List<DefaultMutableTreeNode> methodIOTrees = logcatReader.buildMethodCallTree(feedBack);
		List<String> methodRoots = logcatReader.getMethodRoots(methodIOTrees);
		int majorBranchIndex = logcatReader.findMajorBranch(methodIOTrees);
		
		//build pathsummaries for each method root
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
		//List<PathSummary> majorSummaries = methodRootIndex_Summaries_map.get(majorBranchIndex);
		
		//Read hit lines
		lineReader.setup(app, methodRoots);
		executor_jdbDevice.applyEvent(currentEvent);
		if(inputMethodVisible){
			executor_jdbDevice.applyEvent(closeKeyboardEvent);
		}
		List<List<String>> methodRootIndex_hitline_pair = lineReader.readExecLog(methodRoots);
		List<WrappedSummary> mappedSummaries = new ArrayList<WrappedSummary>();
		//map each method root (path fragment) with a path summary
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
		
		result.resultedGUI = resultedGUI;
		result.methodRoots = methodRoots;
		result.mappedSummaries = mappedSummaries;
		result.majorBranchIndex = majorBranchIndex;
		result.feedBack = feedBack;
		result.methodIOTrees = methodIOTrees;
		
		return result;
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
		
		executor_jdbDevice = new Executer(realDevice_serial);
		executor_viewDevice = new Executer(virtualDevice_serial);
	}
	
	void onApplicationError(){
		//TODO
	}
	
	/**
	 * build the new UI symbolic pair. 
	 * @param original
	 * @param executionResult
	 * @return
	 */
	private UISymbolicPair buildNextState(UISymbolicPair original, EventResultBundle executionResult){
		UISymbolicPair next = new UISymbolicPair(executionResult.resultedGUI);
		//TODO improve -- not sure if the activity cycle matters yet.
		//also the activity stack?
		next.overlaySymbolic(original.getCumulativeSymbolicStates());
		next.overlaySymbolic(executionResult.getCombinedSymbolicStates());
		return next;
	}
	
	/**
	 * The sequence is expected to be not null and non-empty
	 * The first element of the sequence shall have the same layout 
	 * as the actual one.
	 * 
	 * For each event,
	 * a. do the event
	 * b. get information
	 * c. check if the layout is the same as expected
	 * 	  if not first update UIModel and then return false
	 * d. build next UI symbolic pair. 
	 * 
	 * @param sequence
	 * @return
	 */
	private boolean repositionSteps(List<EventSummaryPair> sequence){
		boolean resultFlag = true;
		for(EventSummaryPair esPair : sequence){
			Event toExecute = esPair.getEvent();
			EventResultBundle executionResult = this.executeEvent(toExecute);
			
			if(executionResult.hasCrashed){
				//TODO maybe crashing is desired
				onApplicationError();
				return false;
			}
			
			//check if the layout is correct
			GraphicalLayout dest = executionResult.resultedGUI;
			GraphicalLayout expected = toExecute.getDest();
			
			if(dest.equals(expected)){ 
				// not sure if the path should be also the same
				// which seems to be unnecessary for this part.
				// As long as the layout is reached, it should be fine
				
				//TODO could be improved
			}else{ //what if a known layout in the model?				
				if(executionResult.scope == EventResultBundle.SCOPE_WITHIN){
					List<Event> eList = this.model.createMoreEvents(executionResult.resultedGUI);
					if(eList != null){
						for(Event e : eList){
							this.stack.add(0, e);
						}
					}
				}
				resultFlag = false;
			}
			
			UISymbolicPair nextState = buildNextState(this.currentState, executionResult);
			EventSummaryPair newESPair = new EventSummaryPair(esPair.getEvent().clone(),
					executionResult.mappedSummaries, executionResult.majorBranchIndex );
		
			this.model.update(currentState, newESPair, nextState);
			this.currentState = nextState;
			if(resultFlag == false) return false;
		}
		return true;
	}
	
	void reinstallApplication(){
		Event reinstall = EventFactory.createReinstallEvent(this.pkgName);
		executor_jdbDevice.applyEvent(reinstall);
		executor_viewDevice.applyEvent(reinstall);
		this.currentState = this.model.getRoot();
	}
	
	/**
	 * Reposition to the target layout.
	 * This is for a new event,
	 * Attempt 1. Path from the current UI symbolic pair (breath first search in UI model)
	 * Attempt 2. Path from the very beginning (Re-install the app)
	 * Attempt 3. Path in event deposit (Re-install the app)
	 * 
	 * Attempt 1 and 2 checks the layout and build cumulative symbolic state every time
	 * an event is executed.
	 * Attempt 3 only checks the layout at the end. 
	 * 
	 * @param targetLayout
	 * @return
	 */
	boolean repositionToLayout(GraphicalLayout targetLayout){
		{//try to reposition from current screen-sym to dest layout
			List<EventSummaryPair> sequence = this.model.findPathToLayout(this.currentState, targetLayout);
			if(sequence != null && sequence.isEmpty() == false){
				boolean resultFlag = repositionSteps(sequence);
				if(resultFlag) return true;
			}
		}
		
		{//try to reach this layout after reinstall
			List<EventSummaryPair> sequence_restart = this.model.findPathToLayout(this.model.getRoot(), targetLayout);
			if(sequence_restart != null && sequence_restart.isEmpty() == false){
				reinstallApplication();
				boolean resultFlag = repositionSteps(sequence_restart);
				if(resultFlag) return true;
				return true;
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
				this.executor_viewDevice.enableRecordingEvent(false);
				Event toExecute = null;
				for(InternalPair ip : ipList){
					toExecute = ip.e;
					EventSummaryPair esPair = ip.esp;
					this.executor_jdbDevice.applyEvent(toExecute);
					this.executor_viewDevice.applyEvent(toExecute);
					if(esPair != null && esPair.getDest() != null){
						lastUISymState = esPair.getDest();
					}
//					if(levelOfChecking == 0){ }else{  }
				}
				
				WindowOverview winOverview = collector_viewDevice.getWindowOverview();
				if(winOverview.isKeyboardVisible()){
					Event back = EventFactory.createCloseKeyboardEvent();
					this.executor_jdbDevice.applyEvent(back);
					this.executor_viewDevice.applyEvent(back);
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
				this.executor_viewDevice.enableRecordingEvent(true);
				if(result) return true;
			}
		}
		return false;
	}
	
	
	private int checkWindowScope(WindowInformation focusedInfo){
		String pkgName = focusedInfo.pkgName;
		if(pkgName.toLowerCase().contains("launcher")) return EventResultBundle.SCOPE_LAUNCHER;
		if(pkgName.equals(this.app.getPackageName())) return EventResultBundle.SCOPE_WITHIN;
		else return EventResultBundle.SCOPE_OUT;
	}
}
