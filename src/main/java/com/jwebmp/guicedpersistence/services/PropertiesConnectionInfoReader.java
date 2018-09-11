package com.jwebmp.guicedpersistence.services;

import com.jwebmp.guicedpersistence.db.ConnectionBaseInfo;
import com.oracle.jaxb21.PersistenceUnit;

import java.util.Properties;

/**
 * A functional interface to populate a connection base info based on properties received
 */
@FunctionalInterface
public interface PropertiesConnectionInfoReader
{
	/**
	 * Method populateConnectionBaseInfo ...
	 *
	 * @param unit
	 * 		of type PersistenceUnit
	 * @param filteredProperties
	 * 		of type Properties
	 * @param cbi
	 * 		of type ConnectionBaseInfo
	 *
	 * @return ConnectionBaseInfo
	 */
	ConnectionBaseInfo populateConnectionBaseInfo(PersistenceUnit unit, Properties filteredProperties, ConnectionBaseInfo cbi);
}
