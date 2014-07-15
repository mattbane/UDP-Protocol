package udp;

public class Util 
{
	public static byte[] intToByte(int myVal, int size)
	{
		byte[] info = new byte[size];
		int shift = 0;
		for(int i=0; i<size; i++)
		{
			info[i] = (byte)(myVal >>> shift);
			shift += 8;
		}		
		return info;
	}
	
	public static int byteToInt(byte[] bytes)
	{
		int myVal = 0;
		myVal = (0xff & bytes[3]) << 24;
		myVal |= (0xff & bytes[2]) << 16;
		myVal |= (0xff & bytes[1]) << 8;
		return myVal |= (0xff & bytes[0]);
	}
	
	public static void addPacketID(int num, byte[] buffer)
	{
		byte [] idBytes = intToByte(num, 4);
		System.arraycopy(idBytes, 0, buffer, 6, idBytes.length);
	}
	
	public static void createHeader(int head, int body, byte[] buffer)
	{
		byte[] header = intToByte(head, 2);
		byte[] payload = intToByte(body, 4);
		System.arraycopy(header, 0, buffer, 0, header.length);
		System.arraycopy(payload, 0, buffer, 2, payload.length);
	}
	
	public static int[] checkHeader(byte[] clientBytes)
	{
		int[] header = new int[2];
		header[0] = byteToInt(new byte[]{clientBytes[0], clientBytes[1], (byte) 0, (byte) 0});
		header[1] = byteToInt(new byte[]{clientBytes[2], clientBytes[3], clientBytes[4], clientBytes[5]});
		return header;
	}
	
	public static int getPacketID(byte[] bytes)
	{
		return byteToInt(new byte[]{bytes[6], bytes[7], bytes[8], bytes[9]});
	}
	
	public static String byteToHex (byte b) 
	{
	   int i = (int) b;
	   if (i < 0) {
	      i = 255 + i;
	   }
	   return intToHex (i);
	}

	 public static String intToHex (int i) 
	 {
	    char ls = nibbleToHex (i & 0x0F);
	    char ms = nibbleToHex ((i >> 4) & 0x0F);
	    return 
	      (new Character (ms)).toString () + 
	      (new Character (ls)).toString ();
	 }

	 private static char nibbleToHex (int i) 
	 {
	    if ((0 <= i) && (i <= 9)) {
	      return (char) (i + 0x30);
	    } else {
	      return (char) (i - 10 + 0x41);
	    } 
	 }
}
