package org.flowvisor.config;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Takes a string of the form "foo[key1=[val1],key2=[val2],]" are returns a
 * hashmap containg ( key1 => val1, key2 => val2, "ObjectName" => foo)
 *
 * Critically, values are untouched, i.e., if they contain more square brackets
 * and commas, they are unaffected
 *
 * @author capveg
 *
 */

public class BracketParse {
	final public static String OBJECTNAME = "ObjectName";

	/**
	 * Parse a BacketParse encoded line
	 *
	 * @param line
	 * @return null if unparsed, else a hashmap, as above
	 */

	public static HashMap<String, String> decode(String line) {
		HashMap<String, String> map = new LinkedHashMap<String, String>();
		int index = line.indexOf("[");
		if (index < 0)
			return null; // unparsed
		String name = line.substring(0, index);
		String rest = line.substring(index + 1);
		map.put(OBJECTNAME, name);
		int bracketCount = 1;
		index = 0;
		String key = null;
		String value;
		while (index < rest.length()) {
			switch (rest.charAt(index)) {
			case '[':
				if (bracketCount == 1) { // begin of a value
					rest = rest.substring(index + 1);
					index = -1;
				}
				bracketCount++;
				break;
			case ']':
				bracketCount--;
				if (bracketCount == 1) { // end of a value
					value = rest.substring(0, index);
					rest = rest.substring(index + 1);
					index = -1;
					if (key == null)
						return null; // unparsed
					map.put(key, value);
					key = null;
				}
				break;
			case '=':
				if (bracketCount == 1) { // end of a key
					key = rest.substring(0, index);
					rest = rest.substring(index + 1);
					index = -1;
				}
				break;
			case ',':
				if (bracketCount == 1) { // begin of key
					rest = rest.substring(index + 1);
					index = -1;
				}
			} // switch
			index++;
		} // while
		return map;
	}

	public static String encode(Map<String, String> map) {
		if (!map.containsKey(OBJECTNAME))
			return null; // needs to have a OBJECTNAME key
		String base = map.get(OBJECTNAME) + "[";
		for (String key : map.keySet()) {
			if (key.equals(OBJECTNAME))
				continue;
			base += key + "=[" + map.get(key) + "],";
		}
		return base + "]";
	}
}
