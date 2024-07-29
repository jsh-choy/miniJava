package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;
import miniJava.CodeGeneration.x64.ModRMSIB;
import miniJava.CodeGeneration.x64.Reg64;
import miniJava.CodeGeneration.x64.x64;

public class Pop extends Instruction {
	public Pop(Reg64 r) {
		if( r.getIdx() > 7 )
			rexB = true;
		opcodeBytes.write(0x58 + x64.getIdx(r));
	}
	
	public Pop(ModRMSIB modrmsib) {
		opcodeBytes.write(0x8F);
		modrmsib.SetRegR(x64.mod543ToReg(0));
		byte[] rmsib = modrmsib.getBytes();
		importREX(modrmsib);
		x64.writeBytes(immBytes,rmsib);
	}
}
