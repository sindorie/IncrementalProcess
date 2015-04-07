package PlanBModule;

import java.util.Comparator;

import components.EventSummaryPair;

public class ESPriority implements Comparator<EventSummaryPair>{

	/**
	 * Calculate the priority of input
	 * Less tries, more syms, less constraints, more targets
	 * 
	 * @param esPair
	 * @return
	 */
	public static int calculate(EventSummaryPair esPair){
		int tryCount = esPair.getTryCount();
		int symCount = esPair.getCombinedSymbolic().size();
		int conCount = esPair.getCombinedConstraint().size();
//		int tarCount = esPair.targetLines.size();
		return -tryCount + symCount - conCount;
	}

	@Override
	public int compare(EventSummaryPair o1, EventSummaryPair o2) {
		return calculate(o2) - calculate(o1);
	}
}
