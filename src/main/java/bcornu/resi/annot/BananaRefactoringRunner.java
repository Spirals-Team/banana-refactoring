package bcornu.resi.annot;

import org.junit.internal.runners.statements.FailOnTimeout;
import org.junit.runner.Description;
import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

public class BananaRefactoringRunner extends BlockJUnit4ClassRunner {
	public BananaRefactoringRunner(Class<?> klass) throws InitializationError {
		super(klass);
		System.out.println("BananaRefactoringRunner");
	}

	Object o;

	// @Override
	// protected Object createTest() throws Exception {
	// System.out.println("createTest");
	// if (o==null) {
	// o = super.createTest();
	// }
	// return o;
	// }

	@Override
	protected Statement methodBlock(FrameworkMethod method) {
		try {
			System.out.println("run test method "+method.getName());
			// we instantiate an object
			Statement statement; 
			if (o == null || method.getAnnotation(RunBefore.class)!=null) {
				o = createTest();
		        statement = createStatement(method, o);
		        statement = withBefores(method, o, statement);
			} else {
		        statement = createStatement(method, o);
			}	        
			
			if (method.getAnnotation(RunAfter.class)!=null) {
		        statement = withAfters(method, o, statement);
			}
	        return statement;
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	private Statement createStatement(FrameworkMethod method, Object o) { 
		Statement statement = methodInvoker(method, o);
        statement = possiblyExpectingExceptions(method, o, statement);
        statement = new FailOnTimeout(statement, 10000);
        return statement;
	}

}
