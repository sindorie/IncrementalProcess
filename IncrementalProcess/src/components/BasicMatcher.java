package components;

import java.util.List;
import symbolic.PathSummary;

public class BasicMatcher implements LinesSummaryMatcher{
	public int matchSummary(List<String> concreteLines, List<PathSummary> psList){
		int index = 0;
		for (PathSummary ps : psList){
			if (compareBPRecords(concreteLines, ps.getExecutionLog()))
				return index;
			index += 1;
		}
		return -1;
	}
	
	public boolean compareBPRecords(List<String> A, List<String> B){
//		System.out.println(A);
//		System.out.println(B);
		boolean result = true;
		if (A.size() < 1 || B.size() < 1 || A.size() != B.size()){
			result = false;
		}else{
			for (int i = 1, len = A.size(); i < len; i++){
				if (!A.get(i).equals(B.get(i))){
					result = false; break;
				}
			}
		}
//		System.out.println("Comparison: "+ result);
		return result;
	}
}
