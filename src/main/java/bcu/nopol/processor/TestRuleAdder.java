package bcu.nopol.processor;

import java.util.Arrays;
import java.util.Set;

import junit.framework.TestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;

import bcornu.resi.annot.DontRunAfter;
import bcornu.resi.annot.RunJunitRule;
import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtConstructorCall;
import spoon.reflect.code.CtNewClass;
import spoon.reflect.code.CtThisAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;

@SuppressWarnings("all") 
public class TestRuleAdder extends AbstractProcessor<CtClass> {

	@Override
	public void process(CtClass element) {
		Set<CtMethod> methods = ((Set<CtMethod>) element.getAllMethods());
		Object a = null;
		for (CtMethod method : methods) {
			if(a!=null)
				break;
			a = method.getAnnotation(Test.class);
			if (a == null)
				a = method.getAnnotation(Before.class);
			if (a == null)
				a = method.getAnnotation(After.class);
		}
		if(a==null){
			try{
				if(TestCase.class.isAssignableFrom(element.getActualClass()))
					a="";
			}catch(Throwable cnfe){
				return;
			}
		}
		if(a==null)return;
		
		CtAnnotation annotation = getFactory().Core().createAnnotation();
		annotation.setAnnotationType(getFactory().Type().createReference(Rule.class));
		
		CtConstructorCall initRunner = getFactory().Core().createConstructorCall();
		initRunner.setType(getFactory().Type().createReference(RunJunitRule.class));
		initRunner.setArguments(Arrays.asList(new CtThisAccess[]{getFactory().Core().createThisAccess()}));
		
		CtField f = getFactory().Core().createField();
		f.setType(getFactory().Type().createReference(RunJunitRule.class));
		f.setSimpleName("runJunitRule");
		f.setDefaultExpression(initRunner);
		f.addAnnotation(annotation);
		f.addModifier(ModifierKind.PUBLIC);
		
		element.addField(f);
	}
}
