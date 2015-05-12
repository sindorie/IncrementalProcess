package PlanBModule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;

import android.view.KeyEvent;
import staticFamily.StaticApp;
import support.CommandLine;
import support.Logger;
import support.TreeUtility;
import symbolic.Expression;
import symbolic.PathSummary;
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
public class DualDeviceOperation extends AbstractOperation { 

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
	private EventDeposit eventDeposit;
	private GraphicalLayout currentLayout;
	private EventSummaryPair lastExecutedEvent;
	private EventSummaryDeposit eventSummaryDeposit;
	private List<EventSummaryPair> newValidationEvent;
	private Map<String, List<WrappedSummary>> methodSigToSummaries;
	private Set<String> latestLineHit;
	
	/*
	 * Operation fields
	 */
	protected String viewDeviceSerial, jdbDeviceSerial;		//serials of devices
	private Executer jdbDeviceExecuter, viewDeviceExecuter;	//executers which do the events
	private SymbolicExecution symbolicExecution;			//generate symbolic information of all methods
	
	/*
	 * Miscellaneous fields
	 */
	private Event closeKeyboardEvent; 						//predefined event
	private LinesSummaryMatcher matcher;					//matcher between line numbers
	private long closeKeyboardSleep = 1000;
	private boolean noReinstall = true;
	
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
		
		eventDeposit = new EventDeposit();
		bpReader = new BreakPointReader(jdbDeviceSerial);
		viewInfoJDB = new ViewDeviceInfo(jdbDeviceSerial);
		viewInfoView = new ViewDeviceInfo(viewDeviceSerial);
		collector_jdbDevice = new InformationCollector(jdbDeviceSerial);
		collector_viewDevice = new InformationCollector(viewDeviceSerial);
		methodSigToSummaries = new HashMap<String, List<WrappedSummary>>();
		logcatReader = new LogcatReader(viewDeviceSerial, adbPath, "System.out");
		
		viewDeviceExecuter = new Executer(viewDeviceSerial, eventDeposit);
		jdbDeviceExecuter = new Executer(jdbDeviceSerial);
		symbolicExecution = new SymbolicExecution(app);
		symbolicExecution.debug = false;
		matcher = new BasicMatcher(app);
		eventSummaryDeposit = new EventSummaryDeposit();
		
		closeKeyboardEvent = EventFactory.createCloseKeyboardEvent();
		currentLayout = null;
	}

	@Override
	public void onPreparation() {
		this.viewDeviceExecuter.enableRecordingEvent(false);
		
//		this.jdbDeviceExecuter.applyEvent(EventFactory.CreatePressEvent(GraphicalLayout.Launcher,
//				KeyEvent.KEYCODE_MENU ));
//		this.viewDeviceExecuter.applyEvent(EventFactory.CreatePressEvent(GraphicalLayout.Launcher,
//				KeyEvent.KEYCODE_MENU ));
		
		this.jdbDeviceExecuter.applyEvent(EventFactory.CreatePressEvent(GraphicalLayout.Launcher,
				KeyEvent.KEYCODE_HOME ));
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
	public void onExplorationProcess(final Event newEvent) {
		Logger.trace("Event: "+newEvent +" with current layout: "+this.currentLayout);
		/*
		 * Check if the current layout is the desired one. Reposition if necessary, 
		 * which should take both devices to the desired layout. 
		 */
		if(!this.currentLayout.equals(newEvent.getSource())){
			if (!repositionToLayout(newEvent.getSource())) {
				return; //ignore the event upon failure -- should not happen 
			}
		}
		ExecutionResult actualResult = executeAndConstruct(newEvent);
//		List<List<WrappedSummary>> premutatedList = Utility.permutate(actualResult.mappedSummaryCandidatesList);
		
//		System.out.println("mappedSummaryCandidatesList: "+actualResult.mappedSummaryCandidatesList);
//		System.out.println("premutatedList: "+premutatedList);
//		Logger.trace("Method root: "+actualResult.methodRoots);
//		Logger.trace("Consturcting validation sequence with root amount: "+(actualResult.methodRoots==null?0:actualResult.methodRoots.size()));
//		Logger.trace("Permutated size: "+premutatedList.size());
//		List<Integer> sizeList = new ArrayList<Integer>();
//		for(List<WrappedSummary> wList : actualResult.mappedSummaryCandidatesList){
//			sizeList.add(wList.size());
//		}
//		Logger.trace("Candidates size for each: "+sizeList);
		
		
		List<List<WrappedSummary>> linarList = linearHelper(actualResult.mappedSummaryCandidatesList);
//		premutatedList
		List<EventSummaryPair> validationCandidate = filterConstructAndDesposit(linarList, newEvent, actualResult.methodRoots, actualResult.esPair);
		if(newEvent.getSource()!=GraphicalLayout.Launcher){
			Logger.trace("Size: "+(validationCandidate==null?0:validationCandidate.size()));
			this.newValidationEvent = validationCandidate;
		}
		// no need to check event deposit
		this.lastExecutedEvent = actualResult.esPair;
		this.lastExecutedEvent.setConcreateExecuted();
		this.lastExecutedEvent.increaseTryCount();
		model.update(lastExecutedEvent, actualResult.resultedLayout); //since this is a new event
		this.currentLayout = lastExecutedEvent.getEvent().getDest();
		eventSummaryDeposit.deposit(this.lastExecutedEvent);
		Logger.trace();
	}	

	@Override
	public void onExpansionProcess(EventSummaryPair toValidate) {
		if(toValidate.isConcreateExecuted()){
			Logger.trace("Is concrete executed");
			return;
		}
		if(!this.eventSummaryDeposit.contains(toValidate)){
			Logger.trace(toValidate.toString());
			throw new AssertionError();
		}
		
		Logger.debug(toValidate.toString());
		List<Event> sequence = this.model.solveForEvent(toValidate);
		
		Logger.trace("Validation sequence for "+toValidate+": "+sequence);
		if (sequence == null){ 
			toValidate.increaseTryCount(); return;
		}
		
		// fail to solve
		
		reinstallApplication();
		WindowOverview winOverview = null;
		for (int i = 0 ; i < sequence.size() - 1; i++) {
			this.viewDeviceExecuter.applyEvent(sequence.get(i),false);
			this.jdbDeviceExecuter.applyEvent(sequence.get(i));
			winOverview = collector_jdbDevice.getWindowOverview();
			while(winOverview == null){
				winOverview = collector_jdbDevice.getWindowOverview();
			}//should eventually get the correct one
			if (winOverview.isKeyboardVisible()) { 
				try { Thread.sleep(closeKeyboardSleep); } catch (InterruptedException e) { }
				closeKeyboardForBothDevices(); 
			}
		}
		GraphicalLayout targetLayout = this.model.findSameOrAddLayout(winOverview.getFocusedWindow().actName, viewInfoJDB.loadWindowData());
		if (targetLayout.equals(toValidate.getEvent().getSource()) == false) {
			Logger.debug("unexpected layout"); return;
		}

		Logger.trace("ready to try event");
		ExecutionResult result = executeAndConstruct(toValidate.getEvent());
		
		
		boolean successful = validationComparionUnderLinearPolicy(result, toValidate);
		EventSummaryPair actualOne = eventSummaryDeposit.findOrConstruct(result.esPair);
		if(successful){
			if(actualOne != toValidate){
				toValidate.increaseTryCount();
				toValidate.setIgnored();
			}
		}else{ toValidate.increaseTryCount();  }
		
		actualOne.increaseTryCount();
		if(actualOne.isConcreateExecuted() == false){
			actualOne.setConcreateExecuted();
			this.model.update(actualOne, result.resultedLayout);
		}
		this.lastExecutedEvent = actualOne;
		this.currentLayout = lastExecutedEvent.getEvent().getDest();
		
		Logger.trace(actualOne.toString()+" validation: "+successful);
		
//		if(successful){
//			EventSummaryPair actualOne = eventSummaryDeposit.findOrConstruct(result.esPair);
//			if(actualOne != toValidate){
//				toValidate.increaseTryCount();
//				toValidate.setIgnored();
//			}
//			actualOne.increaseTryCount();
//			if(actualOne.isConcreateExecuted() == false){
//				actualOne.setConcreateExecuted();
//				this.model.update(actualOne, result.resultedLayout);
//			}
//			this.lastExecutedEvent = actualOne;
//			this.currentLayout = lastExecutedEvent.getEvent().getDest();
//		}else{
//			toValidate.increaseTryCount();
//			EventSummaryPair actualOne = eventSummaryDeposit.findOrConstruct(result.esPair);
//			actualOne.increaseTryCount();
//			if(actualOne.isConcreateExecuted() == false){
//				actualOne.setConcreateExecuted();
//				this.model.update(actualOne, result.resultedLayout);
//			}
//			this.lastExecutedEvent = actualOne;
//			this.currentLayout = lastExecutedEvent.getEvent().getDest();	
//		}
		
		
		
		
		/*
		 * Compare the resulted summary with the one to validate
		 * Those two are considered equal if exactly the same summaries. 
		 * 
		 * If the actual execution log contains the log in the input summary,
		 * then the input summary is discard. 
		 * 
		 * Failure in other cases?
		 * 
		 * Generate new validation summary the method root contains unexpected one.
		 * 
		 * 
		 * Same, Mixed, contain
		 * 
		 * Note: Assuming only the last few method of the actual execution could ever be missing
		 */
//		boolean knownBranchFullyMatches = true;
//		if( result.methodRoots.size() < toValidate.getMethodRoots().size() ){
//			knownBranchFullyMatches = false;
//		}else{
//			for(int i =0;i<toValidate.getMethodRoots().size(); i++){
//				WrappedSummary sum1= toValidate.getSummaryList().get(i);
//				WrappedSummary sum2 = result.esPair.getSummaryList().get(i);
//				//check perfect match for the first few
//				if(sum1 == null){ if(sum2 != null){ knownBranchFullyMatches = false; break; }
//				}else{ if(!sum1.equals(sum2)){ knownBranchFullyMatches = false; break; }}
//			}
//		}
//		Logger.trace("knownBranchFullyMatches: "+knownBranchFullyMatches);
//		if(knownBranchFullyMatches){
//			if(result.methodRoots.size() == toValidate.getMethodRoots().size()){
//				Logger.trace("Perfect match");
//				//perfect match
//				toValidate.setConcreateExecuted();
//				toValidate.increaseTryCount();
//				this.model.update(toValidate, result.resultedLayout);
//				this.lastExecutedEvent = toValidate;				
//				this.currentLayout = lastExecutedEvent.getEvent().getDest();
//			}else{
//				Logger.trace("Partial match");
//				//there is missing summaries
//				if(this.eventSummaryDeposit.contains(result.esPair)){
//					//check if the deposit contains it.
//					//does not expect this
//					toValidate.increaseTryCount();
//					EventSummaryPair actualOne = eventSummaryDeposit.checkAndDeposit(result.esPair);
//					actualOne.increaseTryCount();
//					if(actualOne.isConcreateExecuted() == false){
//						this.model.update(actualOne, result.resultedLayout);
//						actualOne.setConcreateExecuted();
//					}
//					this.lastExecutedEvent = actualOne;
//					this.currentLayout = lastExecutedEvent.getEvent().getDest();
//				}else{//this is new as the deposit does not contain it
//					//update first
//					toValidate.increaseTryCount();
//					toValidate.setIgnored();
//					eventSummaryDeposit.deposit(result.esPair);
//					result.esPair.increaseTryCount();
//					model.update(result.esPair, result.resultedLayout);
//					result.esPair.setConcreateExecuted();
//					this.lastExecutedEvent = result.esPair;
//					this.currentLayout = lastExecutedEvent.getEvent().getDest();
//					
//					//create more symbolic
//					List<String> remain = result.methodRoots.subList(toValidate.getMethodRoots().size(), result.methodRoots.size());
//					//create new symbolic summary
//					List<List<WrappedSummary>> listCandidates = this.findSummaryCandidates(remain);
//					List<List<WrappedSummary>> partialPermutation = Utility.permutate(listCandidates);
//					List<EventSummaryPair> permutatedList = new ArrayList<EventSummaryPair>();
//					Set<EventSummaryPair> set = this.eventSummaryDeposit.getSet(toValidate.getEvent());
//					for(int i =0; i< permutatedList.size(); i++){
//						List<WrappedSummary> partial = partialPermutation.get(i);
//						List<WrappedSummary> connected = new ArrayList<WrappedSummary>(toValidate.getSummaryList());
//						connected.addAll(partial);
//						if(result.esPair.hasExactTheSameExecutionLog(connected)) continue;
//						EventSummaryPair candidate = new EventSummaryPair(toValidate.getEvent().clone(),connected, result.methodRoots);
//						if(set.add(candidate)){ permutatedList.add(candidate); }
//					}
//					this.newValidationEvent = permutatedList;
//				}
//			}
//		}else{ //less method or mix method
//			//does not generate any new symbolic esPair
//			//assume not new method could occur in the list
//			toValidate.increaseTryCount();
//			EventSummaryPair actualOne = eventSummaryDeposit.checkAndDeposit(result.esPair);
//			actualOne.increaseTryCount();
//			if(actualOne.isConcreateExecuted() == false){
//				this.model.update(actualOne, result.resultedLayout);
//				actualOne.setConcreateExecuted();
//			}
//			this.lastExecutedEvent = actualOne;
//			this.currentLayout = lastExecutedEvent.getEvent().getDest();
//		}
	}

	@Override
	public void onFinish() {
		int totalValidated = 0, totalPairs = 0;
		for(Entry<String, List<EventSummaryPair>> entry : this.eventSummaryDeposit.data.entrySet()){
			List<EventSummaryPair> set = entry.getValue();
			int validated = 0;
			Iterator<EventSummaryPair> iter = set.iterator();
			while(iter.hasNext()){
				if(iter.next().isConcreateExecuted()){ validated += 1; }
			}
			System.out.println(entry.getKey()+" : "+validated);
			totalValidated += validated;
			totalPairs += set.size();
		}

		System.out.println("Total event path summary pairs: "+totalPairs+" ");
		System.out.println("Validated: "+totalValidated+" ");	
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
		EventSummaryPair result = lastExecutedEvent;
		lastExecutedEvent = null;
		return result;
	}
	
	@Override
	public List<Event> getLatestSequence() {
		return this.eventDeposit.getLastestEventSequnce();
	}
	
	@Override
	public Set<String> getLatestLineHit() {
		Set<String> result = this.latestLineHit;
		this.latestLineHit = null;
		return result;
	}
	
	@Override
	public Serializable getDumpData() {
		ArrayList<Serializable> list = new ArrayList<Serializable>();
		list.add(eventDeposit);
		list.add(eventSummaryDeposit);
		list.add((HashMap<String,List<WrappedSummary>>)methodSigToSummaries);
		list.add(WrappedSummary.model_raw);
		list.add(WrappedSummary.mode_wrap);
		
		return list;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void restore(Object dumped) {
		ArrayList<Serializable> list = (ArrayList<Serializable>) dumped;
		eventDeposit = (EventDeposit) list.remove(0);
		eventSummaryDeposit = (EventSummaryDeposit) list.remove(0);
		methodSigToSummaries = (Map<String, List<WrappedSummary>>) list.remove(0);
		WrappedSummary.model_raw = (DefaultListModel<PathSummary>)list.remove(0);
		WrappedSummary.mode_wrap = (DefaultListModel<WrappedSummary>)list.remove(0);
	}
	
	public EventDeposit getEventDeposit(){
		return this.eventDeposit;
	}
	public Map<String, List<WrappedSummary>> getMethodSigToSummaries(){
		return this.methodSigToSummaries;
	}
	public EventSummaryDeposit getEventSummaryDeposit(){
		return this.eventSummaryDeposit;
	}
	public DefaultListModel<PathSummary> getAllKnownPathSummaries(){
		return WrappedSummary.model_raw;
	}
	public DefaultListModel<WrappedSummary> getAllKnownWrappedSummaries(){
		return WrappedSummary.mode_wrap;
	}
	public EventSummaryDeposit getESDeposit(){
		return this.eventSummaryDeposit;
	}
	
	public void forceNoReinstall(boolean noReinstall){
		this.noReinstall = noReinstall;
		jdbDeviceExecuter.setForceNotReinstall(noReinstall);
		viewDeviceExecuter.setForceNotReinstall(noReinstall);
	}
	
	/*Help method Section*/
	
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
	private void processLogcatFeedBack(List<String> methodRoots, List<List<WrappedSummary>> mappedSummaryCandidates){
		List<String> feedBack = logcatReader.readLogcatFeedBack();
		if (feedBack != null && feedBack.isEmpty() == false) {
			List<DefaultMutableTreeNode> methodIOTrees = logcatReader.buildMethodCallTree(feedBack);
			methodRoots.addAll(logcatReader.getMethodRoots(methodIOTrees));
//			for (String methodSig : methodRoots) {
//				List<WrappedSummary> summaries = methodSigToSummaries.get(methodSig);
//				if (summaries == null) {
//					summaries = WrappedSummary
//							.wrapSummaryList(symbolicExecution.doFullSymbolic(methodSig));
//					methodSigToSummaries.put(methodSig, summaries);
//				}
//				if(summaries == null){
//					summaries = new ArrayList<WrappedSummary>();
//				}
//				mappedSummaryCandidates.add(summaries);
//			}
			mappedSummaryCandidates.addAll(findSummaryCandidates(methodRoots));
		}
	}
	
	private List<List<WrappedSummary>> findSummaryCandidates(List<String> methods){
		List<List<WrappedSummary>> listCandidates = new ArrayList<List<WrappedSummary>>();
		for (String methodSig : methods) {
			List<WrappedSummary> summaries = methodSigToSummaries.get(methodSig);
			if (summaries == null) {
				summaries = WrappedSummary
						.wrapSummaryList(symbolicExecution.doFullSymbolic(methodSig));
				methodSigToSummaries.put(methodSig, summaries);
			}
			if(summaries == null){
				summaries = new ArrayList<WrappedSummary>();
			}
			listCandidates.add(summaries);
		}
		return listCandidates;
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
						try { Thread.sleep(closeKeyboardSleep); } catch (InterruptedException e) { }
						closeKeyboardForBothDevices();
					}
					
				}
				try { Thread.sleep(300); } catch (InterruptedException e) { }
				GraphicalLayout layout = model.findSameOrAddLayout(
						winOverview.getFocusedWindow().actName, 
						this.viewInfoView.loadWindowData());
				this.currentLayout = layout;
				if (layout.equals(targetLayout)) { return true; }
			}else{ Logger.trace("No valid sequence"); }
		}
		
//		Logger.trace(this.currentLayout.getActName() +" with "+ TreeUtility.countNodes(this.currentLayout));
//		Logger.trace(targetLayout.getActName()+" with "+ TreeUtility.countNodes(targetLayout));
//		Logger.trace(targetLayout.hasTheSmaeLayout(this.currentLayout.getRootNode()));
//		Logger.registerJPanel(currentLayout.getActName(), new JTree(currentLayout.getRootNode()));
//		Logger.registerJPanel(targetLayout.getActName(), new JTree(targetLayout.getRootNode()));
//		CommandLine.requestInput();
		
		/*
		 * Find a sequence to the target layout in the event deposit which 
		 * records all events (other than closing keyboard) previously executed.
		 * 
		 * The sequence returned should be the shortest possible one.
		 */
		Logger.trace("Reposition 2nd phase");
		this.reinstallApplication();
		{// try to use the sequence from the event deposit
			List<Event> sequence = this.eventDeposit.findSequenceToLayout(targetLayout);
			Logger.trace("From deposit: "+sequence);
			if (sequence != null && sequence.isEmpty() == false) {
				WindowOverview winOverview = null;
				for (Event event : sequence) {
					this.viewDeviceExecuter.applyEvent(event,false);
					this.jdbDeviceExecuter.applyEvent(event);
					winOverview = this.collector_viewDevice.getWindowOverview();
					
					if (winOverview.isKeyboardVisible()) {
						try { Thread.sleep(closeKeyboardSleep); } catch (InterruptedException e) { }
						this.closeKeyboardForBothDevices();
					}
				}
				try { Thread.sleep(300); } catch (InterruptedException e) { }
				GraphicalLayout layout = model.findSameOrAddLayout(
						winOverview.getFocusedWindow().actName, 
						this.viewInfoView.loadWindowData());
				this.currentLayout = layout;
				if (layout.equals(targetLayout)) { return true; }
			}
		}
		
//		Logger.trace(this.currentLayout.getActName() +" with "+ TreeUtility.countNodes(this.currentLayout));
//		Logger.trace(targetLayout.getActName()+" with "+ TreeUtility.countNodes(targetLayout));
//		Logger.trace(targetLayout.hasTheSmaeLayout(this.currentLayout.getRootNode()));
//		Logger.registerJPanel(currentLayout.getActName(), new JTree(currentLayout.getRootNode()));
//		Logger.registerJPanel(targetLayout.getActName(), new JTree(targetLayout.getRootNode()));
//		CommandLine.requestInput();
		
		Logger.trace("Reposition failure");
		return false;
	}

	/**
	 * Close the keyboard by sending the closeing keyboard event
	 * which now is press back event
	 */
	private void closeKeyboardForBothDevices() {
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

	/**
	 * Filter when 
	 * 1. it is the same as given known esPair
	 * 2. has no constraint which cannot be validated.
	 * 3. the same pair can be found in the deposit
	 * 
	 * Construct the 
	 * 
	 * 
	 * @param potentialCombination
	 * @param known
	 * @return
	 */
	private List<EventSummaryPair> filterConstructAndDesposit(List<List<WrappedSummary>> potentialCombination, Event event, List<String> methodRoots,  EventSummaryPair known){
		this.eventSummaryDeposit.deposit(known);
		List<EventSummaryPair> result = new ArrayList<EventSummaryPair>();
		if(potentialCombination == null) return null;
		for(List<WrappedSummary> wrappedList : potentialCombination){
			boolean hasSameExecutionLog = known.hasExactTheSameExecutionLog(wrappedList);
			if(hasSameExecutionLog) continue;
			//must has constraint, otherwise it is pointless to validate
			EventSummaryPair esPair = new EventSummaryPair(event.clone(), wrappedList, methodRoots);
			List<Expression> constraints = esPair.getCombinedConstraint();
			if(constraints == null || constraints.isEmpty()) continue;
			if(this.eventSummaryDeposit.deposit(esPair)){
				result.add(esPair);//new one 
			}
		}
		return result;
	}
	
	/**
	 * Execute the event on both devices. Read logcat and BP hits. 
	 * Map the result BP hits to a list of path summary.
	 * Construct an esPair based on collected information. 
	 * Will not check with the deposit if the summary existed. 
	 * Does not increase the try count
	 * Does not set concrete execution
	 * @param event
	 * @return
	 */
	private ExecutionResult executeAndConstruct(Event event){
		ExecutionResult result = new ExecutionResult();
		
		logcatReader.clearLogcat();
		/*
		 * Logcat reading can be the indication of when the program 
		 * become stable, which can potentially replace the static 
		 * thread sleeping.
		 * 
		 * Note: There is a static sleep because of the intention
		 * of letting all logcat feedback ready. 
		 */
		this.viewDeviceExecuter.applyEvent(event);
		
		/*
		 * There could be multiple method roots.
		 * Each root could be mapped to a list of summary. 
		 */
		List<String> methodRoots = new ArrayList<String>();
		List<List<WrappedSummary>> mappedSummaryCandidatesList = new ArrayList<List<WrappedSummary>>();
		processLogcatFeedBack(methodRoots, mappedSummaryCandidatesList);
//		Logger.debug("finish reading logcat feedback, methodRoots: "+methodRoots);
//		Logger.debug("Mapped summary Candidates: " + mappedSummaryCandidatesList);
		
		/*
		 * Check the Window information, including visible windows, keyboards
		 * Close the keyboard if necessary.  
		 * Retrieve the layout information the current visible window is in scope
		 */
		WindowOverview winOverview = collector_viewDevice.getWindowOverview();
		WindowInformation focusedWin = winOverview.getFocusedWindow();
		while(focusedWin == null){
			try { Thread.sleep(20); } catch (InterruptedException e) { }
			focusedWin = collector_viewDevice.getWindowOverview().getFocusedWindow();
		}
		
		int scope = focusedWin.isWithinApplciation(this.app);
		GraphicalLayout resultedLayout = null;
		boolean inputMethodVisible = winOverview.isKeyboardVisible();
		if (inputMethodVisible) { 
			try { Thread.sleep(closeKeyboardSleep); } catch (InterruptedException e) { }
			this.viewDeviceExecuter.applyEvent(closeKeyboardEvent);
		}
		
		switch (scope) {
		case WindowInformation.SCOPE_LAUNCHER: {
			Logger.trace("window is launcher");
			resultedLayout = GraphicalLayout.Launcher;
		}break;
		case WindowInformation.SCOPE_WITHIN: {
			Logger.trace("window is within the application");
			resultedLayout = model.findSameOrAddLayout(focusedWin.actName, viewInfoView.loadWindowData());
//					new GraphicalLayout(focusedWin.actName,
//					viewInfoView.loadWindowData());
		}break;
		case WindowInformation.SCOPE_OUT: {
			Logger.trace("window is outside the application");
			resultedLayout = model.findSameOrAddLayout(focusedWin.actName, null);
//			resultedLayout = new GraphicalLayout(focusedWin.actName, null);
		}break;
		}
		
		EventSummaryPair actualPair = null;
		if(event.getSource().equals(GraphicalLayout.Launcher)){
			/**
			 * Cannot get the path information if the event starts from launcher due to 
			 * current implementation. As a compromise, choose the path summary with the 
			 * most symbolic states.
			 */
			this.jdbDeviceExecuter.applyEvent(event);
//			if (inputMethodVisible) { jdbDeviceExecuter.applyEvent(closeKeyboardEvent); }
			
			List<WrappedSummary> mappedSummaryList = new ArrayList<WrappedSummary>();
			for(String method : methodRoots){
				WrappedSummary mapped = findBestCandidate(mappedSummaryCandidatesList, methodRoots, method);
				mappedSummaryList.add(mapped);
			}
			actualPair = new EventSummaryPair(event, mappedSummaryList, methodRoots);
			
			Set<String> hitLines = new HashSet<String>();
			for(WrappedSummary sum : mappedSummaryList){
				if(sum.executionLog != null)
					hitLines.addAll(sum.executionLog);
			}
			this.latestLineHit = hitLines;
			
		}else if(methodRoots.size()>0 && mappedSummaryCandidatesList.size() > 0){
			/**
			 * There is method call information. Map them to some summaries and generate validation sequences
			 */
			Logger.trace("Setup BPs");
			bpReader.setup(app, methodRoots);
			Thread t = new Thread(new Runnable(){
				@Override public void run() { jdbDeviceExecuter.applyEvent(event); }
			});
			t.start();
			try { Thread.sleep(400); } catch (InterruptedException e1) { }
			List<List<String>> logSequences = bpReader.readExecLog();
			if(logSequences != null && !logSequences.isEmpty()){
				Set<String> lineHit = new HashSet<String>();
				for(List<String> subList : logSequences){
					for(String line : subList){
						lineHit.add(line);
					}
				}
				latestLineHit = lineHit;
			}
			
			try { t.join(1000); } catch (InterruptedException e) { e.printStackTrace(); }
			if(t.isAlive()){ 
				t.setPriority(Thread.MIN_PRIORITY);
				t.interrupt();}
			Logger.trace("Thread existed");
//			if (inputMethodVisible) { 
//				try { Thread.sleep(closeKeyboardSleep); } catch (InterruptedException e) { }
//				jdbDeviceExecuter.applyEvent(closeKeyboardEvent); 
//			}
			List<WrappedSummary> mappedList = new ArrayList<WrappedSummary>();
			for(int i = 0 ; i< mappedSummaryCandidatesList.size() && i< logSequences.size(); i++){
				List<String> ithLogSequnence = logSequences.get(i);
				List<WrappedSummary> summaryCandidates = mappedSummaryCandidatesList.get(i);
				
				int matchIndex = matcher.matchSummary(ithLogSequnence, WrappedSummary.unwrapSummary(summaryCandidates));
				if (matchIndex < 0) { mappedList.add(null); // mapping failure
				} else { mappedList.add(summaryCandidates.get(matchIndex)); }
			}
			
			actualPair = new EventSummaryPair(event, mappedList, methodRoots);//dont clone
//			/*
//			 * Generate the symbolic event summary pair for the validation
//			 */
//			List<List<WrappedSummary>> potentialCombination = Utility.permutate(mappedSummaryCandidatesList);
//			List<EventSummaryPair> candidates = (filterAndConstruct(potentialCombination, event.clone(), methodRoots, executedSum));
//			result.validationCandidate = candidates;
		}else{
			Logger.trace("No BP reading");
			/**
			 * No method call information. Simply do the event
			 */
			this.jdbDeviceExecuter.applyEvent(event);
			actualPair = new EventSummaryPair(event, null, methodRoots);
		}
		
		if (inputMethodVisible) { 
			try { Thread.sleep(closeKeyboardSleep); } catch (InterruptedException e) { }
			jdbDeviceExecuter.applyEvent(closeKeyboardEvent); 
		}
		
		result.mappedSummaryCandidatesList = mappedSummaryCandidatesList;
		result.methodRoots = methodRoots;
		result.resultedLayout = resultedLayout;
		result.esPair = actualPair;
		this.eventDeposit.addLatestESPair(actualPair);
		Logger.trace("End");
		return result;
	}
	
	private List<List<WrappedSummary>> linearHelper(List<List<WrappedSummary>> candidateList){
		List<List<WrappedSummary>> result = new ArrayList<List<WrappedSummary>>();
		for(List<WrappedSummary> list : candidateList){
			for(WrappedSummary element : list){
				List<WrappedSummary> local = new ArrayList<WrappedSummary>();
				local.add(element);
				result.add(local);
			}
		}
		return result;
	}
	
	private boolean validationComparionUnderLinearPolicy(ExecutionResult result, EventSummaryPair toValidate){
		EventSummaryPair resultPair = result.esPair;
		if(resultPair.getSummaryList() == null || resultPair.getSummaryList().isEmpty()) return false;
		for(WrappedSummary sum : toValidate.getSummaryList()){
			if(!resultPair.getSummaryList().contains(sum)) return false;
		}
		return true;
	}
	
	private class ExecutionResult{
		EventSummaryPair esPair;
		List<List<WrappedSummary>> mappedSummaryCandidatesList;
		List<String> methodRoots;
		List<String> executionLog;
		GraphicalLayout resultedLayout;
	}

}
