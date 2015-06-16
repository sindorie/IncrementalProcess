package PlanBModule;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import support.Logger;
import symbolic.Blacklist;
import symbolic.Expression;
import symbolic.PathSummary;
import tools.CoverageStats;
import components.EventDeposit;
import components.EventDeposit.InternalPair;
import components.EventSummaryPair;
import components.ExpressionTranfomator;
import components.WrappedSummary;

public class Statistic {

	// does not include UIModel now
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
		
		System.out.println(
					"Restart times: " + 
					operater.getEventDeposit().getRecords().size()
		);
		
		int maxLength = -1;
		for(List<InternalPair> ipList: operater.getEventDeposit().getRecords()){
			if(ipList.isEmpty()) continue;
			if(ipList.get(0).esp == null){
				if( ipList.size() > maxLength) maxLength = ipList.size();
				for(InternalPair ip : ipList){
					System.out.println(ip.e);
				}
				System.out.println();
			}	
		}
		
		
		System.out.println("-------------- Event Path Pair coverage ----------");
		Map<String, List<EventSummaryPair>> eventSummaryPairs = 
				operater.getEventSummaryDeposit().data;
		int totalConcreteCount = 0, totalPathCount = 0;
		for (Map.Entry<String, List<EventSummaryPair>> entry : eventSummaryPairs.entrySet())
		{
			System.out.print("[EventSummaryPair]" + entry.getKey());
			int concreteCount = 0, pathCount = entry.getValue().size();
			totalPathCount += pathCount;
			for (EventSummaryPair esp : entry.getValue())
			{
				if (esp.isConcreateExecuted())
				{
					concreteCount++;
					totalConcreteCount++;
				}
			}
			System.out.println("\t\t" + concreteCount + "/" + pathCount);
		}
		System.out.println("\n Total Path Coverage: " + totalConcreteCount + "/" + totalPathCount);
		
		System.out.println("\n-------------- Target coverage ---------");
		int reachedCount = 0, totalCount = manager.getReachedTargets().size();
		for (Map.Entry<String, Boolean> entry : manager.getReachedTargets().entrySet())
		{
			System.out.println("[target] " + entry.getKey() + "\t\t" + entry.getValue());
			if (entry.getValue())
				reachedCount++;
		}
		System.out.println("Target coverage: " + reachedCount + "/" + totalCount + "\n");
		
		DefaultListModel<PathSummary> listModel = operater.getAllKnownPathSummaries();
		int total = 0;
		final List<List<Expression>> ignoredList = new ArrayList<List<Expression>>();
		Map<String, Integer> ignoredMap = new HashMap<String, Integer>();
		for(int i = 0 ; i < listModel.getSize(); i++){
			PathSummary sum = listModel.getElementAt(i);
			List<Expression> ignored = new ArrayList<Expression>();
			for(Expression expre : sum.getPathConditions()){
				if(ExpressionTranfomator.check(expre) == false){
					ignored.add(expre);
					total += 1;
					for (int j = 0; j < expre.getChildCount(); j++)
					{
						Expression child = (Expression) expre.getChildAt(j);
						if (child.getContent().equals("$api"))
						{
							Expression apiEx = (Expression) child.getChildAt(0);
							String stmt = apiEx.getContent();
							String methodSig = stmt.substring(stmt.lastIndexOf(" ")+1);
							if (!ignoredMap.containsKey(methodSig))
								ignoredMap.put(methodSig, 1);
							else
								ignoredMap.put(methodSig, ignoredMap.get(methodSig)+1);
						}
					}
				}
			}
			ignoredList.add(ignored);
		}
		
		System.out.println("---------------- Ignored API constraints: "+ignoredMap.size());
		for (Map.Entry<String, Integer> entry : ignoredMap.entrySet())
		{
			System.out.println("[API Signature]" + entry.getKey() + "\t\t" + entry.getValue());
		}
		System.out.println("\n----------------- Bytecode lines hit: "+manager.getAllHitList().size());
		final JSplitPane spliter = new JSplitPane();
		JScrollPane sumScroll = new JScrollPane();
		final JList<PathSummary> sumList = new JList<PathSummary>();
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
		
		
		System.out.println("---------------------------------------");
		
		

		DefaultListModel<PathSummary> totalList = operater.getAllKnownPathSummaries();
		List<PathSummary> concreteList = new ArrayList<PathSummary>();
		Set<List<String>> executeLogSet =new HashSet<List<String>>();
		for (Map.Entry<String, List<EventSummaryPair>> entry : eventSummaryPairs.entrySet()){
			for(EventSummaryPair esPair : entry.getValue()){
				if(esPair.isConcreateExecuted()){
					System.out.println(esPair.toString()+"  :  "+esPair.getMethodRoots());
					List<WrappedSummary> wList = esPair.getSummaryList();
					if(wList == null || wList.size() == 0) continue;
					for(WrappedSummary ws : wList){
						if(ws == null || ws.executionLog ==null || ws.executionLog.isEmpty()) continue;
						if(executeLogSet.add(ws.executionLog)){
							concreteList.add(ws.summaryReference);
						}
					}
				}
			}
		}
		
		EventDeposit deposit = operater.getEventDeposit();
		List<List<InternalPair>>  ilist = deposit.getRecords();
		int maxSequenceLength = -1;
		int maxGeneratedSequenceLength = -1;
		for(List<InternalPair> list : ilist){
			if(list!= null && list.size() > 0){
				InternalPair pair = list.get(0);
				if(pair.esp == null){ //indicates it as a generated sequence
					if( maxGeneratedSequenceLength <= list.size()){
						maxGeneratedSequenceLength = list.size();
					}
				}
				if( maxSequenceLength <= list.size()){
					maxSequenceLength = list.size();
				}
			}
		}

		Map<String, String> targetMap = manager.getTargetReachDetail();
		
		
		Set<String> lineHits = manager.getAllHitList();
		Set<String> allLine = getAllLine(manager.app);
		Set<String>[] checked = checkLineHit(allLine, lineHits);
		
		System.out.println("Path Summary execution (Concrete/All): "+concreteList.size()+"/"+totalList.size());
		System.out.println("Recorded hits: "+ lineHits.size());
		System.out.println("Found in App: "+ checked[0].size());
		System.out.println("Error recorded: "+checked[1].size());
		System.out.println("Total Lines: "+allLine.size());
		System.out.println("Max sequence length: "+maxSequenceLength);
		System.out.println("Max generated sequence length: "+maxGeneratedSequenceLength);
		
		if(targetMap.size() > 0){
			for(Entry<String, String> entry : targetMap.entrySet()){
				System.out.println("Target: "+entry.getKey());
				String val = entry.getValue();
				if(val == null || val.equals("")){
					System.out.println("Failure");
				}else{
					System.out.println(entry.getValue());
				}
			}
		}
		
//		if(checked[1].size() >0){
//			for(String line : checked[1]){
//				System.out.println(line);
//			}
//		}
	}

	public static Set<String>[] checkLineHit(Set<String> allLine,
			Set<String> recorded) {
		Set<String> result[] = new HashSet[2];
		result[0] = new HashSet<String>();
		result[1] = new HashSet<String>();

		for (String line : recorded) {
			if (allLine.contains(line)) {
				result[0].add(line);
			} else {
				result[1].add(line);
			}
		}
		return result;
	}

	public static Set<String> getAllLine(StaticApp staticApp) {
		Set<String> result = new HashSet<String>();
		for (StaticClass c : staticApp.getClasses()) {
			if (Blacklist.classInBlackList(c.getDexName()))
				continue;
			for (StaticMethod m : c.getMethods()) {
				for (int i : m.getSourceLineNumbers()) {
					String thisLine = c.getJavaName() + ":" + i;
					result.add(thisLine);
				}
			}
		}
		return result;
	}

}