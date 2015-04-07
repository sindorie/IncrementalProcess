package support;

import java.util.ArrayList;
import java.util.List;

public class Utility {
	
	/**
	 * Permutation over the input which is a 2D array. The size of the list elements can vary. 
	 * Only address checking for the excluded list.
	 * 
	 * @param input
	 * @param excludedForEachColumn
	 * @return
	 */
	public static <E> List<List<E>> permutate(List<List<E>> input){
		return permuatationHelper(input, 0);
	}
	
	private static <E> List<List<E>> permuatationHelper(List<List<E>> input, int index){
		if(index >= input.size() ) return null;
		List<E> list = input.get(index);
		if(list == null){
			return permuatationHelper(input,index+1);
		}else{
			List<List<E>> result = new ArrayList<List<E>>();
			List<List<E>> partial = permuatationHelper(input,index+1);
			if(partial == null || partial.isEmpty()){
				for(E element : list){
					if(element == null){ continue; }
					List<E> local = new ArrayList<E>();
					local.add(element);
					result.add(local);
				}
				return result;
			}else{
				for(E element : list){
					if(element == null){ continue; }
					for(List<E> post:partial){
						List<E> local = new ArrayList<E>();
						local.add(element);
						local.addAll(post);
						result.add(local);
					}
				}
				if(result.isEmpty()){
					return partial;
				}
				return result;
			}
		}
	}
}
