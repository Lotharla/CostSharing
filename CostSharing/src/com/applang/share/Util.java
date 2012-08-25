package com.applang.share;

import java.util.*;

public class Util 
{
	/**
	 * @param <T>	genericized return type
	 * @param defaultParam	the value returned if the indexed value doesn't exist in the <code>Object</code> array
	 * @param index	the <code>int</code> indicating the value in the <code>Object</code> array
	 * @param params	the optional array of <code>Object</code> values
	 * @return	if the value is not existing <code>null</code> is returned
	 * @throws ClassCastException
	 */
	@SuppressWarnings("unchecked")
	public static <T extends Object> T param(T defaultParam, int index, Object... params) throws ClassCastException {
		if (params != null && index > -1 && params.length > index)
			try {
				T returnValue = (T)params[index];
				return returnValue;
			} catch (ClassCastException e) {}

		return defaultParam;
	}
	
	public static Map<String, Object> namedParams(Object... params) {
		HashMap<String, Object> map = new HashMap<String, Object>();
		
		for (int i = 0; i < params.length; i++) {
			Object param = params[i];
			if (param instanceof String && embedsLeft(param.toString(), "=")) {
				String[] sides = param.toString().split("=", 2);
				if (sides.length > 0) {
					if (sides.length > 1) 
						map.put(sides[0], sides[1]);
					else
						map.put(sides[0], "");
					
					continue;
				}
			}
			
			map.put("param" + i, param);
		}
		
		return map;
	}
	
	@SuppressWarnings("unchecked")
	public static <T extends Object> T namedValue(T defaultValue, String name, Map<String, Object> map) throws ClassCastException {
		if (map.containsKey(name)) 
			try {
				T returnValue = (T)map.get(name);
				return returnValue;
			} catch (ClassCastException e) {}
		
		return defaultValue;
	}
	
	public static Boolean namedBoolean(Boolean defaultValue, String name, Map<String, Object> map) {
		return parseBoolean(defaultValue, namedValue((String)null, name, map));
	}
	
	public static Integer namedInteger(Integer defaultValue, String name, Map<String, Object> map) {
		return parseInteger(defaultValue, namedValue((String)null, name, map));
	}
	
	public static Double namedDouble(Double defaultValue, String name, Map<String, Object> map) {
		return parseDouble(defaultValue, namedValue((String)null, name, map));
	}
	/**
	 * @param <T>	type of the given array
	 * @param <U>	type of the cast array
	 * @param array	the given array
	 * @param a	prototype of the cast array
	 * @return	the cast array
	 */
	public static <T, U> U[] arraycast(T[] array, U[] a) {
		return Arrays.asList(array).toArray(a);
	}

	public static Object[] chain(Object head, Object... tail) {
		Object[] array = new Object[1 + tail.length];
		array[0] = head;
		System.arraycopy(tail, 0, array, 1, tail.length);
		return array;
	}

	@SuppressWarnings("unchecked")
	public static <T extends Object> T valueOrElse(T elseValue, Object value) {
		if (value != null)
			try {
				return (T)value;
			} catch (Exception e) {}

		return elseValue;
	}

	public static Integer parseInteger(Integer defaultValue, String string) {
		try {
			return Integer.parseInt(string.trim());
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static Boolean parseBoolean(Boolean defaultValue, String string) {
		try {
			return Boolean.parseBoolean(string.trim());
		} catch (Exception e) {
			return defaultValue;
		}
	}

	public static Double parseDouble(Double defaultValue, String string) {
		try {
			Double d = Double.valueOf(string);
			if (Double.isNaN(d))
				return defaultValue;
			else
				return d;
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static <T> boolean isAvailable(int i, T[] array) {
		return array != null && i > -1 && 
			i < array.length && 
			array[i] != null;
	}

	public static <T> boolean isNullOrEmpty(T[] array) {
		return array == null || array.length < 1;
	}

	public static boolean notNullOrEmpty(String value) {
		return value != null && value.length() > 0;
	}

	public static boolean notNullOrEmptyTrimmed(String value) {
		return value != null && value.trim().length() > 0;
	}

	@SuppressWarnings("unchecked")
	public static <T> String join(String delimiter, T... params) {
	    StringBuilder sb = new StringBuilder();
	    Iterator<T> iter = new ArrayList<T>(Arrays.asList(params)).iterator();
	    if (iter.hasNext())
	        do {
		        sb.append(String.valueOf(iter.next()))
		        	.append(iter.hasNext() ? delimiter : "");
		    }
		    while (iter.hasNext());
	    return sb.toString();
	}

    public static String[] split(String text, String expression) {
        if (text.length() == 0) {
            return new String[]{};
        } else {
            return text.split(expression, -1);
        }
    }

    public static String textFromHtml(String html) {
        return html.replaceAll("(?i)\\<br[/]?>", "\n").replaceAll("\\<.*?>", "");
    }

    public static boolean embedsLeft(String whole, String part) {
		return whole != null && whole.contains(part) && !whole.startsWith(part);
	}

    public static boolean embedsRight(String whole, String part) {
		return whole != null && whole.contains(part) && !whole.endsWith(part);
	}

    public static boolean embeds(String whole, String part) {
		return whole != null && embedsLeft(whole, part) && embedsRight(whole, part);
	}

    public static String enclose(String pad, String string, Object... params) {
    	pad = valueOrElse("", pad);
    	string = pad.concat(valueOrElse("", string));
    	if (params.length < 1)
    		return string.concat(pad);
    	
    	for (int i = 0; i < params.length; i++) 
    		string = string.concat(param("", i, params));
		
    	return string;
	}

    public static String strip(boolean atStart, String string, Object... params) {
    	for (int i = 0; i < params.length; i++) {
    		String pad = param("", i, params);
	    	if (atStart && string.startsWith(pad))
	    		string = string.substring(pad.length());
	    	if (!atStart && string.endsWith(pad))
	    		string = string.substring(0, string.length() - pad.length());
    	}
		
    	return string;
	}
    
    public static <T> boolean areMembersDistinct(Collection<T> target) {
    	return target.size() == new HashSet<T>(target).size();
    }
    
    public interface Job {
		public void dispatch(Object[] params) throws Exception;
	}
    
    public interface Predicate<T> { boolean apply(T type); }

	public static <T> Collection<T> filter(Collection<T> target, boolean negate, Predicate<T> predicate) {
        Collection<T> result = new ArrayList<T>();
        for (T element: target) {
            boolean apply = predicate.apply(element);
			if (apply) {
				if (!negate)
	                result.add(element);
			}
			else if (negate)
                result.add(element);
        }
        return result;
    }
	
	public static Predicate<String> isKitty = new Predicate<String>() {
		public boolean apply(String type) {
			return kitty.equals(type);
		}
	};
	
	public static String kitty = "KITTY";

	/**
	 * no discussion about that amount of currency
	 */
	public static double minAmount = 0.01;

	/**
	 * accuracy of calculations
	 */
	public static double delta = 0.00001;
	
	public static String placeholderIndicator = "_";
    
    public static String placeholder(Integer... nums) {
		return placeholderIndicator.concat(placeholderIndicator).concat(placeholderIndicator) + join(placeholderIndicator, nums);
    }
	
	public static boolean isValidName(String name) {
		if (notNullOrEmpty(name)) {
			boolean noOps = true;
			for (String op : ShareMap.policyOperators)
				noOps &= !name.contains(op);
			
			return noOps && 
					!name.startsWith(placeholderIndicator);
		}
		else
			return false;
	}

	public static List<String> makeList(String head, String... tail) {
		ArrayList<String> list = new ArrayList<String>();
		list.add(head);
		for (String element : tail) 
			if (!head.equals(element))
				list.add(element);
		return list;
	}
	
}
