package bcu.nopol.processor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.FixMethodOrder;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtAssignment;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCodeSnippetStatement;
import spoon.reflect.code.CtFieldAccess;
import spoon.reflect.code.CtForEach;
import spoon.reflect.code.CtLocalVariable;
import spoon.reflect.code.CtStatement;
import spoon.reflect.code.CtVariableAccess;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtField;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.declaration.ModifierKind;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.visitor.Filter;
import bcornu.resi.annot.BananaRefactoringRunner;
import bcornu.resi.annot.DontRunAfter;
import bcornu.resi.annot.DontRunBefore;
import bcornu.resi.annot.OldAfter;
import bcornu.resi.annot.OldBefore;
import bcornu.resi.annot.RunAfter;
import bcornu.resi.annot.RunBefore;
import bcu.nopol.main.Launcher;


@SuppressWarnings({"rawtypes","unchecked"})
public class MethodCutterProcessor extends AbstractProcessor<CtMethod> {

	private List<StackTraceElement> cuts = null;
	private int skippedLine=0;
	private int skippedField=0;
	
	@Override
	public void processingDone() {
		super.processingDone();
		System.out.println("skipped");
		System.out.println(skippedLine+"line");
		System.out.println(skippedField+"field");
	}
	
	@Override
	public void init() {
		super.init();
		cuts=Launcher.getCurrentCuts();
		if(cuts==null){
			throw new IllegalArgumentException("Launcher fields cuts must be initialized first");
		}
	}
	
	@Override
	public void process(final CtMethod element) {
		
		CtClass parent = null;
		try{
			parent = element.getParent(CtClass.class);
		}catch(Exception e){
			return;
		}
		
		if (element.getAnnotation(org.junit.Test.class)==null) {return;}

		BRefactoring.annotateWithNewRunner(parent);
	
		// if it's a test expecting an exception with expected in annotation, don't cut it
		CtAnnotation testAnnot = null;
		for (CtAnnotation ctAnnot : element.getAnnotations()) {
			if (ctAnnot.getAnnotationType().getQualifiedName()
					.equals("org.junit.Test"))
				testAnnot = ctAnnot;
		}
		if (testAnnot != null) {
			Map<String, Object> tmp = new HashMap<String, Object>(
					testAnnot.getElementValues());
			for (String key : tmp.keySet()) {
				if (key.equals("expected")) {
					addRunBefore(element);
					addRunAfter(element);
					return;
				}
			}
		}
				
		boolean toBeCut = false;
		Set<Integer> linestmp = new TreeSet<Integer>();
		for (StackTraceElement ste : cuts) {
			if(element.getSimpleName().equals(ste.getMethodName()) && parent.getActualClass().getCanonicalName().equals(ste.getClassName())){
				toBeCut=true;
				linestmp.add(ste.getLineNumber());
			}
		}
		if(!toBeCut) {
			addRunBefore(element);
			addRunAfter(element);
			return;
		};
		
		CtFieldReference annotArg = getFactory().Core().createFieldReference();
		annotArg.setDeclaringType(getFactory().Type().createReference(MethodSorters.class));
		annotArg.setSimpleName("NAME_ASCENDING");
		
		Map<String,Object> values = new HashMap<>();
		values.put("value", annotArg);
		
		CtAnnotation annot = getFactory().Core().createAnnotation();
		annot.setAnnotationType(getFactory().Type().createReference(FixMethodOrder.class));
		annot.setElementValues(values);
		if(!parent.getAnnotations().contains(annot))
			parent.addAnnotation(annot);
		
		List<CtStatement> statements = element.getBody().getStatements();
		int currentCut = 0;
		int previousCut =0;
		Integer[] lines = linestmp.toArray(new Integer[0]);
		int cutLine = lines[currentCut];
		

		List<CtLocalVariable> localVar = element.getElements(new Filter<CtLocalVariable>() {
			@Override
			public boolean matches(CtLocalVariable element) {
				return !element.getSimpleName().equals("this") && !element.getSimpleName().equals("super");
			}
		});

		for (CtLocalVariable statement : localVar) {
			if(statement.getParent() instanceof CtCatch)continue;
			CtField field = getFactory().Core().createField();
			field.setSimpleName(((CtLocalVariable) statement).getSimpleName()+"_"+element.getSimpleName());
			field.setType(((CtLocalVariable) statement).getType());
			
			// the field has to be static to persistover calls
			field.addModifier(ModifierKind.STATIC);
			parent.addField(field);
			
			if(((CtLocalVariable) statement).getDefaultExpression()!=null){
				
				CtFieldReference ref = getFactory().Core().createFieldReference();
				ref.setSimpleName(((CtLocalVariable) statement).getSimpleName()+"_"+element.getSimpleName());
				ref.setType(((CtLocalVariable) statement).getType());
				
				CtFieldAccess access = getFactory().Core().createFieldWrite();
				access.setVariable(ref);
				access.setType(ref.getType());
				
				CtAssignment assign = getFactory().Core().createAssignment();
				assign.setAssigned(access);
				assign.setAssignment(((CtLocalVariable) statement).getDefaultExpression());
				
				((CtLocalVariable) statement).getDefaultExpression().setParent(assign);
				
				statement.replace(assign);
			}
		}

		List<CtVariableAccess> accesses = element.getElements(new Filter<CtVariableAccess>() {
			@Override
			public boolean matches(CtVariableAccess element) {
				return !(element instanceof CtFieldAccess);
			}
		});
		
		for (CtVariableAccess variableAccess : accesses) {
			boolean extracted = false;
			for (CtField fields : (List<CtField>)parent.getFields()) {
				try{//BCUTAG here too
					if(fields.getSimpleName().equals(variableAccess.getVariable().getSimpleName()+"_"+element.getSimpleName()))
						extracted=true;
				}catch(NullPointerException npe){
					skippedField++;
					continue;
				}
			}
			
			// if we are refereering to a foreach variable, do nothing
			if (variableAccess.getVariable().getDeclaration() != null && variableAccess.getVariable().getDeclaration().getParent() instanceof CtForEach) {
				continue;
			}
			if(!extracted)continue;
			CtFieldReference ref = getFactory().Core().createFieldReference();
			ref.setSimpleName(variableAccess.getVariable().getSimpleName()+"_"+element.getSimpleName());
			ref.setType(variableAccess.getVariable().getType());
			
			CtFieldAccess field = getFactory().Core().createFieldWrite();
			field.setVariable(ref);
			field.setType(ref.getType());
			field.setTypeCasts(variableAccess.getTypeCasts());
			
			variableAccess.replace(field);
		}
		
		List<CtMethod> lm = new ArrayList<CtMethod>();
		
		for (int i = 1; i < statements.size(); i++) {
			int currentStatLine = 0;
			try{//BCUTAG bon courage
				// in some cases there is no position or a line and a npe is thrown
				currentStatLine = statements.get(i).getPosition().getLine();
			}catch(NullPointerException npe){
				skippedLine++;
				continue;
			}
			if(currentStatLine>=cutLine){
				List<CtStatement> stats = statements.subList(previousCut, i);
				previousCut = i;
				
				CtBlock b = getFactory().Core().createBlock();
				b.setStatements(stats);
				
				CtMethod m = getFactory().Core().clone(element);
				m.setSimpleName(element.getSimpleName()+"_"+getRounded(currentCut));
				m.setBody(b);
				parent.addMethod(m);
				lm.add(m);
				if(lines.length>currentCut+1)
					cutLine = lines[++currentCut];
				else break;
			}
		}
		
		if(previousCut<statements.size()){
			List<CtStatement> stats = statements.subList(previousCut, statements.size());
			
			CtBlock b = getFactory().Core().createBlock();
			b.setStatements(stats);
			
			CtMethod m = getFactory().Core().clone(element);
			m.setSimpleName(element.getSimpleName()+"_"+ getRounded(++currentCut));
			m.setBody(b);
			parent.addMethod(m);
			lm.add(m);
		}

		
		addRunBefore(lm.get(0));		
		addRunAfter(lm.get(lm.size()-1));		

		parent.removeMethod(element);
	}

	private void addRunBefore(final CtMethod element) {
		CtAnnotation annotation = getFactory().Core().createAnnotation();
		annotation.setAnnotationType(getFactory().Type().createReference(
				RunBefore.class));
		element.addAnnotation(annotation);
	}

	private void addRunAfter(final CtMethod element) {
		CtAnnotation annotation = getFactory().Core().createAnnotation();
		annotation.setAnnotationType(getFactory().Type().createReference(
				RunAfter.class));
		element.addAnnotation(annotation);
	}
	private String getRounded(int i) {
		if(i<10)return "00"+i;
		if(i<100) return "0"+i;
		return ""+i;
	}

}
