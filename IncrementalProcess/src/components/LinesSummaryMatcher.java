package components;

import java.util.List;

import symbolic.PathSummary;
 

public interface LinesSummaryMatcher {
	/**
	 * Match he break point hit line with a summary within the summary list.
	 * @param lineHits
	 * @param summaries
	 * @return the index of the element
	 */
	public int matchSummary(List<String> lineHits, List<PathSummary> summaries);
	
//	/**
//	 * check if the two break point records match with each other 
//	 * @param A
//	 * @param B
//	 * @return
//	 */
//	public boolean compareBPRecords(List<String> A, List<String> B);
}
