package PlanBModule;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Stack;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

public class DepthFirstManager extends AbstractExecutionManger{
	private boolean enableGUI = true;
	private Stack<Event> newEventStack;
	private List<EventSummaryPair> validationQueue;
	private List<EventSummaryPair> targetQueue;
	
	private int periodExpansionTryCount = 0;
	private int expansionPeriod = 5;
	private String[] targets;
	private int target_index = -1;
	private int maxIndividualValidationTry = 5;
	private EventSummaryPair currentESPair;
	private Map<String, Boolean> reachedTargets = new HashMap<String, Boolean>();
	
	private DefaultTableModel newEventModel, executedEventModel, 
			validationModel, targetSummmaryModel, targetModel;

	private static String[] targetModel_columns = {
		"Target Line","is Reached"
	};
	
	public DepthFirstManager(StaticApp app, UIModel model){
		super(app, model);
		if(!enableGUI){
			newEventStack = new Stack<Event>();
			validationQueue = new ArrayList<EventSummaryPair>();
			targetQueue = new ArrayList<EventSummaryPair>();
			
		}else{
			
			final JPanel pane = new JPanel();
			pane.setLayout(new GridLayout(0,1));
			
//			pane.setLayout(new BoxLayout(pane, BoxLayout.Y_AXIS));
			
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
			
			/*Validation Queue*/
			final JTable validtionTable = new JTable();
			final JScrollPane validationContainer = new JScrollPane();
			validationContainer.setViewportView(validtionTable);
			this.validationModel = new DefaultTableModel();
			this.validationModel.setColumnIdentifiers(EventSummaryPair.ColumnIdentifier);
			validtionTable.setModel(validationModel);
			final TitledBorder border_validation = BorderFactory.createTitledBorder(null, "Validation Queue");
			validationContainer.setBorder(border_validation);
			
			/*Target Summary Queue*/
			JTable targetSummaryTable = new JTable();
			final JScrollPane targetSummaryContainer = new JScrollPane();
			targetSummaryContainer.setViewportView(targetSummaryTable);
			this.targetSummmaryModel = new DefaultTableModel();
			this.targetSummmaryModel.setColumnIdentifiers(EventSummaryPair.ColumnIdentifier);
			targetSummaryTable.setModel(targetSummmaryModel);
			final TitledBorder border_info = BorderFactory.createTitledBorder(null, "Target Summary Queue");
			targetSummaryContainer.setBorder(border_info);
			
			/*Targets queue*/
			final JTable targetTable = new JTable();
			final JScrollPane targetContainer = new JScrollPane();
			targetContainer.setViewportView(targetTable);
			this.targetModel = new DefaultTableModel();
			this.targetModel.setColumnIdentifiers(targetModel_columns);
			targetTable.setModel(targetModel);
			final TitledBorder border_target = BorderFactory.createTitledBorder(null, "Target List");
			targetContainer.setBorder(border_target);
			
			pane.add(eventAreaContainer);
			pane.add(exectuedEventContainer);
			pane.add(validationContainer);
			pane.add(targetContainer);
			pane.add(targetSummaryContainer);
			
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
			validationQueue = new ArrayList<EventSummaryPair>(){
				@Override
				public boolean add(EventSummaryPair esPair){
					validationModel.addRow(esPair.toStringArray());
					return super.add(esPair);
				}

				@Override
				public EventSummaryPair remove(int index){
					validationModel.removeRow(index);
					return super.remove(index);
				}
			};
			targetQueue = new ArrayList<EventSummaryPair>(){
				@Override
				public boolean add(EventSummaryPair esPair){
					targetSummmaryModel.addRow(esPair.toStringArray());
					return super.add(esPair);
				}
				@Override
				public EventSummaryPair remove(int index){
					targetSummmaryModel.removeRow(index);
					return super.remove(index);
				}
			};
		}
	}
	
	/**
	 * define a list of targets in the format of String
	 * @param methodSignature
	 */
	public void reachTargets(String... methodSignature){
		targets = methodSignature;
		target_index = 0;
		for(String line : methodSignature){
			reachedTargets.put(line, false);
		}
		
		updateTargetListTable();
	}
	
	@Override
	public void onPreparation() {
		Logger.trace();
		String mainAct = app.getMainActivity().getJavaName();
		String pkgName = app.getPackageName();	
		
		this.model.defineRoot(GraphicalLayout.Launcher);
		this.add(EventFactory.createLaunchEvent(
				GraphicalLayout.Launcher, pkgName, mainAct));
		
		if(this.operater == null || this.model == null) throw new AssertionError();
	}
	
	@Override
	public void onIterationStepStart() {
		//TODO nothing to do at this point 
	}
	
	/**Exploration Mode**/
	
	@Override
	/**
	 * Exploration UI mode occurs when there are new events
	 * @return
	 */
	public boolean isInExplorationMode() {
		boolean result = newEventStack.isEmpty() == false;
		Logger.trace(result+"");
		return result;
	}

	@Override
	public void onExplorationStepStart() {
		Logger.trace();
		// TODO Auto-generated method stub
	}
	
	@Override
	public Event getNextExplorationEvent(){
		return this.newEventStack.pop();
	}

	@Override
	public void onExplorationStepEnd() {
		Logger.trace();
		//check if any new event 
		List<Event> newEvents = model.getAdditionalEvent();
		if(newEvents != null && !newEvents.isEmpty()){
			Logger.info("Added Events");
			for(Event e : newEvents){
				Logger.info(e);
				this.newEventStack.push(e);
			}
		}

		//check if any new event-summary 
		List<EventSummaryPair> toValidateList = operater.getAdditionalValidationEvents();
		if(toValidateList != null && !toValidateList.isEmpty()){ 
			Logger.info("Added Validations: ");
			for(EventSummaryPair exPair : toValidateList){
				Logger.info(exPair.toString());
			}
			for(EventSummaryPair esPair : toValidateList){
				validationQueue.add(esPair);
			}
			
		}
		
		EventSummaryPair last = operater.getLastExecutedEvent();
		checkTargetReachablility(last);
		Logger.trace("Last esPair: "+last);
	}

	
	/**Expansion mode**/
	
	@Override
	/**
	 * Expansion mode occurs when the program 
	 * 1. is not in exploration mode;
	 * 2. is not in reach target mode;
	 * 3. there is at least one event-summary to validate
	 * @return
	 */
	public boolean isInExpansionMpde() {
		boolean result = !isInReachTargetMode() && validationQueue.isEmpty() == false;
		Logger.trace(result+"");
		return result;
//		return false;
	}
	
	@Override
	public void onExpansionStepStart() {
		Logger.trace();
		// TODO Auto-generated method stub
	}
	
	@Override
	public EventSummaryPair getNextExpansionEvent() {
		currentESPair = validationQueue.remove(0);
		return currentESPair;
	}

	@Override
	public void onExpansionStepEnd() {
		if(currentESPair.isConcreateExecuted()){
			validationQueue.add(currentESPair);
		}
		periodExpansionTryCount += 1;
		checkTargetReachablility(operater.getLastExecutedEvent());
		Logger.trace();
	}

	
	/**Reach target mode**/

	@Override
	/**
	 * After any target is encounter, every after a few try 
	 * on the expansion mode and the program is not in exploration
	 * Note: there could be not preset targets.
	 * mode.
	 * @return
	 */
	public boolean isInReachTargetMode() {
		boolean result = target_index >= 0 && !isInExplorationMode() && 
						periodExpansionTryCount >= expansionPeriod;
				
		Logger.trace(result+"");
		return result;
	}
	
	@Override
	public void onReachTargetStart() {
		Logger.trace();
		periodExpansionTryCount = 0;
	}

	@Override
	public EventSummaryPair getNextTargetSummary() {
		return this.targetQueue.remove(0);//TODO
	}
	
	@Override
	public void onReachTargetEnd() {
		checkTargetReachablility(operater.getLastExecutedEvent());
		Logger.trace();
	}
	
	
	/**Finish condition**/

	@Override
	/**
	 * The program is considered finished when
	 * 1. The state is not expansion mode or exploration mode
	 * 	  if there is no preset targets.
	 * 2. All targets are reached 
	 * 3. Some limitation is reached. 
	 * @return
	 */
	public boolean isFinished() {
		if(isLimitReached()){
			return true;
		}else if(this.target_index < 0){
			return !this.isInExpansionMpde() && !this.isInExplorationMode();
		}else{
			return this.target_index >= this.targets.length;
		}
	}


	@Override
	public void onIterationStepEnd() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onFinish() {
		//Nothing to do at this point
	}


	/**Tasks enqueue**/
	@Override
	public void add(Event event) {
		this.newEventStack.push(event);
	}

	@Override
	public void add(EventSummaryPair symbolicSummary) {
		validationQueue.add(symbolicSummary);
	}

	@Override
	public void addAll(List<EventSummaryPair> symbolicSummaryList) {
		for(EventSummaryPair esPair : symbolicSummaryList){
			validationQueue.add(esPair);
		}
		
	}


	/**Miscellaneous helper**/
	
	private boolean isLimitReached(){
		//TODO to define
		return false;
	}
	
	/**
	 * Check the execution log in the executed summary if any target line is hit
	 * @param executed
	 */
	private void checkTargetReachablility(EventSummaryPair executed){
		if(this.target_index < 0) return;
		//check if any summary which is just executed contains any target line
		List<WrappedSummary> sumList = executed.getSummaryList();
		if(sumList == null) return;
		
		boolean anyChange = false;
		for(WrappedSummary sum : sumList){
			for(String targetLine : targets){
				if(sum.executionLog.contains(targetLine)){
					reachedTargets.put(targetLine, true);
					anyChange = true;
				}
			}
		}
		
		//update index_targetline
		int i = this.target_index ;
		for( ; i < this.targets.length ; i++){
			String line = this.targets[target_index];
			if(this.reachedTargets.get(line) == false) break;
			target_index += 1;
		}
		
		if(anyChange){
			updateTargetListTable();
		}
	}
	
	private void updateTargetListTable(){
		if(this.enableGUI){
			for(Entry<String,Boolean> entry : reachedTargets.entrySet()){
				this.targetModel.addRow(new String[]{entry.getKey(),entry.getValue()+""});
			}
		}
	}
	
}
