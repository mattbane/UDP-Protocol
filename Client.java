package udp;

import java.net.*;
import java.util.*;
import java.io.*;
import java.security.*;

public class Client 
{
	private static InetAddress address;
	private static int port;
	private static int dataPort;
	private static List<byte[]> pwds = new ArrayList<byte[]>();
	private static int pwdAttempt = 0;
	private static File output;
	private static Protocol protocol;
	private static int [] headerData;
	private static Map<Integer, byte[]> serverData = new TreeMap<Integer, byte[]>();
	
	public static void main(String[] args)
	{
		if(args.length != 6)
		{
			System.out.println("Usage: java Client <server name> <server port> <clientpwd1> "
					+ "<clientpwd2> <clientpwd3> <output file path>");
			return;
		}
		
		try{
			address = InetAddress.getByName(args[0]);
			port = Integer.parseInt(args[1]);
			pwds.add(args[2].getBytes());
			pwds.add(args[3].getBytes());
			pwds.add(args[4].getBytes());
			output = new File(args[5]);
			protocol = Protocol.JOIN_REQ;
		}catch(UnknownHostException e){
			System.out.println("UnknownHostException. \"Abort\"");
			protocol = Protocol.REJECT;
		}
		
		while((protocol != Protocol.REJECT) && (protocol != Protocol.TERMINATE))
			connect();		
		if(protocol == Protocol.TERMINATE)
			System.out.println("The client is shutting down: \"OK\"" );
		else
			System.out.println("The client is shutting down: \"ABORT\"");
	}
	
	private static void connect()
	{
		try
		{
			byte[] buffer = new byte[256];
			byte [] data;
			switch (protocol)
			{
				case JOIN_REQ:
					Util.createHeader(1, 0, buffer);
					protocol = Protocol.PASS_REQ;
					data = dataSend(buffer);
					headerData = Util.checkHeader(data);
					break;
				case PASS_REQ:
					if((protocol.getValue() == headerData[0]) && (pwdAttempt < 3))
					{
						Util.createHeader(3, pwds.get(pwdAttempt).length, buffer);
						System.arraycopy(pwds.get(pwdAttempt), 0, buffer, 6, pwds.get(pwdAttempt).length);
						pwdAttempt++;
						data = dataSend(buffer);
						headerData = Util.checkHeader(data);
						if(headerData[0] == Protocol.PASS_ACCEPT.getValue()) { protocol = Protocol.DATA; }
					}else
						protocol = Protocol.REJECT;
					break;
				case DATA:
					byte [] recBuff = new byte[4096];
					DatagramSocket socket = new DatagramSocket(dataPort);
					while(headerData[0] != Protocol.TERMINATE.getValue())
					{
						DatagramPacket packet = new DatagramPacket(recBuff, recBuff.length);
						socket.receive(packet);
						headerData = Util.checkHeader(packet.getData());
						if(headerData[0] != Protocol.TERMINATE.getValue()) 
							serverData.put(Util.getPacketID(packet.getData()), Arrays.copyOfRange(packet.getData(), 10, (10 + headerData[1])));
						else
							checkData(packet.getData());
					}
					socket.close();
					break;
				default:
					
			}	
		}catch(Exception e){
			System.out.println("Client unable to connect to socket. \"Abort\"");
			e.printStackTrace();
			protocol = Protocol.REJECT;
		}
	}
	
	private static byte[] dataSend(byte[] buffer) throws SocketException, IOException
	{
		byte[] receiveBuff = new byte[256];
		DatagramSocket socket = new DatagramSocket();
		DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
		socket.send(packet);
		packet = new DatagramPacket(receiveBuff, receiveBuff.length);
		dataPort = socket.getLocalPort();
		socket.receive(packet);
		socket.close();
		return packet.getData();
	}
	
	private static void checkData(byte[] data) throws Exception
	{
		byte[] digest = Arrays.copyOfRange(data, 6, (6+headerData[1]));
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		
		for(Map.Entry<Integer, byte[]> entry : serverData.entrySet())
		{
			byte[] bytes = entry.getValue();
			if(bytes != null)
				md.update(bytes, 0, bytes.length);
		}

		byte[] clientDigest = md.digest();
		if(MessageDigest.isEqual(digest, clientDigest))
			protocol = Protocol.TERMINATE;
		else
			protocol = Protocol.REJECT;
		
		debugDigests(digest, clientDigest);
		writeToFile();
	}
	
	private static void writeToFile() throws IOException
	{
		FileOutputStream out = new FileOutputStream(output);
		for(Map.Entry<Integer, byte[]> entry : serverData.entrySet())
		{
			out.write(entry.getValue());
		}
		out.close();
	}
	
	private static void debugDigests(byte[] digest, byte[] clientDigest)
	{
		for (int i = 0; i < digest.length; i++) 
		{
			System.out.print (Util.intToHex (digest[i]));
		}
		System.out.println ();
		 
		for (int i = 0; i < clientDigest.length; i++) 
		{
			System.out.print (Util.intToHex (clientDigest[i]));
		}
		System.out.println ();
	}
}
