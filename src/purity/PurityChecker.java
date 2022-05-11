package purity;

import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.diff.ExtractOperationRefactoring;
import gr.uom.java.xmi.diff.RenameClassRefactoring;
import gr.uom.java.xmi.diff.RenameVariableRefactoring;
import gr.uom.java.xmi.diff.UMLModelDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;

import java.util.*;

public class PurityChecker {
    public static Map<Refactoring, PurityCheckResult> isPure(List<Refactoring> refactorings){
        //            List<Refactoring> refactorings = modelDiff.getRefactorings();
        Map<Refactoring, PurityCheckResult> purityCheckResults = new LinkedHashMap<>();

        for (Refactoring refactoring: refactorings){
            purityCheckResults.put(refactoring, isPure(refactoring));
        }
        return purityCheckResults;
    }

    private static PurityCheckResult isPure(Refactoring refactoring){
        PurityCheckResult result;
        switch (refactoring.getRefactoringType()){
            case EXTRACT_OPERATION:
                result = detectExtractOperationPurity((ExtractOperationRefactoring) refactoring);
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

    private static PurityCheckResult detectExtractOperationPurity(ExtractOperationRefactoring refactoring) {

        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty()) {
            if (refactoring.getBodyMapper().allMappingsAreExactMatches())
                return new PurityCheckResult(true, "All mappings are matched! - no non-mapped leaves");


            if (refactoring.getReplacements().isEmpty())
                return new PurityCheckResult(true, "There is no replacement! - no non-mapped leaves");


//        Check each mapping to if it violates the mechanics
            for (AbstractCodeMapping mapping : refactoring.getBodyMapper().getMappings()) {
                if (!mapping.getReplacements().isEmpty()) {
                    if (!checkExtractMethodRefactoringMechanics(mapping.getReplacements())) {
                        return new PurityCheckResult(false, "Violating extract method refactoring mechanics - no non-mapped leaves");
                    }
                }
            }
//        Check non-mapped leaves
        } else{
            if (checkReturnExpressionExtractMethodMechanics(refactoring.getBodyMapper().getNonMappedLeavesT2(), refactoring.getReplacements()))
                return new PurityCheckResult(true, "Adding return statement and variable name changed - with non-mapped leaves");
            else
                return new PurityCheckResult(false, "Violating extract method refactoring mechanics - with non-mapped leaves");
        }

        return new PurityCheckResult(true, "Adding return statement or rename variable - END");
    }

    private static boolean checkReturnExpressionExtractMethodMechanics(List<AbstractCodeFragment> nonMappedLeavesT2, Set<Replacement> replacements){


        for (Replacement rm : replacements) {
            for (AbstractCodeFragment st : nonMappedLeavesT2) {
                if (st.getString().contains(rm.getAfter()) && st.getString().contains("return")) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean checkExtractMethodRefactoringMechanics(Set<Replacement> replacements) {


        if (checkIfArgumentReplacedWithReturnOrRenames(replacements))
            return true;

//        if (checkIfAllReplacementsAreRenames(replacements))
//            return true;


        return false;
    }

    private static boolean checkIfArgumentReplacedWithReturnOrRenames(Set<Replacement> replacements) {

        for (Replacement rep: replacements) {
            return rep.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION) ||
                    rep.getType().equals(Replacement.ReplacementType.VARIABLE_NAME);
        }
        return false;
    }

//    private static boolean checkIfAllReplacementsAreRenames(Set<Replacement> replacements) {
//
//        for (Replacement replacement: replacements) {
//            if (!replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME))
//                return false;
//        }
//        return true;
//    }


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


