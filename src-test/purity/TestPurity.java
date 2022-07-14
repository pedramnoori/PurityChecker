package purity;

import gr.uom.java.xmi.UMLModel;
import gr.uom.java.xmi.UMLModelASTReader;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.junit.Test;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TestPurity {

    @Test
    public void extractMethodTest_1() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("/Users/pedram/Downloads/TestCases/TestCases/1/v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("/Users/pedram/Downloads/TestCases/TestCases/1/v2")).getUmlModel();
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

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\4\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\4\\v2")).getUmlModel();
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

        UMLModel model1 = new UMLModelASTReader(new File("/Users/pedram/Downloads/TestCases/TestCases/5/v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("/Users/pedram/Downloads/TestCases/TestCases/5/v2")).getUmlModel();
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

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\7\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\7\\v2")).getUmlModel();
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

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\8\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\8\\v2")).getUmlModel();
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

    @Test
    public void extractMethodTest_7() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("/Users/pedram/Downloads/TestCases/TestCases/10/v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("/Users/pedram/Downloads/TestCases/TestCases/10/v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_8() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\11\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\11\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_9() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\12\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\12\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_10() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\13\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\13\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_11() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\14\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\14\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_12() throws RefactoringMinerTimedOutException, IOException {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/apache/giraph.git",
                "87f3c4d8572ee8f7244af8a92dc3312ce24260e3", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
//                        System.out.println("Refactorings at " + commitId);
//                        for (Refactoring ref : refactorings) {
//                            System.out.println(ref.toString());
//                        }

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }

                    }
                }, 20);
    }

    @Test
    public void extractMethodTest_13() throws RefactoringMinerTimedOutException, IOException {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/apache/giraph.git",
                "2117d1dbba8f18b08da70e890c30111edb3aebe3", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
//                        System.out.println("Refactorings at " + commitId);
//                        for (Refactoring ref : refactorings) {
//                            System.out.println(ref.toString());
//                        }

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }

                    }
                }, 20);
    }
    @Test
    public void extractMethodTest_14() throws RefactoringMinerTimedOutException, IOException {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/apache/giraph.git",
                "6fc0550bcd20e979bf5a5db88b05c4ef12559152", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
//                        System.out.println("Refactorings at " + commitId);
//                        for (Refactoring ref : refactorings) {
//                            System.out.println(ref.toString());
//                        }

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }

                    }
                }, 100);
    }


    @Test
    public void extractMethodTest_15() throws RefactoringMinerTimedOutException, IOException {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/apache/giraph.git",
                "72562004cfa5106472f61efc5d456788bbc256f9", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
//                        System.out.println("Refactorings at " + commitId);
//                        for (Refactoring ref : refactorings) {
//                            System.out.println(ref.toString());
//                        }

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }

                    }
                }, 100);
    }

    @Test
    public void extractMethodTest_16() {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/apache/giraph.git",
                "7d0e006992574973bbd732373af32462393f00b5", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
//                        System.out.println("Refactorings at " + commitId);
//                        for (Refactoring ref : refactorings) {
//                            System.out.println(ref.toString());
//                        }

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }

                    }
                }, 100);
    }


    @Test
    public void extractMethodTest_17() {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/apache/giraph.git",
                "8fbade6a7801afd6065007473585d9d2a761818a", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
//                        System.out.println("Refactorings at " + commitId);
//                        for (Refactoring ref : refactorings) {
//                            System.out.println(ref.toString());
//                        }

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }

                    }
                }, 100);
    }

    @Test
    public void extractMethodTest_18() {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/apache/giraph.git",
                "add1d4f07c925b8a9044cb3aa5bb4abdeaf49fc7", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {
//                        System.out.println("Refactorings at " + commitId);
//                        for (Refactoring ref : refactorings) {
//                            System.out.println(ref.toString());
//                        }

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }

                    }
                }, 100);
    }

    @Test
    public void extractMethodTest_19() {
        // no _
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/apache/helix.git",
                "059ab387b7fe70e71989e6560b57667c8eda7b60", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);

                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }
                    }
                }, 100);
    }

    @Test
    public void extractMethodTest_20() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\16\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\16\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_21() throws RefactoringMinerTimedOutException, IOException {

        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\17\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\17\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_22() throws RefactoringMinerTimedOutException, IOException {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/Teino1978-Corp/Teino1978-Corp-helix.git",
                "433b0011655c0c42228416488bb6b16f4b2f2700", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);

                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }
                    }
                }, 100);
    }


    @Test
    public void extractMethodTest_23() throws RefactoringMinerTimedOutException, IOException {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/apache/helix.git",
                "579baa5bb061bec9d5b38731f20f51ea29a05f42", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);

                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }
                    }
                }, 100);
    }

    @Test
    public void extractMethodTest_24() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\19\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\19\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_25() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\18\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\18\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_26() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\20\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\20\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_27() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\21\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\21\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_28() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\22\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\22\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_29() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\23\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\23\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_30() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\24\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\24\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_31() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\25\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\25\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_32() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\26\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\26\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_33() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\27\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\27\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_34() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\28\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\28\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }


    @Test
    public void extractMethodTest_35() throws RefactoringMinerTimedOutException, IOException {
        // _
        UMLModel model1 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\29\\v1")).getUmlModel();
        UMLModel model2 = new UMLModelASTReader(new File("C:\\Users\\Pedram\\Desktop\\TestCases\\TestCases\\TestCases\\29\\v2")).getUmlModel();
        UMLModelDiff modelDiff = model1.diff(model2);
        List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);


        for (Refactoring refactoring: refactorings){
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                assertTrue(pcr.get(refactoring).isPure());
        }
    }

    @Test
    public void extractMethodTest_36() throws RefactoringMinerTimedOutException, IOException {

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/JetBrains/MPS.git",
                "2bcd05a827ead109a56cb1f79a83dcd332f42888", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);

                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }
                    }
                }, 100);
    }

    @Test
    public void extractMethodTest_37() throws RefactoringMinerTimedOutException, IOException {

//        IMPURE MOVE METHOD - TODO

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/SonarSource/sonarqube.git",
                "c55a8c3761e9aae9f375d312c14b1bbb9ee9c0fa", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);

                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }
                    }
                }, 100);
    }

    @Test
    public void extractMethodTest_38() throws RefactoringMinerTimedOutException, IOException {

//        IMPURE MOVE METHOD - TODO - deleted class

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/liferay/liferay-portal.git",
                "59fd9e696cec5f2ed44c27422bbc426b11647321", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);

                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }
                    }
                }, 100);
    }

    @Test
    public void extractMethodTest_39() throws RefactoringMinerTimedOutException, IOException {

//        Pure PUSH DOWN METHOD - TODO

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        miner.detectAtCommit("https://github.com/Alluxio/alluxio.git",
                "6d10621465c0e6ae81ad8d240d70a55c72caeea6", new RefactoringHandler() {
                    @Override
                    public void handle(String commitId, List<Refactoring> refactorings) {

                        Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(refactorings);

                        for (Refactoring refactoring: refactorings){
                            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION))
                                assertTrue(pcr.get(refactoring).isPure());
                        }
                    }
                }, 100);
    }

}