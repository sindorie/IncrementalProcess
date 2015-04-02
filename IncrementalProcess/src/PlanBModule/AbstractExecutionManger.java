package PlanBModule;

import java.util.List;

import staticFamily.StaticApp;
import components.Event;
import components.EventSummaryPair;

public abstract class AbstractExecutionManger {
	protected StaticApp app;
	protected UIModel model;
	protected AbstractExuectionOperation operater;
	
	public AbstractExecutionManger(StaticApp app, UIModel model){
		this.app = app;
		this.model = model;
	}
	
	/**
	 * Check if the operation is in the exploration mode
	 * Exploration mode refers to that the program is exploring the UIs. 
	 * @return true if in exploration mode
	 */
	public abstract boolean isInExplorationMode();
	
	/**
	 * Check if the operation is still in expansion mode
	 * Expansion mode refers to that the program is trying to expand the knowledge
	 * of the application under test by concrete execution of a path which is triggered
	 * by an event. 
	 * @return true if in expansion mode
	 */
	public abstract boolean isInExpansionMpde();
	
	/**
	 * Check if the program is trying to reach the target line
	 * @return
	 */
	public abstract boolean isInReachTargetMode();
	
	/**
	 * Check if the operation has ended
	 * @return true if operation is finished.
	 */
	public abstract boolean isFinished();
	
	/**
	 * get the next event for the exploration mode
	 * @return event to be executed
	 */
	public abstract Event getNextExplorationEvent();
	
	/**
	 * get the next event-path pair for expansion mode
	 * @return
	 */
	public abstract EventSummaryPair getNextExpansionEvent();
	
	/**
	 * get the next target event-summary which contains the target line. 
	 * @return target summary
	 */
	public abstract EventSummaryPair getNextTargetSummary();
	
	/**
	 * add events which will be later used in the exploration mode 
	 * @param events to be added
	 */
	public abstract void add(Event event);

	/**
	 * add a event-summary pair which is later used in expansion mode
	 * @param symbolicSummary
	 */
	public abstract void add(EventSummaryPair symbolicSummary);
	
	/**
	 * add a list of event-summary pair which is later used in expansion mode
	 * @param symbolicSummaryList
	 */
	public abstract void addAll(List<EventSummaryPair> symbolicSummaryList);
	
	/**
	 * Prepare for the execution procedure.
	 * Called before iteration starts
	 */
	public abstract void onPreparation();
	
	/**
	 * Terminate whats left
	 * Called after iteration ends
	 */
	public abstract void onFinish();
	
	/**
	 * Called on the starting of one exploration step
	 */
	public abstract void onExplorationStepStart();
	
	/**
	 * Called on the end of one exploration step
	 */
	public abstract void onExplorationStepEnd();
	
	/**
	 * Called on the start of one expansion step
	 */
	public abstract void onExpansionStepStart();
	
	/**
	 * Called on the end of one expansion step
	 */
	public abstract void onExpansionStepEnd();
	
	/**
	 * Called on the start of one reach target step
	 */
	public abstract void onReachTargetStart();
	
	/**
	 * Called on the end of one reach target step
	 */
	public abstract void onReachTargetEnd();
	
	/**
	 * Called on the start of one iteration of the procedure
	 */
	public abstract void onIterationStepStart();
	
	/**
	 * Called on the end of one iteration of the procedure
	 */
	public abstract void onIterationStepEnd();

	
	public void setOperater(AbstractExuectionOperation operater){
		this.operater = operater;
	}
}
