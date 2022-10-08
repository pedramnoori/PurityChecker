package purity;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.*;
import gr.uom.java.xmi.decomposition.replacement.MethodInvocationReplacement;
import gr.uom.java.xmi.decomposition.replacement.Replacement;
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
        PurityCheckResult result;
        switch (refactoring.getRefactoringType()){
            case EXTRACT_OPERATION:
                result = detectExtractOperationPurity((ExtractOperationRefactoring) refactoring, refactorings);
                break;
            case RENAME_CLASS:
                result = detectRenameClassPurity((RenameClassRefactoring) refactoring, refactorings, modelDiff);
                break;
            case RENAME_VARIABLE:
                result = detectRenameVariablePurity((RenameVariableRefactoring) refactoring);
                break;
            case RENAME_PARAMETER:
                result = detectRenameParameterPurity((RenameVariableRefactoring) refactoring);
                break;
            case MOVE_OPERATION:
                result = detectMoveMethodPurity((MoveOperationRefactoring) refactoring, refactorings, modelDiff);
                break;
            default:
                result = null;

        }

        return result;
    }

    private static PurityCheckResult detectMoveMethodPurity(MoveOperationRefactoring refactoring, List<Refactoring> refactorings, UMLModelDiff modelDiff) {

        System.out.println("MOVE METHOD");

        if (refactoring.getOriginalOperation().getParameterNameList().size() != refactoring.getMovedOperation().getParameterNameList().size()) {

            return new PurityCheckResult(false, "Number of parameters is different. Strong clue of impurity in case of Move Method refactorings!");
        }

        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty() &&
        refactoring.getBodyMapper().getNonMappedLeavesT1().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT1().isEmpty()) {

            if (refactoring.getReplacements().isEmpty())
                return new PurityCheckResult(true, "There is no replacement! - all mapped");

            HashSet<Replacement> replacementsToCheck = new HashSet<>(refactoring.getReplacements());


            allReplacementsAreType(refactoring.getReplacements(), replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "All replacements are variable type! - all mapped");
            }

            checkForRemoveParameterOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the moved method - all mapped");
            }

            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the moved method - all mapped");
            }

            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the moved method - all mapped");
            }

            checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Attribute on top of the extracted method - all mapped");
            }

            checkForExtractClassOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Class on top of the extracted method - all mapped");
            }

            checkForExtractMethodOnTop(refactorings, replacementsToCheck, refactoring);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Method on top of the extracted method - all mapped");
            }

            checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped");
            }

            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped");
            }

            checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped");
            }

            checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move method on the top of the moved method - all mapped");
            }

            checkForThisPatternReplacement(replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Contains this pattern - all mapped");
            }

        } else if (refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty()){

        } else {

        }



        return new PurityCheckResult(false, "Not decided yet!");
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

//        ArrayList<ExtractClassRefactoring> extractClassRefactorings = (ArrayList<ExtractClassRefactoring>) getSpecificTypeRefactoring(refactorings, ExtractClassRefactoring.class);

        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty() && refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty()) {


            if (refactoring.getReplacements().isEmpty())
                return new PurityCheckResult(true, "There is no replacement! - all mapped");

            HashSet<Replacement> replacementsToCheck;

//            This method also checks for the exact matches when we have Type Replacement
            if (refactoring.getBodyMapper().allMappingsArePurelyMatched(refactoring.getParameterToArgumentMap())) {
                return new PurityCheckResult(true, "All the mappings are matched! - all mapped");
            }
            else {
                replacementsToCheck = new HashSet<>(refactoring.getReplacements());
                replacementsToCheck = refactoring.getBodyMapper().omitReplacementsRegardingExactMappings(refactoring.getParameterToArgumentMap(), replacementsToCheck);
                replacementsToCheck = refactoring.getBodyMapper().omitReplacementsAccordingToArgumentization(refactoring.getParameterToArgumentMap(), replacementsToCheck);
                omitReplacementsRegardingInvocationArguments(refactoring, replacementsToCheck);
                checkForParameterArgumentPair(refactoring, replacementsToCheck);
                omitPrintAndLogMessagesRelatedReplacements(refactoring, replacementsToCheck);
                omitBooleanVariableDeclarationReplacement(refactoring, replacementsToCheck); // For the runTests commit
                omitEqualStringLiteralsReplacement(replacementsToCheck);
            }

            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "All replacements have been justified - all mapped");
            }


            allReplacementsAreType(refactoring.getReplacements(), replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "All replacements are variable type! - all mapped");
            }


            checkForRenameMethodRefactoringOnTop_Mapped(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Method Refactoring on the top of the extracted method - all mapped");
            }


            checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Parametrization or Add Parameter on top of the extract method - all mapped");
            }

            checkForRemoveParameterOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the extracted method - all mapped");
            }

            checkForRenameVariableOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the extract method - all mapped");
            }

            checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the extracted method - all mapped");
            }

            checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Attribute on top of the extracted method - all mapped");
            }

            checkForExtractClassOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Class on top of the extracted method - all mapped");
            }

            checkForExtractMethodOnTop(refactorings, replacementsToCheck, refactoring);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Method on top of the extracted method - all mapped");
            }

            checkForMoveClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped");
            }

            checkForRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped");
            }

            checkForMoveAndRenameClassRefactoringOnTop(refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move class on the top of the moved method - all mapped");
            }

            checkForMoveMethodRefactoringOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move method on the top of the moved method - all mapped");
            }

            checkForThisPatternReplacement(replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Contains this pattern - all mapped");
            }

            replacementsToCheck = refactoring.getBodyMapper().omitReplacementsAccordingSupplierGetPattern(refactoring.getParameterToArgumentMap(), replacementsToCheck);
//            for https://github.com/infinispan/infinispan/commit/043030723632627b0908dca6b24dae91d3dfd938 commit - performLocalRehashAwareOperation
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Contains supplier-get pattern - all mapped");
            }


            if (replacementsToCheck.size() == 1) {
                for (Replacement replacement: replacementsToCheck) {
                    if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                        return new PurityCheckResult(true, "Argument replaced with return expression - all mapped");
                    }
                }
            }

//        Check non-mapped leaves
        } else if (refactoring.getBodyMapper().getNonMappedInnerNodesT2().isEmpty()){


            if (!checkReplacements(refactoring, refactorings)) {
                return new PurityCheckResult(false, "replacements are not justified - non-mapped leaves");
            }

            List<AbstractCodeFragment> nonMappedLeavesT2 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT2());

            checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "Rename Refactoring on the top of the extracted method - with non-mapped leaves");
            }

            checkForPrint_NonMapped(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "Extra print lines - with non-mapped leaves");
            }

            checkForStatementsBeingMappedInOtherRefactorings(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "Mapped statements in other refactorings - with non-mapped leaves");
            }

            checkVariableDeclarationUsage(refactoring, refactorings, nonMappedLeavesT2);
            if (nonMappedLeavesT2.isEmpty()) {
                return new PurityCheckResult(true, "The new variable declared has not been used within the program logic - with non-mapped leaves");
            }


            if (nonMappedLeavesT2.size() == 1) {
                AbstractCodeFragment nonMappedLeave = nonMappedLeavesT2.get(0);
                if (nonMappedLeave.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                    if (((StatementObject) nonMappedLeave).getTernaryOperatorExpressions().isEmpty())
                        return new PurityCheckResult(true, "Return expression has been added within the Extract Method mechanics - with non-mapped leaves");
                }
            }

            return new PurityCheckResult(false, "Violating extract method refactoring mechanics - with non-mapped leaves");
        } else {

            if (!checkReplacements(refactoring, refactorings)) {
                return new PurityCheckResult(false, "Replacements are not justified - non-mapped inner nodes");
            }

            List<AbstractCodeFragment> nonMappedLeavesT2List = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT2());

//            removeNonMappedLeavesRegardingNullCheck(refactoring ,refactorings, nonMappedLeavesT2List); // Added for ignoring the null check in extract methods: https://github.com/wordpress-mobile/WordPress-Android/commit/ab298886b59f4ad0235cd6d5764854189eb59eb6

            if (!checkNonMappedLeaves(refactoring, refactorings, nonMappedLeavesT2List)) {
                return new PurityCheckResult(false, "Non-mapped leaves are not justified - non-mapped inner nodes");
            }

            List<AbstractCodeFragment> nonMappedInnerNodesT2 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedInnerNodesT2());

            checkForIfCondition(refactoring, nonMappedInnerNodesT2); //TODO -> I don't why I have added this. Need to check with the help of the oracle.

            if (nonMappedInnerNodesT2.size() == 1) {
                AbstractCodeFragment nonMappedLeave = nonMappedInnerNodesT2.get(0);
                if (nonMappedLeave.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.BLOCK)) {
                    return new PurityCheckResult(true, "Just an empty block - with non-mapped leaves");
                }
            }

            return new PurityCheckResult(false, "Contains non-mapped inner nodes");
        }

        return new PurityCheckResult(false, "Not decided yet!");
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
                if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                    for (AbstractCodeMapping mapping : ((ExtractOperationRefactoring) (refactoring1)).getBodyMapper().getMappings()) {
                        if (mapping.getFragment2().equals(abstractCodeFragment)) {
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

    private static void omitPrintAndLogMessagesRelatedReplacements(ExtractOperationRefactoring refactoring, HashSet<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (AbstractCodeMapping mapping : refactoring.getBodyMapper().getMappings()) {
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
                fragmentString.contains("log.trace") || fragmentString.contains("log.tracef");
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
                    replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT)) {
                String invokedOperationAfterName = ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName();
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
                        if (((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getName().equals(invokedOperationAfterName)) {
                            if (((ExtractOperationRefactoring) refactoring1).getExtractedOperation().getParameterNameList().size() == ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getArguments().size()) {
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

    private static void checkForIfCondition(ExtractOperationRefactoring refactoring, List<AbstractCodeFragment> nonMappedInnerNodesT2) {

        List<AbstractCodeFragment> nonMappedInnerNodesToRemove = new ArrayList<>();
        List<String> conditionVariables = new ArrayList<>();

        for (AbstractCodeFragment abstractCodeFragment : nonMappedInnerNodesT2) {
            if (abstractCodeFragment.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.IF_STATEMENT)) {
                for (AbstractExpression expression : ((CompositeStatementObject) abstractCodeFragment).getExpressions()) {
                    conditionVariables.addAll(expression.getVariables());
                }
                if (refactoring.getExtractedOperation().getParameterNameList().containsAll(conditionVariables)) {
                    nonMappedInnerNodesToRemove.add(abstractCodeFragment);
                }
            }
        }

        nonMappedInnerNodesT2.removeAll(nonMappedInnerNodesToRemove);

    }

    private static void checkForRemoveParameterOnTop(List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {


        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName()))) {

                ArrayList<String> temp1 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getArguments());
                ArrayList<String> temp2 = new ArrayList<>(temp1);
                temp1.removeAll(((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getArguments());
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

                ArrayList <Integer> removedArgumentLocationInRefactoring = new ArrayList<>();

                for (Refactoring ref : removeParameterRefactoringList) {
                    if (ref.getRefactoringType().equals(RefactoringType.REMOVE_PARAMETER)) {
                        if (((RemoveParameterRefactoring)ref).getOperationBefore().getName().equals(methodName)) {
                            int ind = ((RemoveParameterRefactoring)ref).getOperationBefore().getParameterNameList().indexOf(((RemoveParameterRefactoring)ref).getParameter().getName());
                            removedArgumentLocationInRefactoring.add(ind);
                        }
                    }
                }
                Collections.sort(removedArgumentsLocationInReplacement);
                Collections.sort(removedArgumentLocationInRefactoring);
                if (removedArgumentsLocationInReplacement.equals(removedArgumentLocationInRefactoring) && !removedArgumentsLocationInReplacement.isEmpty()) {
                    replacementsToCheck.remove(replacement);
                }

            }
        }

    }

    private static void checkForParameterArgumentPair(ExtractOperationRefactoring refactoring, Set<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT)) {
                List<String> invokedAfterArguments = ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getArguments();
                List<String> extractedOperationArguments = refactoring.getExtractedOperation().getParameterNameList();

                if (invokedAfterArguments.containsAll(extractedOperationArguments) && extractedOperationArguments.containsAll(invokedAfterArguments) &&
                ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getArguments().size() == ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getArguments().size()) {
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

    private static boolean checkNonMappedLeaves(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2) {

        if (nonMappedLeavesT2.isEmpty()) {
            return true;
        }

        checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2);
        if (nonMappedLeavesT2.isEmpty()) {
            return true;
        }

        checkForPrint_NonMapped(refactoring, refactorings, nonMappedLeavesT2);
        checkForStatementsBeingMappedInOtherRefactorings(refactoring, refactorings, nonMappedLeavesT2);

        if (nonMappedLeavesT2.isEmpty()) {
            return true;
        }

        if (nonMappedLeavesT2.size() == 1) {
            AbstractCodeFragment nonMappedLeave = nonMappedLeavesT2.get(0);
            if (nonMappedLeave.getLocationInfo().getCodeElementType().equals(LocationInfo.CodeElementType.RETURN_STATEMENT)) {
                if (((StatementObject) nonMappedLeave).getTernaryOperatorExpressions().isEmpty())
                    return true;
            }
        }
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
            omitReplacementsRegardingInvocationArguments(refactoring, replacementsToCheck);
            checkForParameterArgumentPair(refactoring, replacementsToCheck);
            omitPrintAndLogMessagesRelatedReplacements(refactoring, replacementsToCheck);
            omitBooleanVariableDeclarationReplacement(refactoring, replacementsToCheck); // For the runTests commit
            omitEqualStringLiteralsReplacement(replacementsToCheck);
        }


        if (replacementsToCheck.isEmpty()) {
            return true;
        }


        allReplacementsAreType(refactoring.getReplacements(), replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForRemoveParameterOnTop(refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForRenameVariableOnTop(refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForRenameAttributeOnTop(refactorings, replacementsToCheck);
        if(replacementsToCheck.isEmpty())
            return true;

        checkForMoveAttributeOnTop(refactorings, replacementsToCheck);
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

        replacementsToCheck = refactoring.getBodyMapper().omitReplacementsAccordingSupplierGetPattern(refactoring.getParameterToArgumentMap(), replacementsToCheck);
//            for https://github.com/infinispan/infinispan/commit/043030723632627b0908dca6b24dae91d3dfd938 commit - performLocalRehashAwareOperation
        if (replacementsToCheck.isEmpty()) {
            return true;
        }

        replacementsToCheck = refactoring.getBodyMapper().omitReplacementsAccordingSupplierGetPattern(refactoring.getParameterToArgumentMap(), replacementsToCheck);




        if(replacementsToCheck.isEmpty()) {
            return true;
        }

        if (replacementsToCheck.size() == 1) {
            for (Replacement replacement: replacementsToCheck) {
                if (replacement.getType().equals(Replacement.ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void omitBooleanVariableDeclarationReplacement(ExtractOperationRefactoring refactoring, HashSet<Replacement> replacementsToCheck) {
        // For the runTests commit
        Set<Replacement> replacementsToRemove = new HashSet<>();


        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.BOOLEAN_LITERAL)) {
                for (AbstractCodeMapping mapping : refactoring.getBodyMapper().getMappings()) {
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
                    if (refactoring1.getRefactoringType().equals(RefactoringType.RENAME_VARIABLE) || refactoring1.getRefactoringType().equals(RefactoringType.RENAME_PARAMETER)) {
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

    private static void checkForParametrizationOrAddParameterOnTop(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

//        List<MethodInvocationReplacement> methodInvocationReplacements = getSpecificReplacementType(refactoring.getReplacements(), MethodInvocationReplacement.class);
        for (Replacement replacement: refactoring.getReplacements()) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT) ||
                    (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION) && ((MethodInvocationReplacement)replacement).getInvokedOperationAfter().getName().equals(((MethodInvocationReplacement)replacement).getInvokedOperationBefore().getName()))) {

                ArrayList<String> temp1 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getArguments());
                ArrayList<String> temp2 = new ArrayList<>(temp1);
                temp1.removeAll(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getArguments());
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

                ArrayList <Integer> parameterizedAndAddedLocation = new ArrayList<>();

                for (Refactoring ref : parametrizeVariableAndAddParameterRefactoringList) {
                    if (ref.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE)) {
                        if (((RenameVariableRefactoring)ref).getOperationBefore().getName().equals(methodName)) {
                            int ind = ((RenameVariableRefactoring)ref).getOperationAfter().getParameterNameList().indexOf(((RenameVariableRefactoring)ref).getRenamedVariable().getVariableName());
                            if (!parameterizedAndAddedLocation.contains(ind))
                                parameterizedAndAddedLocation.add(ind);
                        }
                    }else {
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
                    replacementsToCheck.remove(replacement);
                    }

                }
        }

    }

    private static void checkForRenameMethodRefactoringOnTop_Mapped(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        // TODO: 8/3/2022 handle "Variable Replaced With Method Invocation case" replacement also
        List<RenameOperationRefactoring> renameOperationRefactoringList = getSpecificTypeRefactoring(refactorings,RenameOperationRefactoring.class);

        Set<Replacement> handledReplacements = new HashSet<>();

        for (Replacement replacement: replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                if (isRenameWithName(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName(), ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName(), renameOperationRefactoringList)) {
                    handledReplacements.add(replacement);
                }
            }
        }

        replacementsToCheck.removeAll(handledReplacements);

    }


    private static void checkForRenameRefactoringOnTop_NonMapped(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, List<AbstractCodeFragment> nonMappedLeavesT2) {

        List<RenameOperationRefactoring> renameOperationRefactoringList = getSpecificTypeRefactoring(refactorings,RenameOperationRefactoring.class);

        if (renameOperationRefactoringList.isEmpty()) {
            return;
        }
        int nonMappedT2 = refactoring.getBodyMapper().getNonMappedLeavesT2().size();
        for(AbstractCodeFragment abstractCodeFragment2 : refactoring.getBodyMapper().getNonMappedLeavesT2()) {
            Map<String, List<AbstractCall>> methodInvocationMap2 = abstractCodeFragment2.getMethodInvocationMap();
            List<String> methodCalls2 = methodInvocationMap2.values().stream().map(l -> l.get(0).getName()).collect(Collectors.toList());
            if (!methodCalls2.isEmpty())
                for (AbstractCodeFragment abstractCodeFragment : refactoring.getBodyMapper().getNonMappedLeavesT1()) {
                    Map<String, List<AbstractCall>> methodInvocationMap = abstractCodeFragment.getMethodInvocationMap();
                    List<String> methodCalls = methodInvocationMap.values().stream().map(l -> l.get(0).getName()).collect(Collectors.toList());
                    boolean check = checkRenameMethodCallsPossibility(methodCalls, methodCalls2, renameOperationRefactoringList);
                    if (check) {
                        nonMappedLeavesT2.remove(abstractCodeFragment2);
                        break;
                    }
                }
        }

    }
    private static boolean checkRenameMethodCallsPossibility(List<String> methodCalls1, List<String> methodCalls2, List<RenameOperationRefactoring> renameOperationRefactoringList) {
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

    private static boolean isRenameWithName(String call1, String call2, List<RenameOperationRefactoring> renameOperationRefactoringList) {
        for(RenameOperationRefactoring renameOperationRefactoring : renameOperationRefactoringList)
            if (renameOperationRefactoring.getOriginalOperation().getName().equals(call1)
                &&
                renameOperationRefactoring.getRenamedOperation().getName().equals(call2))
                return true;
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

    private static void allReplacementsAreType(Set<Replacement> replacements, Set<Replacement> replacementsToCheck) {

        int counter = 0;

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


