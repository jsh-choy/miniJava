package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.Reg64;
import miniJava.CodeGeneration.x64.x64;

public class Mov_ri64 extends Instruction {
	// mov r64,imm64 variant
	public Mov_ri64(Reg64 reg, long imm64) {
		rexW = true; // operand is 64bit
		if( reg.getIdx() > 7 )
			rexB = true;
		opcodeBytes.write(0xB8 + x64.getIdx(reg));
		x64.writeLong(immBytes,imm64);
	}
}
