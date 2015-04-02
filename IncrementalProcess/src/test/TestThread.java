package test;

public class TestThread {

	public static void main(String[] args){
		Thread t = new Thread(new Runnable(){

			@Override
			public void run() {
				while(true){
					System.out.println("asd");;
					
					try { Thread.sleep(200);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
			
		});
		
		t.start();
		try {
			t.join(100);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(t.isAlive()){
			t.interrupt();
		}
		
		
		System.out.println("end");
	}
 
}
