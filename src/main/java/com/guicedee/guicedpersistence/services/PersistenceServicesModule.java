package com.guicedee.guicedpersistence.services;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Module;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.guicedee.guicedpersistence.db.ConnectionBaseInfo;
import com.guicedee.logger.LogFactory;

import javax.sql.DataSource;
import java.lang.annotation.Annotation;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings("unused")
public class PersistenceServicesModule
		extends AbstractModule
		implements IGuiceModule<PersistenceServicesModule>
{
	private static final Logger log = LogFactory.getLog(PersistenceServicesModule.class);
	
	private static final Map<Class<? extends Annotation>, Module> modules = new LinkedHashMap<>();
	private static final Map<Class<? extends Annotation>, ConnectionBaseInfo> jtaConnectionBaseInfo = new LinkedHashMap<>();
	
	private static final Map<String, DataSource> jtaDataSources = new LinkedHashMap<>();
	private static final Map<String, Set<String>> jtaPersistenceUnits = new LinkedHashMap<>();
	
	@Override
	protected void configure()
	{
		log.config("Building Persistence Services Module");
		modules.forEach((key, value) -> install(value));
		
		for (Map.Entry<Class<? extends Annotation>, ConnectionBaseInfo> entry : jtaConnectionBaseInfo.entrySet())
		{
			Class<? extends Annotation> k = entry.getKey();
			ConnectionBaseInfo v = entry.getValue();
			DataSource ds;
			try
			{
				if (!jtaDataSources.containsKey(v.getJndiName()))
				{
					log.config("Starting datasource - " + v.getJndiName());
					ds = v.toPooledDatasource();
					if (ds != null)
					{
						jtaDataSources.put(v.getJndiName(), ds);
						bind(Key.get(DataSource.class, k)).toInstance(ds);
					}
					if (!jtaPersistenceUnits.containsKey(v.getJndiName()))
					{
						jtaPersistenceUnits.put(v.getJndiName(), new LinkedHashSet<>());
					}
					jtaPersistenceUnits.get(v.getJndiName())
					                   .add(jtaConnectionBaseInfo.get(k)
					                                             .getPersistenceUnitName());
				}
				else
				{
					ds = jtaDataSources.get(v.getJndiName());
					if (ds != null)
					{
						bind(Key.get(DataSource.class, k)).toInstance(ds);
					}
				}
			}
			catch (Exception t)
			{
				log.log(Level.SEVERE, "Cannot start datasource!", t);
			}
		}
	}
	
	public static Map<Class<? extends Annotation>, Module> getModules()
	{
		return modules;
	}
	
	public static Map<Class<? extends Annotation>, ConnectionBaseInfo> getJtaConnectionBaseInfo()
	{
		return jtaConnectionBaseInfo;
	}
	
	public static Map<String, DataSource> getJtaDataSources()
	{
		return jtaDataSources;
	}
	
	public static void addJtaPersistenceUnits(String jndi, String persistenceUnitName)
	{
		if (!jtaPersistenceUnits.containsKey(jndi))
		{
			jtaPersistenceUnits.put(jndi, new LinkedHashSet<>());
		}
		jtaPersistenceUnits.get(jndi)
		                   .add(persistenceUnitName);
	}
	
	public static Map<String, Set<String>> getJtaPersistenceUnits()
	{
		return jtaPersistenceUnits;
	}
	
	@Override
	public Integer sortOrder()
	{
		return Integer.MAX_VALUE - 5;
	}
}
