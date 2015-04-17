package PlanBModule;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import staticFamily.StaticApp;
import components.Event;
import components.EventSummaryPair;

public abstract class AbstractManager {
	protected StaticApp app;
	protected UIModel model;
	protected AbstractOperation operater;
	
	public AbstractManager(StaticApp app, UIModel model){
		this.app = app;
		this.model = model;
	}
	
	/**
	 * Make the decision
	 * @return
	 */
	public abstract Decision decideOperation();
		
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

	
	public abstract Serializable getDumpData();
	
	public abstract void restore(Object dumped);
	
	public abstract Set<String> getAllHitList();
	
	public void setOperater(AbstractOperation operater){
		this.operater = operater;
	}
	
	public enum Decision{
		EXPLORE, EXPAND, REACHTARGET, END;
	}
}
