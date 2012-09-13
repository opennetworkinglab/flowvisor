package org.flowvisor.config.convertor;

import java.beans.ExceptionListener;
import java.beans.XMLDecoder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Arrays;
import java.util.List;

import org.flowvisor.config.convertor.ConfDirEntry;

import com.google.gson.stream.JsonWriter;

public class Convertor implements ExceptionListener {

	FileInputStream file = null;
	private ConfDirEntry root;
	private String filename  = null;
	private static String tmpfile = "/tmp/convert";
	
	public Convertor(String filename) throws FileNotFoundException {
		file = new FileInputStream(filename);
		this.filename = filename.replace("xml", "json");
	}
	
	public void convert() throws FileNotFoundException {
		
		 try{
			  FileWriter dos = new FileWriter(tmpfile);
			  BufferedWriter bw = new BufferedWriter(dos);
			  DataInputStream in = new DataInputStream(file);
			  BufferedReader br = new BufferedReader(new InputStreamReader(in));
			  String strLine;
			  /*
			   * Since I can't use my own classloader because the SAX parser 
			   * in XMLDecoder does somthing stupid and whats you to load the class
			   * specified in the file and not the same file in another place, I had 
			   * to resort to rewriting parts of the config. Sucks but whatever.
			   *
			   */
			  while ((strLine = br.readLine()) != null)   {
				  bw.write(strLine.replace("config", "config.convertor"));
				  bw.newLine();
			  }
			  bw.close();
			  in.close();
		}catch (Exception e){//Catch exception if any
			  System.err.println("Error: " + e.getMessage());
		}
		
		/*
		 * Can't freakin' use my own class loader because this piece 
		 * of @#!% uses a SAXParser which apparently doesn't care that
		 * load the class for it. XMLDecoder is a steaming pile of 
		 * @#!%.
		 */
		XMLDecoder dec = new XMLDecoder(new FileInputStream(tmpfile), null, this, null /* new ConfigClassLoader(System.getProperty("user.dir") + "/src/")*/);
		this.root = (ConfDirEntry) dec.readObject();
		
		
		try {
			JsonWriter writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filename))));
			ConfigJson walker = new ConfigJson(writer);
			writer.setIndent("    ");
			writer.beginObject();
			walk(walker);
			walker.write();
			writer.endObject();
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		File f = new File(tmpfile);
		f.delete();
		
	}
	
	public void walk(ConfigIterator walker) {
        walksubdir("", root, walker);
	}
	
	
	private ConfigEntry lookup(String name) {
        List<String> parts = Arrays.asList(name.split("!"));
        ConfigEntry ret = null;
        ConfDirEntry base = this.root;
        for (String part : parts) {
                if (base == null)
                        break;
                ret = base.lookup(part);
                if (ret == null)
                        break;
                if (ret.getType() == ConfigType.DIR)
                        base = (ConfDirEntry) ret;
                else
                        base = null;
        }
        return ret;
}
	
	 public void walksubdir(String base, ConfigIterator walker) {
         ConfigEntry e = lookup(base);
         walksubdir(base, e, walker);
	 }

	 private void walksubdir(String base, ConfigEntry e,
                 ConfigIterator walker) {
         if (e.getType() == ConfigType.DIR) {
                 ConfDirEntry dir = (ConfDirEntry) e;
                 for (ConfigEntry entry : dir.listEntries())
                         walksubdir(base + "!" + entry.getName(), entry, walker);
         } else
                 walker.visit(base, e);

 }
	
	/**
	 * @param args
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws FileNotFoundException {
		Convertor conv = new Convertor(args[0]);
		conv.convert();
	}

	@Override
	public void exceptionThrown(Exception e) {
		System.err.println("XMLDecoder is doing something stupid!");
		e.printStackTrace();
		System.exit(1);
		
	}

}
