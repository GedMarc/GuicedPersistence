package com.jwebmp.guiceinjection.scanners;

import java.util.HashSet;
import java.util.Set;

public class GuiceInjectionMetaInfScanner
		implements PackageContentsScanner
{
	@Override
	public Set<String> searchFor()
	{
		Set<String> strings = new HashSet<>();
		strings.add("META-INF");
		return strings;
	}
}
