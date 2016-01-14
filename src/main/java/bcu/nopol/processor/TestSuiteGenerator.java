package bcu.nopol.processor;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Test;
import org.junit.runner.Runner;
import org.junit.runners.BlockJUnit4ClassRunner;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.CtPackage;
import spoon.reflect.declaration.CtParameter;
import spoon.reflect.declaration.CtType;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.AnnotationFilter;
import bcornu.resi.annot.BananaRefactoringRunner;
import bcu.nopol.main.Launcher;

/** adds two new test classes that calls all tests.
* one uses the BananaRefactoring runner */
@SuppressWarnings("rawtypes")
public class TestSuiteGenerator extends AbstractProcessor<CtClass> {

	CtBlock body;
	
	@Override
	public void processingDone() {
		addStatement("java.util.List<String> l2 = new java.util.ArrayList<>();for (org.junit.runner.notification.Failure f:l) {l2.add(f.getDescription().toString());}if (l.size()>0) throw new AssertionError(l2.toString());");
        
		createClass(Launcher.ORIG_ECLIPSE_PROJECT_NAME+"_Test", body);
		System.out.println("done");
	}

	private void createClass(String klassName, CtBlock b) {
		CtClass c = getFactory().Core().createClass();
		c.addModifier(ModifierKind.PUBLIC);
		c.setSimpleName(klassName.replace('-', '_').replace('.', '_'));
		

//		// main
		{
		CtMethod m = getFactory().Core().createMethod();
		m.setSimpleName("main");
		m.addModifier(ModifierKind.STATIC);
		m.addModifier(ModifierKind.PUBLIC);
		m.setBody(b);		
		m.setType(getFactory().Type().VOID_PRIMITIVE);
		List<CtParameter> l = new ArrayList<>();
		CtParameter par = getFactory().Core().createParameter();
		par.setType((CtTypeReference) getFactory().Core().createTypeReference().setSimpleName("String[]"));
		par.setSimpleName("args");
		l.add(par);
		m.setParameters(l);
		c.addMethod(m);		
		// throws
		m.addThrownType((CtTypeReference) getFactory().Core().createTypeReference().setSimpleName("Exception"));		
		
		CtCodeSnippetStatement e = getFactory().Core().createCodeSnippetStatement ();
		e.setValue("new "+c.getSimpleName()+"().test()");
		CtBlock bod= getFactory().Core().createBlock();
		bod.addStatement(e);
		m.setBody(bod);
		}
	
		{
		CtMethod m = getFactory().Core().createMethod();
		m.setSimpleName("test");
		m.addModifier(ModifierKind.PUBLIC);
		getFactory().Annotation().annotate(m, Test.class);
		m.setBody(b);		
		m.addThrownType((CtTypeReference) getFactory().Core().createTypeReference().setSimpleName("Exception"));		
		m.setType(getFactory().Type().VOID_PRIMITIVE);
		c.addMethod(m);
		}
		
		// adding the class
		CtPackage p = getFactory().Package().getOrCreate("main");
		p.addType(c);

	}
	
	@Override
	public boolean isToBeProcessed(CtClass element) {
		return element.getElements(new AnnotationFilter<>((Class<? extends Annotation>) Test.class)).size()>0
				&& !element.getModifiers().contains(ModifierKind.ABSTRACT)
				&& element.isTopLevel();
	}
	
	private void addStatement(String val) 				{		
		CtCodeSnippetStatement e = getFactory().Core()
				.createCodeSnippetStatement();
		e.setValue(val);
		body.addStatement(e);					
	}
	@SuppressWarnings({ "unchecked"})
	@Override
	public void process(CtClass element) {
		try {
			
			
			if (body == null) {
				body = getFactory().Core().createBlock();
				addStatement("org.junit.runner.JUnitCore core  = new org.junit.runner.JUnitCore()");
				addStatement("org.junit.runner.Result r");
				addStatement("java.util.List<org.junit.runner.notification.Failure> l = new java.util.ArrayList<>()");
			}
			
			CtCodeSnippetStatement e = createTestSnippet(element, BananaRefactoringRunner.class);
			body.addStatement(e);
			

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private CtCodeSnippetStatement createTestSnippet(CtClass element, Class<? extends Runner> runner) {
		CtCodeSnippetStatement e = getFactory().Core()
				.createCodeSnippetStatement();
		String val = "r = core.run"+"("
				+ element.getQualifiedName()
				+ ".class); l.addAll(r.getFailures());";
		e.setValue(val);
		return e;
	}
}
