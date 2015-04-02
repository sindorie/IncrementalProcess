package PlabAmodule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.tree.DefaultMutableTreeNode;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedSubgraph;
import org.jgrapht.graph.ListenableDirectedGraph;

import analysis.Expression;
import components.Event;
import components.EventFactory;
import components.EventSummaryPair;
import components.GraphicalLayout;
import components.LayoutNode;
import components.PathFindingHelper;
import components.UISymbolicPair;

public class UIModel {
	ListenableDirectedGraph<UISymbolicPair, EventSummaryPair> directedGraph 
		= new ListenableDirectedGraph<UISymbolicPair, EventSummaryPair>(EventSummaryPair.class);
	
	UISymbolicPair uisymRoot;
	Map<String, List<UISymbolicPair>> uisymVariances = new HashMap<String, List<UISymbolicPair>>();
	
	Map<String,List<GraphicalLayout>> layoutVariances = new HashMap<String, List<GraphicalLayout>>();
	
	public UISymbolicPair getRoot(){
		return uisymRoot;
	}
	
	/**
	 * The implementation requires a root to start with. 
	 * @param ui	--	rootUI; usually should be the launcher
	 */
	public void defineRootState(UISymbolicPair ui){
		uisymRoot = ui;
	}

	
	
	/**
	 * Plan A.
	 * Check the information provided by UIAutomator. 
	 * 
	 * Plan B.
	 * Information source: 
	 * 1. 	UIAutomator which gives basic information including clickable,
	 * 		scrollable, etc. 
	 * 2.	Existing layout. Comparison with other layout.
	 * Build layout difference tree.
	 * 
	 * @param layout
	 * @return
	 */
	public List<Event> createMoreEvents(GraphicalLayout layout){
		List<GraphicalLayout> glist = layoutVariances.get(layout.getActName());
		if(glist.contains(layout)) return null;
		
		LayoutNode root = layout.getRootNode();
		List<Event> result = new ArrayList<Event>();
		
		List<LayoutNode> queue = new ArrayList<LayoutNode>();
		queue.add(root);
		while(queue.isEmpty() == false){
			LayoutNode current = queue.remove(0);
			boolean clickEvent = false,
					scrollEvent = false,
					inputTextEvent = false;
			
			LayoutNode parent = current.getParent();
			if(parent != null){
				if(parent.checkable){
					result.add(EventFactory.createClickEvent(layout,current));
					clickEvent = true;
				}
				//TODO
			}
			
			if(clickEvent == false && current.clickable){
				result.add(EventFactory.createClickEvent(layout,current));
			}
			//TODO
			
			for(int i=0;i<current.getChildCount();i++){
				LayoutNode child = current.getChildAt(i);
				queue.add(child);
			}
		}
		return result;
	}
	
	/**
	 * Add edge between two nodes. 
	 * 
	 * @param source	--	source node
	 * @param event		--	edge
	 * @param dest		--	destination node
	 */
	public void update(UISymbolicPair source, EventSummaryPair event, UISymbolicPair dest){
		if(directedGraph.containsVertex(dest) == false){
			directedGraph.addVertex(dest);
		}	
		if(source.equals(dest)){
			source.addIneffectiveEvent(event);
		}else{
			directedGraph.addEdge(source, dest, event);
		}
	}
	
	/**
	 * Perform a breath search from the starting point until target layout is encountered. 
	 * List is null or empty if no such path exists.
	 * The first element in the list starts form the starting point.
	 * 
	 * @param startingPoint
	 * @param target
	 */
	public List<EventSummaryPair> findPathToLayout(UISymbolicPair startingPoint, GraphicalLayout target){
		if(!this.directedGraph.containsVertex(startingPoint)){
			return null;
		}
		List<UISymbolicPair> uisymList = uisymVariances.get(target.getActName());
		if(uisymList == null) return null;
		for(int i=0;i<uisymList.size();i++){
			UISymbolicPair current = uisymList.get(i);
			if(current.getLayout().equals(target)){
				return DijkstraShortestPath.findPathBetween(this.directedGraph, startingPoint, current);
			}
		}
		return null;
	}
	
	/**
	 * Find a path from the startingPoint. Null is returned if startingPoint does not exists 
	 * or no path could be found. 
	 * @param event
	 * @param startingPoint
	 * @return
	 */
	public List<EventSummaryPair> findConcretePathForEvent(EventSummaryPair event, UISymbolicPair startingPoint){
		//TODO
		return null;
	}
	
//	public List<EventSummaryPair> findConcretePathToState(UISymbolicPair uisPair){
//		//TODO
//		return null;
//	}
	
	public List<List<EventSummaryPair>> solveForEvent(EventSummaryPair esPair){
		return PathFindingHelper.solveForEvent(this.directedGraph, esPair);
	}
	
}
