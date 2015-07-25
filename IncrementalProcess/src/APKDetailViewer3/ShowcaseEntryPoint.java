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
				return "Just a test #"+(i++);
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
