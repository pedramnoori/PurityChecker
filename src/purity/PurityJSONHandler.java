package purity;

//import org.json.simple.*;
//import org.json.simple.parser.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.*;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class PurityJSONHandler {

    public static void main(String[] args) {

//        addPurityFields("C:\\Users\\Pedram\\Desktop\\data.json", "C:\\Users\\Pedram\\Desktop\\Puritydata.json");

        getStatistics("C:\\Users\\Pedram\\Desktop\\Puritydata.json");
//        runPurity("C:\\Users\\Pedram\\Desktop\\PuritydataTest.json");
        calculatePrecisionAndRecallOnSpecificRefactoring("C:\\Users\\Pedram\\Desktop\\PurityResTest.json", RefactoringType.EXTRACT_OPERATION);

    }

    private static void getStatistics(String sourcePath) {

        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(sourcePath);

        try {
            List<RepositoryJson> repositoryJsonList = objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, RepositoryJson.class));

            int numberOfExtractMethodRefactoringsProcessed = (int) repositoryJsonList.stream().flatMap(r -> r.getRefactorings().stream()).filter(r -> !r.getPurity().getPurityValue().equals("-")).filter(r -> r.getType().equals("Extract Method")).count();
            int numberOfExtractMethodRefactorings = (int) repositoryJsonList.stream().flatMap(r -> r.getRefactorings().stream()).filter(r -> r.getType().equals("Extract Method")).count();

            System.out.println(numberOfExtractMethodRefactorings);
            System.out.println(numberOfExtractMethodRefactoringsProcessed);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void calculatePrecisionAndRecallOnSpecificRefactoring(String sourcePath, RefactoringType refactoringType) {

        ObjectMapper objectMapper = new ObjectMapper();
        File file = new File(sourcePath);

        int TPCounter = 0;
        int TNCounter = 0;
        int FPCounter = 0;
        int FNCounter = 0;
        float precision;
        float recall;

        try {
            List<RepositoryJson> repositoryJsonList = objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, RepositoryJson.class));
            TPCounter = (int) repositoryJsonList.stream().flatMap(r -> r.getRefactorings().stream()).filter(r -> r.getType().equals(refactoringType.getDisplayName())).filter(r -> r.getPurity().getPurityValidation().equals("TP")).count();
            TNCounter = (int) repositoryJsonList.stream().flatMap(r -> r.getRefactorings().stream()).filter(r -> r.getType().equals(refactoringType.getDisplayName())).filter(r -> r.getPurity().getPurityValidation().equals("TN")).count();
            FPCounter = (int) repositoryJsonList.stream().flatMap(r -> r.getRefactorings().stream()).filter(r -> r.getType().equals(refactoringType.getDisplayName())).filter(r -> r.getPurity().getPurityValidation().equals("FP")).count();
            FNCounter = (int) repositoryJsonList.stream().flatMap(r -> r.getRefactorings().stream()).filter(r -> r.getType().equals(refactoringType.getDisplayName())).filter(r -> r.getPurity().getPurityValidation().equals("FN")).count();

            precision = precisionCalculator(TPCounter, TNCounter, FPCounter, FNCounter);
            recall = recallCalculator(TPCounter, TNCounter, FPCounter, FNCounter);

            System.out.println("Precision for " + refactoringType.getDisplayName() + " refactoring is: " + precision * 100);
            System.out.println("Recall for " + refactoringType.getDisplayName() + " refactoring is: " + recall * 100);
            System.out.println("F-score for " + refactoringType.getDisplayName() + " refactoring is: " + (2 * precision * recall) / (float) (precision + recall));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static float recallCalculator(int tpCounter, int tnCounter, int fpCounter, int fnCounter) {

        float res = 0;
        try {
            res = tpCounter / ((float) tpCounter + (float) fnCounter);
        }catch (ArithmeticException e) {
            System.out.println("Division by zero!");
        }
        return res;

    }

    private static float precisionCalculator(int tpCounter, int tnCounter, int fpCounter, int fnCounter) {

        float res = 0;
        try {
            res = tpCounter / ((float) tpCounter + (float) fpCounter);
        }catch (ArithmeticException e) {
            System.out.println("Division by zero!");
        }
        return res;
    }

    private static void runPurity(String sourcePath) {

        ObjectMapper objectMapper = new ObjectMapper();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        File file = new File(sourcePath);
        try {
            JsonNode root = objectMapper.readTree(file);

            for (JsonNode jsonNode : root) {
                miner.detectModelDiff(jsonNode.get("repository").textValue(),
                        jsonNode.get("sha1").textValue(), new RefactoringHandler() {
                            @Override
                            public void processModelDiff(String commitId, UMLModelDiff umlModelDiff) throws RefactoringMinerTimedOutException {
                                Map<Refactoring, PurityCheckResult> pcr = PurityChecker.isPure(umlModelDiff);

                                for (JsonNode refactoring : jsonNode.get("refactorings")) {
                                    if (refactoring.get("validation").textValue().equals("TP") || refactoring.get("validation").textValue().equals("FN")) {
                                        for (Map.Entry<Refactoring, PurityCheckResult> entry : pcr.entrySet()) {
                                            if (entry.getValue() != null) {
                                                if (entry.getKey().toString().replaceAll("\\s+", "").equals(refactoring.get("description").textValue().replaceAll("\\s+", ""))
                                                && (refactoring.get("detectionTools").textValue().contains("RefactoringMiner")) || refactoring.get("detectionTools").textValue().contains("RMiner")) {
                                                    ObjectNode objectNode = (ObjectNode) refactoring.get("purity");
                                                    if (entry.getValue().isPure() && refactoring.get("purity").get("purityValue").textValue().equals("1")) {
                                                        objectNode.put("purityValidation", "TP");
                                                    } else if (!entry.getValue().isPure() && refactoring.get("purity").get("purityValue").textValue().equals("0")) {
                                                        objectNode.put("purityValidation", "TN");
                                                    } else if (entry.getValue().isPure() && refactoring.get("purity").get("purityValue").textValue().equals("0")) {
                                                        objectNode.put("purityValidation", "FP");
                                                    } else if (!entry.getValue().isPure() && refactoring.get("purity").get("purityValue").textValue().equals("1")) {
                                                        objectNode.put("purityValidation", "FN");
                                                    }
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                                arrayNode.add(jsonNode);
                            }

                        }, 100);
            }
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(new File("C:\\Users\\Pedram\\Desktop\\PurityResTest.json"), arrayNode);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void addPurityFields(String sourcePath, String destPath) {

        ObjectMapper objectMapper = new ObjectMapper();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        try {
            File file = new File(sourcePath);
            JsonNode root = objectMapper.readTree(file);

            for (JsonNode jsonNode: root) {

                for (JsonNode refactoring: jsonNode.get("refactorings")) {
                    ObjectNode objectNode = (ObjectNode) refactoring;
                    ObjectNode purity = objectMapper.createObjectNode();

                    purity.put("purityValue", "-");
                    purity.put("purityValidation", "-");
                    purity.put("purityComment", "");

                    objectNode.set("purity", purity);
                }
                arrayNode.add(jsonNode);
            }

            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.writeValue(new File(destPath), arrayNode);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
