package bcu.nopol.processor;

import java.util.Arrays;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtStatement;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtExecutableReference;

@SuppressWarnings("rawtypes")
public class TestLinerAll extends AbstractProcessor<CtMethod> {

	
	@SuppressWarnings({ "unchecked"})
	@Override
	public void process(CtMethod element) {
		if(element.getBody()==null)return;
		CtClass parent = null;
		try{
			parent = element.getParent(CtClass.class);
		}catch(Exception e){
			return;
		}
		if(element.getAnnotations()==null)return;
		boolean isTest = false;
		for (CtAnnotation annot : element.getAnnotations()) {
			if(annot.getAnnotationType().getQualifiedName().equals("org.junit.Test"))
				isTest=true;
		}
//		if(!isTest){
//			if(element.getSimpleName().startsWith("test") && element.hasModifier(ModifierKind.PUBLIC) && !parent.hasModifier(ModifierKind.ABSTRACT))
//				isTest=true;
//		}
		if(!isTest)return;
		
		CtLiteral methNum = getFactory().Core().createLiteral();
		methNum.setValue((parent.getPackage()!=null?(parent.getPackage().getQualifiedName()+"."):"")+parent.getSimpleName()+":"+element.getSimpleName());
		
		CtExecutableReference thenExecutedRef = getFactory().Core().createExecutableReference();
		thenExecutedRef.setDeclaringType(getFactory().Type().createReference("bcu.nopol.control.IfController"));
		thenExecutedRef.setSimpleName("newMethod");
		thenExecutedRef.setStatic(true);
		
		CtInvocation startMeth = getFactory().Core().createInvocation();
		startMeth.setExecutable(thenExecutedRef);
		startMeth.setArguments(Arrays.asList(new CtLiteral[]{methNum}));
		
		startMeth.setParent(element.getBody());
		element.getBody().insertBegin(startMeth);
		
		int nbStats = element.getBody().getStatements().size();
		int j=0;
		for (int i = 1; i<nbStats;i++) {
			CtLiteral lineNum = getFactory().Core().createLiteral();
			lineNum.setValue(((CtStatement) element.getBody().getStatements().get(i+j)).getPosition().getLine());
			
			CtExecutableReference newLineRef = getFactory().Core().createExecutableReference();
			newLineRef.setDeclaringType(getFactory().Type().createReference("bcu.nopol.control.IfController"));
			newLineRef.setSimpleName("newLine");
			newLineRef.setStatic(true);
			
			CtInvocation newLine = getFactory().Core().createInvocation();
			newLine.setExecutable(newLineRef);
			newLine.setArguments(Arrays.asList(new CtLiteral[]{lineNum}));
			
			((CtStatement) element.getBody().getStatements().get(i+j++)).insertBefore(newLine);
		}
	}
	

}
