package purity;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class TestPurity {
//    @Test
    public static void main(String[] args) throws RefactoringMinerTimedOutException, IOException {
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\1\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\1\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        PurityChecker.isPure(modelDiff);
        System.out.println("hi");
//        for (Refactoring ref: refactorings){
//            System.out.println(ref.toString());
//        }

    }
}
