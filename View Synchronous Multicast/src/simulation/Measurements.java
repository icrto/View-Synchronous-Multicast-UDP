package simulation;

import java.io.*;
import java.text.*;
import java.util.*;

public class Measurements {
	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	File file;
	FileWriter writer;
	String filename = null;
	private long init;
	private long finish;

	public Measurements(String filePath, int nodeID, int nrNodes, int nrStableMsgs, int nrNonStableMsgs) throws IOException{

		filename = new String(nodeID + "_" + nrNodes + " " + nrStableMsgs + ".csv");
		try {
			file = new File(filePath, filename);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		if(!file.exists()) {

			file.createNewFile();
			writer = new FileWriter(file, true);
			writer.write(String.valueOf(nrNonStableMsgs));
			writer.flush();
		}
		else {
			writer = new FileWriter(file, true);
		}
		String aux;
		String []contents = null;
		Scanner scan = new Scanner(file);
		
		//go to last line, get its first number (indicating nrNonStableMsgs -> script loop iteration count)
		while(scan.hasNext()) { //has lines
			aux = scan.nextLine();
			contents = aux.split(COMMA_DELIMITER);
		}
		if(nrNonStableMsgs != Integer.parseInt(contents[0])){ //if nrNonStableMsgs changed it's time to add new line
			writer.write(NEW_LINE_SEPARATOR);
			writer.write(String.valueOf(nrNonStableMsgs));
			writer.write(COMMA_DELIMITER);
			writer.flush();
		}

	}
	public File getFile() {
		return file;
	}
	public void setFile(File file) {
		this.file = file;
	}
	public String getFilename() {
		return filename;
	}
	public void setFilename(String filename) {
		this.filename = filename;
	}
	public long getInit() {
		return init;
	}
	public void setInit(long init) {
		this.init = init;
	}
	public long getFinish() {
		return finish;
	}
	public void setFinish(long finish) {
		this.finish = finish;
		writeMeasure();
	}
	public void writeMeasure() {
		long measure = this.finish - this.init;
		try {
			writer.write(COMMA_DELIMITER);
			writer.write(String.valueOf(measure));
			writer.flush();
			writer.close();
		//	System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
