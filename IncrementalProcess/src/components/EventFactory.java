package components;

import android.view.KeyEvent;

public class EventFactory { 
	public static final String pkgPath = "event_path";
	public static final String actName = "event_actName";
	public static final String pkgName = "event_packageName";
	public static final String keyCode = "event_keycode";
	public static final String xCoordinate = "event_clickx";
	public static final String yCoordinate = "event_clicky";
	
	public static final long NON_SLEEP = 0, LAUNCH_SLEEP = 2000;
	public static final long RESTART_SLEEP = 2000,  REINSTALL_SLEEP = 2000;
	public static final long PRESS_SLEEP = 1500, ONCLICK_SLEEP = 1500;
	
	public final static String UNDEFINED = "undefined";//, EMPTY = "empty", UPDATE = "update";
	public final static String LAUNCH = "launch", REINSTALL = "reinstall" ; //, RESTART = "restart";
	public final static String PRESS = "press", ONCLICK = "android:onClick";
	
	public final static int iUNDEFINED = -1;//, iEMPTY = -3, iUPDATE = -2;
	public final static int iLAUNCH = 0, iREINSTALL = 2, iPRESS = 3, iONCLICK = 4;//, iRESTART = 1;
	
	private EventFactory(){}
	
	public static Event createReinstallEvent(String pkgName, String path){
		Event e = new Event();
		e.source = null;
		e.eventType = iREINSTALL;
		e.putAttribute(EventFactory.pkgName, pkgName);
		e.putAttribute(EventFactory.pkgPath, path);
		return e;
	}
	
	public static Event createLaunchEvent(GraphicalLayout source, String pkg, String act){
		Event e = new Event();
		e.source = source;
		e.eventType = iLAUNCH;
		e.putAttribute(EventFactory.pkgName, pkg);
		e.putAttribute(EventFactory.actName, act);
		return e;
	}
	
	public static Event createClickEvent(GraphicalLayout source, LayoutNode node){
		System.out.println(node.className+" - "+node.id+" - "+node.startx+","+node.endx+", "+node.starty+","+node.endy);
		
		return createClickEvent(source, (node.startx+node.endx)/2,
				(node.starty+node.endy)/2 );
	}
	
	public static Event createClickEvent(GraphicalLayout source, int x, int y){
		Event e = new Event();
		e.source = source;
		e.eventType = iONCLICK;
		e.putAttribute(EventFactory.xCoordinate, x);
		e.putAttribute(EventFactory.yCoordinate, y);
		return e;
	}
	
	public static Event createCloseKeyboardEvent(){
		Event e = new Event();
		e.eventType = iPRESS;
		e.putAttribute(EventFactory.keyCode, KeyEvent.KEYCODE_BACK+"");
		e.ignoreByRecorder = true;
		return e;
	}
	
	public static Event CreatePressEvent(GraphicalLayout source, int type){ 
		return createPressEvent(source, type+"");
	}
	
	public static Event createPressEvent(GraphicalLayout source, String type){
		Event e = new Event();
		e.source = source;
		e.eventType = iPRESS;
		e.putAttribute(EventFactory.keyCode,type);
		return e;
	}
	
	public static int stringToint(String eventString){
		if(eventString.equals(LAUNCH)){
			return iLAUNCH;
		}else if(eventString.equals(REINSTALL)){
			return iREINSTALL;
		}else if(eventString.equals(PRESS)){
			return iPRESS;
		}else if(eventString.equals(ONCLICK)){
			return iONCLICK;
		}else return iUNDEFINED;
	}
	
	public static long getNeededSleepDuration(int type){
		switch(type){
		case iLAUNCH: 	return EventFactory.LAUNCH_SLEEP;
		case iREINSTALL: return EventFactory.REINSTALL_SLEEP;
		case iPRESS: 	return EventFactory.PRESS_SLEEP;
		case iONCLICK: 	return EventFactory.ONCLICK_SLEEP;
		case iUNDEFINED:
		default: return EventFactory.NON_SLEEP;
		}
	}
	public static String intToString(int type){
		switch(type){
		case iLAUNCH: 	return LAUNCH;
		case iREINSTALL: return REINSTALL;
		case iPRESS: 	return PRESS;
		case iONCLICK: 	return ONCLICK;
		}
		return UNDEFINED;
	}
}
