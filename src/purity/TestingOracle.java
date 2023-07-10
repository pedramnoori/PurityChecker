package purity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.uom.java.xmi.diff.*;
import org.refactoringminer.api.*;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestingOracle {

    public static void main(String[] args) throws IOException {

        String outputPath = runPurityOnTestOracle("/Users/pedram/Desktop/RefactoringMiner/src/purity/sampleResPurity.json");
        calculatePrecisionAndRecallOnSpecificRefactoring("/Users/pedram/Desktop/RefactoringMiner/src/purity/sampleResPurityValidated.json", RefactoringType.MOVE_OPERATION);
//        buildOracle(RefactoringType.PUSH_DOWN_OPERATION);
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
        float fScore;
        float specificity;

        try {
            List<RepositoryJson> repositoryJsonList = objectMapper.readValue(file, objectMapper.getTypeFactory().constructCollectionType(List.class, RepositoryJson.class));
            TPCounter = (int) repositoryJsonList.stream().flatMap(r -> r.getRefactorings().stream()).filter(r -> r.getType().equals(refactoringType.getDisplayName())).filter(r -> r.getPurity().getPurityValidation().equals("TP")).count();
            TNCounter = (int) repositoryJsonList.stream().flatMap(r -> r.getRefactorings().stream()).filter(r -> r.getType().equals(refactoringType.getDisplayName())).filter(r -> r.getPurity().getPurityValidation().equals("TN")).count();
            FPCounter = (int) repositoryJsonList.stream().flatMap(r -> r.getRefactorings().stream()).filter(r -> r.getType().equals(refactoringType.getDisplayName())).filter(r -> r.getPurity().getPurityValidation().equals("FP")).count();
            FNCounter = (int) repositoryJsonList.stream().flatMap(r -> r.getRefactorings().stream()).filter(r -> r.getType().equals(refactoringType.getDisplayName())).filter(r -> r.getPurity().getPurityValidation().equals("FN")).count();

            precision = precisionCalculator(TPCounter, TNCounter, FPCounter, FNCounter);
            recall = recallCalculator(TPCounter, TNCounter, FPCounter, FNCounter);
            specificity = specificityCalculator(TPCounter, TNCounter, FPCounter, FNCounter);
            fScore = fScoreCalculator(precision, recall);

            System.out.println("Number of true positives for " + refactoringType.getDisplayName() + " refactoring is: " + TPCounter);
            System.out.println("Number of true negatives for " + refactoringType.getDisplayName() + " refactoring is: " + TNCounter);
            System.out.println("Number of false positives for " + refactoringType.getDisplayName() + " refactoring is: " + FPCounter);
            System.out.println("Number of false negatives for " + refactoringType.getDisplayName() + " refactoring is: " + FNCounter);
            System.out.println();
            System.out.println("Precision for " + refactoringType.getDisplayName() + " refactoring is: " + precision * 100);
            System.out.println("Recall for " + refactoringType.getDisplayName() + " refactoring is: " + recall * 100);
            System.out.println("Specificity for " + refactoringType.getDisplayName() + " refactoring is: " + specificity * 100);
            System.out.println("F-score for " + refactoringType.getDisplayName() + " refactoring is: " + fScore);


            try
            {
                String filename= "StatisticsTestOracle.txt";
                FileWriter fw = new FileWriter(filename,true);

                fw.write("For "+ refactoringType.getDisplayName() + ": ");

                fw.write("TIME: " + new Timestamp(System.currentTimeMillis()) + "\n");
                fw.write("TP: "+ TPCounter + "\n");
                fw.write("TN: "+ TNCounter + "\n");
                fw.write("FP: "+ FPCounter + "\n");
                fw.write("FN: "+ FNCounter + "\n");

                fw.write("Precision: "+ precision * 100 + "\n");
                fw.write("Recall: "+ recall * 100 + "\n");
                fw.write("Specificity: "+ specificity * 100 + "\n");
                fw.write("FScore: "+ fScore + "\n");

                fw.write("\n\n");



                fw.close();
            }
            catch(IOException ioe)
            {
                System.err.println("IOException: " + ioe.getMessage());
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static String runPurityOnTestOracle(String sourcePath) {

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
                                    for (Map.Entry<Refactoring, PurityCheckResult> entry : pcr.entrySet()) {
                                        if (entry.getValue() != null) {
                                            if (entry.getKey().toString().replaceAll("\\s+", "").equals(refactoring.get("description").textValue().replaceAll("\\s+", ""))) {
                                                ObjectNode objectNode = (ObjectNode) refactoring.get("purity");
                                                if (entry.getValue().isPure() && refactoring.get("purity").get("purityValue").textValue().equals("1")) {
                                                    objectNode.put("purityValidation", "TP");
                                                    objectNode.put("purityComment", entry.getValue().getPurityComment());
                                                    objectNode.put("mappingState", entry.getValue().getMappingState());
                                                } else if (!entry.getValue().isPure() && refactoring.get("purity").get("purityValue").textValue().equals("0")) {
                                                    objectNode.put("purityValidation", "TN");
                                                    objectNode.put("purityComment", entry.getValue().getPurityComment());
                                                    objectNode.put("mappingState", entry.getValue().getMappingState());
                                                } else if (entry.getValue().isPure() && refactoring.get("purity").get("purityValue").textValue().equals("0")) {
                                                    objectNode.put("purityValidation", "FP");
                                                    objectNode.put("purityComment", entry.getValue().getPurityComment());
                                                    objectNode.put("mappingState", entry.getValue().getMappingState());
                                                } else if (!entry.getValue().isPure() && refactoring.get("purity").get("purityValue").textValue().equals("1")) {
                                                    objectNode.put("purityValidation", "FN");
                                                    objectNode.put("purityComment", entry.getValue().getPurityComment());
                                                    objectNode.put("mappingState", entry.getValue().getMappingState());
                                                }
                                                break;
                                            }
                                        }
                                    }
                                }
                                arrayNode.add(jsonNode);
                            }

                        }, 100);
            }
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            String outputPath = "/Users/pedram/Desktop/RefactoringMiner/src/purity/sampleResPurityValidated.json";
            objectMapper.writeValue(new File(outputPath), arrayNode);
            return outputPath;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public static void buildOracle(RefactoringType refactoringType) throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        File file = new File("/Users/pedram/Desktop/sample.json");

        JsonNode root = objectMapper.readTree(file);

        for (JsonNode jsonNode : root) {
            String url = jsonNode.textValue();
            String commitUrl = URLHelper.getRepo(url);
            String commitSh1 = URLHelper.getCommit(url);
            System.out.println(url);

//            Map<Refactoring, PurityCheckResult> pcr = new LinkedHashMap<>();

            miner.detectModelDiff(commitUrl,
                    commitSh1, new RefactoringHandler() {
                        @Override
                        public void processModelDiff(String commitId, UMLModelDiff umlModelDiff) throws RefactoringMinerTimedOutException {
                        List<Refactoring> refactorings = umlModelDiff.getRefactorings();
                        if (containsSpecificRefactoringType(refactorings, refactoringType)) {
                            ObjectNode refactoring = objectMapper.createObjectNode();
                            refactoring.put("repository", commitUrl);
                            refactoring.put("sha1", commitSh1);
                            refactoring.put("url", url);

                            ArrayNode refactoringListArray = objectMapper.createArrayNode();


                            for (Refactoring refactoring1 : refactorings) {
                                ObjectNode refactoringList = objectMapper.createObjectNode();
                                refactoringList.put("type", refactoring1.getName());
                                refactoringList.put("description", refactoring1.toString());
                                refactoringListArray.add(refactoringList);
                            }
                            refactoring.set("refactorings", refactoringListArray);
//                            PurityChecker.isPure(umlModelDiff, pcr, refactorings);
                            arrayNode.add(refactoring);
                        }
                        }
                    }, 100);
            }

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(new File("/Users/pedram/Desktop/sampleRes.json"), arrayNode);
        }

        public static boolean containsMethodRelatedRefactoring(List<Refactoring> refactorings) {

        List<Refactoring> res = refactorings.stream().filter(refactoring -> refactoring instanceof ExtractOperationRefactoring ||
                refactoring instanceof InlineOperationRefactoring ||
                refactoring instanceof MoveOperationRefactoring ||
                refactoring instanceof SplitOperationRefactoring ||
                refactoring instanceof MergeOperationRefactoring ||
                refactoring instanceof PushDownOperationRefactoring ||
                refactoring instanceof PullUpOperationRefactoring).collect(Collectors.toList());

        return !res.isEmpty();
        }

    public static boolean containsSpecificRefactoringType(List<Refactoring> refactorings, RefactoringType refactoringType) {

        for (Refactoring refactoring : refactorings) {
            if (refactoring.getRefactoringType().equals(refactoringType)) {
                return true;
            }
        }

        return false;
    }


    private static float specificityCalculator(int tpCounter, int tnCounter, int fpCounter, int fnCounter) {

        float res = 0;
        try {
            res = tnCounter / ((float) tnCounter + (float) fpCounter);
        }catch (ArithmeticException e) {
            System.out.println("Division by zero!");
        }
        return res;


    }

    private static float fScoreCalculator(float precision, float recall) {

        float res = 0;
        try {
            res = (2 * precision * recall) / (precision + recall);
        }catch (ArithmeticException e) {
            System.out.println("Division by zero!");
        }
        return res;

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
}
