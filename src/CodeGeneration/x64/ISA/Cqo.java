package miniJava.CodeGeneration.x64.ISA;

import miniJava.CodeGeneration.x64.Instruction;

public class Cqo extends Instruction {
	public Cqo() {
		rexW = true;
		opcodeBytes.write(0x99);
	}
}
