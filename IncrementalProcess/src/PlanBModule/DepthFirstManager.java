package PlanBModule;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.util.Comparator;

import javax.swing.BorderFactory;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import staticFamily.StaticApp;
import support.Logger;
import symbolic.Expression;
import components.Event;
import components.EventFactory;
import components.EventSummaryPair;
import components.GraphicalLayout;
import components.WrappedSummary;

public class DepthFirstManager extends AbstractManager{
	
	/* 
	 * Operation field
	 */
	private EventSummaryPair currentESPair;
	private Stack<Event> newEventStack;
	private PriorityQueue<EventSummaryPair> validationQueue, targetQueue;
	private List<EventSummaryPair> ignoredList, confirmedList;
	private CallBack callBack;
	
	/*
	 * Target related field
	 */
	private String[] targets;
	private Map<String, Boolean> reachedTargets = new HashMap<String, Boolean>();
	
	/*
	 * data recording field 
	 */
	private Set<String> lineHit;
	private int totalConcreateExecution = 0, newConcreteExecution = 0, executionCount = 0;
	private int maxIndividualValidationTry = 5;
	private int iterationCount = 0;
	private Decision previousDecision;
	
	/*
	 * GUI field
	 */
	private boolean enableGUI = true;
	private DefaultTableModel newEventModel, executedEventModel;
	private Map<String, JTextArea> targetTextAreas = new HashMap<String, JTextArea>();
	private Map<String, JTextArea> classCatergoryPane = new HashMap<String, JTextArea>();
	private JTextArea targetArea, validArea;
	
	
	public DepthFirstManager(StaticApp app, UIModel model){
		super(app, model);
		
		if(!enableGUI){
			newEventStack = new Stack<Event>();
			confirmedList = new ArrayList<EventSummaryPair>();
			ignoredList = new ArrayList<EventSummaryPair>();
			targetQueue = new PriorityQueue<EventSummaryPair>(new ESPriority()){
				@Override
				public boolean add(EventSummaryPair esPair){
					if(esPair.getTryCount() >= maxIndividualValidationTry){
						ignoredList.add(esPair);
						return false;
					}
					return super.add(esPair);
				}
			};
			validationQueue = new PriorityQueue<EventSummaryPair>(new ESPriority()){
				@Override
				public boolean add(EventSummaryPair esPair){
					if(esPair.getTryCount() >= maxIndividualValidationTry){
						ignoredList.add(esPair);
						return false;
					}
					return super.add(esPair);
				}
			};
			lineHit = new HashSet<String>();
		}else{
			final JPanel pane = new JPanel();
			pane.setLayout(new GridLayout(0,1));
			/*New Event queue*/
			final JTable newEventTable = new JTable();
			final JScrollPane eventAreaContainer = new JScrollPane();
			eventAreaContainer.setViewportView(newEventTable);
			this.newEventModel = new DefaultTableModel();
			this.newEventModel.setColumnIdentifiers(Event.ColumnsIdentifier);
			newEventTable.setModel(newEventModel);
			final TitledBorder border_newEvent = BorderFactory.createTitledBorder(null, "New Event Stack");
			eventAreaContainer.setBorder(border_newEvent);
			
			/*executed Event queue*/
			final JTable executedEventTable = new JTable();
			final JScrollPane exectuedEventContainer = new JScrollPane();
			exectuedEventContainer.setViewportView(executedEventTable);
			this.executedEventModel = new DefaultTableModel();
			this.executedEventModel.setColumnIdentifiers(Event.ColumnsIdentifier);
			executedEventTable.setModel(executedEventModel);
			final TitledBorder border_executed = BorderFactory.createTitledBorder(null, "Executed Event List");
			exectuedEventContainer.setBorder(border_executed);
			
			pane.add(eventAreaContainer);
			pane.add(exectuedEventContainer);
			
			Logger.registerJPanel("Manager status", pane);			
			newEventStack = new Stack<Event>(){
				@Override
				public Event pop(){
					newEventModel.removeRow(0);
					Event result = super.pop();
					executedEventModel.addRow(result.toStringArray());
					return result;
				}
				
				@Override
				public Event push(Event event){
					newEventModel.insertRow(0, event.toStringArray());
					return super.push(event);
				}
			};
			
			JPanel queuePane = new JPanel();
			queuePane.setLayout(new GridLayout(4,1));
			JScrollPane targetContainer = new JScrollPane();
			JScrollPane validationContainer = new JScrollPane();
			JScrollPane confiemdContainer = new JScrollPane(); 
			JScrollPane ignoedContainer = new JScrollPane();
			
			TitledBorder targetQueueTitle = BorderFactory.createTitledBorder("Target Queue");
			TitledBorder validationQueueTitle = BorderFactory.createTitledBorder("Validation Queue");
			TitledBorder confirmedListTitle = BorderFactory.createTitledBorder("Confirmed list");
			TitledBorder ignoredListTitle = BorderFactory.createTitledBorder("Ignored list");
			
			targetContainer.setBorder(targetQueueTitle);
			validationContainer.setBorder(validationQueueTitle);
			confiemdContainer.setBorder(confirmedListTitle);
			ignoedContainer.setBorder(ignoredListTitle);
			
			targetArea = new JTextArea();
			validArea = new JTextArea();
			JTextArea confirmArea = new JTextArea();
			JTextArea ignoreArea = new JTextArea();
			
			targetContainer.setViewportView(targetArea);
			validationContainer.setViewportView(validArea);
			confiemdContainer.setViewportView(confirmArea);
			ignoedContainer.setViewportView(ignoreArea);
			
			queuePane.add(targetContainer);
			queuePane.add(validationContainer);
			queuePane.add(confiemdContainer);
			queuePane.add(ignoedContainer);
			
			confirmedList = new ArrayList<EventSummaryPair>(){
				@Override
				public boolean add(EventSummaryPair esPair){
					confirmArea.append(esPair.toString()+"\n");
					return super.add(esPair);
				}
			};
			ignoredList = new ArrayList<EventSummaryPair>(){
				@Override
				public boolean add(EventSummaryPair esPair){
					ignoreArea.append(esPair.toString()+"\n");
					return super.add(esPair);
				}
			};
			targetQueue = new PriorityQueue<EventSummaryPair>(new ESPriority()){
				@Override
				public boolean add(EventSummaryPair esPair){
					if(esPair.getTryCount() >= maxIndividualValidationTry){
						ignoredList.add(esPair);
					}else{ super.add(esPair); }
					updateTargetQueuePane();
					return true;
				}
				@Override
				public EventSummaryPair poll(){
					EventSummaryPair result = super.poll();
					updateTargetQueuePane();
					return result;
				}
			};
			validationQueue = new PriorityQueue<EventSummaryPair>(new ESPriority()){
				@Override
				public boolean add(EventSummaryPair esPair){
					if(esPair.getTryCount() >= maxIndividualValidationTry){
						ignoredList.add(esPair);
					}else{ super.add(esPair); }
					updateValidationPane();
					return true;
				}
				@Override
				public EventSummaryPair poll(){
					EventSummaryPair result = super.poll();
					updateValidationPane();
					return result;
				}
			};
			
			Logger.registerJPanel("Queue", queuePane);
			
			
			JTabbedPane lineHitPane = new JTabbedPane();
			classCatergoryPane = new HashMap<String, JTextArea>();
			lineHit = new HashSet<String>(){
				@Override
				public boolean add(String line){
					boolean isAdded = super.add(line);
					if(isAdded){
						int index = line.lastIndexOf(":");
						String className = line.substring(0, index);
						JTextArea area = classCatergoryPane.get(className);
						if(area == null){
							area = new JTextArea();
							classCatergoryPane.put(className, area);
							JScrollPane jsp = new JScrollPane();
							jsp.setViewportView(area);
							lineHitPane.add(className, jsp);
						}
						area.append(line+"\n");
					}
					return isAdded;
				}
			};
			Logger.registerJPanel("Line hit", lineHitPane);
		}
	}
	
	/**
	 * define a list of targets in the format of String
	 * @param methodSignature
	 */
	public void setTargets(String... methodSignature){
		if(methodSignature == null || methodSignature.length == 0) return;
		targets = methodSignature;
		for(String line : methodSignature){ reachedTargets.put(line, false); }
		if(this.enableGUI){
			JScrollPane targetPanel = new JScrollPane();
			Container container = new Container();
			targetPanel.setViewportView(container);
			
			container.setLayout(new GridLayout(0,1)); // only column 
			for(String line : methodSignature){
				TitledBorder titleBorder = BorderFactory.createTitledBorder(line);
				JTextArea area = new JTextArea();
				area.setEditable(false);
				area.setBorder(titleBorder);
				
				JScrollPane jsp = new JScrollPane();
				jsp.setViewportView(area);
				jsp.setPreferredSize(new Dimension(400, 100));
				
				container.add(jsp);
				this.targetTextAreas.put(line, area);
			}
			Logger.registerJPanel("Targets", targetPanel);
		}
	}
	
	@Override
	public void onPreparation() {
		String mainAct = app.getMainActivity().getJavaName();
		String pkgName = app.getPackageName();	
		this.model.defineRoot(GraphicalLayout.Launcher);
		this.newEventStack.push(EventFactory.createLaunchEvent(
				GraphicalLayout.Launcher, pkgName, mainAct));
		if(this.operater == null || this.model == null) throw new AssertionError();
		
		iterationCount =0;
	}
	@Override public void onIterationStepStart() { currentESPair = null; }
	@Override
	public Decision decideOperation() {
		if(isLimitReached()){
			Logger.trace("Limit reached: "+
						"event limit:"+this.newEventStack.isEmpty() +
						"Target limit: "+ ((targetQueue == null) || (this.targetQueue.isEmpty()) || (this.targetQueue.peek().getTryCount() > maxIndividualValidationTry))+
						"ES limit: "+
						(validationQueue == null ||
						validationQueue.isEmpty() ||
						validationQueue.peek().getTryCount() > maxIndividualValidationTry)
					);
			
			return previousDecision=Decision.END;
		}
		if(!this.newEventStack.isEmpty()){ 	  return previousDecision=Decision.EXPLORE;
		}else if(decideReachTarget()){ 		  return previousDecision=Decision.REACHTARGET;
		}else if(!validationQueue.isEmpty()){ return previousDecision=Decision.EXPAND;
		}else{ return previousDecision=Decision.END; }
	}
	
	/**Exploration Mode**/
	@Override public void onExplorationStepStart() { }
	@Override public Event getNextExplorationEvent(){ return this.newEventStack.pop(); }
	@Override public void onExplorationStepEnd() {  }

	/**Expansion mode**/
	@Override public void onExpansionStepStart() {  }
	@Override public EventSummaryPair getNextExpansionEvent() { 
		currentESPair = validationQueue.poll();  
		return this.currentESPair;
	}
	@Override public void onExpansionStepEnd() {}

	/**Reach target mode**/
	@Override public void onReachTargetStart() {}
	@Override public EventSummaryPair getNextTargetSummary() { return targetQueue.poll(); }	
	@Override public void onReachTargetEnd() { Logger.trace(); }
	
	@Override
	public void onIterationStepEnd() {
		// Check from the UI model if there is any new event.
		List<Event> newEvents = this.model.getAdditionalEvent();
		if(newEvents != null){
			for(Event event : newEvents){
				this.newEventStack.push(event);
			}
		}		
		// Check from the Operator if there is any new summary which needs to be validated
		List<EventSummaryPair> toValidateList = operater.getAdditionalValidationEvents();
		int valCount = 0;
		if(toValidateList != null && !toValidateList.isEmpty()){
			for(EventSummaryPair esPair : toValidateList){
//				if(checkEventConstraintExistence(esPair)){
//					//such event constraint pair exist
//					Logger.trace("Ignored: "+esPair);
//				}else{
//					//it is new
//					if(this.targets != null && esPair.getSummaryList() != null){
//						for(WrappedSummary sum : esPair.getSummaryList()){
//							for(String line : this.targets){
//								if(sum.executionLog.contains(line)){
//									esPair.targetLines.add(line);
//								}
//							}
//						}
//					}
//					if(esPair.targetLines.size() > 0){ this.targetQueue.add(esPair);
//					}else{ validationQueue.add(esPair); }
//					valCount += 1;
//				}
				
				if(this.targets != null && esPair.getSummaryList() != null){
					for(WrappedSummary sum : esPair.getSummaryList()){
						for(String line : this.targets){
							if(sum.executionLog.contains(line)){
								esPair.targetLines.add(line);
							}
						}
					}
				}
//				Logger.trace("Validation: "+toValidateList.toString());
				if(esPair.targetLines.size() > 0){ this.targetQueue.add(esPair);
				}else{ validationQueue.add(esPair); }
				valCount += 1;
			}
		}

		//An event summary might be ignored due to the incompleteness of summary list
		//the current esPair is null iff it was an exploration operation
		if(this.currentESPair != null){
			if(this.currentESPair.isIgnored()){
				//do not queue it back
				this.ignoredList.add(currentESPair);
			}else if(!currentESPair.isConcreateExecuted()){
				//not concrete execution, check if it still contain useful target
				boolean containUnreachedTarget = false;
				for(String line : this.currentESPair.targetLines){
					if(reachedTargets.get(line).booleanValue() == false){
						containUnreachedTarget = true;
						break;
					}
				}
				if(containUnreachedTarget){ //and not concrete executed
					this.targetQueue.add(currentESPair);
				}else{ this.validationQueue.add(currentESPair); }
			}else{
				confirmedList.add(currentESPair);
			}
		}
		
		Set<String> lastestLineHit = this.operater.getLatestLineHit();
		if(callBack != null && lastestLineHit != null) callBack.checkExecutionLog(lastestLineHit);
		EventSummaryPair actual = this.operater.getLastExecutedEvent();
		if(actual != null){
			if(!actual.isConcreateExecuted()) throw new AssertionError();
			if(isTargetSet()){
				List<String> targetHits = new ArrayList<String>();
				if(lastestLineHit != null){
					Iterator<String> lineIter = lastestLineHit.iterator();
					while(lineIter.hasNext()){
						String line = lineIter.next();
						this.lineHit.add(line); 
						for(String target : this.targets){
							if(target.equals(line)){targetHits.add(line);break;}
						}
					}
				}
				
				// integrity checking. A BP hit should also be noticed in es pair
				String decisionPrefix = "";
				if(this.previousDecision == Decision.EXPLORE){
					decisionPrefix = "[Explore] ";
				}else if(this.previousDecision == Decision.REACHTARGET){
					decisionPrefix = "[Reach T] ";
				}else if(this.previousDecision == Decision.EXPAND){
					decisionPrefix = "[Expand ] ";
				}
				
				String esString = actual.toString();
				for(String hit : targetHits){
					List<Event> sequence = this.operater.getLatestSequence();
					if(actual.containLine(hit)){ 
						onTargetLineReached(hit, sequence,decisionPrefix + esString + " [As Expected]    ");
					}else{
						onTargetLineReached(hit, sequence,decisionPrefix + esString + " [ES Log missing] ");
					}
				}
			}else{
				List<String> targetHits = new ArrayList<String>();
				if(lastestLineHit != null){
					Iterator<String> lineIter = lastestLineHit.iterator();
					while(lineIter.hasNext()){
						String line = lineIter.next();
						this.lineHit.add(line); 
					}
				}
			}
			
			if(actual.getTryCount() == 1){ // newly executed event summary pair
				newConcreteExecution+=1;
			}
		}
//		//the actual esPair is null if reposition failure during exploration or 
//		//solve for event failure.
//		if(actual != null){ checkTargetReachablility(actual); }
		iterationCount+= 1;
	}
	
	@Override 
	public void onFinish() { 
		for(Entry<String,Boolean> entry : this.reachedTargets.entrySet()){
			String line = entry.getKey();
			boolean reached = entry.getValue();
			System.out.println("Target:    "+line);
			System.out.println("Sucessful: "+reached);
			if(reached){
				JTextArea area = targetTextAreas.get(line);
				String sequences = area.getText();
				System.out.println(sequences);
			}	
		}
		
		if(this.enableGUI){
			Iterator<Entry<String,JTextArea>> iter = classCatergoryPane.entrySet().iterator();
			while(iter.hasNext()){
				Entry<String,JTextArea> entry = iter.next();
				JTextArea area = entry.getValue();
				if(area != null){
					String[] lines = area.getText().split("\n");
					if(lines!=null && lines.length > 0){
						Arrays.sort(lines);
						area.setText(String.join("\n", lines));
					}
				}
			}
		}
	}

	@Override
	public Object getDumpData() {
		List<Object> list = new ArrayList<Object>();
		list.add(newEventStack);
		list.add(validationQueue);
		list.add(targetQueue);
		list.add(ignoredList);
		list.add(confirmedList);
		list.add(targets);
		list.add(reachedTargets);
		list.add(lineHit);
		list.add(totalConcreateExecution);
		list.add(executionCount);
		
		list.add(newEventModel);
		list.add(executedEventModel);
		list.add(targetTextAreas);
		list.add(classCatergoryPane);
		list.add(targetArea);
		list.add(validArea);
		
		return list.toArray(new Object[0]);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void restore(Object dumped) {
		Object[] list = (Object[]) dumped;
		int index = 0;
		newEventStack = (Stack<Event>) list[index]; index++;
		validationQueue = (PriorityQueue<EventSummaryPair>) list[index]; index++;
		targetQueue = (PriorityQueue<EventSummaryPair>) list[index]; index++;
		ignoredList = (List<EventSummaryPair>) list[index]; index++;
		confirmedList = (List<EventSummaryPair>) list[index]; index++;
		targets = (String[]) list[index]; index++;
		reachedTargets = (Map<String, Boolean>) list[index]; index++;
		lineHit = (Set<String>) list[index]; index++;
		totalConcreateExecution = (Integer)list[index]; index++;
		executionCount = (Integer)list[index]; index++;
		
		/*GUI related*/
		newEventModel = (DefaultTableModel) list[index]; index++;
		executedEventModel = (DefaultTableModel) list[index]; index++;
		targetTextAreas = (Map<String, JTextArea>) list[index]; index++;
		classCatergoryPane = (Map<String, JTextArea>) list[index]; index++;
		targetArea = (JTextArea) list[index]; index++;
		validArea = (JTextArea) list[index]; index++;
	}
	
	
	@Override
	public Set<String> getAllHitList() {
		return this.lineHit;
	}
	
	public void setMaxIndividualValidationTry(int max){
		this.maxIndividualValidationTry = max;
	}
	
	public void setCallBack(CallBack callBack){
		this.callBack = callBack;
	}
	/**Miscellaneous helper**/
	
	private boolean isLimitReached(){
		if(newEventStack == null) return false;

		boolean eventLimit = this.newEventStack.isEmpty();
		boolean valLimit = 
				validationQueue == null ||
				validationQueue.isEmpty() ||
				validationQueue.peek().getTryCount() > maxIndividualValidationTry;
		boolean result = eventLimit && valLimit;
		if(this.isTargetSet()){
			boolean targetLimit =
					(targetQueue == null) ||
					(this.targetQueue.isEmpty()) ||
					(this.targetQueue.peek().getTryCount() > maxIndividualValidationTry);
			result = result && targetLimit;
		}
		return result;
	}
	
	/**
	 * Check the execution log in the executed summary if any target line is hit
	 * @param executed
	 */
//	private boolean checkTargetReachablility(EventSummaryPair executed){
//		if(isTargetSet() == false) return false;
//		if(!executed.isConcreateExecuted()) throw new AssertionError();
//		if(executed.getTryCount() == 1){ // newly executed event summary pair
//			newConcreteExecution+=1;
//			List<WrappedSummary> sumList = executed.getSummaryList();
//			if(sumList == null || sumList.isEmpty()) return false;
//			List<Event> sequence = this.operater.getLatestSequence();
//			if(sequence == null) throw new AssertionError();
//			for(WrappedSummary sum : sumList){
//				if(sum == null || sum.executionLog == null) continue;
//				for(String targetLine : targets){//check any target line is hit
//					if(sum.executionLog.contains(targetLine)){
//						onTargetLineReached(targetLine, sequence);
//					}
//				}
//			}
//		}else{ //executed more than one times
//			//don't care
//		}
//		return false;
//	}

	private boolean isTargetSet(){
		return this.targets != null && targets.length > 0;
	}
	
	/**
	 * Every 5 new concrete execution or 10 execution
	 * @return
	 */
	private boolean decideReachTarget(){
		if(isAllReached()) return false;
		if(this.isTargetSet() && !this.targetQueue.isEmpty()){
			EventSummaryPair esPair = this.targetQueue.peek();
			if(esPair.getTryCount() < this.maxIndividualValidationTry &&
					(this.newConcreteExecution >= 5 || executionCount >= 10)){
				return true;
			}
		}
		return false;
	}
	
	private void onTargetLineReached(String line, List<Event> sequence, String prefix){
		boolean wasReached = this.reachedTargets.get(line);
		if(!wasReached){
			reachedTargets.put(line, true);
			transferBetweenQueue();

			JTextArea area = targetTextAreas.get(line);
			area.append(prefix+sequence.toString());
		}
	}
	
	private boolean isAllReached(){
		for(Boolean b : this.reachedTargets.values()){
			if(b.booleanValue() == false){
				return false;
			}
		}
		return true;
	}
	
	private void transferBetweenQueue(){
		List<EventSummaryPair> transfer = new ArrayList<EventSummaryPair>();
		Iterator<EventSummaryPair> iter = this.targetQueue.iterator();
		while(iter.hasNext()){
			EventSummaryPair esPair = iter.next();
			for(String line : esPair.targetLines){
				if(this.reachedTargets.get(line).booleanValue() == false){
					//which indicates the summary contains a unreached line
					//do not transfer
					continue;
				}
			}
			transfer.add(esPair);
		}		
		this.targetQueue.removeAll(transfer);
		this.validationQueue.addAll(transfer);
		updateTargetQueuePane();
		updateValidationPane();
	}
	
	private void updateTargetQueuePane(){
		if(enableGUI){
			StringBuilder sb = new StringBuilder();
			Iterator<EventSummaryPair> iter = this.targetQueue.iterator();
			while(iter.hasNext()){
				sb.append(iter.next().toString()+"\n");
			}
			this.targetArea.setText(sb.toString());
			this.targetArea.revalidate();
		}
	}
	
	private void updateValidationPane(){
		if(enableGUI){
			StringBuilder sb = new StringBuilder();
			Iterator<EventSummaryPair> iter = this.validationQueue.iterator();
			while(iter.hasNext()){
				sb.append(iter.next().toString()+"\n");
			}
			this.validArea.setText(sb.toString());
			this.validArea.revalidate();
		}
	}


	public interface CallBack{
		/**
		 * Called when there is break point reading
		 * @param log -- a 2 dimension matrix where each String represent a execution log
		 * 				and each List<String> represents an executionLog for a method root
		 */
		public void checkExecutionLog(Set<String> log);
		
//		/**
//		 * Called when there is new event generated
//		 * @param newEvents
//		 */
//		public void checkNewEvent(List<Event> newEvents);
	}


	
//	private Map<String, Set<Set<String>>> ecExistence = new HashMap<String,Set<Set<String>>>();
	/**
	 * Check the existence of event-constraint pair
	 * @param esPair
	 * @return true if exist
	 */
//	private boolean checkEventConstraintExistence(EventSummaryPair esPair){
//		Event event = esPair.getEvent();
//		String key = event.toString()+event.getSource();
//		Set<Set<String>> constraintSet = ecExistence.get(key);
//		if(constraintSet == null){//first encounter
//			constraintSet = new HashSet<Set<String>>();
//			ecExistence.put(key, constraintSet);
//		}
//		List<Expression> constraints = esPair.getCombinedConstraint();
//		Set<String> organizedConstraint = new HashSet<String>();
//		for(Expression expre : constraints){
//			String expString = expre.toYicesStatement();
//			organizedConstraint.add(expString);
//		}
//		return !constraintSet.add(organizedConstraint);
//	}
}
