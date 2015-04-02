package test;

import components.GraphicalLayout;

import PlanBModule.UIModel;

public class TestSolvingProcedure {

	public static void main(String[] args){
		
		UIModel model = new UIModel();
		model.defineRoot(GraphicalLayout.Launcher);
		
		GraphicalLayout act1 = new GraphicalLayout("act1",null);
		
		
		GraphicalLayout act2 = new GraphicalLayout("act2",null);
		GraphicalLayout act3 = new GraphicalLayout("act3",null);
		
	}
	
}
