package com.applang.shared;

import java.util.*;

/**
 * A <code>TreeMap</code> containing names as keys and sharing amounts as values
 *
 */
@SuppressWarnings("serial")
public class ShareMap extends TreeMap<String, Double> 
{
	public static class ParseException extends Exception {
		public int loc = -1;
		public ParseException(int loc) {
			super(INVALID);
			this.loc = loc;
		}
		public ParseException(String message) {
			super(message);
		}
		public static String INVALID = "invalid sharing policy";
	}
	
	public ShareMap() {}
    /**
	 * creates a sorted map of shares for a number of sharers according to a sharing policy.
	 * Integers separated by colons are interpreted as proportions of shares.
	 * If no proportions are specified uniform sharing is assumed (amount divided by the number of sharers).
	 * @param names	the array of names of the sharers (not necessarily sorted)
	 * @param amount	the value of the amount to share
	 * @param proportions	given proportions for the sharers according to the order of the names
     */
    public ShareMap(String[] names, Double amount, String policy) {
		this(names, amount == null ? 0. : amount.doubleValue());
		
		if (!Util.isNullOrEmpty(names) && (!Util.notNullOrEmpty(policy) || policy.trim().length() < 1)) {
			String[] parts = new String[names.length];
			for (int i = 0; i < parts.length; i++)
				parts[i] = "1";
			policy = Util.join(":", parts);
		}

		try {
			reorganize(amount, policy);
		} catch (ParseException e) {
	    	this.clear();
			return;
		}
		
		renameWith(names);
    }
    
    public void reorganize(Double amount, String policy) throws ParseException {
    	this.clear();
    	
		String[] parts = new String[0];
		List<String> names = new ArrayList<String>();
		if (Util.notNullOrEmpty(policy.trim())) {
			List<String> list = new ArrayList<String>();
			list.addAll(Arrays.asList(policy.split(":|\\n", -1)));
			for (int i = list.size() - 1; i > -1; i--) {
				String part = list.get(i).trim();
				if (part.contains("*")) {
					String[] facts = part.split("\\*");
					Integer factor = Util.parseInt(1, facts[1]);
					if (factor == null || factor < 1) 
						throw new ParseException(1);
					else if (factor > 1) {
						list.remove(i);
						for (int j = factor; j > 0; j--) {
							list.add(i, facts[0]);
							names.add(0, placeholder(1 + i, j));
						}
					}
				}
				else
					names.add(0, placeholder(1 + i));
			}
			parts = list.toArray(parts);
		}
		else {
			parts = new String[] {"1"};
			names.add(placeholder());
		}

		Integer[] proportions = new Integer[parts.length];
		
		for (int i = 0; i < parts.length; i++) {
			String part = parts[i];
			String[] sides = part.split("=", 2);
			boolean twoSides = sides.length > 1;
			part = sides[twoSides ? 1 : 0];
			if (twoSides)
				names.set(i, sides[0].trim());
			
			proportions[i] = Util.parseInt(null, part);
			if (proportions[i] == null) {
				Double value = Util.parseDouble(null, part);
				if (value == null) 
					throw new ParseException(2);
				else
					put(names.get(i), value);
			}
		}
		
		if (!Arrays.asList(proportions).contains(null)) {
			Double[] normalized = normalize(proportions, amount == null ? 1. : -amount.doubleValue());
			for (int i = 0; i < normalized.length; i++)
				put(names.get(i), normalized[i]);
		}
		else
			for (Integer integer : proportions) 
				if (integer != null)
					throw new ParseException(3);
    }
    
    Double[] normalize(Integer[] proportions, double amount) {
    	int n = proportions.length;
    	Double[] normalized = new Double[n];
    	
    	double denominator = 0;
		for (int i = 0; i < n; i++)
			denominator += Math.abs(proportions[i]);
		
		if (denominator > 0) {
			for (int i = 0; i < n; i++) 
				normalized[i] = amount * Math.abs(proportions[i]) / denominator;
		}

		return normalized;
    }
    
    public String placeholder(Integer... nums) {
    	String name = "_Name";
    	for (int i = 0; i < nums.length; i++) {
    		name += "_%d";
		}
    	Object[] params = Arrays.asList(nums).toArray();
		return String.format(name, params);
    }
    
    public void renameWith(String... names) {
    	if (Util.isNullOrEmpty(names))
    		return;
    	
    	String[] keys = this.keySet().toArray(new String[0]);
    	Double[] values = this.values().toArray(new Double[0]);
    	this.clear();
    	
		for (int i = 0; i < keys.length; i++) 
			if (Util.isAvailable(i, names))
				put(names[i], values[i]);
			else
				put(keys[i], values[i]);
		
		if (names.length > keys.length)
			for (int i = keys.length; i < names.length; i++) 
				put(names[i], 0.);
    }
	/**
	 * creates a sorted map of deals for a number of participants.
	 * A portion is assigned to the name in the corresponding spot.
	 * If the portion of an participant appears as null value the corresponding name is ignored in the map.
	 * @param names	the array of names of the dealers (not necessarily sorted)
	 * @param portions	given deals, if any, for individual participants according to the order of the names
	 */
	public ShareMap(String[] names, Double[] portions) {
    	int n = Math.min(names.length, portions.length);
    	for (int i = 0; i < n; i++) {
    		if (Util.isAvailable(i, portions))
	        	put(names[i], portions[i]);
    	}
	}
	/**
	 * creates a sorted map of shares for a number of sharers optionally allowing for fixed portions of any of the sharers
	 * except the first one whom the remainder of the amount is assigned to in any case.
	 * If no portion is specified for an individual sharer an uniformly shared portion is assumed (amount divided by the number of sharers).
	 * If the portion of an individual sharer appears as null value the corresponding name is ignored in the map.
	 * The values in the resulting map are the negated portions of the amount so that their sum equals the negative value of the amount.
	 * @param names	the array of names of the sharers (not necessarily sorted)
	 * @param amount	the value of the amount to share
	 * @param portions	given portions, if any, for individual sharers according to the order of the names
	 */
    public ShareMap(String[] names, double amount, Double... portions) {
    	int n = Util.isNullOrEmpty(names) ? 0 : names.length;
    	if (n > 0) {
    		double portion = amount / n;
    		
			for (int i = 1; i < n; i++) {
				String name = names[i];
				
				if (Util.isAvailable(i, portions))
					put(name, -portions[i]);
				else if (i >= portions.length)
					put(name, -portion);
				
				if (containsKey(name))
					amount += get(name);
			}
			
			put(names[0], -amount);
		}
    }
    /**
     * calculates the sum of all the values in the map
     * @return	the value of the sum
     */
    public double sum() {
    	double sum = 0;
    	for (double value : values()) 
			sum += value;
    	return sum;
    }
    /**
     * turns each value in the map to its negative
     * @return	the negated map
     */
    public ShareMap negated() {
		for (Map.Entry<String, Double> share : entrySet())
			put(share.getKey(), -share.getValue());
    	return this;
    }
    /**
     * subtracts a vector from the values (vector) of this map
     * @param subtrahend	the vector to be subtracted from this
     */
    public void minus(Collection<Double> vector) {
    	Double[] subtrahend = vector.toArray(new Double[0]);
    	int n = Math.min(size(), subtrahend.length);
    	if (n > 0) {
    		Set<String> keys = keySet();
    		Iterator<String> it = keys.iterator();
			for (int i = 0; i < n; i++) {
				String name = it.next();
				put(name, get(name) - subtrahend[i]);
			}
    	}
    }
}