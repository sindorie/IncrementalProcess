package PlanBModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.jgraph.JGraph;
import org.jgraph.event.GraphSelectionEvent;
import org.jgraph.event.GraphSelectionListener;
import org.jgraph.graph.AttributeMap.SerializableRectangle2D;
import org.jgraph.graph.DefaultGraphCell;
import org.jgraph.graph.GraphConstants;
import org.jgrapht.alg.DijkstraShortestPath;
import org.jgrapht.ext.JGraphModelAdapter;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.ListenableDirectedGraph;

import android.view.KeyEvent;
import support.Logger;
import support.TreeUtility;
import support.TreeUtility.Searcher;
import support.UIUtility;
import components.Event;
import components.EventFactory;
import components.EventSummaryPair;
import components.GraphicalLayout;
import components.LayoutNode;

//extends AbstractModel<GraphicalLayout, EventSummaryPair, Event>
public class UIModel {
	private boolean enableGUI = true;
	
	private Map<String,List<GraphicalLayout>> actLayouts = new HashMap<String,List<GraphicalLayout>>();
	private ListenableDirectedGraph<GraphicalLayout, EventSummaryPair> graph;
	private JGraphModelAdapter<GraphicalLayout, EventSummaryPair> adapter;
	private JGraph jgraph;
	
	private Map<GraphicalLayout, List<EventSummaryPair>> vertex_to_loopEdges = new HashMap<GraphicalLayout, List<EventSummaryPair>>();
	private List<Event> eventBuffer;
	private GraphicalLayout root;
	private List<EventSummaryPair> edgesReference = new ArrayList<EventSummaryPair>();
	private Map<EventSummaryPair, SequenceStatus> solvedSummaryRecord = new HashMap<EventSummaryPair, SequenceStatus>();
	private int LAYOUT_MAX_AMOUNT = 25;
	
	public UIModel() {
		graph = new ListenableDirectedGraph<GraphicalLayout, EventSummaryPair>(EventSummaryPair.class);
		
		
		if(enableGUI){
			adapter = new JGraphModelAdapter<GraphicalLayout, EventSummaryPair>(graph);
			jgraph = new JGraph(adapter);
			jgraph.setEditable(false);
			
			final JTree tree = new JTree();
			final JTextArea detailPanel = new JTextArea();
			final JScrollPane detailContainer = new JScrollPane();
			final JScrollPane treeDetailContainer = new JScrollPane();
			final JScrollPane leftGraphContainer = new JScrollPane();
			final DefaultTreeModel treeModel = new DefaultTreeModel(null);
			final JSplitPane rightSpliter = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
			final JSplitPane masterSpliter = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
			
			tree.setModel(treeModel);
			detailPanel.setEditable(false);
			detailContainer.setViewportView(detailPanel);
			leftGraphContainer.setViewportView(jgraph);
			treeDetailContainer.setViewportView(tree);
			rightSpliter.setTopComponent(treeDetailContainer);
			rightSpliter.setBottomComponent(detailContainer);
			
			masterSpliter.setRightComponent(rightSpliter);
			masterSpliter.setLeftComponent(leftGraphContainer);
			masterSpliter.setDividerLocation(800);
			masterSpliter.setDividerSize(3);
			
			jgraph.addGraphSelectionListener(new GraphSelectionListener(){
				@Override
				public void valueChanged(GraphSelectionEvent event) {
					Object eventCell = event.getCell();
					if(eventCell instanceof DefaultGraphCell){
						Object content = ((DefaultGraphCell) eventCell).getUserObject();
						if(content instanceof DefaultEdge){
							EventSummaryPair edge = (EventSummaryPair)content;
							detailPanel.setText(edge.toFormatedString());
						}else if(content instanceof GraphicalLayout){
							GraphicalLayout layout = (GraphicalLayout)content;
							treeModel.setRoot(layout.getRootNode()); 
						}else {
							Logger.trace("unkown content: "+content.getClass());
						}
					}else{
						Logger.trace("unkown class: "+eventCell.getClass());
					}
				}
			});
			
			tree.addTreeSelectionListener(new TreeSelectionListener(){ 
				@Override
				public void valueChanged(TreeSelectionEvent e) {
					TreePath path = e.getPath();
					LayoutNode node = (LayoutNode) path.getLastPathComponent();
					detailPanel.setText(node.toFormatedString());
				} 
			});
			
			Logger.registerJPanel("UI model", masterSpliter);
		}
	}
	
	public List<EventSummaryPair> findSequence(GraphicalLayout source,GraphicalLayout dest) {
		return DijkstraShortestPath.findPathBetween(this.graph, source, dest);
	}

	/**
	 * get a sequence for the given event summary pair
	 * will first check if there is more sequence
	 * otherwise solve
	 * 
	 * @param edge
	 * @return
	 */
	public List<Event> solveForEvent(EventSummaryPair edge) {
		Logger.trace(edge);
		SequenceStatus list = solvedSummaryRecord.get(edge);
		if(list == null){
			AnchorSolver aSolver = new AnchorSolver(this);
			aSolver.solve(edge);
			List<List<Event>> eSeq = aSolver.getResult();
			if(eSeq == null || eSeq.isEmpty()){
				return null;
			}
			list = new SequenceStatus(eSeq, this);
		}else if(list.needUpdate(this)){
			AnchorSolver aSolver = new AnchorSolver(this);
			aSolver.solve(edge);
			List<List<Event>> eSeq = aSolver.getResult();
			list.update(eSeq, this);
		}
		return list.getNext();
	}
	
	public ListenableDirectedGraph<GraphicalLayout, EventSummaryPair> getGraph(){
		return this.graph;
	}

	public void update(EventSummaryPair edge, GraphicalLayout dest) {
		Logger.trace(edge+" to "+dest+" with summary "+edge.getMajorBranch());
		GraphicalLayout source = edge.getEvent().getSource();
		GraphicalLayout resultedLayout = findSameOrAddLayout(dest);
		if(source.equals(resultedLayout)){
			Logger.trace("Loop edge");
			addLoopEdge(source, edge);
			edge.getEvent().setDest(source);
		}else{
			graph.addEdge(source, resultedLayout, edge);
			edge.getEvent().setDest(resultedLayout);
		}
		edgesReference.add(edge);
	}
	
	public List<Event> getAdditionalEvent(){
		List<Event> result = eventBuffer;
		eventBuffer = null;
		return result;
	}
	
	public void defineRoot(GraphicalLayout root){
		this.graph.addVertex(root);
		List<GraphicalLayout> arr = new ArrayList<GraphicalLayout>();
		arr.add(root);
		actLayouts.put(root.getActName(), arr);
	}

	public Set<GraphicalLayout> getVertexSet(){
		return this.graph.vertexSet();
	}
	
	public void addLoopEdge(GraphicalLayout source, EventSummaryPair edge){
		Logger.debug(edge + " on " +source);
		List<EventSummaryPair> loopEdges = vertex_to_loopEdges.get(source);
		if(loopEdges == null){
			loopEdges = new ArrayList<EventSummaryPair>();
			vertex_to_loopEdges.put(source, loopEdges);
		}
		loopEdges.add(edge);
	}
	
	public List<EventSummaryPair> getLoopEdgeList(GraphicalLayout vertex){
		return this.vertex_to_loopEdges.get(vertex);
	}
	
	public List<EventSummaryPair> getAllEdges(){
		return edgesReference;
	}
	
	
	private GraphicalLayout findSameOrAddLayout(GraphicalLayout layout){
		List<GraphicalLayout> layList = actLayouts.get(layout.getActName());
		if(layList != null){
			for(GraphicalLayout lay: layList){
				if(lay.equals(layout)) return lay; //It is in the encountered one
			}
		}
		this.onNewLayoutHelper(layout);
		return layout;
	}
	
	@SuppressWarnings("unchecked")
	private void onNewLayoutHelper(GraphicalLayout layout){
		Logger.trace(layout);
		this.graph.addVertex(layout);
		List<GraphicalLayout> layList = actLayouts.get(layout.getActName());
		if(layList == null){
			layList = new ArrayList<GraphicalLayout>();
			this.actLayouts.put(layout.getActName(),layList);
		}
		layList.add(layout);
		
		//TODO to improve  
		/*
		 * Only the Click events are implemented at this point
		 * 
		 * Click event:
		 * 	a. if node is leaf and clickable
		 * 	b. if the node is first level sub node of a clickable node. 
		 * 	c. if the node is checkable
		 * 
		 * Long click event:
		 * 	a. if the node is elaf and long clickable
		 * 	b. if the node is first level sub node of a long clickable node.
		 * 
		 * Scroll event:
		 * 	a. if the node is scrollable
		 * 
		 * Text input event:
		 * 	a. if the node is focusable
		 * 	b. if the node is focused
		 */
		if(this.actLayouts.values().size() < LAYOUT_MAX_AMOUNT){
			final List<Event> toAdd = new ArrayList<Event>();
			
			/*
			 * generate some basic press event
			 */
			toAdd.add(EventFactory.CreatePressEvent(layout, KeyEvent.KEYCODE_BACK));
			
			/*
			 * generate click event
			 */
			TreeUtility.breathFristSearch(layout.getRootNode(), new Searcher(){
				@Override
				public int check(TreeNode treeNode) {
					if(treeNode == null){
						UIUtility.showTree(treeNode);
					}
					
					LayoutNode node = (LayoutNode)treeNode;
					if(node.isLeaf()){
						if(node.clickable){ toAdd.add(EventFactory.createClickEvent(layout, node)); }
					}else{
						if(node.getParent() != null && node.getParent().clickable){
							toAdd.add(EventFactory.createClickEvent(layout, node));
						}
					}
					return Searcher.NORMAL;
				}
			});
			
			
			eventBuffer = toAdd;
			Logger.trace(toAdd);
		}else{
			Logger.info("Max amount of layouts reached");
		}
		
		int[] pos = getNextPos();
		DefaultGraphCell cell = this.adapter.getVertexCell(layout);
		@SuppressWarnings("rawtypes")
		Map attr = cell.getAttributes();
		SerializableRectangle2D b = (SerializableRectangle2D) GraphConstants.getBounds(attr);
        GraphConstants.setBounds( attr, new SerializableRectangle2D( pos[0], pos[1], b.width, b.height ) );
        @SuppressWarnings("rawtypes")
		Map cellAttr = new HashMap();
        cellAttr.put( cell, attr ); 
        adapter.edit( cellAttr, null, null, null);
        this.jgraph.revalidate();
	}
	
	private static int posIndex = 1;
	private int[] getNextPos(){
		Logger.trace();
		//assume 800 * 600
		if(posIndex >= 25){
			return new int[]{50,50};
		}else{
			int row = posIndex%4;
			int col = posIndex/10;
			posIndex += 1;
			return new int[]{50+row*300, 50+col*300};
		}
	}
	
	public class SequenceStatus{
		public SequenceStatus(List<List<Event>> inputSequence, UIModel model){ 
			for(List<Event> eS : inputSequence){
				if(! sequences.contains(eS)){
					sequences.add(eS);
				}
			}
			vertexAmount = model.getGraph().vertexSet().size();
			edgeAmount = model.getGraph().edgeSet().size();
		}
		public List<List<Event>> sequences = new ArrayList<List<Event>>();
		public int index = 0;
		public int vertexAmount, edgeAmount;
		
		public boolean needUpdate(UIModel model){
			int tmpVertexAmount = model.getGraph().vertexSet().size();
			int tmpEdgeAmount = model.getGraph().edgeSet().size();
			
			return tmpVertexAmount == vertexAmount && tmpEdgeAmount == edgeAmount;
		}
		
		public void update(List<List<Event>> additionSequences, UIModel model){
			if(additionSequences!= null){
				for(List<Event> input : additionSequences){
					if(!sequences.contains(input)){
						sequences.add(input);
					}
				}
			}

			vertexAmount = model.getGraph().vertexSet().size();
			edgeAmount = model.getGraph().edgeSet().size();
		}
		
		public List<Event> getNext(){
			if(sequences != null && index < sequences.size()){
				List<Event> result = sequences.get(index);
				index += 1;
				return result;
			}
			return null;
		}
	}

}
