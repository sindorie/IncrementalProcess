package PlanBModule;

import java.util.List;
import java.util.Set;

import staticFamily.StaticApp;
import components.Event;
import components.EventSummaryPair;

public abstract class AbstractOperation {
	protected StaticApp app;
	protected UIModel model;
	protected AbstractManager manager;
	
	/**
	 * @param app -- the application under testing
	 * @param model -- the model of the application
	 * @param manager -- the manager which decides the next procedure of the program 
	 */
	public AbstractOperation(StaticApp app, UIModel model){ 
		this.app = app;
		this.model = model;
	}
	
	/**
	 * Prepare for the operation.
	 * Called before the iteration.
	 */
	public abstract void onPreparation();
	
	/** 
	 * The operation during the exploration process.
	 * Called during exploration mode. Execute event and collect necessary information
	 * update the model if necessary
	 * @param newEvent -- an event which was tried
	 * @param model -- the UI model
	 */
	public abstract void onExplorationProcess(Event newEvent);
	
	/**
	 * Called during expansion mode. Try to concretely execute event and check the path.
	 * update the model if necessary
	 * @param toValidate -- the event path pair to validate 
	 * @param model -- the UI model
	 */
	public abstract void onExpansionProcess(EventSummaryPair toValidate);
	
	/**
	 * Clear up every thing
	 * Called after entire iterations
	 */
	public abstract void onFinish();
	
	/**
	 * get additional event-summary pairs which needs validation
	 * if there is any
	 * @return
	 */
	public abstract List<EventSummaryPair> getAdditionalValidationEvents();
	
	/**
	 * Called during exploration and expansion mode.
	 * @return
	 */
	public abstract EventSummaryPair getLastExecutedEvent();
	
	/**
	 * Get latest sequence from launching
	 * @return
	 */
	public abstract List<Event> getLatestSequence();
	
	
	public abstract Set<String> getLatestLineHit();
	
	public void setManager(AbstractManager manager){
		this.manager = manager;
	}
	
}
