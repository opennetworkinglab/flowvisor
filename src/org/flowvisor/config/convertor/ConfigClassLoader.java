package org.flowvisor.config.convertor;

import java.io.*;

public class ConfigClassLoader extends ClassLoader {

  private String root;
  private static String path = "org.flowvisor.config.convertor.";

  public ConfigClassLoader (String rootDir) {
	  super();
    if (rootDir == null)
      throw new IllegalArgumentException ("Null root directory");
    root = rootDir;
  }

  protected Class<?> loadClass (String name, boolean resolve) 
    throws ClassNotFoundException {
	 
	  /*
	   * All convertor classes are stored within this package
	   * But the xml config file expects them to be in 
	   * org.flowvisor.config, so we must ignore this 
	   * package definition and load the class locally.
	   * 
	   */
	
	  
	  String[] filename = name.split("\\.");
	  
	  String toLoad = "";
	  if (filename.length == 0)
		  return null;
	  
	  if (!filename[0].equals("java") && filename[filename.length - 1].startsWith("Conf")) {
		  toLoad = path + filename[filename.length - 1];
		  System.err.println("Renamed and loading " + toLoad);
	  } else {
		  System.err.println("Loading " + name );
		  toLoad = name;
	  }
	  
	  Class<?> c = findLoadedClass(toLoad);
	  System.err.println("Attempt to find loaded class");
	  
	  try {
		  c = findSystemClass(toLoad);
		  System.err.println("Attempt to find System class");
	  } catch (Exception e) {
		 e.printStackTrace();
	  }
	  
	  
	  if (c == null) {
		  String theclazz = name.replace ('.', File.separatorChar) + ".class";
	    try {
	    	
	    	 byte data[] = loadClassData(theclazz);
	         c = defineClass (toLoad, data, 0, data.length);
	         if (c == null)
	           throw new ClassNotFoundException (toLoad);
	
	    } catch (IOException e) {
	        throw new ClassNotFoundException ("Error reading file: " + theclazz);
	    }
	  }
	
	  if (resolve)
	      resolveClass (c);
	  
	  System.err.println(toLoad + " loaded " + c);

	  return c;
  	}
  
  
  private byte[] loadClassData (String filename) 
      throws IOException {

    // Create a file object relative to directory provided
    File f = new File (root, filename);

    // Get size of class file
    int size = (int)f.length();

    // Reserve space to read
    byte buff[] = new byte[size];

    // Get stream to read from
    FileInputStream fis = new FileInputStream(f);
    DataInputStream dis = new DataInputStream (fis);

    // Read in data
    dis.readFully (buff);

    // close stream
    dis.close();

    // return data
    return buff;
  }
}
