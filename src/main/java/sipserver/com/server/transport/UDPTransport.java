package sipserver.com.server.transport;

import gov.nist.core.CommonLogger;
import gov.nist.core.StackLogger;
import sipserver.com.parameter.ParamConstant.TransportType;
import sipserver.com.server.SipServerTransport;

public class UDPTransport extends SipServerTransport {

	// Logger
	private static StackLogger logger = CommonLogger.getLogger(UDPTransport.class);

	public UDPTransport(String host, int port) {
		super(host, port, TransportType.UDP, logger);
	}

}
