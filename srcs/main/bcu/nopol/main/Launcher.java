package bcu.nopol.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import sacha.impl.DefaultSpooner;
import sacha.impl.TestRunnerCore;
import sacha.interfaces.ISpooner;
import sacha.interfaces.ITestResult;
import bcu.nopol.control.IfController;
import bcu.nopol.control.IfUsage;

public class Launcher {
	
	// set here this to your own path
	public static final String WORKSPACE_METADATA_PATH = "/home/martin/workspace/.metadata";
	public static final String ECLIPSE_PROJECT_NAME = "spojo-core-refactored";
	public static final String OUTPUT_PROJECT_PATH = "/home/martin/workspace/spojo-core-refactored";
	
	
	private List<StackTraceElement> cuts = null;
	private String eclipseMetadata = WORKSPACE_METADATA_PATH;
	private String eclipseProjectName;
	private String outputProjectPath;
	private static Launcher launch;


	private final String projectName = "spojo-core";

	private final String srcJava = "src/main/java";
	private String srcTest = "src/test/java";
//	private final String projectName = "math-309";
//
//	private final String ifClass = "org.apache.commons.math.random.RandomDataImpl";
//	private int ifLine = 465;
//	
//	private final String srcJava = "src/main/java";
//	private String srcTest = "src/test/java";
		
	public Launcher(String[] args) {
	}

	public static void main(String[] args) throws Throwable {
		launch = new Launcher(args);
		launch.eclipseProjectName = ECLIPSE_PROJECT_NAME;
		launch.outputProjectPath = OUTPUT_PROJECT_PATH;
		if(args.length==0 || args[0].equals("-1"))
			launch.expe1();
		else if(args[0].equals("-2"))
			launch.expe2();
		else if(args[0].equals("-3"))
			launch.expe3();
		else if(args[0].equals("-4"))
			launch.expe4();
	}

	
	private void expe1() throws Throwable {
		ISpooner spooner = new DefaultSpooner();
		spooner.setEclipseProject(projectName);
		spooner.setEclipseMetadataFolder(eclipseMetadata);
		spooner.setSourceFolder(new String[]{srcJava});
		spooner.setProcessors("bcu.nopol.processor.IfProcessorAll");
		spooner.setOutputFolder(outputProjectPath+"/"+srcJava);
		spooner.spoon();
		
		spooner = new DefaultSpooner();
		spooner.setEclipseProject(projectName);
		spooner.setEclipseMetadataFolder(eclipseMetadata);
		spooner.setSourceFolder(new String[]{srcTest});
		spooner.setProcessors("bcu.nopol.processor.TestLinerAll");
		spooner.setOutputFolder(outputProjectPath+"/"+srcTest);
		spooner.spoon();
	}
	
	private void expe2() throws Throwable {
		TestRunnerCore runner = new TestRunnerCore();
		runner.setEclipseMetadataFolder(eclipseMetadata);
		runner.setEclipseProject(eclipseProjectName);
		System.out.println("searching for tests in "+outputProjectPath+"/"+srcTest);
		ITestResult result= runner.runAllTestsInDirectory(outputProjectPath+"/"+srcTest);
		
		Map<Integer, Map<String, Map<String, Map<Integer, IfUsage>>>> ifMap2 = IfController.getIfMap();
		Set<Integer> impureOnce = new TreeSet<>();
		Set<Integer> pureOnce = new TreeSet<>();
		Set<Integer> impureAlways = new TreeSet<>();
		Set<Integer> pureAlways = new TreeSet<>();
		cuts = new ArrayList<>();
		int impureConst = 0;
		List<String> spoonedFiles = new ArrayList<>();
		File cutsPerTest = new File(outputProjectPath+"/cutsPerIf");
		if(!cutsPerTest.exists())
			if(!cutsPerTest.createNewFile())
				System.err.println("cannot write MEDIAN FILE");
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(cutsPerTest)));

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
		
		ISpooner spooner2 = new DefaultSpooner();
		spooner2.setEclipseProject(projectName);
		spooner2.setEclipseMetadataFolder(eclipseMetadata);
		spooner2.setSourceFolder(spoonedFiles.toArray(new String[0]));
		spooner2.setProcessors("bcu.nopol.processor.TestRuleAdder","bcu.nopol.processor.MethodCutterProcessor");
		spooner2.setOutputFolder(outputProjectPath+"/"+srcTest);
		spooner2.spoon();
		
		System.out.println("test always pure :"+purTest.size());
		System.out.println("test absol impure :"+impureTests.size());
		System.out.println("number of const :"+IfController.getConstNumberRetenue()+":"+IfController.getConstNumber());
		System.out.println("number of impure const: "+impureConst+ " =? "+impureConsts.size());

		System.out.println("----------------------------");
		System.out.println(projectName+"&"+result.getNbRunTests()+
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
		
}
	
	private void expe3() throws Throwable {
		ISpooner spooner2 = new DefaultSpooner();
		spooner2.setEclipseProject(eclipseProjectName);
		spooner2.setEclipseMetadataFolder(eclipseMetadata);
		spooner2.setSourceFolder(srcTest);
		spooner2.setProcessors("bcu.nopol.processor.TestLinerAll","bcu.nopol.processor.ExpectedRemover");
		spooner2.setOutputFolder(outputProjectPath+"/"+srcTest);
		spooner2.spoon();
	}
	
	private void expe4() throws Throwable {
		TestRunnerCore runner = new TestRunnerCore();
		runner.setEclipseMetadataFolder(eclipseMetadata);
		runner.setEclipseProject(eclipseProjectName);
		runner.runAllTestsInDirectory(outputProjectPath+"/"+srcTest);
		
		Map<Integer, Map<String, Map<String, Map<Integer, IfUsage>>>> ifMap2 = IfController.getIfMap();
		Set<Integer> impureOnce = new TreeSet<>();
		Set<Integer> pureOnce = new TreeSet<>();
		Set<Integer> impureAlways = new TreeSet<>();
		Set<Integer> pureAlways = new TreeSet<>();
		cuts = new ArrayList<>();
		List<String> spoonedFiles = new ArrayList<>();
		for (Integer ifNumber : ifMap2.keySet()) {
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
							tmp=currentValue;
							cuts.add(new StackTraceElement(className, methodName, "atLine", line));
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
						}
					}
					if(pure){
						pureOnce.add(ifNumber);
						alwaysImpure = false;
					}
				}
				spoonedFiles.add(srcTest+"/"+className.replaceAll("\\.", "/")+".java");
			}
			if(alwaysImpure)
				impureAlways.add(ifNumber);
			if(alwaysPure)
				pureAlways.add(ifNumber);
		}
		System.out.println("exec ifs: "+ifMap2.keySet().size());
		System.out.println("pure once ifs: "+pureOnce.size());
		System.out.println("purely cov ifs: "+pureAlways.size());
		System.out.println("impurely cov ifs: "+impureAlways.size());
		System.out.println("impure once ifs: "+impureOnce.size());
	}

	public static List<StackTraceElement> getCurrentCuts() {
		return launch.cuts;
	}
	
}
