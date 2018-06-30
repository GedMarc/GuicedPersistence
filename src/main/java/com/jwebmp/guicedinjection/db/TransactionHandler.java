package com.jwebmp.guicedinjection.db;

import bitronix.tm.jndi.BitronixContext;
import com.google.inject.persist.Transactional;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Status;
import javax.transaction.UserTransaction;
import javax.validation.constraints.NotNull;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionHandler
		implements MethodInterceptor
{
	private static final Logger log = Logger.getLogger("TransactionHandler");

	@Override
	public Object invoke(MethodInvocation invocation) throws Throwable
	{
		UserTransaction ut = null;
		try
		{
			ut = getUserTransaction();
		}
		catch (Exception T)
		{
			log.log(Level.SEVERE, "Unable to get User Transaction", T);
			return invocation.proceed();
		}

		Object returnable = null;
		if (ut.getStatus() != Status.STATUS_ACTIVE)
		{
			ut.begin();
		}
		try
		{
			returnable = invocation.proceed();
			ut.commit();
		}
		catch (IllegalStateException ise)
		{
			log.log(Level.FINEST, "Nothing to commit in transaction?", ise);
		}
		catch (Throwable T)
		{
			Transactional t = invocation.getMethod()
			                            .getAnnotation(Transactional.class);
			for (Class<? extends Exception> aClass : t.rollbackOn())
			{
				if (aClass.isAssignableFrom(T.getClass()))
				{
					log.log(Level.FINE, "Exception In Commit Rolled Back from class [" + T.getClass() + "]: ", T);
					ut.rollback();
				}
			}
			log.log(Level.SEVERE, "Exception In Commit : " + T.getMessage(), T);
			throw T;
		}
		return returnable;
	}

	/**
	 * Returns the current user transaction
	 *
	 * @return
	 *
	 * @throws NamingException
	 */
	@NotNull
	public static UserTransaction getUserTransaction() throws NamingException
	{
		UserTransaction ut = null;
		try
		{
			InitialContext ic = new InitialContext();
			return (UserTransaction) ic.lookup("java:comp/UserTransaction");
		}
		catch (Throwable T)
		{
			BitronixContext ic = new BitronixContext();
			return (UserTransaction) ic.lookup("java:comp/UserTransaction");
		}
	}
}