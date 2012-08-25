package com.applang.share;

import java.util.*;

import static com.applang.share.Util.*;

/**
 * A <code>TreeMap</code> containing names as keys and shared amounts as values
 *
 */
public class ShareMap implements java.io.Serializable
{
	private static final long serialVersionUID = -8319562705846900169L;

	public Map<String, Double> rawMap = new TreeMap<String, Double>();
	
	@Override
	public String toString() {
		return rawMap.toString();
	}
	
	public Object[] toArray(Object... params) {
		int len = param(2, 0, params);
		
		Object[] array = new Object[len * rawMap.size()];
		
		int i = 0;
		for (Map.Entry<String, Double> share : rawMap.entrySet()) {
			if (len > 0)
				array[i] = share.getKey();
			if (len > 1)
				array[i + 1] = share.getValue();
			
			i += len;
		}
		
		return array;
	}

	public static ShareMap fromArray(Object... params) {
		ShareMap sm = new ShareMap();
		sm.rawMap = new LinkedHashMap<String, Double>();
		for (int i = 0; i < params.length; i+=2) 
			sm.rawMap.put(params[i].toString(), (Double)params[i + 1]);
		return sm;
	}
	
	public ShareMap() {
	}
	
	private String[] names = null;
	private Double amount = null;

	/** @hide */ public String[] getNames() {
		return names;
	}

	/** @hide */ public void setNames(String[] names) {
		String spender = getSpender();
		this.names = names;
		if (spender != null)
			setSpender(spender);
	}

	/** @hide */ public Double getAmount() {
		return amount;
	}

	/** @hide */ public void setAmount(Double amount) {
		this.amount = amount;
	}
	
	private boolean spender = false;
	
	/** @hide */ public boolean hasSpender() {
		return getAmount() != null && isAvailable(0, getNames()) && spender;
	}
	
	/** @hide */ public String getSpender() {
		return hasSpender() ? names[0] : null;
	}
	
	/** @hide */ public void setSpender(String name) {
		spender = isValidName(name);
		if (spender) 
			names = makeList(name, names).toArray(new String[0]);
	}
	
	class SpenderComparator implements Comparator<String>
	{
		public SpenderComparator(String spender) {
			this.spender = spender;
		}

		String spender = null;
		
		public int compare(String s1, String s2) {
			if (spender != null && spender.equals(s1))
				return spender.equals(s2) ? 0 : -1;
			else if (spender != null && spender.equals(s2))
				return spender.equals(s1) ? 0 : 1;
			else
				return s1.compareTo(s2);
		}
	};
	
	/** @hide */ public TreeMap<String, Double> getSpenderMap() {
		TreeMap<String, Double> map = new TreeMap<String, Double>(new SpenderComparator(getSpender()));
		map.putAll(this.rawMap);
		return map;
	}
	
	/** @hide */ public TreeSet<String> getSpenderKeys() {
		TreeSet<String> keys = new TreeSet<String>(new SpenderComparator(getSpender()));
		keys.addAll(this.rawMap.keySet());
		return keys;
	}

	/** @hide */ public static void checkNames(boolean validate, String... names) throws PolicyException {
		List<String> list = Arrays.asList(names);
		for (String name : list) 
			if (validate && !isValidName(name))
				throw new PolicyException(5);
		if (!areMembersDistinct(list))
			throw new PolicyException(4);
		else if (list.contains(kitty))
			throw new PolicyException(6);
	}
	
	/** @hide */ public Integer option = null;

	public static class PolicyException extends Exception
	{
		public int loc = -1;
		@Override
		public String getMessage() {
			switch (loc) {
			case 1:
				return POLICY + " : invalid factor detected";
			case 2:
				return POLICY + " : invalid name detected";
			case 3:
				return POLICY + " : at least one part is invalid";
			case 4:
				return "each name has to be distinct and not null";
			case 5:
				return "invalid names are not allowed";
			case 6:
				return enclose("'", kitty) + " not allowed here as a name";
			default:
				return super.getMessage();
			}
		}
		public PolicyException(int loc) {
			super(INVALID + "(" + loc + ")");
			this.loc = loc;
		}
		public PolicyException(String message) {
			super(message);
		}
		public static String POLICY = "sharing policy";
		public static String INVALID = "invalid " + POLICY;
	}

	public static String[] policyOperators = new String[] {":", "*", "=", "\n"};
	
	public static boolean isPolicy(String policy) {
		return embeds(policy, policyOperators[0]) || 
				embeds(policy, policyOperators[3]) || 
				embeds(policy, policyOperators[1]) || 
				isElaborate(policy);
	}
	
	public static boolean isElaborate(String policy) {
		return embedsLeft(policy, policyOperators[2]);
	}
	
	public static String association(String name, String value) {
		return join(policyOperators[2], name, value);
	}
	
	public static String[] dissociation(String value) {
		return value.split(policyOperators[2], 2);
	}

	public static String makePolicy(boolean elaborate, String[] names, Number... values) {
		int len = Math.max(
			names == null ? 0 : names.length, 
			values == null ? 0 : values.length);
		String[] parts = new String[len];
		
		boolean noValues = values.length < 1, available;
		for (int i = 0; i < len; i++) {
			available = isAvailable(i, values);
			parts[i] = noValues ? 
				"1" : 
				(available ? values[i] + "" : "0");
			if (elaborate) {
				available = isAvailable(i, names);
				parts[i] = association(
					available ? names[i] : placeholder(1 + i), 
					parts[i]);
			}
		}
		
		return join(elaborate ? policyOperators[3] : policyOperators[0], parts);
	}
	
	public interface PolicyEvaluator<T> {
		public T evaluate(List<String> names, List<Integer> proportions, List<Double> portions) throws PolicyException;
	}
	
	public static <T> T analyzePolicy(String policy, String options, PolicyEvaluator<T> analysis) throws PolicyException {
		List<String> names = new ArrayList<String>();
		List<Integer> proportions = new ArrayList<Integer>();
		List<Double> portions = new ArrayList<Double>();
		
		int len = parsePolicy(policy, options, names, proportions, portions);
		if (len < 1)
			return null;
		else 
			return analysis.evaluate(names, proportions, portions);
	}

	public static String translatePolicy(final boolean elaborate, String policy, final String... names) throws PolicyException {
		return analyzePolicy(policy, "", new PolicyEvaluator<String>() {
			public String evaluate(List<String> _names, List<Integer> proportions, List<Double> portions) {
				int len = _names.size();
				Number[] values = new Number[len];
				
				for (int i = 0; i < len; i++) {
					values[i] = proportions.get(i);
					if (values[i] == null)
						values[i] = portions.get(i);
					
					if (isAvailable(i, names)) {
						_names.remove(i);
						_names.add(i, names[i]);
					}
				}
				
				return makePolicy(elaborate, _names.toArray(new String[0]), values);
			}
		});
	}

	public static int parsePolicy(String policy, String options, Object... params) throws PolicyException {
		List<String> names = param(new ArrayList<String>(), 0, params);

		String[] parts = new String[0];
		
		if (notNullOrEmptyTrimmed(policy)) {
			List<String> list = new ArrayList<String>();
			list.addAll(Arrays.asList(policy.split(policyOperators[0] + "|\\n", -1)));		//	regex pattern requires \\n
			for (int i = list.size() - 1; i > -1; i--) {
				String part = list.get(i).trim();
				if (part.contains(policyOperators[1])) {
					String[] facts = part.split("\\" + policyOperators[1], 2);
					Integer factor = parseInteger(0, facts[1]);
					if (factor == null || factor < 1) 
						throw new PolicyException(1);
					else {
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

		List<Integer> proportions = param(new ArrayList<Integer>(), 1, params);
		List<Double> portions = param(new ArrayList<Double>(), 2, params);
		
		boolean elaborate = isElaborate(policy);
		if (elaborate)
			names.clear();
		
		for (int i = 0; i < parts.length; i++) {
			String[] sides = dissociation(parts[i]);
			if (elaborate) {
				if (sides.length < 2 || options.contains("2") && !isValidName(sides[0]))
					throw new PolicyException(2);
				
				String name = sides[0].trim();
				names.add(i, name);
			}
			
			String part = sides[sides.length - 1];
			Integer proportion = parseInteger(null, part);
			proportions.add(i, proportion);
			portions.add(i, proportion == null ? parseDouble(null, part) : null);
		}
		
		return parts.length;
	}

    /**
	 * creates a sorted map of shares for a number of sharers according to a sharing policy.
	 * Integers separated by colons are interpreted as proportions of shares.
	 * If no proportions are specified uniform sharing is assumed (amount divided by the number of sharers).
	 * @param names	the array of names of the sharers (not necessarily sorted)
	 * @param amount	the value of the amount to share
	 * @param policy	a <code>String</code> defining proportions for the sharers according to the order of the names
     */
    public ShareMap(String[] names, double amount, String policy) {
    	setNames(names);
    	
		if (!isNullOrEmpty(names) && !notNullOrEmptyTrimmed(policy)) 
			policy = makePolicy(false, names);

		try {
			updateWith(amount, policy);
		} catch (PolicyException e) {
			rawMap.clear();
		}
    }
    
    /** @hide */ public void updateWith(final double amount, final String policy) throws PolicyException {
    	rawMap.clear();
		
    	setAmount(amount);
    	
    	analyzePolicy(policy, "2", new PolicyEvaluator<Void>() {
			public Void evaluate(List<String> _names, List<Integer> proportions, List<Double> portions) throws PolicyException {
				for (int i = 0; i < _names.size(); i++) {
					if (proportions.get(i) == null) {
						Double portion = portions.get(i);
						if (portion != null) 
							rawMap.put(_names.get(i), -portion);
						else 
							throw new PolicyException(3);
					}
				}
				
		    	double _amount = amount;
				for (Double portion : rawMap.values()) 
					_amount += portion;
				
				Double[] distributed = distribute(-_amount, proportions.toArray(new Integer[0]));
				for (int i = 0; i < distributed.length; i++)
					if (distributed[i] != null)
						rawMap.put(_names.get(i), distributed[i]);
				
				boolean elaborate = isElaborate(policy);
				if (elaborate) 
			    	setNames(_names.toArray(new String[0]));
				else
					renameWith(getNames());
				return null;
			}
		});
    }
    
    /** @hide */ public void renameWith(String... names) {
    	if (isNullOrEmpty(names))
    		return;
    	
    	String[] keys = rawMap.keySet().toArray(new String[0]);
    	Double[] values = rawMap.values().toArray(new Double[0]);
    	rawMap.clear();
    	
		for (int i = 0; i < keys.length; i++) 
			if (isAvailable(i, names))
				rawMap.put(names[i], values[i]);
			else
				rawMap.put(keys[i], values[i]);
		
		if (names.length > keys.length)
			for (int i = keys.length; i < names.length; i++) 
				rawMap.put(names[i], 0.);
		
    	setNames(names);
    }
	/**
	 * creates a sorted map of deals for a number of participants.
	 * A portion is assigned to the name in the corresponding spot.
	 * If the portion of an participant appears as null value the corresponding name is ignored in the map.
	 * @param names	the array of names of the dealers (not necessarily sorted)
	 * @param portions	given deals, if any, for individual participants according to the order of the names
	 */
	public ShareMap(String[] names, Double[] portions) {
    	setNames(names);
    	
    	int n = Math.min(names.length, portions.length);
    	for (int i = 0; i < n; i++) {
    		if (isAvailable(i, portions))
    			rawMap.put(names[i], portions[i]);
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
    	setNames(names);
    	updateWith(amount, portions);
    }
    
    /** @hide */ public void updateWith(double amount, Double[] portions) {
    	rawMap.clear();
		
    	setAmount(amount);
    	
    	String[] names = getNames();
    	int n = names == null ? 0 : names.length;
    	if (n > 0) {
    		double portion = amount / n;
    		
			for (int i = 1; i < n; i++) {
				String name = names[i];
				
				if (isAvailable(i, portions))
					rawMap.put(name, -portions[i]);
				else if (i >= portions.length)
					rawMap.put(name, -portion);
				
				if (rawMap.containsKey(name))
					amount += rawMap.get(name);
			}
			
			rawMap.put(names[0], -amount);
		}
	}
    /**
     * @param name	the name of the sharer in question
     * @return	the amount of the sharer 's portion, if null, a portion for the sharer in question doesn't exist.
     */
    public Double getPortion(String name) {
    	if (rawMap.containsKey(name)) 
    		return -rawMap.get(name);
    	else
    		return null;
    }
    /**
     * calculates the sum of all the values in the map
     * @return	the value of the sum
     */
    public double sum() {
    	double sum = 0;
    	for (Double value : rawMap.values()) 
    		if (value != null)
    			sum += value;
    	return sum;
    }
    /**
     * turns each value in the map to its negative
     * @return	the negated map
     */
    public ShareMap negated() {
		for (Map.Entry<String, Double> share : rawMap.entrySet())
			rawMap.put(share.getKey(), -share.getValue());
    	return this;
    }
    /**
     * subtracts a vector from the values (vector) of this map
     * @param subtrahend	the vector to be subtracted from this
     */
    public void minus(Collection<Double> vector) {
    	Double[] subtrahend = vector.toArray(new Double[0]);
    	int n = Math.min(rawMap.size(), subtrahend.length);
    	if (n > 0) {
    		Set<String> keys = rawMap.keySet();
    		Iterator<String> it = keys.iterator();
			for (int i = 0; i < n; i++) {
				String name = it.next();
				rawMap.put(name, rawMap.get(name) - subtrahend[i]);
			}
    	}
    }
    /**
     * @return	true if the sum of the shares and the amount is zero
     */
    public boolean isComplete() {
    	return getAmount() != null && Math.abs(getAmount() + sum()) < delta;
    }
    
    /**
     * calculates the distribution of an amount according to given proportions
     * @param amount
	 * @param proportions	given proportions for the sharers according to the order of the names
     * @return	an array of distributed values
     */
    public Double[] distribute(double amount, Integer[] proportions) {
    	int n = proportions.length;
    	Double[] distributed = new Double[n];
    	
    	double denominator = 0;
		for (int i = 0; i < n; i++)
			denominator += proportions[i] == null ? 0 : Math.abs(proportions[i]);
		
		if (denominator > 0) {
			for (int i = 0; i < n; i++) 
				distributed[i] = proportions[i] == null ? null : 
					amount * Math.abs(proportions[i]) / denominator;
		}

		return distributed;
    }
}