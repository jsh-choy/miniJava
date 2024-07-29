package miniJava.SyntacticAnalyzer;
import java.util.HashMap;
import java.util.Map;

public class TokenMap {
    private Map<String, TokenType> map;

    public TokenMap() {
        map = new HashMap<>();

        map.put("private", TokenType.VISIBILITY);
        map.put("public", TokenType.VISIBILITY);
        map.put("static", TokenType.ACCESS);
        map.put("class", TokenType.CLASS);
        map.put("void", TokenType.VOID);
        map.put("this", TokenType.THIS);
        map.put("return", TokenType.RETURN);
        map.put("new", TokenType.NEW);
        map.put("int", TokenType.INT);
        map.put("boolean", TokenType.BOOLEAN);
        map.put("if", TokenType.IF );
        map.put("else", TokenType.ELSE);
        map.put("while", TokenType.WHILE);
        map.put("true", TokenType.TRUE);
        map.put("false", TokenType.FALSE);
        map.put("null", TokenType.NULL);
    }

    public TokenType getTokenType(String s) {
        if (map.containsKey(s)) {
            return map.get(s);
        }

        return TokenType.ID;
    }
}