package udp;

import java.io.*;
import java.net.*;
import java.security.*;

public class Server 
{
	private static int port;
	private static String password;
	private static File file;
	private static Protocol protocol;
	private static DatagramSocket socket;
	private static DatagramPacket packet;
	private static int pwdChk=1;
	
	public static void main(String[] args) 
	{
		if(args.length != 3)
		{
			System.out.println("Usage: java Server <server port> <password> <input file path>");
			return;
		}
		
		port = Integer.parseInt(args[0]);
		password = args[1];
		file = new File(args[2]);
		protocol = Protocol.JOIN_REQ;
		
		while((protocol != Protocol.REJECT) && (protocol != Protocol.TERMINATE))
			acceptClient();		
		if(protocol == Protocol.TERMINATE)
			System.out.println("The server is shutting down: \"OK\"" );
		else
			System.out.println("The server is shutting down: \"ABORT\"");
	}
	
	private static void acceptClient()
	{
		try
		{
			socket = new DatagramSocket(port);
			byte[] buffer = new byte[256];
			packet = new DatagramPacket(buffer, buffer.length);
			socket.receive(packet);
			processRequest(packet.getData());
			socket.close();	
			if(protocol == Protocol.DATA)
				sendData();
		}catch(Exception e){
			System.out.println("Error occured processing client request. \"Abort\"");
			e.printStackTrace();
			protocol = Protocol.REJECT;
		}
	}
	
	private static void processRequest(byte[] clientData) throws Exception
	{
		byte[] buffer = new byte[256];
		switch (protocol)
		{
			case JOIN_REQ:
				int[] header = Util.checkHeader(clientData);
				if(header[0] == protocol.getValue())
				{
					Util.createHeader(2, 0, buffer);
					protocol = Protocol.PASS_RESP;
				}
				else
				{
					Util.createHeader(7, 0, buffer);
					protocol = Protocol.REJECT;
				}
				break;
			case PASS_RESP:
				header = Util.checkHeader(clientData);
				if(header[0] == protocol.getValue())
				{
					if(checkPwd(clientData, header[1]))
					{
						Util.createHeader(4, 0, buffer);
						protocol = Protocol.DATA;
					}
					else
					{
						if(pwdChk < 3)
						{
							Util.createHeader(2, 0, buffer);
							protocol = Protocol.PASS_RESP;
							pwdChk++;
						}
						else
						{
							Util.createHeader(7, 0, buffer);
							protocol = Protocol.REJECT;
						}
					}
				}
				break;
			default:		
		}
		InetAddress address = packet.getAddress();
		int port = packet.getPort();
		packet = new DatagramPacket(buffer, buffer.length, address, port);
		socket.send(packet);
	}
	
	private static boolean checkPwd(byte[] clientBytes, int pwdLenght)
	{
		return password.equals(new String(clientBytes, 6, pwdLenght));	
	}
	
	private static void sendData() throws Exception
	{
		int packetID = 1;
		byte[] buff = new byte[1000];
		FileInputStream in =  new FileInputStream(file);
		int numRead = 0;
		DatagramSocket socket = new DatagramSocket(port);

		while((numRead = in.read(buff, 10, 990)) != -1)
		{
			Util.createHeader(5, numRead, buff);
			Util.addPacketID(packetID++, buff);
			packet = new DatagramPacket(buff, buff.length, packet.getAddress(), packet.getPort());
			socket.send(packet);
		}
		in.close();
		socket.close();
		termination(secureHash());
	}
	
	private static byte[] secureHash() throws Exception
	{
		FileInputStream in = new FileInputStream(file);
		byte[] buffer = new byte[1024];
		MessageDigest md = MessageDigest.getInstance("SHA-1");
		int numRead = 0;
		while((numRead = in.read(buffer)) != -1)
			md.update(buffer, 0, numRead);
		in.close();
		return md.digest();	
	}
	
	private static void termination(byte[] digest) throws Exception
	{
		byte[] buffer = new byte[digest.length + 6];
		Util.createHeader(6, digest.length, buffer);
		System.arraycopy(digest, 0, buffer, 6, digest.length);
		DatagramSocket socket = new DatagramSocket(port);
		packet = new DatagramPacket(buffer, buffer.length, packet.getAddress(), packet.getPort());
		socket.send(packet);
		socket.close();
		protocol = Protocol.TERMINATE;
	}
}
