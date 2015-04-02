package test;

import components.system.InformationCollector;

public class TestInformationCollector {

	public static void main(String[] args) {
		//08563207 -- broken one
		//192.168.56.102:5555 
		
		String serial = 
//				"08563207";
				"015d3f1936080c05";
		InformationCollector collector = new InformationCollector(serial);

//		System.out.println(collector.getWindowPolicy());
		System.out.println(collector.getWindowOverview().getFocusedWindow());
//		System.out.println(collector.getInputMethodOverview());
	}
 
}


/**
NavigationBar, 10013, false, com.android.systemui, NONE, (1080.0,144.0), (0.0,1776.0)

StatusBar, 10013, false, com.android.systemui, NONE, (1080.0,75.0), (0.0,0.0)

com.example.testclickable/com.example.testclickable.MainActivity, 
10060, true, com.example.testclickable, NONE, (1080.0,1920.0), (0.0,0.0)

**/