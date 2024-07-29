package miniJava.SyntacticAnalyzer;

import miniJava.ErrorReporter;
import miniJava.AbstractSyntaxTrees.*;
import miniJava.AbstractSyntaxTrees.Package;
import miniJava.ContextualAnalysis.Identification;
import miniJava.ContextualAnalysis.TypeChecking;


public class Parser {
	private Scanner _scanner;
	private ErrorReporter _errors;
	private Token _currentToken;

	public Parser( Scanner scanner, ErrorReporter errors ) {
		this._scanner = scanner;
		this._errors = errors;
		this._currentToken = this._scanner.scan();
	}

	class SyntaxError extends Error {
		private static final long serialVersionUID = -6461942006097999362L;
	}

	public AST parse() {
		try {
			// The first thing we need to parse is the Program
			Package prog = parseProgram();

			Identification identification = new Identification(_errors);
			identification.parse(prog);

			TypeChecking typeChecking = new TypeChecking(_errors);
			typeChecking.parse(prog);

			return prog;
		} catch( SyntaxError e ) {}
		return null;
	}

	// Program ::= (ClassDeclaration)* eot
	private Package parseProgram() throws SyntaxError {
		ClassDeclList classList = new ClassDeclList();

		while (_currentToken != null && _currentToken.getTokenType() != TokenType.EOT) {
			classList.add(parseClassDeclaration());
		}

		return new Package(classList, new SourcePosition(0));
	}

	// ClassDeclaration ::= class identifier { (FieldDeclaration|MethodDeclaration)* }
	private ClassDecl parseClassDeclaration() throws SyntaxError {
		accept(TokenType.CLASS);

		ClassDecl Class = new ClassDecl(_currentToken.getTokenText(), new FieldDeclList(), new MethodDeclList(),
				_currentToken.getTokenPosition());

		accept(TokenType.ID);
		accept(TokenType.LBRACE);

		while (!acceptOptional(TokenType.RBRACE)) {
			FieldDecl tempField = null;
			MethodDecl tempMethod = null;

			boolean isPrivate = false;
			TypeDenoter type = null;
			ParameterDeclList paramaters = new ParameterDeclList();
			StatementList statements = new StatementList();

			boolean isMethodDeclaration = false;

			String optionalVisibility = _currentToken.getTokenText();
			if (acceptOptional(TokenType.VISIBILITY) && optionalVisibility.equals("private")) {
				isPrivate = true;
			}

			boolean isStatic = acceptOptional(TokenType.ACCESS);
			String className = null;

			SourcePosition typePos = _currentToken.getTokenPosition();
			if (acceptOptional(TokenType.VOID)) {
				isMethodDeclaration = true;
				type = new BaseType(TypeKind.VOID, typePos);
			} else {
				className = _currentToken.getTokenText();
				type = parseType();
			}

			String id = _currentToken.getTokenText();
			accept(TokenType.ID);

			if (isMethodDeclaration || _currentToken.getTokenType() == TokenType.LPAREN) {
				accept(TokenType.LPAREN);

				if (!acceptOptional(TokenType.RPAREN)) {
					paramaters = parseParameters(Class.name);
					accept(TokenType.RPAREN);
				}

				accept(TokenType.LBRACE);

				statements = parseMethodDeclaration();

				tempField = new FieldDecl(isPrivate, isStatic, type, id, _currentToken.getTokenPosition(), className);
				tempMethod = new MethodDecl(tempField, paramaters, statements, _currentToken.getTokenPosition());

				Class.methodDeclList.add(tempMethod);
			} else {
				tempField = new FieldDecl(isPrivate, isStatic, type, id, _currentToken.getTokenPosition(), className);

				accept(TokenType.SEMICOLON);
				Class.fieldDeclList.add(tempField);
			}
		}

		return Class;
	}

	private StatementList parseMethodDeclaration() throws SyntaxError {
		StatementList statementList = new StatementList();

		while (!acceptOptional(TokenType.RBRACE)) {
			statementList.add(parseStatement());
		}

		return statementList;
	}

	private Statement parseStatement() {
		Token curr = _currentToken;
		SourcePosition pos = _currentToken.getTokenPosition();
		Expression expression = null;

		if (acceptOptional(TokenType.RETURN)) {
			if (_currentToken.getTokenType() != TokenType.SEMICOLON) {
				expression = parseOrOperator();
			}
			accept(TokenType.SEMICOLON);

			return new ReturnStmt(expression, pos);
		}
		else if (acceptOptional(TokenType.WHILE)) {
			Statement whileStmt = null;

			accept(TokenType.LPAREN);
			expression = parseOrOperator();
			accept(TokenType.RPAREN);

			if (!acceptOptional(TokenType.LBRACE)) {
				whileStmt = parseStatement();
			} else {
				StatementList list = new StatementList();

				while (!acceptOptional(TokenType.RBRACE)) {
					list.add(parseStatement());
				}

				whileStmt = new BlockStmt(list, pos);
			}

			return new WhileStmt(expression, whileStmt, pos);
		}
		else if (acceptOptional(TokenType.IF)) {
			Statement ifStmt = null;
			Statement elseStmt = null;

			accept(TokenType.LPAREN);
			expression = parseOrOperator();
			accept(TokenType.RPAREN);

			if (acceptOptional(TokenType.LBRACE)) {
				StatementList list = new StatementList();

				while (!acceptOptional(TokenType.RBRACE)) {
					list.add(parseStatement());
				}
				ifStmt = new BlockStmt(list, pos);
			}
			else {
				ifStmt = parseStatement();
			}

			if (acceptOptional(TokenType.ELSE)) {
				if (acceptOptional(TokenType.LBRACE)) {
					StatementList list = new StatementList();

					while (!acceptOptional(TokenType.RBRACE)) {
						list.add(parseStatement());
					}

					elseStmt = new BlockStmt(list, pos);
				}
				else {
					elseStmt = parseStatement();
				}
			}

			return new IfStmt(expression, ifStmt, elseStmt, pos);

		}
		else if (_currentToken.getTokenType() == TokenType.ID || _currentToken.getTokenType() == TokenType.THIS) {
			Reference reference = parseReference();
			Statement statement = null;

			if (acceptOptional(TokenType.EQUAL)) {
				Expression assignExpression = parseOrOperator();
				statement = new AssignStmt(reference, assignExpression, pos);
			}
			else if (acceptOptional(TokenType.LBRACKET)) {
				if (!acceptOptional(TokenType.RBRACKET)) {
					Expression ex1 = parseOrOperator();
					accept(TokenType.RBRACKET);
					accept(TokenType.EQUAL);
					Expression ex2 = parseOrOperator();

					statement = new IxAssignStmt(reference, ex1, ex2, pos);
				}
				else {
					Token idToken = _currentToken;

					accept(TokenType.ID);
					accept(TokenType.EQUAL);
					Expression valDecal = parseOrOperator();

					statement = new VarDeclStmt(
							new VarDecl(new ArrayType(new ClassType(new Identifier(curr),
									pos), pos), idToken.getTokenText(), pos, curr.getTokenText()),
							valDecal, pos);
				}
			}
			else if (acceptOptional(TokenType.LPAREN)) {
				ExprList list = new ExprList();

				if (!acceptOptional(TokenType.RPAREN)) {
					list = parseArgumentList();
					accept(TokenType.RPAREN);
				}

				statement = new CallStmt(reference, list, pos);
			}
			else {
				Token idToken = _currentToken;

				accept(TokenType.ID);
				accept(TokenType.EQUAL);
				Expression valDecal = parseOrOperator();

				statement = new VarDeclStmt(new VarDecl(new ClassType(new Identifier(curr), pos), idToken.getTokenText(), pos, curr.getTokenText()),valDecal, pos);
			}

			accept(TokenType.SEMICOLON);
			return statement;
		}
		else {
			TypeDenoter type = parseType();

			Token idToken = _currentToken;
			accept(TokenType.ID);
			accept(TokenType.EQUAL);

			Expression valDecal = parseOrOperator();
			accept(TokenType.SEMICOLON);

			return new VarDeclStmt(
					new VarDecl(type, idToken.getTokenText(), pos, curr.getTokenText()), valDecal, pos);
		}
	}

	private Expression parseExpression() {
		Token curr = _currentToken;
		Expression exp = null;

		if (acceptOptional(TokenType.NEW)) {
			Expression newExp = null;
			curr = _currentToken;

			TokenType[] param = { TokenType.ID, TokenType.INT };
			TokenType acceptedTokenType = acceptMultiple(param);

			if (acceptedTokenType == TokenType.ID) {
				if (acceptOptional(TokenType.LPAREN)) {
					accept(TokenType.RPAREN);

					newExp = new NewObjectExpr(new ClassType(new Identifier(curr), curr.getTokenPosition()), curr.getTokenPosition());
				}
				else {
					accept(TokenType.LBRACKET);
					Expression idArrExp = parseOrOperator();
					accept(TokenType.RBRACKET);

					newExp = new NewArrayExpr(new ClassType(new Identifier(curr), curr.getTokenPosition()), idArrExp, curr.getTokenPosition());
				}
			}
			else {
				accept(TokenType.LBRACKET);
				Expression intArrExp = parseOrOperator();
				accept(TokenType.RBRACKET);

				newExp = new NewArrayExpr(new BaseType(TypeKind.INT, curr.getTokenPosition()), intArrExp, curr.getTokenPosition());
			}

			exp = newExp;
		}
		else if (acceptOptional(TokenType.LPAREN)) {
			exp = parseOrOperator();
			accept(TokenType.RPAREN);
		}
		else if (acceptOptional(TokenType.MINUS) || acceptOptional(TokenType.UNOP)) {
			Expression unopExp = parseExpression();
			exp = new UnaryExpr(new Operator(curr), unopExp, curr.getTokenPosition());
		}
		else if (_currentToken.getTokenType() == TokenType.ID || _currentToken.getTokenType() == TokenType.THIS) {
			Reference reference = parseReference();

			if (acceptOptional(TokenType.LBRACKET)) {
				Expression ixExp = parseOrOperator();
				accept(TokenType.RBRACKET);

				exp = new IxExpr(reference, ixExp, curr.getTokenPosition());
			} else if (acceptOptional(TokenType.LPAREN)) {
				ExprList argumentList = new ExprList();
				if (!acceptOptional(TokenType.RPAREN)) {
					argumentList = parseArgumentList();
					accept(TokenType.RPAREN);
				}

				exp = new CallExpr(reference, argumentList, curr.getTokenPosition());
			}
			else {
				exp = new RefExpr(reference, curr.getTokenPosition());
			}
		} else if (acceptOptional(TokenType.NULL)) {
			exp = new LiteralExpr(new NullLiteral(curr), curr.getTokenPosition());
		}
		else if (acceptOptional(TokenType.INTLITERAL)) {
			exp = new LiteralExpr(new IntLiteral(curr), curr.getTokenPosition());
		}
		else if (acceptOptional(TokenType.TRUE) || acceptOptional(TokenType.FALSE)) {
			exp = new LiteralExpr(new BooleanLiteral(curr), curr.getTokenPosition());
		}
		else {
			_errors.reportError("Unexpected Token " + _currentToken.getTokenType() + " detected on " + _currentToken.getTokenPosition().toString());
			throw new SyntaxError();
		}

		return exp;
	}

	private Expression parseOrOperator() {
		SourcePosition pos = _currentToken.getTokenPosition();
		Expression leftExpr = parseAndOperator();

		Token operator = _currentToken;
		while (_currentToken.getTokenText().equals("||")) {
			accept(TokenType.BINOP);

			Expression rightExpr = parseAndOperator();
			leftExpr = new BinaryExpr(new Operator(operator), leftExpr, rightExpr, pos);

			operator = _currentToken;
		}

		return leftExpr;
	}

	private Expression parseAndOperator() {
		SourcePosition pos = _currentToken.getTokenPosition();
		Expression leftExpr = parseEquality();

		Token operator = _currentToken;
		while (_currentToken.getTokenText().equals("&&")) {
			accept(TokenType.BINOP);

			Expression rightExpr = parseEquality();
			leftExpr = new BinaryExpr(new Operator(operator), leftExpr, rightExpr, pos);

			operator = _currentToken;
		}

		return leftExpr;
	}

	private Expression parseEquality() {
		SourcePosition pos = _currentToken.getTokenPosition();
		Expression leftExpr = parseComparators();

		Token operator = _currentToken;
		while (acceptOptional(TokenType.EQUALEQUAL) || acceptOptional(TokenType.NEQ)) {
			Expression rightExpr = parseComparators();
			leftExpr = new BinaryExpr(new Operator(operator), leftExpr, rightExpr, pos);

			operator = _currentToken;
		}

		return leftExpr;
	}

	private Expression parseComparators() {
		SourcePosition pos = _currentToken.getTokenPosition();
		Expression leftExpr = parseAdditive();

		Token operator = _currentToken;
		while (acceptOptional(TokenType.GT) ||
				acceptOptional(TokenType.LT) ||
				acceptOptional(TokenType.GTEQ) ||
				acceptOptional(TokenType.LTEQ) ||
				acceptOptional(TokenType.EQUALEQUAL)) {
			Expression rightExpr = parseAdditive();
			leftExpr = new BinaryExpr(new Operator(operator), leftExpr, rightExpr, pos);

			operator = _currentToken;
		}

		return leftExpr;
	}

	private Expression parseAdditive() {
		SourcePosition pos = _currentToken.getTokenPosition();
		Expression leftExpr = parseMultiplicative();

		Token operator = _currentToken;
		while (_currentToken.getTokenText().equals("+") || _currentToken.getTokenText().equals("-")) {
			if (!acceptOptional(TokenType.PLUS)) {
				accept(TokenType.MINUS);
			}

			Expression rightExpr = parseMultiplicative();
			leftExpr = new BinaryExpr(new Operator(operator), leftExpr, rightExpr, pos);

			operator = _currentToken;
		}

		return leftExpr;
	}

	private Expression parseMultiplicative() {
		SourcePosition pos = _currentToken.getTokenPosition();
		Expression leftExpr = parseExpression();

		Token operator = _currentToken;
		while (_currentToken.getTokenText().equals("*") || _currentToken.getTokenText().equals("/")) {
			if (_currentToken.getTokenText().equals("*")) {
				accept(TokenType.MULT);
			} else if (_currentToken.getTokenText().equals("/")) {
				acceptOptional(TokenType.DIV);
			}

			Expression rightExpr = parseExpression();
			leftExpr = new BinaryExpr(new Operator(operator), leftExpr, rightExpr, pos);

			operator = _currentToken;
		}

		return leftExpr;
	}

	private TypeDenoter parseType() {
		TokenType[] param = { TokenType.INT, TokenType.BOOLEAN, TokenType.ID };
		Token typeToken = _currentToken;
		TokenType type = acceptMultiple(param);

		if (type == TokenType.BOOLEAN && acceptOptional(TokenType.LBRACKET)) {
			_errors.reportError("Boolean Array is not a valid type");
			return new BaseType(TypeKind.UNSUPPORTED, typeToken.getTokenPosition());
		} else if (acceptOptional(TokenType.LBRACKET)) {
			accept(TokenType.RBRACKET);

			if (type == TokenType.INT) {
				return new ArrayType(new BaseType(TypeKind.INT, typeToken.getTokenPosition()), typeToken.getTokenPosition());
			}
			else {
				return new ArrayType(new ClassType(new Identifier(typeToken), typeToken.getTokenPosition()), typeToken.getTokenPosition());
			}
		}

		if (type == TokenType.INT) {
			return new BaseType(TypeKind.INT, typeToken.getTokenPosition());
		} else if (type == TokenType.BOOLEAN) {
			return new BaseType(TypeKind.BOOLEAN, typeToken.getTokenPosition());
		}

		return new ClassType(new Identifier(typeToken), typeToken.getTokenPosition());
	}

	private ParameterDeclList parseParameters(String cn) throws SyntaxError {
		ParameterDeclList paramList = new ParameterDeclList();

		boolean comma = true;

		while (_currentToken.getTokenType() != TokenType.RPAREN) {
			if (!comma) {
				_errors.reportError("Syntax Error: Missing Comma Detected on " + _currentToken.getTokenPosition().toString());
				throw new SyntaxError();
			}

			ParameterDecl param = new ParameterDecl(parseType(), _currentToken.getTokenText(), _currentToken.getTokenPosition(), cn);

			accept(TokenType.ID);
			comma = acceptOptional(TokenType.COMMA);

			paramList.add(param);
		}

		if (comma) {
			_errors.reportError("Syntax Error: Unexpected Comma Detected on " + _currentToken.getTokenPosition().toString());

			throw new SyntaxError();
		}

		return paramList;
	}

	private Reference parseReference() {
		Reference reference = null;
		Token curr = _currentToken;

		TokenType[] refTokenTypes = { TokenType.ID, TokenType.THIS };

		if (acceptMultiple(refTokenTypes) == TokenType.ID) {
			reference = new IdRef(new Identifier(curr), curr.getTokenPosition());
		}
		else {
			reference = new ThisRef(curr.getTokenPosition());
		}

		while (acceptOptional(TokenType.DOT)) {
			curr = _currentToken;
			accept(TokenType.ID);

			reference = new QualRef(reference, new Identifier(curr), curr.getTokenPosition());
		}

		return reference;
	}

	private ExprList parseArgumentList() {
		boolean comma = true;
		ExprList list = new ExprList();

		while (_currentToken.getTokenType() != TokenType.RPAREN) {
			if (!comma) {
				_errors.reportError("Syntax Error: Missing Comma Detected on " + _currentToken.getTokenPosition().toString());
				throw new SyntaxError();
			}

			list.add(parseOrOperator());
			comma = acceptOptional(TokenType.COMMA);
		}

		if (comma) {
			_errors.reportError("Syntax Error: Unexpected Comma Detected on " + _currentToken.getTokenPosition().toString());

			throw new SyntaxError();
		}

		return list;
	}

	// This method will accept the token and retrieve the next token.
	//  Can be useful if you want to error check and accept all-in-one.
	private void accept(TokenType expectedType) throws SyntaxError {
		if (_currentToken.getTokenType() == expectedType) {
			_currentToken = _scanner.scan();
			return;
		}

		//  "Expected token X, but got Y"
		_errors.reportError("Syntax Error: Expected token " + expectedType + ", but got " + _currentToken.getTokenType() + " on " + _currentToken.getTokenPosition().toString());
		throw new SyntaxError();
	}

	private TokenType acceptMultiple(TokenType[] expectedTypes) throws SyntaxError {
		for (TokenType expectedType : expectedTypes) {
			if (_currentToken.getTokenType() == expectedType) {
				_currentToken = _scanner.scan();
				return expectedType;
			}
		}

		_errors.reportError("Syntax Error: Unexpected token " + _currentToken.getTokenType() + " detected on " + _currentToken.getTokenPosition().toString());
		throw new SyntaxError();
	}

	private boolean acceptOptional(TokenType expectedType) {
		if (_currentToken.getTokenType() == expectedType) {
			_currentToken = _scanner.scan();
			return true;
		}

		return false;
	}
}
