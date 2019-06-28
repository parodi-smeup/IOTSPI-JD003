package com.smeup.iotspi.jd003;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import com.smeup.rpgparser.CommandLineProgram;
import com.smeup.rpgparser.RunnerKt;
import com.smeup.rpgparser.jvminterop.JavaSystemInterface;

import Smeup.smeui.iotspi.datastructure.interfaces.SezInterface;
import Smeup.smeui.iotspi.datastructure.interfaces.SubConfInterface;
import Smeup.smeui.iotspi.datastructure.interfaces.SubInterface;
import Smeup.smeui.iotspi.datastructure.interfaces.SubMessageInterface;
import Smeup.smeui.iotspi.datastructure.iotconnector.IoTConnectorConf;
import Smeup.smeui.iotspi.datastructure.iotconnector.IoTConnectorInput;
import Smeup.smeui.iotspi.datastructure.iotconnector.IoTConnectorResponse;
import Smeup.smeui.iotspi.interaction.SPIIoTConnectorAdapter;

public class Jd003Plugin extends SPIIoTConnectorAdapter {
	
	private IoTConnectorConf connectorConf = null;
	private String socketPort = null;
	
	private final String RPG_FILENAME = "JD_003.rpgle";
	private String rpgSourceName = null;
	private CommandLineProgram commandLineProgram;
	private JavaSystemInterface javaSystemInterface;
	private ByteArrayOutputStream byteArrayOutputStream;
	private PrintStream printStream;
	
	@Override
	public boolean postInit(SezInterface sezInterface, IoTConnectorConf configuration) {
		
		String logMsg = "Called post-init " + getClass().getName() + "(listeners: " + this.getListenerList().size() + ")";;
		log(0, logMsg);
		System.out.println(logMsg);
		
		// sezInterface not used because cabled response
		connectorConf = configuration;		
		
		// To handle system.out response
		byteArrayOutputStream = new ByteArrayOutputStream();
		printStream = new PrintStream(byteArrayOutputStream);
		
		// load Jd_url commandLineProgram (a java programm called as an RPG from an interpreted
		// RPG)
		javaSystemInterface = new MyJavaSystemInterface(printStream);
		javaSystemInterface.addJavaInteropPackage("com.smeup.jd");
		
		// Read variables CNFSEZ from script SCP_SET.LOA38_JD1
		if (configuration != null) {
			socketPort = connectorConf.getData("Port");
			logMsg = "Selected port: "+ socketPort;
			log(0, logMsg);
			System.out.println(logMsg);
			
			rpgSourceName = configuration.getData("RpgSources").trim() + RPG_FILENAME;
			logMsg = "Selected rpgSourceName: "+ rpgSourceName;
			log(0, logMsg);
			System.out.println(logMsg);
		}

		// Read variables SUBVAR from script SCP_SET.LOA38_JD1
		String a37tags = readSubVars(configuration);
			
		// Inizialize and call RPG Parser
		commandLineProgram = RunnerKt.getProgram(rpgSourceName, javaSystemInterface);
		commandLineProgram.setTraceMode(true);
		
		String response = null;
		List<String> parms = new ArrayList<String>();
		// Call JD003 A37TAGS method
		parms.add("INZ");
		parms.add("A37TAGS");
		parms.add(a37tags);
		parms.add("");
		response = callProgram(parms);
		System.out.println("Response A37TAGS RPG method: " +response);
		
		parms.clear();
		
		// Call JD003 POSTINIT method
		parms.add("INZ");
		parms.add("POSTINIT");
		parms.add(socketPort);
		parms.add("");
		response = callProgram(parms);
		
		logMsg = "Program " + RPG_FILENAME + " exited with response: " + response;
		log(0, logMsg);
		System.out.println(logMsg);
		
		if(response.equals("")) {
			return false;
		}else {
			return true;				
		}
	}
	
	/* Method to create a big string with all information of SUBVARS
	 * 
	 * idSub@valueName1{tagName1[valueTag1]tagname2[valueTag2].....}|
	 * ....
	 * valuename2{tagName1[valueTag1]tagname2[valueTag2].....}
	*/
	private String readSubVars(IoTConnectorConf configuration) {
		
		ArrayList<SubInterface> subList = configuration.getSubList();
		
		// This plug-in implements only ONE Sub. (get(0))
		SubInterface sub = subList.get(0);
		SubConfInterface subConf = sub.getConf();
		String subId = sub.getId();		
		
		// Table of all plugin-in tabella di tutte le variabili del plug-in
		ArrayList<Hashtable<String, String>> subVarTable = subConf.getConfTable();
		StringBuilder a37tags = new StringBuilder();
		
		a37tags.append(subId + "@");
		
		for (int i = 0; i < subVarTable.size(); i++) {
			
			a37tags.append(subVarTable.get(i).get("Name"));
			a37tags.append("{");
			a37tags.append(createValueString("Txt", subVarTable.get(i).get("Txt")));
			a37tags.append(createValueString("TpDato", subVarTable.get(i).get("TpDato")));
			a37tags.append(createValueString("TpVar", subVarTable.get(i).get("TpVar")));
			a37tags.append(createValueString("DftVal", subVarTable.get(i).get("DftVal")));
			a37tags.append(createValueString("HowRead", subVarTable.get(i).get("HowRead")));
			a37tags.append(createValueString("IO", calcolateIOVars(sub,subVarTable.get(i).get("Name"))));
			a37tags.append("}");
			if (i != subVarTable.size()-1) {
				a37tags.append("|");
			}
		}
		
		String logMsg = "a37tags: " + a37tags.toString();
		log(0, logMsg);
		System.out.println(logMsg);

		return a37tags.toString();
	}
	
	// Calcolate if SUBVAR in MSGVAR is input or output
	private String calcolateIOVars(SubInterface sub, String name) {
	
		SubMessageInterface subMsg = sub.getMessage();
		ArrayList<Hashtable<String, String>> msgVarTable = subMsg.getConfTable();
		String msgIO = "";
		
		for (int i = 0; i < msgVarTable.size(); i++) {
				String msgNome = msgVarTable.get(i).get("Name");
				if (msgNome.equals(name)) {
					msgIO = msgVarTable.get(i).get("IO");
					break;
				}
		}
		return msgIO;
	}
	
	// For create a name[value] string
	private String createValueString(String name, String value) {
		return name + "[" + value + "]";
	}
	
	private String callProgram(final List<String> parms) {
		String logMsg = "Calling " + rpgSourceName + " with " + parms.size() + " parms: " + String.join(",", parms);
		log(0, logMsg);
		System.out.println(logMsg);
		
		commandLineProgram.singleCall(parms);
		String response = new String(byteArrayOutputStream.toByteArray(), StandardCharsets.UTF_8);
		byteArrayOutputStream.reset();
		
		return response;
	}
	
	@Override
	public IoTConnectorResponse invoke(IoTConnectorInput aDataTable) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean unplug() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean ping() {
		// TODO Auto-generated method stub
		return false;
	}
	
}
