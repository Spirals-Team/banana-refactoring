package bcu.nopol.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;

import sacha.impl.DefaultSpooner;
import sacha.impl.TestRunnerCore;
import sacha.interfaces.ISpooner;
import sacha.interfaces.ITestResult;
import bcornu.resi.annot.BananaRefactoringRunner;
import bcornu.resi.annot.RunBefore;
import bcu.nopol.control.IfController;
import bcu.nopol.control.IfUsage;
import bcu.nopol.processor.MethodCutterProcessor;

public class Launcher {
	
	// set here this to your own path
	public String WORKSPACE_METADATA_PATH = 
			System.getProperty("WORKSPACE_METADATA_PATH") != null ? 
			System.getProperty("WORKSPACE_METADATA_PATH")
			: "/home/martin/workspace/.metadata";
			
			// has to be static because being used in LsClass, TODO change to non-static
	public static String ORIG_ECLIPSE_PROJECT_NAME = 
			System.getProperty("ORIG_ECLIPSE_PROJECT_NAME") != null ? 
			System.getProperty("ORIG_ECLIPSE_PROJECT_NAME")
			: "shindig-java-gadgets";
	public String TARGET_ECLIPSE_PROJECT_NAME = 
			System.getProperty("TARGET_ECLIPSE_PROJECT_NAME") != null ? 
			System.getProperty("TARGET_ECLIPSE_PROJECT_NAME")
			: "shindig-java-gadgets-refactored";
	public static String OUTPUT_PROJECT_PATH = 
		System.getProperty("OUTPUT_PROJECT_PATH") != null ? 
			System.getProperty("OUTPUT_PROJECT_PATH")
			: "/home/martin/workspace/shindig-java-gadgets-refactored";
	
	// has to be static for use in MethodCutterProcessor TODO change to non-static			
	private static List<StackTraceElement> cuts = null;
	
	private String eclipseMetadata = WORKSPACE_METADATA_PATH;

	private final String srcJava = "src/main/java";
	private String srcTest = "src/test/java";
//	private final String projectName = "math-309";
//
//	private final String ifClass = "org.apache.commons.math.random.RandomDataImpl";
//	private int ifLine = 465;
//	
//	private final String srcJava = "src/main/java";
//	private String srcTest = "src/test/java";

	public static void main(String[] args) throws Throwable {
		Launcher launch;
		launch = new Launcher();
		if(args.length==0 || args[0].equals("-1"))
			launch.expe1();
		else if(args[0].equals("-2"))
			launch.expe2();
		else if(args[0].equals("-3"))
			launch.expe3();
		else if(args[0].equals("-4"))
			launch.expe4();
	}

	/** transforms the app (and not the tests) and generate an overview test class */
	public void expe1() throws Throwable {
		ISpooner spooner = new DefaultSpooner();
		spooner.setEclipseProject(ORIG_ECLIPSE_PROJECT_NAME);
		spooner.setEclipseMetadataFolder(eclipseMetadata);
		System.out.println(ORIG_ECLIPSE_PROJECT_NAME);
		System.out.println(eclipseMetadata);
		spooner.setSourceFolder(new String[]{srcJava});
		spooner.setProcessors("bcu.nopol.processor.IfProcessorAll");
		spooner.setOutputFolder(OUTPUT_PROJECT_PATH+"/"+srcJava);
		spooner.spoon();
		
		spooner = new DefaultSpooner();
		spooner.setEclipseProject(ORIG_ECLIPSE_PROJECT_NAME);
		spooner.setEclipseMetadataFolder(eclipseMetadata);
		spooner.setSourceFolder(new String[]{srcTest});
		spooner.setProcessors("bcu.nopol.processor.AddCallToNewMethod","bcu.nopol.processor.TestLinerAll","bcu.nopol.processor.LsClass","bcu.nopol.processor.TestSuiteGenerator");
		spooner.setOutputFolder(OUTPUT_PROJECT_PATH+"/"+srcTest);
		spooner.spoon();
		
		System.out.println("recompile (refresh in your IDE)");
	}
	
	/** executes the test suite, creates file cutsPerIf, and the cuts the test suite */
	public void expe2() throws Throwable {
		TestRunnerCore runner = new TestRunnerCore();
		runner.setEclipseMetadataFolder(eclipseMetadata);
		runner.setEclipseProject(TARGET_ECLIPSE_PROJECT_NAME);
		System.out.println("searching for tests in "+OUTPUT_PROJECT_PATH+"/"+srcTest);
		ITestResult result= runner.runAllTestsInDirectory(OUTPUT_PROJECT_PATH+"/"+srcTest);
		
		Map<Integer, Map<String, Map<String, Map<Integer, IfUsage>>>> ifMap2 = IfController.getIfMap();
		Set<Integer> impureOnce = new TreeSet<>();
		Set<Integer> pureOnce = new TreeSet<>();
		Set<Integer> impureAlways = new TreeSet<>();
		Set<Integer> pureAlways = new TreeSet<>();
		cuts = new ArrayList<>();
		int impureConst = 0;
		List<String> spoonedFiles = new ArrayList<>();
		File cutsPerTest = new File(OUTPUT_PROJECT_PATH+"/cutsPerIf");
		if(!cutsPerTest.exists())
			if(!cutsPerTest.createNewFile())
				System.err.println("cannot write MEDIAN FILE");
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(cutsPerTest)));

		System.out.println("map size "+ifMap2.keySet().size());
		Set<String> impTest = new TreeSet<>();
		Set<String> purTest = new TreeSet<>();
		for (Integer ifNumber : ifMap2.keySet()) {
			int perIf=0;
			Map<String, Map<String, Map<Integer, IfUsage>>> ifMap = ifMap2.get(ifNumber);
			boolean alwaysPure = true;
			boolean alwaysImpure = true;
			for (String className : ifMap.keySet()) {
				for (String methodName : ifMap.get(className).keySet()) {
					IfUsage tmp = null;
					List<Integer> lines = new ArrayList<>(ifMap.get(className).get(methodName).keySet());
					Collections.sort(lines);
					boolean pure = true;
					for (Integer line : lines) {
						IfUsage currentValue = ifMap.get(className).get(methodName).get(line);
						if(currentValue==IfUsage.BOTH){
							impureConst++;
							tmp=currentValue;
							cuts.add(new StackTraceElement(className, methodName, "atLine", line));
							perIf++;
							impureOnce.add(ifNumber);
							pure = false;
							alwaysPure=false;
						}else if(tmp==null){
							tmp=currentValue;
							continue;
						}else if(tmp==currentValue){
							continue;
						}else{
							impureOnce.add(ifNumber);
							pure = false;
							alwaysPure=false;
							tmp=currentValue;
							cuts.add(new StackTraceElement(className, methodName, "atLine", line));
							perIf++;
						}
					}
					if(pure){
						pureOnce.add(ifNumber);
						alwaysImpure = false;
						if(!impTest.contains(methodName+"+"+className))
                            purTest.add(methodName+"+"+className);
                    }
                    else{//new code
                        impTest.add(methodName+"+"+className);
                        purTest.remove(methodName+"+"+className);
                    }
				}
				spoonedFiles.add(srcTest+"/"+className.replaceAll("\\.", "/")+".java");
			}
			if(alwaysImpure)
				impureAlways.add(ifNumber);
			if(alwaysPure)
				pureAlways.add(ifNumber);
			if(perIf>0)
				pw.println(perIf);
		}
		pw.close();

		Set<StackTraceElement> impureTests = new TreeSet<>(new Comparator<StackTraceElement>() {
			@Override
			public int compare(StackTraceElement o1, StackTraceElement o2) {
				int a = o1.getClassName().compareTo(o2.getClassName());
				a=a==0?o1.getMethodName().compareTo(o2.getMethodName()):a;
				return a;
			}
		});

		Set<StackTraceElement> impureConsts = new TreeSet<>(new Comparator<StackTraceElement>() {
			@Override
			public int compare(StackTraceElement o1, StackTraceElement o2) {
				int a = o1.getClassName().compareTo(o2.getClassName());
				a=a==0?o1.getMethodName().compareTo(o2.getMethodName()):a;
				a=a==0?(o1.getLineNumber()-o2.getLineNumber()):a;
				return a;
			}
		});
		impureTests.addAll(cuts);
		impureConsts.addAll(cuts);

		
		for (StackTraceElement ste : cuts) {
			System.out.println(ste);
		}
		
		
		// now the transformation
		ISpooner spooner2 = new DefaultSpooner();
		spooner2.setEclipseProject(ORIG_ECLIPSE_PROJECT_NAME);
		spooner2.setEclipseMetadataFolder(eclipseMetadata);
		spooner2.setSourceFolder(spoonedFiles.toArray(new String[0]));
		System.out.println(Arrays.toString(spoonedFiles.toArray(new String[0])));
		//spooner2.setProcessors("bcu.nopol.processor.MethodCutterProcessor");
		spooner2.setProcessors("bcu.nopol.processor.AddCallToNewMethod","bcu.nopol.processor.MethodCutterProcessor");
		spooner2.setOutputFolder(OUTPUT_PROJECT_PATH+"/"+srcTest);
		spooner2.spoon();
		
		// plus putting back the original app code
		spooner2 = new DefaultSpooner();
		spooner2.setEclipseProject(ORIG_ECLIPSE_PROJECT_NAME);
		spooner2.setEclipseMetadataFolder(eclipseMetadata);
		spooner2.setSourceFolder(srcJava);
		spooner2.setOutputFolder(OUTPUT_PROJECT_PATH+"/"+srcJava);
		spooner2.spoon();

		
		System.out.println("nb tests :"+MethodCutterProcessor.nbTest);
		System.out.println("nb refactored tests :"+MethodCutterProcessor.nbRefactoredTest);
		System.out.println("test always pure :"+purTest.size());
		System.out.println("test always pure :"+purTest.size());
		System.out.println("test absol impure :"+impureTests.size());
		System.out.println("number of const :"+IfController.getConstNumberRetenue()+":"+IfController.getConstNumber());
		System.out.println("number of impure const: "+impureConst+ " =? "+impureConsts.size());

		System.out.println("----------------------------");
		System.out.println(ORIG_ECLIPSE_PROJECT_NAME+"&"+result.getNbRunTests()+
				"&"+purTest.size()+
				"&"+"\\%"+
				"&"+impureTests.size()+
				"&"+"\\%"+
				"&"+IfController.getConstNumber()+
				"&"+impureConsts.size()+
				"&"+"\\%"+
				"&"+"?"+
				"&"+ifMap2.keySet().size()+
				"&"+pureAlways.size()+
				"&"+"\\%"+
				"&"+impureAlways.size()+
				"&"+"\\% \\\\");
		System.out.println("----------------------------");
		System.out.println("exec ifs: "+ifMap2.keySet().size());
		System.out.println("pure once ifs: "+pureOnce.size());
		System.out.println("purely cov ifs: "+pureAlways.size());
		System.out.println("impurely cov ifs: "+impureAlways.size());
		System.out.println("impure once ifs: "+impureOnce.size());
		
		System.out.println("now refresh to recompile");
}
	
	public void expe3() throws Throwable {
		TestRunnerCore runner = new TestRunnerCore();
		runner.setEclipseMetadataFolder(eclipseMetadata);
		runner.setEclipseProject(TARGET_ECLIPSE_PROJECT_NAME);
		runner.runAllTestsInDirectory(OUTPUT_PROJECT_PATH+"/"+srcTest);

//		ISpooner spooner2 = new DefaultSpooner();
//		spooner2.setEclipseProject(TARGET_ECLIPSE_PROJECT_NAME);
//		spooner2.setEclipseMetadataFolder(eclipseMetadata);
//		spooner2.setSourceFolder(srcTest);
//		spooner2.setProcessors("bcu.nopol.processor.TestLinerAll","bcu.nopol.processor.ExpectedRemover");
//		spooner2.setOutputFolder(OUTPUT_PROJECT_PATH+"/"+srcTest);
//		spooner2.spoon();
	}
	
	/** same as expe2 but transforms from junit3 to junit4 (for lang) */
	public void expe4() throws Throwable {
		ISpooner spooner = new DefaultSpooner();
		spooner.setEclipseProject(ORIG_ECLIPSE_PROJECT_NAME);
		spooner.setEclipseMetadataFolder(eclipseMetadata);
		System.out.println(ORIG_ECLIPSE_PROJECT_NAME);
		System.out.println(eclipseMetadata);
		spooner.setSourceFolder(new String[]{srcJava});
		spooner.setProcessors("bcu.nopol.processor.IfProcessorAll");
		spooner.setOutputFolder(OUTPUT_PROJECT_PATH+"/"+srcJava);
		spooner.spoon();

		
		spooner = new DefaultSpooner();
		spooner.setEclipseProject(ORIG_ECLIPSE_PROJECT_NAME);
		spooner.setEclipseMetadataFolder(eclipseMetadata);
		spooner.setSourceFolder(new String[]{srcTest});
		spooner.setProcessors("bcu.nopol.processor.Junit3ToJunit4","bcu.nopol.processor.AddCallToNewMethod","bcu.nopol.processor.TestLinerAll");
		spooner.setOutputFolder(OUTPUT_PROJECT_PATH+"/"+srcTest);
		spooner.spoon();
	}

	public static List<StackTraceElement> getCurrentCuts() {
		return cuts;
	}
	
}
