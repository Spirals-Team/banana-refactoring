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

	
	static boolean consider(CtMethod element) {
		if(element.getBody()==null)return false;
		CtClass parent = null;
		try{
			parent = element.getParent(CtClass.class);
		}catch(Exception e){
			return false;
		}
		if(element.getAnnotations()==null) return false;
		for (CtAnnotation annot : element.getAnnotations()) {
			if(annot.getAnnotationType().getQualifiedName().equals("org.junit.Test"))
				return true;
		}
		return false;
	}
	
	@SuppressWarnings({ "unchecked"})
	@Override
	public void process(CtMethod element) {
//		if(!isTest){
//			if(element.getSimpleName().startsWith("test") && element.hasModifier(ModifierKind.PUBLIC) && !parent.hasModifier(ModifierKind.ABSTRACT))
//				isTest=true;
//		}
		if(!consider(element))return;
		
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
