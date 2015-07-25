package APKDetailViewer3;

import java.util.List;

import symbolic.Expression;

public interface SolverTester {

	/**
	 * Check if the symbolics satisfy the constraints.
	 * The list of expressions are directly from path summary in this version.
	 * Therefore they might contains numerical problem which can simply ignored by you.
	 * 
	 * If you have any question, ASK !
	 * 
	 * @param constraints
	 * @param symbolics
	 * @return
	 */
	public Object solve(List<Expression> constraints, List<Expression> symbolics);
}
