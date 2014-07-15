package udp;

public enum Protocol 
{
	JOIN_REQ(1), PASS_REQ(2), PASS_RESP(3), PASS_ACCEPT(4), 
	DATA(5), TERMINATE(6), REJECT(7);
	
	private int value;
	
	private Protocol(int value)
	{
		this.value = value;
	}
	
	public int getValue()
	{
		return value;
	}
}
