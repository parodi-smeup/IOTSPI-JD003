package com.smeup.jd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.smeup.rpgparser.interpreter.NumberType;
import com.smeup.rpgparser.interpreter.Program;
import com.smeup.rpgparser.interpreter.ProgramParam;
import com.smeup.rpgparser.interpreter.StringType;
import com.smeup.rpgparser.interpreter.StringValue;
import com.smeup.rpgparser.interpreter.SystemInterface;
import com.smeup.rpgparser.interpreter.Value;

import Smeup.smeui.iotspi.interaction.SPIIoTConnectorAdapter;

public class JD_RCVSCK implements Program {

	private List<ProgramParam> parms;
	private String iError;
	private ServerSocket serverSocket;
	private SPIIoTConnectorAdapter sPIIoTConnectorAdapter;
	
	private static final int DEBUG = 0;
	private static final int INFO = 10;
	private static final int ERROR = 50;	
	private int logLevel = DEBUG;

	public JD_RCVSCK() {
		parms = new ArrayList<ProgramParam>();
		// Socket address
		parms.add(new ProgramParam("ADDRSK", new StringType(15)));
		// Response (read from socket)
		parms.add(new ProgramParam("BUFFER", new StringType(30000)));
		// Response length
		parms.add(new ProgramParam("BUFLEN", new NumberType(5, 0)));
		// Error
		parms.add(new ProgramParam("IERROR", new StringType(1)));
	}

	private String listenSocket(final int port){
		
		String msgLog = "Executing listenSocket(" +port+ ")";
		System.out.println(msgLog);
		getsPIIoTConnectorAdapter().log(logLevel, msgLog);
		
		StringBuilder responseAsString = null;
		Socket socket = null;
		InputStream input = null;
		BufferedReader reader = null;
		
		try {
			responseAsString = new StringBuilder();
			
			msgLog = "Socket listening on port " + port + "...";
			System.out.println(msgLog);
			getsPIIoTConnectorAdapter().log(logLevel, msgLog);
			
			socket = this.serverSocket.accept();
			
			socket.setSoTimeout(1000); //SAME AS VEGA PLUGIN (MONKEY COPY, DON'T KNOW WHY)
			
			msgLog = "...client connected";
			System.out.println(msgLog);
			getsPIIoTConnectorAdapter().log(logLevel, msgLog);
			
			input = socket.getInputStream();
			reader = new BufferedReader(new InputStreamReader(input));
			String line;
			while (reader.ready()) {
				line = reader.readLine();
				if (null == line) {
					break;
				}
				responseAsString.append(line + "\n");
			}
			
			msgLog = "Content written: " + responseAsString;
			System.out.println(msgLog);
			getsPIIoTConnectorAdapter().log(logLevel, msgLog);

			socketAndInBufferDestroy(socket, reader);
			
		} catch (IOException e) {
			msgLog = "IOException " + e.getMessage();
			System.out.println(msgLog);
			getsPIIoTConnectorAdapter().log(logLevel, msgLog);
			e.printStackTrace();
			responseAsString.append("*ERROR " + e.getMessage());
			iError = "1";
		} 

		return responseAsString.toString();
	}

	@Override
	public List<ProgramParam> params() {
		return parms;
	}

	@Override
	public List<Value> execute(SystemInterface arg0, LinkedHashMap<String, Value> arg1) {
		String msgLog = "Executing JD_RCVSCK.execute(...)";
		System.out.println(msgLog);
		getsPIIoTConnectorAdapter().log(logLevel, msgLog);
		
		ArrayList<Value> arrayListResponse = new ArrayList<Value>();

		String response = "";
		int bufferLength = 0;
		iError = "";
		String addrsk = "";
		String buffer = "";
		Long buflen = 0L;
		String ierror = "";

		for (Map.Entry<String, ? extends Value> entry : arg1.entrySet()) {

			String parmName = entry.getKey().toString();

			switch (parmName) {
			case "ADDRSK":
				addrsk = entry.getValue().asString().getValue();
				break;
			case "BUFFER":
				buffer = entry.getValue().asString().getValue();
				break;
			case "BUFLEN":
				buflen = entry.getValue().asInt().getValue();
				break;
			case "IERROR":
				ierror = entry.getValue().asString().getValue();
				break;
			}

			// all parms values as received
			arrayListResponse.add(entry.getValue());

		}

		// listen to socket
		int port = Integer.parseInt(addrsk.trim());
		response = listenSocket(port);

		// response from socket content
		arrayListResponse.set(1, new StringValue(response.trim()));

		// response length
		bufferLength = response.trim().length();
		arrayListResponse.set(2, new StringValue(String.valueOf(bufferLength)));

		return arrayListResponse;
	}

	public ServerSocket getServerSocket() {
		return serverSocket;
	}

	public void setServerSocket(ServerSocket serverSocket) {
		this.serverSocket = serverSocket;
	}

	public SPIIoTConnectorAdapter getsPIIoTConnectorAdapter() {
		return sPIIoTConnectorAdapter;
	}

	public void setsPIIoTConnectorAdapter(SPIIoTConnectorAdapter sPIIoTConnectorAdapter) {
		this.sPIIoTConnectorAdapter = sPIIoTConnectorAdapter;
	}
	
	public void socketAndInBufferDestroy(Socket aClientSocket, BufferedReader aInBuffer) throws IOException{
		if(aInBuffer != null){
			aInBuffer.close();
		}
		if(aClientSocket != null){
			aClientSocket.close();
		}
	}
}
