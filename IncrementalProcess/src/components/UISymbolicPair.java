package components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import symbolic.Expression;
 

public class UISymbolicPair { 
	private int gIndex = 0;
	public int index = gIndex++ ;
	
	GraphicalLayout layout;
	Map<Expression,Expression> cumulativeSymbolicStates;
	List<EventSummaryPair> ineffectiveEvent = new ArrayList<EventSummaryPair>();
	
	public UISymbolicPair(GraphicalLayout layout){ 
		this.layout = layout;
		this.cumulativeSymbolicStates = new HashMap<Expression,Expression>();
	}
	public UISymbolicPair(GraphicalLayout layout,Map<Expression,Expression> cumulativeSymbolicStates){ 
		this.layout = layout;this.cumulativeSymbolicStates = cumulativeSymbolicStates;
	}
	public void setSymbolic(Map<Expression,Expression> inputs){
		this.cumulativeSymbolicStates.clear();
		this.cumulativeSymbolicStates.putAll(inputs);
	}
	
	public void setGraphicalLayout(GraphicalLayout layout){
		this.layout = layout;
	}
	
	public int overlaySymbolic(Map<Expression,Expression> inputs){
		int count = 0;
		for(Entry<Expression,Expression> entry : inputs.entrySet()){
			Expression previous = cumulativeSymbolicStates.put(entry.getKey(), entry.getValue());
			if(previous == null || !previous.equals(entry.getValue())){
				count += 1;
			}
		}
		return count;
	}	
	
	public void addIneffectiveEvent(EventSummaryPair esPair){
		this.ineffectiveEvent.add(esPair);
	}
	
	public List<EventSummaryPair> getIneffectiveEvent(){
		return this.ineffectiveEvent;
	}
	
	public UISymbolicPair clone(){
		UISymbolicPair result = new UISymbolicPair(this.layout);
		//deep copy for symbolic
		for(Entry<Expression,Expression> entry : this.cumulativeSymbolicStates.entrySet()){
			result.cumulativeSymbolicStates.put(entry.getKey(), entry.getValue().clone());
		}
		return result;
	}
	
	public GraphicalLayout getLayout() {
		return layout;
	}
	public Map<Expression,Expression> getCumulativeSymbolicStates() {
		return cumulativeSymbolicStates;
	}
	
	public boolean equals(Object input){
		if(input instanceof UISymbolicPair){
			UISymbolicPair pair = (UISymbolicPair)input;
			return pair.getLayout().equals(pair.getLayout())
					&&pair.getCumulativeSymbolicStates().equals(this.getCumulativeSymbolicStates());
		}
		return false;
	}
}
