package simulation;

import java.io.*;
import java.util.*;

public class Measurements {
	private static final String COMMA_DELIMITER = ",";
	private static final String NEW_LINE_SEPARATOR = "\n";

	File file;
	FileWriter writer;
	String filename = null;
	private long init;
	private long finish;
	private int var;
	private int nonvar1;
	private int nonvar2;
	
	public Measurements(String filePath, int nodeID, int nrNodes, int nrStableMsgs, int nrNonStableMsgs, String variable) throws IOException{

		if(variable.equals("STABLE")) {
			var = nrStableMsgs;
			filename = new String(nodeID + "_" + nrNodes + " " + nrNonStableMsgs + ".csv");
		} else if(variable.equals("NON_STABLE")) {
			var = nrNonStableMsgs;
			filename = new String(nodeID + "_" + nrNodes + " " + nrStableMsgs + ".csv");
		} else if(variable.equals("NODES")) {
			var = nrNodes;
			filename = new String(nodeID + "_" + nrStableMsgs + " " + nrNonStableMsgs + ".csv");
		} else {
			System.out.println("Undefined variable");
			System.exit(2);
		}

		try {
			file = new File(filePath, filename);
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		if(!file.exists()) {

			file.createNewFile();
			writer = new FileWriter(file, true);
			writer.write(String.valueOf(var));
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
		if(var != Integer.parseInt(contents[0])){
			writer.write(NEW_LINE_SEPARATOR);
			writer.write(String.valueOf(var));
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
			//System.exit(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
