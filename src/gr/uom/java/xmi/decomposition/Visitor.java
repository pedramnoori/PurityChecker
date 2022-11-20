package gr.uom.java.xmi.decomposition;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import javax.swing.tree.DefaultMutableTreeNode;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.ArrayType;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionMethodReference;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.LambdaExpression;
import org.eclipse.jdt.core.dom.MarkerAnnotation;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.ParameterizedType;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.PrimitiveType;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.QualifiedType;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SuperMethodReference;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.TypeMethodReference;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.WildcardType;

import gr.uom.java.xmi.VariableDeclarationContainer;

public class Visitor extends ASTVisitor {
	public static final Pattern METHOD_INVOCATION_PATTERN = Pattern.compile("!(\\w|\\.)*@\\w*");
	public static final Pattern METHOD_SIGNATURE_PATTERN = Pattern.compile("(public|protected|private|static|\\s) +[\\w\\<\\>\\[\\]]+\\s+(\\w+) *\\([^\\)]*\\) *(\\{?|[^;])");
	private CompilationUnit cu;
	private String filePath;
	private VariableDeclarationContainer container;
	private List<String> variables = new ArrayList<String>();
	private List<String> types = new ArrayList<String>();
	private Map<String, List<AbstractCall>> methodInvocationMap = new LinkedHashMap<String, List<AbstractCall>>();
	private List<VariableDeclaration> variableDeclarations = new ArrayList<VariableDeclaration>();
	private List<AnonymousClassDeclarationObject> anonymousClassDeclarations = new ArrayList<AnonymousClassDeclarationObject>();
	private List<String> stringLiterals = new ArrayList<String>();
	private List<String> numberLiterals = new ArrayList<String>();
	private List<String> nullLiterals = new ArrayList<String>();
	private List<String> booleanLiterals = new ArrayList<String>();
	private List<String> typeLiterals = new ArrayList<String>();
	private Map<String, List<ObjectCreation>> creationMap = new LinkedHashMap<String, List<ObjectCreation>>();
	private List<String> infixExpressions = new ArrayList<String>();
	private List<String> infixOperators = new ArrayList<String>();
	private List<String> arrayAccesses = new ArrayList<String>();
	private List<String> prefixExpressions = new ArrayList<String>();
	private List<String> postfixExpressions = new ArrayList<String>();
	private List<String> arguments = new ArrayList<String>();
	private List<TernaryOperatorExpression> ternaryOperatorExpressions = new ArrayList<TernaryOperatorExpression>();
	private List<LambdaExpressionObject> lambdas = new ArrayList<LambdaExpressionObject>();
	private DefaultMutableTreeNode root = new DefaultMutableTreeNode();
	private DefaultMutableTreeNode current = root;

	public Visitor(CompilationUnit cu, String filePath, VariableDeclarationContainer container) {
		this.cu = cu;
		this.filePath = filePath;
		this.container = container;
	}

	public boolean visit(ArrayAccess node) {
		arrayAccesses.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getArrayAccesses().add(stringify(node));
		}
		return super.visit(node);
	}

	public boolean visit(PrefixExpression node) {
		prefixExpressions.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getPrefixExpressions().add(stringify(node));
		}
		return super.visit(node);
	}

	public boolean visit(PostfixExpression node) {
		postfixExpressions.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getPostfixExpressions().add(stringify(node));
		}
		return super.visit(node);
	}

	public boolean visit(ConditionalExpression node) {
		TernaryOperatorExpression ternary = new TernaryOperatorExpression(cu, filePath, node, container);
		ternaryOperatorExpressions.add(ternary);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTernaryOperatorExpressions().add(ternary);
		}
		return super.visit(node);
	}

	public boolean visit(InfixExpression node) {
		List extendedOperands = node.extendedOperands();
		String nodeAsString = stringify(node);
		infixExpressions.add(nodeAsString);
		infixOperators.add(node.getOperator().toString());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getInfixExpressions().add(nodeAsString);
			anonymous.getInfixOperators().add(node.getOperator().toString());
		}
		return super.visit(node);
	}

	public boolean visit(ClassInstanceCreation node) {
		List<Expression> arguments = node.arguments();
		for(Expression argument : arguments) {
			processArgument(argument);
		}
		ObjectCreation creation = new ObjectCreation(cu, filePath, node);
		String nodeAsString = stringify(node);
		if(creationMap.containsKey(nodeAsString)) {
			creationMap.get(nodeAsString).add(creation);
		}
		else {
			List<ObjectCreation> list = new ArrayList<ObjectCreation>();
			list.add(creation);
			creationMap.put(nodeAsString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<ObjectCreation>> anonymousCreationMap = anonymous.getCreationMap();
			if(anonymousCreationMap.containsKey(nodeAsString)) {
				anonymousCreationMap.get(nodeAsString).add(creation);
			}
			else {
				List<ObjectCreation> list = new ArrayList<ObjectCreation>();
				list.add(creation);
				anonymousCreationMap.put(nodeAsString, list);
			}
		}
		return super.visit(node);
	}

	public boolean visit(ArrayCreation node) {
		ObjectCreation creation = new ObjectCreation(cu, filePath, node);
		String nodeAsString = stringify(node);
		if(creationMap.containsKey(nodeAsString)) {
			creationMap.get(nodeAsString).add(creation);
		}
		else {
			List<ObjectCreation> list = new ArrayList<ObjectCreation>();
			list.add(creation);
			creationMap.put(nodeAsString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<ObjectCreation>> anonymousCreationMap = anonymous.getCreationMap();
			if(anonymousCreationMap.containsKey(nodeAsString)) {
				anonymousCreationMap.get(nodeAsString).add(creation);
			}
			else {
				List<ObjectCreation> list = new ArrayList<ObjectCreation>();
				list.add(creation);
				anonymousCreationMap.put(nodeAsString, list);
			}
		}
		ArrayInitializer initializer = node.getInitializer();
		if(initializer != null) {
			List<Expression> expressions = initializer.expressions();
			if(expressions.size() > 10) {
				return false;
			}
		}
		return super.visit(node);
	}

	public boolean visit(VariableDeclarationFragment node) {
		if(!(node.getParent() instanceof LambdaExpression)) {
			VariableDeclaration variableDeclaration = new VariableDeclaration(cu, filePath, node, container);
			variableDeclarations.add(variableDeclaration);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariableDeclarations().add(variableDeclaration);
			}
		}
		return super.visit(node);
	}

	public boolean visit(SingleVariableDeclaration node) {
		VariableDeclaration variableDeclaration = new VariableDeclaration(cu, filePath, node, container, node.isVarargs());
		variableDeclarations.add(variableDeclaration);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getVariableDeclarations().add(variableDeclaration);
		}
		return super.visit(node);
	}

	public boolean visit(AnonymousClassDeclaration node) {
		DefaultMutableTreeNode childNode = insertNode(node);
		AnonymousClassDeclarationObject childAnonymous = (AnonymousClassDeclarationObject)childNode.getUserObject();
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject currentAnonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			currentAnonymous.getAnonymousClassDeclarations().add(childAnonymous);
		}
		anonymousClassDeclarations.add(childAnonymous);
		this.current = childNode;
		return super.visit(node);
	}

	public void endVisit(AnonymousClassDeclaration node) {
		DefaultMutableTreeNode parentNode = deleteNode(node);
		removeAnonymousData();
		this.current = parentNode;
	}

	private void removeAnonymousData() {
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			removeLast(this.variables, anonymous.getVariables());
			removeLast(this.types, anonymous.getTypes());
			for(String key : anonymous.getMethodInvocationMap().keySet()) {
				this.methodInvocationMap.remove(key, anonymous.getMethodInvocationMap().get(key));
			}
			for(String key : anonymous.getCreationMap().keySet()) {
				this.creationMap.remove(key, anonymous.getCreationMap().get(key));
			}
			this.variableDeclarations.removeAll(anonymous.getVariableDeclarations());
			removeLast(this.stringLiterals, anonymous.getStringLiterals());
			removeLast(this.nullLiterals, anonymous.getNullLiterals());
			removeLast(this.booleanLiterals, anonymous.getBooleanLiterals());
			removeLast(this.typeLiterals, anonymous.getTypeLiterals());
			removeLast(this.numberLiterals, anonymous.getNumberLiterals());
			removeLast(this.infixExpressions, anonymous.getInfixExpressions());
			removeLast(this.infixOperators, anonymous.getInfixOperators());
			removeLast(this.postfixExpressions, anonymous.getPostfixExpressions());
			removeLast(this.prefixExpressions, anonymous.getPrefixExpressions());
			removeLast(this.arguments, anonymous.getArguments());
			this.ternaryOperatorExpressions.removeAll(anonymous.getTernaryOperatorExpressions());
			this.anonymousClassDeclarations.removeAll(anonymous.getAnonymousClassDeclarations());
			this.lambdas.removeAll(anonymous.getLambdas());
			this.arrayAccesses.removeAll(anonymous.getArrayAccesses());
		}
	}

	private static void removeLast(List<String> parentList, List<String> childList) {
		for(int i=childList.size()-1; i>=0; i--) {
			String element = childList.get(i);
			int lastIndex = parentList.lastIndexOf(element);
			parentList.remove(lastIndex);
		}
	}

	private DefaultMutableTreeNode deleteNode(AnonymousClassDeclaration childAnonymous) {
		Enumeration enumeration = root.postorderEnumeration();
		DefaultMutableTreeNode childNode = findNode(childAnonymous);
		
		DefaultMutableTreeNode parentNode = root;
		while(enumeration.hasMoreElements()) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)enumeration.nextElement();
			AnonymousClassDeclarationObject currentAnonymous = (AnonymousClassDeclarationObject)currentNode.getUserObject();
			if(currentAnonymous != null && isParent(childAnonymous, currentAnonymous.getAstNode())) {
				parentNode = currentNode;
				break;
			}
		}
		parentNode.remove(childNode);
		AnonymousClassDeclarationObject childAnonymousObject = (AnonymousClassDeclarationObject)childNode.getUserObject();
		childAnonymousObject.setAstNode(null);
		return parentNode;
	}

	private DefaultMutableTreeNode insertNode(AnonymousClassDeclaration childAnonymous) {
		Enumeration enumeration = root.postorderEnumeration();
		AnonymousClassDeclarationObject anonymousObject = new AnonymousClassDeclarationObject(cu, filePath, childAnonymous);
		DefaultMutableTreeNode childNode = new DefaultMutableTreeNode(anonymousObject);
		
		DefaultMutableTreeNode parentNode = root;
		while(enumeration.hasMoreElements()) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)enumeration.nextElement();
			AnonymousClassDeclarationObject currentAnonymous = (AnonymousClassDeclarationObject)currentNode.getUserObject();
			if(currentAnonymous != null && isParent(childAnonymous, currentAnonymous.getAstNode())) {
				parentNode = currentNode;
				break;
			}
		}
		parentNode.add(childNode);
		return childNode;
	}

	private DefaultMutableTreeNode findNode(AnonymousClassDeclaration anonymous) {
		Enumeration enumeration = root.postorderEnumeration();
		
		while(enumeration.hasMoreElements()) {
			DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)enumeration.nextElement();
			AnonymousClassDeclarationObject currentAnonymous = (AnonymousClassDeclarationObject)currentNode.getUserObject();
			if(currentAnonymous != null && currentAnonymous.getAstNode().equals(anonymous)) {
				return currentNode;
			}
		}
		return null;
	}

	private boolean isParent(ASTNode child, ASTNode parent) {
		ASTNode current = child;
		while(current.getParent() != null) {
			if(current.getParent().equals(parent))
				return true;
			current = current.getParent();
		}
		return false;
	}

	public boolean visit(StringLiteral node) {
		stringLiterals.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getStringLiterals().add(stringify(node));
		}
		return super.visit(node);
	}

	public boolean visit(NumberLiteral node) {
		numberLiterals.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getNumberLiterals().add(stringify(node));
		}
		return super.visit(node);
	}

	public boolean visit(NullLiteral node) {
		nullLiterals.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getNullLiterals().add(stringify(node));
		}
		return super.visit(node);
	}

	public boolean visit(BooleanLiteral node) {
		booleanLiterals.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getBooleanLiterals().add(stringify(node));
		}
		return super.visit(node);
	}

	public boolean visit(TypeLiteral node) {
		typeLiterals.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypeLiterals().add(stringify(node));
		}
		return super.visit(node);
	}

	public boolean visit(ThisExpression node) {
		if(!(node.getParent() instanceof FieldAccess)) {
			variables.add(stringify(node));
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(stringify(node));
			}
		}
		return super.visit(node);
	}

	public boolean visit(SimpleName node) {
		if(node.getParent() instanceof FieldAccess && ((FieldAccess)node.getParent()).getExpression() instanceof ThisExpression) {
			FieldAccess fieldAccess = (FieldAccess)node.getParent();
			variables.add(stringify(fieldAccess));
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(stringify(fieldAccess));
			}
		}
		else if(node.getParent() instanceof MethodInvocation &&
				((MethodInvocation)node.getParent()).getName().equals(node)) {
			// skip method invocation names
		}
		else if(node.getParent() instanceof SuperMethodInvocation &&
				((SuperMethodInvocation)node.getParent()).getName().equals(node)) {
			// skip super method invocation names
		}
		else if(node.getParent() instanceof Type) {
			// skip type names
		}
		else if(node.getParent() instanceof MarkerAnnotation &&
				((MarkerAnnotation)node.getParent()).getTypeName().equals(node)) {
			// skip marker annotation names
		}
		else if(node.getParent() instanceof MethodDeclaration &&
				((MethodDeclaration)node.getParent()).getName().equals(node)) {
			// skip method declaration names
		}
		else if(node.getParent() instanceof SingleVariableDeclaration &&
				node.getParent().getParent() instanceof MethodDeclaration) {
			// skip method parameter names
		}
		else if(node.getParent() instanceof SingleVariableDeclaration &&
				node.getParent().getParent() instanceof CatchClause) {
			// skip catch clause formal parameter names
		}
		else if(node.getParent() instanceof QualifiedName &&
				(node.getParent().getParent() instanceof QualifiedName ||
				node.getParent().getParent() instanceof MethodInvocation ||
				node.getParent().getParent() instanceof SuperMethodInvocation ||
				node.getParent().getParent() instanceof ClassInstanceCreation)) {
			// skip names being part of qualified names
		}
		else {
			variables.add(node.getIdentifier());
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(node.getIdentifier());
			}
		}
		return super.visit(node);
	}
	
	public boolean visit(ArrayType node) {
		types.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(stringify(node));
		}
		return false;
	}
	
	public boolean visit(ParameterizedType node) {
		types.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(stringify(node));
		}
		return false;
	}
	
	public boolean visit(WildcardType node) {
		types.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(stringify(node));
		}
		return false;
	}
	
	public boolean visit(QualifiedType node) {
		types.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(stringify(node));
		}
		return false;
	}
	
	public boolean visit(PrimitiveType node) {
		types.add(stringify(node));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(stringify(node));
		}
		return false;
	}
	
	public boolean visit(SimpleType node) {
		Name name = node.getName();
		types.add(name.getFullyQualifiedName());
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getTypes().add(name.getFullyQualifiedName());
		}
		return false;
	}
	
	public boolean visit(MethodInvocation node) {
		List<Expression> arguments = node.arguments();
		for(Expression argument : arguments) {
			processArgument(argument);
		}
		String methodInvocation = null;
		if(METHOD_INVOCATION_PATTERN.matcher(stringify(node)).matches()) {
			methodInvocation = processMethodInvocation(node);
		}
		else {
			methodInvocation = stringify(node);
		}
		OperationInvocation invocation = new OperationInvocation(cu, filePath, node);
		if(methodInvocationMap.containsKey(methodInvocation)) {
			methodInvocationMap.get(methodInvocation).add(invocation);
		}
		else {
			List<AbstractCall> list = new ArrayList<AbstractCall>();
			list.add(invocation);
			methodInvocationMap.put(methodInvocation, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<AbstractCall>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(methodInvocation)) {
				anonymousMethodInvocationMap.get(methodInvocation).add(invocation);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(invocation);
				anonymousMethodInvocationMap.put(methodInvocation, list);
			}
		}
		return super.visit(node);
	}

	public boolean visit(ExpressionMethodReference node) {
		MethodReference reference = new MethodReference(cu, filePath, node);
		String referenceString = stringify(node);
		if(methodInvocationMap.containsKey(referenceString)) {
			methodInvocationMap.get(referenceString).add(reference);
		}
		else {
			List<AbstractCall> list = new ArrayList<AbstractCall>();
			list.add(reference);
			methodInvocationMap.put(referenceString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<AbstractCall>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(referenceString)) {
				anonymousMethodInvocationMap.get(referenceString).add(reference);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(reference);
				anonymousMethodInvocationMap.put(referenceString, list);
			}
		}
		return super.visit(node);
	}
	
	public boolean visit(SuperMethodReference node) {
		MethodReference reference = new MethodReference(cu, filePath, node);
		String referenceString = stringify(node);
		if(methodInvocationMap.containsKey(referenceString)) {
			methodInvocationMap.get(referenceString).add(reference);
		}
		else {
			List<AbstractCall> list = new ArrayList<AbstractCall>();
			list.add(reference);
			methodInvocationMap.put(referenceString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<AbstractCall>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(referenceString)) {
				anonymousMethodInvocationMap.get(referenceString).add(reference);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(reference);
				anonymousMethodInvocationMap.put(referenceString, list);
			}
		}
		return super.visit(node);
	}
	
	public boolean visit(TypeMethodReference node) {
		MethodReference reference = new MethodReference(cu, filePath, node);
		String referenceString = stringify(node);
		if(methodInvocationMap.containsKey(referenceString)) {
			methodInvocationMap.get(referenceString).add(reference);
		}
		else {
			List<AbstractCall> list = new ArrayList<AbstractCall>();
			list.add(reference);
			methodInvocationMap.put(referenceString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<AbstractCall>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(referenceString)) {
				anonymousMethodInvocationMap.get(referenceString).add(reference);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(reference);
				anonymousMethodInvocationMap.put(referenceString, list);
			}
		}
		return super.visit(node);
	}

	public static String processMethodInvocation(MethodInvocation node) {
		StringBuilder sb = new StringBuilder();
		sb.append(node.getName().getIdentifier());
		sb.append("(");
		List<Expression> arguments = node.arguments();
		if(arguments.size() > 0) {
		    for(int i=0; i<arguments.size()-1; i++)
		        sb.append(stringify(arguments.get(i))).append(", ");
		    sb.append(stringify(arguments.get(arguments.size()-1)));
		}
		sb.append(")");
		return sb.toString();
	}
	
	public static String processClassInstanceCreation(ClassInstanceCreation node) {
		StringBuilder sb = new StringBuilder();
		sb.append("new").append(" ");
		sb.append(stringify(node.getType()));
		List<Expression> arguments = node.arguments();
		if(arguments.size() > 0) {
		    for(int i=0; i<arguments.size()-1; i++)
		        sb.append(stringify(arguments.get(i))).append(", ");
		    sb.append(stringify(arguments.get(arguments.size()-1)));
		}
		sb.append(")");
		return sb.toString();
	}
	
	public boolean visit(SuperMethodInvocation node) {
		List<Expression> arguments = node.arguments();
		for(Expression argument : arguments) {
			processArgument(argument);
		}
		OperationInvocation invocation = new OperationInvocation(cu, filePath, node);
		String nodeAsString = stringify(node);
		if(methodInvocationMap.containsKey(nodeAsString)) {
			methodInvocationMap.get(nodeAsString).add(invocation);
		}
		else {
			List<AbstractCall> list = new ArrayList<AbstractCall>();
			list.add(invocation);
			methodInvocationMap.put(nodeAsString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<AbstractCall>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(nodeAsString)) {
				anonymousMethodInvocationMap.get(nodeAsString).add(invocation);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(invocation);
				anonymousMethodInvocationMap.put(nodeAsString, list);
			}
		}
		return super.visit(node);
	}

	public boolean visit(SuperConstructorInvocation node) {
		List<Expression> arguments = node.arguments();
		for(Expression argument : arguments) {
			processArgument(argument);
		}
		OperationInvocation invocation = new OperationInvocation(cu, filePath, node);
		String nodeAsString = stringify(node);
		if(methodInvocationMap.containsKey(nodeAsString)) {
			methodInvocationMap.get(nodeAsString).add(invocation);
		}
		else {
			List<AbstractCall> list = new ArrayList<AbstractCall>();
			list.add(invocation);
			methodInvocationMap.put(nodeAsString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<AbstractCall>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(nodeAsString)) {
				anonymousMethodInvocationMap.get(nodeAsString).add(invocation);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(invocation);
				anonymousMethodInvocationMap.put(nodeAsString, list);
			}
		}
		return super.visit(node);
	}

	public boolean visit(ConstructorInvocation node) {
		List<Expression> arguments = node.arguments();
		for(Expression argument : arguments) {
			processArgument(argument);
		}
		OperationInvocation invocation = new OperationInvocation(cu, filePath, node);
		String nodeAsString = stringify(node);
		if(methodInvocationMap.containsKey(nodeAsString)) {
			methodInvocationMap.get(nodeAsString).add(invocation);
		}
		else {
			List<AbstractCall> list = new ArrayList<AbstractCall>();
			list.add(invocation);
			methodInvocationMap.put(nodeAsString, list);
		}
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			Map<String, List<AbstractCall>> anonymousMethodInvocationMap = anonymous.getMethodInvocationMap();
			if(anonymousMethodInvocationMap.containsKey(nodeAsString)) {
				anonymousMethodInvocationMap.get(nodeAsString).add(invocation);
			}
			else {
				List<AbstractCall> list = new ArrayList<AbstractCall>();
				list.add(invocation);
				anonymousMethodInvocationMap.put(nodeAsString, list);
			}
		}
		return super.visit(node);
	}

	private void processArgument(Expression argument) {
		if(argument instanceof SuperMethodInvocation ||
				argument instanceof Name ||
				argument instanceof StringLiteral ||
				argument instanceof BooleanLiteral ||
				argument instanceof NumberLiteral ||
				(argument instanceof FieldAccess && ((FieldAccess)argument).getExpression() instanceof ThisExpression) ||
				(argument instanceof ArrayAccess && invalidArrayAccess((ArrayAccess)argument)) ||
				(argument instanceof InfixExpression && invalidInfix((InfixExpression)argument)) ||
				castExpressionInParenthesizedExpression(argument))
			return;
		if(argument instanceof ExpressionMethodReference) {
			LambdaExpressionObject lambda = new LambdaExpressionObject(cu, filePath, (ExpressionMethodReference)argument, container);
			lambdas.add(lambda);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getLambdas().add(lambda);
			}
		}
		else if(argument instanceof SuperMethodReference) {
			LambdaExpressionObject lambda = new LambdaExpressionObject(cu, filePath, (SuperMethodReference)argument, container);
			lambdas.add(lambda);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getLambdas().add(lambda);
			}
		}
		else if(argument instanceof TypeMethodReference) {
			LambdaExpressionObject lambda = new LambdaExpressionObject(cu, filePath, (TypeMethodReference)argument, container);
			lambdas.add(lambda);
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getLambdas().add(lambda);
			}
		}
		this.arguments.add(stringify(argument));
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getArguments().add(stringify(argument));
		}
	}

	private boolean castExpressionInParenthesizedExpression(Expression argument) {
		if(argument instanceof ParenthesizedExpression) {
			Expression parenthesizedExpression = ((ParenthesizedExpression)argument).getExpression();
			if(parenthesizedExpression instanceof CastExpression) {
				CastExpression castExpression = (CastExpression)parenthesizedExpression;
				if(castExpression.getExpression() instanceof SimpleName) {
					return true;
				}
			}
		}
		return false;
	}

	public boolean visit(QualifiedName node) {
		Name qualifier = node.getQualifier();
		if(Character.isUpperCase(qualifier.getFullyQualifiedName().charAt(0))) {
			types.add(qualifier.getFullyQualifiedName());
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getTypes().add(qualifier.getFullyQualifiedName());
			}
			variables.add(stringify(node));
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(stringify(node));
			}
		}
		else if(qualifier instanceof SimpleName && !(node.getParent() instanceof QualifiedName)) {
			String qualifierIdentifier = ((SimpleName)qualifier).getIdentifier();
			MethodDeclaration parentMethodDeclaration = findParentMethodDeclaration(node);
			if(parentMethodDeclaration != null) {
				boolean qualifierIsParameter = false;
				List<SingleVariableDeclaration> parameters = parentMethodDeclaration.parameters();
				for(SingleVariableDeclaration parameter : parameters) {
					if(parameter.getName().getIdentifier().equals(qualifierIdentifier)) {
						qualifierIsParameter = true;
						break;
					}
				}
				if(qualifierIsParameter) {
					variables.add(stringify(node));
					if(current.getUserObject() != null) {
						AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
						anonymous.getVariables().add(stringify(node));
					}
				}
			}
			EnhancedForStatement enhancedFor = findParentEnhancedForStatement(node);
			if(enhancedFor != null) {
				if(enhancedFor.getParameter().getName().getIdentifier().equals(qualifierIdentifier)) {
					variables.add(stringify(node));
					if(current.getUserObject() != null) {
						AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
						anonymous.getVariables().add(stringify(node));
					}
				}
			}
		}
		return super.visit(node);
	}

	private EnhancedForStatement findParentEnhancedForStatement(ASTNode node) {
		ASTNode parent = node.getParent();
		while(parent != null) {
			if(parent instanceof EnhancedForStatement) {
				return (EnhancedForStatement)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	private MethodDeclaration findParentMethodDeclaration(ASTNode node) {
		ASTNode parent = node.getParent();
		while(parent != null) {
			if(parent instanceof MethodDeclaration) {
				return (MethodDeclaration)parent;
			}
			parent = parent.getParent();
		}
		return null;
	}

	public boolean visit(CastExpression node) {
		Expression castExpression = node.getExpression();
		if(castExpression instanceof SimpleName) {
			variables.add(stringify(node));
			if(current.getUserObject() != null) {
				AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
				anonymous.getVariables().add(stringify(node));
			}
		}
		return super.visit(node);
	}

	public boolean visit(LambdaExpression node) {
		LambdaExpressionObject lambda = new LambdaExpressionObject(cu, filePath, node, container);
		lambdas.add(lambda);
		if(current.getUserObject() != null) {
			AnonymousClassDeclarationObject anonymous = (AnonymousClassDeclarationObject)current.getUserObject();
			anonymous.getLambdas().add(lambda);
		}
		return false;
	}

	public Map<String, List<AbstractCall>> getMethodInvocationMap() {
		return this.methodInvocationMap;
	}

	public List<VariableDeclaration> getVariableDeclarations() {
		return variableDeclarations;
	}

	public List<String> getTypes() {
		return types;
	}

	public List<AnonymousClassDeclarationObject> getAnonymousClassDeclarations() {
		return anonymousClassDeclarations;
	}

	public List<String> getStringLiterals() {
		return stringLiterals;
	}

	public List<String> getNumberLiterals() {
		return numberLiterals;
	}

	public List<String> getNullLiterals() {
		return nullLiterals;
	}

	public List<String> getBooleanLiterals() {
		return booleanLiterals;
	}

	public List<String> getTypeLiterals() {
		return typeLiterals;
	}

	public Map<String, List<ObjectCreation>> getCreationMap() {
		return creationMap;
	}

	public List<String> getInfixExpressions() {
		return infixExpressions;
	}

	public List<String> getInfixOperators() {
		return infixOperators;
	}

	public List<String> getArrayAccesses() {
		return arrayAccesses;
	}

	public List<String> getPrefixExpressions() {
		return prefixExpressions;
	}

	public List<String> getPostfixExpressions() {
		return postfixExpressions;
	}

	public List<String> getArguments() {
		return this.arguments;
	}

	public List<TernaryOperatorExpression> getTernaryOperatorExpressions() {
		return ternaryOperatorExpressions;
	}

	public List<String> getVariables() {
		return variables;
	}

	public List<LambdaExpressionObject> getLambdas() {
		return lambdas;
	}

	private static boolean invalidArrayAccess(ArrayAccess e) {
		return e.getArray() instanceof SimpleName && simpleNameOrNumberLiteral(e.getIndex());
	}

	private static boolean invalidInfix(InfixExpression e) {
		return simpleNameOrNumberLiteral(e.getLeftOperand()) && simpleNameOrNumberLiteral(e.getRightOperand());
	}

	private static boolean simpleNameOrNumberLiteral(Expression e) {
		return e instanceof SimpleName || e instanceof NumberLiteral;
	}

	public static String stringify(ASTNode node) {
		ASTFlattener printer = new ASTFlattener();
        node.accept(printer);
        return printer.getResult();
	}
}
