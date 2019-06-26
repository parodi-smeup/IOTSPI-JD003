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

public class Jd_rcvsck implements Program {

	private List<ProgramParam> parms;
	private String iError;

	public Jd_rcvsck() {
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

	private String listenSocket(final int port) {
		String responseAsString = "";
		try (ServerSocket serverSocket = new ServerSocket(port)) {
			System.out.println("Socket listening on port " + port + "...");
			Socket socket = serverSocket.accept();
			System.out.println("Client connected");
			InputStream input = socket.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(input));
			responseAsString = reader.readLine();
			System.out.println("Client content written: " + responseAsString);
			socket.close();
			System.out.println("Socket closed");
		} catch (IOException e) {
			e.printStackTrace();
			responseAsString = "*ERROR " + e.getMessage();
			iError = "1";
		}
		return responseAsString;
	}

	@Override
	public List<ProgramParam> params() {
		return parms;
	}

	@Override
	public List<Value> execute(SystemInterface arg0, LinkedHashMap<String, Value> arg1) {
		ArrayList<Value> arrayListResponse = new ArrayList<Value>();
		
		String response = "";
		int bufferLength = 0;
		iError = "";
		
		for (Map.Entry<String, ? extends Value> entry : arg1.entrySet()) {
			
			String parmName = entry.getKey().toString();
			
			switch(parmName) {
			case "ADDRSK":
				String p = entry.getValue().asString().getValue();
				int port = Integer.parseInt(p.trim());
				response = listenSocket(port);
				bufferLength = response.trim().length();
				arrayListResponse.add(entry.getValue());
				break;
			case "BUFFER":
				arrayListResponse.add(new StringValue(response.trim()));
				break;
			case "BUFLEN":
				arrayListResponse.add(new StringValue(String.valueOf(bufferLength)));
				break;
			case "IERROR":
				arrayListResponse.add(new StringValue(iError));
				break;
			}
		}
		return arrayListResponse;
	}

}
