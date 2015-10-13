package bcornu.resi.annot;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

import com.sun.xml.internal.bind.v2.util.FatalAdapter;

public class RunJunitRule implements TestRule {

	private static TestClass fTestClass;
	private static Object testThis;
	
	public RunJunitRule(Object o) {
		testThis = o;
		fTestClass = new TestClass(o.getClass());
	}

	class Nothing extends Statement {

		private String s;
		Nothing(String s) {this.s=s;}
		@Override
		public void evaluate() throws Throwable {
			System.out.println(s+ " (nothing)");
		}
		
	}
	
	@Override
	public Statement apply(final Statement base, final Description desc) {
		//System.out.println(base.getClass().getSimpleName()+" "+desc.getDisplayName());
		if (desc.getAnnotation(DontRunBefore.class) != null 
				&& desc.getAnnotation(DontRunAfter.class) != null 
				) {			
		return new Nothing("nothing for "+desc.getDisplayName());
		}
		
		// skip before if necessary
		if (desc.getAnnotation(DontRunBefore.class) == null 
				
				) {
			for (FrameworkMethod fm : fTestClass.getAnnotatedMethods(OldBefore.class)) {
				try {
					fm.invokeExplosively(testThis);
				} catch (Throwable e) {
					throw new RuntimeException(e);
				}
			}

		}
		
		if (desc.getAnnotation(DontRunAfter.class) == null 
				) {			
//				return new Nothing("dd");
				return new Statement() {
				
				@Override
				public void evaluate() throws Throwable {
					List<FrameworkMethod> annotatedMethods = fTestClass.getAnnotatedMethods(OldAfter.class);
					for (FrameworkMethod fm : annotatedMethods) {
						fm.invokeExplosively(testThis);
					}
				}
			};
		}

		return base;
	}

}
