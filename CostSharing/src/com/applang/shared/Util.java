package com.applang.shared;

import java.io.*;
import java.text.SimpleDateFormat;
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
			return Double.valueOf(string);
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	public static <T> boolean isAvailable(int i, T[] array) {
		return array != null && 
				i > -1 && 
				i < array.length && 
				array[i] != null;
	}

	public static <T> boolean isNullOrEmpty(T[] array) {
		return array == null || array.length < 1;
	}

	public static boolean notNullOrEmpty(String value) {
		return value != null && value.length() > 0;
	}

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

	public static double delta = 0.00001;
	
	public static String placeholderIndicator = "_";
    
    public static String placeholder(Integer... nums) {
		return placeholderIndicator.concat(placeholderIndicator).concat(placeholderIndicator) + join(placeholderIndicator, nums);
    }
	
	public static boolean isValidName(String name) {
		return notNullOrEmpty(name) && !name.startsWith(placeholderIndicator) && !name.contains("=");
	}

}
