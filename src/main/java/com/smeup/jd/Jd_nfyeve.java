package com.smeup.jd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;

import com.smeup.iotspi.jd003.EventComponent;
import com.smeup.iotspi.jd003.DataDocumentEvent;
import com.smeup.iotspi.jd003.DataDocumentListenerInterface;
import com.smeup.iotspi.jd003.DocumentCreator;
import com.smeup.rpgparser.interpreter.Program;
import com.smeup.rpgparser.interpreter.ProgramParam;
import com.smeup.rpgparser.interpreter.StringType;
import com.smeup.rpgparser.interpreter.StringValue;
import com.smeup.rpgparser.interpreter.SystemInterface;
import com.smeup.rpgparser.interpreter.Value;

import Smeup.smeui.iotspi.datastructure.interfaces.SezInterface;
import Smeup.smeui.iotspi.datastructure.iotconnector.IoTConnectorConf;
import Smeup.smeui.iotspi.datastructure.iotconnector.IoTConnectorInput;
import Smeup.smeui.iotspi.datastructure.iotconnector.IoTConnectorResponse;
import Smeup.smeui.iotspi.interaction.SPIIoTConnectorAdapter;
import Smeup.smeui.iotspi.interaction.SPIIoTEvent;

public class Jd_nfyeve extends SPIIoTConnectorAdapter implements Program, DataDocumentListenerInterface {

	private List<ProgramParam> parms;
	private Map<String, EventComponent> eventList = new HashMap<>();
	private String a37SubId;

	public Jd_nfyeve() {
		parms = new ArrayList<ProgramParam>();
		// Sme.UP Function
		parms.add(new ProgramParam("§§FUNZ", new StringType(15)));
		// Sme.UP Method
		parms.add(new ProgramParam("§§METO", new StringType(30000)));
		// XML from camera
		parms.add(new ProgramParam("§§SVAR", new StringType(210000)));
		// List of A37 attributes from script
		parms.add(new ProgramParam("A37TAGS", new StringType(4096)));
	}

	private String notifyEvent(final String xml) {
		String responseAsString = "";
		DocumentCreator reader = new DocumentCreator(xml.trim());
		reader.addDataDocumentEventListener(this);
		reader.start();
		
		//TODO remove sleep...
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
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

		for (Map.Entry<String, ? extends Value> entry : arg1.entrySet()) {

			String parmName = entry.getKey().toString();

			switch (parmName) {
			case "§§FUNZ":
				arrayListResponse.add(entry.getValue());
				break;
			case "§§METO":
				arrayListResponse.add(entry.getValue());
				break;
			case "§§SVAR":
				final String xml = entry.getValue().asString().getValue();
				response = notifyEvent(xml);
				arrayListResponse.add(new StringValue(response.trim()));
				break;
			case "A37TAGS":
				final String a37tags = entry.getValue().asString().getValue();
				extractTags(a37tags);
				arrayListResponse.add(entry.getValue());
				break;
			}
		}
		return arrayListResponse;
	}

	private Map<String, EventComponent> extractTags(final String a37tags) {

		setA37SubId(a37tags.split("@")[0]);
		final String[] rows = a37tags.split("@")[1].split("\\|");

		for (String row : rows) {
			// name
			String name = row.split("\\{")[0];

			// attributes of name var
			String nameAttributes = row.split("\\{")[1];

			// assume 6 attributes
			String txt_keyValue = nameAttributes.split("\\]")[0];
			String txt_key = txt_keyValue.split("\\[")[0];
			String txt_value = "";
			if (txt_keyValue.split("\\[").length > 1) {
				txt_value = txt_keyValue.split("\\[")[1];
			}
			
			String tpDato_keyValue = nameAttributes.split("\\]")[1];
			String tpDato_key = tpDato_keyValue.split("\\[")[0];
			String tpDato_value = "";
			if (tpDato_keyValue.split("\\[").length > 1) {
				tpDato_value = tpDato_keyValue.split("\\[")[1];
			}
			
			String tpVar_keyValue = nameAttributes.split("\\]")[2];
			String tpVar_key = tpVar_keyValue.split("\\[")[0];
			String tpVar_value = "";
			if (tpVar_keyValue.split("\\[").length > 1) {
				tpVar_value = tpVar_keyValue.split("\\[")[1];
			}

			String dftVal_keyValue = nameAttributes.split("\\]")[3];
			String dftVal_key = dftVal_keyValue.split("\\[")[0];
			String dftVal_value = "";
			if (dftVal_keyValue.split("\\[").length > 1) {
				dftVal_value = dftVal_keyValue.split("\\[")[1];
			}

			String howRead_keyValue = nameAttributes.split("\\]")[4];
			String howRead_key = howRead_keyValue.split("\\[")[0];
			String howRead_value = "";
			if (howRead_keyValue.split("\\[").length > 1) {
				howRead_value = howRead_keyValue.split("\\[")[1];
			}

			String iO_keyValue = nameAttributes.split("\\]")[5];
			String iO_key = iO_keyValue.split("\\[")[0];
			String iO_value = "";
			if (iO_keyValue.split("\\[").length > 1) {
				iO_value = iO_keyValue.split("\\[")[1];
			}

			EventComponent eventComponent = new EventComponent(a37SubId);
			eventComponent.setIEventName(name);
			eventComponent.setIDataType(tpDato_value);
			eventComponent.setIType(tpVar_value);
			eventComponent.setIHowRead(howRead_value);
			eventComponent.setIsMsgRet(iO_value == "I" ? true : false);
			eventComponent.setIDftValue(dftVal_value);
			eventComponent.setIEventDesc(txt_value);

			this.eventList.put(name, eventComponent);
		}

		return this.eventList;
	}

	@Override
	public void menageEventDataDocument(DataDocumentEvent e) {
		this.readData(e.getDataDocument());

	}

	public void readData(Document aDoc) {
		try {
			log(0, "Metodo readData - Lettura buffer plugin");
			// Alimento i vari TAG
			for (String vKey : this.eventList.keySet()) {
				EventComponent vEvtComp = this.eventList.get(vKey);
				if (vEvtComp.getIsMsgRet())
					vEvtComp.setIValue(aDoc);
			}

			// Creo evento - di default crea evento con tutte le variabili
			createEvent();
		} catch (Exception vEx) {
			log(0, "Errore metodo readData - " + vEx.getMessage());
		}
	}

	private synchronized void createEvent() {
		try {
			// Crea SPIIOTEvent
			SPIIoTEvent vEvent = new SPIIoTEvent(getA37SubId());
			// Alimentazione struttura Event
			for (String vKey : this.eventList.keySet()) {
				EventComponent vEvtComp = this.eventList.get(vKey);
				// Ritorno solo le variabili non di tipo CMD
				if (vEvtComp.getIsMsgRet())
					vEvent.setData(vKey, vEvtComp.getIValue());
			}
			// invia Evento
			log(0, "invio evento " + vEvent.getDataTable().toString());
			fireEventToSmeup(vEvent);
		} catch (Exception vEx) {
			log(0, "Errore metodo createEvent- " + vEx.getMessage());
		}
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

	@Override
	public boolean postInit(SezInterface aSez, IoTConnectorConf aConfiguration) {
		// TODO Auto-generated method stub
		return false;
	}

	public String getA37SubId() {
		return a37SubId;
	}

	public void setA37SubId(String a37SubId) {
		this.a37SubId = a37SubId;
	}

}
