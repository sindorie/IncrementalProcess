package APKDetailViewer2;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.StyledDocument;

/**
*
* @author zhenxu
*/
public class ShowcaseFrame extends javax.swing.JFrame {

   /**
    * Creates new form NewJFrame1
    */
   public ShowcaseFrame() {
       initComponents();
   }

   /**
    * This method is called from within the constructor to initialize the form.
    * WARNING: Do NOT modify this code. The content of this method is always
    * regenerated by the Form Editor.
    */
   @SuppressWarnings("unchecked")
   // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
   private void initComponents() {

       jLabel1 = new javax.swing.JLabel();
       jLabel2 = new javax.swing.JLabel();
       jLabel3 = new javax.swing.JLabel();
       jLabel5 = new javax.swing.JLabel();
       jLabel6 = new javax.swing.JLabel();
       smaliPanel = new javax.swing.JPanel();
       jSplitPane1 = new javax.swing.JSplitPane();
       jSplitPane2 = new javax.swing.JSplitPane();
       jScrollPane1 = new javax.swing.JScrollPane(); 
       jScrollPane2 = new javax.swing.JScrollPane();
       
       urlField = new javax.swing.JTextField();
       inspectButton = new javax.swing.JButton();
       executionButton = new javax.swing.JButton();
       methodBox = new javax.swing.JComboBox<String>();
       classBox = new javax.swing.JComboBox<String>();
       summaryBox = new javax.swing.JComboBox<String>();
       smaliClassBox = new javax.swing.JComboBox<String>();
       executionPane = new javax.swing.JList<String>();
       detailPane = new javax.swing.JPanel();
       symbolicPane = new javax.swing.JTextArea();
       smaliScroll = new javax.swing.JScrollPane();
       infoLabel = new javax.swing.JLabel();
       
       classModel = new javax.swing.DefaultComboBoxModel<String>();
       methodModel = new javax.swing.DefaultComboBoxModel<String>();
       summaryModel = new javax.swing.DefaultComboBoxModel<String>();
       smaliModel = new javax.swing.DefaultComboBoxModel<String>();
       lineModel = new javax.swing.DefaultListModel<String>();
       
       methodBox.setModel(methodModel);
       classBox.setModel(classModel);
       summaryBox.setModel(summaryModel);
       smaliClassBox.setModel(smaliModel);
       executionPane.setModel(lineModel);       
       
       setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);//javax.swing.WindowConstants.EXIT_ON_CLOSE)
       setTitle("APK Detail Viewer");

       jLabel1.setText("  EnterURL: ");
       inspectButton.setText("Inspect");
       jLabel5.setText("  Class:");
       jLabel5.setPreferredSize(new java.awt.Dimension(46, 20));
       jLabel6.setText("  Method:");
       jLabel6.setPreferredSize(new java.awt.Dimension(46, 20));
       jLabel2.setText("  Path:");
       jSplitPane1.setResizeWeight(0.2);
       jSplitPane2.setResizeWeight(0.5);
       jSplitPane2.setToolTipText("");

       jScrollPane2.setBorder(javax.swing.BorderFactory.createTitledBorder("Constraint Symbolic Detail"));
       symbolicPane.setColumns(20);
       symbolicPane.setRows(5);
       jScrollPane2.setViewportView(symbolicPane);
       jSplitPane2.setRightComponent(jScrollPane2);
       smaliPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Smali Code"));
       javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(smaliPanel);
       smaliPanel.setLayout(jPanel1Layout);
       jPanel1Layout.setHorizontalGroup(
           jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
           .addComponent(smaliClassBox, 0, 198, Short.MAX_VALUE)
           .addComponent(smaliScroll)
       );
       jPanel1Layout.setVerticalGroup(
           jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
           .addGroup(jPanel1Layout.createSequentialGroup()
               .addComponent(smaliClassBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
               .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
               .addComponent(smaliScroll, javax.swing.GroupLayout.DEFAULT_SIZE, 241, Short.MAX_VALUE))
       );

       jSplitPane2.setLeftComponent(smaliPanel);
       jSplitPane1.setRightComponent(jSplitPane2);
       jScrollPane1.setBorder(javax.swing.BorderFactory.createTitledBorder("Execution Log"));

       executionPane.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
       jScrollPane1.setViewportView(executionPane);

       jSplitPane1.setLeftComponent(jScrollPane1);

       javax.swing.GroupLayout detailPaneLayout = new javax.swing.GroupLayout(detailPane);
       detailPane.setLayout(detailPaneLayout);
       detailPaneLayout.setHorizontalGroup(
           detailPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
           .addComponent(jSplitPane1)
       );
       detailPaneLayout.setVerticalGroup(
           detailPaneLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
           .addComponent(jSplitPane1)
       );

       jLabel3.setText("  Info:");
       infoLabel.setText("Welcome");
       executionButton.setText("Analyze");
       
       javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
       getContentPane().setLayout(layout);
       layout.setHorizontalGroup(
           layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
           .addGroup(layout.createSequentialGroup()
               .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                   .addComponent(jLabel2, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                   .addComponent(jLabel1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                   .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                   .addComponent(jLabel6, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
               .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
               .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                   .addGroup(layout.createSequentialGroup()
                       .addComponent(urlField)
                       .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                       .addComponent(inspectButton, javax.swing.GroupLayout.PREFERRED_SIZE, 74, javax.swing.GroupLayout.PREFERRED_SIZE))
                   .addComponent(classBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                   .addComponent(summaryBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                   .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                       .addComponent(methodBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                       .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                       .addComponent(executionButton, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE)))
               .addGap(6, 6, 6))
           .addComponent(detailPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
           .addGroup(layout.createSequentialGroup()
               .addComponent(jLabel3)
               .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
               .addComponent(infoLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
       );
       layout.setVerticalGroup(
           layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
           .addGroup(layout.createSequentialGroup()
               .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                   .addComponent(inspectButton)
                   .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 27, javax.swing.GroupLayout.PREFERRED_SIZE)
                   .addComponent(urlField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
               .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                   .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                   .addComponent(classBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addGap(1, 1, 1)
               .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                   .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                   .addComponent(methodBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                   .addComponent(executionButton))
               .addGap(1, 1, 1)
               .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                   .addComponent(jLabel2)
                   .addComponent(summaryBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
               .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
               .addComponent(detailPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
               .addGap(0, 0, 0)
               .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                   .addComponent(jLabel3)
                   .addComponent(infoLabel)))
       );
       
       pack();
   }// </editor-fold>                        
   
   void addTextPane(String text, List<String> toHightlight){
	   JTextPane pane = new JTextPane();
	   pane.setText(text);
	   pane.setMinimumSize(new Dimension(300,400));
	   for (String line : toHightlight) {
			int start = text.indexOf(line);
			if(start >= 0){
				try {
					int end = text.indexOf('\n', text.indexOf('\n', start)+1);
					pane.getHighlighter().addHighlight(start, end, primaryHightlight);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}else{
				System.out.println("Fail to find: "+line);
			}
	   }
	   pane.setOpaque(true);
	   pane.setVisible(true);
	   this.textList.add(pane);
   }
   
   void chooseTextPane(int index, String toHightlight){
	   JTextPane pane = this.textList.get(index);
	   if(pane == null) return;
	   smaliScroll.setViewportView(pane);
	   if(toHightlight != null){
		   if(this.previousIndex >= 0 && this.previousIndex < this.textList.size()){
			  JTextPane pPane =  textList.get(previousIndex);
			  pPane.getHighlighter().removeHighlight(previousTag);
		   }
		   
		   String text = pane.getText();
		   int start = text.indexOf(toHightlight);
		   if(start >= 0){
			   int end = text.indexOf('\n', text.indexOf('\n', start)+1);
			   try {
				   previousTag = pane.getHighlighter().addHighlight(start, end, secondaryHight);
				   this.previousIndex = index;
			   } catch (BadLocationException e) {
				   e.printStackTrace();
				   this.previousIndex = -1;
				   this.previousTag = null;
			   }
		   }else{
			   this.previousIndex = -1;
			   this.previousTag = null;
		   }
	   }else{ 
		   smaliScroll.getViewport().setViewPosition(new Point(0,0)); 
		   if(this.textList.isEmpty()){
			   this.previousIndex = -1;
			   this.previousTag = null;
		   }
	   }
   }
   void clearTextPane(){
	   this.textList.clear();
//	   this.smaliScroll.removeAll();
   }
   
   
   /**
    * @param args the command line arguments
    */
   public static void main(String args[]) {
       /* Set the Nimbus look and feel */
       //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
       /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
        * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
        */
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
       //</editor-fold>
       //</editor-fold>

       /* Create and display the form */
       java.awt.EventQueue.invokeLater(new Runnable() {
           public void run() {
               new ShowcaseFrame().setVisible(true);
           }
       });
   }

   // Variables declaration
   javax.swing.DefaultComboBoxModel<String> classModel;
   javax.swing.DefaultComboBoxModel<String> methodModel;
   javax.swing.DefaultComboBoxModel<String> summaryModel;
   javax.swing.DefaultComboBoxModel<String> smaliModel;
   javax.swing.DefaultListModel<String> lineModel;
   
   javax.swing.JTextField urlField;
   javax.swing.JComboBox<String> classBox;
   javax.swing.JComboBox<String> methodBox;
   javax.swing.JComboBox<String> summaryBox;
   javax.swing.JComboBox<String> smaliClassBox;
   javax.swing.JButton inspectButton;
   javax.swing.JButton executionButton;
   javax.swing.JList<String> executionPane;
   javax.swing.JLabel infoLabel;
   javax.swing.JScrollPane smaliScroll;
   javax.swing.JTextArea symbolicPane;
   
   private javax.swing.JPanel detailPane;
   private javax.swing.JLabel jLabel1;
   private javax.swing.JLabel jLabel2;
   private javax.swing.JLabel jLabel3;
   private javax.swing.JLabel jLabel5;
   private javax.swing.JLabel jLabel6;
   private javax.swing.JPanel smaliPanel;
   private javax.swing.JScrollPane jScrollPane1;
   private javax.swing.JScrollPane jScrollPane2;
   private javax.swing.JSplitPane jSplitPane1;
   private javax.swing.JSplitPane jSplitPane2;
   
   private int previousIndex = -1;
   private Object previousTag = null;
   private List<JTextPane> textList = new ArrayList<JTextPane>();
   private DefaultHighlighter.DefaultHighlightPainter primaryHightlight = 
	        new DefaultHighlighter.DefaultHighlightPainter(Color.GREEN);

   private DefaultHighlighter.DefaultHighlightPainter secondaryHight = 
	        new DefaultHighlighter.DefaultHighlightPainter(Color.RED);
   // End of variables declaration                   
}
