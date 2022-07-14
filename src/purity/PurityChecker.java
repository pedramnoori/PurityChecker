package purity;

import gr.uom.java.xmi.LocationInfo;
import gr.uom.java.xmi.UMLOperation;
import gr.uom.java.xmi.decomposition.AbstractCall;
import gr.uom.java.xmi.decomposition.AbstractCodeFragment;
import gr.uom.java.xmi.decomposition.AbstractCodeMapping;
import gr.uom.java.xmi.decomposition.UMLOperationBodyMapper;
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

        System.out.println("Here honey");

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

            System.out.println("Hoy!");

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

        if (refactoring.getBodyMapper().getNonMappedLeavesT2().isEmpty()) {

//            only renames
            if (refactoring.getBodyMapper().allMappingsAreExactMatches())
                return new PurityCheckResult(true, "All the mappings are matched! - all mapped");


            if (allReplacementsAreVariableNameOrType(refactoring.getReplacements(), 0))
                return new PurityCheckResult(true, "All replacements are variable names or variable type! - all mapped");


            if (refactoring.getReplacements().isEmpty())
                return new PurityCheckResult(true, "There is no replacement! - all mapped");

//            Handling SECOND issue

            if (checkForRenameRefactoringOnTopMapped(refactoring, refactorings)) {
                return new PurityCheckResult(true, "Rename Refactoring on the top of the extracted method - all mapped");
            }

            if (checkForParametrizationOnTop(refactoring, refactorings)) {
                return new PurityCheckResult(true, "Parametrization on top of the extract method - all mapped");
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
            if ((refactoring.getBodyMapper().allMappingsAreExactMatches() || allReplacementsAreVariableNameOrType(refactoring.getReplacements(), 0)) && addOneReturnExpression(refactoring.getBodyMapper().getNonMappedLeavesT2(), 0))
                return new PurityCheckResult(true, "Adding return statement and variable name changed - with non-mapped leaves");

//            Handling FIRST issue
            else if ((refactoring.getBodyMapper().allMappingsAreExactMatches() || allReplacementsAreVariableNameOrType(refactoring.getReplacements(), 0)) && AlTest(refactoring, refactorings, 2)) {
                return new PurityCheckResult(true, "Rename Refactoring on the top of the extracted method - with non-mapped leaves");
            }

            else
                return new PurityCheckResult(false, "Violating extract method refactoring mechanics - with non-mapped leaves");
        }

        return new PurityCheckResult(true, "Adding return statement or rename variable - END");
    }

    private static boolean checkForParametrizationOnTop(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings) {

        List<MethodInvocationReplacement> methodInvocationReplacements = getSpecificReplacementType(refactoring.getReplacements(), MethodInvocationReplacement.class);
        for (Replacement replacement: refactoring.getReplacements()) {
            if (replacement.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_ARGUMENT)) {

                ArrayList<String> temp1 = new ArrayList<>(((MethodInvocationReplacement) replacement).getInvokedOperationAfter().getArguments());
                ArrayList<String> temp2 = new ArrayList<>(temp1);
                temp1.removeAll(((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getArguments());
                Map<String, Integer> addedArgumentsMap = new HashMap<>();

                for (int i = 0; i < temp1.size(); i++) {
                    for (int j = 0; j < temp2.size(); j++) {
                        if (temp1.get(i).equals(temp2.get(j)))
                            addedArgumentsMap.put(temp1.get(i), j);
                    }
                }

                String methodName = ((MethodInvocationReplacement) replacement).getInvokedOperationBefore().getName();
                List<RenameVariableRefactoring> renameVariableRefactoringList = getSpecificTypeRefactoring(refactorings,RenameVariableRefactoring.class);
                List<RenameVariableRefactoring> parametrizeVariableRefactoringList = new ArrayList<>();

                for (RenameVariableRefactoring ref : renameVariableRefactoringList) {
                    if (ref.getRefactoringType().equals(RefactoringType.PARAMETERIZE_VARIABLE)) {
                        parametrizeVariableRefactoringList.add(ref);
                    }
                }

                Map <String, Integer> parameterInitializerMap = new HashMap<>();

                for (RenameVariableRefactoring ref : parametrizeVariableRefactoringList) {
                    if (ref.getOperationBefore().getName().equals(methodName)) {

                        int ind = ref.getOperationAfter().getParameterNameList().indexOf(ref.getRenamedVariable().getVariableName());
                        parameterInitializerMap.put(ref.getOriginalVariable().getInitializer().getExpression(), ind);
                        }
                    }
                if (!addedArgumentsMap.equals(parameterInitializerMap)) {
                    return false;
                    }
                }
            }

        return true;
        }

    private static boolean checkForRenameRefactoringOnTopMapped(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings) {


        int numberOfReplacements = refactoring.getReplacements().size();
        int numberOfMethodInvocationNameReplacement = 0;

        for (Replacement rep: refactoring.getReplacements()) {
            if (rep.getType().equals(Replacement.ReplacementType.METHOD_INVOCATION_NAME)) {
                numberOfMethodInvocationNameReplacement++;
            }
        }


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
        }

        if (numberOfReplacements == 0) {
            return true;
        }

        if (allReplacementsAreVariableNameOrType(refactoring.getReplacements(), numberOfMethodInvocationNameReplacement)) {
            return true;
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


    private static boolean AlTest(ExtractOperationRefactoring refactoring, List<Refactoring> refactorings, float temp) {
        List<RenameOperationRefactoring> renameOperationRefactoringList = getSpecificTypeRefactoring(refactorings,RenameOperationRefactoring.class);
        int nonMappedT2 = refactoring.getBodyMapper().getNonMappedLeavesT2().size();
        for(AbstractCodeFragment abstractCodeFragment2 : refactoring.getBodyMapper().getNonMappedLeavesT2()) {
            Map<String, List<AbstractCall>> methodInvocationMap2 = abstractCodeFragment2.getMethodInvocationMap();
            List<String> methodCalls2 = methodInvocationMap2.values().stream().map(l -> l.get(0).getName()).collect(Collectors.toList());
            for (AbstractCodeFragment abstractCodeFragment : refactoring.getBodyMapper().getNonMappedLeavesT1()) {
                Map<String, List<AbstractCall>> methodInvocationMap = abstractCodeFragment.getMethodInvocationMap();
                List<String> methodCalls = methodInvocationMap.values().stream().map(l -> l.get(0).getName()).collect(Collectors.toList());
                boolean check = checkRenameMethodCallsPossibility(methodCalls, methodCalls2, renameOperationRefactoringList);
                if (check) {
                    nonMappedT2 -= 1;
                    break;
                }
            }
        }
        if (nonMappedT2 == 0) {
            return true;
        }
        if (nonMappedT2 == 1 &&  addOneReturnExpression(refactoring.getBodyMapper().getNonMappedLeavesT2(), 1))
            return true;

        return false;
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
//        return true;
        return false;
    }

    private static boolean allReplacementsAreVariableNameOrType(Set<Replacement> replacements, int standToSomeExtent) {

        int counter = 0;

        for (Replacement rep: replacements) {
            if (rep.getType().equals(Replacement.ReplacementType.VARIABLE_NAME) ||
                    rep.getType().equals(Replacement.ReplacementType.TYPE)) {

            }else {
                counter += 1;
            }
        }

        if (counter <= standToSomeExtent) {
            return true;
        }
        return false;

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


