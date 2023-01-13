package gr.uom.java.xmi.decomposition;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.util.PrefixSuffixUtils;

import gr.uom.java.xmi.LocationInfo.CodeElementType;
import gr.uom.java.xmi.VariableDeclarationContainer;
import gr.uom.java.xmi.decomposition.replacement.*;
import gr.uom.java.xmi.decomposition.replacement.Replacement.ReplacementType;
import gr.uom.java.xmi.diff.ExtractVariableRefactoring;
import gr.uom.java.xmi.diff.InlineVariableRefactoring;
import gr.uom.java.xmi.diff.RenameOperationRefactoring;
import gr.uom.java.xmi.diff.UMLAbstractClassDiff;
import org.refactoringminer.api.Refactoring;
import org.refactoringminer.util.PrefixSuffixUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class AbstractCodeMapping {

	private AbstractCodeFragment fragment1;
	private AbstractCodeFragment fragment2;
	private VariableDeclarationContainer operation1;
	private VariableDeclarationContainer operation2;
	private Set<Replacement> replacements;
	private boolean identicalWithExtractedVariable;
	private boolean identicalWithInlinedVariable;
	private Set<Refactoring> refactorings = new LinkedHashSet<Refactoring>();
	
	public AbstractCodeMapping(AbstractCodeFragment fragment1, AbstractCodeFragment fragment2,
			VariableDeclarationContainer operation1, VariableDeclarationContainer operation2) {
		this.fragment1 = fragment1;
		this.fragment2 = fragment2;
		this.operation1 = operation1;
		this.operation2 = operation2;
		this.replacements = new LinkedHashSet<Replacement>();
	}

	public abstract double editDistance();

	public boolean equalContainer() {
		return operation1.equals(operation2);
	}

	public AbstractCodeFragment getFragment1() {
		return fragment1;
	}

	public AbstractCodeFragment getFragment2() {
		return fragment2;
	}

	public VariableDeclarationContainer getOperation1() {
		return operation1;
	}

	public VariableDeclarationContainer getOperation2() {
		return operation2;
	}

	public boolean isIdenticalWithExtractedVariable() {
		return identicalWithExtractedVariable;
	}

	public boolean isIdenticalWithInlinedVariable() {
		return identicalWithInlinedVariable;
	}

	public void addRefactoring(Refactoring r) {
		refactorings.add(r);
	}

	public Set<Refactoring> getRefactorings() {
		return refactorings;
	}

	public boolean isExact() {
		return (fragment1.getArgumentizedString().equals(fragment2.getArgumentizedString()) || argumentizedStringExactAfterTypeReplacement() ||
				fragment1.getString().equals(fragment2.getString()) || isExactAfterAbstraction() || containsIdenticalOrCompositeReplacement()) && !fragment1.isKeyword();
	}

	public boolean isPurelyExact(Map<String, String> parameterToArgumentMap) {
		fragment2.argumentizationAfterRefactorings(parameterToArgumentMap);
		return (fragment1.getArgumentizedString().equals(fragment2.getArgumentizedString()) || argumentizedStringExactAfterTypeReplacement() ||
				fragment1.getString().equals(fragment2.getString()) || isExactAfterAbstraction() || fragment1.getString().equals(fragment2.getArgumentizedAfterRefactorings()));
	}

	public boolean checkForSupplierPattern(Replacement replacement, Map<String, String> parameterToArgumentMap) {
		fragment2.argumentizationAfterRefactorings(parameterToArgumentMap);
		fragment1.argumentizationAfterRefactorings(parameterToArgumentMap);

		String s1 = fragment1.getArgumentizedAfterRefactorings();
		String s2 = fragment2.getArgumentizedAfterRefactorings();

		s1 = s1.replaceAll(";", "");
		s2 = s2.replaceAll(";", "");

		s1 = s1.replaceAll("\n", "");
		s2 = s2.replaceAll("\n", "");

		int equalSign1 = s1.indexOf("=");
		int equalSign2 = s2.indexOf("=");

		if (equalSign1 != -1 && equalSign2 != -1) {

			String s1WithoutDeclaration = s1.substring(equalSign1 + 1);
			String s2WithoutDeclaration = s2.substring(equalSign2 + 1);

			int find = s2WithoutDeclaration.indexOf(s1WithoutDeclaration);

			if (find != -1) {
				s1WithoutDeclaration = s1WithoutDeclaration.replaceAll("\\)", "\\\\)");
				s1WithoutDeclaration = s1WithoutDeclaration.replaceAll("\\(", "\\\\(");

				List<String> finded = List.of(s2WithoutDeclaration.split(s1WithoutDeclaration));

				for (String s3 : finded) {
					if (s3.contains("->")) {
						for (String s4 : finded) {
							if (s4.contains(".get")){
								return true;
							}
						}
					}
				}
			}

		}
		return false;
	}

	private boolean argumentizedStringExactAfterTypeReplacement() {
		String s1 = fragment1.getArgumentizedString();
		String s2 = fragment2.getArgumentizedString();
		for(Replacement r : replacements) {
			if(r.getType().equals(ReplacementType.TYPE)) {
				if(s1.startsWith(r.getBefore()) && s2.startsWith(r.getAfter())) {
					String temp = s2.replace(r.getAfter(), r.getBefore());
					if(s1.equals(temp) || (s1 + ";\n").equals(temp)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private boolean isExactAfterAbstraction() {
		AbstractCall invocation1 = fragment1.invocationCoveringEntireFragment();
		AbstractCall invocation2 = fragment2.invocationCoveringEntireFragment();
		if(invocation1 != null && invocation2 != null) {
			return invocation1.actualString().equals(invocation2.actualString());
		}
		ObjectCreation creation1 = fragment1.creationCoveringEntireFragment();
		ObjectCreation creation2 = fragment2.creationCoveringEntireFragment();
		if(creation1 != null && creation2 != null) {
			return creation1.actualString().equals(creation2.actualString());
		}
		return false;
	}

	private boolean containsIdenticalOrCompositeReplacement() {
		for(Replacement r : replacements) {
			if(r.getType().equals(ReplacementType.ARRAY_INITIALIZER_REPLACED_WITH_METHOD_INVOCATION_ARGUMENTS) &&
					r.getBefore().equals(r.getAfter())) {
				return true;
			}
			else if(r.getType().equals(ReplacementType.COMPOSITE)) {
				return true;
			}
		}
		return false;
	}

	public CompositeReplacement containsCompositeReplacement() {
		for(Replacement r : replacements) {
			if(r.getType().equals(ReplacementType.COMPOSITE)) {
				return (CompositeReplacement)r;
			}
		}
		return null;
	}

	public void addReplacement(Replacement replacement) {
		this.replacements.add(replacement);
	}

	public void addReplacements(Set<Replacement> replacements) {
		if(replacements != null) {
			this.replacements.addAll(replacements);
		}
	}

	public Set<Replacement> getReplacements() {
		return replacements;
	}

	public boolean containsReplacement(ReplacementType type) {
		for(Replacement replacement : replacements) {
			if(replacement.getType().equals(type)) {
				return true;
			}
		}
		return false;
	}

	public boolean containsOnlyReplacement(ReplacementType type) {
		for(Replacement replacement : replacements) {
			if(!replacement.getType().equals(type)) {
				return false;
			}
		}
		return replacements.size() > 0;
	}

	public Set<ReplacementType> getReplacementTypes() {
		Set<ReplacementType> types = new LinkedHashSet<ReplacementType>();
		for(Replacement replacement : replacements) {
			types.add(replacement.getType());
		}
		return types;
	}

	public String toString() {
		return fragment1.toString() + fragment2.toString();
	}

	public void temporaryVariableAssignment(Set<Refactoring> refactorings, boolean insideExtractedOrInlinedMethod) {
		if(this instanceof LeafMapping && getFragment1() instanceof AbstractExpression
				&& getFragment2() instanceof StatementObject) {
			StatementObject statement = (StatementObject) getFragment2();
			List<VariableDeclaration> variableDeclarations = statement.getVariableDeclarations();
			boolean validReplacements = true;
			for(Replacement replacement : getReplacements()) {
				if(replacement instanceof MethodInvocationReplacement || replacement instanceof ObjectCreationReplacement) {
					validReplacements = false;
					break;
				}
			}
			if(variableDeclarations.size() == 1 && validReplacements) {
				VariableDeclaration variableDeclaration = variableDeclarations.get(0);
				ExtractVariableRefactoring ref = new ExtractVariableRefactoring(variableDeclaration, operation1, operation2, insideExtractedOrInlinedMethod);
				processExtractVariableRefactoring(ref, refactorings);
				identicalWithExtractedVariable = true;
			}
		}
	}

	public void temporaryVariableAssignment(AbstractCodeFragment statement,
			List<? extends AbstractCodeFragment> nonMappedLeavesT2, UMLAbstractClassDiff classDiff, boolean insideExtractedOrInlinedMethod) {
		for(VariableDeclaration declaration : statement.getVariableDeclarations()) {
			String variableName = declaration.getVariableName();
			AbstractExpression initializer = declaration.getInitializer();
			for(Replacement replacement : getReplacements()) {
				if(replacement.getAfter().startsWith(variableName + ".")) {
					String suffixAfter = replacement.getAfter().substring(variableName.length(), replacement.getAfter().length());
					if(replacement.getBefore().endsWith(suffixAfter)) {
						String prefixBefore = replacement.getBefore().substring(0, replacement.getBefore().indexOf(suffixAfter));
						if(initializer != null) {
							if(initializer.toString().equals(prefixBefore) ||
									overlappingExtractVariable(initializer, prefixBefore, nonMappedLeavesT2, refactorings)) {
								ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
								processExtractVariableRefactoring(ref, refactorings);
								if(identical()) {
									identicalWithExtractedVariable = true;
								}
								return;
							}
						}
					}
				}
				if(variableName.equals(replacement.getAfter()) && initializer != null) {
					if(initializer.toString().equals(replacement.getBefore()) ||
							(initializer.toString().equals("(" + declaration.getType() + ")" + replacement.getBefore()) && !containsVariableNameReplacement(variableName)) ||
							ternaryMatch(initializer, replacement.getBefore()) ||
							infixOperandMatch(initializer, replacement.getBefore()) ||
							wrappedAsArgument(initializer, replacement.getBefore()) ||
							reservedTokenMatch(initializer, replacement, replacement.getBefore()) ||
							overlappingExtractVariable(initializer, replacement.getBefore(), nonMappedLeavesT2, refactorings)) {
						ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
						processExtractVariableRefactoring(ref, refactorings);
						if(identical()) {
							identicalWithExtractedVariable = true;
						}
						return;
					}
				}
			}
			if(classDiff != null && initializer != null) {
				AbstractCall invocation = initializer.invocationCoveringEntireFragment();
				if(invocation != null) {
					for(Refactoring refactoring : classDiff.getRefactoringsBeforePostProcessing()) {
						if(refactoring instanceof RenameOperationRefactoring) {
							RenameOperationRefactoring rename = (RenameOperationRefactoring)refactoring;
							if(invocation.getName().equals(rename.getRenamedOperation().getName())) {
								String initializerBeforeRename = initializer.getString().replace(rename.getRenamedOperation().getName(), rename.getOriginalOperation().getName());
								if(getFragment1().getString().contains(initializerBeforeRename) && getFragment2().getString().contains(variableName)) {
									ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
									processExtractVariableRefactoring(ref, refactorings);
									return;
								}
							}
						}
					}
				}
			}
		}
		String argumentizedString = statement.getArgumentizedString();
		if(argumentizedString.contains("=")) {
			String beforeAssignment = argumentizedString.substring(0, argumentizedString.indexOf("="));
			String[] tokens = beforeAssignment.split("\\s");
			String variable = tokens[tokens.length-1];
			String initializer = null;
			if(argumentizedString.endsWith(";\n")) {
				initializer = argumentizedString.substring(argumentizedString.indexOf("=")+1, argumentizedString.length()-2);
			}
			else {
				initializer = argumentizedString.substring(argumentizedString.indexOf("=")+1, argumentizedString.length());
			}
			for(Replacement replacement : getReplacements()) {
				if(variable.endsWith(replacement.getAfter()) &&	initializer.equals(replacement.getBefore())) {
					List<VariableDeclaration> variableDeclarations = operation2.getVariableDeclarationsInScope(fragment2.getLocationInfo());
					for(VariableDeclaration declaration : variableDeclarations) {
						if(declaration.getVariableName().equals(variable)) {
							ExtractVariableRefactoring ref = new ExtractVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
							processExtractVariableRefactoring(ref, refactorings);
							if(identical()) {
								identicalWithExtractedVariable = true;
							}
							return;
						}
					}
				}
			}
		}
	}

	public void inlinedVariableAssignment(AbstractCodeFragment statement,
			List<? extends AbstractCodeFragment> nonMappedLeavesT2, boolean insideExtractedOrInlinedMethod) {
		for(VariableDeclaration declaration : statement.getVariableDeclarations()) {
			for(Replacement replacement : getReplacements()) {
				String variableName = declaration.getVariableName();
				AbstractExpression initializer = declaration.getInitializer();
				if(replacement.getBefore().startsWith(variableName + ".")) {
					String suffixBefore = replacement.getBefore().substring(variableName.length(), replacement.getBefore().length());
					if(replacement.getAfter().endsWith(suffixBefore)) {
						String prefixAfter = replacement.getAfter().substring(0, replacement.getAfter().indexOf(suffixBefore));
						if(initializer != null) {
							if(initializer.toString().equals(prefixAfter) ||
									overlappingExtractVariable(initializer, prefixAfter, nonMappedLeavesT2, refactorings)) {
								InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
								processInlineVariableRefactoring(ref, refactorings);
								if(identical()) {
									identicalWithInlinedVariable = true;
								}
								return;
							}
						}
					}
				}
				if(variableName.equals(replacement.getBefore()) && initializer != null) {
					if(initializer.toString().equals(replacement.getAfter()) ||
							(initializer.toString().equals("(" + declaration.getType() + ")" + replacement.getAfter()) && !containsVariableNameReplacement(variableName)) ||
							ternaryMatch(initializer, replacement.getAfter()) ||
							infixOperandMatch(initializer, replacement.getAfter()) ||
							wrappedAsArgument(initializer, replacement.getAfter()) ||
							reservedTokenMatch(initializer, replacement, replacement.getAfter()) ||
							overlappingExtractVariable(initializer, replacement.getAfter(), nonMappedLeavesT2, refactorings)) {
						InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
						processInlineVariableRefactoring(ref, refactorings);
						if(identical()) {
							identicalWithInlinedVariable = true;
						}
						return;
					}
				}
			}
		}
		String argumentizedString = statement.getArgumentizedString();
		if(argumentizedString.contains("=")) {
			String beforeAssignment = argumentizedString.substring(0, argumentizedString.indexOf("="));
			String[] tokens = beforeAssignment.split("\\s");
			String variable = tokens[tokens.length-1];
			String initializer = null;
			if(argumentizedString.endsWith(";\n")) {
				initializer = argumentizedString.substring(argumentizedString.indexOf("=")+1, argumentizedString.length()-2);
			}
			else {
				initializer = argumentizedString.substring(argumentizedString.indexOf("=")+1, argumentizedString.length());
			}
			for(Replacement replacement : getReplacements()) {
				if(variable.endsWith(replacement.getBefore()) && initializer.equals(replacement.getAfter())) {
					List<VariableDeclaration> variableDeclarations = operation1.getVariableDeclarationsInScope(fragment1.getLocationInfo());
					for(VariableDeclaration declaration : variableDeclarations) {
						if(declaration.getVariableName().equals(variable)) {
							InlineVariableRefactoring ref = new InlineVariableRefactoring(declaration, operation1, operation2, insideExtractedOrInlinedMethod);
							processInlineVariableRefactoring(ref, refactorings);
							if(identical()) {
								identicalWithInlinedVariable = true;
							}
							return;
						}
					}
				}
			}
		}
	}

	private boolean identical() {
		if(getReplacements().size() == 1 && fragment1.getVariableDeclarations().size() == fragment2.getVariableDeclarations().size()) {
			return true;
		}
		int stringLiteralReplacents = 0;
		for(Replacement r : replacements) {
			if((r.getBefore().startsWith("\"") && r.getBefore().endsWith("\"")) || (r.getAfter().startsWith("\"") && r.getAfter().endsWith("\""))) {
				stringLiteralReplacents++;
			}
		}
		if(stringLiteralReplacents == replacements.size()) {
			return true;
		}
		return false;
	}

	private boolean wrappedAsArgument(AbstractExpression initializer, String replacedExpression) {
		int replacementCount = 0;
		for(Replacement r : replacements) {
			if(r.getBefore().equals(replacedExpression) || r.getAfter().equals(replacedExpression)) {
				replacementCount++;
			}
		}
		if(replacementCount > 1) {
			return false;
		}
		AbstractCall invocation = initializer.invocationCoveringEntireFragment();
		if(invocation != null) {
			if(invocation.arguments().contains(replacedExpression)) {
				return true;
			}
			String expression = invocation.getExpression();
			if(expression != null && (expression.equals(replacedExpression) || ReplacementUtil.contains(expression, replacedExpression))) {
				return true;
			}
		}
		ObjectCreation creation = initializer.creationCoveringEntireFragment();
		if(creation != null) {
			if(creation.arguments().contains(replacedExpression)) {
				return true;
			}
		}
		return false;
	}

	private boolean infixOperandMatch(AbstractExpression initializer, String replacedExpression) {
		List<LeafExpression> infixExpressions = initializer.getInfixExpressions();
		for(LeafExpression infixExpression : infixExpressions) {
			String infix = infixExpression.getString();
			if(infix.startsWith(replacedExpression) || infix.endsWith(replacedExpression)) {
				return true;
			}
		}
		return false;
	}

	private boolean ternaryMatch(AbstractExpression initializer, String replacedExpression) {
		List<TernaryOperatorExpression> ternaryList = initializer.getTernaryOperatorExpressions();
		for(TernaryOperatorExpression ternary : ternaryList) {
			if(ternary.getThenExpression().toString().equals(replacedExpression) || ternary.getElseExpression().toString().equals(replacedExpression)) {
				return true;
			}
		}
		return false;
	}

	private boolean containsVariableNameReplacement(String variableName) {
		for(Replacement replacement : getReplacements()) {
			if(replacement.getType().equals(ReplacementType.VARIABLE_NAME)) {
				if(replacement.getBefore().equals(variableName) || replacement.getAfter().equals(variableName)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean reservedTokenMatch(AbstractExpression initializer, Replacement replacement, String replacedExpression) {
		AbstractCall initializerInvocation = initializer.invocationCoveringEntireFragment();
		AbstractCall replacementInvocation = replacement instanceof VariableReplacementWithMethodInvocation ? ((VariableReplacementWithMethodInvocation)replacement).getInvokedOperation() : null;
		boolean methodInvocationMatch = true;
		if(initializerInvocation != null && replacementInvocation != null) {
			if(!initializerInvocation.getName().equals(replacementInvocation.getName())) {
				methodInvocationMatch = false;
			}
		}
		else if(initializerInvocation != null && replacementInvocation == null) {
			methodInvocationMatch = false;
		}
		else if(initializerInvocation == null && replacementInvocation != null) {
			methodInvocationMatch = false;
		}
		String initializerReservedTokens = ReplacementUtil.keepReservedTokens(initializer.toString());
		String replacementReservedTokens = ReplacementUtil.keepReservedTokens(replacedExpression);
		return methodInvocationMatch && !initializerReservedTokens.isEmpty() && !initializerReservedTokens.equals("[]") && !initializerReservedTokens.equals(".()") && !initializerReservedTokens.equals(" ()") && initializerReservedTokens.equals(replacementReservedTokens);
	}

	private void processInlineVariableRefactoring(InlineVariableRefactoring ref, Set<Refactoring> refactorings) {
		if(!refactorings.contains(ref)) {
			ref.addReference(this);
			refactorings.add(ref);
		}
		else {
			for(Refactoring refactoring : refactorings) {
				if(refactoring.equals(ref)) {
					((InlineVariableRefactoring)refactoring).addReference(this);
					break;
				}
			}
		}
	}

	private void processExtractVariableRefactoring(ExtractVariableRefactoring ref, Set<Refactoring> refactorings) {
		if(!refactorings.contains(ref)) {
			ref.addReference(this);
			refactorings.add(ref);
		}
		else {
			for(Refactoring refactoring : refactorings) {
				if(refactoring.equals(ref)) {
					((ExtractVariableRefactoring)refactoring).addReference(this);
					break;
				}
			}
		}
	}

	private boolean overlappingExtractVariable(AbstractExpression initializer, String input, List<? extends AbstractCodeFragment> nonMappedLeavesT2, Set<Refactoring> refactorings) {
		String output = input;
		for(Refactoring ref : refactorings) {
			if(ref instanceof ExtractVariableRefactoring) {
				ExtractVariableRefactoring extractVariable = (ExtractVariableRefactoring)ref;
				VariableDeclaration declaration = extractVariable.getVariableDeclaration();
				if(declaration.getInitializer() != null && input.contains(declaration.getInitializer().toString())) {
					output = output.replace(declaration.getInitializer().toString(), declaration.getVariableName());
				}
			}
		}
		if(initializer.toString().equals(output)) {
			return true;
		}
		String longestCommonSuffix = PrefixSuffixUtils.longestCommonSuffix(initializer.toString(), input);
		if(!longestCommonSuffix.isEmpty() && longestCommonSuffix.startsWith(".")) {
			String prefix1 = initializer.toString().substring(0, initializer.toString().indexOf(longestCommonSuffix));
			String prefix2 = input.substring(0, input.indexOf(longestCommonSuffix));
			//skip static variable prefixes
			if(prefix1.equals(prefix2) || (!prefix1.toUpperCase().equals(prefix1) && !prefix2.toUpperCase().equals(prefix2))) {
				return true;
			}
		}
		String longestCommonPrefix = PrefixSuffixUtils.longestCommonPrefix(initializer.toString(), input);
		if(!longestCommonSuffix.isEmpty() && !longestCommonPrefix.isEmpty() &&
				!longestCommonPrefix.equals(initializer.toString()) && !longestCommonPrefix.equals(input) &&
				!longestCommonSuffix.equals(initializer.toString()) && !longestCommonSuffix.equals(input) &&
				longestCommonPrefix.length() + longestCommonSuffix.length() < input.length() &&
				longestCommonPrefix.length() + longestCommonSuffix.length() < initializer.toString().length()) {
			String s1 = input.substring(longestCommonPrefix.length(), input.lastIndexOf(longestCommonSuffix));
			String s2 = initializer.toString().substring(longestCommonPrefix.length(), initializer.toString().lastIndexOf(longestCommonSuffix));
			for(AbstractCodeFragment statement : nonMappedLeavesT2) {
				VariableDeclaration variable = statement.getVariableDeclaration(s2);
				if(variable != null) {
					if(variable.getInitializer() != null && variable.getInitializer().toString().equals(s1)) {
						return true;
					}
					List<TernaryOperatorExpression> ternaryOperators = statement.getTernaryOperatorExpressions();
					for(TernaryOperatorExpression ternaryOperator : ternaryOperators) {
						if(ternaryOperator.getThenExpression().toString().equals(s1) ||
								ternaryOperator.getElseExpression().toString().equals(s1)) {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public Set<Replacement> commonReplacements(AbstractCodeMapping other) {
		Set<Replacement> intersection = new LinkedHashSet<Replacement>(this.replacements);
		intersection.retainAll(other.replacements);
		return intersection;
	}

	public Set<Replacement> getReplacementsInvolvingMethodInvocation() {
		Set<Replacement> replacements = new LinkedHashSet<Replacement>();
		for(Replacement replacement : getReplacements()) {
			if(involvesMethodInvocation(replacement)) {
				replacements.add(replacement);
			}
		}
		return replacements;
	}

	public Pair<CompositeStatementObject, CompositeStatementObject> nestedUnderCatchBlock() {
		CompositeStatementObject parent1 = fragment1.getParent();
		CompositeStatementObject parent2 = fragment2.getParent();
		while(parent1 != null && parent2 != null) {
			if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE) &&
					parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.CATCH_CLAUSE)) {
				return Pair.of(parent1, parent2);
			}
			else if(parent1.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK) &&
					parent2.getLocationInfo().getCodeElementType().equals(CodeElementType.FINALLY_BLOCK)) {
				return Pair.of(parent1, parent2);
			}
			parent1 = parent1.getParent();
			parent2 = parent2.getParent();
		}
		return null;
	}

	private static boolean involvesMethodInvocation(Replacement replacement) {
		return replacement instanceof MethodInvocationReplacement ||
				replacement instanceof VariableReplacementWithMethodInvocation ||
				replacement instanceof ClassInstanceCreationWithMethodInvocationReplacement ||
				replacement.getType().equals(ReplacementType.ARGUMENT_REPLACED_WITH_RIGHT_HAND_SIDE_OF_ASSIGNMENT_EXPRESSION) ||
				replacement.getType().equals(ReplacementType.ARGUMENT_REPLACED_WITH_RETURN_EXPRESSION) ||
				replacement.getType().equals(ReplacementType.ARGUMENT_REPLACED_WITH_METHOD_INVOCATION) ||
				replacement instanceof IntersectionReplacement ||
				replacement.getType().equals(ReplacementType.ANONYMOUS_CLASS_DECLARATION);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fragment1 == null) ? 0 : fragment1.hashCode());
		result = prime * result + ((fragment2 == null) ? 0 : fragment2.hashCode());
		result = prime * result + ((operation1 == null) ? 0 : operation1.hashCode());
		result = prime * result + ((operation2 == null) ? 0 : operation2.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractCodeMapping other = (AbstractCodeMapping) obj;
		if (fragment1 == null) {
			if (other.fragment1 != null)
				return false;
		} else if (!fragment1.equals(other.fragment1))
			return false;
		if (fragment2 == null) {
			if (other.fragment2 != null)
				return false;
		} else if (!fragment2.equals(other.fragment2))
			return false;
		if (operation1 == null) {
			if (other.operation1 != null)
				return false;
		} else if (!operation1.equals(other.operation1))
			return false;
		if (operation2 == null) {
			if (other.operation2 != null)
				return false;
		} else if (!operation2.equals(other.operation2))
			return false;
		return true;
	}
}
