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
		
		log(0, "Called post-init " + getClass().getName());
		
		// sezInterface not used because cabled response
		connectorConf = configuration;		
		
		// To handle system.out response
		byteArrayOutputStream = new ByteArrayOutputStream();
		printStream = new PrintStream(byteArrayOutputStream);
		
		// load Jd_url commandLineProgram (a java programm called as an RPG from an interpreted
		// RPG)
		javaSystemInterface = new JavaSystemInterface(printStream);
		javaSystemInterface.addJavaInteropPackage("com.smeup.jd");
		
		// Read variables CNFSEZ from script SCP_SET.LOA38_JD1
		if (configuration != null) {
			socketPort = connectorConf.getData("Port");
			rpgSourceName = configuration.getData("RpgSources").trim() + RPG_FILENAME;
			log(0, "Selected port: "+ socketPort);
			log(0, "Selected rpgSourceName: "+ rpgSourceName.trim());
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
		
		parms.clear();
		
		// Call JD003 POSTINIT method
		parms.add("INZ");
		parms.add("POSTINIT");
		parms.add(socketPort);
		parms.add("");
		response = callProgram(parms);
		
		log(0, "Program " + RPG_FILENAME + " exited with response: " + response);
		
		if(response.equals("")) {
			return false;
		}else {
			return true;				
		}
	}
	
	private String readSubVars(IoTConnectorConf configuration) {
		
		Hashtable<String,EventComponent> iEventList = new Hashtable<String,EventComponent>();
		ArrayList<SubInterface> subList = configuration.getSubList();
		
		// This plug-in implements only ONE Sub. (get(0))
		SubInterface sub = subList.get(0);
		SubConfInterface subConf = sub.getConf();
		String subId = sub.getId();
		
		//tabella di tutte le variabili del plug-in
		ArrayList<Hashtable<String, String>> subVarTable = subConf.getConfTable();
		StringBuilder a37tags = new StringBuilder();
		
		
		for (int i = 0; i < subVarTable.size(); i++) {
			
			a37tags.append(subVarTable.get(i).get("Name"));
			a37tags.append("{");
			a37tags.append(createValueString("Txt", subVarTable.get(i).get("Txt")));
			a37tags.append(createValueString("TpDato", subVarTable.get(i).get("TpDato")));
			a37tags.append(createValueString("TpVar", subVarTable.get(i).get("TpVar")));
			a37tags.append(createValueString("DftVal", subVarTable.get(i).get("DftVal")));
			a37tags.append(createValueString("HowRead", subVarTable.get(i).get("HowRead")));
			a37tags.append("}");
			if (i != subVarTable.size()-1) {
				a37tags.append("|");
			}
		}
		
		return a37tags.toString();
	}
	
	// For create a name[value] string
	private String createValueString(String name, String value) {
		return name + "[" + value + "]";
	}
	
	
	private String callProgram(final List<String> parms) {
		log(0, "Calling " + rpgSourceName + " with " + parms.size() + " parms: " + String.join(",", parms));
		
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
