package PlanBModule;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.DefaultListModel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import support.Logger;
import symbolic.Expression;
import symbolic.PathSummary;
import components.ExpressionTranfomator;
import components.EventDeposit.InternalPair;

public class Statistic {

	//does not include UIModel now
	public static void probe(DualDeviceOperation operater, DepthFirstManager manager){
		//all possible accessible data
		manager.getNewEventStack();
		manager.getValidationQueue();
		manager.getTargetQueue();
		manager.getIgnoredList();
		manager.getConfirmedList();
		manager.getTargets();
		manager.getReachedTargets();
		manager.getAllHitList();
		manager.getNewEventModel();
		manager.getExecutedEventModel();
		manager.getTargetArea();
		manager.getClassCatergoryPane();
		manager.getTargetArea();
		manager.getValidArea();
		
		operater.getEventDeposit();
		operater.getEventSummaryDeposit();
		operater.getMethodSigToSummaries();
		operater.getAllKnownPathSummaries();
		operater.getAllKnownWrappedSummaries();
		
		System.out.println("Hit size: "+manager.getAllHitList().size());
		
//		manager.getAllHitList().forEach(new Consumer<String>(){//just play around 
//			@Override public void accept(String t) { System.out.println(t); }
//		});
//		List<List<InternalPair>> listOfEvents = operater.getEventDeposit().getRecords();
//		for(List<InternalPair> list : listOfEvents){
//			for(InternalPair ip: list){
//				if(ip.esp == null) System.out.println(ip.e);
//				else System.out.println(ip.esp);
//			}
//		}
		
		DefaultListModel<PathSummary> listModel = operater.getAllKnownPathSummaries();
		int total = 0;
		List<List<Expression>> ignoredList = new ArrayList<List<Expression>>();
		for(int i = 0 ; i < listModel.getSize(); i++){
			PathSummary sum = listModel.getElementAt(i);
			List<Expression> ignored = new ArrayList<Expression>();
			for(Expression expre : sum.getPathConditions()){
				if(ExpressionTranfomator.check(expre) == false){
					ignored.add(expre);
					total += 1;
				}
			}
			ignoredList.add(ignored);
		}
		 
		System.out.println("total ignored: "+total);
		
		JSplitPane spliter = new JSplitPane();
		JScrollPane sumScroll = new JScrollPane();
		JList<PathSummary> sumList = new JList<PathSummary>();
		sumList.setModel(listModel);
		sumScroll.setViewportView(sumList);
		spliter.setLeftComponent(sumScroll);
		sumList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		sumList.addListSelectionListener(new ListSelectionListener(){
			int index = -1;
			@Override
			public void valueChanged(ListSelectionEvent e) {
				int cIndex = sumList.getSelectedIndex();
				if(cIndex >=0 && cIndex != index){
					List<Expression> ignored = ignoredList.get(cIndex);
					if(ignored == null || ignored.isEmpty()){
						spliter.setRightComponent(new JLabel("Empty"));
					}else{ 
						JScrollPane treeTopScroll = new JScrollPane();
						JPanel panel = new JPanel();
						treeTopScroll.setViewportView(panel);
						panel.setLayout(new GridLayout(0,1));
						for(Expression expre : ignored){
							JScrollPane treeScroll = new JScrollPane();
							treeScroll.setViewportView(new JTree(expre));
							treeScroll.setMinimumSize(new Dimension(200,200));
							panel.add(treeScroll);
						}
						spliter.setRightComponent(treeTopScroll);
					}
					index = cIndex;
				}
			}
		});
		Logger.registerJPanel("Ignored Constraint Expression", spliter);
		
		
	}
}
