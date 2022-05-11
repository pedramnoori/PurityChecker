package purity;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.Test;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPurity {

    @Test
    public void extractMethodTest_1() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\1\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\1\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                assertTrue(pcr.get(refactoring).isPure());
                assertEquals(pcr.get(refactoring).getDescription(), "Adding return statement or rename variable");
            }
        }
    }

    @Test
    public void extractMethodTest_2() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\4\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\4\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_3() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\5\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\5\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_4() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\7\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\7\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_5() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("/Users/pedram/Downloads/TestCases/TestCases/8/v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("/Users/pedram/Downloads/TestCases/TestCases/8/v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_6() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("/Users/pedram/Downloads/TestCases/TestCases/9/v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("/Users/pedram/Downloads/TestCases/TestCases/9/v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }


}
