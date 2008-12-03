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
package org.milyn.persistence.dao.adapter.ibatis;

import static junit.framework.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.milyn.persistence.test.TestGroup;
import org.milyn.persistence.test.util.BaseTestCase;
import org.mockito.Mock;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.ibatis.sqlmap.client.SqlMapClient;

/**
 * @author <a href="mailto:maurice.zeijen@smies.com">maurice.zeijen@smies.com</a>
 *
 */
public class SqlMapClientDaoAdapterTest extends BaseTestCase {

	@Mock
	private SqlMapClient sqlMapClient;

	private SqlMapClientDaoAdapter adapter;

	@Test( groups = TestGroup.UNIT )
	public void test_persist() throws SQLException {

		// EXECUTE

		Object toPersist = new Object();

		// VERIFY

		adapter.persist("id", toPersist);

		verify(sqlMapClient).insert(eq("id"), same(toPersist));

	}

	@Test( groups = TestGroup.UNIT )
	public void test_merge() throws SQLException {

		// EXECUTE

		Object toMerge = new Object();

		Object merged = adapter.merge("id", toMerge);

		// VERIFY

		verify(sqlMapClient).update(eq("id"), same(toMerge));

		assertNull(merged);

	}


	@Test( groups = TestGroup.UNIT )
	public void test_lookup_map_parameters() throws SQLException {

		// STUB

		List<?> listResult = Collections.emptyList();

		stub(sqlMapClient.queryForList(anyString(), anyObject())).toReturn(listResult);

		// EXECUTE

		Map<String, Object> params = new HashMap<String, Object>();
		params.put("key1", "value1");
		params.put("key2", "value2");

		Collection<Object> result = adapter.lookup("name", params);

		// VERIFY

		assertSame(listResult, result);

		verify(sqlMapClient).queryForList(eq("name"), same(params));


	}

	@Test( groups = TestGroup.UNIT )
	public void test_lookup_array_parameters() throws SQLException {

		// STUB

		List<?> listResult = Collections.emptyList();

		stub(sqlMapClient.queryForList(anyString(), anyObject())).toReturn(listResult);

		// EXECUTE

		Object[] params = new Object[2];
		params[0] = "value1";
		params[1] = "value2";

		Collection<Object> result = adapter.lookup("name", params);

		// VERIFY

		assertSame(listResult, result);

		verify(sqlMapClient).queryForList(eq("name"), same(params));

	}


	/* (non-Javadoc)
	 * @see org.milyn.persistence.test.util.BaseTestCase#beforeMethod()
	 */
	@BeforeMethod(alwaysRun = true)
	@Override
	public void beforeMethod() {
		super.beforeMethod();

		adapter = new SqlMapClientDaoAdapter(sqlMapClient);
	}


}
