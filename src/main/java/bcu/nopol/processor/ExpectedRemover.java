package bcu.nopol.processor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeSet;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtInvocation;
import spoon.reflect.code.CtTry;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;
import spoon.reflect.visitor.filter.TypeFilter;

@SuppressWarnings("rawtypes")
/** removes the expected exceptions */
public class ExpectedRemover extends AbstractProcessor<CtMethod> {

	@SuppressWarnings({ "unchecked" })
	@Override
	public void process(CtMethod element) {
		if (element.getBody() == null)
			return;
		CtClass parent = null;
		try {
			parent = element.getParent(CtClass.class);
		} catch (Exception e) {
			return;
		}
		if (element.getAnnotations() == null)
			return;
		CtAnnotation annot = null;
		for (CtAnnotation ctAnnot : element.getAnnotations()) {
			if (ctAnnot.getAnnotationType().getQualifiedName()
					.equals("org.junit.Test"))
				annot = ctAnnot;
		}
		if (annot == null)
			return;
		Map<String, Object> tmp = new HashMap<String, Object>(
				annot.getElementValues());
		for (String key : tmp.keySet()) {
			if (key.equals("expected")) {
				Object tmp2 = tmp.get(key);

				CtTypeReference expectedtype = null;
				if (tmp2 instanceof CtTypeReference) {
					expectedtype = (CtTypeReference) tmp2;
				} else if (tmp2 instanceof CtFieldReference) {
					expectedtype = ((CtFieldReference) tmp2).getDeclaringType();
				}

				CtCatchVariable e = getFactory().Core().createCatchVariable();
				e.setSimpleName("e");
				// We catch all exceptions
				e.setType(expectedtype);

				CtBlock catchBlock = getFactory().Core().createBlock();

				CtCatch c = getFactory().Core().createCatch();
				c.setParameter(e);
				c.setBody(catchBlock);

				boolean thrownExceptionCanBeFound = isRuntimeException(expectedtype);
				if (thrownExceptionCanBeFound) {
					System.err.println(expectedtype.toString() +" is runtime ");					
				}

				for (CtInvocation<?> inv : element.getBody().getElements(
						new TypeFilter<>(CtInvocation.class))) {
					if (inv.getExecutable().getDeclaration() == null)
						continue;
					for (CtTypeReference thrown : inv.getExecutable()
							.getDeclaration().getThrownTypes()) {
						if (expectedtype.isAssignableFrom(thrown)) {
							thrownExceptionCanBeFound = true;
						}
					}
				}
				if (!thrownExceptionCanBeFound) {
					System.err.println("cannot throw "+expectedtype.toString());					
				}
				
				expectedtype.setSimpleName("java.lang.Throwable") ;expectedtype.setPackage(null);
				{
					CtTry t = getFactory().Core().createTry();
					t.setBody(element.getBody());
					t.setCatchers(Arrays.asList(new CtCatch[] { c }));

					CtBlock b = getFactory().Core().createBlock();
					b.addStatement(t);

					element.setBody(b);
				}

				tmp.remove(key);
				annot.setElementValues(tmp);
				return;
			}
		}
	}

	
	
	private boolean isRuntimeException(CtTypeReference type) {
		System.err.println("isruntime? of"+type.toString());
		if (type.getActualClass() != null) {
			if (RuntimeException.class.isAssignableFrom(type.getActualClass())) {
				return true;
			}
			if (Error.class.isAssignableFrom(type.getActualClass())) {
				System.out.println(type.getActualClass().toString()+" instanceof Error");
				return true;
			}
		} else if (type.getDeclaration() != null) {
			if ("java.lang.Exception".equals(type.getDeclaration().getSuperclass().toString())) {
				return false;
			}
			if ("java.lang.Exception".equals(type.getDeclaration().getSuperclass().toString())) {
				return false;
			}
			return isRuntimeException(type.getDeclaration().getSuperclass());
		}
		return false;
	}

}
