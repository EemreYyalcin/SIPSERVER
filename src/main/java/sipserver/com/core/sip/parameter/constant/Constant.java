package sipserver.com.core.sip.parameter.constant;

public class Constant {

	public enum TransportType {
		UDP,WS
	}
	
	public enum MessageState {
		STARTING, TRYING, RINGING, BUSY, OK, CALLING, CANCELING, BYE, FINISH, FAIL
	}
	
}
