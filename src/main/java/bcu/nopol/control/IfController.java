package bcu.nopol.control;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IfController {

	/**
	 * ifNumber, classname, methodname, line, ifvalue
	 */
	private static Map<Integer, Map<String, Map<String, Map<Integer,IfUsage>>>> ifMap2 = new HashMap<>();

	private static Map<Integer, List<StackTraceElement>> ifMaptmp = new HashMap<>();

	private static String currentMethod;

	private static int currentLine;

	private static int constituents = 0;

	private static int constituentRetenue = 0;
	
	public static Map<Integer, Map<String, Map<String, Map<Integer, IfUsage>>>> getIfMap() {
		return ifMap2;
	}
	
	private static void addSTE(int i, StackTraceElement ste, Boolean b){
//		if(!ifMaptmp.containsKey(i)){
//			ifMaptmp.put(i, new ArrayList<StackTraceElement>());
//		}
//		ifMaptmp.get(i).add(ste);
//		
		
		if(!ifMap2.containsKey(i)){
			ifMap2.put(i, new HashMap<String, Map<String, Map<Integer,IfUsage>>>());
		}
		Map<String, Map<String, Map<Integer, IfUsage>>> ifMap = ifMap2.get(i);
		if(!ifMap.containsKey(ste.getClassName())){
			ifMap.put(ste.getClassName(), new HashMap<String, Map<Integer,IfUsage>>());
		}
		if(!ifMap.get(ste.getClassName()).containsKey(ste.getMethodName())){
			ifMap.get(ste.getClassName()).put(ste.getMethodName(), new HashMap<Integer,IfUsage>());
		}
		if(ifMap.get(ste.getClassName()).get(ste.getMethodName()).containsKey(ste.getLineNumber())){
			if(ifMap.get(ste.getClassName()).get(ste.getMethodName()).get(ste.getLineNumber())!= IfUsage.NONE &&
					ifMap.get(ste.getClassName()).get(ste.getMethodName()).get(ste.getLineNumber()) != IfUsage.fromBool(b)){
				ifMap.get(ste.getClassName()).get(ste.getMethodName()).put(ste.getLineNumber(), IfUsage.BOTH);
				return;
			}
		}
		ifMap.get(ste.getClassName()).get(ste.getMethodName()).put(ste.getLineNumber(), IfUsage.fromBool(b));
	}
	
//	public static void beforeIf() {
//		StackTraceElement[] stes = Thread.currentThread().getStackTrace();
//		StackTraceElement previous = null;
//		for (StackTraceElement ste : stes) {
//			if(ste.getClassName().equals("sun.reflect.NativeMethodAccessorImpl")){
//				addSTE(previous, null);
//				return;
//			}
//			 previous = ste;
//		}
//	}

	public static void thenExecuted(int i) {
		try{
		StackTraceElement ste = new StackTraceElement(currentMethod.split(":")[0], currentMethod.split(":")[1], "...", currentLine);
		addSTE(i, ste, true);
		}catch(NullPointerException npe){
			
		}
	}

	public static void elseExecuted(int i) {
		try{
		StackTraceElement ste = new StackTraceElement(currentMethod.split(":")[0], currentMethod.split(":")[1], "...", currentLine);
		addSTE(i, ste, false);
		}catch(NullPointerException npe){
			
		}
	}

	public static void newMethod(String string) {
		currentMethod = string;
	}

	public static void newLine(int i) {
		currentLine = i;
		constituents++;
		if (constituents == Integer.MAX_VALUE){
			System.err.println("MAX VALUE REACHED");
			constituentRetenue++;
			constituents=0;
		}
	}

	public static int getConstNumber() {
		return constituents;
	}

	public static int getConstNumberRetenue() {
		return constituentRetenue;
	}

}
