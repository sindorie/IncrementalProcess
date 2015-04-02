package components.system;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;

import support.TreeUtility;
import support.TreeUtility.Searcher;

public class InputMethodOverview {
	public final static String TAG = "InputMethodOverview";
	public String inputType, imeOptions, privateImeOptions;
	public String actionLabel, actionId;
	public String initialSelStart, initialSelEnd, initialCapsMode;
	public String hintText, label;
	public String packageName, fieldId, fieldName;
	public String extras;
	
	public final static String DumpCommand = "dumpsys input_method";
	public final static String RootNodeIdentifier = "mInputEditorInfo:";
	
	public InputMethodOverview(){}
	public InputMethodOverview(DefaultMutableTreeNode tree){
		TreeUtility.breathFristSearch(tree, new Searcher(){
			@Override
			public int check(TreeNode node) {
				TreeUtility.breathFristSearch(tree, new Searcher(){
					@Override
					public int check(TreeNode node) {
						String line = ((DefaultMutableTreeNode) node).getUserObject().toString();
						if(line.startsWith("inputType")){
							String[] data = InformationCollector.extractMultiValue(line);
							inputType = data[0];
							imeOptions = data[1];
							privateImeOptions = data[2];
							return Searcher.NORMAL;
						}
						if(line.startsWith("actionLabel")){
							String[] data = InformationCollector.extractMultiValue(line);
							actionLabel = data[0];
							actionId = data[1];
							return Searcher.NORMAL;
						}
						if(line.startsWith("initialSelStart")){
							String[] data = InformationCollector.extractMultiValue(line);
							initialSelStart = data[0];
							initialSelEnd = data[1];
							initialCapsMode = data[2];
							return Searcher.NORMAL;
						}
						if(line.startsWith("hintText")){
							String[] data = InformationCollector.extractMultiValue(line);
							hintText = data[0];
							label = data[1];
							return Searcher.NORMAL;
						}
						if(line.startsWith("packageName")){
							System.out.println(line);
							String[] data = InformationCollector.extractMultiValue(line);
							packageName = data[0];
							fieldId = data[1];
							fieldName = data[2];
							return Searcher.NORMAL;
						}
						if(line.startsWith("extras")){
							String[] data = InformationCollector.extractMultiValue(line);
							extras = data[0];
							return Searcher.NORMAL;
						}
						
						return Searcher.NORMAL;
					}
				});
				return Searcher.NORMAL;
			}
		});
	}
	
	@Override
	public String toString(){
		return String.join("; ", inputType, imeOptions, privateImeOptions, packageName, fieldId, fieldName);
	}
}

/* Sample for mInputEditorInfo
 
  mInputEditorInfo:
    inputType=0x2002 imeOptions=0xc000005 privateImeOptions=null
    actionLabel=null actionId=0
    initialSelStart=0 initialSelEnd=0 initialCapsMode=0x2000
    hintText=null label=null
    packageName=com.example.testlayout fieldId=2131296327 fieldName=null
    extras=null
 

 */
