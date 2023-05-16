package purity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import gr.uom.java.xmi.diff.*;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.io.File;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestingOracle {

    public static void main(String[] args) throws IOException {

        buildOracle();
    }

    public static void buildOracle() throws IOException {

        ObjectMapper objectMapper = new ObjectMapper();
        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();
        ArrayNode arrayNode = objectMapper.createArrayNode();

        File file = new File("/Users/pedram/Desktop/sample.json");

        JsonNode root = objectMapper.readTree(file);

        for (JsonNode jsonNode : root) {
            String url = jsonNode.textValue();
            String commitUrl = URLHelper.getRepo(url);
            String commitSh1 = URLHelper.getCommit(url);

            Map<Refactoring, PurityCheckResult> pcr = new LinkedHashMap<>();

            miner.detectModelDiff(commitUrl,
                    commitSh1, new RefactoringHandler() {
                        @Override
                        public void processModelDiff(String commitId, UMLModelDiff umlModelDiff) throws RefactoringMinerTimedOutException {
                        List<Refactoring> refactorings = umlModelDiff.getRefactorings();
                        if (containsMethodRelatedRefactoring(refactorings)) {
                            ObjectNode refactoring = objectMapper.createObjectNode();
                            refactoring.put("repository", url);
                            refactoring.put("sha1", commitSh1);
                            refactoring.put("url", commitUrl);



//                            PurityChecker.isPure(umlModelDiff, pcr, refactorings);
                            System.out.println("HERE");
                            arrayNode.add(refactoring);
                        }
                        }
                    }, 100);
            }

        objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        objectMapper.writeValue(new File("/Users/pedram/Desktop/sampres.json"), arrayNode);
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
}
