package test;

import java.util.Comparator;
import java.util.PriorityQueue;

public class MiscellaneousTestGround {

	public static void main(String[] args) {
		PriorityQueue<cls> q = new PriorityQueue<cls>(new Comparator<cls>(){
			@Override
			public int compare(cls o1, cls o2) {
				return o2.i - o1.i;
			}
			
		}); 
		
		q.add(new cls(1));
		q.add(new cls(2));
		
		System.out.println(q.peek());
	}

	static class cls{
		int i = 0;
		public cls(int i){
			this.i = i;
		}
		public String toString(){
			return i+"";
		}
	}
}
