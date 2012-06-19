package com.applang.shared;

import java.util.*;

/**
 * A <code>TreeMap</code> containing names as keys and sharing amounts as values
 *
 */
@SuppressWarnings("serial")
public class ShareMap extends TreeMap<String, Double> 
{
	public ShareMap() {}
    /**
	 * creates a sorted map of shares for a number of sharers according to a sharing policy.
	 * Integers separated by colons are interpreted as proportions of shares.
	 * If no proportions are specified uniform sharing is assumed (amount divided by the number of sharers).
	 * @param names	the array of names of the sharers (not necessarily sorted)
	 * @param amount	the value of the amount to share
	 * @param proportions	given proportions for the sharers according to the order of the names
     */
    public ShareMap(String[] names, double amount, String policy) {
		this(names, amount);
		
		if (!Util.isNullOrEmpty(names) && (!Util.notNullOrEmpty(policy) || policy.trim().length() < 1)) {
			String[] parts = new String[names.length];
			for (int i = 0; i < parts.length; i++)
				parts[i] = "1";
			policy = Util.join(":", parts);
		}

		reorganize(amount, policy);
		renameWith(names);
    }
    
    public void reorganize(double amount, String policy) {
    	this.clear();
    	
		String[] parts = new String[0];
		List<String> names = new ArrayList<String>();
		int n = 0;
		if (Util.notNullOrEmpty(policy.trim())) {
			List<String> list = new ArrayList<String>();
			list.addAll(Arrays.asList(policy.split(":")));
			n = list.size();
			for (int i = n - 1; i > -1; i--) {
				String part = list.get(i).trim();
				if (part.length() < 1) {
					list.remove(i);
					continue;
				}
				else if (part.contains("*")) {
					String[] facts = part.split("\\*");
					int factor = Util.parseInt(1, facts[1]);
					if (factor > 1) {
						list.remove(i);
						for (int j = 0; j < factor; j++) {
							list.add(i, facts[0]);
							names.add(0, placeholder(n, 1 + j));
						}
						part = "";
					}
				}
				if (part.length() > 0)
					names.add(0, placeholder(n));
				n--;
			}
			parts = list.toArray(parts);
		}
		else {
			parts = new String[] {"1"};
			names.add(placeholder());
			n = 1;
		}

		Integer[] proportions = new Integer[parts.length];
		for (int i = 0; i < proportions.length; i++) {
			proportions[i] = Util.parseInt(0, parts[i]);
		}
		
    	n = Util.isNullOrEmpty(proportions) ? 0 : proportions.length;
    	if (n > 0) {
    		Double[] normalized = normalize(proportions);
			for (int i = 0; i < normalized.length; i++) 
				put(names.get(i), -amount * normalized[i]);
    	}
    }
    
    Double[] normalize(Integer[] proportions) {
    	int n = proportions.length;
    	Double[] normalized = new Double[n];
    	
    	double denominator = 0;
		for (int i = 0; i < n; i++)
			denominator += Math.abs(proportions[i]);
		
		if (denominator > 0) {
			for (int i = 0; i < n; i++) 
				normalized[i] = Math.abs(proportions[i]) / denominator;
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