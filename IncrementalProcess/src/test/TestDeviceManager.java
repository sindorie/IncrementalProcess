/**
 * 
 */
package test;

import ignored.DeviceManager;
import ignored.DeviceManager.DeviceBundle;

/**
 * @author zhenxu
 *
 */
public class TestDeviceManager {
	public static void main(String[] args){
		DeviceManager manager = new DeviceManager();
		long start = System.currentTimeMillis();
		int size = 0;
		while(true){
			int cSize = manager.deviceList.size();
			if(cSize != size){
				for(DeviceBundle device : manager.deviceList){
					System.out.println(device);
				}
				System.out.println("\n");
				size = cSize;
			}
			
			try { Thread.sleep(1000);
			} catch (InterruptedException e) { }
			
			long current = System.currentTimeMillis();
			if(current - start > 10 * 1000/* 10 sc */ ){
				break;
			}
		}
	}
}
