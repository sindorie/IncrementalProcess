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
import java.util.List;
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


/**
 * Make sure the uniqueness of event summary pair.
 * 1. the attributes of given event along with the source
 * 2. the executionLog of the summary
 * The combination of the two info should be unique.
 * 
 * @author zhenxu
 *
 */
public class EventSummaryDeposit implements Serializable{
	
	boolean debug = true;
	private DefaultListModel<EventSummaryPair> listModel;
	private JLabel infoLabel = new JLabel();
	public Map<String, List<EventSummaryPair>> data = new HashMap<String, List<EventSummaryPair>>();
	
	public EventSummaryPair findOrConstruct(EventSummaryPair esPair){
		String key = esPair.getIdentityString();
		List<EventSummaryPair> list = data.get(key);
		if(list == null){
			list = new ArrayList<EventSummaryPair>();
			data.put(key, list);
		}
		for(EventSummaryPair es : list){
			if(es.hasExactTheSameExecutionLog(esPair.getSummaryList())){
				return es;
			}
		}
		list.add(esPair);
		listModel.addElement(esPair);
		Logger.trace("Added: "+esPair.toString());
		return esPair;
	}
	public boolean deposit(EventSummaryPair esPair){
		return findOrConstruct(esPair) == esPair;
	}
	
	public boolean contains(EventSummaryPair esPair){
		String key = esPair.getIdentityString();
		List<EventSummaryPair> list = data.get(key);
		if(list != null){
			for(EventSummaryPair es : list){
				if(es.hasExactTheSameExecutionLog(esPair.getSummaryList())){
					return true;
				}
			}
		}
		return false;
	}
	
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
				@Override public void mousePressed(MouseEvent e) {}
				@Override public void mouseReleased(MouseEvent e) {}
				@Override public void mouseEntered(MouseEvent e) { }
				@Override public void mouseExited(MouseEvent e) { }
			});
		}
	}
	
}
