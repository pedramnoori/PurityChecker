package purity;

import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.GitHistoryRefactoringMiner;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringHandler;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.astDiff.utils.URLHelper;
import org.refactoringminer.rm1.GitHistoryRefactoringMinerImpl;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class API {

/*   Here is the sample provided to run the PurityChecker API. Within the pcr object, you will find
        the purity object for each Extract Method, Inline Method, Move Method, and Push Down Method cases.
*/

    public static void main(String[] args) {
        isPureAPI("https://github.com/DSpace/DSpace/commit/a3854f54a8f851a0d2212ae047e2f43553fc69d4");
    }

    public static Map<Refactoring, PurityCheckResult> isPureAPI(String url) {
        String commitUrl = URLHelper.getRepo(url);
        String commitSh1 = URLHelper.getCommit(url);
        Map<Refactoring, PurityCheckResult> pcr = new LinkedHashMap<>();

        GitHistoryRefactoringMiner miner = new GitHistoryRefactoringMinerImpl();

        miner.detectModelDiff(commitUrl,
                commitSh1, new RefactoringHandler() {
                    @Override
                    public void processModelDiff(String commitId, UMLModelDiff umlModelDiff) throws RefactoringMinerTimedOutException {
                        List<Refactoring> refactorings = umlModelDiff.getRefactorings();
                        PurityChecker.isPure(umlModelDiff, pcr, refactorings);
                    }
                }, 100);

        System.out.println("Test");
        return pcr;
    }
}
