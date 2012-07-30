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

	public static Integer parseInt(Integer defaultValue, String string) {
		try {
			return Integer.parseInt(string.trim());
		} catch (NumberFormatException e) {
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
		return whole.contains(part) && !whole.startsWith(part);
	}

    public static boolean embedsRight(String whole, String part) {
		return whole.contains(part) && !whole.endsWith(part);
	}

    public static boolean embeds(String whole, String part) {
		return embedsLeft(whole, part) && embedsRight(whole, part);
	}

	/**
	 * no discussion about that amount of currency
	 */
	public static double minAmount = 0.01;

	/**
	 * accuracy of calculations
	 */
	public static double delta = 0.00001;
	
	public static String tableName = "KITTY";
	
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
					!name.startsWith(placeholderIndicator) && 
					!name.equals(tableName);
		}
		else
			return false;
	}

	public static String[] otherNamesAfter(String first, String... names) {
		ArrayList<String> list = new ArrayList<String>();
		list.add(first);
		for (String name : names) 
			if (!first.equals(name))
				list.add(name);
			
		return list.toArray(names);
	}
	
}
