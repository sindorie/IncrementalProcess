package PlanBModule;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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
public class UIModel{
	private boolean enableGUI = true;
	
	private Map<String,List<GraphicalLayout>> actLayouts = new HashMap<String,List<GraphicalLayout>>();
	private ListenableDirectedGraph<GraphicalLayout, EventSummaryPair> graph;
	private JGraphModelAdapter<GraphicalLayout, EventSummaryPair> adapter;
	private JGraph jgraph;
	
	private Map<GraphicalLayout, List<EventSummaryPair>> vertex_to_loopEdges = new HashMap<GraphicalLayout, List<EventSummaryPair>>();
	private List<Event> eventBuffer;
	private GraphicalLayout root;
	private List<EventSummaryPair> edgesReference = new ArrayList<EventSummaryPair>();
	private Map<String, SequenceStatus> solvedSummaryRecord = new HashMap<String, SequenceStatus>();
	private int LAYOUT_MAX_AMOUNT = 25;
	private int maxDepth = 10, maxBandWith = 10;
	
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
			
			JSplitPane topEdgePane = new JSplitPane();
			JList<EventSummaryPair> edgeList = new JList<EventSummaryPair>();
			JTextArea edgeDetail = new JTextArea();

			DefaultListModel<EventSummaryPair> listModel = new DefaultListModel<EventSummaryPair>();
			edgeList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
			edgeList.setModel(listModel);
			edgeList.addListSelectionListener(new ListSelectionListener(){
				private EventSummaryPair previous = null;
				@Override
				public void valueChanged(ListSelectionEvent e) {
					EventSummaryPair current = edgeList.getSelectedValue();
					if(current != previous){
						if(current!=null){
							edgeDetail.setText(current.toFormatedString());
						}
						previous = current;
					}
				}
			});
			
			edgeDetail.setEditable(false);
			
			JScrollPane edgeListContainer = new JScrollPane();
			edgeListContainer.setViewportView(edgeList);
			JScrollPane edgeDetailContainer = new JScrollPane();
			edgeDetailContainer.setViewportView(edgeDetail);
			topEdgePane.setLeftComponent(edgeListContainer);
			topEdgePane.setRightComponent(edgeDetailContainer);
			
			Logger.registerJPanel("edge", topEdgePane);
			
			edgesReference = new ArrayList<EventSummaryPair>(){
				@Override
				public boolean add(EventSummaryPair element){
					listModel.addElement(element);
					return super.add(element);
				}
			};
		}
	}
	
	public Serializable getDumpObject(){
		ArrayList<Serializable> list = new ArrayList<Serializable>();
		list.add(new HashMap<String,List<GraphicalLayout>>(actLayouts));
		list.add(new ListenableDirectedGraph<GraphicalLayout, EventSummaryPair>(graph));
		list.add(new HashMap<GraphicalLayout, List<EventSummaryPair>>(vertex_to_loopEdges));
		list.add(root);
		list.add(new ArrayList<EventSummaryPair>(edgesReference));
		list.add(new HashMap<String, SequenceStatus>(solvedSummaryRecord));
		return list;
	}
	
	public void restore(Object dumped){
		ArrayList<Serializable> list = (ArrayList<Serializable>)dumped;
		actLayouts = (Map<String, List<GraphicalLayout>>) list.remove(0);
		graph = (ListenableDirectedGraph<GraphicalLayout, EventSummaryPair>) list.remove(0);
		vertex_to_loopEdges = (Map<GraphicalLayout, List<EventSummaryPair>>) list.remove(0);
		root = (GraphicalLayout) list.remove(0);
		edgesReference = (List<EventSummaryPair>) list.remove(0);
		solvedSummaryRecord = (Map<String, SequenceStatus>) list.remove(0);
	}
	
	public Map<String, List<GraphicalLayout>> getActLayouts() {
		return actLayouts;
	}

	public Map<GraphicalLayout, List<EventSummaryPair>> getVertex_to_loopEdges() {
		return vertex_to_loopEdges;
	}

	public GraphicalLayout getRoot() {
		return root;
	}

	public List<EventSummaryPair> getEdgesReference() {
		return edgesReference;
	}

	public Map<String, SequenceStatus> getSolvedSummaryRecord() {
		return solvedSummaryRecord;
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
		String key = edge.getIdentityString();
		SequenceStatus list = solvedSummaryRecord.get(key);
		if(list == null){
			AnchorSolver aSolver = new AnchorSolver(this,maxDepth,maxBandWith);
			aSolver.solve(edge);
			List<List<Event>> eSeq = aSolver.getResult();
			if(eSeq == null){eSeq = new ArrayList<List<Event>>();}
			list = new SequenceStatus(eSeq, this);
			solvedSummaryRecord.put(key, list);
		}else if(list.needUpdate(this)){
			AnchorSolver aSolver = new AnchorSolver(this,maxDepth,maxBandWith);
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
		GraphicalLayout source = edge.getEvent().getSource();
		
		/*Changed*/
		GraphicalLayout resultedLayout = dest; //findSameOrAddLayout(dest);
		
		if(source.equals(resultedLayout)){
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
	
	
	public GraphicalLayout findSameOrAddLayout(String actName, LayoutNode node){
		List<GraphicalLayout> layList = actLayouts.get(actName);
		if(layList != null){
			for(GraphicalLayout lay: layList){
				if(lay.hasTheSmaeLayout(node)) return lay; //It is in the encountered one
			}
		}
		GraphicalLayout newLay = new GraphicalLayout(actName, node);
		this.onNewLayoutHelper(newLay);
		return newLay;
	}
	
//	public GraphicalLayout findLayout(String actName, LayoutNode node){
//		List<GraphicalLayout> layList = actLayouts.get(actName);
//		for(GraphicalLayout lay : layList){
//			if(lay.hasTheSmaeLayout(node)){
//				return lay;
//			}
//		}
//		return null;
//	}
	
//	public GraphicalLayout findSameOrAddLayout(GraphicalLayout layout){
//		List<GraphicalLayout> layList = actLayouts.get(layout.getActName());
//		if(layList != null){
//			for(GraphicalLayout lay: layList){
//				if(lay.equals(layout)) return lay; //It is in the encountered one
//			}
//		}
//		this.onNewLayoutHelper(layout);
//		return layout;
//	}
	
	public void setMaxSolveDimension(int maxDepth, int maxBandwith){
		this.maxDepth = maxDepth;
		this.maxBandWith = maxBandwith;
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
		
		if(layout.getRootNode() == null){
			Logger.trace("Layout root is null");
			return;
		}
		
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
		if(this.actLayouts.values().size() < LAYOUT_MAX_AMOUNT ){
			final List<Event> toAdd = new ArrayList<Event>();
			
			/*
			 * generate some basic press event
			 */
			toAdd.add(EventFactory.CreatePressEvent(layout, KeyEvent.KEYCODE_BACK));
			
			/*
			 * generate click event
			 * TODO improvement: check if the click happens on the same widget
			 */
			TreeUtility.breathFristSearch(layout.getRootNode(), new Searcher(){
				@Override
				public int check(TreeNode treeNode) {
//					if(treeNode == null){ UIUtility.showTree(treeNode); }
					if(treeNode == null) return Searcher.NORMAL;
					LayoutNode node = (LayoutNode)treeNode;
					if(node.isLeaf()){
						if(node.clickable){ 
							Event next = EventFactory.createClickEvent(layout, node);
							if(!toAdd.contains(next)){
								toAdd.add(next); 
							}
						}else if(node.checkable){
							Event next = EventFactory.createClickEvent(layout, node);
							if(!toAdd.contains(next)){
								toAdd.add(next); 
							}
						}
					}else{
						if(node.getParent() != null && node.getParent().clickable){
							Event next = EventFactory.createClickEvent(layout, node);
							if(!toAdd.contains(next)){
								toAdd.add(next); 
							}
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
	
	/*For the GUI cell location*/
	private static int posIndex = 1;
	private int[] getNextPos(){
		if(posIndex >= 25){ return new int[]{50,50};
		}else{
			int row = posIndex%4, col = posIndex/10;
			posIndex += 1;
			return new int[]{50+row*300, 50+col*300};
		}
	}
	
	public class SequenceStatus implements Serializable{
		public List<List<Event>> sequences = new ArrayList<List<Event>>();
		public List<List<Event>> triedSequence = new ArrayList<List<Event>>();
		public int lastRecordVertexAmount, lastRecordEdgeAmount; //index = 0,
		
		public SequenceStatus(List<List<Event>> inputSequence, UIModel model){ 
			for(List<Event> eS : inputSequence){
				if(! sequences.contains(eS)){
					sequences.add(eS);
				}
			}
			lastRecordVertexAmount = model.getGraph().vertexSet().size();
			lastRecordEdgeAmount = model.edgesReference.size();
		}

		/**
		 * Check if there could be an update
		 * @param model
		 * @return
		 */
		public boolean needUpdate(UIModel model){
			int currentVertexAmount = model.getGraph().vertexSet().size();
			int currentEdgeAmount = model.edgesReference.size();
			boolean result =  currentVertexAmount != lastRecordVertexAmount || currentEdgeAmount != lastRecordEdgeAmount;
			return result;
		}
		
		public void update(List<List<Event>> additionSequences, UIModel model){
			if(additionSequences!= null){
				for(List<Event> input : additionSequences){
					if(!sequences.contains(input) && !triedSequence.contains(input)){
						sequences.add(input);
					}
				}
			}
			lastRecordVertexAmount = model.getGraph().vertexSet().size();
			lastRecordEdgeAmount = model.edgesReference.size();
		}
		
		public List<Event> getNext(){
			if(sequences == null || sequences.isEmpty()) return null;
			List<Event> result = sequences.remove(0);
			triedSequence.add(result);
			return result;
		}
	}

}
