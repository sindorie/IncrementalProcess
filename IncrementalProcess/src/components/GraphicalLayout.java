package components;

import java.util.ArrayList;
import java.util.List;

import support.Logger;

public class GraphicalLayout { 
	
	public final static GraphicalLayout Launcher;
	public final List<Event> candidates = new ArrayList<Event>();
	static{
		Launcher = new GraphicalLayout("Launcher",null);
	}
	private static int gIndex = 0;
	
	String actName;
	LayoutNode layout;
	int index = gIndex++;
	
	public GraphicalLayout(String actName, LayoutNode node){
		this.layout = node;
		this.actName = actName;
	}
		
	public String getActName() {
		return actName;
	}

	public LayoutNode getRootNode() {
		return layout;
	}

	@Override
	public String toString(){
		return this.actName + "_"+index;
	}
	
	@Override
	public boolean equals(Object other){
		if(other instanceof GraphicalLayout){
			GraphicalLayout input = (GraphicalLayout)other;
			if(!input.actName.equals(this.actName))return false;
			if(this.layout != null){
				if(!this.layout.equals(input.layout)){
					return false;
				}
			}else{
				if(input.layout != null) return false;
			}
			return true;
		}
		return false;
	}
	
	public boolean hasTheSmaeLayout(LayoutNode input){
		if( this.layout == null){ return input == null;
		}else{ return this.layout.equals(input); }
	}
	
	@Override
	public int hashCode(){
		return actName.hashCode();
	}
	
}
