package PlanBModule;

import support.Logger;
import components.Event;
import components.EventSummaryPair;

public class ExuectionLoop implements Runnable{
	private UIModel model;
	private AbstractExecutionManger manager;
	private AbstractExuectionOperation operation;
	private boolean working = true, forzen = false, temp_release = false;

	public ExuectionLoop(
			AbstractExecutionManger manager, 
			AbstractExuectionOperation operation,
			UIModel model ){
		this.manager = manager;
		this.operation = operation;
		this.model = model;
		
		manager.setOperater(operation);
		operation.setManager(manager);
	}
	@Override
	public void run() {
		manager.onPreparation();
		operation.onPreparation();
		
		while(working){
			if(this.forzen){
				Logger.info("Forzen");
				while(this.forzen){
					if(temp_release){
						Logger.info("Relased");
						temp_release = false;
						break;
					}
					
					try { Thread.sleep(500);
					} catch (InterruptedException e) { }
				}
			}

			manager.onIterationStepStart();
			
			if(manager.isInExplorationMode()){
				//the program is exploring the UIs
				manager.onExplorationStepStart();
				Event nextEvent = manager.getNextExplorationEvent();
				operation.onExplorationProcess(nextEvent);
				manager.onExplorationStepEnd();
				
			}else if(manager.isInExpansionMpde()){
				//the program is expanding the knowledge on the existing world
				manager.onExpansionStepStart();
				EventSummaryPair esPair = manager.getNextExpansionEvent();
				operation.onExpansionProcess(esPair);
				manager.onExplorationStepEnd();
				
			}else if(manager.isInReachTargetMode()){
				//the program is trying to reach a target line
				manager.onReachTargetStart();
				EventSummaryPair target = manager.getNextTargetSummary();
				operation.onExpansionProcess(target);
				manager.onReachTargetEnd();
			}
			if(manager.isFinished()){
				working = false;
			}
			manager.onIterationStepEnd();
		}
		
		operation.onFinish();
		manager.onFinish();
	}
	
	public void enableCycleBreak(boolean freeze){
		this.forzen = freeze;
	}

	public AbstractExecutionManger getManager() {
		return manager;
	}
	public AbstractExuectionOperation getOperation() {
		return operation;
	}
	public UIModel getModel() {
		return model;
	}
	public boolean isWorking() {
		return working;
	}
	public void setWorking(boolean working) {
		this.working = working;
	}
	
	public void nextCycle(){
		this.temp_release = true;
	}
}
