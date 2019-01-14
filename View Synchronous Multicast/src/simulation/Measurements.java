package simulation;

import java.io.*;
import java.text.*;
import java.util.*;

public class Measurements {
	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";
	private static final DateFormat sdf = new SimpleDateFormat("dd-MM-yyyy HH-mm-ss");

	FileWriter file;
	String filename = null;
	private Date fileTimestamp;

	public Measurements(int nodeID) {
		fileTimestamp = new Date();
		filename = new String(nodeID + " " + sdf.format(fileTimestamp) + ".csv");
		System.out.println(filename);
		try {
			file = new FileWriter(filename);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	public FileWriter getFile() {
		return file;
	}
	public void setFile(FileWriter file) {
		this.file = file;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public Date getFileTimestamp() {
		return fileTimestamp;
	}
	public void setFileTimestamp(Date fileTimestamp) {
		this.fileTimestamp = fileTimestamp;
	}
	public void closeFile() {
		try {
			file.flush();
			file.close();
		} catch (IOException e) {
			System.out.println("Error while flushing/closing file.");
			e.printStackTrace();
		}

	}
	

}
