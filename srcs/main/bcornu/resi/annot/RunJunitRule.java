package bcornu.resi.annot;

import java.lang.reflect.Field;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.internal.runners.statements.RunBefores;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;

public class RunJunitRule implements TestRule {

	private static String oldClassName = null;
	private static TestClass fTestClass;
	private static List<FrameworkMethod> befores;
	private static Object testThis;
	private static Object oldThis;
	private static List<FrameworkMethod> afters;
	private static String oldMethodName;

	public RunJunitRule(Object o) {
//		oldThis=testThis;
		testThis = o;
		fTestClass = new TestClass(o.getClass());
		befores = fTestClass.getAnnotatedMethods(OldBefore.class);
		afters = fTestClass.getAnnotatedMethods(OldAfter.class);
	}

	@Override
	public Statement apply(final Statement base, final Description desc) {
		
		if(desc.getClassName()==oldClassName && desc.getMethodName().matches(".*_[0-9]{3}$") && desc.getMethodName().substring(0,desc.getMethodName().length()-4).equals(oldMethodName)){
			testThis=oldThis;
		}else
			oldThis=testThis;
		oldMethodName = desc.getMethodName().substring(0,desc.getMethodName().length()-4);
		oldClassName=desc.getClassName();
		
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				if (desc.getAnnotation(DontRunBefore.class) == null) {
					for (FrameworkMethod fm : befores) {
						fm.invokeExplosively(testThis);
					}
				}
//				InvokeMethod im = null;
				try{
					if(base instanceof RunBefores){
						Field f = base.getClass().getDeclaredField("fBefores");
						f.setAccessible(true);
						List<FrameworkMethod> fBefores = (List<FrameworkMethod>) f.get(base);
						
						f = base.getClass().getDeclaredField("fNext");
						f.setAccessible(true);
						Statement fNext = (Statement) f.get(base);
						for (FrameworkMethod frameworkMethod : fBefores) {
							frameworkMethod.invokeExplosively(testThis);
						}
//						fNext.evaluate();
						f = fNext.getClass().getDeclaredField("fTestMethod");
						f.setAccessible(true);
						FrameworkMethod m = (FrameworkMethod) f.get(fNext);
						m.invokeExplosively(testThis);
					}else{
	//					im = (InvokeMethod) base;
						Field f = base.getClass().getDeclaredField("fTestMethod");
						f.setAccessible(true);
						FrameworkMethod m = (FrameworkMethod) f.get(base);
						m.invokeExplosively(testThis);
					}
				}catch(Throwable t){
					System.out.println(base.getClass());
					t.printStackTrace();
					throw t;
				}
				if (desc.getAnnotation(DontRunAfter.class) == null) {
					for (FrameworkMethod fm : afters) {
						fm.invokeExplosively(testThis);
					}
				}
			}
		};
	}
}
