package de.hdu.pvs.crashfinder.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.wala.ipa.slicer.Statement;

import de.hdu.pvs.crashfinder.util.PackageExtractor;
import de.hdu.pvs.crashfinder.util.WALAUtils;

/**
 * 
 * @author Mohammad Ghanavati Created in November 2015
 */

public class Intersection {

	public List<String> matchingSet(String diffFile) {

		BufferedReader br = null;
		String sCurrentLine;
		List<String> diffClassSet = new ArrayList<String>();
		List<String> matchingSet = new ArrayList<String>();
		try {

			br = new BufferedReader(new FileReader(diffFile));

			while ((sCurrentLine = br.readLine()) != null) {
				Pattern p = Pattern.compile("\\+++ (.*)/(.*?).java");
				Matcher m = p.matcher(sCurrentLine);
				if (m.find()) {
					String strFound = m.group();
					String absPath = strFound.replace("+", "").trim();
					File javaFile = new File(absPath);
					if (!javaFile.exist())
						continue;
					String packageName = new PackageExtractor(javaFile)
							.extractPackageName();
					String fileName = javaFile.getName();
					String fullClassName = packageName + "."
							+ fileName.substring(0, fileName.length() - 5);

					matchingSet.add(fullClassName);
					diffClassSet.add(fullClassName);
				}
			}
			return matchingSet;

		} catch (IOException e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	public Collection<Statement> intersection(List<String> diff,
			Collection<? extends Statement> slice) {
		if (diff.isEmpty()) {
			throw new IllegalArgumentException("Cannot intersect with empty "
					+ "diff.");
		}
		if (slice.isEmpty()) {
			throw new IllegalArgumentException("Cannot intersect with empty "
					+ "slice");
		}
		Collection<Statement> sliceDiff = new ArrayList<Statement>();
		for (Statement s1 : slice) {
			String fullClassName = null;
			String extractedFullClassName = WALAUtils.getJavaFullClassName(s1
					.getNode().getMethod().getDeclaringClass());
			if (extractedFullClassName.contains("$")) {

				String[] dollarReplace = extractedFullClassName.split("\\$");
				fullClassName = dollarReplace[0];
			} else {
				fullClassName = extractedFullClassName;
			}
			if (diff.contains(fullClassName)) {
				sliceDiff.add(s1);
			}
		}
		return sliceDiff;
	}
}
