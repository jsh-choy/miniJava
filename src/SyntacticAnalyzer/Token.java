package miniJava.SyntacticAnalyzer;

public class Token {
	private TokenType _type;
	private String _text;
	private SourcePosition _position;

	public Token(TokenType type, String text, SourcePosition position) {
		// TODO: Store the token's type and text
		this._type = type;
		this._text = text;
		this._position = position;
	}

	public TokenType getTokenType() {
		// TODO: Return the token type
		return _type;
	}

	public String getTokenText() {
		// TODO: Return the token text
		return _text;
	}

	public SourcePosition getTokenPosition() {
		return _position;
	}
}
