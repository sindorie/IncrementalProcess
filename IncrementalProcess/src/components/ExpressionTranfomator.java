package components;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.tree.TreeNode;

import support.TreeUtility;
import support.TreeUtility.Searcher;
import symbolic.Expression;
import symbolic.Variable;

public class ExpressionTranfomator {
	private static boolean shouldBeIgnored;
	private static boolean valid;
	public final static String tmpVairableMatcher = "\\$?[vp]\\d+$";
	public final static String FilterString = "[:\\;\\$\\-\\>\\/\\\\]";
	
	public final static String 
		FINSTANCE = 	"$Finstance",
		FSTATIC = 		"$Fstatic",
		NEWINSTANCE = 	"$new-instance",
		
		CONST_STRING = 	"$const-string",
		CONST_CLASS = 	"$const-class",
		INSTANCEOF = 	"$instance-of",
		ARRAY = 		"$array",
		RETURN = 		"$return",
		API = 			"$api"
		;
	
	
	
	static Map<String,String> typeMap = new HashMap<String,String>();
	static Map<String,String> operatorMap = new HashMap<String,String>();
	static{
		String[] typePairs = {
				"Z","int",//"Z" to "bool" requires more analysis
				"B","int",
				"S","int",
				"C","int",
				"I","int",
				"J","int",
				"F","real",
				"D","real",
			};
		for(int i=0;i<typePairs.length;i+=2){
			typeMap.put(typePairs[i], typePairs[i+1]);
		}
		String[] operatorPairs = {
				"add", "+",
				"sub", "-",
				"mul", "*",
				"dev", "/",
				//do no support in this version TODO
//				"and", "and",
//				"or", "or",
//				"xor", "xor",
//				"neg", "neg",  //this should be replace by xor X true 
				"<","<",
				">",">",
				"=","=",
				"==","=",
				"/=","/=",
				"!=","/=",
				"<=","<=",
				">=",">=",
		};
		for(int i =0;i<operatorPairs.length;i+=2){
			operatorMap.put(operatorPairs[i], operatorPairs[i+1]);
		}
	}
	
	/**
	 * Basic checking to see if the expression contains any 
	 * tmp variables, API, instanceof, constclass, conststring, array are ignored for now
	 * 
	 * @param input
	 * @return
	 */
	public static boolean check(Expression input){
		valid = true;
		TreeUtility.breathFristSearch(input, new Searcher(){
			@Override
			public int check(TreeNode treeNode) {
				Expression node = (Expression)treeNode;
				String content = node.getContent();
				if(content.matches(tmpVairableMatcher)){ //tmp variables. e.g. v0, p0
					valid = false;
					return Searcher.STOP;
				}else if(content.equalsIgnoreCase(API)){
					valid = false;
					return Searcher.STOP;
				}else if(content.matches(INSTANCEOF)){
					valid = false;
					return Searcher.STOP;
				}else if(content.matches(CONST_CLASS)){
					valid = false;
					return Searcher.STOP;
				}else if(content.matches(ARRAY)){
					valid = false;
					return Searcher.STOP;
				}
				return Searcher.NORMAL;
			}
		});
		return valid;
	}
	
	
	public static List<Expression> transform(List<Expression> inputs){
		List<Expression> result = new ArrayList<Expression>();
		for(Expression expre : inputs){
			Expression tmp = transform(expre);
			if(tmp != null) result.add(tmp);
		}
		return result;
	}
	
	/**
	 * API -- filtered at this point 
	 * 
	 * Variable -- transform leaves to variable
	 * 
	 * @param input
	 * @return
	 */
	public static Expression transform(Expression input){
		shouldBeIgnored = false;
		if(check(input) == false) return null;

		return recursiveTransform(input);
	}
	
	private static Expression recursiveTransform(Expression input){
		String content = input.getContent();
		if(content.equalsIgnoreCase(FSTATIC)){
			Expression child = (Expression) input.getChildAt(0);	
			String childContent = child.getContent();
			String[] parts = childContent.split(":");
			if(parts.length <2) return null;
			String name = parts[0].replaceAll(FilterString, "");
			String mappedType = typeMap.get(parts[1]);
			if(mappedType == null) return null;
			Variable var = new Variable(name, mappedType);
			return var;
		}else if(content.equalsIgnoreCase(FINSTANCE)){
			return processFinstance(input);
		}else if(content.equalsIgnoreCase(NEWINSTANCE)){
			//object is useless encountered here
			return null;
		}else if(input.isLeaf()){
			return input.clone();
		}else{
			String mappedOperator = operatorMap.get(content);
			if(mappedOperator == null){
				return null;
			}
			Expression expre = new Expression(mappedOperator);
			for(int i = 0; i<input.getChildCount();i++){
				Expression child = (Expression) input.getChildAt(i);
				Expression tranformed = recursiveTransform(child);
				if(tranformed == null) return null;
				expre.add(tranformed);
			}
			return expre;
		}
	}

	
	private static Expression processFinstance(Expression expre){
		int count = expre.getChildCount();
		/*
		 * According to the document, there should be two children.
		 * 1. field signature
		 * No sub child
		 * 
		 * 2. object expression
		 * There could be sub children
		 * 
		 */
		if(count == 2 ){
			//according to the document, there should be two children
			//the first is field signature, the second should be object expression
			Expression fieldSig = (Expression) expre.getChildAt(0);
			Expression objectExpre = (Expression) expre.getChildAt(1);
			String[] parts = fieldSig.getContent().split(":");
			String name = parts[0];
			String type = typeMap.get(parts[1]);
			//filter ":", ";", "$", "-", ">", "/","\"
			String fieldName = (processFinstanceHelper(objectExpre)+name).replaceAll(FilterString, "");
			return new Variable(fieldName,type);
		}else{
			return null;
		}
	}
	
	
	private static String processFinstanceHelper(Expression objectExpression){
		String objectContent = objectExpression.getContent();
		if(objectContent.equalsIgnoreCase(FINSTANCE)){
			Expression fieldSig = (Expression) objectExpression.getChildAt(0);
			Expression subObject = (Expression) objectExpression.getChildAt(1);
			String prefix = processFinstanceHelper(subObject);
			return (prefix + fieldSig);
		}else if(objectContent.equalsIgnoreCase(FSTATIC)){
			return ((Expression)objectExpression.getChildAt(0)).getContent();
		}else if(objectContent.equalsIgnoreCase(NEWINSTANCE)){
			return ((Expression)objectExpression.getChildAt(0)).getContent();
		}
		return objectExpression.getContent();
	}
	
//	public static void main(String[] args){
//		
//		Expression root = new Expression("/=");
//
//		Expression left = new Expression("$Fstatic");
//		Expression subsLeft = new Expression("Ladsf;->x:I");
//		left.add(subsLeft);
//		
//		Expression right = new Expression("1");
//		
//		root.add(left);
//		root.add(right);
//		System.out.println(Arrays.toString("Ladsf;->x:I".split(":")));
//		System.out.println(ExpressionTranfomator.transform(left));
//		;
//	}
	
}
