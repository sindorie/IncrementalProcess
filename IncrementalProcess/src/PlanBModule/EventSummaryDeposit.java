package PlanBModule;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap; 
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import support.Logger;
import components.Event;
import components.EventSummaryPair;

public class EventSummaryDeposit implements Serializable{
	
	boolean debug = true;
	
	private Map<Event, Set<EventSummaryPair>> deposit = new HashMap<Event,Set<EventSummaryPair> >();
	private DefaultListModel<EventSummaryPair> listModel;
	private JLabel infoLabel = new JLabel();
	
	public EventSummaryDeposit(){
		if(debug){
			JSplitPane topEdgePane = new JSplitPane();
			Container edgeInfoContainer = new Container();
			edgeInfoContainer.setLayout(new BorderLayout());
			
			JList<EventSummaryPair> edgeList = new JList<EventSummaryPair>();
			JTextArea edgeDetail = new JTextArea();
			
			listModel = new DefaultListModel<EventSummaryPair>();
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
			
			edgeInfoContainer.add(edgeListContainer, BorderLayout.CENTER);
			edgeInfoContainer.add(infoLabel, BorderLayout.SOUTH);
			
			topEdgePane.setLeftComponent(edgeInfoContainer);
			topEdgePane.setRightComponent(edgeDetailContainer);
			
			Logger.registerJPanel("es deposit", topEdgePane);
			
			infoLabel.setText("Empty list");
			
			infoLabel.addMouseListener(new MouseListener(){

				@Override
				public void mouseClicked(MouseEvent e) {
					infoLabel.setText("Size: "+listModel.getSize());
				}

				@Override
				public void mousePressed(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void mouseReleased(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void mouseEntered(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void mouseExited(MouseEvent e) {
					// TODO Auto-generated method stub
					
				}
				
			});
		}
	}
	
	
	public Map<Event, Set<EventSummaryPair>> getInternalDeposit(){
		return this.deposit;
	}
	
	
	/**
	 * Deposit the summary
	 * @param esPair
	 */
	public void deposit(EventSummaryPair esPair){
		Event event = esPair.getEvent();
		Set<EventSummaryPair> set = deposit.get(event);
		if(set == null){ 
			set = new MyHashSet(); 
			deposit.put(event, set);
		}
		set.add(esPair);
	}
	
//	public void depositAll(Event event, List<EventSummaryPair> list){
//		Set<EventSummaryPair> set = deposit.get(event);
//		if(set == null){ 
//			set = new HashSet<EventSummaryPair>(); 
//			deposit.put(event, set);
//		}
//		for(EventSummaryPair esPair : list){
//			set.add(esPair);
//		}
//	}
	
	public EventSummaryPair checkAndDeposit(EventSummaryPair esPair){
		Event event = esPair.getEvent();
		Set<EventSummaryPair> set = deposit.get(event);
		if(set == null){ 
			set = new MyHashSet(); 
			deposit.put(event, set);
			set.add(esPair);
			return esPair;
		}else{
			Iterator<EventSummaryPair> iter = set.iterator();
			while(iter.hasNext()){
				EventSummaryPair current = iter.next();
				if(current.equals(esPair)) return current;
			}
			set.add(esPair);
			return esPair;
		}
	}
	
	/**
	 * The result will not be null
	 * @param event
	 * @return
	 */
	public Set<EventSummaryPair> getSet(Event event){
		Set<EventSummaryPair> set = deposit.get(event);
		if(set == null){ 
			set = new MyHashSet(); 
			deposit.put(event, set);
		}
		return set;
	}
	
	/**
	 * check if existence
	 * @param esPair
	 * @return
	 */
	public boolean contains(EventSummaryPair esPair){
		Event event = esPair.getEvent();
		Set<EventSummaryPair> set = deposit.get(event);
		if(set == null){ 
			set = new MyHashSet(); 
			deposit.put(event, set);
			return false;
		}
		return set.contains(esPair);
	}
	
	private class MyHashSet extends HashSet<EventSummaryPair>{
		public boolean add(EventSummaryPair esPair){
			if(debug){ 
				listModel.addElement(esPair); 
				infoLabel.setText("Size: "+listModel.getSize());
			}
			return super.add(esPair);
		}
	}
}
