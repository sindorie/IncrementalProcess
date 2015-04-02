package ignored;

public class Snippet {
//	public static void main(String[] args) {
//		if(enableThreadMonitorPanel){
//					leftModel = new DefaultListModel<String>();
//					leftList = new JList<String>();
//					leftList.setModel(leftModel);
//					final JScrollPane leftListWrapper = new JScrollPane(); // for horizontal scroll -- maybe can skip this
//					leftListWrapper.setViewportView(leftList);
//					
//					final JTextField textField = new JTextField();
//					final JScrollPane rightListWrapper = new JScrollPane(); // for horizontal scroll -- maybe can skip this
//					rightListWrapper.setViewportView(textField);
//					
//					leftList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
//					leftList.addListSelectionListener(new ListSelectionListener(){
//						@Override
//						public void valueChanged(ListSelectionEvent e) {
//							if(e.getValueIsAdjusting() == false){
//								String key = leftList.getSelectedValue();
//								if(key == null) return;
//								
//								List<String> info = threadToLog.get(key);
//								String result = "";
//								if(info == null || info.isEmpty()){
//									
//								}else{
//									StringBuilder sb = new StringBuilder();
//									sb.append("<html>");
//									for(String line : info){
//										if(line == null) continue;
//										String[] parts = line.split("\n");
//										for(String part : parts){
//											if(part.isEmpty()) continue;
//											sb.append(StringEscapeUtils.escapeHtml3(part)).append("<br>");
//										}
//									}
//									sb.append("</html>");
//								}
//								textField.setText(result);
//							}	
//						}
//					});
//					
//					JSplitPane spliter = new JSplitPane();
//					spliter.setDividerLocation(150);
//					spliter.setDividerSize(3);
//					spliter.setLeftComponent(leftListWrapper);
//					spliter.setRightComponent(rightListWrapper);
//					
//					threadLogMonitorFrame.setLayout(new BorderLayout());
//					threadLogMonitorFrame.add(spliter, BorderLayout.CENTER);
//					
//					threadLogMonitorFrame.setSize(800, 600);
//					threadLogMonitorFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//					threadLogMonitorFrame.setVisible(true); 
//					
//					
//					//setup the uncaught exception handler for all Threads
//					Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler(){
//						@Override
//						public void uncaughtException(Thread t, Throwable e) {
//							String key = t.getName()+DELIMITER +t.getId();
//		//					e.get TODO
//							//Check this; 
//							//http://stackoverflow.com/questions/12945537/how-to-set-output-stream-to-textarea
//						}
//					});
//					
//					
//				}else{
//		
//				}
//	}
	//a short sleep wait for jdb process
//	try { Thread.sleep(20); } catch (InterruptedException e1) { }
//	final Thread readThread = new Thread(new Runnable(){
//		@Override
//		public void run() {
//			try {
//				String result;
//				synchronized(stdReader){
//					result = stdReader.readLine();
//				}
//				if(result == null) return;
//				readLog.add(result);
//				
//				
//			} catch (IOException e) {
//				e.printStackTrace();
//				eList.add(e);
//			} catch (Exception e1){
//				e1.printStackTrace();
//			}
//		}
//	});
//	readThread.start(); 
//	try { readThread.join(200);
//	} catch (InterruptedException e) {  e.printStackTrace(); }
//	if(readThread.isAlive()){ readThread.interrupt(); }
}

//WrappedSummary onCreateSum = findBestCandidateForLaunching(mappedSummaryCandidates, methodRoots, "onCreate(");
//if(onCreateSum == null){ 
//	Logger.info("Cannot find onCreate method");
//	Logger.info("Method Root List: "+methodRoots);
//}else{ result.add(onCreateSum); }
//
//WrappedSummary onStartSum = findBestCandidateForLaunching(mappedSummaryCandidates, methodRoots, "onStart(");
//if(onCreateSum == null){ Logger.debug("Cannot find onStart method");
//}else{ result.add(onStartSum); }
//
//WrappedSummary onResumeSum = findBestCandidateForLaunching(mappedSummaryCandidates, methodRoots, "onResume(");
//if(onCreateSum == null){ Logger.debug("Cannot find onResume method");
//}else{ result.add(onResumeSum); }
//
//if(result.size() <= 0){
//	majorBranchIndex = -1;
//}else{
//	majorBranchIndex = 0;
//	mappedSummaries = result; 
//}

