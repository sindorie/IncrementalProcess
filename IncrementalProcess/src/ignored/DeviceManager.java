package ignored;

import java.util.ArrayList;
import java.util.List;

import main.Paths;

import com.android.ddmlib.AndroidDebugBridge;
import com.android.ddmlib.IDevice;
import com.android.hierarchyviewerlib.device.DeviceBridge;
import com.android.hierarchyviewerlib.device.HvDeviceFactory;
import com.android.hierarchyviewerlib.device.IHvDevice;
import components.system.Configuration;

public class DeviceManager {
	public final static String TAG = "DeviceManager";

	public final List<DeviceBundle> deviceList = new ArrayList<DeviceBundle>();
	private List<AndroidDebugBridge.IDeviceChangeListener> additionalList 
	= new ArrayList<AndroidDebugBridge.IDeviceChangeListener>();
	
	public DeviceManager(){
		DeviceBridge.initDebugBridge(Configuration.getValue(Configuration.attADB));
		DeviceBridge.startListenForDevices(masterListener);
	}
	
	public void terminate(){
		DeviceBridge.terminate();
	}
	
	private AndroidDebugBridge.IDeviceChangeListener masterListener 
	= new AndroidDebugBridge.IDeviceChangeListener(){
		@Override
		public void deviceChanged(IDevice arg0, int arg1) { 
			
		}
		@Override
		public void deviceConnected(IDevice arg0) { 
			IHvDevice device = HvDeviceFactory.create(arg0);
			DeviceBundle element = new DeviceBundle();
			element.rawDevice = arg0;
			element.viewDevice = device;
			deviceList.add(element);
		}
		@Override
		public void deviceDisconnected(IDevice arg0) { 
			IHvDevice device = HvDeviceFactory.create(arg0);
			DeviceBundle element = new DeviceBundle();
			element.rawDevice = arg0;
			element.viewDevice = device;
			deviceList.remove(element);
		}
	};
	
	public void addAdditionalDeivceListener(AndroidDebugBridge.IDeviceChangeListener lis){
		additionalList.add(lis);
		DeviceBridge.startListenForDevices(lis);
	}
	
	public void removeListener(AndroidDebugBridge.IDeviceChangeListener lis){
		this.additionalList.remove(lis);
		DeviceBridge.stopListenForDevices(lis);
	}
	
	public void tearDown(){
		DeviceBridge.stopListenForDevices(masterListener);
		for(AndroidDebugBridge.IDeviceChangeListener lis : additionalList){
			DeviceBridge.stopListenForDevices(lis);
		}
	}
	
	public static class DeviceBundle{
		public IDevice rawDevice;
		public IHvDevice viewDevice;
		
		@Override
		public String toString(){
			return rawDevice.getName();
		}
		
		@Override
		public boolean equals(Object o){
			if(o instanceof DeviceBundle){
				return this.rawDevice.getSerialNumber().equals(((DeviceBundle)o).rawDevice.getSerialNumber());
			}else if(o instanceof String){
				return this.rawDevice.getSerialNumber().equals(o);
			}
			return false;
		}
	}
}
