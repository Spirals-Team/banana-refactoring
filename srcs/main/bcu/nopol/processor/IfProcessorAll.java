package bcu.nopol.processor;

import java.util.Arrays;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtIf;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtLiteral;
import spoon.reflect.code.CtStatement;
import spoon.reflect.reference.CtExecutableReference;

@SuppressWarnings("rawtypes")
public class IfProcessorAll extends AbstractProcessor<CtIf> {

	int ifs = 0;
	
	@Override
	public void process(CtIf element) {
		ifs++;
		CtLiteral tryNum = getFactory().Core().createLiteral();
		tryNum.setValue(ifs);
		
		CtExecutableReference thenExecutedRef = getFactory().Core().createExecutableReference();
		thenExecutedRef.setDeclaringType(getFactory().Type().createReference("bcu.nopol.control.IfController"));
		thenExecutedRef.setSimpleName("thenExecuted");
		thenExecutedRef.setStatic(true);
		
		CtInvocation thenExecuted = getFactory().Core().createInvocation();
		thenExecuted.setExecutable(thenExecutedRef);
		thenExecuted.setArguments(Arrays.asList(new CtLiteral[]{tryNum}));
		
		CtStatement then = element.getThenStatement();
		if(then instanceof CtBlock){
			((CtBlock)then).insertBegin(thenExecuted);
		}else{
			CtBlock tmp = getFactory().Core().createBlock();
			tmp.addStatement(thenExecuted);
			tmp.addStatement(then);
			element.setThenStatement(tmp);
		}
		
		CtExecutableReference elseExecutedRef = getFactory().Core().createExecutableReference();
		elseExecutedRef.setDeclaringType(getFactory().Type().createReference("bcu.nopol.control.IfController"));
		elseExecutedRef.setSimpleName("elseExecuted");
		elseExecutedRef.setStatic(true);
		
		CtInvocation elseExecuted = getFactory().Core().createInvocation();
		elseExecuted.setExecutable(elseExecutedRef);
		elseExecuted.setArguments(Arrays.asList(new CtLiteral[]{tryNum}));
		
		CtStatement elseStat = element.getElseStatement();
		if(elseStat==null){
			element.setElseStatement(elseExecuted);
		}else if(elseStat instanceof CtBlock){
			((CtBlock)elseStat).insertBegin(elseExecuted);
		}else{
			CtBlock tmp = getFactory().Core().createBlock();
			tmp.addStatement(elseExecuted);
			tmp.addStatement(elseStat);
			element.setElseStatement(tmp);
		}
		
	}
	
	@Override
	public void processingDone() {
		super.processingDone();
		System.out.println("ifs: "+ifs);
	}

}
