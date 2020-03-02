package com.guicedee.guicedpersistence.db;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.guicedee.guicedinjection.GuiceContext;
import com.guicedee.guicedinjection.interfaces.IGuiceModule;
import com.guicedee.guicedinjection.interfaces.IGuicePostStartup;
import com.guicedee.guicedpersistence.services.IPropertiesEntityManagerReader;
import com.guicedee.guicedpersistence.injectors.JpaPersistPrivateModule;
import com.guicedee.guicedpersistence.scanners.PersistenceFileHandler;
import com.guicedee.logger.LogFactory;
import com.oracle.jaxb21.PersistenceUnit;
import com.oracle.jaxb21.Property;

import javax.persistence.EntityManager;
import javax.sql.DataSource;
import javax.validation.constraints.NotNull;
import java.lang.annotation.Annotation;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An abstract implementation for persistence.xml
 * <p>
 * Configuration conf = TransactionManagerServices.getConfiguration(); can be used to configure the transaction manager.
 */
public abstract class DatabaseModule<J extends DatabaseModule<J>>
		extends AbstractModule
		implements IGuiceModule<J>
{
	/**
	 * Field log
	 */
	private static final Logger log = LogFactory.getLog("DatabaseModule");

	/**
	 * A set of all annotations that this abstraction built
	 */
	private static final Set<Class<? extends Annotation>> boundAnnotations = new HashSet<>();

	/**
	 * Constructor DatabaseModule creates a new DatabaseModule instance.
	 */
	public DatabaseModule()
	{
		//Config required
	}

	/**
	 * Returns a full list of all annotations that have bindings
	 *
	 * @return The set of all annotations that have bindings
	 */
	public static Set<Class<? extends Annotation>> getBoundAnnotations()
	{
		return DatabaseModule.boundAnnotations;
	}

	/**
	 * Configures the module with the bindings
	 */
	@SuppressWarnings("unchecked")
	@Override
	protected void configure()
	{
		DatabaseModule.log.config("Loading Database Module - " + getClass().getName() + " - " + getPersistenceUnitName());
		Properties jdbcProperties = getJDBCPropertiesMap();
		PersistenceUnit pu = getPersistenceUnit();
		if (pu == null)
		{
			DatabaseModule.log
					.severe("Unable to register persistence unit with name " + getPersistenceUnitName() + " - No persistence unit containing this name was found.");
			return;
		}
		for (IPropertiesEntityManagerReader entityManagerReader : GuiceContext.instance()
																			  .getLoader(IPropertiesEntityManagerReader.class, true,
		                                                                                 ServiceLoader.load(IPropertiesEntityManagerReader.class)))
		{

			Map<String, String> output = entityManagerReader.processProperties(pu, jdbcProperties);
			if (output != null && !output.isEmpty())
			{
				jdbcProperties.putAll(output);
			}
		}
		ConnectionBaseInfo connectionBaseInfo = getConnectionBaseInfo(pu, jdbcProperties);
		connectionBaseInfo.populateFromProperties(pu, jdbcProperties);
		if (connectionBaseInfo.getJndiName() == null)
		{
			connectionBaseInfo.setJndiName(getJndiMapping());
		}
		DatabaseModule.log.fine(getPersistenceUnitName() + " - Connection Base Info Final - " + connectionBaseInfo);

		install(new JpaPersistPrivateModule(getPersistenceUnitName(), jdbcProperties, getBindingAnnotation()));

		ConnectionBaseInfo ds;
		DbStartup startup = new DbStartup(getBindingAnnotation(),getJndiMapping());
		if(isDataSourceAvailable())
		{
			DatabaseModule.log.log(Level.FINE, "Bound DataSource.class with @" + getBindingAnnotation().getSimpleName());
			DbStartup.getAvailableDataSources()
			         .add(getBindingAnnotation());
			bind(getDataSourceKey()).toProvider(startup).in(Singleton.class);
		}
		DbStartup.getLoadedConnectionBaseInfos().put(getJndiMapping(), connectionBaseInfo);
		GuiceContext.instance().loadPostStartupServices();

		GuiceContext.getAllLoadedServices()
		            .get(IGuicePostStartup.class)
		            .add(startup);

		log.config("Starting Datasource - " + getBindingAnnotation() + " - " + getJndiMapping());
		new DatasourceStartup(getBindingAnnotation(), getJndiMapping()).postLoad();


		DatabaseModule.log.log(Level.FINE, "Bound PersistenceUnit.class with @" + getBindingAnnotation().getSimpleName());
		bind(Key.get(PersistenceUnit.class, getBindingAnnotation())).toInstance(pu);
		DatabaseModule.boundAnnotations.add(getBindingAnnotation());
	}

	/**
	 * The name found in persistence.xml
	 *
	 * @return The persistence unit name to sear h
	 */
	@NotNull
	protected abstract String getPersistenceUnitName();

	/**
	 * Returns the persistence unit associated with the supplied name
	 *
	 * @return The given persistence unit
	 */
	protected PersistenceUnit getPersistenceUnit()
	{
		try
		{
			for (PersistenceUnit pu : PersistenceFileHandler.getPersistenceUnits())
			{
				if (pu.getName()
				      .equals(getPersistenceUnitName()))
				{
					return pu;
				}
			}
		}
		catch (Throwable T)
		{
			DatabaseModule.log.log(Level.SEVERE, "Couldn't Find Persistence Unit for the given name [" + getPersistenceUnitName() + "]", T);
		}
		DatabaseModule.log.log(Level.WARNING, "Couldn't Find Persistence Unit for the given name [" + getPersistenceUnitName() + "]. Returning a Null Instance");
		return null;
	}

	/**
	 * Builds up connection base data info from a persistence unit.
	 * <p>
	 * Use with the utility methods e.g.
	 *
	 * @param unit
	 * 		The physical persistence unit, changes have no effect the persistence ready
	 *
	 * @return The new connetion base info
	 */
	@NotNull
	protected abstract ConnectionBaseInfo getConnectionBaseInfo(PersistenceUnit unit, Properties filteredProperties);

	/**
	 * A properties map of the properties from the file
	 *
	 * @return A properties map of the given persistence units properties
	 */
	@NotNull
	private Properties getJDBCPropertiesMap()
	{
		Properties jdbcProperties = new Properties();
		PersistenceUnit pu = getPersistenceUnit();
		configurePersistenceUnitProperties(pu, jdbcProperties);
		return jdbcProperties;
	}

	/**
	 * The name found in jta-data-source from the persistence.xml
	 *
	 * @return The JNDI mapping name to use
	 */
	@NotNull
	protected abstract String getJndiMapping();

	/**
	 * Returns the generated key for the data source
	 *
	 * @return The key of the annotation and data source
	 */
	@NotNull
	protected Key<DataSource> getDataSourceKey()
	{
		return Key.get(DataSource.class, getBindingAnnotation());
	}

	/**
	 * Returns the key used for the entity manager
	 *
	 * @return The key for the entity manager and the annotation
	 */
	@NotNull
	protected Key<EntityManager> getEntityManagerKey()
	{
		return Key.get(EntityManager.class, getBindingAnnotation());
	}

	/**
	 * The annotation which will identify this guy
	 *
	 * @return The annotation that will identify the given databsae
	 */
	@NotNull
	protected abstract Class<? extends Annotation> getBindingAnnotation();

	/**
	 * Builds a property map from a persistence unit properties file
	 * <p>
	 * Overwrites ${} items with system properties
	 *
	 * @param pu
	 * 		The persistence unit
	 * @param jdbcProperties
	 * 		The final properties map
	 */
	protected void configurePersistenceUnitProperties(PersistenceUnit pu, Properties jdbcProperties)
	{
		if (pu != null)
		{
			try
			{
				for (Property props : pu.getProperties()
				                        .getProperty())
				{
					jdbcProperties.put(props.getName(), props.getValue());

				}
			}
			catch (Throwable t)
			{
				log.log(Level.SEVERE, "Unable to load persistence unit properties for [" + pu.getName() + "]", t);
			}
		}
	}

	@Override
	public Integer sortOrder()
	{
		return 50;
	}

	public boolean isDataSourceAvailable()
	{
		return true;
	}
}