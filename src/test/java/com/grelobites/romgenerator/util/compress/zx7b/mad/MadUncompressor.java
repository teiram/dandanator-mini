package com.grelobites.romgenerator.util.compress.zx7b.mad;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class MadUncompressor
{


    public static byte[] uncompress(byte[] fileArray) throws Exception {
        int InputLen=-1;	// will contain len of input file
        int FinalDE=-1;		// will contain size of uncompressed file (max=65535)

        InputLen=fileArray.length;
        if (InputLen>65535) {
            throw new IllegalArgumentException("Maximum file lenght aprox 64K=65535 bytes");
        }

        byte[] OutRevArray = new byte[65536]; // maximum zx7b size (really for ZX Spect should be 49152)
        StatusZ80 status = new StatusZ80();
        FinalDE=Process(status,fileArray,OutRevArray);


        byte[] CopyArray = new byte[65535-FinalDE];		// final size of file (65535-1 because array begins in 0)
        System.arraycopy(OutRevArray,FinalDE+1,CopyArray,0,65535-FinalDE);
        return CopyArray;
    }

	public static void main(String[] args)
	{
		int InputLen=-1;	// will contain len of input file
		int FinalDE=-1;		// will contain size of uncompressed file (max=65535)
		if (args.length!=2) {
			System.out.println("Syntax: dzx7b <input_file> <output_file>");
			System.exit(1);
		}
		Path file = Paths.get(args[0]);
		byte[] fileArray = null;
		try {
			fileArray = Files.readAllBytes(file);
			InputLen=fileArray.length;
			System.out.println("Read: " + fileArray.length + " bytes.");
		}
		catch (IOException e){
			System.out.println("Error : " + e);
			System.exit(1);
		}
		catch (OutOfMemoryError e){
			System.out.println("Error : " + e);
			System.exit(1);
		}
		catch (SecurityException e){
			System.out.println("Error : " + e);
			System.exit(1);
		}
		finally {};

		if (InputLen>65535)
		{
			System.out.println("Maximum file lenght aprox 64K=65535 bytes");
			System.exit(1);

		}

		byte[] OutRevArray = new byte[65536]; // maximum zx7b size (really for ZX Spect should be 49152)
		StatusZ80 status = new StatusZ80();
		FinalDE=Process(status,fileArray,OutRevArray);

		file = Paths.get(args[1]);	// now point to output file

		byte[] CopyArray = new byte[65535-FinalDE];		// final size of file (65535-1 because array begins in 0)
		System.arraycopy(OutRevArray,FinalDE+1,CopyArray,0,65535-FinalDE);

		try {
			Files.write(file,CopyArray,StandardOpenOption.CREATE);
			System.out.println("Uncompressed " + file.getFileName() + ". Length of file: " + CopyArray.length + " bytes.");
		}
		catch (IOException e){
			System.out.println("Error : " + e);
			System.exit(1);
		}
		catch (OutOfMemoryError e){
			System.out.println("Error : " + e);
			System.exit(1);
		}
		catch (SecurityException e){
			System.out.println("Error : " + e);
			System.exit(1);
		}
		finally {};

	}

	private static int Process(StatusZ80 status,byte[] fileArray,byte[] OutRevArray)
	{
		// auxiliary variables out of dzx7b_fast.asm but necessary in Java to control/store data
		//	while uncompressing
		boolean Cont_mainlo=false; // controls mainlo loop (TRUE= does not execute copyby zone)
		boolean Cont_Double=false; // controls lenval loop (TRUE= execute add a,a)
		//int SPDato;		// For use in commands like PUSH/POP/EX (SP),HL
		int AuxReg;		// Aux used for DE 16bit register when 8bit operation in D or E reg
		status.Carry=false; // Carry flag (updated where needed)
		status.Zero=false; // Zero flag (updated where needed)

		// begin defining variables similarly to dzx7b_fast.asm
		// notation from assembler a,b,c -> aReg, bReg, cReg.... in java a,b,c is not clear
		status.HL=fileArray.length-1; // as routine is backwards, we'll begin from last byte to first byte
		status.DE=65535; //In java we'll uncompress from high address to address zero
		//so data will be uncompressed in a reverse order
		//(later on only will be output to file the used zone)
		status.bReg = 0x80;
		status.cReg =0;								// LD BC,$8000
		status.aReg=status.bReg;					// LD A,B

		do												//	label copyby....
		{
			do
			{
				if (!Cont_mainlo) 						//	....label copyby
				{
					status.cReg++; // inc c
					OutRevArray[status.DE]=fileArray[status.HL];		//  ldd (begin...)
					status.DE--;
					status.HL--;
					status.cReg--;
					if (status.cReg<0)
					{
						status.cReg=255;
						status.bReg--;
						if (status.bReg<0)
						{
							status.bReg=255;
						}	// should never do this but to emulate the same behaviour
					}									// ldd (...end)
				}
				// label mainlo
				status.aReg+=status.aReg ;
				if (status.aReg>255) { status.aReg-=256; status.Carry=true;}	//add a,a
				status.Zero=(status.aReg==0);	//update zero flag
				if (status.Zero)							// call z,getbit
				{
					getbit(status,fileArray);
				}
				Cont_mainlo=false;	// assure execution of copy bytes
			} while (!status.Carry);						// jr nz,copyby
			status.SPDato=status.DE;								// push de
			status.DE=(status.cReg*256) + (status.DE %256);				// ld d,c
			do
			{								// debf $30 -> It's a trick so "add a,a" is skipped
				// when arrived from mainlo (not from jr nc,lenval)
				if(Cont_Double)			// only executed coming from jr nc,lenval (see below)
				{
					status.aReg+=status.aReg ;
					if (status.aReg>255) { status.aReg-=256; status.Carry=true;}	//add a,a
					status.Zero=(status.aReg==0);		//update zero flag
				}
				if (status.Zero)							// call z,getbit
				{
					getbit(status,fileArray);
				}
				status.cReg+=status.cReg+(status.Carry?1:0);	// <rl c...
				if (status.cReg>255)
				{
					status.Carry=true;
					status.cReg-=256;
				}
				else
				{
					status.Carry=false;
				}												// ... end rl c>
				status.bReg+=status.bReg+(status.Carry?1:0);	// <rl b...
				if (status.bReg>255)
				{
					status.Carry=true;
					status.bReg-=256;
				}
				else
				{
					status.Carry=false;
				}												// ... end rl b>
				status.aReg+=status.aReg ;						// < add a,a ...
				if (status.aReg>255)
				{
					status.aReg-=256;
					status.Carry=true;
				}
				else
				{
					status.Carry=false;
				}												// ... add a,a >
				status.Zero=(status.aReg==0);		//update zero flag
				if (status.Zero)							// call z,getbit
				{
					getbit(status,fileArray);
				}
				Cont_Double=true;	// does execute add a,a if return when not carry
			} while (!status.Carry);						//	jr nc, lenval
			status.cReg++;
			if (status.cReg>255) { status.cReg-=256;}				// inc c
			status.Zero=(status.cReg==0);		//update zero flag from previous inc c
			if (!status.Zero)			// if not zero then execute, otherwise: jr z,exitdz
			{
				AuxReg=fileArray[status.HL];				// < ld e,(hl) ...
				if (AuxReg<0)
				{
					AuxReg+=256;
				}
				//status.DE=(status.DE-status.DE%256)+AuxReg;	//	...ld      e, (hl>)
				status.HL--;								//	dec hl
				//AuxReg=status.DE%256;		// E alone
				AuxReg+=AuxReg;			// < sll e ...
				AuxReg++;				// ... insert 1 in bit 0 ...  sll e>
				status.Carry = (AuxReg >255);
				if (status.Carry) { AuxReg-=256; }			// sll e
				status.DE=(status.DE-status.DE%256)+AuxReg;	// compose DE again (change e only)
				if(status.Carry)		// if carry then execute, otherwise: jr nc, offend
				{
					status.DE=(4096)+AuxReg; // +status.DE%256;			// ld d,$10
					do
					{
						status.aReg+=status.aReg ;
						if (status.aReg>255)
						{
							status.aReg-=256;
							status.Carry=true;						//add a,a
						}
						else
						{
							status.Carry=false;
						}
						status.Zero=(status.aReg==0);		//update zero flag
						if (status.Zero)							// call z,getbit
						{
							getbit(status,fileArray);
						}
						AuxReg=(status.DE-status.DE%256)/256;					// extract D
						AuxReg+=AuxReg+(status.Carry?1:0);	// <rl d...
						if (AuxReg>255)
						{
							status.Carry=true;
							AuxReg-=256;
						}
						else
						{
							status.Carry=false;
						}												// ... end rl d>
						status.DE=AuxReg*256+status.DE%256;			// compose DE again (change d only)
					} while (!status.Carry);				// jr nc,nexbit
					AuxReg++;						// inc d
					status.Zero=(AuxReg==0);		//update zero flag
					status.Carry=(AuxReg%2!=0);	// low bit for carry
					AuxReg=AuxReg/2;		// srl d
					status.Zero=(AuxReg==0);		//update zero flag
					status.DE=AuxReg*256+status.DE%256;			// compose DE again (change d only)
				}
				// label offend
				AuxReg=status.DE%256;		// E alone
				if(status.Carry) { AuxReg+=256; }	// put carry in "nine bit" prior to shift right
				status.Carry=(AuxReg%2!=0);	// low bit for carry
				AuxReg=AuxReg/2;								// rr e
				status.Zero=(AuxReg==0);		//update zero flag
				status.DE=(status.DE-status.DE%256)+AuxReg;	// compose DE again (change e only)

				AuxReg=status.SPDato;			// copy (SP) to aux
				status.SPDato=status.HL;				// copy HL to (SP)
				status.HL=AuxReg;							// ex (sp),hl

				AuxReg=status.DE;				// copy DE to aux
				status.DE=status.HL;					// copy HL to DE
				status.HL=AuxReg;							// ex de,hl

				status.HL=status.HL+status.DE+(status.Carry?1:0);				// adc hl,de
				if (status.HL>65535)
				{
					status.Carry=true;
				}
				status.Carry=false;	// it's supossed the sum will not be greater than 65535
				lddr(status,OutRevArray);	// lddr
			} // label exitdz
			status.HL=status.SPDato;								// POP HL
			Cont_mainlo=true;			// assure jump to mainlo, not copyby
			Cont_Double=false;		// does NOT execute add a,a if return when not carry to original mainlo label
		} while (!status.Carry);							// jr nc,mainlo

		//getbit(status,fileArray);  // in asm it's here to save 1 byte, but in java makes FAIL because index=-1
		return status.HL;			// return final address  to caller
	}


	private static void getbit(StatusZ80 status,byte[] buff)
	{
		status.aReg=(int)buff[status.HL];					//		ld      a, (hl)
		if (status.aReg<0)
		{
			status.aReg+=256;	// if byte from buffer is negative (java limitation to type byte) make it positive
		}
		status.HL--;								//		dec     hl
		status.aReg+=status.aReg;					//equivalent to add a,a...
		status.aReg+=(status.Carry?1:0) ;			// ...add carry (adc)...
		status.Carry=false;				// ...preset new carry to non carry...
		if (status.aReg>255)				// ...limit to byte and activate Carry if exceeded...
		{
			status.aReg-=256;
			status.Carry=true;						//...activate carry...
		}										//... adc a,a
		status.Zero=(status.aReg==0);		//update zero flag
	}

	private static void lddr(StatusZ80 status,byte[] buff)
	{
		int BC= status.bReg*256+status.cReg;
		do
		{
			buff[status.DE]=buff[status.HL];
			status.HL--;
			status.DE--;
			BC--;
		} while (BC>0);
		status.bReg=0;	// update prior to return
		status.cReg=0;	// update prior to return, lddr/ldir always finish with BC=0
	}

}

