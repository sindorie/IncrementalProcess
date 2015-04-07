package PlanBModule;

import PlanBModule.AbstractManager.Decision;
import support.Logger;
import components.Event;
import components.EventSummaryPair;

public class ExuectionLoop implements Runnable{
	private UIModel model;
	private AbstractManager manager;
	private AbstractOperation operation;
	private CheckCallBack callBack;
	private boolean working = true;
	private int iterationCount;
	private long startTime = -1;
	private int maxCount = -1;
	
	public ExuectionLoop(
			AbstractManager manager, 
			AbstractOperation operation,
			UIModel model ){
		this.manager = manager;
		this.operation = operation;
		this.model = model;
		
		manager.setOperater(operation);
		operation.setManager(manager);
	}
	
	@Override
	public void run() {
		this.startTime = System.currentTimeMillis();
		manager.onPreparation();
		operation.onPreparation();
		
		while(working){
			if(callBack != null){ callBack.onIterationStart(this); }
			manager.onIterationStepStart();
			Decision nextOperation = manager.decideOperation();
			if(callBack != null){ callBack.onDecisionMade(this, nextOperation); }
			
			if(nextOperation.equals(Decision.EXPLORE)){
				manager.onExplorationStepStart();
				Event nextEvent = manager.getNextExplorationEvent();
				operation.onExplorationProcess(nextEvent);
				manager.onExplorationStepEnd();
			}else if(nextOperation.equals(Decision.EXPAND)){
				manager.onExpansionStepStart();
				EventSummaryPair esPair = manager.getNextExpansionEvent();
				operation.onExpansionProcess(esPair);
				manager.onExplorationStepEnd();
			}else if(nextOperation.equals(Decision.REACHTARGET)){
				manager.onReachTargetStart();
				EventSummaryPair target = manager.getNextTargetSummary();
				operation.onExpansionProcess(target);
				manager.onReachTargetEnd();
			}else if(nextOperation.equals(Decision.END)){
				working = false;
			}
			if(callBack != null){ callBack.onOperationFinish(this); }
			manager.onIterationStepEnd();
			iterationCount += 1;
			
			if(maxCount > 0 && iterationCount > maxCount) break;
		}
		
		operation.onFinish();
		manager.onFinish();
	}

	public AbstractManager getManager() { return manager; }
	public AbstractOperation getOperation() { return operation; }
	public UIModel getModel() { return model; }
	public boolean isWorking() { return working; }
	public void setWorking(boolean working) { this.working = working; }
	public void setCheckCallBack(CheckCallBack callBack){ this.callBack = callBack; }
	public int getIteerationCount(){ return this.iterationCount;}
	public long getStartTime(){ return this.startTime; }
	public void setMaxIteration(int max){ maxCount = max; }
	
	public interface CheckCallBack{
		public boolean onIterationStart(ExuectionLoop loop);
		public void onDecisionMade(ExuectionLoop loop, Decision nextOperation);
		public void onOperationFinish(ExuectionLoop loop);
	}
}
