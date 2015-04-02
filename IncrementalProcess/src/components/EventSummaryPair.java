package components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.jgrapht.graph.DefaultEdge;

import symbolic.Expression;
 

public class EventSummaryPair extends DefaultEdge{ 
	
	Event event;
	List<WrappedSummary> summaryList;
	UISymbolicPair source, dest;
	int majorBranch = -1;
	boolean isExecuted = false;
	int tries = 0;
	List<String> methodRoots;
	Map<Expression,Expression> combinedSym;
	List<Expression> combinedCons;
	
	public final static String[] ColumnIdentifier = {
		"Event","Amount","Tries","is Conrete", "Method Roots","Primary Index", 
		"Constraints", "Symbolics","Combined Constraints", "Combined Symbolics"
	};
	
	public String[] toStringArray(){
		List<String> result = new ArrayList<String>();
		result.add(event.toFormatedString());
		result.add(summaryList==null?"0":summaryList.size()+"");
		result.add(tries+"");
		result.add(isExecuted+"");
		if(this.methodRoots != null && !this.methodRoots.isEmpty()){
			StringBuilder sb = new StringBuilder();
			for(String s : this.methodRoots){
				sb.append(s+"\n");
			}
			result.add(sb.toString());
		}else{
			result.add("");
		}
		result.add(majorBranch+"");
		if(summaryList!= null && !summaryList.isEmpty()){
			WrappedSummary major = this.getMajorBranch();
			
			//Constraints
			StringBuilder conSb = new StringBuilder();
			for(Expression expre: major.constraints){
				conSb.append(expre.toYicesStatement()+"\n");
			}
			result.add(conSb.toString());
			//Symbolics
			StringBuilder symSb = new StringBuilder();
			for(Entry<Expression, Expression> entry : major.symbolicStates.entrySet()){
				symSb.append(entry.getKey().toYicesStatement()+" = "+entry.getValue().toYicesStatement()+"\n");
			}
			result.add(symSb.toString());
			
			//Combined Constraints
			StringBuilder combConSb = new StringBuilder();
			for(Expression expre : this.getCombinedConstraint()){
				combConSb.append(expre.toYicesStatement()+"\n");
			}
			result.add(combConSb.toString());
			
			//Combined Symbolics
			StringBuilder combSymSb = new StringBuilder();
			for(Entry<Expression, Expression> entry : this.getCombinedSymbolic().entrySet()){
				combSymSb.append(entry.getKey().toYicesStatement()+" = "+entry.getValue().toYicesStatement()+"\n");
			}
			result.add(combSymSb.toString());
		}else{
			result.add("");
			result.add("");
			result.add("");
			result.add("");
		}

		return result.toArray(new String[0]);
	}
	
	
	public void increateTryCount(){
		tries += 1;
	}
	public int getTryCount(){
		return tries;
	}
	
	public EventSummaryPair(EventResultBundle result){
		this(result.event, result.mappedSummaries, result.majorBranchIndex, result.methodRoots);
	}
	
	public EventSummaryPair(Event event, List<WrappedSummary> summaries, int majorBranch, List<String> methodRoots){
		this.event = event ; 
		if(summaries != null && majorBranch >= 0){
			this.summaryList = new ArrayList<WrappedSummary>(summaries);
			this.majorBranch = majorBranch;
			this.methodRoots = methodRoots;
		}
	}

	public String toFormatedString(){
		StringBuilder sb = new StringBuilder();
		if(event == null){ sb.append("Event: null\n");
		}else{
			sb.append("Event: \n");
			String detail = event.toFormatedString().replace("\n", "\n\t").trim();
			sb.append(detail).append("\n");
		}
		
		if(summaryList==null){
			sb.append("Summary list is empty\n");
		}else{
			int index = 1;
			for(WrappedSummary sum : summaryList){
				if(sum == null) continue;
				sb.append("#"+index+" Summary:\n");
				String detail = sum.toFormatedString().replace("\n", "\n\t").trim();
				sb.append(detail).append("\n");
				index += 1;
			}
		}
		sb.append("Major Branch index: "+majorBranch).append("\n");
		sb.append("Is Concrete: "+this.isExecuted).append("\n");
		sb.append("Tries: "+this.tries).append("\n");
		sb.append("Method Roots: "+methodRoots).append("\n");
		
		combinedSym = this.getCombinedSymbolic();
		combinedCons = this.getCombinedConstraint();
		
		sb.append("Combined Symbolics: \n");
		for(Entry<Expression,Expression> entry : combinedSym.entrySet()){
			sb.append("\t");
			sb.append(entry.getKey().toYicesStatement()).append(" = ");
			sb.append(entry.getValue().toYicesStatement()).append("\n");
		}
		sb.append("Combined Constraints:\n");
		for(Expression expre : combinedCons){
			sb.append("\t").append(expre.toYicesStatement()).append("\n");
		}
		
		return sb.toString();
	}
	
	@Override
	public String toString(){
		return (event == null? "null" : event.toString());// +" with "+(majorBranch < 0? "null" : summaryList.get(majorBranch));
	}
	
	@Override
	public boolean equals(Object o){
		if(o instanceof EventSummaryPair){
			EventSummaryPair other = (EventSummaryPair)o; 
			if(!other.event.equals(this.event)) return false;
			if(other.summaryList == null ^ this.summaryList == null) return false;
			if(other.summaryList != null && !other.summaryList.equals(this.summaryList))  return false;
			return true;
		}
		return false;
	}
	
	@Override
	public int hashCode(){
		return summaryList == null? 0 : summaryList.size();
	}
	
	public Map<Expression,Expression> getCombinedSymbolic(){
		if(combinedSym == null){
			Map<Expression,Expression> result = new HashMap<Expression,Expression>();
			if(summaryList != null){
				for(WrappedSummary sum : summaryList){
					if(sum == null) continue;
					result.putAll(sum.symbolicStates);
				}
			}
			combinedSym = result;
		}
		return combinedSym;
	}
	
	public List<Expression> getCombinedConstraint(){
		if(combinedCons == null){
			List<Expression> result = new ArrayList<Expression>();
			if(summaryList != null){
				for(WrappedSummary sum : summaryList){
					if(sum == null) continue;
					for(Expression con : sum.constraints){
						if(!result.contains(con)){
							result.add(con);
						}
					}
				}
			}
			combinedCons = result;
		}
		return combinedCons;
	}
	
	public void setConcreateExecuted(){
		this.isExecuted = true;
	}
	public boolean isConcreateExecuted(){
		return this.isExecuted;
	}
	
	public List<String> getMethodRoots() {
		return methodRoots;
	}
	public Event getEvent() {
		return event;
	}
	public WrappedSummary getMajorBranch(){
		return summaryList == null ? null : this.summaryList.get(majorBranch);
	}
	public List<WrappedSummary> getSummaryList() {
		return summaryList;
	}
}