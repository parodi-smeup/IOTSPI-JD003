package com.smeup.IOTSPI_VegaANPR;

import org.dom4j.Document;

public class VegaANPRDataDocumentEvent {
	
	Document iDoc;
	
	public void setDataDocument(Document aDoc){
		this.iDoc = aDoc;
	}
	
	public Document getDataDocument(){
		return this.iDoc;
	}
}
