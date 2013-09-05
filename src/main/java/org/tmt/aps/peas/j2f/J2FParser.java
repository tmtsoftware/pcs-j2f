package org.tmt.aps.peas.j2f;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class J2FParser {

	// parses an input fortran file and returns a function descriptor structure

	public FunctionDescriptor parse(String inputFileName) {

		try {
			// open input file and read xml block into buf
			BufferedReader in = new BufferedReader(
					new FileReader(inputFileName));
			String line;
			StringBuffer buf = new StringBuffer("<?xml version=\"1.0\" encoding=\"us-ascii\"?>");
			boolean inBlock = false;
			while ((line = in.readLine()) != null) {

				if (line.indexOf("<function>") >= 0) {
					inBlock = true;
				}
				if (inBlock) {
					// strip off the comments
					buf.append(line.substring(1) + "\n");
				}
				if (line.indexOf("</function>") >= 0) {
					break;
				}
			}
			in.close();

			// TODO: parse out function and args, constructing the
			// FunctionDescriptor
			
			Document doc = createDocument(buf.toString());
			
			FunctionDescriptor fd = createFunctionDescriptor(doc);
			
			return fd;
			
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

	public Document createDocument(String xmlString) {
		boolean validate = false;

		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setValidating(validate);
		dbf.setNamespaceAware(true);
		dbf.setIgnoringElementContentWhitespace(true);

		Document doc = null;
		try {
			DocumentBuilder builder = dbf.newDocumentBuilder();
			doc = builder.parse(new InputSource(new StringReader(xmlString)));
		} catch (Exception e) {
			System.out.println("" + e);
			e.printStackTrace();
		}
		return doc;
	}

	
  	private FunctionDescriptor createFunctionDescriptor(Document doc) {
  		FunctionDescriptor fd = new FunctionDescriptor();
  		
  		// 
  		Element function = (Element)doc.getElementsByTagName("function").item(0);
  		fd.setFunctionName(getTagValue(function, "function-name"));
  		fd.setAuthor(getTagValue(function, "author"));
  		fd.setFunctionShortDesc(getTagValue(function, "function-short-desc"));
  		fd.setFunctionLongDesc(getTagValue(function, "function-long-desc"));
  		
  		
  		// workflow data fields
  		List<ArgumentDescriptor> argDescs = new ArrayList<ArgumentDescriptor>();
  		Element argsElement = (Element)function.getElementsByTagName("args").item(0);
  		NodeList argNodeList = function.getElementsByTagName("arg");
 		for (int i=0; i<argNodeList.getLength(); i++) {
 			ArgumentDescriptor argDesc = new ArgumentDescriptor();
 			Element argElement = (Element)argNodeList.item(i);
 			argDesc.setArgName(getTagValue(argElement, "arg-name"));
 			argDesc.setArgShortDesc(getTagValue(argElement, "arg-short-desc"));
 			argDesc.setArgLongDesc(getTagValue(argElement, "arg-long-desc"));
 			argDesc.setArgDataType(getTagValue(argElement, "arg-data-type"));
 			argDesc.setArgIsArray(getTagValue(argElement, "arg-is-array").equals("TRUE"));
 			argDesc.setArgInOut(getTagValue(argElement, "arg-in-out"));
 			argDesc.setArgUnits(getTagValue(argElement, "arg-units"));
 			argDescs.add(argDesc);
 		}
  		fd.setFunctionArgs(argDescs);
  		
  		return fd;
  	}
  	
 	// returns the 'terminal' tag's value - if it is terminal
  	private String getTagValue(Element parent, String tagName) {
  		Node tag = parent.getElementsByTagName(tagName).item(0);
  		
  		if (tag == null || tag.getFirstChild() == null) {
  			return "";
  		}
  		String value = tag.getFirstChild().getNodeValue();
  		return value;
  	}

	
}
