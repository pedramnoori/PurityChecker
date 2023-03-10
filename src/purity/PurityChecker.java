package purity;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLAttribute;
import gr.uom.java.xmi.UMLClass;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
import gr.uom.java.xmi.decomposition.replacement.VariableReplacementWithMethodInvocation;
import gr.uom.java.xmi.diff.*;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.api.RefactoringMinerTimedOutException;
import org.refactoringminer.api.RefactoringType;

import java.util.*;
import java.util.stream.Collectors;


public class PurityChecker {

    public static Map<Refactoring, PurityCheckResult> isPure(UMLModelDiff modelDiff) throws RefactoringMinerTimedOutException {
        Map<Refactoring, PurityCheckResult> purityCheckResults = new LinkedHashMap<>();
        List<Refactoring> refactorings = new ArrayList<>(modelDiff.getRefactorings());


        for (Refactoring refactoring: refactorings){
            purityCheckResults.put(refactoring, checkPurity(refactoring, refactorings, modelDiff));
        }
        return purityCheckResults;
    }

    private static PurityCheckResult checkPurity(Refactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff){
        PurityCheckResult result = null;
        switch (refactoring.getRefactoringType()){
            case EXTRACT_OPERATION:
//                result = detectExtractOperationPurity((ExtractOperationRefactoring) refactoring, refactorings);
                break;
            case RENAME_CLASS:
//                result = detectRenameClassPurity((RenameClassRefactoring) refactoring, refactorings, modelDiff);
                break;
            case RENAME_VARIABLE:
//                result = detectRenameVariablePurity((RenameVariableRefactoring) refactoring);
                break;
            case RENAME_PARAMETER:
//                result = detectRenameParameterPurity((RenameVariableRefactoring) refactoring);
                break;
            case MOVE_OPERATION:
                result = detectMoveMethodPurity((MoveOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            case PUSH_DOWN_OPERATION:
                result = detectPushDownMethodPurity((PushDownOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            case INLINE_OPERATION:
//                result = detectInlineMethodPurity((InlineOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            default:
                result = null;

        }

        return result;
    }

    private static PurityCheckResult detectPushDownMethodPurity(PushDownOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {

        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty() &&
                refactoring.getBodyMapper().getNonMappedLeavesT1().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT1().isEmpty()) {

            int mappingState = 1;
            String purityComment = "";

            if (refactoring.getReplacements().isEmpty())
                return new PurityCheckResult(true, "There is no replacement! - all mapped", "Identical statements", mappingState);

            HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

            int sizeToCheckBefore = replacementsToCheck.size();

            omitThisPatternReplacements(replacementsToCheck);
            omitPrintAndLogMessagesRelatedReplacements(refactoring, replacementsToCheck);
            omitBooleanVariableDeclarationReplacement(refactoring, replacementsToCheck); // For the runTests commit
            omitEqualStringLiteralsReplacement(replacementsToCheck);
            omitPrimitiveTypeReplacements(refactoring.getReplacements(), replacementsToCheck);
            omitReturnRelatedReplacements(refactoring, replacementsToCheck);

//            PurityUtils.makeAll(modelDiff, refactoring.getOriginalOperation().getClassName(), null);


            omitReplacementRegardingInvertCondition(refactoring, replacementsToCheck);

            omitAnonymousClassDeclarationReplacements(replacementsToCheck);

            omitStringRelatedReplacements(replacementsToCheck);

            int sizeToCheckAfter = replacementsToCheck.size();

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "All replacements are variables' type! - all mapped", "Tolerable changes in the body", mappingState);
            }

            if (sizeToCheckAfter != sizeToCheckBefore) {
                purityComment += "Tolerable changes in the body" + "\n";
            }

            omitMoveMethodRelatedReplacements(refactoring, replacementsToCheck);

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Push Down Method specific changes - all mapped", "Changes are within the Push Down Method refactoring mechanics", mappingState);
            }

            purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring" + "\n";

            checkForRemoveParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the push down method - all mapped", purityComment, mappingState);
            }

            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Attribute on top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForExtractClassOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Class on top of the push down method - all mapped", purityComment, mappingState);
            }


            checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract variable on the top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForExtractMethodOnTop(refactorings, replacementsToCheck, refactoring);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Method on top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move method on the top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForThisPatternReplacement(replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Contains this pattern - all mapped", purityComment, mappingState);
            }

            checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck); // This method also handles the MoveAndRename Method on top
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForPullUpMethodOnTop(refactoring, refactorings, replacementsToCheck, modelDiff);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Pull Up Method on top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Encapsulate refactoring on top of the push down method - all mapped", purityComment, mappingState);
            }

            checkForAddParameterInSubExpressionOnTop(refactoring, refactorings, replacementsToCheck);

            checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the push down method - all mapped", purityComment, mappingState);
            }

            return new PurityCheckResult(false, "Replacements cannot be justified", "Severe changes", mappingState);

        } else {
            if (!checkReplacementsPushDownMethod(refactoring, refactorings)) {
                return new PurityCheckResult(false, "Replacements cannot be justified - with non-mapped leaves or nodes", "Severe changes", 5);
            }

            List<AbstractCodeFragment> nonMappedLeavesT2 = refactoring.getBodyMapper().getNonMappedLeavesT2();
            List<AbstractCodeFragment> nonMappedLeavesT1 = refactoring.getBodyMapper().getNonMappedLeavesT1();
            List<CompositeStatementObject> nonMappedNodesT2 = refactoring.getBodyMapper().getNonMappedInnerNodesT2();
            List<CompositeStatementObject> nonMappedNodesT1 = refactoring.getBodyMapper().getNonMappedInnerNodesT1();

            checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the push down method - with non-mapped leaves or nodes", "Severe Changes", 5);
            }

            checkForRemoveVariableOnTop_nonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1, nonMappedNodesT2, nonMappedNodesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "One or more variables have been removed from the body of the push down method - with non-mapped leaves or nodes", "Severe Changes", 5);
            }

            checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extract Variable on top of the push down method - with non-mapped leaves or nodes", "Severe Changes", 5);
            }

            checkForExtraBreakStatementsWithinSwitch(nonMappedLeavesT1, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extra break statements within a switch statement - with non-mapped leaves or nodes", "Severe Changes", 5);
            }

            checkForLocalizeParameterOnTop(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Localize Parameter on top of the push down method - with non-mapped leaves or nodes", "Severe Changes", 5);
            }

            int size1 = nonMappedLeavesT1.size();
            int size2 = nonMappedLeavesT2.size();
            int returnStatementCounter1 = 0;
            int returnStatementCounter2 = 0;


            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter1++;
                }
            }

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter2++;
                }
            }

            if (size1 == returnStatementCounter1 && size2 == returnStatementCounter2 && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Return expression has been added within the Push Down Method mechanics - with non-mapped leaves", "Severe Changes", 5);
            }


            return new PurityCheckResult(false, "Contains non-mapped leaves or nodes", "Severe Changes", 5);


        }
    }

    private static boolean checkReplacementsPushDownMethod(PushDownOperationRefactoring refactoring, List<Refactoring> refactorings) {

        if (refactoring.getReplacements().isEmpty()) {
            return true;
        }

        HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

//        int sizeToCheckBefore = replacementsToCheck.size();

        omitThisPatternReplacements(replacementsToCheck);
        omitPrintAndLogMessagesRelatedReplacements(refactoring, replacementsToCheck);
        omitBooleanVariableDeclarationReplacement(refactoring, replacementsToCheck); // For the runTests commit
        omitEqualStringLiteralsReplacement(replacementsToCheck);
        omitPrimitiveTypeReplacements(refactoring.getReplacements(), replacementsToCheck);
        omitReturnRelatedReplacements(refactoring, replacementsToCheck);


        omitReplacementRegardingInvertCondition(refactoring, replacementsToCheck);

        omitAnonymousClassDeclarationReplacements(replacementsToCheck);
        omitMoveMethodRelatedReplacements(refactoring, replacementsToCheck);
        omitStringRelatedReplacements(replacementsToCheck);



        if (replacementsToCheck.isEmpty()) {
            return true;
        }

        checkForRemoveParameterOnTop(refactoring, refactorings, replacementsToCheck);
        checkForRenameVariableOnTop(refactorings, replacementsToCheck);
        checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
        checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
        checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
        checkForExtractClassOnTop(refactorings, replacementsToCheck);
        checkForExtractMethodOnTop(refactorings, replacementsToCheck, refactoring);
        checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
        checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
        checkForThisPatternReplacement(replacementsToCheck);
        checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);
        checkForRemoveVariableOnTop(refactoring, refactorings, replacementsToCheck);
        checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);



        checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
        checkForAddParameterInSubExpressionOnTop(refactoring, refactorings, replacementsToCheck);
        checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);

        if (replacementsToCheck.isEmpty()) {
            return true;
        }


        return false;
    }

    private static PurityCheckResult detectInlineMethodPurity(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {

        System.out.println("Inline purity detection");

        if (refactoring.getBodyMapper().getNonMappedLeavesT1().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT1().isEmpty()) {

            int mappingState = 1;
            String purityComment = "";

            if (refactoring.getReplacements().isEmpty()) {
                purityComment = "Identical statements";
                return new PurityCheckResult(true, "There is no replacement! - all mapped", purityComment, mappingState);
            }


            HashSet<Replacement> replacementsToCheck;
            replacementsToCheck = new HashSet<>(refactoring.getReplacements());

            if (refactoring.getBodyMapper().allMappingsArePurelyMatched(refactoring.getParameterToArgumentMap())) {
                purityComment = "Changes are within the Extract Method refactoring mechanics";
                return new PurityCheckResult(true, "All the mappings are matched! - all mapped", purityComment, mappingState);
            }

            replacementsToCheck = refactoring.getBodyMapper().omitReplacementsRegardingExactMappings(refactoring.getParameterToArgumentMap(), replacementsToCheck);
            replacementsToCheck = refactoring.getBodyMapper().omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);

            if (refactoring.getBodyMapper().allMappingsArePurelyMatched(refactoring.getParameterToArgumentMap())) {
                purityComment = "Changes are within the Extract Method refactoring mechanics";
                return new PurityCheckResult(true, "All the mappings are matched! - all mapped", purityComment, mappingState);
            }

            omitThisPatternReplacements(replacementsToCheck);
            omitPrintAndLogMessagesRelatedReplacements(refactoring, replacementsToCheck);
            omitBooleanVariableDeclarationReplacement(refactoring, replacementsToCheck);
            omitEqualStringLiteralsReplacement(replacementsToCheck);
            omitPrimitiveTypeReplacements(refactoring.getReplacements(), replacementsToCheck);

            omitReplacementRegardingInvertCondition(refactoring, replacementsToCheck);

            checkForTernaryThenReplacement(refactoring, replacementsToCheck);
            
            checkForVariableReplacedWithMethodInvocationSpecialCases(refactoring, replacementsToCheck); // For this special case: https://github.com/netty/netty/commit/d31fa31cdcc5ea2fa96116e3b1265baa180df58a#diff-8976fed22cf939e3b9a8a4eba74620d04992dbce5ffb16769df9fcb1019bec7a
            omitAnonymousClassDeclarationReplacements(replacementsToCheck);
            omitStringRelatedReplacements(replacementsToCheck);


            if (replacementsToCheck.isEmpty()) {
                purityComment += "Tolerable changes in the body" + "\n";
                return new PurityCheckResult(true, "All replacements have been justified - all mapped", purityComment, mappingState);

            }

            purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring" + "\n";


            checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method Refactoring on the top of the Inline Method - all mapped", purityComment, mappingState);
            }

//            checkForRemoveVariableOnTop(refactoring, refactorings, replacementsToCheck);
//            if (replacementsToCheck.isEmpty()) {
//                return new PurityCheckResult(true, "One or more variables have been removed from the body of the moved method - all mapped", purityComment, mappingState);
//            }

            checkForRemoveParameterOnTopInline(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the inlined method - all mapped", purityComment, mappingState);
            }

            checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the inlined method - all mapped", purityComment, mappingState);
            }

            checkForAddParameterInSubExpressionOnTop(refactoring, refactorings, replacementsToCheck);

            checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the inlined method - all mapped", purityComment, mappingState);
            }

            checkForInlineMethodOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Inline Method on top of the inlined method - all mapped", purityComment, mappingState);
            }

            checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Variable on top of the inlined method - all mapped", purityComment, mappingState);
            }

            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the inlined method - all mapped", purityComment, mappingState);
            }

            checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Encapsulate refactoring on top of the inlined method - all mapped", purityComment, mappingState);
            }

            checkForInlineVariableOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Inline Variable on top of the inlined method - all mapped", purityComment, mappingState);
            }

            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the inlined method - all mapped", purityComment, mappingState);
            }

            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename class on the top of the inlined method - all mapped", purityComment, mappingState);
            }

            checkTheReplacementsAlreadyHandled(refactoring, replacementsToCheck);
            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "One of the overlapping cases - all mapped", purityComment, mappingState);
            }

            int size1 = replacementsToCheck.size();
            int numberOfArgumentReplacedWithReturnReplacements = 0;

            for (Replacement replacement : replacementsToCheck) {
                if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                    numberOfArgumentReplacedWithReturnReplacements++;
                }
            }

            if (numberOfArgumentReplacedWithReturnReplacements == size1) {
                return new PurityCheckResult(true, "Argument replaced with return expression - all mapped", purityComment, mappingState);
            }

            purityComment = "Severe changes";
            return new PurityCheckResult(false, "Replacements cannot be justified", purityComment, mappingState);

        } else if (refactoring.getBodyMapper().getNonMappedInnerNodesT1().isEmpty()) {
            int mappingState = 2;
            String purityComment = "";

            List<AbstractCodeFragment> nonMappedLeavesT1 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT1());

            int size = nonMappedLeavesT1.size();
            checkForInevitableVariableDeclarationInline(refactoring, nonMappedLeavesT1, refactorings); // This method can also change the state of the refactoring's replacements
            int size2 = nonMappedLeavesT1.size();

            if (size != size2) {
                purityComment += "Severe changes + \n";
            }

            if (!checkReplacementsInlineMethod(refactoring, refactorings)) {
                purityComment = "Severe changes";
                return new PurityCheckResult(false, "replacements are not justified - non-mapped leaves", purityComment, mappingState);
            }

            if (nonMappedLeavesT1.isEmpty()) {
                return new PurityCheckResult(true, "Variable declaration hasn't been inlined, but it doesn't change the logic - non-mapped leaves", purityComment, mappingState);
            }

            checkForStatementsBeingMappedInTargetOperation(refactoring, refactorings, nonMappedLeavesT1, modelDiff);
            checkForNestedInlineMethod(refactoring, refactorings, nonMappedLeavesT1);

            if (nonMappedLeavesT1.isEmpty()) {
                purityComment += "Severe changes";
                return new PurityCheckResult(true, "Nested Inline Method or statements being mapped in other refactorings - non-mapped leaves", purityComment, mappingState);
            }

            checkForRemoveAttributeOnTop(refactoring, modelDiff, nonMappedLeavesT1);

            if (nonMappedLeavesT1.isEmpty()) {
                purityComment += "Severe changes";
                return new PurityCheckResult(true, "Remove Attribute change on top of the Inline Method - non-mapped leaves", purityComment, mappingState);
            }

            checkForInlineVariableNonMappedLeaves(refactoring, refactorings, nonMappedLeavesT1);


            if (nonMappedLeavesT1.isEmpty()) {
                purityComment += "Severe changes";
                return new PurityCheckResult(true, "Inline Variable on top of the Inline Method - non-mapped leaves", purityComment, mappingState);
            }

            checkForRenameRefactoringOnTopOfInline_NonMapped(refactoring, refactorings, nonMappedLeavesT1);

            if (nonMappedLeavesT1.isEmpty()) {
                purityComment += "Severe changes";
                return new PurityCheckResult(true, "Rename Method on top of the Inline Method - non-mapped leaves", purityComment, mappingState);
            }

            checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT1);
            if (nonMappedLeavesT1.isEmpty()) {
                purityComment += "Severe changes";
                return new PurityCheckResult(true, "Extract Variable on the top of the Inline Method - with non-mapped leaves", purityComment, mappingState);
            }



            return new PurityCheckResult(false, "Violating the mechanics of Inline Method refactoring", purityComment, mappingState);
        } else {
            int mappingState = 3;
            String purityComment = "";

            List<AbstractCodeFragment> nonMappedInnerNodesT1 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedInnerNodesT1());

            if (!checkReplacementsInlineMethod(refactoring, refactorings)) {
                purityComment = "Severe changes";
                return new PurityCheckResult(false, "replacements are not justified - non-mapped leaves", purityComment, mappingState);
            }

            List<AbstractCodeFragment> nonMappedLeavesT1List = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT1());

            checkForIfCondition(refactoring, nonMappedInnerNodesT1, nonMappedLeavesT1List);

            if (!checkForNonMappedLeavesT1(refactoring, refactorings, nonMappedLeavesT1List, modelDiff)) {
                purityComment = "Severe changes";
                return new PurityCheckResult(false, "non-mapped are not justified - non-mapped leaves", purityComment, mappingState);
            }

            checkForRemoveAttributeOnTop(refactoring, modelDiff, nonMappedInnerNodesT1);

            if (nonMappedInnerNodesT1.isEmpty()) {
                purityComment = "Severe changes";
                return new PurityCheckResult(true, "Remove Attribute change on top of the Inline Method - non-mapped leaves", purityComment, mappingState);
            }

            int numberOfWrongNonMappedBlocks = 0;
            for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT1) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                    numberOfWrongNonMappedBlocks++;
                }else {
                    break;
                }
            }

            if (numberOfWrongNonMappedBlocks == nonMappedInnerNodesT1.size()) {
                purityComment = "Identical statements";
                return new PurityCheckResult(true, "Just an empty block - with non-mapped leaves", purityComment, mappingState);
            }


            return new PurityCheckResult(false, "Violating the mechanics of Inline Method refactoring", purityComment, mappingState);
        }
    }

    private static void checkForRemoveVariableOnTop(Refactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        } else {
            return;
        }

        if (bodyMapper.getRemovedVariables() != null) {
            if (bodyMapper.getRemovedVariables().isEmpty()) {
                return;
            }
        } else {
            return;
        }

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (VariableDeclaration removedVariable : bodyMapper.getRemovedVariables()) {
            for (Replacement replacement : replacementsToCheck) {
                if (replacement.getBefore().contains(removedVariable.getVariableName())) {
                    replacementsToRemove.add(replacement);
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);

    }

    private static void checkTheReplacementsAlreadyHandled(Refactoring refactoring, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacements = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            replacements = ((ExtractOperationRefactoring) (refactoring)).getReplacements();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            replacements = ((InlineOperationRefactoring) (refactoring)).getReplacements();
        }else {
            return;
        }

        Set<Replacement> replacementsToRemove = new HashSet<>();
        Set<Replacement> handledReplacements = new HashSet<>();

        handledReplacements.addAll(replacements);
        handledReplacements.removeAll(replacementsToCheck);


        for (Replacement replacement : handledReplacements) {
            for (Replacement refactoringReplacement : replacementsToCheck) {
                if (replacement.getBefore().equals(refactoringReplacement.getBefore()) ||
                        replacement.getAfter().equals(refactoringReplacement.getAfter())) {
                    replacementsToRemove.add(refactoringReplacement);
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForVariableReplacedWithMethodInvocationSpecialCases(InlineOperationRefactoring refactoring, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION)) {
                if (((VariableReplacementWithMethodInvocation) replacement).getDirection().name().equals("VARIABLE_TO_INVOCATION")) {
                    if (replacement.getAfter().contains(replacement.getBefore())) {
                        replacementsToRemove.add(replacement);
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForRemoveAttributeOnTop(InlineOperationRefactoring refactoring, UMLModelDiff modelDiff, List<AbstractCodeFragment> nonMappedLeavesT1) {

        String className = refactoring.getInlinedOperation().getClassName();
        if (!modelDiff.getUMLClassDiff(className).getRemovedAttributes().isEmpty()) {

            List<UMLAttribute> removedAttributes = modelDiff.getUMLClassDiff(className).getRemovedAttributes();
            List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                for (UMLAttribute removedAttribute : removedAttributes) {
                    if (abstractCodeFragment.getArgumentizedString().contains(removedAttribute.getName())) {
                        nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                    }
                }
            }
            nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
        }
    }


    private static void checkForTernaryThenReplacement(InlineOperationRefactoring refactoring, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.EXPRESSION_REPLACED_WITH_TERNARY_THEN) ||
                    replacement.getType().equals(Replacement.ReplacementType.CONDITIONAL)) {
                AbstractCodeMapping mapping = findTheMapping(replacement, refactoring);
                if (!mapping.getFragment1().getTernaryOperatorExpressions().isEmpty()) {
                    for (TernaryOperatorExpression ternaryOperatorExpression : mapping.getFragment1().getTernaryOperatorExpressions()) {
                        AbstractExpression condition = ternaryOperatorExpression.getCondition();
                        if (mapping.getFragment2().getParent().getParent() != null) {
                            if (mapping.getFragment2().getParent().getParent().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
                                if (mapping.getFragment2().getParent().getParent().getExpressions().size() == 1) {
                                    if (mapping.getFragment2().getParent().getParent().getExpressions().get(0).getExpression().equals(condition.getExpression()))
                                        replacementsToRemove.add(replacement);
                                }
                            }
                        }
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);

    }

    private static void checkForIfCondition(InlineOperationRefactoring refactoring, List<AbstractCodeFragment> nonMappedInnerNodesT1, List<AbstractCodeFragment> nonMappedLeavesT1List) {

        List<AbstractCodeFragment> nonMappedInnerNodesT1ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();


        for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT1) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
                abstractCodeFragment.argumentizationAfterRefactorings(refactoring.getParameterToArgumentMap());
                abstractCodeFragment.replaceParametersWithArguments(refactoring.getParameterToArgumentMap());
                if (abstractCodeFragment.getArgumentizedAfterRefactorings().equals("if(false)")) {
                    nonMappedInnerNodesT1ToRemove.add(abstractCodeFragment);
                    List<AbstractStatement> statements = ((CompositeStatementObject) (abstractCodeFragment)).getStatements();
                    if (statements.size() == 2 || statements.size() == 1) { //it's an if-else condition
                        nonMappedLeavesT1ToRemove.addAll(((CompositeStatementObject)(statements.get(0))).getStatements());
                    }
                } else if (abstractCodeFragment.getArgumentizedAfterRefactorings().equals("if(true)")) {
                    nonMappedInnerNodesT1ToRemove.add(abstractCodeFragment);
                    List<AbstractStatement> statements = ((CompositeStatementObject) (abstractCodeFragment)).getStatements();
                    if (statements.size() == 2) {
                        nonMappedLeavesT1ToRemove.addAll(((CompositeStatementObject)(statements.get(1))).getStatements());
                    }
                }
            }
        }
        nonMappedInnerNodesT1.removeAll(nonMappedInnerNodesT1ToRemove);
        nonMappedLeavesT1List.removeAll(nonMappedLeavesT1ToRemove);

    }

    private static boolean checkForNonMappedLeavesT1(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1List, UMLModelDiff modelDiff) {

        if (nonMappedLeavesT1List.isEmpty()) {
            return true;
        }

        checkForInevitableVariableDeclarationInline(refactoring, nonMappedLeavesT1List, refactorings); // This method can also change the state of the refactoring's replacements
        checkForStatementsBeingMappedInTargetOperation(refactoring, refactorings, nonMappedLeavesT1List, modelDiff);
        checkForNestedInlineMethod(refactoring, refactorings, nonMappedLeavesT1List);
        checkForInlineVariableNonMappedLeaves(refactoring, refactorings, nonMappedLeavesT1List);
        checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT1List);
        checkForRemoveAttributeOnTop(refactoring, modelDiff, nonMappedLeavesT1List);


        if (nonMappedLeavesT1List.isEmpty()) {
            return true;
        }

        int size = nonMappedLeavesT1List.size();
        int returnStatementCounter = 0;

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1List) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                returnStatementCounter++;
            }
        }

        if (size == returnStatementCounter) {
            return true;
        }

        return false;
    }

    private static void checkForInlineVariableNonMappedLeaves(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1) {

        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.INLINE_VARIABLE)) {
                VariableDeclaration variableDeclaration = ((InlineVariableRefactoring) (refactoring1)).getVariableDeclaration();
                for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                    if (abstractCodeFragment.getVariableDeclarations().contains(variableDeclaration)) {
                        nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                    }
                }
            }
        }

        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);

    }

    private static void checkForInlineVariableOnTop(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            AbstractCodeMapping mapping = findTheMapping(replacement, refactoring);

            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.INLINE_VARIABLE)) {
                    if (((InlineVariableRefactoring) refactoring1).getReferences().contains(mapping)) {
                        replacementsToRemove.add(replacement);
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForNestedInlineMethod(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1) {

        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {

            for (AbstractCall methodInvocation : abstractCodeFragment.getMethodInvocations()) {
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.INLINE_OPERATION) && !refactoring1.equals(refactoring)) {
                        if (methodInvocation.getName().equals(((InlineOperationRefactoring) (refactoring1)).getInlinedOperation().getName())) {
                            if (((InlineOperationRefactoring) refactoring1).getTargetOperationAfterInline().getName().equals(refactoring.getTargetOperationAfterInline().getName())) {
                                nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                            }
                        }
                    }
                }
            }
            //TODO: Double check code above
//            for (Map.Entry<String, List<AbstractCall>> stringListEntry : abstractCodeFragment.getMethodInvocationMap().entrySet()) {
//                for (Refactoring refactoring1 : refactorings) {
//                    if (refactoring1.getRefactoringType().equals(RefactoringType.INLINE_OPERATION) && !refactoring1.equals(refactoring)) {
//                        if (stringListEntry.getValue().get(0).getName().equals(((InlineOperationRefactoring) (refactoring1)).getInlinedOperation().getName())) {
//                            if (((InlineOperationRefactoring) refactoring1).getTargetOperationAfterInline().getName().equals(refactoring.getTargetOperationAfterInline().getName())) {
//                                nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
//                            }
//                        }
//                    }
//                }
//            }
        }
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
    }

    private static void checkForStatementsBeingMappedInTargetOperation(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1, UMLModelDiff modelDiff) {

        UMLClassBaseDiff umlClassDiff = modelDiff.getUMLClassDiff(refactoring.getTargetOperationAfterInline().getClassName());
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();


        for (UMLOperationBodyMapper umlOperationBodyMapper : umlClassDiff.getOperationBodyMapperList()) {
            if (umlOperationBodyMapper != null) {
                if (umlOperationBodyMapper.getOperation2().getName().equals(refactoring.getTargetOperationAfterInline().getName())) {
                    for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                        for (AbstractCodeMapping mapping : umlOperationBodyMapper.getMappings()) {
                            if (mapping.getFragment2().getString().equals(abstractCodeFragment.getString())) {
                                nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                            }
                        }
                    }
                }
            }
        }
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
    }

    private static void checkForInlineMethodOnTop(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if ((replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT)) ||
                    ((replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) && ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getName())))) {
                String invokedOperationAfterName = ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName();
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
                        if (((InlineOperationRefactoring) refactoring1).getTargetOperationAfterInline().getName().equals(invokedOperationAfterName)) {
                            if (((InlineOperationRefactoring) refactoring1).getTargetOperationAfterInline().getParameterNameList().size() == ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().arguments().size()) {
                                replacementsToRemove.add(replacement);
                                break;
                            }
                        }
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static boolean checkReplacementsInlineMethod(InlineOperationRefactoring refactoring, List<Refactoring> refactorings) {

        HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

        if (replacementsToCheck.isEmpty()) {
            return true;
        }

        omitThisPatternReplacements(replacementsToCheck);
        omitPrintAndLogMessagesRelatedReplacements(refactoring, replacementsToCheck);
        omitBooleanVariableDeclarationReplacement(refactoring, replacementsToCheck);
        omitEqualStringLiteralsReplacement(replacementsToCheck);
        omitPrimitiveTypeReplacements(refactoring.getReplacements(), replacementsToCheck);
        omitReplacementRegardingInvertCondition(refactoring, replacementsToCheck);
        checkForTernaryThenReplacement(refactoring, replacementsToCheck);
        omitAnonymousClassDeclarationReplacements(replacementsToCheck);
        omitStringRelatedReplacements(replacementsToCheck);



        if (replacementsToCheck.isEmpty()) {
            return true;
        }


        checkForRemoveParameterOnTop(refactoring, refactorings, replacementsToCheck);
        checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
        checkForInlineMethodOnTop(refactoring, refactorings, replacementsToCheck);
        checkForInlineVariableOnTop(refactoring, refactorings, replacementsToCheck);
        checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
        checkForRenameVariableOnTop(refactorings, replacementsToCheck);
        checkForRemoveVariableOnTop(refactoring, refactorings, replacementsToCheck);
        checkForVariableReplacedWithMethodInvocationSpecialCases(refactoring, replacementsToCheck); // For this special case: https://github.com/netty/netty/commit/d31fa31cdcc5ea2fa96116e3b1265baa180df58a#diff-8976fed22cf939e3b9a8a4eba74620d04992dbce5ffb16769df9fcb1019bec7a
        checkTheReplacementsAlreadyHandled(refactoring, replacementsToCheck);
        checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);


        if (replacementsToCheck.isEmpty()) {
            return true;
        }

        int size1 = replacementsToCheck.size();
        int numberOfArgumentReplacedWithReturnReplacements = 0;

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                numberOfArgumentReplacedWithReturnReplacements++;
            }
        }

        if (numberOfArgumentReplacedWithReturnReplacements == size1) {
            return true;
        }

        return false;
    }

    private static PurityCheckResult detectMoveMethodPurity(MoveOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {

/*
Mapping state for Move Method refactoring purity:

1: When there is no nan-mapped leaves or nodes
2: When the non-mapped leaves in T2
3:
4:
5: When the number of parameters is different between the two targeted methods.

 */
        System.out.println("MOVE METHOD");

//        if (refactoring.getOriginalOperation().getParameterNameList().size() != refactoring.getMovedOperation().getParameterNameList().size()) {
//
//            return new PurityCheckResult(false, "Number of parameters is different. Strong clue of impurity in case of Move Method refactorings!", "Severe changes", 5);
//        }

        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty() &&
        refactoring.getBodyMapper().getNonMappedLeavesT1().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT1().isEmpty()) {

            int mappingState = 1;
            String purityComment = "";

            if (refactoring.getReplacements().isEmpty())
                return new PurityCheckResult(true, "There is no replacement! - all mapped", "Identical statements", mappingState);

            HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

            int sizeToCheckBefore = replacementsToCheck.size();

            omitThisPatternReplacements(replacementsToCheck);
            omitPrintAndLogMessagesRelatedReplacements(refactoring, replacementsToCheck);
            omitBooleanVariableDeclarationReplacement(refactoring, replacementsToCheck); // For the runTests commit
            omitEqualStringLiteralsReplacement(replacementsToCheck);
            omitPrimitiveTypeReplacements(refactoring.getReplacements(), replacementsToCheck);
            omitReturnRelatedReplacements(refactoring, replacementsToCheck);

//            PurityUtils.makeAll(modelDiff, refactoring.getOriginalOperation().getClassName(), null);


            omitReplacementRegardingInvertCondition(refactoring, replacementsToCheck);

            omitAnonymousClassDeclarationReplacements(replacementsToCheck);

            omitStringRelatedReplacements(replacementsToCheck);

            int sizeToCheckAfter = replacementsToCheck.size();

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "All replacements are variables' type! - all mapped", "Tolerable changes in the body", mappingState);
            }

            if (sizeToCheckAfter != sizeToCheckBefore) {
                purityComment += "Tolerable changes in the body" + "\n";
            }

            omitMoveMethodRelatedReplacements(refactoring, replacementsToCheck);

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Method specific changes - all mapped", "Changes are within the Move Method refactoring mechanics", mappingState);
            }

            purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring" + "\n";

            checkForRemoveParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the moved method - all mapped", purityComment, mappingState);
            }

            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Attribute on top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForExtractClassOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Class on top of the moved method - all mapped", purityComment, mappingState);
            }

//            checkForRemoveVariableOnTop(refactoring, refactorings, replacementsToCheck);
//            if (replacementsToCheck.isEmpty()) {
//                return new PurityCheckResult(true, "One or more variables have been removed from the body of the moved method - all mapped", purityComment, mappingState);
//            }

            checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract variable on the top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForExtractMethodOnTop(refactorings, replacementsToCheck, refactoring);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Method on top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move method on the top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForThisPatternReplacement(replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Contains this pattern - all mapped", purityComment, mappingState);
            }

            checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck); // This method also handles the MoveAndRename Method on top
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForPullUpMethodOnTop(refactoring, refactorings, replacementsToCheck, modelDiff);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Pull Up Method on top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Encapsulate refactoring on top of the moved method - all mapped", purityComment, mappingState);
            }

            checkForAddParameterInSubExpressionOnTop(refactoring, refactorings, replacementsToCheck);

            checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the moved method - all mapped", purityComment, mappingState);
            }

            return new PurityCheckResult(false, "Replacements cannot be justified", "Severe changes", mappingState);

        } else {

            if (!checkReplacementsMoveMethod(refactoring, refactorings)) {
                return new PurityCheckResult(false, "Replacements cannot be justified - with non-mapped leaves or nodes", "Severe changes", 5);

            }

            List<AbstractCodeFragment> nonMappedLeavesT2 = refactoring.getBodyMapper().getNonMappedLeavesT2();
            List<AbstractCodeFragment> nonMappedLeavesT1 = refactoring.getBodyMapper().getNonMappedLeavesT1();
            List<CompositeStatementObject> nonMappedNodesT2 = refactoring.getBodyMapper().getNonMappedInnerNodesT2();
            List<CompositeStatementObject> nonMappedNodesT1 = refactoring.getBodyMapper().getNonMappedInnerNodesT1();

            checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method on top of the moved method - with non-mapped leaves or nodes", "Severe Changes", 5);
            }

            checkForRemoveVariableOnTop_nonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1, nonMappedNodesT2, nonMappedNodesT1);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "One or more variables have been removed from the body of the moved method - with non-mapped leaves or nodes", "Severe Changes", 5);
            }

            checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extract Variable on top of the moved method - with non-mapped leaves or nodes", "Severe Changes", 5);
            }

            checkForExtraBreakStatementsWithinSwitch(nonMappedLeavesT1, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Extra break statements within a switch statement - with non-mapped leaves or nodes", "Severe Changes", 5);
            }

            checkForLocalizeParameterOnTop(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty() && nonMappedLeavesT1.isEmpty() && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Localize Parameter on top of the moved method - with non-mapped leaves or nodes", "Severe Changes", 5);
            }

            int size1 = nonMappedLeavesT1.size();
            int size2 = nonMappedLeavesT2.size();
            int returnStatementCounter1 = 0;
            int returnStatementCounter2 = 0;


            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter1++;
                }
            }

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter2++;
                }
            }

            if (size1 == returnStatementCounter1 && size2 == returnStatementCounter2 && nonMappedNodesT2.isEmpty() && nonMappedNodesT1.isEmpty()) {
                return new PurityCheckResult(true, "Return expression has been added within the Move Method mechanics - with non-mapped leaves", "Severe Changes", 5);
            }


            return new PurityCheckResult(false, "Contains non-mapped leaves or nodes", "Severe Changes", 5);
        }

    }

    private static void checkForLocalizeParameterOnTop(MoveOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2) {

        UMLOperation originalOperation = null;
        UMLOperation movedOperation = null;

        


        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.LOCALIZE_PARAMETER)) {
                if (refactoring.getOriginalOperation().equals(((RenameVariableRefactoring) (refactoring1)).getOperationBefore()) &&
                        refactoring.getMovedOperation().equals(((RenameVariableRefactoring) (refactoring1)).getOperationAfter())) {
                    for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                        for (VariableDeclaration variableDeclaration : abstractCodeFragment.getVariableDeclarations()) {
                            if (variableDeclaration.equals(((RenameVariableRefactoring) (refactoring1)).getRenamedVariable())) {
                                nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                            }
                        }
                    }
                }
            }
        }

        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);

    }

    private static void checkForEncapsulateAttributeOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION)) {
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.ENCAPSULATE_ATTRIBUTE)) {
                        if (replacement.getBefore().equals(((EncapsulateAttributeRefactoring) refactoring1).getAttributeBefore().getName())) {
                            if (((EncapsulateAttributeRefactoring) refactoring1).getAddedGetter() != null) {
                                if (((EncapsulateAttributeRefactoring) refactoring1).getAddedGetter().getName().equals(((VariableReplacementWithMethodInvocation) replacement).getInvokedOperation().getName())) {
                                    replacementsToRemove.add(replacement);
                                }
                            } else if (((EncapsulateAttributeRefactoring) refactoring1).getAddedSetter() != null) {
                                if (((EncapsulateAttributeRefactoring) refactoring1).getAddedSetter().getName().equals(((VariableReplacementWithMethodInvocation) replacement).getInvokedOperation().getName())) {
                                    replacementsToRemove.add(replacement);
                                }
                            }
                        }
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForExtraBreakStatementsWithinSwitch(List<AbstractCodeFragment> nonMappedLeavesT1, List<AbstractCodeFragment> nonMappedLeavesT2) {

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BREAK_STATEMENT)) {
                if (abstractCodeFragment.getParent().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.SWITCH_STATEMENT)) {
                    nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                }
            }
        }

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BREAK_STATEMENT)) {
                if (abstractCodeFragment.getParent().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.SWITCH_CASE)) {
                    nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                }
            }
        }
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);


    }

    private static void omitStringRelatedReplacements(HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getBefore().replaceAll("\"", "").replaceAll("'", "").equals(replacement.getAfter().replaceAll("\"", "").replaceAll("'", ""))) {
                replacementsToRemove.add(replacement);
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void omitMoveMethodRelatedReplacements(Refactoring refactoring, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName().equals(
                        ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName()
                )) {
                    if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments().equals(
                            ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments()
                    )) {
                        String before = replacement.getBefore();
                        String after = replacement.getAfter();

                        int foundInAfter = after.indexOf(before);
                        int foundInBefore = before.indexOf(after);
                        String classBefore = "";
                        String classAfter = "";

                        if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
                            classBefore = ((MoveOperationRefactoring) (refactoring)).getOriginalOperation().getNonQualifiedClassName();
                            classAfter = ((MoveOperationRefactoring) (refactoring)).getMovedOperation().getNonQualifiedClassName();
                        }else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
                            classBefore = ((PushDownOperationRefactoring) (refactoring)).getOriginalOperation().getNonQualifiedClassName();
                            classAfter = ((PushDownOperationRefactoring) (refactoring)).getMovedOperation().getNonQualifiedClassName();
                        }

                        if (foundInBefore != -1) {
                            String instanceOrVariable = before.substring(0, foundInBefore - 1);
                            if (instanceOrVariable.equals(classAfter)) {
                                replacementsToRemove.add(replacement);
                            }

                        }else if (foundInAfter != -1) {
                            String instanceOrVariable = after.substring(0, foundInAfter - 1);
                            if (instanceOrVariable.equals(classBefore)) {
                                replacementsToRemove.add(replacement);
                            }
                        }
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForPullUpMethodOnTop(Refactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck, UMLModelDiff modelDiff) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName().equals(
                        ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName()
                )) {
                if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments().equals(
                        ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments()
                    )) { //Taking into account that two lists will be equal in Java when the order of insertion was the same along with the values themselves

                        String before = replacement.getBefore();
                        String after = replacement.getAfter();
                        String methodInvocationName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();

                    for (Refactoring refactoring1 : refactorings) {
                        if (refactoring1.getRefactoringType().equals(RefactoringType.PULL_UP_OPERATION)) {
                           if (((PullUpOperationRefactoring) refactoring1).getOriginalOperation().getName().equals(methodInvocationName)) {
                               int foundInAfter = after.indexOf(before);
                               int foundInBefore = before.indexOf(after);

                               if (foundInBefore != -1) {
                                   String instanceOrVariable = before.substring(0, foundInBefore - 1);
                                   if (specificCheckForPullUpMethodOnTopFoundInBefore(refactoring, ((PullUpOperationRefactoring) refactoring1), instanceOrVariable, (MethodInvocationReplacement) replacement, modelDiff)) {
                                       replacementsToRemove.add(replacement);
                                   }
                               } else if (foundInAfter != -1) {
                                   String instanceOrVariable = after.substring(0, foundInAfter - 1);
                                   if (specificCheckForPullUpMethodOnTopFoundInAfter(refactoring, ((PullUpOperationRefactoring) refactoring1), instanceOrVariable, (MethodInvocationReplacement) replacement, modelDiff)) {
                                       replacementsToRemove.add(replacement);
                                   }
                               }
                           }
                        }
                    }
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static boolean specificCheckForPullUpMethodOnTopFoundInBefore(Refactoring refactoring, PullUpOperationRefactoring pullUpOperationRefactoring, String instanceOrVariable, MethodInvocationReplacement replacement, UMLModelDiff modelDiff) {
        if (pullUpOperationRefactoring.getOriginalOperation().getClassName().equals(instanceOrVariable)) {
            return true;
        }

//        Search in the class or method for the attribute or variable
        for (UMLClass umlClass : modelDiff.getParentModel().getClassList()) {
            if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
                if (umlClass.getNonQualifiedName().equals(((PushDownOperationRefactoring) (refactoring)).getOriginalOperation().getNonQualifiedClassName())) {
                    for (UMLAttribute attribute : umlClass.getAttributes()) {
                        if (attribute.getName().equals(instanceOrVariable)) {
                            if (attribute.getType().getClassType().equals(pullUpOperationRefactoring.getOriginalOperation().getNonQualifiedClassName())) {
                                return true;
                            }
                        }
                    }
                }
            }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
                if (umlClass.getNonQualifiedName().equals(((MoveOperationRefactoring) (refactoring)).getOriginalOperation().getNonQualifiedClassName())) {
                    for (UMLAttribute attribute : umlClass.getAttributes()) {
                        if (attribute.getName().equals(instanceOrVariable)) {
                            if (attribute.getType().getClassType().equals(pullUpOperationRefactoring.getOriginalOperation().getNonQualifiedClassName())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static boolean specificCheckForPullUpMethodOnTopFoundInAfter(Refactoring refactoring, PullUpOperationRefactoring pullUpOperationRefactoring, String instanceOrVariable, MethodInvocationReplacement replacement, UMLModelDiff modelDiff) {
        if (pullUpOperationRefactoring.getMovedOperation().getClassName().equals(instanceOrVariable)) {
            return true;
        }

//        Search in the class or method for the attribute or variable
        for (UMLClass umlClass : modelDiff.getChildModel().getClassList()) {
            if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
                if (umlClass.getNonQualifiedName().equals(((PushDownOperationRefactoring) (refactoring)).getMovedOperation().getNonQualifiedClassName())) {
                    for (UMLAttribute attribute : umlClass.getAttributes()) {
                        if (attribute.getName().equals(instanceOrVariable)) {
                            if (attribute.getType().getClassType().equals(pullUpOperationRefactoring.getMovedOperation().getNonQualifiedClassName())) {
                                return true;
                            }
                        }
                    }
                }
            } else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
                if (umlClass.getNonQualifiedName().equals(((MoveOperationRefactoring) (refactoring)).getMovedOperation().getNonQualifiedClassName())) {
                    for (UMLAttribute attribute : umlClass.getAttributes()) {
                        if (attribute.getName().equals(instanceOrVariable)) {
                            if (attribute.getType().getClassType().equals(pullUpOperationRefactoring.getMovedOperation().getNonQualifiedClassName())) {
                                return true;
                            }
                        }
                    }
                }
            }
        }

        return false;
    }

    private static void checkForRemoveVariableOnTop_nonMapped(Refactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2, List<AbstractCodeFragment> nonMappedLeavesT1, List<CompositeStatementObject> nonMappedNodesT2, List<CompositeStatementObject> nonMappedNodesT1) {



        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        } else {
            return;
        }

        if (bodyMapper.getRemovedVariables().isEmpty()) {
            return;
        }

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT2ToRemove = new ArrayList<>();
        List<CompositeStatementObject> nonMappedNodesT1ToRemove = new ArrayList<>();



        for (VariableDeclaration removedVariable : bodyMapper.getRemovedVariables()) {
            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                if (abstractCodeFragment.getString().contains(removedVariable.getVariableName())) {
                    nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                }
            }

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT1) {
                if (abstractCodeFragment.getString().contains(removedVariable.getVariableName())) {
                    nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                }
            }
            for (CompositeStatementObject compositeStatementObject : nonMappedNodesT2) {
                if (compositeStatementObject.getString().contains(removedVariable.getVariableName())) {
                    nonMappedNodesT2ToRemove.add(compositeStatementObject);
                }
            }

            for (CompositeStatementObject compositeStatementObject : nonMappedNodesT1) {
                if (compositeStatementObject.getString().contains(removedVariable.getVariableName())) {
                    nonMappedNodesT1ToRemove.add(compositeStatementObject);
                }
            }
        }
        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
        nonMappedNodesT2.removeAll(nonMappedNodesT2ToRemove);
        nonMappedNodesT1.removeAll(nonMappedNodesT1ToRemove);

    }

    private static boolean checkReplacementsMoveMethod(MoveOperationRefactoring refactoring, List<Refactoring> refactorings) {
        if (refactoring.getReplacements().isEmpty()) {
            return true;
        }

        HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());

//        int sizeToCheckBefore = replacementsToCheck.size();

        omitThisPatternReplacements(replacementsToCheck);
        omitPrintAndLogMessagesRelatedReplacements(refactoring, replacementsToCheck);
        omitBooleanVariableDeclarationReplacement(refactoring, replacementsToCheck); // For the runTests commit
        omitEqualStringLiteralsReplacement(replacementsToCheck);
        omitPrimitiveTypeReplacements(refactoring.getReplacements(), replacementsToCheck);
        omitReturnRelatedReplacements(refactoring, replacementsToCheck);


        omitReplacementRegardingInvertCondition(refactoring, replacementsToCheck);

        omitAnonymousClassDeclarationReplacements(replacementsToCheck);
        omitMoveMethodRelatedReplacements(refactoring, replacementsToCheck);
        omitStringRelatedReplacements(replacementsToCheck);



//        int sizeToCheckAfter = replacementsToCheck.size();

        if (replacementsToCheck.isEmpty()) {
            return true;
        }

        checkForRemoveParameterOnTop(refactoring, refactorings, replacementsToCheck);
        checkForRenameVariableOnTop(refactorings, replacementsToCheck);
        checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
        checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
        checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
        checkForExtractClassOnTop(refactorings, replacementsToCheck);
        checkForExtractMethodOnTop(refactorings, replacementsToCheck, refactoring);
        checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
        checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
        checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
        checkForThisPatternReplacement(replacementsToCheck);
        checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);
        checkForRemoveVariableOnTop(refactoring, refactorings, replacementsToCheck);
        checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);



        checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
        checkForAddParameterInSubExpressionOnTop(refactoring, refactorings, replacementsToCheck);
        checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);

        if (replacementsToCheck.isEmpty()) {
            return true;
        }


//        if (sizeToCheckAfter != sizeToCheckBefore) {
//            purityComment += "Tolerable changes in the body" + "\n";
//        }


        return false;
    }

    private static void checkForThisPatternReplacement(HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                if (findLongestPrefixSuffix(replacement.getBefore(), replacement.getAfter()).equals("this") ||
                        findLongestPrefixSuffix(replacement.getAfter(), replacement.getBefore()).equals("this")) {
                    replacementsToRemove.add(replacement);
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }


    private static String findLongestPrefixSuffix(String s1, String s2) {

        for( int i = Math.min(s1.length(), s2.length()); ; i--) {
            if(s2.endsWith(s1.substring(0, i))) {
                return s1.substring(0, i);
            }
        }
    }

    private static void checkForMoveMethodRefactoringOnTop(Refactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();
        String methodName = "";
        String className = "";
        Map<String, String> patterns = new HashMap<>();


        if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            methodName = ((MoveOperationRefactoring) refactoring).getMovedOperation().getName();
            className = ((MoveOperationRefactoring) refactoring).getMovedOperation().getNonQualifiedClassName();
        }
        else if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            methodName = ((ExtractOperationRefactoring) refactoring).getExtractedOperation().getName();
            className = ((ExtractOperationRefactoring) refactoring).getExtractedOperation().getNonQualifiedClassName();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            methodName = ((PushDownOperationRefactoring) refactoring).getMovedOperation().getName();
            className = ((PushDownOperationRefactoring) refactoring).getMovedOperation().getNonQualifiedClassName();
        }


        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
                String classNameBeforeRefactoring = ((MoveOperationRefactoring) (refactoring1)).getOriginalOperation().getNonQualifiedClassName();
                String classNameAfterRefactoring = ((MoveOperationRefactoring) (refactoring1)).getMovedOperation().getNonQualifiedClassName();
                String methodNameRefactoring = ((MoveOperationRefactoring) (refactoring1)).getMovedOperation().getName();

                for (Replacement replacement : replacementsToCheck) {
                    if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                        if (((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName()) &&
                                ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName().equals(methodNameRefactoring)) {
                            if (((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getExpression() != null && ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().getExpression() != null) {
                                if (((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getExpression().equals(classNameBeforeRefactoring) &&
                                        ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().getExpression().equals(classNameAfterRefactoring)) {
                                    replacementsToRemove.add(replacement);
                                }
                            } else if (((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getExpression() != null && ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().getExpression() == null) {
                                if (((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getExpression().equals(classNameBeforeRefactoring)) {
                                    replacementsToRemove.add(replacement);
                                }
                            }
                        }
                    }
                }
            }
        }



        replacementsToCheck.removeAll(replacementsToRemove);

    }


    private static void checkForMoveClassRefactoringOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Refactoring refactoring : refactorings) {
            if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_CLASS)) {
                for (Replacement replacement : replacementsToCheck) {
                    if (replacement.getBefore().equals(((MoveClassRefactoring)refactoring).getOriginalClass().getNonQualifiedName()) &&
                            replacement.getAfter().equals(((MoveClassRefactoring)refactoring).getMovedClass().getNonQualifiedName())) {
                        if (!replacement.getBefore().equals(replacement.getAfter())) //For the cases where the method arguments or the class instance creation arguments have been changed, not the name of the method itself. Also, changing the directory can break the previous check.
                            replacementsToRemove.add(replacement);
//                        TODO handle the anonymous class cases.
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForRenameClassRefactoringOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Refactoring refactoring : refactorings) {
            if (refactoring.getRefactoringType().equals(RefactoringType.RENAME_CLASS)) {
                for (Replacement replacement : replacementsToCheck) {
                    if (replacement.getBefore().equals(((RenameClassRefactoring)refactoring).getOriginalClass().getNonQualifiedName()) &&
                            replacement.getAfter().equals(((RenameClassRefactoring)refactoring).getRenamedClass().getNonQualifiedName())) {
                        replacementsToRemove.add(replacement);
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForMoveAndRenameClassRefactoringOnTop(List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Refactoring refactoring : refactorings) {
            if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_RENAME_CLASS)) {
                for (Replacement replacement : replacementsToCheck) {
                    if (replacement.getBefore().equals(((MoveAndRenameClassRefactoring)refactoring).getOriginalClass().getNonQualifiedName()) &&
                            replacement.getAfter().equals(((MoveAndRenameClassRefactoring)refactoring).getRenamedClass().getNonQualifiedName())) {
                        replacementsToRemove.add(replacement);
                        //                        TODO handle the anonymous class cases.

                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static PurityCheckResult detectRenameClassPurity(RenameClassRefactoring refactoring,  List<Refactoring> refactorings, UMLModelDiff modelDiff) {


        for (UMLClassRenameDiff umlClassRenameDiff :modelDiff.getClassRenameDiffList()) {
            int numberOfMappedOperations = umlClassRenameDiff.getOperationBodyMapperList().size();
            int numberOfOperationsOfOriginalClass = umlClassRenameDiff.getOriginalClass().getOperations().size();
            int numberOfOperationsOfRenamedClass = umlClassRenameDiff.getRenamedClass().getOperations().size();

            if (numberOfMappedOperations == numberOfOperationsOfOriginalClass && numberOfOperationsOfOriginalClass == numberOfOperationsOfRenamedClass)
                return new PurityCheckResult(true, "All the operations inside the original and renamed classes are mapped!");

            List<UMLOperation> originalClassOperationsMapper = new ArrayList<>();
            List<UMLOperation> renamedClassOperationsMapper = new ArrayList<>();


            for (UMLOperationBodyMapper umlOperationBodyMapper : umlClassRenameDiff.getOperationBodyMapperList()) {
                originalClassOperationsMapper.add(umlOperationBodyMapper.getOperation1());
                renamedClassOperationsMapper.add(umlOperationBodyMapper.getOperation2());
            }

            List<UMLOperation> removedOperationsList = new ArrayList<>(umlClassRenameDiff.getOriginalClass().getOperations());

            List<UMLOperation> addedOperationsList = new ArrayList<>(umlClassRenameDiff.getRenamedClass().getOperations());

            removedOperationsList.removeAll(originalClassOperationsMapper);
            addedOperationsList.removeAll(renamedClassOperationsMapper);


            for (Refactoring refactoring1: refactorings) {
                if(refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                    addedOperationsList.remove(((ExtractOperationRefactoring) refactoring1).getExtractedOperation());
                }
                if(refactoring1.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
                    removedOperationsList.remove(((InlineOperationRefactoring) refactoring1).getInlinedOperation());
                }
            }

            if (removedOperationsList.isEmpty() && addedOperationsList.isEmpty()) {
                return new PurityCheckResult(true, "Added or removed operations are justified with other refactorings");
            }

//            TODO




        }

        return new PurityCheckResult(false, "This Rename Class refactoring is impure");
    }

    private static PurityCheckResult detectRenameVariablePurity(RenameVariableRefactoring refactoring) {

        return new PurityCheckResult(true, "Rename Variable refactorings are always pure!");
    }

    private static PurityCheckResult detectRenameParameterPurity(RenameVariableRefactoring refactoring) {

        return new PurityCheckResult(true, "Rename Parameter refactorings are always pure!");
    }



    private static PurityCheckResult detectExtractOperationPurity(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings) {

        System.out.println("TEST");


        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty()) {

            int mappingState = 1;
            String purityComment = "";

            if (refactoring.getReplacements().isEmpty()) {
                purityComment = "Identical statements";
                return new PurityCheckResult(true, "There is no replacement! - all mapped", purityComment, mappingState);
            }

            HashSet<Replacement> replacementsToCheck;

//            This method also checks for the exact matches when we have Type Replacement
            if (refactoring.getBodyMapper().allMappingsArePurelyMatched(refactoring.getParameterToArgumentMap())) {
                purityComment = "Changes are within the Extract Method refactoring mechanics";
                return new PurityCheckResult(true, "All the mappings are matched! - all mapped", purityComment, mappingState);
            }

            replacementsToCheck = new HashSet<>(refactoring.getReplacements());
            replacementsToCheck = refactoring.getBodyMapper().omitReplacementsRegardingExactMappings(refactoring.getParameterToArgumentMap(), replacementsToCheck);
            replacementsToCheck = refactoring.getBodyMapper().omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);

            if (replacementsToCheck.isEmpty()) {
                purityComment = "Changes are within the Extract Method refactoring mechanics";
                return new PurityCheckResult(true, "All replacements have been justified - all mapped", purityComment, mappingState);
            }

            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                    if (((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(refactoring.getSourceOperationAfterExtraction().getName()) &&
                            !((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(refactoring.getExtractedOperation().getName())) {

                        adjustTheParameterArgumentField(refactoring, (ExtractOperationRefactoring) refactoring1);
                        replacementsToCheck = refactoring.getBodyMapper().omitReplacementsRegardingExactMappings(refactoring.getParameterToArgumentMap(), replacementsToCheck);
                    }
                }
            }

            replacementsToCheck = refactoring.getBodyMapper().omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);
//            omitReplacementsRegardingInvocationArguments(refactoring, replacementsToCheck);
            checkForParameterArgumentPair(refactoring, replacementsToCheck);

            adjustTheParameterArgumentFieldSourceOperationAfterExtraction(refactoring);

            replacementsToCheck = refactoring.getBodyMapper().omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);


            if (replacementsToCheck.isEmpty()) {
                purityComment = "Changes are within the Extract Method refactoring mechanics";
                return new PurityCheckResult(true, "All replacements have been justified - all mapped", purityComment, mappingState);
            }

            purityComment += "Changes are within the Extract Method refactoring mechanics" + "\n";

            if (replacementsToCheck.size() == 1) {
                for (Replacement replacement: replacementsToCheck) {
                    if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                        return new PurityCheckResult(true, "Argument replaced with return expression - all mapped", purityComment, mappingState);
                    }
                }
            }

            omitThisPatternReplacements(replacementsToCheck);
            omitPrintAndLogMessagesRelatedReplacements(refactoring, replacementsToCheck);
            omitBooleanVariableDeclarationReplacement(refactoring, replacementsToCheck); // For the runTests commit
            omitEqualStringLiteralsReplacement(replacementsToCheck);
            omitPrimitiveTypeReplacements(refactoring.getReplacements(), replacementsToCheck);
            // for https://github.com/infinispan/infinispan/commit/043030723632627b0908dca6b24dae91d3dfd938 commit - performLocalRehashAwareOperation
            refactoring.getBodyMapper().omitReplacementsAccordingSupplierGetPattern(refactoring.getParameterToArgumentMap(), replacementsToCheck);
            omitReturnRelatedReplacements(refactoring, replacementsToCheck);


            omitReplacementRegardingInvertCondition(refactoring, replacementsToCheck);

            omitAnonymousClassDeclarationReplacements(replacementsToCheck);
            omitStringRelatedReplacements(replacementsToCheck);


            if (replacementsToCheck.isEmpty()) {
                purityComment += "Tolerable changes in the body" + "\n";
                return new PurityCheckResult(true, "All replacements have been justified - all mapped", purityComment, mappingState);

            }

            purityComment += "Overlapped refactoring - can be identical by undoing the overlapped refactoring" + "\n";

            checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method Refactoring on the top of the extracted method - all mapped", purityComment, mappingState);
            }


            checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the extract method - all mapped", purityComment, mappingState);
            }

            checkForAddParameterInSubExpressionOnTop(refactoring, refactorings, replacementsToCheck);

            checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the extract method - all mapped", purityComment, mappingState);
            }

            checkForRemoveParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the extracted method - all mapped", purityComment, mappingState);
            }

            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the extract method - all mapped", purityComment, mappingState);
            }

            checkForRemoveVariableOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "One or more variables have been removed from the body of the moved method - all mapped", purityComment, mappingState);
            }

            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the extracted method - all mapped", purityComment, mappingState);
            }

            checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Encapsulate refactoring on top of the extracted method - all mapped", purityComment, mappingState);
            }

            checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Attribute on top of the extracted method - all mapped", purityComment, mappingState);
            }

            checkForMergeVariableOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Merge Variable on top of the extracted method - all mapped", purityComment, mappingState);
            }

            checkForExtractClassOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Class on top of the extracted method - all mapped", purityComment, mappingState);
            }

            checkForExtractMethodOnTop(refactorings, replacementsToCheck, refactoring);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Method on top of the extracted method - all mapped", purityComment, mappingState);
            }

            checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the extract method - all mapped", purityComment, mappingState);
            }

            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename class on the top of the extract method - all mapped", purityComment, mappingState);
            }

            checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move and rename class on the top of the extract method - all mapped", purityComment, mappingState);
            }

            checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move method on the top of the extract method - all mapped", purityComment, mappingState);
            }

            checkForThisPatternReplacement(replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Contains this pattern - all mapped", purityComment, mappingState);
            }

            checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract variable on the top of the extract method - all mapped", purityComment, mappingState);
            }

            checkForMergeConditionalOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Merge Conditional on the top of the extract method - all mapped", purityComment, mappingState);
            }

            checkTheReplacementsAlreadyHandled(refactoring, replacementsToCheck);
            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "One of the overlapping cases - all mapped", purityComment, mappingState);
            }

            int size1 = replacementsToCheck.size();
            int numberOfArgumentReplacedWithReturnReplacements = 0;

            for (Replacement replacement : replacementsToCheck) {
                if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                    numberOfArgumentReplacedWithReturnReplacements++;
                }
            }

            if (numberOfArgumentReplacedWithReturnReplacements == size1) {
                return new PurityCheckResult(true, "Argument replaced with return expression - all mapped", purityComment, mappingState);
            }

//            if (replacementsToCheck.size() == 1) {
//                for (Replacement replacement: replacementsToCheck) {
//                    if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
//
//                    }
//                }
//            }


            purityComment = "Severe changes";

            return new PurityCheckResult(false, "Replacements cannot be justified", purityComment, mappingState);

//        Check non-mapped leaves
        } else if (refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty()){

            int mappingState = 2;
            String purityComment = "";

            List<AbstractCodeFragment> nonMappedLeavesT2 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT2());
            List<AbstractCodeFragment> nonMappedLeavesT1 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT1());
            int size = nonMappedLeavesT2.size();
            checkForInevitableVariableDeclaration(refactoring, nonMappedLeavesT2, refactorings); // This method can also change the state of the refactoring's replacements
            int size2 = nonMappedLeavesT2.size();

            if (size != size2) {
                purityComment += "Severe changes + \n";
            }

            if (!checkReplacements(refactoring, refactorings)) {
                purityComment = "Severe changes";
                return new PurityCheckResult(false, "replacements are not justified - non-mapped leaves", purityComment, mappingState);
            }

            int size4 = nonMappedLeavesT2.size();
            int returnStatementCounter0 = 0;

            if (size4 == returnStatementCounter0) {
                return new PurityCheckResult(true, "Return expression has been added within the Extract Method mechanics - with non-mapped leaves", purityComment, mappingState);
            }


//            if (nonMappedLeavesT2.size() == 1) {
//                AbstractCodeFragment nonMappedLeave = nonMappedLeavesT2.get(0);
//                if (nonMappedLeave.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
//                    if (nonMappedLeave.getTernaryOperatorExpressions().isEmpty()) {
//                        purityComment += "Changes are within the Extract Method refactoring mechanics";
//                        return new PurityCheckResult(true, "Return expression has been added within the Extract Method mechanics - with non-mapped leaves", purityComment, mappingState);
//                    }
//                }
//            }

            purityComment = "Overlapped refactoring - can be identical by undoing the overlapped refactoring";

            checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "Rename Refactoring on the top of the extracted method - with non-mapped leaves", purityComment, mappingState);
            }

            checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "Extract Variable on the top of the extracted method - with non-mapped leaves", purityComment, mappingState);
            }

//            TODO - MoveAndRenameMethod refactoring on top of the extract method can cause a non-mapped leaf.
            purityComment = "Severe changes";

            checkForPrint_NonMapped(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "Extra print lines - with non-mapped leaves", purityComment, mappingState);
            }

            checkForStatementsBeingMappedInOtherRefactorings(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "Mapped statements in other refactorings - with non-mapped leaves", purityComment, mappingState);
            }

            checkVariableDeclarationUsage(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "The new variable declared has not been used within the program logic - with non-mapped leaves", purityComment, mappingState);
            }

            int size3 = nonMappedLeavesT2.size();
            int returnStatementCounter = 0;

            for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    returnStatementCounter++;
                }
            }

            if (size3 == returnStatementCounter) {
                return new PurityCheckResult(true, "Return expression has been added within the Extract Method mechanics - with non-mapped leaves", purityComment, mappingState);
            }


//            if (nonMappedLeavesT2.size() == 1) {
//                AbstractCodeFragment nonMappedLeave = nonMappedLeavesT2.get(0);
//                if (nonMappedLeave.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
//                    if (nonMappedLeave.getTernaryOperatorExpressions().isEmpty())
//                        return new PurityCheckResult(true, "Return expression has been added within the Extract Method mechanics - with non-mapped leaves", purityComment, mappingState);
//                }
//            }

            return new PurityCheckResult(false, "Violating extract method refactoring mechanics - with non-mapped leaves", purityComment, mappingState);
        } else {

            int mappingState = 3;
            String purityComment = "";

            List<AbstractCodeFragment> nonMappedLeavesT2List = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT2());
            List<AbstractCodeFragment> nonMappedLeavesT1List = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT1());



            if (!checkNonMappedLeaves(refactoring, refactorings, nonMappedLeavesT2List, nonMappedLeavesT1List)) {
                purityComment = "Severe changes";
                return new PurityCheckResult(false, "Non-mapped leaves are not justified - non-mapped inner nodes", purityComment, mappingState);
            }

            if (!checkReplacements(refactoring, refactorings)) {
                purityComment = "Severe changes";
                return new PurityCheckResult(false, "Replacements are not justified - non-mapped inner nodes", purityComment, mappingState);
            }



            List<AbstractCodeFragment> nonMappedInnerNodesT2 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedInnerNodesT2());

            int numberOfWrongNonMappedBlocks = 0;
            for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                    numberOfWrongNonMappedBlocks++;
                }else {
                    break;
                }
            }

            if (numberOfWrongNonMappedBlocks == nonMappedInnerNodesT2.size()) {
                purityComment = "Identical statements";
                return new PurityCheckResult(true, "Just an empty block - with non-mapped leaves", purityComment, mappingState);
            }

//            if (nonMappedInnerNodesT2.size() == 1) {
//                AbstractCodeFragment nonMappedLeave = nonMappedInnerNodesT2.get(0);
//                if (nonMappedLeave.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
//                    purityComment = "Identical statements";
//                    return new PurityCheckResult(true, "Just an empty block - with non-mapped leaves", purityComment, mappingState);
//                }
//            }

            purityComment = "Severe changes";

            checkForNodesBeingMappedInOtherRefactorings(refactoring, refactorings, nonMappedInnerNodesT2);

            if (nonMappedInnerNodesT2.isEmpty()) {
                return new PurityCheckResult(true, "Nodes being mapped with other nodes in other refactorings - with non-mapped leaves", purityComment, mappingState);
            }

            checkForIfTrueCondition(refactoring, nonMappedInnerNodesT2);

            if (nonMappedInnerNodesT2.isEmpty()) {
                return new PurityCheckResult(true, "Non-changing if statement has been added - with non-mapped leaves", purityComment, mappingState);
            }

//            checkForIfCondition(refactoring, nonMappedInnerNodesT2); //For the big commit - https://github.com/robovm/robovm/commit/bf5ee44b3b576e01ab09cae9f50300417b01dc07 - and the cryptoOperation extract method

            int size = nonMappedInnerNodesT2.size();
            int blockStatementCounter = 0;

            for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
                if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                    blockStatementCounter++;
                }
            }

            if (size == blockStatementCounter) {
                return new PurityCheckResult(true, "Just an empty block - with non-mapped leaves", purityComment, mappingState);
            }

//            if (nonMappedInnerNodesT2.size() == 1) {
//                AbstractCodeFragment nonMappedLeave = nonMappedInnerNodesT2.get(0);
//                if (nonMappedLeave.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
//
//                }
//            }

            purityComment = "Severe changes";
            return new PurityCheckResult(false, "Contains non-mapped inner nodes", purityComment, mappingState);
        }
    }

    private static void omitAnonymousClassDeclarationReplacements(HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.ANONYMOUS_CLASS_DECLARATION)) {
                replacementsToRemove.add(replacement);
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void omitReturnRelatedReplacements(Refactoring refactoring, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            AbstractCodeMapping mapping = findTheMapping(replacement, refactoring);
            if (mapping.getFragment1().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT) &&
                    mapping.getFragment2().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) { //Relax check
                if (mapping.getFragment1().getMethodInvocations().size() == mapping.getFragment2().getMethodInvocations().size()
                        && mapping.getFragment1().getCreations().size() == mapping.getFragment2().getCreations().size()) { //Strict check
                    if (mapping.getFragment1().getVariables().size() == mapping.getFragment2().getVariables().size()) { //More strict check
                        replacementsToRemove.add(replacement);
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForMergeVariableOnTop(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.MERGE_VARIABLES)) {
                for (AbstractCodeMapping mapping : refactoring.getBodyMapper().getMappings()) {
                    for (Replacement mappingReplacement : mapping.getReplacements()) {
                        if (mappingReplacement.equals(replacement)) {
                            if (!mapping.getFragment2().getTypes().isEmpty()) {
                                for (Refactoring refactoring1 : refactorings) {
                                    if (refactoring1.getRefactoringType().equals(RefactoringType.MERGE_PARAMETER)) {
                                        if (mapping.getFragment2().getTypes().contains(((MergeVariableRefactoring) (refactoring1)).getOperationAfter().getName()) ||
                                                mapping.getFragment2().getTypes().contains(((MergeVariableRefactoring) (refactoring1)).getOperationBefore().getName())) {
                                            replacementsToRemove.add(replacement);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);

    }

    private static void adjustTheParameterArgumentFieldSourceOperationAfterExtraction(ExtractOperationRefactoring refactoring) {

        Map<String, String> mp = Map.copyOf(refactoring.getParameterToArgumentMap());

        for (Map.Entry<String, String> entry :mp.entrySet()) {

            if (entry.getValue().equals(entry.getKey())) {

                if (refactoring.getSourceOperationAfterExtraction().getVariableDeclaration(entry.getKey()) != null && refactoring.getSourceOperationAfterExtraction().getVariableDeclaration(entry.getKey()).getInitializer() != null) {
                    refactoring.getParameterToArgumentMap().put(entry.getKey(), refactoring.getSourceOperationAfterExtraction().getVariableDeclaration(entry.getKey()).getInitializer().getExpression());
                }
            }
        }
    }

    private static void checkForAddParameterInSubExpressionOnTop(Refactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToAdd = new HashSet<>();
        Set<Replacement> replacementsToRemove = new HashSet<>();


        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                String subExpressionMethodInvocation = candidateForAddParameter(replacement);
                if (subExpressionMethodInvocation != null) {
                    AbstractCodeMapping mapping = findTheMapping(replacement, refactoring);

                    String before = null;
                    String after = null;
                    AbstractCall invokedOperationBefore = null;
                    AbstractCall invokedOperationAfter = null;


                    for (AbstractCall entry: mapping.getFragment1().getMethodInvocations()) {
                        if (entry.getName().equals(subExpressionMethodInvocation)) {
                            before = entry.actualString();
                            invokedOperationBefore = entry;
                        }
                    }

                    for (AbstractCall entry: mapping.getFragment2().getMethodInvocations()) {
                        if (entry.getName().equals(subExpressionMethodInvocation)) {
                            after = entry.actualString();
                            invokedOperationAfter = entry;
                        }
                    }

                    if (before == null || after == null || invokedOperationBefore == null || invokedOperationAfter == null) {

                        for (AbstractCall entry: mapping.getFragment1().getCreations()) {
                            if (entry.getName().equals(subExpressionMethodInvocation)) {
                                before = entry.actualString();
                                invokedOperationBefore = entry;
                            }
                        }

                        for (AbstractCall entry: mapping.getFragment2().getCreations()) {
                            if (entry.getName().equals(subExpressionMethodInvocation)) {
                                after = entry.actualString();
                                invokedOperationAfter = entry;
                            }
                        }
                    }

                    if (before == null || after == null || invokedOperationBefore == null || invokedOperationAfter == null) {
                        break;
                    }

                    replacementsToRemove.add(replacement);
                    replacementsToAdd.add(new MethodInvocationReplacement(before, after, invokedOperationBefore, invokedOperationAfter, Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT));
                }
            }
        }

        replacementsToCheck.addAll(replacementsToAdd);
        replacementsToCheck.removeAll(replacementsToRemove);

    }

    private static AbstractCodeMapping findTheMapping(Replacement replacement, Refactoring refactoring) {

        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        } else {
            return null;
        }

        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            if (mapping.getReplacements().contains(replacement)) {
                return mapping;
            }
        }
        return null;
    }

    private static String candidateForAddParameter(Replacement replacement) {

        if (!((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().getName().equals(((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().getName())) {
            return null;
        }

        if (((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().size() != ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().arguments().size()) {
            return null;
        }

        for (int i = 0; i < ((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().size(); i++) {
            if (!((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().get(i).equals(((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().arguments().get(i))) {
                if (isMethodInvocation(((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().get(i), ((MethodInvocationReplacement) (replacement)).getInvokedOperationAfter().arguments().get(i))) {

                    int parIndex = ((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().get(i).indexOf("(");
                    String subExpressionMethodInvocation = ((MethodInvocationReplacement) (replacement)).getInvokedOperationBefore().arguments().get(i).substring(0, parIndex);
                    if (subExpressionMethodInvocation.split(" ").length == 2) {
                        String[] nameSplitted = subExpressionMethodInvocation.split(" ");
                        if (nameSplitted[0].toLowerCase().equals("new")) {
                            return nameSplitted[1];
                        }
                    }else {
                        return subExpressionMethodInvocation;
                    }

                }
            }
        }
        return null;
    }

    private static boolean isMethodInvocation(String s, String s1) {
        return s1.contains("(") && s.contains("(");
    }


    private static void omitReplacementRegardingInvertCondition(Refactoring refactoring, HashSet<Replacement> replacementsToCheck) {

        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        } else {
            return;
        }

        Set<Replacement> replacementsToRemove = new HashSet<>();
        Map<String, String> patterns = new HashMap<>();

        patterns.put(">", "<=");
        patterns.put("<", ">=");
        patterns.put(">=", "<");
        patterns.put("<=", ">");
        patterns.put("=", "!=");
        patterns.put("!=", "=");



        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.INFIX_OPERATOR)) {
                if (invertingCheck(replacement, patterns)) {
                    AbstractCodeMapping mapping = findCorrespondingMappingForAReplacement(bodyMapper, replacement);
//                    I don't check for the non-emptiness of the non-mapped leaves because there would be some cases that the return statements mapped with other unrelated return statements.
                    if (((CompositeStatementObject) (mapping.getFragment2())).getAllStatements().size() == 2) {
                        for (AbstractStatement allStatement : ((CompositeStatementObject) (mapping.getFragment2())).getAllStatements()) {
                            if (allStatement.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                                replacementsToRemove.add(replacement);
                                break;
                            }
                        }
                    }

                }
            }
        }
    replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static AbstractCodeMapping findCorrespondingMappingForAReplacement(UMLOperationBodyMapper bodyMapper, Replacement replacement) {

        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            for (Replacement mappingReplacement : mapping.getReplacements()) {
                if (replacement.equals(mappingReplacement)) {
                    return mapping;
                }
            }
        }
        return null;
    }


    private static boolean invertingCheck(Replacement replacement, Map<String, String> patterns) {

        for (Map.Entry<String, String> entry: patterns.entrySet()) {
            if (replacement.getBefore().equals(entry.getKey()) && replacement.getAfter().equals(entry.getValue())) {
                return true;
            }
        }
        return false;
    }

    private static void omitThisPatternReplacements(HashSet<Replacement> replacementsToCheck) throws StringIndexOutOfBoundsException {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
                if (replacement.getBefore().contains("this") || replacement.getAfter().contains("this")) {
                    int findSimilar1 = replacement.getAfter().indexOf(replacement.getBefore());
                    int findSimilar2 = replacement.getBefore().indexOf(replacement.getAfter());
                    if (findSimilar1 != -1) {
                        try {
                        if (replacement.getAfter().substring(0, findSimilar1 - 1).equals("this")) {
                            replacementsToRemove.add(replacement);
                        }
                        }catch (StringIndexOutOfBoundsException ignored) {
                            System.out.println("ignored");
                        }
                    } else if (findSimilar2 != -1) {
                        try {
                            String temp = replacement.getBefore().substring(0, findSimilar2 - 1);
                            if (temp.equals("this")) {
                                replacementsToRemove.add(replacement);
                            }
                        } catch (StringIndexOutOfBoundsException ignored) {
                            System.out.println("ignored");
                        }
                    }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void adjustTheParameterArgumentField(ExtractOperationRefactoring refactoring, ExtractOperationRefactoring refactoring1) {

        for (Map.Entry<String, String> stringStringEntry : refactoring.getParameterToArgumentMap().entrySet()) {
            for (Map.Entry<String, String> stringEntry : refactoring1.getParameterToArgumentMap().entrySet()) {
                if (stringStringEntry.getValue().equals(stringEntry.getKey())) {
                    refactoring.getParameterToArgumentMap().put(stringStringEntry.getKey(), stringEntry.getValue());
                    break;
                }
            }
        }
    }

    private static void checkForInevitableVariableDeclaration(ExtractOperationRefactoring refactoring, List<AbstractCodeFragment> nonMappedLeavesT2, List<Refactoring> refactorings) {

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();


        for (AbstractCodeFragment nonMappedLeaf : nonMappedLeavesT2) {
            if (!nonMappedLeaf.getVariableDeclarations().isEmpty()) {
                List<VariableDeclaration> variableDeclarations = nonMappedLeaf.getVariableDeclarations();
                for (VariableDeclaration variableDeclaration : variableDeclarations) {
                    if (checkUsageOfTheNewVariableDeclaration(variableDeclaration, refactoring, refactorings)) {
                        nonMappedLeavesT2ToRemove.add(nonMappedLeaf);
                        for (Replacement replacement : refactoring.getBodyMapper().getReplacements()) {
                            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME) &&
                            replacement.getAfter().equals(variableDeclaration.getVariableName())) {
                                refactoring.getReplacements().remove(replacement);
                                break;
                            }
                        }
                    }
                }
            }
        }


        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
    }

    private static void checkForInevitableVariableDeclarationInline(InlineOperationRefactoring refactoring, List<AbstractCodeFragment> nonMappedLeavesT1, List<Refactoring> refactorings) {

        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();


        for (AbstractCodeFragment nonMappedLeaf : nonMappedLeavesT1) {
            if (!nonMappedLeaf.getVariableDeclarations().isEmpty()) {
                List<VariableDeclaration> variableDeclarations = nonMappedLeaf.getVariableDeclarations();
                for (VariableDeclaration variableDeclaration : variableDeclarations) {
                    if (checkUsageOfTheNewVariableDeclaration(variableDeclaration, refactoring, refactorings)) {
                        nonMappedLeavesT1ToRemove.add(nonMappedLeaf);
                        for (Replacement replacement : refactoring.getBodyMapper().getReplacements()) {
                            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME) &&
                                    replacement.getAfter().equals(variableDeclaration.getVariableName())) {
                                refactoring.getReplacements().remove(replacement);
                                break;
                            }
                        }
                    }
                }
            }
        }


        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);
    }


    private static boolean checkUsageOfTheNewVariableDeclaration(VariableDeclaration variableDeclaration, Refactoring refactoring, List<Refactoring> refactorings) {

        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();;
        }else {
            return false;
        }

        boolean existFlag = false;
        boolean checkFlag = false;

        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            if (mapping.getFragment2().getString().contains(variableDeclaration.getVariableName())) {
                existFlag = true;
                if (variableDeclaration.getInitializer() == null || variableDeclaration.getInitializer().getExpression().equals("null")
                || variableDeclaration.getInitializer().getExpression().equals("0"))
                    checkFlag = checkMappingReplacements(mapping, variableDeclaration, refactorings);
                if (!checkFlag) {
                    break;
                }
            }
        }
        if (!existFlag) {
            return true;
        }

        return checkFlag;
    }

    private static boolean checkMappingReplacements(AbstractCodeMapping mapping, VariableDeclaration variableDeclaration, List<Refactoring> refactorings) {

        if (mapping.getReplacements().isEmpty()) {
            return true;
        }

        for (Replacement replacement : mapping.getReplacements()) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                if (replacement.getAfter().equals(variableDeclaration.getVariableName())) {
                    return true;
                }

                Set<Replacement> tempReplacement = new HashSet<>();
                tempReplacement.add(replacement);

                checkForRenameVariableOnTop(refactorings, tempReplacement);
                if (tempReplacement.isEmpty()) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void checkForExtractVariableOnTop_NonMapped(List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeaves) {

        List<AbstractCodeFragment> nonMappedLeavesToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeaves) {
            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_VARIABLE)) {
                    if (abstractCodeFragment.getVariableDeclarations().size() == 1) {
                        if (abstractCodeFragment.getVariableDeclarations().get(0).equals(((ExtractVariableRefactoring) (refactoring1)).getVariableDeclaration())) {
                            nonMappedLeavesToRemove.add(abstractCodeFragment);
                        }
                    }
                }
            }
        }

        nonMappedLeaves.removeAll(nonMappedLeavesToRemove);
    }

    private static void checkForExtractVariableOnTop(Refactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Map<String, String> parameterToArgumentMap = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            parameterToArgumentMap = ((ExtractOperationRefactoring) (refactoring)).getParameterToArgumentMap();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            parameterToArgumentMap = ((InlineOperationRefactoring) (refactoring)).getParameterToArgumentMap();;
        }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION) ||
                refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {

            Set<Replacement> replacementsToRemove = new HashSet<>();

            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_VARIABLE)) {
                    if (((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer() != null) {
                        for (Replacement replacement : replacementsToCheck) {
                            if (replacement.getBefore().equals((((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer().getExpression())) &&
                            replacement.getAfter().equals(((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getVariableName())) {
                                replacementsToRemove.add(replacement);
                            }
                        }
                    }
                }
            }

            replacementsToCheck.removeAll(replacementsToRemove);
            return;
        }else {
            return;
        }

        Set<Replacement> replacementsToRemove = new HashSet<>();
        Map<String, String> patterns = new HashMap<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_VARIABLE)) {
                if (((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer() != null) {
                    ((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer().replaceParametersWithArguments(parameterToArgumentMap);
                    patterns.put(((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer().getArgumentizedString(), ((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getVariableName());
                    patterns.put(((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getInitializer().getString(), ((ExtractVariableRefactoring) refactoring1).getVariableDeclaration().getVariableName());
                }
            }
        }

        for (Replacement replacement : replacementsToCheck) {
            for (Map.Entry<String, String> entry: patterns.entrySet()) {
                if (replacement.getBefore().equals(entry.getKey()) && replacement.getAfter().equals(entry.getValue())) {
                    replacementsToRemove.add(replacement);
                    break;
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkForIfTrueCondition(ExtractOperationRefactoring refactoring, List<AbstractCodeFragment> nonMappedInnerNodesT2) {

        List<AbstractCodeFragment> nonMappedNodesT2ToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
            abstractCodeFragment.argumentizationAfterRefactorings(refactoring.getParameterToArgumentMap());
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
                if (abstractCodeFragment.getArgumentizedAfterRefactorings().equals("if(true)")) {
                    nonMappedNodesT2ToRemove.add(abstractCodeFragment);
                }
            }
        }
        nonMappedInnerNodesT2.removeAll(nonMappedNodesT2ToRemove);

    }

    private static void checkForNodesBeingMappedInOtherRefactorings(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedInnerNodesT2) {

        List<AbstractCodeFragment> nonMappedNodesT2ToRemove = new ArrayList<>();

        String sourceOperation = refactoring.getSourceOperationBeforeExtraction().getName();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION) && !refactoring1.equals(refactoring)) {
                    for (AbstractCodeMapping mapping : ((ExtractOperationRefactoring) (refactoring1)).getBodyMapper().getMappings()) {
                        if (mapping.getFragment2().equals(abstractCodeFragment)) {
                            if (mapping.getOperation1().getName().equals(sourceOperation) && mapping.getReplacements().isEmpty()) {
                                nonMappedNodesT2ToRemove.add(mapping.getFragment2());
                                break;
                            }
                        }
                    }
                }
            }
        }

        nonMappedInnerNodesT2.removeAll(nonMappedNodesT2ToRemove);

    }

    private static void omitEqualStringLiteralsReplacement(HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.STRING_LITERAL)) {
                if (replacement.getBefore().replaceAll("\"", "").trim().equals(replacement.getAfter().replaceAll("\"", "").trim())) {
                    replacementsToRemove.add(replacement);
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void checkVariableDeclarationUsage(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2) {

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            for (VariableDeclaration variableDeclaration : abstractCodeFragment.getVariableDeclarations()) {
                String variableName = variableDeclaration.getVariableName();
                if (checkUsageWithinTheRefactoring(variableName, refactoring, abstractCodeFragment)) {
                    nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
                }
            }
        }

        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
    }

    private static boolean checkUsageWithinTheRefactoring(String variableName, ExtractOperationRefactoring refactoring, AbstractCodeFragment abstractCodeFragment) {

        List<AbstractStatement> statementList = refactoring.getExtractedOperation().getBody().getCompositeStatement().getAllStatements();

        for (AbstractStatement abstractStatement : statementList) {
            if (abstractStatement.getString().contains(variableName) && !abstractStatement.equalFragment(abstractCodeFragment)) {
                if (!isPrintOrLog(abstractStatement.getString())) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void checkForStatementsBeingMappedInOtherRefactorings(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2) {

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();

        String sourceOperation = refactoring.getSourceOperationBeforeExtraction().getName();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION) && !refactoring1.equals(refactoring)) {
                    for (AbstractCodeMapping mapping : ((ExtractOperationRefactoring) (refactoring1)).getBodyMapper().getMappings()) {
                        if (mapping.getFragment2().getString().equals(abstractCodeFragment.getString())) {
                            if (mapping.getOperation1().getName().equals(sourceOperation)) {
                                nonMappedLeavesT2ToRemove.add(mapping.getFragment2());
                                break;
                            }
                        }
                    }
                }
            }
        }

        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
    }

    private static void checkForPrint_NonMapped(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2) {

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            if (isPrintOrLog(abstractCodeFragment.getString())) {
                nonMappedLeavesT2ToRemove.add(abstractCodeFragment);
            }
        }

        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
    }

    private static void omitPrintAndLogMessagesRelatedReplacements(Refactoring refactoring, HashSet<Replacement> replacementsToCheck) {
        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        }else {
            return;
        }

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            if (isPrintOrLog(mapping.getFragment1().getString()) && isPrintOrLog(mapping.getFragment2().getString())) {
                replacementsToRemove.addAll(mapping.getReplacements());
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static boolean isPrintOrLog(String fragmentString) {
        return fragmentString.contains("System.out.println") || fragmentString.contains("System.err.println") ||
                fragmentString.contains("System.out.print") || fragmentString.contains("System.err.print") ||
                fragmentString.contains("System.out.printf") || fragmentString.contains("System.err.printf") ||
                fragmentString.contains("log.trace") || fragmentString.contains("log.tracef") ||
                fragmentString.contains("log.info");
    }

    private static void removeNonMappedLeavesRegardingNullCheck(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2List) {

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();

        for (AbstractCodeFragment nonMappedLeavesT2 : nonMappedLeavesT2List) {
            if (nonMappedLeavesT2.getParent().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                if (nonMappedLeavesT2.getParent().getParent() != null) {
                if (nonMappedLeavesT2.getParent().getParent().getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
                    AbstractExpression ifStatementConditionExpression = nonMappedLeavesT2.getParent().getParent().getExpressions().get(0);
                    if (nullCheckIf(ifStatementConditionExpression) && variableCheck(refactoring, ifStatementConditionExpression)) {
                        nonMappedLeavesT2ToRemove.add(nonMappedLeavesT2);
                    }
                }
            }
        }
        }
    nonMappedLeavesT2List.removeAll(nonMappedLeavesT2ToRemove);
    }

    private static boolean variableCheck(ExtractOperationRefactoring refactoring, AbstractExpression ifStatementConditionExpression) {

        return refactoring.getExtractedOperation().getParameterNameList().containsAll(ifStatementConditionExpression.getVariables());


    }

    private static boolean nullCheckIf(AbstractExpression ifStatementConditionExpression) {

        return ifStatementConditionExpression.getVariables().size() == ifStatementConditionExpression.getNullLiterals().size();

    }

    private static void checkForExtractMethodOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck, Refactoring refactoring) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME)) {
                String invokedOperationAfterName = ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName();
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                        if (((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(invokedOperationAfterName)) {
                            if (((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getParameterNameList().size() == ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().arguments().size()) {
                                replacementsToRemove.add(replacement);
                                break;
                            }
                        }
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

//    private static void checkForIfCondition(ExtractOperationRefactoring refactoring, List<AbstractCodeFragment> nonMappedInnerNodesT2) {
//
//        List<AbstractCodeFragment> nonMappedInnerNodesToRemove = new ArrayList<>();
//        List<String> conditionVariables = new ArrayList<>();
//
//        for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
//            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
//                for (AbstractExpression expression : ((CompositeStatementObject) abstractCodeFragment).getExpressions()) {
//                    conditionVariables.addAll(expression.getVariables());
//                }
//                if (refactoring.getExtractedOperation().getParameterNameList().containsAll(conditionVariables)) {
//                    nonMappedInnerNodesToRemove.add(abstractCodeFragment);
//                }
//            }
//        }
//
//        nonMappedInnerNodesT2.removeAll(nonMappedInnerNodesToRemove);
//
//    }

    private static void checkForRemoveParameterOnTopInline(Refactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();;
        }else {
            return;
        }


        Set<Replacement> replacementsToRemove = new HashSet<>();


        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT)) {
                checkForRemoveParameterOnTopConstructorVersion(bodyMapper, replacement, refactorings, replacementsToCheck);
            }
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName()))) {

                ArrayList<String> temp1 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments());
                ArrayList<String> temp2 = new ArrayList<>(temp1);
//                temp1.removeAll(((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments());

                for (String argument : ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments()) {
                    for (String s : temp1) {
                        if (argument.equals(s)) {
                            temp1.remove(s);
                            break;
                        }
                    }
                }
                ArrayList<Integer> removedArgumentsLocationInReplacement = new ArrayList<>();

                for (int i = 0; i < temp1.size(); i++) {
                    for (int j = 0; j < temp2.size(); j++) {
                        if (temp1.get(i).equals(temp2.get(j)))
                            removedArgumentsLocationInReplacement.add(j);
                    }
                }

                String methodName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();
                List<Refactoring> removeParameterRefactoringList = new ArrayList<>();

                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                        removeParameterRefactoringList.add(refactoring1);
                    }
                }

                ArrayList<Integer> removedArgumentLocationInRefactoring = new ArrayList<>();

                for (Refactoring ref : removeParameterRefactoringList) {
                    if (ref.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                        if (((RemoveParameterRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                            int ind = ((RemoveParameterRefactoring) ref).getOperationBefore().getParameterNameList().indexOf(((RemoveParameterRefactoring) ref).getParameter().getName());
                            removedArgumentLocationInRefactoring.add(ind);
                        }
                    }
                }
                Collections.sort(removedArgumentsLocationInReplacement);
                Collections.sort(removedArgumentLocationInRefactoring);
                if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
                    replacementsToRemove.add(replacement);
                }

            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);

    }

    private static void checkForRemoveParameterOnTop(Refactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        }else {
            return;
        }


        Set<Replacement> replacementsToRemove = new HashSet<>();


        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT)) {
                checkForRemoveParameterOnTopConstructorVersion(bodyMapper, replacement, refactorings, replacementsToCheck);
            }
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName())) ||
                    (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName()))) {

                    ArrayList<String> temp1 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments());
                    ArrayList<String> temp2 = new ArrayList<>(temp1);
                    temp1.removeAll(((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments());
                    ArrayList<Integer> removedArgumentsLocationInReplacement = new ArrayList<>();

                    for (int i = 0; i < temp1.size(); i++) {
                        for (int j = 0; j < temp2.size(); j++) {
                            if (temp1.get(i).equals(temp2.get(j)))
                                removedArgumentsLocationInReplacement.add(j);
                        }
                    }

                    String methodName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();
                    List<Refactoring> removeParameterRefactoringList = new ArrayList<>();

                    for (Refactoring refactoring1 : refactorings) {
                        if (refactoring1.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                            removeParameterRefactoringList.add(refactoring1);
                        }
                    }

                    ArrayList<Integer> removedArgumentLocationInRefactoring = new ArrayList<>();

                    for (Refactoring ref : removeParameterRefactoringList) {
                        if (ref.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                            if (((RemoveParameterRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                                int ind = ((RemoveParameterRefactoring) ref).getOperationBefore().getParameterNameList().indexOf(((RemoveParameterRefactoring) ref).getParameter().getName());
                                removedArgumentLocationInRefactoring.add(ind);
                            }
                        }
                    }
                    Collections.sort(removedArgumentsLocationInReplacement);
                    Collections.sort(removedArgumentLocationInRefactoring);
                    if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
                        replacementsToRemove.add(replacement);
                    }

            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);

    }

    private static void checkForRemoveParameterOnTopConstructorVersion(UMLOperationBodyMapper bodyMapper, Replacement replacement, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            for (Replacement mappingReplacement : mapping.getReplacements()) {
                if (mappingReplacement.equals(replacement)) {
                    Optional<AbstractCall> actualValue1 = mapping.getFragment1().getCreations()
                            .stream()
                            .findFirst();

                    if (actualValue1.isPresent()) {
                        ArrayList<String> temp1 = new ArrayList<>(actualValue1.get().arguments());
                        ArrayList<String> temp2 = new ArrayList<>(temp1);
                        Optional<AbstractCall> actualValue2 = mapping.getFragment2().getCreations()
                                .stream()
                                .findFirst();
                        actualValue2.ifPresent(stringListEntry -> temp1.removeAll(stringListEntry.arguments()));

                        ArrayList<Integer> removedArgumentsLocationInReplacement = new ArrayList<>();

                        for (int i = 0; i < temp1.size(); i++) {
                            for (int j = 0; j < temp2.size(); j++) {
                                if (temp1.get(i).equals(temp2.get(j)))
                                    removedArgumentsLocationInReplacement.add(j);
                            }
                        }

                        String methodName = actualValue1.get().getName();
                        List<Refactoring> removeParameterRefactoringList = new ArrayList<>();

                        for (Refactoring refactoring1 : refactorings) {
                            if (refactoring1.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                                removeParameterRefactoringList.add(refactoring1);
                            }
                        }

                        ArrayList <Integer> removedArgumentLocationInRefactoring = new ArrayList<>();

                        for (Refactoring ref : removeParameterRefactoringList) {
                            if (ref.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                                if (((RemoveParameterRefactoring)ref).getOperationBefore().getName().equals(methodName)) {
                                    int ind = ((RemoveParameterRefactoring)ref).getOperationBefore().getParameterNameList().indexOf(((RemoveParameterRefactoring)ref).getParameter().getName());
                                    removedArgumentLocationInRefactoring.add(ind);
                                }
                            }
                        }


//                    In case of presence of constructors, it might there are multiple constructors. Like calling overloaded methods. So, in this case the location of
//                    the removed parameter may not be deterministic. So, in this case, we just check the size of the two lists.

//                    Collections.sort(removedArgumentsLocationInReplacement);
//                    Collections.sort(removedArgumentLocationInRefactoring);
//                    if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
//                        replacementsToCheck.remove(replacement);
//                    }

                        if ((removedArgumentsLocationInReplacement.size() == removedArgumentLocationInRefactoring.size()) && !removedArgumentsLocationInReplacement.isEmpty()) {
                            replacementsToCheck.remove(replacement);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static void checkForParameterArgumentPair(ExtractOperationRefactoring refactoring, Set<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT)) {
                List<String> invokedAfterArguments = ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments();
                List<String> extractedOperationArguments = refactoring.getExtractedOperation().getParameterNameList();

                if (invokedAfterArguments.containsAll(extractedOperationArguments) && extractedOperationArguments.containsAll(invokedAfterArguments) &&
                ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments().size() == ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments().size()) {
                    replacementsToRemove.add(replacement);
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void omitReplacementsRegardingInvocationArguments(ExtractOperationRefactoring refactoring, Set<Replacement> replacementsToCheck) {


        Set<Replacement> replacementsToRemove = new HashSet<>();

        List<String> extractedOperationInvocationArguments = new ArrayList<>(refactoring.getParameterToArgumentMap().values());

        for (Replacement replacement : replacementsToCheck) {
            if (extractedOperationInvocationArguments.contains(replacement.getAfter()) && (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) ||
                    (extractedOperationInvocationArguments.contains(replacement.getAfter()) && replacement.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION))) {
                replacementsToRemove.add(replacement);
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);

    }

    private static boolean checkNonMappedLeaves(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2, List<AbstractCodeFragment> nonMappedLeavesT1) {

        if (nonMappedLeavesT2.isEmpty()) {
            return true;
        }

        checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2, nonMappedLeavesT1);
        checkForExtractVariableOnTop_NonMapped(refactorings, nonMappedLeavesT2);

        checkForInevitableVariableDeclaration(refactoring, nonMappedLeavesT2, refactorings); // This method can also change the state of the refactoring's replacements



        if (nonMappedLeavesT2.isEmpty()) {
            return true;
        }

        checkForPrint_NonMapped(refactoring, refactorings, nonMappedLeavesT2);
        checkForStatementsBeingMappedInOtherRefactorings(refactoring, refactorings, nonMappedLeavesT2);

        if (nonMappedLeavesT2.isEmpty()) {
            return true;
        }

        int size = nonMappedLeavesT2.size();
        int returnStatementCounter = 0;

        for (AbstractCodeFragment abstractCodeFragment : nonMappedLeavesT2) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                returnStatementCounter++;
            }
        }

        if (size == returnStatementCounter) {
            return true;
        }

//        if (nonMappedLeavesT2.size() == 1) {
//            AbstractCodeFragment nonMappedLeave = nonMappedLeavesT2.get(0);
//            if (nonMappedLeave.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
//                if (((StatementObject) nonMappedLeave).getTernaryOperatorExpressions().isEmpty())
//                    return true;
//            }
//        }
        return false;
    }

    private static void checkForExtractClassOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        Set<Replacement> handledReplacements = new HashSet<>();
        Map<String, String> patterns = findPatternsExtractClass(refactorings);

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_REPLACED_WITH_METHOD_INVOCATION)) {

                if (((replacement.getBefore().lastIndexOf("(") == -1)) || ((replacement.getAfter().lastIndexOf("(") == -1))) {
                    if (patterns.containsKey(replacement.getBefore()) && patterns.containsValue(replacement.getAfter())) {
                        handledReplacements.add(replacement);
                    }
                } else if (patterns.containsKey(replacement.getBefore().substring(0,replacement.getBefore().lastIndexOf("("))) && patterns.containsValue(replacement.getAfter().substring(0,replacement.getAfter().lastIndexOf("(")))) {
                    handledReplacements.add(replacement);
                }

            } else if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME)) {

            }else if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {

            } else if (replacement.getType().equals(Replacement.ReplacementType.TYPE)) {
                for (Map.Entry<String, String> entry: patterns.entrySet()) {
                    if (entry.getKey().equals(replacement.getBefore()) && entry.getValue().equals(replacement.getAfter())) {
                        handledReplacements.add(replacement);
                    }
                }
            }

        }

        replacementsToCheck.removeAll(handledReplacements);

    }

    private static Map<String, String> findPatternsExtractClass(List<Refactoring> refactorings) {

        Map<String, String> patterns = new HashMap<>();

        for (Refactoring refactoring: refactorings) {
            if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_CLASS)) {

                String extractedClassName = ((ExtractClassRefactoring) refactoring).getExtractedClass().getNonQualifiedName();

                for (Map.Entry<UMLOperation, UMLOperation> operation: ((ExtractClassRefactoring) refactoring).getExtractedOperations().entrySet()) {
                    patterns.put(operation.getKey().getName(), operation.getValue().getName());
                    patterns.put(operation.getKey().getName(), extractedClassName + "." + operation.getValue().getName());
                    patterns.put(((ExtractClassRefactoring) refactoring).getOriginalClass().getNonQualifiedName(), ((ExtractClassRefactoring) refactoring).getExtractedClass().getNonQualifiedName());
                    // TODO: 8/3/2022 Think about more possible patterns
                }
            }
        }

        return patterns;
    }

    private static void checkForMoveAttributeOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        Set<Replacement> handledReplacements = new HashSet<>();
        ArrayList<String> patterns = findPatternsMoveAttribute(refactorings);

        if (!patterns.isEmpty()) {

            for (Replacement replacement : replacementsToCheck) {
                if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                    String pattern = replacement.getBefore() + replacement.getAfter();
                    if (patterns.contains(pattern)) {
                        handledReplacements.add(replacement);
                    }
                }
            }
        }

        replacementsToCheck.removeAll(handledReplacements);
    }

    private static ArrayList<String> findPatternsMoveAttribute(List<Refactoring> refactorings) {

        ArrayList<String> patterns = new ArrayList<>();

        for (Refactoring refactoring: refactorings) {
            if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_ATTRIBUTE) ||
                    refactoring.getRefactoringType().equals(RefactoringType.MOVE_RENAME_ATTRIBUTE)) {

                String classNameBefore = ((MoveAttributeRefactoring) refactoring).getOriginalAttribute().getNonQualifiedClassName();
                String before = ((MoveAttributeRefactoring) refactoring).getOriginalAttribute().getName();

                String classNameAfter = ((MoveAttributeRefactoring) refactoring).getMovedAttribute().getNonQualifiedClassName();
                String after = ((MoveAttributeRefactoring) refactoring).getMovedAttribute().getName();

                patterns.add(classNameBefore + "." + before + after);
                patterns.add("this." + before + after);
                patterns.add(before + after);
                patterns.add(before + classNameAfter + "." + after);
                patterns.add(classNameBefore + "." + before + after);
                patterns.add(classNameBefore + "." + before + classNameAfter + "." + after);
                patterns.add("this." + before + classNameAfter + "." + after);
            }
        }
        return patterns;
    }

    private static void checkForRenameAttributeOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        Set<Replacement> handledReplacements = new HashSet<>();

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                for (Refactoring refactoring1: refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_ATTRIBUTE)) {
                        if (replacement.getBefore().equals(((RenameAttributeRefactoring)refactoring1).getOriginalAttribute().getName()) &&
                                replacement.getAfter().equals(((RenameAttributeRefactoring)refactoring1).getRenamedAttribute().getName())) {
                            handledReplacements.add(replacement);
                            break;
                        }

//                        This second check is kind of a relax check to cover the cases contain "this." pattern. It can happen in cases of Rename Attribute.
                        if (replacement.getBefore().contains(((RenameAttributeRefactoring)refactoring1).getOriginalAttribute().getName()) &&
                                replacement.getAfter().contains(((RenameAttributeRefactoring)refactoring1).getRenamedAttribute().getName())) {
                            handledReplacements.add(replacement);
                            break;
                        }
                    }
                }
            }
        }

        replacementsToCheck.removeAll(handledReplacements);

    }

    private static boolean checkReplacements(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings) {

        HashSet<Replacement> replacementsToCheck;

//            This method also checks for the exact matches when we have Type Replacement
        if (refactoring.getBodyMapper().allMappingsArePurelyMatched(refactoring.getParameterToArgumentMap())) {
            return true;
        }
        else {
            replacementsToCheck = new HashSet<>(refactoring.getReplacements());
            replacementsToCheck = refactoring.getBodyMapper().omitReplacementsRegardingExactMappings(refactoring.getParameterToArgumentMap(), replacementsToCheck);
            replacementsToCheck = refactoring.getBodyMapper().omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);

            for (Refactoring refactoring1 : refactorings) {
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                    if (((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(refactoring.getSourceOperationAfterExtraction().getName()) &&
                            !((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(refactoring.getExtractedOperation().getName())) {

                        adjustTheParameterArgumentField(refactoring, (ExtractOperationRefactoring) refactoring1);
                        replacementsToCheck = refactoring.getBodyMapper().omitReplacementsRegardingExactMappings(refactoring.getParameterToArgumentMap(), replacementsToCheck);
                    }
                }
            }

//            omitReplacementsRegardingInvocationArguments(refactoring, replacementsToCheck);
            replacementsToCheck = refactoring.getBodyMapper().omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);

            omitThisPatternReplacements(replacementsToCheck);
            checkForParameterArgumentPair(refactoring, replacementsToCheck);
            omitPrintAndLogMessagesRelatedReplacements(refactoring, replacementsToCheck);
            omitBooleanVariableDeclarationReplacement(refactoring, replacementsToCheck); // For the runTests commit
            omitEqualStringLiteralsReplacement(replacementsToCheck);

            omitReplacementRegardingInvertCondition(refactoring, replacementsToCheck);
            omitReturnRelatedReplacements(refactoring, replacementsToCheck);
            omitAnonymousClassDeclarationReplacements(replacementsToCheck);
            omitStringRelatedReplacements(replacementsToCheck);




        }


        if (replacementsToCheck.isEmpty()) {
            return true;
        }


        omitPrimitiveTypeReplacements(refactoring.getReplacements(), replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForAddParameterInSubExpressionOnTop(refactoring, refactorings, replacementsToCheck);
        if (replacementsToCheck.isEmpty()) {
            return true;
        }

        checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForRemoveParameterOnTop(refactoring, refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForRenameVariableOnTop(refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForRenameMethodRefactoringOnTop_Mapped(refactorings, replacementsToCheck);

        checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForEncapsulateAttributeOnTop(refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForRemoveVariableOnTop(refactoring, refactorings, replacementsToCheck);
        if (replacementsToCheck.isEmpty())
            return true;

        checkForMergeVariableOnTop(refactoring, refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForExtractClassOnTop(refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;
        checkForExtractMethodOnTop(refactorings, replacementsToCheck, refactoring);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForThisPatternReplacement(replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForExtractVariableOnTop(refactoring, refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForMergeConditionalOnTop(refactoring, refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        refactoring.getBodyMapper().omitReplacementsAccordingSupplierGetPattern(refactoring.getParameterToArgumentMap(), replacementsToCheck);

        checkTheReplacementsAlreadyHandled(refactoring, replacementsToCheck);

//            for https://github.com/infinispan/infinispan/commit/043030723632627b0908dca6b24dae91d3dfd938 commit - performLocalRehashAwareOperation
        if (replacementsToCheck.isEmpty()) {
            return true;
        }

        int size1 = replacementsToCheck.size();
        int numberOfArgumentReplacedWithReturnReplacements = 0;

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                numberOfArgumentReplacedWithReturnReplacements++;
            }
        }

        if (numberOfArgumentReplacedWithReturnReplacements == size1) {
            return true;
        }

        return false;
    }

    private static void checkForMergeConditionalOnTop(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Refactoring refactoring1 : refactorings) {

            if (refactoring1.getRefactoringType().equals(RefactoringType.MERGE_CONDITIONAL)) {

                for (Replacement replacement : replacementsToCheck) {

                    if (replacement.getType().equals(Replacement.ReplacementType.COMPOSITE)
                            || replacement.getType().equals(Replacement.ReplacementType.CONDITIONAL)) {
                        if (replacement.getAfter().equals(((MergeConditionalRefactoring) (refactoring1)).getNewConditional().getString()) ||
                                replacement.getAfter().equals(((MergeConditionalRefactoring) (refactoring1)).getNewConditional().getArgumentizedString())) {
                            replacementsToRemove.add(replacement);
                        }
                    }
                }
            }
        }
        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static void omitBooleanVariableDeclarationReplacement(Refactoring refactoring, HashSet<Replacement> replacementsToCheck) {
        // For the runTests commit, boolean result = false need to map with boolean result = true

        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        } else {
            return;
        }

        Set<Replacement> replacementsToRemove = new HashSet<>();


        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.BOOLEAN_LITERAL)) {
                for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
                    for (Replacement mappingReplacement : mapping.getReplacements()) {
                        if (mappingReplacement.equals(replacement)) {
                            if (checkForBooleanLiteralChangeInDeclaration(mapping)) {
                                replacementsToRemove.add(mappingReplacement);
                                break;
                            }
                        }
                    }
                }
            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);
    }

    private static boolean checkForBooleanLiteralChangeInDeclaration(AbstractCodeMapping mapping) {

        if (mapping.getFragment1().getVariableDeclarations().isEmpty() || mapping.getFragment2().getVariableDeclarations().isEmpty()) {
            return false;
        }

        if (mapping.getFragment1().getVariableDeclarations().size() == 1) {
            VariableDeclaration declaration = mapping.getFragment1().getVariableDeclarations().get(0);
            return declaration.getType().getClassType().equals("boolean");
        }

        return false;
    }

    private static void checkForRenameVariableOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        Set<Replacement> handledReplacements = new HashSet<>();

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
                for (Refactoring refactoring1: refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_VARIABLE) || refactoring1.getRefactoringType().equals(RefactoringType.RENAME_PARAMETER) ||
                            refactoring1.getRefactoringType().equals(RefactoringType.PARAMETERIZE_ATTRIBUTE) || refactoring1.getRefactoringType().equals(RefactoringType.REPLACE_ATTRIBUTE_WITH_VARIABLE) ||
                            refactoring1.getRefactoringType().equals(RefactoringType.LOCALIZE_PARAMETER)) {
                        if (replacement.getBefore().equals(((RenameVariableRefactoring)refactoring1).getOriginalVariable().getVariableName()) &&
                                replacement.getAfter().equals(((RenameVariableRefactoring)refactoring1).getRenamedVariable().getVariableName())) {
                            handledReplacements.add(replacement);
                            break;
                        }
                    }
                }
            }
        }

//        For handling: https://github.com/crashub/crash/commit/2801269c7e47bd6e243612654a74cee809d20959. When we have extracted some part of an expression.
//        for (Replacement replacement: replacementsToCheck) {
//            if (replacement.getType().equals(Replacement.ReplacementType.VARIABLE_NAME)) {
//                if (refactoring.getExtractedOperation().getParameterNameList().contains(replacement.getAfter())) {
//                    handledReplacements.add(replacement);
//                }
//            }
//        }

        replacementsToCheck.removeAll(handledReplacements);
    }

    private static void checkForParametrizationOrAddParameterOnTop(Refactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.INLINE_OPERATION)) {
            bodyMapper = ((InlineOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        }else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        }else {
            return;
        }

        Set<Replacement> replacementsToRemove = new HashSet<>();

//        List<MethodInvocationReplacement> methodInvocationReplacements = getSpecificReplacementType(refactoring.getReplacements(), MethodInvocationReplacement.class);
        for (Replacement replacement: replacementsToCheck) {

            if (replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.CLASS_INSTANCE_CREATION_ARGUMENT)) {
                checkForAddParameterOnTopConstructorVersion(bodyMapper, replacement, refactorings, replacementsToCheck);
            }

            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    ((replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) || (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME_AND_ARGUMENT))) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName()))) {

                    ArrayList<String> temp1 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationAfter().arguments());
                    ArrayList<String> temp2 = new ArrayList<>(temp1);
                    temp1.removeAll(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().arguments());
                    ArrayList<Integer> addedArgumentsLocation = new ArrayList<>();

                    for (int i = 0; i < temp1.size(); i++) {
                        for (int j = 0; j < temp2.size(); j++) {
                            if (temp1.get(i).equals(temp2.get(j)))
                                addedArgumentsLocation.add(j);
                        }
                    }

                    String methodName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();
//                List<RenameVariableRefactoring> renameVariableRefactoringList = getSpecificTypeRefactoring(refactorings,RenameVariableRefactoring.class);
                    List<Refactoring> parametrizeVariableAndAddParameterRefactoringList = new ArrayList<>();

                    for (Refactoring refactoring1 : refactorings) {
                        if (refactoring1.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE) ||
                                refactoring1.getRefactoringType().equals(RefactoringType.ADD_PARAMETER)) {
                            parametrizeVariableAndAddParameterRefactoringList.add(refactoring1);
                        }
                    }

                    ArrayList<Integer> parameterizedAndAddedLocation = new ArrayList<>();

                    for (Refactoring ref : parametrizeVariableAndAddParameterRefactoringList) {
                        if (ref.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE)) {
                            if (((RenameVariableRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                                int ind = ((RenameVariableRefactoring) ref).getOperationAfter().getParameterNameList().indexOf(((RenameVariableRefactoring) ref).getRenamedVariable().getVariableName());
                                if (!parameterizedAndAddedLocation.contains(ind))
                                    parameterizedAndAddedLocation.add(ind);
                            }
                        } else {
                            if (((AddParameterRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                                int ind = ((AddParameterRefactoring) ref).getOperationAfter().getParameterNameList().indexOf(((AddParameterRefactoring) ref).getParameter().getName());
                                if (!parameterizedAndAddedLocation.contains(ind))
                                    parameterizedAndAddedLocation.add(ind);
                            }
                        }
                    }
                    Collections.sort(addedArgumentsLocation);
                    Collections.sort(parameterizedAndAddedLocation);
                    if (addedArgumentsLocation.equals(parameterizedAndAddedLocation) && !addedArgumentsLocation.isEmpty()) {
                        replacementsToRemove.add(replacement);
                    }

            }
        }

        replacementsToCheck.removeAll(replacementsToRemove);

    }

    private static void checkForAddParameterOnTopConstructorVersion(UMLOperationBodyMapper bodyMapper, Replacement replacement, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        for (AbstractCodeMapping mapping : bodyMapper.getMappings()) {
            for (Replacement mappingReplacement : mapping.getReplacements()) {
                if (mappingReplacement.equals(replacement)) {
                    Optional<AbstractCall> actualValue1 = mapping.getFragment2().getCreations()
                            .stream()
                            .findFirst();

                    if (actualValue1.isPresent()) {
                        ArrayList<String> temp1 = new ArrayList<>(actualValue1.get().arguments());
                        ArrayList<String> temp2 = new ArrayList<>(temp1);
                        Optional<AbstractCall> actualValue2 = mapping.getFragment1().getCreations()
                                .stream()
                                .findFirst();
                        actualValue2.ifPresent(stringListEntry -> temp1.removeAll(stringListEntry.arguments()));

                        ArrayList<Integer> addedArgumentsLocationInReplacement = new ArrayList<>();

                        for (int i = 0; i < temp1.size(); i++) {
                            for (int j = 0; j < temp2.size(); j++) {
                                if (temp1.get(i).equals(temp2.get(j)))
                                    addedArgumentsLocationInReplacement.add(j);
                            }
                        }

                        String methodName = actualValue1.get().getName();
                        List<Refactoring> addParameterRefactoringList = new ArrayList<>();

                        for (Refactoring refactoring1 : refactorings) {
                            if (refactoring1.getRefactoringType().equals(RefactoringType.ADD_PARAMETER)) {
                                addParameterRefactoringList.add(refactoring1);
                            }
                        }

                        ArrayList <Integer> addedArgumentLocationInRefactoring = new ArrayList<>();

                        for (Refactoring ref : addParameterRefactoringList) {
                            if (ref.getRefactoringType().equals(RefactoringType.ADD_PARAMETER)) {
                                if (((AddParameterRefactoring)ref).getOperationBefore().getName().equals(methodName)) {
                                    int ind = ((AddParameterRefactoring)ref).getOperationBefore().getParameterNameList().indexOf(((AddParameterRefactoring)ref).getParameter().getName());
                                    addedArgumentLocationInRefactoring.add(ind);
                                }
                            }
                        }


//                    In case of presence of constructors, it might there are multiple constructors. Like calling overloaded methods. So, in this case the location of
//                    the removed parameter may not be deterministic. So, in this case, we just check the size of the two lists.

//                    Collections.sort(removedArgumentsLocationInReplacement);
//                    Collections.sort(removedArgumentLocationInRefactoring);
//                    if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
//                        replacementsToCheck.remove(replacement);
//                    }

                        if ((addedArgumentsLocationInReplacement.size() == addedArgumentLocationInRefactoring.size()) && !addedArgumentsLocationInReplacement.isEmpty()) {
                            replacementsToCheck.remove(replacement);
                        }
                        break;
                    }
                }
            }
        }
    }

    private static void checkForRenameMethodRefactoringOnTop_Mapped(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        // This method also handles the MoveAndRename Method on top
        // TODO: 8/3/2022 handle "Variable Replaced With Method Invocation case" replacement also
        List<Refactoring> renameOperationRefactoringList = new ArrayList<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_METHOD) || refactoring1.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
                renameOperationRefactoringList.add(refactoring1);
            }
        }

        Set<Replacement> handledReplacements = new HashSet<>();

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) ||
                    replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME))  {
                if (isRenameWithName(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName(), ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName(), renameOperationRefactoringList)) {
                    handledReplacements.add(replacement);
                }
            }
        }

        replacementsToCheck.removeAll(handledReplacements);

    }

    private static void checkForRenameRefactoringOnTopOfInline_NonMapped(InlineOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT1) {

        List<Refactoring> renameOperationRefactoringList = new ArrayList<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_METHOD) || refactoring1.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
                renameOperationRefactoringList.add(refactoring1);
            }
        }

        if (renameOperationRefactoringList.isEmpty()) {
            return;
        }

        for(AbstractCodeFragment abstractCodeFragment2 : refactoring.getBodyMapper().getNonMappedLeavesT1()) {
            List<AbstractCall> methodInvocationMap2 = abstractCodeFragment2.getMethodInvocations();
            List<String> methodCalls2 = methodInvocationMap2.stream().map(l -> l.getName()).collect(Collectors.toList());
            if (!methodCalls2.isEmpty())
                for (AbstractCodeFragment abstractCodeFragment : refactoring.getBodyMapper().getNonMappedLeavesT2()) {
                    List<AbstractCall> methodInvocationMap = abstractCodeFragment.getMethodInvocations();
                    List<String> methodCalls = methodInvocationMap.stream().map(l -> l.getName()).collect(Collectors.toList());
                    boolean check = checkRenameMethodCallsPossibility(methodCalls2, methodCalls, renameOperationRefactoringList);
                    if (check) {
                        nonMappedLeavesT1.remove(abstractCodeFragment2);
                        break;
                    }
                }
        }
    }


    private static void checkForRenameRefactoringOnTop_NonMapped(Refactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2, List<AbstractCodeFragment> nonMappedLeavesT1) {

//        List<RenameOperationRefactoring> renameOperationRefactoringList = getSpecificTypeRefactoring(refactorings,RenameOperationRefactoring.class);
        List<Refactoring> renameOperationRefactoringList = new ArrayList<>();

        for (Refactoring refactoring1 : refactorings) {
            if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_METHOD) || refactoring1.getRefactoringType().equals(RefactoringType.MOVE_AND_RENAME_OPERATION)) {
                renameOperationRefactoringList.add(refactoring1);
            }
        }

        if (renameOperationRefactoringList.isEmpty()) {
            return;
        }

        List<AbstractCodeFragment> nonMappedLeavesT2ToRemove = new ArrayList<>();
        List<AbstractCodeFragment> nonMappedLeavesT1ToRemove = new ArrayList<>();


        UMLOperationBodyMapper bodyMapper = null;
        if (refactoring.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
            bodyMapper = ((ExtractOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.MOVE_OPERATION)) {
            bodyMapper = ((MoveOperationRefactoring) (refactoring)).getBodyMapper();
        } else if (refactoring.getRefactoringType().equals(RefactoringType.PUSH_DOWN_OPERATION)) {
            bodyMapper = ((PushDownOperationRefactoring) (refactoring)).getBodyMapper();
        }else {
            return;
        }

        int nonMappedT2 = bodyMapper.getNonMappedLeavesT2().size();
        for(AbstractCodeFragment abstractCodeFragment2 : bodyMapper.getNonMappedLeavesT2()) {
            List<AbstractCall> methodInvocationMap2 = abstractCodeFragment2.getMethodInvocations();
            List<String> methodCalls2 = methodInvocationMap2.stream().map(l -> l.getName()).collect(Collectors.toList());
            if (!methodCalls2.isEmpty())
                for (AbstractCodeFragment abstractCodeFragment : bodyMapper.getNonMappedLeavesT1()) {
                    List<AbstractCall> methodInvocationMap = abstractCodeFragment.getMethodInvocations();
                    List<String> methodCalls = methodInvocationMap.stream().map(l -> l.getName()).collect(Collectors.toList());
                    boolean check = checkRenameMethodCallsPossibility(methodCalls, methodCalls2, renameOperationRefactoringList);
                    if (check) {
                        nonMappedLeavesT2ToRemove.add(abstractCodeFragment2);
                        nonMappedLeavesT1ToRemove.add(abstractCodeFragment);
                        break;
                    }
                }
        }

        nonMappedLeavesT2.removeAll(nonMappedLeavesT2ToRemove);
        nonMappedLeavesT1.removeAll(nonMappedLeavesT1ToRemove);

    }
    private static boolean checkRenameMethodCallsPossibility(List<String> methodCalls1, List<String> methodCalls2, List<Refactoring> renameOperationRefactoringList) {
        if (methodCalls2.size() != methodCalls1.size())
            return false;
        ArrayList<String> mc1Temp = new ArrayList<>(methodCalls1);
        ArrayList<String> mc2Temp = new ArrayList<>(methodCalls2);
        mc1Temp.removeAll(methodCalls2);
        mc2Temp.removeAll(methodCalls1);
        int _renameCounter = mc2Temp.size();
        for (String call1 : mc1Temp) {
            boolean _met = false;
            for (String call2 : mc2Temp) {
                boolean _check = isRenameWithName(call1,call2,renameOperationRefactoringList);
                if (_check)
                {
                    _met = true;
                    break;
                }
            }
            if (_met)
                _renameCounter -= 1;
        }
        return (_renameCounter == 0);
    }

    private static boolean isRenameWithName(String call1, String call2, List<Refactoring> renameOperationRefactoringList) {
        for(Refactoring renameOperationRefactoring : renameOperationRefactoringList) {
            if (renameOperationRefactoring instanceof RenameOperationRefactoring) {
                if (((RenameOperationRefactoring) (renameOperationRefactoring)).getOriginalOperation().getName().equals(call1)
                        &&
                        ((RenameOperationRefactoring) (renameOperationRefactoring)).getRenamedOperation().getName().equals(call2)) {
                    return true;
                }
            } else {
                if (((MoveOperationRefactoring) (renameOperationRefactoring)).getOriginalOperation().getName().equals(call1)
                        &&
                        ((MoveOperationRefactoring) (renameOperationRefactoring)).getMovedOperation().getName().equals(call2)) {
                    return true;
                }
            }
        }
        return false;
    }


    private static <T extends Refactoring> List<T> getSpecificTypeRefactoring(List<Refactoring> refactorings, Class<T> clazz)
    {
        List<T> result = new ArrayList<>();
        for (Refactoring refactoring : refactorings)
        {
                if (refactoring.getClass().equals(clazz))
                    result.add(clazz.cast(refactoring));
        }
        return result;
    }

    private static <T extends Replacement> List<T> getSpecificReplacementType(Set<Replacement> replacements, Class<T> clazz) {
        List<T> result = new ArrayList<>();

        for (Replacement replacement: replacements) {
            if (replacement.getClass().equals(clazz))
                result.add(clazz.cast(replacement));
        }
        return result;
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
        int returnCounter = 0;

            for (AbstractCodeFragment st : nonMappedLeavesT2) {
                if (!st.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    counter++;
                }else
                    returnCounter++;
            }
        if (returnCounter == 1)
            return counter <= acceptanceRate;

        return false;
    }

    private static void omitPrimitiveTypeReplacements(Set<Replacement> replacements, Set<Replacement> replacementsToCheck) {


        for (Replacement rep: replacements) {
            if (rep.getType().equals(Replacement.ReplacementType.TYPE)) {
                replacementsToCheck.remove(rep);
            }
        }

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

    String purityComment;
    String description;

    /*
    1: All statements have been mapped
    2: There is at least one non-mapped leaf
    3: There is at least one non-mapped node
 */
    int mappingState;



    PurityCheckResult(boolean isPure, String description, String purityComment){
        this.isPure = isPure;
        this.description = description;
        this.purityComment = purityComment;
    }

    PurityCheckResult(boolean isPure, String description){
        this.isPure = isPure;
        this.description = description;
    }

    PurityCheckResult(boolean isPure, String description, String purityComment, int mappingState){
        this.isPure = isPure;
        this.description = description;
        this.purityComment = purityComment;
        this.mappingState = mappingState;
    }

    public boolean isPure() {
        return isPure;
    }

    public String getDescription() {
        return description;
    }

    public String getPurityComment() { return purityComment; }

    public int getMappingState() { return mappingState; }

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


