package miniJava.CodeGeneration.x64;

// Rex: 0100 W R X B
// instruction lhs, rhs
// B= RegRM is r8-15
// X= Scaled Index RegIdx is r8-15
// R= RegR is r8-15
// W= 64-bit operand (RegR is rax instead of eax, etc., not always needed).

public enum OpcodePrefix {
	LOCK(0xF0),
	REPNE(0xF2), REPNZ(0xF2),
	REP(0xF3), REPE(0xF3), REPZ(0xF3),
	CS(0x2E), SS(0x36), DS(0x3E), ES(0x26),
	FS(0x64), GS(0x65), BranchNotTaken(0x2E), BranchTaken(0x3E),
	OperandSizeOverride(0x66),
	AddressSizeOverride(0x67);
	
	public int prefix;
	private OpcodePrefix(int prefix) {
		this.prefix = prefix;
	}
}
