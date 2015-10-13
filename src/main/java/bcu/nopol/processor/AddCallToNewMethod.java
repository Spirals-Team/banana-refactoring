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
public class AddCallToNewMethod extends AbstractProcessor<CtMethod> {

	
	@SuppressWarnings({ "unchecked"})
	@Override
	public void process(CtMethod element) {
		if(!TestLinerAll.consider(element))return;
		CtClass parent = element.getParent(CtClass.class);
		
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
	}
	

}
