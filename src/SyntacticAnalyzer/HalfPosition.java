package miniJava.SyntacticAnalyzer;

public class HalfPosition {
    public int _lineNum;
    public int _lineWidth;

    public HalfPosition(int lineNum, int lineWidth){
        this._lineNum = lineNum;
        this._lineWidth = lineWidth;
    }

    public int getLineNum(){
        return _lineNum;
    }

    public int getLineWidth(){
        return _lineWidth;
    }
}
