package among.controller;
import java.io.File;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;

public class ParameterWrapper {
	
	 HashMap<Integer, Object> allParams;

	    public HashMap<Integer, Object> readParams(String FileName) {

	        try {

	            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
	            Document doc = docBuilder.parse(new File(FileName));

	            doc.getDocumentElement().normalize();
	            System.out.println("Root element of the doc is " + doc.getDocumentElement().getNodeName());

	            NodeList listOfParameters = doc.getElementsByTagName("parameter");
	            int totalParameters = listOfParameters.getLength();
	            System.out.println("Total no of parameters : " + totalParameters);

	             allParams = new HashMap<Integer, Object>();

	            for (int s = 0; s < listOfParameters.getLength(); s++) {

	                Node firstParamNode = listOfParameters.item(s);

	                NamedNodeMap attributes = firstParamNode.getAttributes();

	                HashMap<String, String> params = new HashMap<String, String>();

	                for (int t = 0; t < attributes.getLength(); t++) {
	                    Node theAttribute = attributes.item(t);
	                    params.put(theAttribute.getNodeName(), theAttribute.getNodeValue());

	                }

	                allParams.put(s,params);

	            }

	        } catch (
	                SAXParseException err) {
	            System.out.println("** Parsing error" + ", line " + err.getLineNumber() + ", uri " + err.getSystemId());
	            System.out.println(" " + err.getMessage());

	        } catch (
	                SAXException e) {
	            Exception x = e.getException();
	            ((x == null) ? e : x).printStackTrace();

	        } catch (Throwable t) {
	            t.printStackTrace();

	        }

	        return allParams;

	    }

}



