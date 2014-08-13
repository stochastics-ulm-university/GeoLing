/*
 * This file is based on StandardUtilities.java of jEdit 5.1.0.
 * :tabSize=4:indentSize=4:noTabs=false:
 * :folding=explicit:collapseFolds=1:
 *
 * Copyright (C) 1999, 2006 Matthieu Casanova, Slava Pestov
 * Portions copyright (C) 2000 Richard S. Hall
 * Portions copyright (C) 2001 Dirk Moebius
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package geoling.util.vendor;

import java.util.Comparator;

/**
 * A comparator for strings that uses a more "human" ordering than
 * <function>String.compareTo()</function>.
 * 
 * @author Matthieu Casanova (StandardUtilities.java of jEdit 5.1.0)
 * @author Aaron Spettl, Institute of Stochastics, Ulm University
 */
public class HumaneStringComparator implements Comparator<String> {
	
	/**
     * A default instance of the comparator that can be used without instantiating
     * a new copy every time one is needed.
     */
    public static final HumaneStringComparator DEFAULT = new HumaneStringComparator();
	
    /** Determines whether we want to ignore lower-/upper case. */
	private boolean icase;
	
	/**
	 * Constructs a new comparator for strings.
	 * 
	 * @param icase  if true, case will be ignored
	 */
	public HumaneStringComparator(boolean icase) {
		this.icase = icase;
	}
	
	/**
	 * Constructs a new case-sensitive comparator for strings.
	 */
	public HumaneStringComparator() {
		this(false);
	}
	
	/**
	 * Compares two strings.
	 *
	 * Unlike <function>String.compareTo()</function>,
	 * this method correctly recognizes and handles embedded numbers.
	 * For example, it places "My file 2" before "My file 10".
	 * 
	 * @param str1  the first string
	 * @param str2  the second string
	 * @return negative if str1 &lt; str2, 0 if both are the same,
	 *         positive if str1 &gt; str2
	 */
	public int compare(String str1, String str2) {
		return compareStrings(str1, str2, icase);
	}
	
	/**
	 * Compares two strings.
	 *
	 * Unlike <function>String.compareTo()</function>,
	 * this method correctly recognizes and handles embedded numbers.
	 * For example, it places "My file 2" before "My file 10".
	 * 
	 * @param str1       the first string
	 * @param str2       the second string
	 * @param ignoreCase if true, case will be ignored
	 * @return negative if str1 &lt; str2, 0 if both are the same,
	 *         positive if str1 &gt; str2
	 */
	public static int compareStrings(String str1, String str2, boolean ignoreCase) {
		char[] char1 = str1.toCharArray();
		char[] char2 = str2.toCharArray();

		int len = Math.min(char1.length,char2.length);

		for(int i = 0, j = 0; i < len && j < len; i++, j++)
		{
			char ch1 = char1[i];
			char ch2 = char2[j];
			if(Character.isDigit(ch1) && Character.isDigit(ch2)
				&& ch1 != '0' && ch2 != '0')
			{
				int _i = i + 1;
				int _j = j + 1;

				for(; _i < char1.length; _i++)
				{
					if(!Character.isDigit(char1[_i]))
					{
						//_i--;
						break;
					}
				}

				for(; _j < char2.length; _j++)
				{
					if(!Character.isDigit(char2[_j]))
					{
						//_j--;
						break;
					}
				}

				int len1 = _i - i;
				int len2 = _j - j;
				if(len1 > len2)
					return 1;
				else if(len1 < len2)
					return -1;
				else
				{
					for(int k = 0; k < len1; k++)
					{
						ch1 = char1[i + k];
						ch2 = char2[j + k];
						if(ch1 != ch2)
							return ch1 - ch2;
					}
				}

				i = _i - 1;
				j = _j - 1;
			}
			else
			{
				if(ignoreCase)
				{
					ch1 = Character.toLowerCase(ch1);
					ch2 = Character.toLowerCase(ch2);
				}

				if(ch1 != ch2)
					return ch1 - ch2;
			}
		}

		return char1.length - char2.length;
	}
	
}