package miniJava.CodeGeneration.x64;

import java.io.ByteArrayOutputStream;

public class ModRMSIB {
	private ByteArrayOutputStream _b; // = new ByteArrayOutputStream();
	private boolean rexW = false;
	private boolean rexR = false;
	private boolean rexX = false;
	private boolean rexB = false;
	
	public boolean getRexW() {
		return rexW;
	}
	
	public boolean getRexR() {
		return rexR;
	}
	
	public boolean getRexX() {
		return rexX;
	}
	
	public boolean getRexB() {
		return rexB;
	}
	
	public byte[] getBytes() {
		_b = new ByteArrayOutputStream();
		// construct
		if( rdisp != null && ridx != null && r != null )
			Make(rdisp,ridx,mult,disp,r);
		else if( ridx != null && r != null )
			Make(ridx,mult,disp,r);
		else if( rdisp != null && r != null )
			Make(rdisp,disp,r);
		else if( rm != null && r != null )
			Make(rm,r);
		else if( r != null )
			Make(disp,r);
		else throw new IllegalArgumentException("Cannot determine ModRMSIB");
		
		return _b.toByteArray();
	}
	
	private Reg64 rdisp = null, ridx = null;
	private Reg rm = null, r = null;
	private int disp = 0, mult = 0;
	
	// [rdisp+ridx*mult+disp],r64
	public ModRMSIB(Reg64 rdisp, Reg64 ridx, int mult, int disp, Reg r) {
		SetRegR(r);
		SetRegDisp(rdisp);
		SetRegIdx(ridx);
		SetDisp(disp);
		SetMult(mult);
	}
	
	// r must be set by some mod543 instruction
	// [rdisp+ridx*mult+disp]
	public ModRMSIB(Reg64 rdisp, Reg64 ridx, int mult, int disp) {
		SetRegDisp(rdisp);
		SetRegIdx(ridx);
		SetDisp(disp);
		SetMult(mult);
	}
	
	// [rdisp+disp],r
	public ModRMSIB(Reg64 rdisp, int disp, Reg r) {
		SetRegDisp(rdisp);
		SetRegR(r);
		SetDisp(disp);
	}
	
	// r will be set by some instruction to a mod543
	// [rdisp+disp]
	public ModRMSIB(Reg64 rdisp, int disp) {
		SetRegDisp(rdisp);
		SetDisp(disp);
	}
	
	// rm64,r64
	public ModRMSIB(Reg64 rm, Reg r) {
		SetRegRM(rm);
		SetRegR(r);
	}
	
	// rm or r
	public ModRMSIB(Reg64 r_or_rm, boolean isRm) {
		if( isRm )
			SetRegRM(r_or_rm);
		else
			SetRegR(r_or_rm);
	}
	
	public int getRMSize() {
		if( rm == null ) return 0;
		return rm.size();
	}
	
	//public ModRMSIB() {
	//}
	
	public void SetRegRM(Reg rm) {
		if( rm.getIdx() > 7 ) rexB = true;
		rexW = rexW || rm instanceof Reg64;
		this.rm = rm;
	}
	
	public void SetRegR(Reg r) {
		if( r.getIdx() > 7 ) rexR = true;
		rexW = rexW || r instanceof Reg64;
		this.r = r;
	}
	
	public void SetRegDisp(Reg64 rdisp) {
		if( rdisp.getIdx() > 7 ) rexB = true;
		this.rdisp = rdisp;
	}
	
	public void SetRegIdx(Reg64 ridx) {
		if( ridx.getIdx() > 7 ) rexX = true;
		this.ridx = ridx;
	}
	
	public void SetDisp(int disp) {
		this.disp = disp;
	}
	
	public void SetMult(int mult) {
		this.mult = mult;
	}
	
	public boolean IsRegR_R8() {
		return r instanceof Reg8;
	}
	
	public boolean IsRegR_R64() {
		return r instanceof Reg64;
	}
	
	public boolean IsRegRM_R8() {
		return rm instanceof Reg8;
	}
	
	public boolean IsRegRM_R64() {
		return rm instanceof Reg64;
	}
	
	// rm,r
	private void Make(Reg rm, Reg r) {
		int mod = 3;
		
		int regByte = ( mod << 6 ) | ( getIdx(r) << 3 ) | getIdx(rm);
		_b.write( regByte ); 
	}
	
	// [rdisp+disp],r
	private void Make(Reg64 rdisp, int disp, Reg r) {
		int mod;
		
		if( rdisp == Reg64.RIP ) {
			// mod=0, RBP turns into [rip]+disp32
			_b.write( (getIdx(r) << 3) | 5 );
			writeInt(_b,disp);
			return;
		}
		
		// RBP must have a specified displacement, because mod=0 w/ disp RBP turns into [rip+disp32]
		if( disp == 0 && rdisp != Reg64.RBP ) mod = 0;
		else if( isOneByte(disp) ) mod = 1;
		else mod = 2;
		
		int regByte = ( mod << 6 ) | ( getIdx(r) << 3 ) | ( getIdx(rdisp) );
		_b.write(regByte);
		
		if( getIdx(rdisp) == 4 ) { // RSP or R12
			// forced to output an SIB where SS=anything
			_b.write( 4 << 3 | ( getIdx(rdisp) ) );
		}
		
		if( mod == 1 ) _b.write(disp);
		else if( mod == 2 ) writeInt(_b,disp);
	}
	
	/*
	// [ridx*mult+disp],r
	private void Make( Reg64 ridx, int mult, int disp, Reg r ) {
		if( !(mult == 1 || mult == 2 || mult == 4 || mult == 8) )
			throw new IllegalArgumentException("Invalid multiplier value: " + mult);
		if( ridx == Reg64.RSP )
			throw new IllegalArgumentException("Index cannot be rsp");
		
		int mod;
		// RBP must have an offset specified, otherwise mod=0&disp=RBP turns into [disp]
		if( disp == 0 && rdisp != Reg64.RBP ) mod = 0;
		else if( isOneByte(disp) ) mod = 1;
		else mod = 2;
		
		int regByte = ( mod << 6 ) | ( getIdx(r) << 3 ) | 4;
		_b.write(regByte);
		
		int ss;
		if( mult == 1 ) ss = 0;
		else if( mult == 2 ) ss = 1;
		else if( mult == 4 ) ss = 2;
		else ss = 3;
		
		int reg2Byte = ( ss << 6 ) | ( getIdx(ridx) << 3 ) | 5;
		_b.write(reg2Byte);
		
		if( mod == 1 ) _b.write(disp);
		else if( mod == 2 ) writeInt(_b,disp);
	}*/
	
	// [ridx*mult+disp],r
	private void Make( Reg64 ridx, int mult, int disp, Reg r ) {
		if( !(mult == 1 || mult == 2 || mult == 4 || mult == 8) )
			throw new IllegalArgumentException("Invalid multiplier value: " + mult);
		if( ridx == Reg64.RSP )
			throw new IllegalArgumentException("Index cannot be rsp");
		
		// what is happening in this function deserves explanation.
		// First, we set mod=00, and enforce SIB with by setting lowest bits to 100=4
		// Then, we set SIB's ss according to the multiplier, and idx in 543
		// Lastly, we enforce RBP=101=5 in lowest bits, because when mod=00 and lowest bits=5,
		//   then it always accepts a displacement of 32 bits afterwards.

		int regByte = ( getIdx(r) << 3 ) | 4;
		_b.write(regByte);
		
		int ss;
		if( mult == 1 ) ss = 0;
		else if( mult == 2 ) ss = 1;
		else if( mult == 4 ) ss = 2;
		else ss = 3;
		
		int reg2Byte = ( ss << 6 ) | ( getIdx(ridx) << 3 ) | 5;
		_b.write(reg2Byte);
		
		writeInt(_b,disp);
	}
	
	// [rdisp+ridx*mult+disp],r
	private void Make( Reg64 rdisp, Reg64 ridx, int mult, int disp, Reg r ) {
		if( !(mult == 1 || mult == 2 || mult == 4 || mult == 8) )
			throw new IllegalArgumentException("Invalid multiplier value: " + mult);
		if( ridx == Reg64.RSP )
			throw new IllegalArgumentException("Index cannot be rsp");
		int mod;
		
		// RBP must have an offset specified, otherwise mod=0&disp=RBP turns into [disp]
		if( disp == 0 && rdisp != Reg64.RBP ) mod = 0;
		else if( isOneByte(disp) ) mod = 1;
		else mod = 2;
		
		int regByte = ( mod << 6 ) | ( getIdx(r) << 3 ) | 4;
		_b.write(regByte);
		
		int ss;
		if( mult == 1 ) ss = 0;
		else if( mult == 2 ) ss = 1;
		else if( mult == 4 ) ss = 2;
		else ss = 3;
		
		int reg2Byte = ( ss << 6 ) | ( getIdx(ridx) << 3 ) | getIdx(rdisp);
		_b.write(reg2Byte);
		
		if( mod == 1 ) _b.write(disp);
		else if( mod == 2 ) writeInt(_b,disp);
	}
	
	// [disp],r
	private void Make( int disp, Reg r ) {
		_b.write( ( getIdx(r) << 3 ) | 4 );
		_b.write( ( 4 << 3 ) | ( 5 ) ); // ss doesn't matter
		writeInt(_b,disp);
	}
	
	private int getIdx(Reg r) {
		return x64.getIdx(r);
	}
	
	private boolean isOneByte(int v) {
		return v >= Byte.MIN_VALUE && v < Byte.MAX_VALUE; // [-128,127]
	}
	
	private void writeInt(ByteArrayOutputStream b, int n) {
		for( int i = 0; i < 4; ++i ) {
			b.write( n & 0xFF );
			n >>= 8;
		}
	}
}
