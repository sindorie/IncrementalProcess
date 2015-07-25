package APKDetailViewer3;

import java.awt.Color;
import java.awt.Component;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.JFileChooser;
import javax.swing.JList;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import support.TreeUtility;
import symbolic.Blacklist;
import symbolic.Expression;
import symbolic.PathSummary;

public class Showcase {
	private boolean working = false;
	private final Object lock = new Object();
	private ShowcaseFrame frame = new ShowcaseFrame();
	private DataCache storage = new DataCache();
	private StaticApp currentApp;
	private List<StaticClass> clzList;
	private List<StaticMethod> methodList;
	private List<PathSummary> sumList;
	private DefaultComboBoxModel<String> methodModel = new DefaultComboBoxModel<String>();
	private DefaultComboBoxModel<String> classModel = new DefaultComboBoxModel<String>();
	private DefaultComboBoxModel<String> sumModel = new DefaultComboBoxModel<String>();
	private DefaultComboBoxModel<String> smaliModel = new DefaultComboBoxModel<String>();
	private DefaultListModel<String> lineModel = new DefaultListModel<String>();
	private DefaultListModel<Expression> symModel = new DefaultListModel<Expression>();
	private DefaultListModel<Expression> conModel = new DefaultListModel<Expression>();	
	private DefaultListModel<Expression> selectedSymModel = new DefaultListModel<Expression>();
	private DefaultListModel<Expression> selectedConModel = new DefaultListModel<Expression>();
	private List<SolverTester> testers = new ArrayList<SolverTester>();
	
	public Showcase(){
		initControl();
	}
	private void initControl(){
		//Setup the model
		//general
		frame.classBox.setModel(classModel);
		frame.methodBox.setModel(methodModel);
		frame.pathSummaryBox.setModel(sumModel);
		//detail panel
		frame.executionPane.setModel(lineModel);
		frame.smaliClassBox.setModel(smaliModel);
		frame.symbolicList.setModel(symModel);
		frame.constraintList.setModel(conModel);
		//solver panel
		frame.selectedConList.setModel(selectedConModel);
		frame.selectedSymList.setModel(selectedSymModel);
		
		//setup renderer
		//detail panel
		frame.symbolicList.setCellRenderer(new ListCellRenderer<Expression>(){
			@Override
			public Component getListCellRendererComponent(JList<? extends Expression> list, Expression value, int index, boolean isSelected, boolean cellHasFocus) {
				JTree tree = new JTree(value);
				if(cellHasFocus) tree.setBackground(Color.LIGHT_GRAY);
				TreeUtility.expandJTree(tree, -1);
				return tree;
			}
		});
		frame.constraintList.setCellRenderer(new ListCellRenderer<Expression>(){
			@Override
			public Component getListCellRendererComponent(JList<? extends Expression> list, Expression value, int index, boolean isSelected, boolean cellHasFocus) {
				JTree tree = new JTree(value);
				if(cellHasFocus) tree.setBackground(Color.LIGHT_GRAY);
				TreeUtility.expandJTree(tree, -1);
				return tree;
			}
		});
		//solver panel
		frame.selectedConList.setCellRenderer(new ListCellRenderer<Expression>(){
			@Override
			public Component getListCellRendererComponent(JList<? extends Expression> list, Expression value, int index, boolean isSelected, boolean cellHasFocus) {
				JTree tree = new JTree(value);
				if(cellHasFocus) tree.setBackground(Color.LIGHT_GRAY);
				TreeUtility.expandJTree(tree, -1);
				return tree;
			}
		});
		frame.selectedSymList.setCellRenderer(new ListCellRenderer<Expression>(){
			@Override
			public Component getListCellRendererComponent(JList<? extends Expression> list, Expression value, int index, boolean isSelected, boolean cellHasFocus) {
				JTree tree = new JTree(value);
				if(cellHasFocus) tree.setBackground(Color.LIGHT_GRAY);
				TreeUtility.expandJTree(tree, -1);
				return tree;
			}
		});
		
		//setup action listener
		frame.clearMenuItem.addActionListener(new ActionListener(){//clear the local data
			@Override
			public void actionPerformed(ActionEvent e) {
				storage.clearData();
			}
		});
		frame.openButton.addActionListener(new ActionListener(){//open file chooser
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				int returnVal = chooser.showOpenDialog(frame);
				if (returnVal == JFileChooser.APPROVE_OPTION) {
					String path = chooser.getSelectedFile().getAbsolutePath();
					frame.apkPathField.setText(path);
				}
			}
		});
		frame.initButton.addActionListener(new ActionListener(){//init the static analysis
			@Override
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable(){
					@Override
					public void run() {
						String path = frame.apkPathField.getText();
						boolean force = frame.newCheckBox.isSelected();
						if((currentApp = storage.retrieveApp(path, force)) == null){
							message("Analysis failure");
							return;
						}
						clzList = storage.retrieveFilteredClasses();
						SwingUtilities.invokeLater(new Runnable(){
							@Override
							public void run() {
								classModel.removeAllElements();
								if(clzList != null){
									for(StaticClass clz : clzList){
										classModel.addElement(clz.getJavaName());
									}
								}
							}
						});
					}
				}).start();;
			
				
			}
		});
		frame.classBox.addActionListener(new ActionListener(){//action for the class selection - update method combo box 
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = frame.classBox.getSelectedIndex();
				if(index >= 0){
					StaticClass clz = clzList.get(index);
					methodList = clz.getMethods();
					methodModel.removeAllElements();
					for(StaticMethod m : methodList){
						methodModel.addElement(m.getSignature());
					}
				}
			}
		});
		frame.analyzeButton.addActionListener(new ActionListener(){ // symbolic execution starting at a method
			@Override
			public void actionPerformed(ActionEvent e) {
				String signature = frame.methodBox.getSelectedItem().toString();
				if(signature != null){
					sumList = storage.retrievePathSummary(signature);
					sumModel.removeAllElements();
					if(sumList!= null){
						for(int i =0;i<sumList.size();i++){
							sumModel.addElement("Path Summary #"+i);
						}
					}
				}
			}
		});
		frame.pathSummaryBox.addActionListener(new ActionListener(){//upon choose a path summary, update the detail panel
			@Override
			public void actionPerformed(ActionEvent e) {
				updateDetailPane();
			}
		});
		frame.smaliClassBox.addActionListener(new ActionListener(){//show difference class file upon choice
			@Override
			public void actionPerformed(ActionEvent e) {
				int index = frame.smaliClassBox.getSelectedIndex();
				if(index >= 0) frame.chooseTextPane(index, null);
			}
		});
		frame.checkButton.addActionListener(new ActionListener(){ //call the solver
//			final Object resultObjectLock = new Object(); //Do not care now
			@Override
			public void actionPerformed(ActionEvent e) {
				for(final SolverTester st : testers){
					if(st == null) continue;
					new Thread(new Runnable(){
						@Override
						public void run() {
							Object result = st.solve(modelToList(selectedConModel), modelToList(selectedSymModel));
							String msg = result == null ? "null" : result.toString();
							frame.resultArea.setText(msg);
							System.out.println(msg);
						}
					}).start();
				}
			}
		});
		
		//setup mouse listener
		frame.constraintList.addMouseListener(new MouseAdapter(){ //double click on constraint list -> add to selected con list
			long time = -1;
			int pIndex = -1;
			@Override
			public void mouseClicked(MouseEvent e){
				long now = e.getWhen();
				int index = frame.constraintList.getSelectedIndex();
				if(now - time < 300){
					if(index == pIndex && index >= 0){
						Expression expre = frame.constraintList.getSelectedValue();
						selectedConModel.addElement(expre);
					}
				}
				time = now;
				pIndex = index;
			}
		});
		frame.symbolicList.addMouseListener(new MouseAdapter(){ //double click on symbolic list -> add to selected sym list
			long time = -1;
			int pIndex = -1;
			
			@Override
			public void mouseClicked(MouseEvent e){
				long now = e.getWhen();
				int index = frame.symbolicList.getSelectedIndex();
				if(now - time < 300){
					if(index == pIndex && index >= 0){
						Expression expre = frame.symbolicList.getSelectedValue();
						selectedSymModel.addElement(expre);
					}
				}
				time = now;
				pIndex = index;
			}
		});
		frame.selectedConList.addMouseListener(new MouseAdapter(){ //double click to remove selected item
			long time = -1;
			int pIndex = -1;
			@Override
			public void mouseClicked(MouseEvent e){
				long now = e.getWhen();
				int index = frame.selectedConList.getSelectedIndex();
				if(now - time < 300){
					if(index == pIndex && index >= 0){
						selectedConModel.remove(index);
						index = -1;
					}
				}
				time = now;
				pIndex = index;
			}
		});
		frame.selectedSymList.addMouseListener(new MouseAdapter(){//double click to remove selected item
			long time = -1;
			int pIndex = -1;
			@Override
			public void mouseClicked(MouseEvent e){
				long now = e.getWhen();
				int index = frame.selectedSymList.getSelectedIndex();
				if(now - time < 300){
					if(index == pIndex && index >= 0){
						selectedSymModel.remove(index);
						index = -1;
					}
				}
				time = now;
				pIndex = index;
			}
		});
	}
	public void show(){
		java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
            	frame.setVisible(true);
            }
        });
	}
	public void addTester(SolverTester st){
		if(st != null){
			System.out.println("adf");
			testers.add(st);
		}
		System.out.println("adf1");
	}
	
	//Synchronization related
	private boolean startWork(String message){
		synchronized(lock){
			this.message(message);
			if(working) return true;
			else{
				working = true;
				return false;
			}
		}
	}
	private boolean startWork(){
		return startWork(null);
	}
	private void stopWork(String message){
		this.message(message);
		working = false;
	}
	private void stopWork(){
		stopWork(null);
	}
	private void message(String msg){
		if(msg != null) frame.infoLabel.setText(msg);
	}
	
	//detail pane update
	private void updateDetailPane(){
		int index = frame.pathSummaryBox.getSelectedIndex();
		if(index < 0 || this.sumList == null) return;
		PathSummary summary = this.sumList.get(index);
		
		lineModel.removeAllElements();
		smaliModel.removeAllElements();
		frame.clearTextPane();
		
		symModel.clear();
		conModel.clear();
		initSmaliFilePane(summary.getExecutionLog());
		
		for(Expression expre : summary.getPathConditions()){
			conModel.addElement(expre);
		}
		for(Expression expre : summary.getSymbolicStates()){
			symModel.addElement(expre);
		}
		index = frame.smaliClassBox.getSelectedIndex();
		if(index >= 0){
			frame.chooseTextPane(index, null);
		}
	}
	private void initSmaliFilePane(List<String> logs){
		Map<String,MyPair> javaToPair = new HashMap<String,MyPair>();
		for(String log : logs){
			lineModel.addElement(log);
			String[] parts = log.split(":");
			if(parts.length!=2){
				System.out.println("Unexecpted: "+log);
				continue;
			}
			StaticClass clz = this.currentApp.findClassByJavaName(parts[0]);
			if(clz == null){
				System.out.println("Cannot find class: "+clz);
				continue;
			}
			if(Blacklist.methodInBlackList(clz.getDexName())){
				continue;
			}
			
			MyPair pair = javaToPair.get(parts[0]);
			if(pair == null){
				pair = new MyPair();
				pair.clz = clz;
				javaToPair.put(parts[0], pair);
				pair.lines.add(".line "+parts[1]);
			}else{
				pair.lines.add(".line "+parts[1]);
			}
		}
		for(Entry<String, MyPair>entry:javaToPair.entrySet()){
			String name = entry.getKey();
			StaticClass clz = entry.getValue().clz;
			String text = storage.retrieveClzText(clz);
			frame.addTextPane(text, entry.getValue().lines);
			smaliModel.addElement(name);
		}
	}
	private class MyPair{
		StaticClass clz;
		List<String> lines = new ArrayList<String>();
	}
	private static List<Expression> modelToList(DefaultListModel<Expression> model){
		List<Expression> result = new ArrayList<Expression>();
		for(int i = 0; i< model.getSize(); i++){
			result.add(model.getElementAt(i));
		}
		return result;
	}
}
