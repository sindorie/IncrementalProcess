package ignored;

import com.android.ddmlib.IDevice;
import com.android.hierarchyviewerlib.device.DeviceBridge;
import com.android.hierarchyviewerlib.device.WindowUpdater;
import com.android.hierarchyviewerlib.models.ViewNode;
import com.android.hierarchyviewerlib.models.Window;

import components.system.DeviceManager.DeviceBundle;

//might want to extend AbstractHvDevice
public class ViewDeviceInformaion {
	private DeviceBundle info;
	private Window[] windowList;
	private int focusedWindowHash;
	
	public ViewDeviceInformaion(DeviceBundle info){
		this.info = info;
		info.viewDevice.terminateViewDebug();
		boolean viewDebugEnable = info.viewDevice.isViewDebugEnabled();
		System.out.println("isViewDebugEnabled: "+ viewDebugEnable);
		info.viewDevice.initializeViewDebug();
		try { Thread.sleep(50); } catch (InterruptedException e) { }
		info.viewDevice.addWindowChangeListener(windowListener);	
		windowList = DeviceBridge.loadWindows(info.viewDevice, info.rawDevice);
		focusedWindowHash = DeviceBridge.getFocusedWindow(info.rawDevice);
	}
	
	public Window[] getWindowList(){
		return windowList==null?new Window[0]:windowList;
	}
	
    public Window getFocusedWindow() {
    	if(windowList == null) return null;
    	for(Window win: windowList){
    		if(win.getHashCode() == focusedWindowHash){
    			return win;
    		}
    	}
        return null;
    }
    
    public LayoutNode_hier loadWindowData(Window window) {
    	ViewNode raw = window==null?null:DeviceBridge.loadWindowData(window);
    	if(raw == null) return null;
    	else return new LayoutNode_hier(raw);
    }
    
    public LayoutNode_hier loadFocusedWindowData(){
    	return loadWindowData(Window.getFocusedWindow(info.viewDevice));
    }
    
    public void invalidView(ViewNode node){
    	DeviceBridge.invalidateView(node);
    }
    
    public void requestLayout(ViewNode node){
    	DeviceBridge.requestLayout(node);
    }

	private WindowUpdater.IWindowChangeListener windowListener = new WindowUpdater.IWindowChangeListener(){
		@Override
		public void focusChanged(IDevice arg0) {
			if(info.rawDevice == arg0 || info.rawDevice.equals(arg0))
			focusedWindowHash = DeviceBridge.getFocusedWindow(arg0);
		}
		@Override
		public void windowsChanged(IDevice arg0) {
			if(info.rawDevice == arg0 || info.rawDevice.equals(arg0)){
				windowList = DeviceBridge.loadWindows(info.viewDevice, arg0);
			}
		}
	};
}
