package za.co.mmagon.guiceinjection.db;

import com.google.inject.persist.PersistService;
import za.co.mmagon.guiceinjection.annotations.DBStartup;
import za.co.mmagon.guiceinjection.annotations.GuicePostStartup;

import javax.sql.DataSource;
import java.util.logging.Logger;

@DBStartup
public abstract class DBStartupAsync implements GuicePostStartup
{
	private static final Logger log = Logger.getLogger("DBStartupAsync");

	/**
	 * The given persistService
	 */
	private PersistService persistService;
	/**
	 * The given dataSource
	 */
	@SuppressWarnings("")
	private DataSource dataSource;

	DBStartupAsync()
	{
	}

	/**
	 * Starts up the database with the given persist service and initializes the BTM datasource,
	 * The Datasource must initialize before the persist service is started, so it must be injected into this class
	 * <p>
	 * <p>
	 * <p>
	 */
	protected DBStartupAsync(DataSource dataSource)
	{
		this.dataSource = dataSource;
	}

	/**
	 * Starts up the database with the given persist service and initializes the BTM datasource,
	 * The Datasource must initialize before the persist service is started, so it must be injected into this class
	 * <p>
	 * <p>
	 * <p>
	 */
	protected DBStartupAsync(PersistService persistService, DataSource dataSource)
	{
		this(dataSource);
		this.persistService = persistService;
	}

	/**
	 * Starts the persistence service, should be threaded if the sort orders are properly applied
	 */
	@Override
	public void postLoad()
	{
		if (persistService != null)
		{
			log.config("Starting DB Startup [" + getClass() + "]");
			persistService.start();
		}
	}

	@Override
	public Integer sortOrder()
	{
		return 50;
	}
}
