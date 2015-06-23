package PlanBModule;

import java.io.File;
import java.io.Serializable;

import main.Paths;
import PlanBModule.AbstractManager.Decision;
import support.Logger;
import support.Utility;
import components.Event;
import components.EventSummaryPair;

public class ExuectionLoop implements Runnable{
	private UIModel model;
	private AbstractManager manager;
	private AbstractOperation operation;
	private CheckCallBack callBack;
	private boolean working = true;
	private int iterationCount = 0, maxCount = -1;
	private long startTime = -1, maxTime = -1, endTime = -1;
//	private String dumpTag;
	
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
		
		try{
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
				
				long currentTime = System.currentTimeMillis();
				if( (maxCount > 0 && iterationCount > maxCount) || /*Iteration Limit*/
					(maxTime > 0 && currentTime - startTime > maxTime) ){ /*Time Limit*/ 
					break;
				}
			}
		}catch(Exception e1){ e1.printStackTrace();Logger.trace(e1.getMessage());
		}catch(Error e){ e.printStackTrace();Logger.trace(e.getMessage()); }
		
		operation.onFinish();
		manager.onFinish();
		
		endTime = System.currentTimeMillis();
		

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
	public void setMaxDuration(long maxDuration){this.maxTime = maxDuration;}
	public long getDuration(){
		return this.endTime - this.startTime;
	}
	
	public interface CheckCallBack{
		public boolean onIterationStart(ExuectionLoop loop);
		public void onDecisionMade(ExuectionLoop loop, Decision nextOperation);
		public void onOperationFinish(ExuectionLoop loop);
	}
}
