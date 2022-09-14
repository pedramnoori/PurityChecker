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
            default:
                result = null;

        }

        return result;
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

            Set<Replacement> replacementsToCheck;

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

            checkForRemoveParameterOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Remove Parameter refactoring on top the extracted method - all mapped");
            }

            checkForRenameVariableOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Variable on top of the extract method - all mapped");
            }

            checkForRenameAttributeOnTop(refactoring, refactorings, replacementsToCheck);
            if(replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Rename Attribute on the top of the extracted method - all mapped");
            }

            checkForMoveAttributeOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Move Attribute on top of the extracted method - all mapped");
            }

            checkForExtractClassOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Class on top of the extracted method - all mapped");
            }

            checkForExtractMethodOnTop(refactoring, refactorings, replacementsToCheck);
            if (replacementsToCheck.isEmpty()) {
                return new PurityCheckResult(true, "Extract Method on top of the extracted method - all mapped");
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

            if (!checkNonMappedLeaves(refactoring, refactorings)) {
                return new PurityCheckResult(false, "Non-mapped leaves are not justified - non-mapped inner nodes");
            }

            List<AbstractCodeFragment> nonMappedInnerNodesT2 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedInnerNodesT2());

            checkForIfCondition(refactoring, nonMappedInnerNodesT2);

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

    private static void checkForExtractMethodOnTop(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

        Set<Replacement> replacementsToRemove = new HashSet<>();

        for (Replacement replacement : replacementsToCheck) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION)) {
                String invokedOperationAfterName = ((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getName();
                for (Refactoring refactoring1 : refactorings) {
                    if (refactoring1.getRefactoringType().equals(RefactoringType.EXTRACT_OPERATION)) {
//                        TODO
                    }
                }
            }
        }
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

    private static void checkForRemoveParameterOnTop(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {


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

    private static boolean checkNonMappedLeaves(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings) {

        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty()) {
            return true;
        }

        List<AbstractCodeFragment> nonMappedLeavesT2 = new ArrayList<>(refactoring.getBodyMapper().getNonMappedLeavesT2());

        checkForRenameRefactoringOnTop_NonMapped(refactoring, refactorings, nonMappedLeavesT2);
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

    private static void checkForExtractClassOnTop(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

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

    private static void checkForMoveAttributeOnTop(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

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

    private static void checkForRenameAttributeOnTop(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

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

        Set<Replacement> replacementsToCheck;

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
        }


        if (replacementsToCheck.isEmpty()) {
            return true;
        }


        allReplacementsAreType(refactoring.getReplacements(), replacementsToCheck);
        checkForParametrizationOrAddParameterOnTop(refactoring, refactorings, replacementsToCheck);
        checkForRemoveParameterOnTop(refactoring, refactorings, replacementsToCheck);
        checkForRenameVariableOnTop(refactoring, refactorings, replacementsToCheck);
        checkForRenameAttributeOnTop(refactoring, refactorings, replacementsToCheck);
        checkForMoveAttributeOnTop(refactoring, refactorings, replacementsToCheck);
        checkForExtractClassOnTop(refactoring, refactorings, replacementsToCheck);


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

    private static void checkForRenameVariableOnTop(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, Set<Replacement> replacementsToCheck) {

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
                            parameterizedAndAddedLocation.add(ind);
                        }
                    }else {
                        if (((AddParameterRefactoring) ref).getOperationBefore().getName().equals(methodName)) {
                            int ind = ((AddParameterRefactoring) ref).getOperationAfter().getParameterNameList().indexOf(((AddParameterRefactoring) ref).getParameter().getName());
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


