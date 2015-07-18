package APKDetailViewer2;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import staticFamily.StaticApp;
import staticFamily.StaticClass;
import staticFamily.StaticMethod;
import symbolic.Blacklist;
import symbolic.Expression;
import symbolic.PathSummary;

public class Showcase {

	String currentPath;
	StaticApp currentApp;
	List<PathSummary> currentList;
	
	
	ShowcaseFrame frame = new ShowcaseFrame();
	ShowcaseData data = new ShowcaseData();
	final Object lock = new Object();
	boolean working = false;
	
	public Showcase(){
		initControl();
	}
	public void show(){
		frame.setVisible(true);
	}
	
	private void initControl(){
		frame.inspectButton.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent e) {
				System.out.println("inspectButton");
				synchronized(lock){
					if(working) return;
					else startWork("Inspecting");
				}
				
				Thread t = new Thread(new Runnable(){
					@Override public void run(){
						//basic checking
						String url = frame.urlField.getText();
						if(url == null || (url = url.trim()).isEmpty()){
							stopWork("Invalid URL"); return;
						}//basic checking
						File apkFile = new File(url); //file existence checking
						if(!apkFile.exists()){
							stopWork("Invalid URL"); return;
						}
						
						String absPath = apkFile.getAbsolutePath();
						final StaticApp app = data.retrieveApp(absPath);						
						if(app == null){ 
							stopWork("Cannot inspect APK: "+absPath); return; 
						}
						currentApp = app;
						currentPath = absPath;
						SwingUtilities.invokeLater(new Runnable(){
							@Override public void run() {
								//update UI -- clear region, populate class box
								updateClassList();
								updateMethodList();
								stopWork("Finished");
							}
						});
					}
				});
				t.setPriority(Thread.MAX_PRIORITY);
				t.start();
			}
		});

		frame.classBox.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				System.out.println("classBox");
				synchronized(lock){
					if(working) return;
					else startWork();
				}
				
				SwingUtilities.invokeLater(new Runnable(){
					@Override public void run() {
						//update UI -- clear region, populate class box
						updateMethodList();
						stopWork("Finished");
					}
				});
			}
		});
		
		frame.executionButton.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent e) {
				synchronized(lock){
					if(working) return;
					else startWork("Start analysis");
				}
				
				new Thread(new Runnable(){
					@Override public void run() {
						String signature = frame.methodBox.getSelectedItem().toString();
						currentList = data.retrieveSummaries(currentPath, signature);
						SwingUtilities.invokeLater(new Runnable(){
							@Override public void run() {
								updateSummaryList();
								updateDetailPane();
								stopWork("Finished");
							}
						});
					}
				}).start();
			}
		});
		
		frame.summaryBox.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent e) {
				synchronized(lock){
					if(working) return;
					else startWork();
				}
				
				SwingUtilities.invokeLater(new Runnable(){
					@Override public void run() {
						updateDetailPane();
						stopWork("Finished");
					}
				});
			}
		});
		
		frame.smaliClassBox.addActionListener(new ActionListener(){
			@Override public void actionPerformed(ActionEvent e) {
				synchronized(lock){
					if(working) return;
					else startWork();
				}
				final int index = frame.smaliClassBox.getSelectedIndex();
				if(index >= 0){
					SwingUtilities.invokeLater(new Runnable(){
						@Override public void run() {
							frame.chooseTextPane(index, null);
							stopWork("Finished");
						}
					});
				}else{
					stopWork("Finished");
				}
			}
		});
		
		frame.executionPane.addListSelectionListener(new ListSelectionListener(){
			@Override public void valueChanged(ListSelectionEvent e) {
				synchronized(lock){
					if(working) return;
					else startWork();
				}
				SwingUtilities.invokeLater(new Runnable(){
					@Override public void run() {
						String line = frame.executionPane.getSelectedValue();
						if(line != null){
							String[] parts = line.split(":");
							String clzName = parts[0];
							String lineSig = ".line "+parts[1];
							
							int index = frame.smaliModel.getIndexOf(clzName);
							if(index >= 0){
								frame.chooseTextPane(index, lineSig);
								frame.smaliClassBox.setSelectedIndex(index);
							}else{
								System.out.println("Cannot locate tab "+clzName);
							}
						}
						stopWork("Finished");
					}
				});
			}
		});
	}
	
	
	void updateClassList(){
		frame.classModel.removeAllElements();
		frame.methodModel.removeAllElements();
		for(StaticClass clz : currentApp.getClasses()){
			if(symbolic.Blacklist.classInBlackList(clz.getDexName()) == false)
				frame.classModel.addElement(clz.getJavaName());
		}
	}
	
	void updateMethodList(){
		frame.methodModel.removeAllElements();
		Object selectedObj = this.frame.classModel.getSelectedItem();
		if(selectedObj instanceof String){
			String selected = (String)selectedObj;
			StaticClass clz = this.currentApp.findClassByJavaName(selected);
			if(clz == null){
				System.out.println("Cannot find class by: "+selected);
			}else{
				for(StaticMethod method : clz.getMethods()){
					frame.methodModel.addElement(method.getSignature());
				}
			}
		}
	}
	
	void updateSummaryList(){
		frame.summaryModel.removeAllElements();
		if(this.currentList != null){
			int index = 0;
			for(PathSummary sum : this.currentList){
				frame.summaryModel.addElement("Path Summary #"+index);
				index += 1;
			}
		}
	}
	
	
	void updateDetailPane(){
		int index = frame.summaryBox.getSelectedIndex();
		if(index < 0 || this.currentList == null) return;
		PathSummary summary = this.currentList.get(index);
		
		frame.lineModel.removeAllElements();
		frame.smaliModel.removeAllElements();
		frame.clearTextPane();
		
		initSmaliFilePane(summary.getExecutionLog());
		StringBuilder sb = new StringBuilder();
		sb.append("------------- Constraints --------------------\n");
		for(Expression expre : summary.getPathConditions()){
			sb.append(treeToText(expre, 0));
		}

		sb.append("------------- Symbolics ----------------------\n");
		for(Expression expre : summary.getSymbolicStates()){
			sb.append(treeToText(expre, 0));
		}
		
		frame.symbolicPane.setText(sb.toString());
		
		index = frame.smaliClassBox.getSelectedIndex();
		if(index >= 0){
			frame.chooseTextPane(index, null);
		}
	}
	
	void initSmaliFilePane(List<String> logs){
		Map<String,MyPair> javaToPair = new HashMap<String,MyPair>();
		for(String log : logs){
			frame.lineModel.addElement(log);
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
		System.out.println(javaToPair.size());
		for(Entry<String, MyPair>entry:javaToPair.entrySet()){
			String name = entry.getKey();
			frame.smaliModel.addElement(name);
			StaticClass clz = entry.getValue().clz;
			String text = data.retrieveClzText(clz);
			frame.addTextPane(text, entry.getValue().lines);
		}
	}
	
	void startWork(){
		System.out.println("Start working...");
		this.working = true;
	}
	void startWork(String msg){
		System.out.println("Start working...");
		this.working = true;
		this.frame.infoLabel.setText(msg);
	}
	
	void stopWork(String msg){
		System.out.println("Stop working...");
		this.working = false;
		this.frame.infoLabel.setText(msg);
	}
	
	void clear(){
		frame.classModel.removeAllElements();
	}
	
	
	
	private class MyPair{
		StaticClass clz;
		List<String> lines = new ArrayList<String>();
	}
	
	static String treeToText(Expression node, int level){
		StringBuilder sb = new StringBuilder();
		for(int i =0;i<level;i++){
			sb.append("    ");
		}
		sb.append(node.getContent()).append("\n");
		for(int i =0 ;i<node.getChildCount(); i ++){
			sb.append(treeToText((Expression)node.getChildAt(i),level+1));
		}
		return sb.toString();
	}
	
	
	
	public static void main(String[] args) {
		/* Set the Nimbus look and feel */
		try {
			for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
				if ("Nimbus".equals(info.getName())) {
					javax.swing.UIManager.setLookAndFeel(info.getClassName());
					break;
				}
			}
		} catch (ClassNotFoundException ex) {
			java.util.logging.Logger.getLogger(ShowcaseFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (InstantiationException ex) {
			java.util.logging.Logger.getLogger(ShowcaseFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (IllegalAccessException ex) {
			java.util.logging.Logger.getLogger(ShowcaseFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		} catch (javax.swing.UnsupportedLookAndFeelException ex) {
			java.util.logging.Logger.getLogger(ShowcaseFrame.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
		}
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				new Showcase().show();
			}
		});
	}
}
