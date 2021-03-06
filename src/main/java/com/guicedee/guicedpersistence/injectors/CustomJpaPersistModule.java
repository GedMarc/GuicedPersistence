/*
 * Copyright (C) 2010 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.guicedee.guicedpersistence.injectors;

import com.google.common.base.Preconditions;
import com.google.inject.Provides;
import com.google.inject.TypeLiteral;
import com.google.inject.persist.PersistService;
import com.google.inject.persist.UnitOfWork;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.lang.annotation.Annotation;
import java.util.Map;

/**
 * JPA provider for guice persist.
 *
 * @author dhanji@gmail.com (Dhanji R. Prasanna)
 */
public final class CustomJpaPersistModule
		extends CustomPersistModule
{
	/**
	 * Field jpaUnit
	 */
	private final String jpaUnit;

	/**
	 * Field properties
	 */
	private Map<?, ?> properties;

	/**
	 * The annotation to use
	 */
	private Class<? extends Annotation> annotation;

	private CustomJpaLocalTxnInterceptor transactionInterceptor;

	/**
	 * Constructor CustomJpaPersistModule creates a new CustomJpaPersistModule instance.
	 *
	 * @param jpaUnit
	 * 		of type String
	 * @param annotation
	 * 		The given annotation
	 */
	public CustomJpaPersistModule(String jpaUnit, Class<? extends Annotation> annotation)
	{
		this.annotation = annotation;
		Preconditions.checkArgument(null != jpaUnit && jpaUnit.length() > 0, "JPA unit name must be a non-empty string.");
		this.jpaUnit = jpaUnit;
	}

	/**
	 * Method configurePersistence ...
	 */
	@Override
	protected void configurePersistence()
	{
		bindConstant().annotatedWith(CustomJpa.class)
		              .to(jpaUnit);

		bind(new TypeLiteral<Class<? extends Annotation>>() {}).annotatedWith(CustomJpa.class)
		                                                       .toInstance(annotation);

		CustomJpaPersistService persistService = new CustomJpaPersistService();
		persistService.setPersistenceUnitName(jpaUnit);
		persistService.setAnnotation(annotation);
		persistService.setPersistenceProperties(properties);

		bind(CustomJpaPersistService.class).toInstance(persistService);

		bind(PersistService.class).to(CustomJpaPersistService.class);
		bind(UnitOfWork.class).to(CustomJpaPersistService.class);
		bind(EntityManager.class).toProvider(CustomJpaPersistService.class);

		CustomJpaPersistService.EntityManagerFactoryProvider provider = new CustomJpaPersistService.EntityManagerFactoryProvider();
		provider.setEmProvider(persistService);

		bind(EntityManagerFactory.class).toProvider(provider);

		transactionInterceptor = new CustomJpaLocalTxnInterceptor();
		requestInjection(transactionInterceptor);
	}

	/**
	 * Method provideProperties ...
	 *
	 * @return Map
	 */
	@Provides
	@CustomJpa
	Map<?, ?> provideProperties()
	{
		return properties;
	}

	/**
	 * Configures the JPA persistence provider with a set of properties.
	 *
	 * @param properties
	 * 		A set of name value pairs that configure a JPA persistence provider as per
	 * 		the specification.
	 *
	 * @since 4.0 (since 3.0 with a parameter type of {@code java.util.Properties})
	 */
	public CustomJpaPersistModule properties(Map<?, ?> properties)
	{
		this.properties = properties;
		return this;
	}

}
