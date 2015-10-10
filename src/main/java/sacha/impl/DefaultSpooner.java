package sacha.impl;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import sacha.classloader.enrich.EnrichableClassloader;
import sacha.interfaces.ISpooner;
import spoon.Launcher;

public class DefaultSpooner extends AbstractConfigurator implements ISpooner {

	public static void main(String[] args) throws Exception {
		Launcher.main(new String[]{"-h"});
	}
	private String outputFolder = null;
	private String[] processors = null;
	private String[] sources = null;
	private boolean graphical = false;
	
	
	@Override
	public void setOutputFolder(String folderPath) {
		outputFolder=folderPath;
	}

	@Override
	public void setProcessors(String... processorNames) {
		processors=processorNames;
	}

	@Override
	public void setSourceFolder(String... sources) {
		this.sources = sources;
	}
	
	@Override
	public void setGraphicalOutput(boolean b) {
		graphical = b;
	}


	@Override
	public void spoon() {
		EnrichableClassloader eClassloader = getEnrichableClassloader();

		Thread.currentThread().setContextClassLoader(eClassloader);
		
		List<String> args = new ArrayList<>();
		args.add("-v");
		if(graphical)
			args.add("-g");
		args.add("--compliance");
		args.add("7");
		if(sources == null || sources.length==0)
			throw new IllegalArgumentException("you have to use setSourceFolder before");
		args.add("-i");
		String tmp = "";
		for (String string : sources) {
			if(string==null || string.length()==0)
				throw new IllegalArgumentException("setSourceFolder can not be used with empty value");
			tmp+=getProjectDir().getAbsolutePath()+"/"+string+":";
		}
		tmp = tmp.substring(0, tmp.length()-1);
		args.add(tmp);
		
		//BCUTAG classpath?
		args.add("--source-classpath");
		String tmp2 = "";
		for (URL url : eClassloader.getURLs()) {
			tmp2+=url.getPath()+":";
		}
		tmp2 = tmp2.substring(0, tmp2.length()-1);
		args.add(tmp2);
		
		
		args.add("-r");
//		args.add("-x");
		if(processors != null && processors.length>0){
			args.add("-p");
			tmp = "";
			for (String string : processors) {
				if(string==null || string.length()==0)
					throw new IllegalArgumentException("setProcessors can not be used with empty value");
				tmp+=string+":";
			}
			tmp = tmp.substring(0, tmp.length()-1);
			args.add(tmp);
		}

		if(outputFolder != null){
			args.add("-o");
			if(outputFolder==null || outputFolder.length()==0)
				throw new IllegalArgumentException("setOutputFolder can not be used with empty value");
			args.add(outputFolder);
		}
		
		try {
			System.out.println("spoon "+args);
			Launcher.main(args.toArray(new String[]{}));
		} catch (Exception t) {
			throw t instanceof RuntimeException?(RuntimeException)t:new RuntimeException(t);
		}
	}

}
