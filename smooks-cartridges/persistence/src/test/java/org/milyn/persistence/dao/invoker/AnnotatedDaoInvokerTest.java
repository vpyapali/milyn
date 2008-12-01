/*
	Milyn - Copyright (C) 2006

	This library is free software; you can redistribute it and/or
	modify it under the terms of the GNU Lesser General Public
	License (version 2.1) as published by the Free Software
	Foundation.

	This library is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

	See the GNU Lesser General Public License for more details:
	http://www.gnu.org/licenses/lgpl.txt
*/
package org.milyn.persistence.dao.invoker;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.milyn.persistence.NoMethodWithAnnotationFound;
import org.milyn.persistence.dao.reflection.AnnotatedDaoRuntimeInfo;
import org.milyn.persistence.dao.reflection.AnnotatedDaoRuntimeInfoFactory;
import org.milyn.persistence.test.TestGroup;
import org.milyn.persistence.test.dao.AnnotatedDaoNoEntityReturned;
import org.milyn.persistence.test.dao.FullAnnotatedDao;
import org.milyn.persistence.test.dao.MinimumAnnotatedDao;
import org.milyn.persistence.test.util.BaseTestCase;
import org.mockito.MockitoAnnotations.Mock;
import org.testng.annotations.Test;

/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 *
 */
public class AnnotatedDaoInvokerTest extends BaseTestCase {


	@Mock
	FullAnnotatedDao fullDao;

	@Mock
	AnnotatedDaoNoEntityReturned daoNoEntityReturned;

	@Mock
	MinimumAnnotatedDao minimumDao;

	AnnotatedDaoRuntimeInfoFactory runtimeInfoFactory = new AnnotatedDaoRuntimeInfoFactory();

	AnnotatedDaoRuntimeInfo fullDaoRuntimeInfo = runtimeInfoFactory.create(FullAnnotatedDao.class);

	AnnotatedDaoRuntimeInfo daoNoEntityReturnedRuntimeInfo = runtimeInfoFactory.create(AnnotatedDaoNoEntityReturned.class);

	AnnotatedDaoRuntimeInfo minimumDaoRuntimeInfo = runtimeInfoFactory.create(MinimumAnnotatedDao.class);

	@Test(groups = TestGroup.UNIT)
	public void test_persist_with_entity_return() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(fullDao, fullDaoRuntimeInfo);

		Object toPersist = new Object();

		Object expectedResult = new Object();

		stub(fullDao.persistIt(toPersist)).toReturn(expectedResult);

		Object result = invoker.persist(toPersist);

		verify(fullDao).persistIt(same(toPersist));

		assertSame(expectedResult, result);
	}

	@Test(groups = TestGroup.UNIT)
	public void test_persist_with_null_return() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(fullDao, fullDaoRuntimeInfo);

		Object toPersist = new Object();

		stub(fullDao.persistIt(toPersist)).toReturn(null);

		Object result = invoker.persist(toPersist);

		verify(fullDao).persistIt(same(toPersist));

		assertNull(result);
	}

	@Test(groups = TestGroup.UNIT)
	public void test_persist_noEntityReturned() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(daoNoEntityReturned, daoNoEntityReturnedRuntimeInfo);

		Object toPersist = new Object();

		stub(daoNoEntityReturned.persistIt(toPersist)).toReturn(toPersist);

		Object result = invoker.persist(toPersist);

		verify(daoNoEntityReturned).persistIt(same(toPersist));

		assertNull(result);
	}

	@Test(groups = TestGroup.UNIT, expectedExceptions = NoMethodWithAnnotationFound.class)
	public void test_persist_no_annotation() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(minimumDao, minimumDaoRuntimeInfo);

		Object toPersist = new Object();

		invoker.persist(toPersist);


	}


	@Test(groups = TestGroup.UNIT)
	public void test_merge_with_entity_return() {


		DaoInvoker invoker = new AnnotatedDaoInvoker(fullDao, fullDaoRuntimeInfo);

		Object toMerge = new Object();

		Object expectedResult = new Object();

		stub(fullDao.mergeIt(toMerge)).toReturn(expectedResult);

		Object result = invoker.merge(toMerge);

		verify(fullDao).mergeIt(same(toMerge));

		assertSame(expectedResult, result);

	}

	@Test(groups = TestGroup.UNIT)
	public void test_merge_with_null_return() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(fullDao, fullDaoRuntimeInfo);

		Object toMerge = new Object();

		stub(fullDao.mergeIt(toMerge)).toReturn(null);

		Object result = invoker.merge(toMerge);

		verify(fullDao).mergeIt(same(toMerge));

		assertNull(result);
	}

	@Test(groups = TestGroup.UNIT, expectedExceptions = NoMethodWithAnnotationFound.class)
	public void test_merge_no_annotation() {


		DaoInvoker invoker = new AnnotatedDaoInvoker(minimumDao, minimumDaoRuntimeInfo);

		Object toMerge = new Object();

		invoker.merge(toMerge);

	}


	@Test(groups = TestGroup.UNIT)
	public void test_merge_noEntityReturned() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(daoNoEntityReturned, daoNoEntityReturnedRuntimeInfo);

		Object toMerge = new Object();

		stub(daoNoEntityReturned.mergeIt(toMerge)).toReturn(toMerge);

		Object result = invoker.merge(toMerge);

		verify(daoNoEntityReturned).mergeIt(same(toMerge));

		assertNull(result);
	}


	@Test(groups = TestGroup.UNIT)
	public void test_flush() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(fullDao, fullDaoRuntimeInfo);

		invoker.flush();

		verify(fullDao).flushIt();

	}

	@Test(groups = TestGroup.UNIT, expectedExceptions = NoMethodWithAnnotationFound.class)
	public void test_flush_no_annotation() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(minimumDao, minimumDaoRuntimeInfo);

		invoker.flush();

	}

	@Test(groups = TestGroup.UNIT)
	public void test_lookup() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(fullDao, fullDaoRuntimeInfo);

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("id", 1L);

		invoker.lookup("id", params);

		verify(fullDao).findById(1L);

	}


	@Test(groups = TestGroup.UNIT, expectedExceptions = NoMethodWithAnnotationFound.class)
	public void test_lookup_no_annotation() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(minimumDao, minimumDaoRuntimeInfo);

		Map<String, Object> params = new HashMap<String, Object>();

		invoker.lookup("id", params);

	}

	@Test(groups = TestGroup.UNIT)
	public void test_lookupByQuery_map_params() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(fullDao, fullDaoRuntimeInfo);

		Map<String, Object> params = new HashMap<String, Object>();

		invoker.lookupByQuery("query", params);

		verify(fullDao).findByQuery(eq("query"), same(params));

	}

	@Test(groups = TestGroup.UNIT)
	public void test_lookupByQuery_array_params() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(fullDao, fullDaoRuntimeInfo);

		Object[] params = new Object[0];

		invoker.lookupByQuery("query", params);

		verify(fullDao).findByQuery(eq("query"), same(params));

	}


	@Test(groups = TestGroup.UNIT, expectedExceptions = NoMethodWithAnnotationFound.class)
	public void test_lookupByQuery_non_query_finder_dao_map_params() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(minimumDao, minimumDaoRuntimeInfo);

		Map<String, Object> params = new HashMap<String, Object>();

		invoker.lookupByQuery("id", params);

	}

	@Test(groups = TestGroup.UNIT, expectedExceptions = NoMethodWithAnnotationFound.class)
	public void test_lookupByQuery_non_query_finder_dao_array_params() {

		DaoInvoker invoker = new AnnotatedDaoInvoker(minimumDao, minimumDaoRuntimeInfo);

		Object[] params = new Object[0];

		invoker.lookupByQuery("id", params);

	}


}
