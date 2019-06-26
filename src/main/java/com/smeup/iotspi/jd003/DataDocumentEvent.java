package com.smeup.iotspi.jd003;

import org.dom4j.Document;

public class DataDocumentEvent {
	
	Document iDoc;
	
	public void setDataDocument(Document aDoc){
		this.iDoc = aDoc;
	}
	
	public Document getDataDocument(){
		return this.iDoc;
	}
}
