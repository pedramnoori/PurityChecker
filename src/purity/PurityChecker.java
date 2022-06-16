package purity;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PurityChecker {

    public static Map<Refactoring, PurityCheckResult> isPure(List<Refactoring> refactorings){
        Map<Refactoring, PurityCheckResult> purityCheckResults = new LinkedHashMap<>();

        for (Refactoring refactoring: refactorings){
            purityCheckResults.put(refactoring, checkPurity(refactoring, refactorings));
        }
        return purityCheckResults;
    }

    private static PurityCheckResult checkPurity(Refactoring refactoring, List<Refactoring> refactorings){
        PurityCheckResult result;
        switch (refactoring.getRefactoringType()){
            case EXTRACT_OPERATION:
                result = detectExtractOperationPurity((ExtractOperationRefactoring) refactoring, refactorings);
                break;
            case RENAME_CLASS:
                result = detectRenameClassPurity((RenameClassRefactoring) refactoring);
                break;
            case RENAME_VARIABLE:
                result = detectRenameVariablePurity((RenameVariableRefactoring) refactoring);
                break;
            case RENAME_PARAMETER:
                result = detectRenameParameterPurity((RenameVariableRefactoring) refactoring);
                break;
            default:
                result = null;

        }

        return result;
    }

    private static PurityCheckResult detectRenameClassPurity(RenameClassRefactoring refactoring) {

        return new PurityCheckResult(true, "Rename Class refactorings are always pure!");
    }

    private static PurityCheckResult detectRenameVariablePurity(RenameVariableRefactoring refactoring) {

        return new PurityCheckResult(true, "Rename Variable refactorings are always pure!");
    }

    private static PurityCheckResult detectRenameParameterPurity(RenameVariableRefactoring refactoring) {

        return new PurityCheckResult(true, "Rename Parameter refactorings are always pure!");
    }

    private static PurityCheckResult detectExtractOperationPurity(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings) {

        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty()) {

//            only renames
            if (refactoring.getBodyMapper().allMappingsAreExactMatches())
                return new PurityCheckResult(true, "All the mappings are matched! - all mapped");


            if (refactoring.getReplacements().isEmpty())
                return new PurityCheckResult(true, "There is no replacement! - all mapped");

//            Handling SECOND issue

            if (checkForRenameRefactoringOnTopMapped(refactoring, refactorings)) {
                return new PurityCheckResult(true, "Rename Refactoring on the top of the extracted method - all mapped");
            }



//        Check each mapping to if it violates the mechanics
            for (AbstractCodeMapping mapping : refactoring.getBodyMapper().getMappings()) {
                if (!mapping.getReplacements().isEmpty()) {
                    if (!checkExtractMethodRefactoringMechanics(mapping.getReplacements())) {
                        return new PurityCheckResult(false, "Violating extract method refactoring mechanics - all mapped");
                    }
                }
            }
//        Check non-mapped leaves
        } else{
            if ((refactoring.getBodyMapper().allMappingsAreExactMatches() || allReplacementsAreVariableNameOrType(refactoring.getReplacements())) && addOneReturnExpression(refactoring.getBodyMapper().getNonMappedLeavesT2(), 0))
                return new PurityCheckResult(true, "Adding return statement and variable name changed - with non-mapped leaves");

//            Handling FIRST issue
            else if (checkForRenameRefactoringOnTopNonMapped(refactoring, refactorings)) {
                return new PurityCheckResult(true, "Rename Refactoring on the top of the extracted method - with non-mapped leaves");
            }

            else
                return new PurityCheckResult(false, "Violating extract method refactoring mechanics - with non-mapped leaves");
        }

        return new PurityCheckResult(true, "Adding return statement or rename variable - END");
    }

    private static boolean checkForRenameRefactoringOnTopMapped(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings) {


        int numberOfReplacements = refactoring.getReplacements().size();


        for (Refactoring rf: refactorings) {
            if (rf.getRefactoringType().equals(RefactoringType.RENAME_METHOD)) {
                for (Replacement rep: refactoring.getReplacements()) {
                    if (rep.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME)) {
                        if (rep.getBefore().equals(((RenameOperationRefactoring) rf).getOriginalOperation().getName()) && rep.getAfter().equals(((RenameOperationRefactoring) rf).getRenamedOperation().getName())) {
                            numberOfReplacements--;
                        } else {
                            return false;
                        }
                    }
                }
            }

            if (numberOfReplacements == 0) {
                return true;
            }
        }

        return false;
    }
    private static boolean checkForRenameRefactoringOnTopNonMapped(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings) {

        int nonMappedLeavesToCheck = refactoring.getBodyMapper().getNonMappedLeavesT2().size();

        if (nonMappedLeavesToCheck != 0) {

            for (Refactoring rf : refactorings) {
                if (rf.getRefactoringType().equals(RefactoringType.RENAME_METHOD)) {
                    if (check1(refactoring, (RenameOperationRefactoring) rf)) {
                        nonMappedLeavesToCheck--;
                    }
                    if (nonMappedLeavesToCheck == 0) {
                        return true;
                    }

                    if (nonMappedLeavesToCheck == 1 && addOneReturnExpression(refactoring.getBodyMapper().getNonMappedLeavesT2(), 1)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static boolean check1(ExtractOperationRefactoring exRefactoring, RenameOperationRefactoring rnRefactoring) {

        for (AbstractCodeFragment acf: exRefactoring.getBodyMapper().getNonMappedLeavesT1()) {

            int paren = acf.getString().indexOf("(");
            if (paren != -1) {
                String methodName = acf.getString().substring(0, paren);

                if (methodName.equals(rnRefactoring.getOriginalOperation().getName())){
                    return check2(exRefactoring, rnRefactoring);
                }
            }
        }
        return false;
    }

    private static boolean check2(ExtractOperationRefactoring exRefactoring, RenameOperationRefactoring rnRefactoring) {

        for (AbstractCodeFragment acf: exRefactoring.getBodyMapper().getNonMappedLeavesT2()) {

            int paren = acf.getString().indexOf("(");
            if (paren != -1) {
                String methodName = acf.getString().substring(0, paren);

                if (methodName.equals(rnRefactoring.getRenamedOperation().getName())){
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean addOneReturnExpression(List<AbstractCodeFragment> nonMappedLeavesT2, int acceptanceRate){

        int counter = 0;

            for (AbstractCodeFragment st : nonMappedLeavesT2) {
                if (!st.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    counter++;
                }
            }
//            if (counter > acceptanceRate) {
//                return false;
//            }
        return counter <= acceptanceRate;
//        return true;
    }

    private static boolean allReplacementsAreVariableNameOrType(Set<Replacement> replacements) {

        for (Replacement rep: replacements) {
            if (rep.getType().equals(Replacement.ReplacementType.VARIABLE_NAME) ||
                    rep.getType().equals(Replacement.ReplacementType.TYPE)) {

                continue;
            }
            return false;
        }
        return true;
    }

    private static boolean checkExtractMethodRefactoringMechanics(Set<Replacement> replacements) {


        if (checkIfArgumentReplacedWithReturnOrRenames(replacements))
            return true;

        return false;
    }

    private static boolean checkIfArgumentReplacedWithReturnOrRenames(Set<Replacement> replacements) {

        for (Replacement rep: replacements) {
            if (!rep.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION) &&
                    !rep.getType().equals(Replacement.ReplacementType.VARIABLE_NAME) &&
                    !rep.getType().equals(Replacement.ReplacementType.TYPE)){
                return false;
            }
        }
        return true;
    }


}
class PurityCheckResult
{
    private boolean isPure;
    float result;
    String description;

    PurityCheckResult(boolean isPure, String description){
        this.isPure = isPure;
        this.description = description;
    }

    public boolean isPure() {
        return isPure;
    }

    public String getDescription() {
        return description;
    }

    //    private void calcPurity(){
//        if (result < 0.5)
//            isPure = true;
//        else
//            isPure = false;
//    }

    @Override
    public String toString() {
        return "PurityCheckResult{" +
                "isPure=" + isPure +
                ", description='" + description + '\'' +
                '}';
    }
}


