package components.solver;

import java.io.IOException;

public class SolverInstance {
	static YicesProcessInterface yices;
	static{
		try {
			yices = new YicesProcessInterface(YicesProcessInterface.yicesLocation);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	public static boolean solve(String... statements){
		return yices.solve(statements);
	}
}
