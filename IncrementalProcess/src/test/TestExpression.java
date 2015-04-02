package test;

import symbolic.Expression;

public class TestExpression {

	public static void main(String[] args) {
		Expression root = new Expression("=");
		Expression x = new Expression("x");
		Expression y = new Expression("y");
		Expression z = new Expression("z");
		
		root.add(x);
		root.add(y);
		
		System.out.println(root.toYicesStatement());
		
		root.replace(y, z);
		System.out.println(root.toYicesStatement());
	}

}
