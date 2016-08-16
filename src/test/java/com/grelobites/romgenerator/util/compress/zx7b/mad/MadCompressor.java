package com.grelobites.romgenerator.util.compress.zx7b.mad;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class MadCompressor
{
	public static byte[] compress(byte[] input_data) {
        byte[] output_data;
        int input_size;
        int output_size;
        int i;
        byte j;
        input_size = input_data.length;

        for (i = 0; i<input_size>>>1; i++)
        {
            j = input_data[i];
            input_data[i] = input_data[input_size-1 - i];
            input_data[input_size-1 - i] = j;
        }

	  /* generate output file */
        Optimal optimal[] = new Optimal[(Integer) input_size];		//optimal = (optimal_t)calloc(input_size, sizeof(optimal_t));
        optimize(optimal, input_data, input_size);
        output_data = compress(optimal, input_data, input_size);//, output_size);
        output_size=output_data.length;

        for (i = 0; i<output_size>>>1; i++)
        {
            j = output_data[i];
            output_data[i] = output_data[output_size-1 - i];
            output_data[output_size-1 - i] = j;
        }
        return output_data;
    }

	public static void main(String[] args)
	{
	  byte[] input_data; //ORIGINAL LINE: byte *input_data;
	  byte[] output_data;	//ORIGINAL LINE: unsigned char *output_data;
	  int input_size;	//ORIGINAL LINE: size_t input_size;
	  int output_size;	//ORIGINAL LINE: size_t output_size;
	//ORIGINAL LINE: size_t partial_counter;	  //int partial_counter; // not needed in java for reading file
	//ORIGINAL LINE: size_t total_counter;	  //int total_counter; // not needed in java for reading file
	 // String output_name; // not needed in java (file.getName() instead)
	  int i;
	  byte j;

	  if (args.length != 2)
	  {
		System.out.println("ZX7 Backwards compressor v1.01 by Einar Saukas/AntonioVillena, 28 Dec 2013");
		System.out.println("  ported to Java by Mad3001 28Jul2016");
		System.out.println("  zx7b <input_file> <output_file>");
		System.out.println("  <input_file>    Raw input file");
		System.out.println("  <output_file>   Compressed output file");
		System.out.println("Example: zx7b Cobra.scr Cobra.zx7b\n");
		System.exit(0);
	  }

	  File ifp = new File(args[0]);
	  /* open input file */
	  FileInputStream finp = null;
	  input_data = new byte[(int)ifp.length()];

	  try{
			finp = new FileInputStream(ifp);
			finp.read(input_data);
	  }
	  catch (FileNotFoundException e) {
			System.out.println("File not found: " + args[0] + ", " +e);
			System.exit(1);
	  }
	  catch (IOException ioe) {
			System.out.println("Exception reading file, " + ioe);
			System.exit(1);
	  }
	  finally {
			// close the streams using close method
			try {
				if (finp != null) {
					finp.close();
				}
			}
			catch (IOException ioe) {
				System.out.println("Error closing file, " + ioe);
			}
	  }

	  input_size = (int) ifp.length();

	  if (input_size == 0)
	  {
		  System.out.println("Error: Empty input file " +  ifp.getName());
		  System.exit(1);
	  }


	  File ofp = new File(args[1]);
	  /* open output file */
	  FileOutputStream finO = null;
	  //output_data = new byte[(int)ofp.length()];

	  try{
			finO = new FileOutputStream(ofp);
	  }
	  catch (FileNotFoundException e) {
			System.out.println("File not found: " + args[1] + ", " +e);
			System.exit(1);
	  }



	  for (i = 0; i<input_size>>>1; i++)
	  {
		j = input_data[i];
		input_data[i] = input_data[input_size-1 - i];
		input_data[input_size-1 - i] = j;
	  }

	  /* generate output file */
	  Optimal optimal[] = new Optimal[(Integer) input_size];		//optimal = (optimal_t)calloc(input_size, sizeof(optimal_t));
	  optimize(optimal, input_data, input_size);
	  output_data = compress(optimal, input_data, input_size);//, output_size);
	  output_size=output_data.length;

	  for (i = 0; i<output_size>>>1; i++)
	  {
		j = output_data[i];
		output_data[i] = output_data[output_size-1 - i];
		output_data[output_size-1 - i] = j;
	  }

	  /* write output file */
	  try{
			finO.write(output_data);
	  }
	  catch (FileNotFoundException e) {
			System.out.println("File not found: " + args[1] + ", " +e);
			System.exit(1);
	  }
	  catch (IOException ioe) {
			System.out.println("Exception writing file, " + ioe);
			System.exit(1);
	  }
	  finally {
			// close the streams using close method
			try {
				if (finO != null) {
					finO.close();
				}
			}
			catch (IOException ioe) {
				System.out.println("Error closing file, " + ioe);
			}
	  }

	  /* done! */
	  System.out.println("File " + ofp.getName() + " compressed from " + ifp.getName() + " (" + (int) input_size +" to "+(int) output_size + " bytes - " + (100-(100*output_size/input_size)) +"% less)");
	}

	public static byte[] output_data;	//ORIGINAL LINE: unsigned char* output_data;
	public static int output_index;	//ORIGINAL LINE: size_t output_index;
	public static int bit_index;	//ORIGINAL LINE: size_t bit_index;
	public static int bit_mask;

	//ORIGINAL LINE: optimal_t *optimize(unsigned char *input_data, size_t input_size);
	public static void optimize(Optimal optimal[], byte[] input_data, Integer input_size)
	{
		//match_t match;
		Integer match_index;
		Integer offset;
		Integer len; 	//ORIGINAL LINE: size_t len;
		Integer best_len;	//ORIGINAL LINE: size_t best_len;
		Integer bits;	//ORIGINAL LINE: size_t bits;
		Integer i;	//ORIGINAL LINE: size_t i;

		/* allocate all data structures at once */
		//ORIGINAL LINE: size_t *min;	//ORIGINAL LINE: uint *min;
		//Integer min;
		int min[] = new int[DefineConstants.MAX_OFFSET + 1];		//min = (int)calloc(DefineConstants.MAX_OFFSET + 1, Integer.SIZE);
		//ORIGINAL LINE: size_t *max;	//ORIGINAL LINE: uint *max;
		//Integer max;
		int max[] = new int[DefineConstants.MAX_OFFSET + 1];		//max = (int)calloc(DefineConstants.MAX_OFFSET + 1, Integer.SIZE);
		//match_t matches;
		Match matches[] = new Match[(Integer)256 * 256 ];		//matches = (match_t)calloc(256 * 256, sizeof(match_t));
		//match_t match_slots;
		Match match_slots[] = new Match[(Integer) input_size];		//match_slots = (match_t)calloc(input_size, sizeof(match_t));
		//optimal_t optimal; /see below
		//optimal_t optimal[] = new optimal_t[(Integer) input_size];		//optimal = (optimal_t)calloc(input_size, sizeof(optimal_t));

		if (min.length == 0 || max.length == 0 || matches.length == 0 || match_slots.length == 0 || optimal.length == 0)
		{
			System.out.println("Error: Insufficient memory");
			 System.exit(1);
		}
		/* first byte is always literal */
		//Initialize arrays
		for(i=0;i<optimal.length;i++) {
			optimal[i] = new Optimal();
		}
		for(i=0;i<matches.length;i++) {
			matches[i] = new Match();
		}
		for(i=0;i<match_slots.length;i++) {
			match_slots[i] = new Match();
		}

		optimal[0].bites = 8;

		/* process remaining bytes */
		for (i = 1; i < input_size; i++)
		{
		  optimal[i].bites = optimal[i - 1].bites + 9;

		  match_index = (int) (input_data[i - 1]<0?(int)input_data[i - 1]+256:input_data[i - 1]) << 8 | (int)(input_data[i]<0?(int)input_data[i]+256:input_data[i]);

		  best_len = 1;
		  for (Match match = matches[match_index]; match.next != null && best_len < DefineConstants.MAX_LEN; match = match.next)
		  {
			offset = i - match.next.index;
			if (offset > DefineConstants.MAX_OFFSET)
			{
			  match.next = null;
			  break;
			}
			for (len = 2; len <= DefineConstants.MAX_LEN; len++)
			{
			  if ((len > best_len) && (len & 0xff)!=0)
			  {
				best_len = len;
				bits = optimal[i - len].bites + count_bits(offset, len);
				if (optimal[i].bites > bits)
				{
				  optimal[i].bites = bits;
				  optimal[i].offset = offset;
				  optimal[i].len = len;
				}
			  }
			  else if (i + 1 == max[offset] + len && max[offset] != 0)
			  {
				len = i - min[offset];
				if (len > best_len)
				{
				  len = best_len;
				}
			  }
			  if (i < offset + len || input_data[i - len] != input_data[i - len - offset])
				break;
			}
			min[offset] =  i + 1 - len;
			max[offset] =  i;
		  }
		  match_slots[i].index = i;
		  match_slots[i].next = matches[match_index].next;
		  matches[match_index].next = match_slots[i];
		}

		/* save time by releasing the largest block only, the O.S. will clean everything else later */
		//free(match_slots); // not necessary in java
		//return optimal;
	}

	//ORIGINAL LINE: unsigned char *compress(optimal_t *optimal, unsigned char *input_data, size_t input_size, size_t *output_size);
//ORIGINAL LINE: byte *compress(optimal_t *optimal, byte *input_data, uint input_size, uint *output_size)
	public static byte[] compress(Optimal[] optimal, byte[] input_data, int input_size)//,  int output_size)
	{

	  int output_size;
	  Integer input_index;	//ORIGINAL LINE: size_t input_index;
	  Integer input_prev;	//ORIGINAL LINE: size_t input_prev;
	  int offset1;
	  int mask;
	  //int i;

	  /* calculate and allocate output buffer */
	  input_index = input_size-1;
	  output_size = (optimal[input_index].bites + 16 + 7) / 8;
	  byte output_data[] = new byte[output_size];	//ORIGINAL LINE: output_data= (unsigned char *)malloc(*output_size);
	  if (output_data.length == 0)
	  {
		  System.out.println( "Error: Insufficient memory\n");
		  System.exit(1);
	  }

	  /* un-reverse optimal sequence */
	  optimal[input_index].bites = 0;
	  while (input_index > 0)
	  {
    	if (optimal[input_index].len==null){
    		input_prev = input_index - 1;
    	}
    	else {
    		input_prev = input_index- optimal[input_index].len;
    	}
		optimal[input_prev].bites = input_index;
		input_index = input_prev;
	  }

	  output_index = 0;
	  bit_mask = 0;

	  /* first byte is always literal */
	  write_byte(output_data,input_data[0]);

	  /* process remaining bytes */
	  while ((input_index = optimal[input_index].bites) > 0)
	  {
		if (optimal[input_index].len==null) {
			  write_bit(output_data,0);
			  write_byte(output_data,input_data[input_index]);
		}
		else if ( optimal[input_index].len == 0 ){
			  write_bit(output_data,0);
			  write_byte(output_data,input_data[input_index]);
		}
		else
		{
		  /* sequence indicator */
		  write_bit(output_data,1);

		  /* sequence length */
		  write_elias_gamma(output_data,optimal[input_index].len - 1);

		  /* sequence offset */
		  offset1 = optimal[input_index].offset - 1;
		  if (offset1 < 128)
		  {
			write_byte(output_data,offset1);
		  }
		  else
		  {
			offset1 -= 128;
			write_byte(output_data,(offset1 & 127) | 128);
			for (mask = 1024; mask > 127; mask >>= 1)
			{
			  write_bit(output_data,offset1 & mask);
			}
		  }
		}
	  }

	  /* end mark */
	  write_bit(output_data,1);
	  write_elias_gamma(output_data,0xff);
	  return output_data;
	}

	public static void write_byte(byte[] output_data,int value)
	{
	  output_data[output_index++] = (byte) value;
	}

	public static void write_bit(byte[] output_data,int value)
	{
	  if (bit_mask == 0)
	  {
		bit_mask = 128;
		bit_index = output_index;
		write_byte(output_data,0);
	  }
	  if (value > 0)
	  {
		output_data[bit_index] |= bit_mask;
	  }
	  bit_mask >>= 1;
	}

	public static int elias_gamma_bits(int value)
	{
	  int bits;
	  bits = 1;
	  while (value > 1)
	  {
		bits += 2;
		value >>= 1;
	  }
	  return bits;
	}

	public static void write_elias_gamma(byte[] output_data,int value)
	{
	  int bits = 0;
	  int rvalue = 0;
	  while (value > 1)
	  {
		++bits;
		rvalue <<= 1;
		rvalue |= value & 1;
		value >>= 1;
	  }
	  while (bits-- != 0)
	  {
		write_bit(output_data,0);
		write_bit(output_data,rvalue & 1);
		rvalue >>= 1;
	  }
	  write_bit(output_data,1);
	}

	public static int count_bits(int offset, int len)
	{
		return 1 + (offset > 128 ? 12 : 8) + elias_gamma_bits(len - 1);
	}

}