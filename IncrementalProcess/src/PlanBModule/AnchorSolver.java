package PlanBModule;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.graph.DirectedMaskSubgraph;
import org.jgrapht.graph.MaskFunctor;

import support.CommandLine;
import support.Logger;
import support.TreeUtility;
import support.TreeUtility.Searcher;
import symbolic.Expression;
import symbolic.Variable;
import components.Event;
import components.EventSummaryPair;
import components.GraphicalLayout;
import components.solver.SolverInstance;

public class AnchorSolver {

	boolean debug = false;
	
	private static JTabbedPane jtb;
	static{
		jtb = new JTabbedPane();
		Logger.registerJPanel("Solution", jtb);
	}
	
	private UIModel model;
	private final int maxDepth, maxBandwith;
	public AnchorSolver(UIModel model,int maxDepth, int maxBandWith){
		this.model = model;
		this.maxBandwith = maxBandWith;
		this.maxDepth = maxDepth;
	}
	
	DefaultMutableTreeNode root;
	List<DefaultMutableTreeNode> leaves;
	List<List<Event>> sequenceList = new ArrayList<List<Event>>();
	
	/**
	 * First create a sequence of events which 
	 * @param esPair
	 */
	public void solve(EventSummaryPair esPair){
		Logger.trace(esPair.toString());
		if(debug)CommandLine.requestInput();
		
		root = new DefaultMutableTreeNode();
		leaves = new ArrayList<DefaultMutableTreeNode>();
		Set<Expression> initCumCon = new HashSet<Expression>();
		initCumCon.addAll(esPair.getCombinedConstraint());
		InternalEventPair content = new InternalEventPair(esPair,initCumCon);
		if(debug){
			Logger.trace("Starting cumulative constraints");
			for(Expression expre : esPair.getCombinedConstraint()){
				for(Variable var : expre.getUniqueVarSet()){
					Logger.trace(var);
				}
			}
		}

		root.setUserObject(content);
		leaves.add(root);
		
		locateAnchor();
		findConnecter();
		
		JScrollPane jsp = new JScrollPane();
		jsp.setViewportView(new JTree(root));
		jtb.add(esPair.toString(), jsp);
	}
	
	
	public List<List<Event>> getResult(){
		return sequenceList;
	}
	
	/*****Locate the anchor******/
	
	private void locateAnchor(){
		int depth = 0;
		while(depth < maxDepth && leaves.isEmpty() == false){
			Logger.trace("Depth: "+depth);
			List<List<DefaultMutableTreeNode>> candidatePacks = new ArrayList<List<DefaultMutableTreeNode>>();
			for(DefaultMutableTreeNode leaf : leaves){
				InternalEventPair content = (InternalEventPair) leaf.getUserObject();
				if(content.isComplete) continue; //do not operate on completed node
				if(content.isFailed) continue; // no need to operated on failed node
				
//				Logger.trace("Cumulative constraints Point 1");
				for(Expression expre : content.cumulativeConstraints){
					Logger.trace(expre.getChildCount()+"-> "+expre.toYicesStatement());
					if(expre.getChildCount() == 1){
						throw new AssertionError();
					}
				}
				
				List<InternalEventPair> candidates = findCandidate(content.cumulativeConstraints);
				if(candidates==null || candidates.isEmpty()){
					content.isFailed = true;
					continue;
				}
				
				List<DefaultMutableTreeNode> nodeCandidates = local_prioritize(leaf, candidates);
				candidatePacks.add(nodeCandidates);
				
				Logger.trace("Candidate list");
//				int index = 0;
				for(InternalEventPair in : candidates){
//					Logger.trace("InternalEventPair: "+index);
					for(Expression expre : in.cumulativeConstraints){
						Logger.trace(expre.toYicesStatement());
						if(expre.getChildCount() == 1) throw new AssertionError();
					}
//					index += 1;
				}
				
//				Logger.trace("Cumulative constraints Point 2");
//				for(Expression expre : content.cumulativeConstraints){
//					Logger.trace(expre.toYicesStatement());
//				}
			}
			List<DefaultMutableTreeNode> toAttach = flate(candidatePacks);
			
			for(DefaultMutableTreeNode node : toAttach){
				InternalEventPair pair = (InternalEventPair) node.getUserObject();
				for(Expression expre : pair.cumulativeConstraints){
					if(expre.getChildCount() == 1){
						throw new AssertionError();
					}
				}
			}
			
			
			if(toAttach== null || toAttach.isEmpty()){break;}
			updateLeaves(toAttach);
			depth += 1;
			
			if(debug)CommandLine.requestInput();
		}
	}
	
	/**
	 * Find the canddiate for the current cumulative constraints
	 * @param cumulativeConstraint
	 * @return
	 */
	private List<InternalEventPair> findCandidate(Set<Expression> cumulativeConstraint){
		List<InternalEventPair> result = new ArrayList<InternalEventPair>();
		List<EventSummaryPair> esList = model.getAllEdges();
		
		if(debug){
			CommandLine.requestInput();
			Logger.trace("Cumulative Constraint");
			for(Expression expre : cumulativeConstraint){
				Logger.trace(expre.toYicesStatement());
				for(Variable var : expre.getUniqueVarSet()){
					Logger.trace(var);
				}
			}
		}
		
		
		
		for(EventSummaryPair esPair : esList){
			Logger.trace(esPair.getEvent());
			Logger.trace(esPair.getEvent().getSource());
			Logger.trace(esPair.getCombinedConstraint());
			Logger.trace(esPair.getCombinedSymbolic());
			if(debug)CommandLine.requestInput();
			
			Set<Expression> updated = isRelatedAndSatisfiable(cumulativeConstraint , esPair);
			if(updated != null){
				InternalEventPair toAdd = new InternalEventPair(esPair, updated);
				toAdd.isComplete = isComplete(updated);
				result.add(toAdd);
				if(debug){
					Logger.trace("updated: ");
					for(Expression expre : updated){ Logger.trace(expre.toYicesStatement()); }
				}
			}
		}
		return result;
	}

	/**
	 * 1. related in terms of variable.
	 * 2. modification on variable should result change
	 * @param cumulativeSym -- the current cumulative 
	 * @param candidate -- the 	
	 * @return
	 */
	private Set<Expression> isRelatedAndSatisfiable(Set<Expression> cumulativeConstraint, EventSummaryPair esPair){
		
//		Logger.trace("Vars in Cumulative Constraint");
//		for(Expression expre : cumulativeConstraint){
//			Logger.trace(expre.toYicesStatement());
//			for(Variable var : expre.getUniqueVarSet()){
//				Logger.trace(var);
//			}
//		}
//		Logger.trace("Event: "+esPair.getEvent());
		
		Map<Expression,Expression> symCandidate = esPair.getCombinedSymbolic();
		List<Expression> additionalCumstraint = esPair.getCombinedConstraint();
		
		if(debug){
			for(Entry<Expression,Expression> entry : symCandidate.entrySet()){
				Logger.trace(entry.getKey()+" : "+entry.getValue());
			}
			for(Expression expre : additionalCumstraint){
				Logger.trace(expre.toYicesStatement());
			}
		}

		
		boolean related = false;
		//if any variable in the given symbolic states is in the cumulative constraint
		for(Expression var : symCandidate.keySet()){
			for(Expression expre:cumulativeConstraint){
				if(expre.contains(var)){
					related = true;
					break;
				}
			}
		}
		
		Logger.trace("Related: "+related);
		
		if(related){
			//replace the variable
			Set<Expression> conResult = new HashSet<Expression>();		
			for(Expression cons : cumulativeConstraint){
				Expression copy = cons.clone();
				for(Expression var : symCandidate.keySet()){
					Expression value = symCandidate.get(var);
//					if(value.getContent().trim().isEmpty()) throw new AssertionError();
					copy.replace(var, value.clone());
				}
				conResult.add(copy);
				
				if(copy.getChildCount() == 1){
					Logger.trace(copy.toYicesStatement());
					throw new AssertionError();
				}
			}
			//add on the constraints in the path summary
			if(additionalCumstraint!= null && !additionalCumstraint.isEmpty()){
				for(Expression expre : additionalCumstraint){
					if(expre.getChildCount() == 1){
						Logger.trace(expre.toYicesStatement());
						throw new AssertionError();
					}
				}
				
				conResult.addAll(additionalCumstraint);
			}
			
			//check satisifiable
			List<String> statements = new ArrayList<String>();
			//add variable define statement
			for(Expression con : conResult){
				for(Variable var : con.getUniqueVarSet()){
					String varDef = var.toVariableDefStatement();
					if(!statements.contains(varDef)){statements.add(varDef);}
				}
			}
			
			//add constraint statement
			for(Expression con : conResult){
				statements.add(Expression.createAssertion(con.toYicesStatement()));
			}
			
			statements.add("(check)\n");
			
			boolean satifiable = SolverInstance.solve(statements.toArray(new String[0]));
			Logger.trace("satifiable: "+satifiable);
			if(satifiable) return conResult;
		}
		return null;
	}
	
	/**
	 * Check if all variables are solved
	 * @param constraints
	 * @return
	 */
	private boolean isComplete(Set<Expression> constraints){
		//TODO to improve 
		for(Expression expre : constraints){
			if(!expre.getUniqueVarSet().isEmpty()){
				return false;
			}
		}
		Logger.trace("IsComplete");
		return true;
	}
	
	/**
	 * e.g. if "+" operation happens at parent level, then "-" then has a lower priority
	 * + vs -; * vs /
	 * 
	 * @param parent -- allows to check the entire sequence
	 * @param candidates
	 * @return
	 */
	private List<DefaultMutableTreeNode> local_prioritize(DefaultMutableTreeNode parent, List<InternalEventPair> candidates){
		InternalEventPair content = (InternalEventPair) parent.getUserObject();
		EventSummaryPair parentSum = content.esPair;
		
		int[] pri = new int[candidates.size()];
		
		//TODO
		
		List<DefaultMutableTreeNode> result = new ArrayList<DefaultMutableTreeNode>();
		for(InternalEventPair pair : candidates){
			DefaultMutableTreeNode node = new DefaultMutableTreeNode();
			node.setUserObject(pair);
			result.add(node);
			parent.add(node);
		}
		return result;
	}

	/**
	 * Given a list of tree node list, put all the nodes into one list
	 * Taken the max breath into consideration
	 * Use round robin
	 * @param candidatePacks
	 * @return
	 */
	private List<DefaultMutableTreeNode> flate(List<List<DefaultMutableTreeNode>> candidatePacks){
		List<DefaultMutableTreeNode> result = new ArrayList<DefaultMutableTreeNode>();
		if(candidatePacks.size() <= 0){
			return result;
		}
		
		int total = 0;
		for(List<DefaultMutableTreeNode> list : candidatePacks){
			total += list.size();
		}
		int min = Math.min(total, maxBandwith);
		
		int[] localIndexRecord = new int[candidatePacks.size()];
		for(int i = 0;i<localIndexRecord.length;i++){localIndexRecord[i] = 0; }
		for(int count = 0, round = 0; count < min ;count++){
			int localIndex = localIndexRecord[round];
			List<DefaultMutableTreeNode> nodeList = candidatePacks.get(round);
			if(localIndex < nodeList.size()){
				DefaultMutableTreeNode node = candidatePacks.get(round).get(localIndexRecord[round]);
				result.add(node);
				localIndexRecord[round] += 1;
			}else{
				round += 1;
				round %= candidatePacks.size();
			}
		}
		
		return result;
	}
	
	/**
	 * update the leaves
	 * @param newLeaves
	 */
	private void updateLeaves(List<DefaultMutableTreeNode> newLeaves){
		this.leaves = newLeaves;
	}
	
	private class InternalEventPair{
		InternalEventPair(EventSummaryPair esPair, Set<Expression> cumulativeConstraints){
			this.esPair = esPair;
			this.cumulativeConstraints = cumulativeConstraints;
		}
		
		EventSummaryPair esPair;
		Set<Expression> cumulativeConstraints;
		boolean isComplete = false;
		boolean isFailed = false;
		
		public String toString(){
			return esPair.toString();
		}
	}
	
	/**
	 * Use plan one 
	 * find event connect between layouts but ignore the constraint of 
	 */
	private void findConnecter(){
		DefaultMutableTreeNode node = root.getFirstLeaf();
		DefaultMutableTreeNode previous = node;
		do{
			InternalEventPair content = (InternalEventPair) node.getUserObject();
			if(content.isComplete){
				List<Event> inflatedSequence = findConnecterForSequence(node);
				if(!sequenceList.contains(inflatedSequence)){
					sequenceList.add(inflatedSequence);
				}
			}
			previous = node;
			node = node.getNextLeaf();
		}while(node != null && previous != node);
	}
	
	private List<Event> findConnecterForSequence(DefaultMutableTreeNode leaf){
		List<Event> result = new ArrayList<Event>();
		
		List<EventSummaryPair> tmpResult = new ArrayList<EventSummaryPair>();
		DefaultMutableTreeNode node = leaf;
		GraphicalLayout source = GraphicalLayout.Launcher;
		List<Expression> excluded = new ArrayList<Expression>();
		while(node != null){
			InternalEventPair content = (InternalEventPair) node.getUserObject();
			GraphicalLayout currentDest = content.esPair.getEvent().getSource();
			if(source.equals(currentDest)){
				
			}else{
				List<EventSummaryPair>  local_seqeuence = findLocalConnecter(source, currentDest, excluded );
				for(Expression var : content.esPair.getCombinedSymbolic().keySet()){
					if(!excluded.contains(var)){
						excluded.add(var);
					}
				}
				if(local_seqeuence == null || local_seqeuence.isEmpty()) {return null;} // cannot find the connecters between anchors
				tmpResult.addAll(local_seqeuence);
			}
			
			source = content.esPair.getEvent().getDest();
			tmpResult.add(content.esPair);
			node = (DefaultMutableTreeNode) node.getParent();
		}
		
		for(EventSummaryPair esPair : tmpResult){
			result.add(esPair.getEvent());
		}
		return result;
	}
	
	
	private List<EventSummaryPair> findLocalConnecter(GraphicalLayout source, GraphicalLayout dest, 
			final List<Expression> exculded){
		DirectedMaskSubgraph<GraphicalLayout, EventSummaryPair> subgraph =
			new DirectedMaskSubgraph<GraphicalLayout, EventSummaryPair>(this.model.getGraph(),
				new MaskFunctor<GraphicalLayout, EventSummaryPair>(){
				
			@Override
			public boolean isEdgeMasked(EventSummaryPair edge) {
				if(edge.getCombinedSymbolic() != null){
					for(Expression var: edge.getCombinedSymbolic().keySet()){
						if(exculded.contains(var)) return true;
					}
				}
				return false;
			}
			@Override
			public boolean isVertexMasked(GraphicalLayout vertex) {
				return false;
			}
		});
		
		return DijkstraShortestPath.findPathBetween(subgraph, source, dest);
	}
	
	
}	
