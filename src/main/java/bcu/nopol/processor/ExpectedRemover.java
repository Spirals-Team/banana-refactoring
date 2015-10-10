package bcu.nopol.processor;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import spoon.processing.AbstractProcessor;
import spoon.reflect.code.CtBlock;
import spoon.reflect.code.CtCatch;
import spoon.reflect.code.CtCatchVariable;
import spoon.reflect.code.CtTry;
import spoon.reflect.declaration.CtAnnotation;
import spoon.reflect.declaration.CtClass;
import spoon.reflect.declaration.CtMethod;
import spoon.reflect.reference.CtFieldReference;
import spoon.reflect.reference.CtTypeReference;

@SuppressWarnings("rawtypes")
/** removes the expected exceptions */
public class ExpectedRemover extends AbstractProcessor<CtMethod> {

	
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
		CtAnnotation annot = null;
		for (CtAnnotation ctAnnot : element.getAnnotations()) {
			if(ctAnnot.getAnnotationType().getQualifiedName().equals("org.junit.Test"))
				annot=ctAnnot;
		}
		if(annot==null)return;
		Map<String,Object> tmp = new HashMap<String, Object>(annot.getElementValues());
		for (String key : tmp.keySet()) {
			if(key.equals("expected")){
				Object tmp2 = tmp.get(key);
				CtTypeReference type = null;
				if(tmp2 instanceof CtTypeReference){
					type=(CtTypeReference) tmp2;
				}else if(tmp2 instanceof CtFieldReference){
					type= ((CtFieldReference) tmp2).getDeclaringType();
				}
				
				CtCatchVariable e = getFactory().Core().createCatchVariable();
				e.setSimpleName("e");
				e.setType(type);
				
				CtBlock catchBlock = getFactory().Core().createBlock();

				CtCatch c = getFactory().Core().createCatch();
				c.setParameter(e);
				c.setBody(catchBlock);
				
				CtTry t = getFactory().Core().createTry();
				t.setBody(element.getBody());
				t.setCatchers(Arrays.asList(new CtCatch[]{c}));

				CtBlock b = getFactory().Core().createBlock();
				b.addStatement(t);
				
				element.setBody(b);
				
				tmp.remove(key);
				annot.setElementValues(tmp);
				return;
			}
		}
	}
	

}
