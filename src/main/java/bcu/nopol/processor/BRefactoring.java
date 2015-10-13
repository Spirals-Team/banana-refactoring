package bcu.nopol.processor;

import org.junit.runner.RunWith;

import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import bcornu.resi.annot.BananaRefactoringRunner;

public class BRefactoring {
	public static void annotateWithNewRunner(CtClass klass) {
		if (klass.getAnnotation(RunWith.class)==null) {
			CtAnnotation annotation = klass.getFactory().Core().createAnnotation();
			annotation.setAnnotationType(klass.getFactory().Type().createReference(
					RunWith.class));
			annotation.addValue("value", BananaRefactoringRunner.class);
			klass.addAnnotation(annotation);
		}
	}


}
