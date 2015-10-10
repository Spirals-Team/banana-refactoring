
This repository contains a research prototype for automatically refactoring test cases in smaller parts.

See [Dynamic Analysis can be Improved with Automatic Test Suite Refactoring](http://arxiv.org/pdf/1506.01883) (Jifeng Xuan, Benoit Cornu, Matias Martinez, Benoit Baudry, Lionel Seinturier, Martin Monperrus), Technical report 1506.01883, Arxiv, 2015

```
@techreport{brefactoring,
   author = {Jifeng Xuan and Benoit Cornu and Matias Martinez and Benoit Baudry and Lionel Seinturier and Martin Monperrus},
    title = {{Dynamic Analysis can be Improved with Automatic Test Suite Refactoring}},
  institution = {Arxiv},
   number = {1506.01883},
     year = 2015,
     url = {http://arxiv.org/pdf/1506.01883},
}
```

Contact: [Martin Monperrus](http://www.monperrus.net/martin/contact)

Compilation
=====

We provide the guidelines for the Eclipse IDE.

```
git clone https://github.com/Spirals-Team/banana-refactoring
cd banana-refactoring/
mvn eclipse:eclipse
```

Now, import and open the project in the Eclipse IDE (File >> Import >> Existing project into workspace)

Sample usage
=======

We now consider the example provided in the repo (spojo-core).
```
cd example
mvn eclipse:eclipse
```
import the project `spojo-core` in the Eclipse IDE.

banana-refactoring uses code transformation, so you need to clone the example project (copy and paste) in say `spojo-core-refactored`.

Add as dependency of `spojo-core-refactored`, the banana-refactoring project.

Go to class `bcu.nopol.main.Launcher.java` and set the constants:

* `WORKSPACE_METADATA_PATH` 
* `ORIG_ECLIPSE_PROJECT_NAME` 
* `TARGET_ECLIPSE_PROJECT_NAME` 
* `OUTPUT_PROJECT_PATH`

Run `bcu.nopol.main.Launcher.main` with argument "-1" (Run >> Run configurations). This transforms the source code of test classes and application classes of `spojo-core` and put the instrumented version in TARGET_ECLIPSE_PROJECT_NAME. Refresh the project `spojo-core-refactored` to force full re-compilation.

Run `bcu.nopol.main.Launcher.main` with  argument "-2". This computes the number of pure and impure tests, and the file `cutsPerIf` and split the test cases.  Refresh the project `spojo-core-refactored` to force full re-compilation (esp of the split test cases).

That's it, you have the refactored test suite!

** If one of the phase fails, always start at phase #1 again **


