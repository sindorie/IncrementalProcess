package PlanBModule;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.PriorityQueue;
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
	
	/*
	 * Target related field
	 */
	private String[] targets;
	private Map<String, Boolean> reachedTargets = new HashMap<String, Boolean>();
	
	/*
	 * data recording field 
	 */
	private int totalConcreateExecution = 0, newConcreteExecution = 0, executionCount = 0;
	private int maxIndividualValidationTry = 5;
	private int iterationCount = 0;
	
	/*
	 * GUI field
	 */
	private boolean enableGUI = true;
	private DefaultTableModel newEventModel, executedEventModel;
	private Map<String, JTextArea> targetTextAreas = new HashMap<String, JTextArea>();
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
					confirmArea.append(esPair.toString());
					return super.add(esPair);
				}
			};
			ignoredList = new ArrayList<EventSummaryPair>(){
				@Override
				public boolean add(EventSummaryPair esPair){
					ignoreArea.append(esPair.toString());
					return super.add(esPair);
				}
			};
			targetQueue = new PriorityQueue<EventSummaryPair>(new ESPriority()){
				@Override
				public boolean add(EventSummaryPair esPair){
					if(esPair.getTryCount() >= maxIndividualValidationTry){
						ignoredList.add(esPair);
					}else{
						super.add(esPair);
					}
					updateTargetQueuePane();
					return true;
				}
			};
			validationQueue = new PriorityQueue<EventSummaryPair>(new ESPriority()){
				@Override
				public boolean add(EventSummaryPair esPair){
					if(esPair.getTryCount() >= maxIndividualValidationTry){
						ignoredList.add(esPair);
					}else{
						super.add(esPair);
					}
					updateValidationPane();
					return true;
				}
			};
			
			Logger.registerJPanel("Queue", queuePane);
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
				jsp.setPreferredSize(new Dimension(400, 500));
				
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
		if(isLimitReached()) return Decision.END;
		if(!this.newEventStack.isEmpty()){ 	  return Decision.EXPLORE;
		}else if(decideReachTarget()){ 		  return Decision.REACHTARGET;
		}else if(!validationQueue.isEmpty()){ return Decision.EXPAND;
		}else{ return Decision.END; }
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
		if(toValidateList != null && !toValidateList.isEmpty()){
			for(EventSummaryPair esPair : toValidateList){
				if(this.targets != null && esPair.getSummaryList() != null){
					for(WrappedSummary sum : esPair.getSummaryList()){
						for(String line : this.targets){
							if(sum.executionLog.contains(line)){
								esPair.targetLines.add(line);
							}
						}
					}
				}
				if(esPair.targetLines.size() > 0){ this.targetQueue.add(esPair);
				}else{ validationQueue.add(esPair); }
			}
		}

		//An event summary is ignored due to the incompleteness of summary list
		//the current esPair is null iff it was an exploration operation
		if(this.currentESPair != null && !this.currentESPair.isIgnored()){
			if(this.currentESPair.isIgnored()){
				
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
			}
		}
		
		//Check the actual event summary which occurred. 
		EventSummaryPair actual = this.operater.getLastExecutedEvent();
		//the actual esPair is null if reposition failure during exploration or 
		//solve for event failure.
		if(actual != null){ checkTargetReachablility(actual); }
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
	}

	/**Miscellaneous helper**/
	
	/* TODO Under construction section */
	
	private boolean isLimitReached(){
		if(newEventStack == null) return false;
		
		boolean eventLimit = this.newEventStack.isEmpty();
				
		boolean targetLimit =
				(targetQueue == null) ||
				(this.targetQueue.isEmpty()) ||
				(this.targetQueue.peek().getTryCount() > maxIndividualValidationTry);
				
		boolean valLimit = 
				validationQueue == null ||
				validationQueue.isEmpty() ||
				validationQueue.peek().getTryCount() > maxIndividualValidationTry;
		return (eventLimit && targetLimit && valLimit);
	}
	
	/**
	 * Check the execution log in the executed summary if any target line is hit
	 * @param executed
	 */
	private boolean checkTargetReachablility(EventSummaryPair executed){
		if(isTargetSet() == false) return false;
		if(!executed.isConcreateExecuted()) throw new AssertionError();
		
		if(executed.getTryCount() == 1){ // newly executed event summary pair
			newConcreteExecution+=1;
			List<WrappedSummary> sumList = executed.getSummaryList();
			if(sumList == null || sumList.isEmpty()) return false;
			List<Event> sequence = this.operater.getLatestSequence();
			if(sequence == null) throw new AssertionError();
			for(WrappedSummary sum : sumList){
				if(sum == null || sum.executionLog == null) continue;
				for(String targetLine : targets){//check any target line is hit
					if(sum.executionLog.contains(targetLine)){
						onTargetLineReached(targetLine, sequence);
					}
				}
			}
		}else{ //executed more than one times
			//don't care
		}
		return false;
	}

	private boolean isTargetSet(){
		return this.targets != null;
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
	
	private void onTargetLineReached(String line, List<Event> sequence){
		boolean wasReached = this.reachedTargets.get(line);
		if(!wasReached){
			reachedTargets.put(line, true);
			transferBetweenQueue();

			JTextArea area = targetTextAreas.get(line);
			area.append(sequence.toString());
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
		}
	}
}
