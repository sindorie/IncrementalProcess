package PlanBModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jgrapht.graph.ListenableDirectedGraph;

import components.solver.ProblemSolver;

public abstract class AbstractModel <V,E,K>{
	protected V root;
	protected ListenableDirectedGraph<V,E> graph;
	
	protected Map<V, List<E>> vertex_to_loopEdges = new HashMap<V, List<E>>();
	
	public AbstractModel(Class<? extends E> edgeClass){
		graph = new ListenableDirectedGraph<V,E>(edgeClass);
	}
	
	public void defineRoot(V root){
		this.root = root;
	}
	
	public V getRoot(){
		return this.root;
	}

	public ListenableDirectedGraph<V,E> getGraph(){
		return this.graph;
	}
	
	public abstract List<E> findSequence(V source, V dest);
	
	public abstract List<E> solveForEdge(E edge, ProblemSolver solver);
	
	public abstract List<K> update(E edge, V dest);
	
	public Set<V> getVertexSet(){
		return this.graph.vertexSet();
	}
	
	public void addLoopEdge(V source, E edge){
		List<E> loopEdges = vertex_to_loopEdges.get(source);
		if(loopEdges == null){
			loopEdges = new ArrayList<E>();
			vertex_to_loopEdges.put(source, loopEdges);
		}
		loopEdges.add(edge);
	}
	
	public List<E> getLoopEdgeList(V vertex){
		return this.vertex_to_loopEdges.get(vertex);
	}

}
