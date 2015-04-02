package test;

import ignored.LayoutNode_hier;
import ignored.ViewDeviceInformaion;

import javax.swing.JFrame;
import javax.swing.JTree;

import components.system.DeviceManager;
import components.system.DeviceManager.DeviceBundle;

public class TestViewDeviceInformaion {

	public static void main(String[] args){
		DeviceManager manager = new DeviceManager();
		long start = System.currentTimeMillis();
		long last = 0;
		while(true){
			int cSize = manager.deviceList.size();
			if(cSize > 0){
				break;
			}
			try { Thread.sleep(1000);
			} catch (InterruptedException e) { }
			last = System.currentTimeMillis();
			if(last - start > 10 * 1000){break; }
		}
		DeviceBundle bundle = manager.deviceList.get(0);
		ViewDeviceInformaion vdi = new ViewDeviceInformaion(bundle);
		LayoutNode_hier layout = vdi.loadFocusedWindowData();
		JTree tree = new JTree(layout);
		JFrame frame = new JFrame();
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.add(tree);
		frame.pack();
		frame.setVisible(true);
		
	}

}
