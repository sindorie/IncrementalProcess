package components;

import java.util.ArrayList;
import java.util.List;

import support.Logger;

public class EventDeposit { 
	List<List<InternalPair>> records = new ArrayList<List<InternalPair>>();
	List<InternalPair> currentSequence = null;
	final Object simpleLock = new Object();
	
	public void hasReinstalled(){ 
		Logger.trace();
		currentSequence = null;
	}
	
	public void addEvent(Event e){
		if(e.ignoreByRecorder) return;
		addEvent(e,null);
	}
	
	public void addEvent(Event e, EventSummaryPair esPair){
		synchronized(simpleLock){
			Logger.trace(e);
			if(currentSequence == null){
				currentSequence = new ArrayList<InternalPair>();
				records.add(currentSequence);
			}
			InternalPair ip = new InternalPair(e,esPair);
			currentSequence.add(ip);
		}
	}
	
	/**
	 * Find the shortest path to layout among recorded sequences
	 * @param targetLayout
	 * @return
	 */
	public List<Event> findSequenceToLayout(GraphicalLayout targetLayout){
		Logger.trace();
		int length = Integer.MAX_VALUE;
		List<Event> result = null;
		List<InternalPair> choosen = null;
		if(records.size() == 0) return null;
		for(List<InternalPair> ipList : records){
			boolean found = false;
			int count = 0;
			for(InternalPair ip : ipList){
				GraphicalLayout lay = ip.e.getDest();
				if(lay == null) continue;
				if(lay.equals(targetLayout)){
					found = true; break;
				}
				count += 1;
			}
			if(found && count < length){
				choosen = ipList;
				length = count;
			}
		}
		
		if(choosen != null){
			result = new ArrayList<Event>();
			for(InternalPair pair : choosen){
				result.add(pair.e);
				if(pair.e.getDest() != null && pair.e.getDest().equals(targetLayout)) break;
			}
		}	
		return result;
	}
	
	public List<Event> getLastestEventSequnce(){
		if(currentSequence == null) return null;
		List<Event> sequence = new ArrayList<Event>();
		for(InternalPair in : currentSequence){
			sequence.add(in.e);
		}
		return sequence;
	}
	
	
	/**
	 * A wrapper class which acts like an union (in C language) for
	 * Event and EventSummaryPair
	 * @author zhenxu
	 *
	 */
	private class InternalPair{
		InternalPair(Event e, EventSummaryPair esp){
			this.e = e; this.esp = esp;
		};
		public Event e;
		public EventSummaryPair esp;
	}
}
