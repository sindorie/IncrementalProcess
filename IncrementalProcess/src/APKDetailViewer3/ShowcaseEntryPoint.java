package APKDetailViewer3;

import java.util.List;

import symbolic.Expression;

public class ShowcaseEntryPoint {

	public static void main(String[] args) {
		uiSetup();
		SolverTester solver = null; // new YourSolver();
		
		solver = new SolverTester(){
			int i = 0;
			@Override
			public Object solve(List<Expression> constraints, List<Expression> symbolics) {
				StringBuilder sb = new StringBuilder();
				sb.append("Just a test #"+(i++));
				for(Expression expre : constraints){
					sb.append(treeToText(expre, 0));
					sb.append("\n");
				}
				
				sb.append("\n\n");
				
				for(Expression expre : symbolics){
					sb.append(treeToText(expre, 0));
					sb.append("\n");
				}
				return sb.toString();
			}
			
			private String treeToText(Expression node, int level){
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
		};
		Showcase showcase = new Showcase();
		showcase.addTester(solver);
		showcase.show();
	}

	
	static void uiSetup(){
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
	}
}
