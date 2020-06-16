package unit731.boxon.utils;

import java.util.regex.Pattern;


/**
 * @version	1.0.0 - 16 Jun 2008
 */
public class VersionHelper{

	private static final Pattern VERSION_SPLITTER = Pattern.compile("[^a-z0-9]+");


	private VersionHelper(){}

	public static int compare(final String version1, final String version2){
		if(version1 == null && version2 == null)
			return 0;
		if(version1 == null)
			return -1;
		if(version2 == null)
			return 1;

		final String[] tokens1 = VERSION_SPLITTER.split(version1.toLowerCase());
		final String[] tokens2 = VERSION_SPLITTER.split(version2.toLowerCase());

		final int size1 = tokens1.length;
		final int size2 = tokens2.length;
		final int minSize = Math.min(size1, size2);
		for(int i = 0; i <= minSize; i ++){
			if(i == size1)
				return (i == size2? 0: -1);
			if(i == size2)
				return 1;

			int subValue1 = Integer.MAX_VALUE;
			int subValue2 = Integer.MAX_VALUE;
			try{ subValue1 = Integer.parseInt(tokens1[i]); }
			catch(final Exception ignored){ }
			try{ subValue2 = Integer.parseInt(tokens2[i]); }
			catch(final Exception ignored){ }

			if(subValue1 != subValue2)
				return subValue1 - subValue2;

			final int result = tokens1[i].compareTo(tokens2[i]);
			if(result != 0)
				return result;
		}
		return 0;
	}

}
