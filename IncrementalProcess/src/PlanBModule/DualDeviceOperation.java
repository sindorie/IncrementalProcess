package PlanBModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.DefaultMutableTreeNode;

import android.view.KeyEvent;
import staticFamily.StaticApp;
import support.CommandLine;
import support.Logger;
import symbolic.SymbolicExecution;
import components.BasicMatcher;
import components.BreakPointReader;
import components.Event;
import components.EventDeposit;
import components.EventFactory;
import components.EventSummaryPair;
import components.Executer;
import components.GraphicalLayout;
import components.LinesSummaryMatcher;
import components.ViewDeviceInfo;
import components.WrappedSummary;
import components.system.Configuration;
import components.system.InformationCollector;
import components.system.LogcatReader;
import components.system.WindowInformation;
import components.system.WindowOverview;
import components.system.WindowPolicy;

/**
 * 
 * 
 * @author zhenxu
 *
 */
public class DualDeviceOperation extends AbstractExuectionOperation { 

	/* 
	 * Fields for information collection
	 */
	private InformationCollector collector_viewDevice, collector_jdbDevice;
	private ViewDeviceInfo viewInfoView, viewInfoJDB;
	private BreakPointReader bpReader;
	private LogcatReader logcatReader;
	
	/*
	 * Fields for data recording
	 */
	private EventDeposit deposit;
	private GraphicalLayout currentLayout;
	private EventSummaryPair lastExecutedEvent;
	private List<EventSummaryPair> newValidationEvent;
	private Map<String, List<WrappedSummary>> methodSigToSummaries;
	
	/*
	 * Miscellaneous fields
	 */
	protected String viewDeviceSerial, jdbDeviceSerial;		//serials of devices
	private Event closeKeyboardEvent; 						//predefined event
	private LinesSummaryMatcher matcher;					//matcher between line numbers
	private SymbolicExecution symbolicExecution;			//generate symbolic information of all methods
	private Executer jdbDeviceExecuter, viewDeviceExecuter;	//executers which do the events
	
	/**
	 * @param app -- the application under investigation
	 * @param model	-- the model which the operator works on
	 * @param viewDeviceSerial -- the serial of device which provides the view information
	 * @param jdbDeviceSerial -- the serial of devices which provides the path information
	 */
	public DualDeviceOperation(StaticApp app, UIModel model,
			String viewDeviceSerial, String jdbDeviceSerial) {
		super(app, model);
		this.viewDeviceSerial = viewDeviceSerial;
		this.jdbDeviceSerial = jdbDeviceSerial;
		String adbPath = Configuration.getValue(Configuration.attADB);
		
		deposit = new EventDeposit();
		bpReader = new BreakPointReader(jdbDeviceSerial);
		viewInfoJDB = new ViewDeviceInfo(jdbDeviceSerial);
		viewInfoView = new ViewDeviceInfo(viewDeviceSerial);
		collector_jdbDevice = new InformationCollector(jdbDeviceSerial);
		collector_viewDevice = new InformationCollector(viewDeviceSerial);
		methodSigToSummaries = new HashMap<String, List<WrappedSummary>>();
		logcatReader = new LogcatReader(viewDeviceSerial, adbPath, "System.out");
		
		viewDeviceExecuter = new Executer(viewDeviceSerial, deposit);
		jdbDeviceExecuter = new Executer(jdbDeviceSerial);
		symbolicExecution = new SymbolicExecution(app);
		symbolicExecution.debug = false;
		matcher = new BasicMatcher();
		
		closeKeyboardEvent = EventFactory.createCloseKeyboardEvent();
		currentLayout = null;
	}

	@Override
	public void onPreparation() {
		Logger.trace();
		this.jdbDeviceExecuter.applyEvent(EventFactory.CreatePressEvent(GraphicalLayout.Launcher,
				KeyEvent.KEYCODE_HOME ));
		this.viewDeviceExecuter.enableRecordingEvent(false);
		this.viewDeviceExecuter.applyEvent(EventFactory.CreatePressEvent(GraphicalLayout.Launcher,
				KeyEvent.KEYCODE_HOME ));
		this.viewDeviceExecuter.enableRecordingEvent(true);
		
		this.reinstallApplication();
		WindowPolicy viewDevicePolicy = this.collector_viewDevice.getWindowPolicy();
		WindowPolicy jdbDevicePolicy = this.collector_jdbDevice.getWindowPolicy();
		
		double[] ratio = new double[2];
		ratio[0] = jdbDevicePolicy.width/viewDevicePolicy.width;
		ratio[1] = jdbDevicePolicy.height/viewDevicePolicy.height;
		this.jdbDeviceExecuter.setRatio(ratio);
		
		logcatReader.clearLogcat();
	}

	@Override
	public void onExplorationProcess(Event newEvent) {
		Logger.trace("Event: "+newEvent);
		Logger.trace("Current: "+this.currentLayout);
		
		/*
		 * Check if the current layout is the desired one
		 * Reposition if necessary, which should take both 
		 * devices to the desired layout. 
		 */
		if(!this.currentLayout.equals(newEvent.getSource())){
			Logger.trace("Needs reposition from "+this.currentLayout+" to "+newEvent.getSource());
			if (!repositionToLayout(newEvent.getSource())) {
				//ignore the event upon failure
				return;
			}
		}
		
		logcatReader.clearLogcat();
		/*
		 * Logcat reading can be the indication of when the program 
		 * become stable, which can potentially replace the static 
		 * thread sleeping.
		 * 
		 * Note: There is a static sleep because of the intention
		 * of letting all logcat feedback ready. 
		 */
		this.viewDeviceExecuter.applyEvent(newEvent);
		
		/*
		 * There could be multiple method roots.
		 * Each root could be mapped to a list of summary. 
		 */
		Logger.trace("start reading logcat feedback");
		List<String> methodRoots = new ArrayList<String>();
		List<List<WrappedSummary>> mappedSummaryCandidates = new ArrayList<List<WrappedSummary>>();
		int majorBranchIndex = processLogcatFeedBack(methodRoots, mappedSummaryCandidates);
		Logger.debug("finish reading logcat feedback, methodRoots: "+methodRoots);
		Logger.debug("prmary mapped path index: " + majorBranchIndex);
		Logger.debug("Mapped summary Candidates: " + mappedSummaryCandidates);
		
		/*
		 * Check the Window information, including visible windows, keyboards
		 * Close the keyboard if necessary.  
		 * Retrieve the layout information the current visible window is in scope
		 */
		Logger.debug("Collects visible window information");
		WindowOverview winOverview = collector_viewDevice.getWindowOverview();
		WindowInformation focusedWin = winOverview.getFocusedWindow();
		int scope = focusedWin.isWithinApplciation(this.app);
		GraphicalLayout resultedLayout = null;
		boolean inputMethodVisible = winOverview.isKeyboardVisible();
		switch (scope) {
		case WindowInformation.SCOPE_LAUNCHER: {
			Logger.debug("window is launcher");
			resultedLayout = GraphicalLayout.Launcher;
		}break;
		case WindowInformation.SCOPE_WITHIN: {
			Logger.debug("window is within the application");
			resultedLayout = new GraphicalLayout(focusedWin.actName,
					viewInfoView.loadWindowData());
			if (inputMethodVisible) { closeKeyboard(); }
		}break;
		case WindowInformation.SCOPE_OUT: {
			Logger.debug("window is outside the application");
			resultedLayout = new GraphicalLayout(focusedWin.actName, null);
		}break;
		}

		
		/*
		 * Due to current limitation on implementation (jdb), the lines in 
		 * the process initialization can not be monitored. Skip the jdb 
		 * setup for events which starts at launcher. 
		 * 
		 * Create event summary pair without concrete execution
		 */
		
		/*
		 * get the path information if logcat has readings 
		 */
		List<WrappedSummary> mappedSummaries = null;
		if (majorBranchIndex >= 0 && !newEvent.getSource().equals(GraphicalLayout.Launcher)) {
			Logger.debug("Parimary mapped path: "+ mappedSummaryCandidates.get(majorBranchIndex));

			bpReader.setup(app, methodRoots);
			this.jdbDeviceExecuter.applyEvent(newEvent);
			if (inputMethodVisible) {
				jdbDeviceExecuter.applyEvent(closeKeyboardEvent);
			}
			
			/*
			 * There could be multiple method roots
			 * for each method root, there is a list of break point hits
			 * for each list, try to map it a summary, null if failure. 
			 * 
			 * Each event summary symbolically generated should have a cloned
			 * event as the destination is uncertain.
			 * 
			 */
			List<List<String>> logSequences = bpReader.readExecLog();
			for(List<String> hitList : logSequences){
				Logger.debug(hitList);
			}
			
			mappedSummaries = new ArrayList<WrappedSummary>();
			/*
			 * For each method root, there is a corresponding list of hit lines
			 * Such list can be mapped to a summary.
			 */
			for (int methodRoot_index = 0; methodRoot_index < methodRoots.size(); methodRoot_index++) {
				List<String> hitLines = logSequences.get(methodRoot_index);
				List<WrappedSummary> summary_candidatelist = mappedSummaryCandidates.get(methodRoot_index);
				int matchedIndex = matcher.matchSummary(hitLines, WrappedSummary.unwrapSummary(summary_candidatelist));
				
				if (matchedIndex < 0) { mappedSummaries.add(null); // mapping failure
				} else { mappedSummaries.add(summary_candidatelist.get(matchedIndex)); }
			}

			/*
			 * Only generate validation event for the other branches of the major summary 
			 * NOTE: TODO this is only a compromised solution
			 */
			WrappedSummary selectedMajorBranch = mappedSummaries.get(majorBranchIndex);
			List<EventSummaryPair> validationCandidates = new ArrayList<EventSummaryPair>();
			List<WrappedSummary> toValidateSummaries = mappedSummaryCandidates.get(majorBranchIndex);
			List<String> singular_mathodRoot = new ArrayList<String>();
			String mRoot = methodRoots.get(majorBranchIndex);
			singular_mathodRoot.add(mRoot);
			Logger.debug("MethodRoot of major branch: "+mRoot);
			Logger.debug("selectedMajorBranch: "+selectedMajorBranch);

			for(WrappedSummary wSum : toValidateSummaries){
				if(wSum == selectedMajorBranch) continue; //which indicates it was conrete executed
				Logger.debug("Summary to validate: "+wSum);
				List<WrappedSummary> singluar = new ArrayList<WrappedSummary>();
				singluar.add(wSum);
				EventSummaryPair toValidate = new EventSummaryPair(
						newEvent.clone(), singluar, 0, singular_mathodRoot);	
				validationCandidates.add(toValidate);
			}
			this.newValidationEvent = validationCandidates;
			if(validationCandidates != null){
				for(EventSummaryPair esPair : validationCandidates){
					Logger.debug("##Candidates: "+esPair.getEvent()+"; sum list: "+esPair.getMajorBranch().methodSignature+"; "+esPair.getMajorBranch().constraints);
				}
			}
		} else {// no method in apk is called
			Logger.debug("No mapped path summary");
			this.jdbDeviceExecuter.applyEvent(newEvent);
			if (inputMethodVisible) {
				jdbDeviceExecuter.applyEvent(closeKeyboardEvent);
			}
		}
		
		if(newEvent.getSource().equals(GraphicalLayout.Launcher)){
			Logger.debug("Event source is launcher");
			//TODO choose the summary which has the most symbolic result
			//find onCreate, onStart, onResume -- does not care about onMenuCreation etc. 
			List<WrappedSummary> result = new ArrayList<WrappedSummary>();
			for(String method : methodRoots){
				WrappedSummary mapped = findBestCandidate(mappedSummaryCandidates, methodRoots, method);
				result.add(mapped);
			}
			
			if(result.size() <= 0){
				majorBranchIndex = -1;
			}else{
				majorBranchIndex = 0;
				mappedSummaries = result; 
			}
		}
		
		//TODO under constructing : there seems to be some problem in terms of selecting
		// summary and creating path-summary
		
		Logger.trace("New ESPair: "+newEvent+", "+majorBranchIndex+", "+mappedSummaries);
		EventSummaryPair esPair = new EventSummaryPair(newEvent, mappedSummaries, majorBranchIndex, methodRoots);
		/**
		 * The destination layout of event is set the model as it might find the exisiting one.
		 */
		model.update(esPair, resultedLayout);
		esPair.setConcreateExecuted();
		
		lastExecutedEvent = esPair;
		this.currentLayout = resultedLayout;
	}	

	@Override
	public void onExpansionProcess(EventSummaryPair toValidate) {
		Logger.debug(toValidate.toString());
		List<Event> sequence = this.model.solveForEvent(toValidate);
		Logger.debug(sequence==null? "null":sequence.toString());
		if (sequence == null || sequence.isEmpty()) return;
		
		reinstallApplication();
		int i = 0;
		WindowOverview winOverview = null;
		for (; i < sequence.size() - 1; i++) {
			this.viewDeviceExecuter.applyEvent(sequence.get(i),false);
			this.jdbDeviceExecuter.applyEvent(sequence.get(i));
			winOverview = collector_jdbDevice.getWindowOverview();
			// TODO needs to find a way to avoid closing keyboard
			// when the next event is about enter text
			boolean inputVisible = winOverview.isKeyboardVisible();
			if (inputVisible) {
				closeKeyboard();
			}
		}
//		if (winOverview != null) {
//			winOverview.getFocusedWindow();
//		}
		GraphicalLayout targetLayout = new GraphicalLayout(
				winOverview.getFocusedWindow().actName,
				viewInfoJDB.loadWindowData());
		if (targetLayout.equals(toValidate.getEvent().getSource()) == false) {
			Logger.debug("unexpected layout");
			return;
		}

		Logger.debug("ready to try event");
		/**
		 * Due to the incompleteness of symbolically generated path summary,
		 * the jdb should be set up in accordance of the logcat feedback
		 * The resulted feedback 
		 */
//		this.logcatReader.clearLogcat();
//		this.viewDeviceExecuter.applyEvent(toValidate.getEvent());
//		List<String> feedBack = this.logcatReader.readLogcatFeedBack();
//		List<DefaultMutableTreeNode> methodIOTree = logcatReader.buildMethodCallTree(feedBack);
//		List<String> methodRoots = logcatReader.getMethodRoots(methodIOTree);
//		
//		boolean comparionResult = false;
//		bpReader.setup(app,methodRoots);
//		this.jdbDeviceExecuter.applyEvent(toValidate.getEvent());
//		List<List<String>> methodRootIndex_hitline_pair = bpReader.readExecLog();
		
		/*
		 * Successful Scenario:
		 * 1. Perfect match for each path summary. Each execution log can be mapped to a summary
		 * and vice versa. 
		 * 2. Semi-perfect match. Each path summary corresponds to a execution log, but not vice 
		 * versa for all execution logs.
		 * 3. Imperfect match. An execution log contains the lines from a path summary. 
		 * Not sure this should be considered correct. (Method Call back)
		 */
		
		
		
		//Only the major branch is taken into consideration. .
		
		
		
		//TODO this is only a compromised implementation.


		this.jdbDeviceExecuter.applyEvent(toValidate.getEvent(), false);
		this.viewDeviceExecuter.applyEvent(toValidate.getEvent());
		bpReader.setup(app, toValidate.getMethodRoots());

		List<List<String>> methodRootIndex_hitline_pair = bpReader.readExecLog();
		// do linear comparison with the major branch.
		WrappedSummary majorBranch = toValidate.getMajorBranch();
		boolean comparionResult = false;
		for (List<String> methodHits : methodRootIndex_hitline_pair) {
			comparionResult |= matcher.compareBPRecords(methodHits,
					majorBranch.summaryReference.getExecutionLog());
			if (comparionResult)
				break;
		}

		if (comparionResult) {
			Logger.debug("found path summary");
			WindowOverview overview = this.collector_jdbDevice.getWindowOverview();
			WindowInformation focusedWin = overview.getFocusedWindow();
			
			int tryCount = 0;
			while(focusedWin == null){
				focusedWin = collector_jdbDevice.getWindowOverview().getFocusedWindow();
				
				try { Thread.sleep(200);
				} catch (InterruptedException e) { }
				
				tryCount += 1;
				if(tryCount >3){
					System.out.println("Cannot get focused window");
					CommandLine.requestInput();
				}
			}
			
			int scope = focusedWin.isWithinApplciation(this.app);
			GraphicalLayout resultGUI = null;
			switch (scope) {
			case WindowInformation.SCOPE_LAUNCHER: {
				resultGUI = GraphicalLayout.Launcher;
			}
				break;
			case WindowInformation.SCOPE_WITHIN: {
				resultGUI = new GraphicalLayout(focusedWin.actName,
						this.viewInfoJDB.loadWindowData());
			}
				break;
			case WindowInformation.SCOPE_OUT: {
				resultGUI = new GraphicalLayout(focusedWin.actName, null);
			}
				break;
			}
			this.model.update(toValidate, resultGUI);
			toValidate.setConcreateExecuted();
			
			this.currentLayout = resultGUI;
		} else {// fail
			Logger.debug("fail to map path summary");
		}
		
		lastExecutedEvent = toValidate;
	}

	@Override
	public void onFinish() {
		// Nothing to do at this point
	}

	@Override
	public List<EventSummaryPair> getAdditionalValidationEvents() {
		Logger.trace();
		List<EventSummaryPair> result = this.newValidationEvent;
		this.newValidationEvent = null;
		return result;
	}

	@Override
	public EventSummaryPair getLastExecutedEvent() {
		return lastExecutedEvent;
	}
	
	
	/**
	 * Try to select the best candidate out of the summary candidates for a method
	 * @param mappedSummaryCandidates -- a list of candidates for a list of method roots
	 * @param methodRoots -- a list of method roots where each of them corresponds to some candidates 
	 * @param sig -- the method signature (partial function name)
	 * @return
	 */
	private WrappedSummary findBestCandidate(List<List<WrappedSummary>> mappedSummaryCandidates,List<String> methodRoots, String sig){
		WrappedSummary result = null;
		for(int i = 0; i<methodRoots.size();i++){
			String methodSig = methodRoots.get(i);
			if(methodSig.contains(sig)){
				List<WrappedSummary> wSumList = mappedSummaryCandidates.get(i);
				Logger.debug("Found for "+sig+": "+wSumList);
				if(wSumList != null){
					for(WrappedSummary wSum : wSumList){
						if(result == null){ result = wSum;
						}else{
							if(result.symbolicStates.size() < wSum.symbolicStates.size()){
								result = wSum;
							}
						}
					}
				}
				break;
			}
		}
		return result;
	}
	
	/**
	 * @param methodRoots -- a buffer where the resulted method roots will be stored
	 * @param mappedSummaryCandidates -- the mapped summary resides at
	 * @return
	 */
	private int processLogcatFeedBack(List<String> methodRoots, List<List<WrappedSummary>> mappedSummaryCandidates){
		List<String> feedBack = logcatReader.readLogcatFeedBack();
		int majorBranchIndex = -1;
		if (feedBack != null && feedBack.isEmpty() == false) {
			List<DefaultMutableTreeNode> methodIOTrees = logcatReader.buildMethodCallTree(feedBack);
			methodRoots.addAll(logcatReader.getMethodRoots(methodIOTrees));
			for (String methodSig : methodRoots) {
				Logger.trace("methodSig: "+methodSig);
				List<WrappedSummary> summaries = methodSigToSummaries.get(methodSig);
				if (summaries == null) {
					summaries = WrappedSummary
							.wrapSummaryList(symbolicExecution.doFullSymbolic(methodSig));
					methodSigToSummaries.put(methodSig, summaries);
				}
				if(summaries == null){
					summaries = new ArrayList<WrappedSummary>();
				}
				mappedSummaryCandidates.add(summaries);
			}
			majorBranchIndex = logcatReader.findMajorBranch(methodIOTrees, mappedSummaryCandidates);
		}
		return majorBranchIndex;
	}
	
	private boolean repositionToLayout(GraphicalLayout targetLayout) {
		Logger.trace("TargetLayout: "+targetLayout+"  Current: "+this.currentLayout);
		{
			/*
			 * Reposition from the current layout. Find a sequence
			 * from model which does not actually try to solve anything
			 * deterministically. 
			 */
			List<EventSummaryPair> sequence = this.model.findSequence(this.currentLayout, targetLayout);
			if (sequence != null && sequence.isEmpty() == false) {
				Logger.trace("From model: "+sequence);
				WindowOverview winOverview = null;
				for (EventSummaryPair esPair : sequence) {
					this.viewDeviceExecuter.applyEvent(esPair.getEvent(),false);
					this.jdbDeviceExecuter.applyEvent(esPair.getEvent());
					winOverview = this.collector_viewDevice.getWindowOverview();
					if (winOverview.isKeyboardVisible()) {
						viewDeviceExecuter.applyEvent(this.closeKeyboardEvent);
					}
				}
				GraphicalLayout layout = new GraphicalLayout(
						winOverview.getFocusedWindow().actName,
						this.viewInfoView.loadWindowData());
				if (layout.equals(targetLayout)) {
					return true;
				}
				this.currentLayout = layout;
			}else{
				Logger.trace("No valid sequence");
			}
		}

		/*
		 * Find a sequence to the target layout in the event deposit which 
		 * records all events (other than closing keyboard) previously executed.
		 * 
		 * The sequence returned should be the shortest possible one.
		 */
		Logger.trace("Reposition 2nd phase");
		this.reinstallApplication();
		{// try to use the sequence from the event deposit
			List<Event> sequence = this.deposit.findSequenceToLayout(targetLayout);
			Logger.trace("From deposit: "+sequence);
			if (sequence != null && sequence.isEmpty() == false) {
				WindowOverview winOverview = null;
				for (Event event : sequence) {
					this.viewDeviceExecuter.applyEvent(event,false);
					this.jdbDeviceExecuter.applyEvent(event);
					winOverview = this.collector_viewDevice.getWindowOverview();
					if (winOverview.isKeyboardVisible()) {
						viewDeviceExecuter.applyEvent(this.closeKeyboardEvent);
					}
				}
				GraphicalLayout layout = new GraphicalLayout(
						winOverview.getFocusedWindow().actName,
						this.viewInfoView.loadWindowData());
				if (layout.equals(targetLayout)) {
					return true;
				}
				this.currentLayout = layout;
			}
		}
		Logger.trace("Reposition failure");
		return false;
	}

	/**
	 * Close the keyboard by sending the closeing keyboard event
	 * which now is press back event
	 */
	private void closeKeyboard() {
		Logger.trace();
		this.viewDeviceExecuter.applyEvent(closeKeyboardEvent, false);
		this.jdbDeviceExecuter.applyEvent(closeKeyboardEvent);
	}

	/**
	 * Reinstall the application. 
	 * Note: the Smali and Soot version of application have been the same. 
	 */
	private void reinstallApplication() {
		Logger.trace("starts");
		String pkgName = this.app.getPackageName();
		this.viewDeviceExecuter.applyEvent(EventFactory.createReinstallEvent(
				pkgName, app.getInstrumentedApkPath()), false);
		this.jdbDeviceExecuter.applyEvent(EventFactory.createReinstallEvent(
				pkgName, app.getInstrumentedApkPath()));
		this.currentLayout = GraphicalLayout.Launcher;
	}


}
