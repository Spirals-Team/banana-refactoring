package bcu.nopol.processor;

import java.util.Arrays;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtExecutableReference;

@SuppressWarnings("rawtypes")
public class Junit3ToJunit4 extends AbstractProcessor<CtMethod> {

	
	@Override
	public boolean isToBeProcessed(CtMethod element) {
		if(element.getBody()==null)return false;
		CtClass parent = null;
		try{
			parent = element.getParent(CtClass.class);
		}catch(Exception e){
			return false;
		}
		if (element.getAnnotation(org.junit.After.class)==null) {return false;}
		if (element.getAnnotation(org.junit.Before.class)==null) {return false;}
		if (element.getAnnotation(org.junit.Test.class)==null) {return  false;}

		return true;
	}
	
	@SuppressWarnings({ "unchecked"})
	@Override
	public void process(CtMethod element) {
		
		if (element.getSimpleName().startsWith("test")) {
			CtAnnotation annotation = element.getFactory().Core().createAnnotation();
			annotation.setAnnotationType(element.getFactory().Type().createReference(
					RunWith.class));
			element.addAnnotation(annotation);
		}
		if (element.getSimpleName().startsWith("setup")) {
			CtAnnotation annotation = element.getFactory().Core().createAnnotation();
			annotation.setAnnotationType(element.getFactory().Type().createReference(
					Before.class));
			element.addAnnotation(annotation);
		}

		if (element.getSimpleName().startsWith("tearDown")) {
			CtAnnotation annotation = element.getFactory().Core().createAnnotation();
			annotation.setAnnotationType(element.getFactory().Type().createReference(
					After.class));
			element.addAnnotation(annotation);
		}

	}
	

}
