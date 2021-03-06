/**
 * Licensed to Apereo under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Apereo licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License.  You may obtain a
 * copy of the License at the following location:
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apereo.services.persondir.support.jdbc;

import org.apereo.services.persondir.IPersonAttributeDaoFilter;
import org.apereo.services.persondir.IPersonAttributes;
import org.apereo.services.persondir.support.AbstractDefaultQueryPersonAttributeDaoTest;
import org.apereo.services.persondir.util.CaseCanonicalizationMode;
import org.apereo.services.persondir.util.Util;
import org.hsqldb.jdbcDriver;
import org.springframework.jdbc.datasource.SimpleDriverDataSource;

import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Otherwise would be huge amounts of duplicated boilerplate for verifying
 * both {@link SingleRowJdbcPersonAttributeDao} and
 * {@link MultiRowJdbcPersonAttributeDao}.
 *
 * <p> Was no point, though, in extending
 * {@link AbstractDefaultQueryPersonAttributeDaoTest}
 * b/c those tests shouldn't necessarily behave the same way if we try to manipulate
 * the case-sensitivity behavior of the DAO under test.</p>
 */
public abstract class AbstractCaseSensitivityJdbcPersonAttributeDaoTest extends AbstractDefaultQueryPersonAttributeDaoTest {

    protected DataSource testDataSource;

    protected abstract void setUpSchema(DataSource dataSource) throws SQLException;

    protected abstract void tearDownSchema(DataSource dataSource) throws SQLException;

    protected abstract AbstractJdbcPersonAttributeDao<Map<String, Object>> newDao(DataSource dataSource);

    protected abstract void beforeNonUsernameQuery(AbstractJdbcPersonAttributeDao<Map<String, Object>> dao);

    /**
     * Some DAOs, e.g. {@link MultiRowJdbcPersonAttributeDao} cannot distinguish
     * between mulitple data attributes for case canonicalization purposes,
     * which invalidates some tests.
     *
     * @return boolean
     */
    protected abstract boolean supportsPerDataAttributeCaseSensitivity();

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.testDataSource = setUpDataSource();
        setUpSchema(testDataSource);
    }

    protected DataSource setUpDataSource() {
        return new SimpleDriverDataSource(new jdbcDriver(), "jdbc:hsqldb:mem:adhommemds", "sa", "");
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        tearDownSchema(this.testDataSource);
        this.testDataSource = null;
    }

    public void testCaseSensitiveUsernameQuery() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);

        final IPersonAttributes wrongCaseResult = impl.getPerson("AWP9", IPersonAttributeDaoFilter.alwaysChoose());
        assertNull(wrongCaseResult);
        final IPersonAttributes correctCaseResult = impl.getPerson("awp9", IPersonAttributeDaoFilter.alwaysChoose());
        assertNotNull(correctCaseResult);
        assertEquals("awp9", correctCaseResult.getName());
    }

    public void testCaseSensitiveUsernameQuery_CanonicalizedUsernameResult() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        // above was all boilerplate... here's the important stuff...
        impl.setUsernameCaseCanonicalizationMode(CaseCanonicalizationMode.UPPER);

        final IPersonAttributes wrongCaseResult = impl.getPerson("AWP9", IPersonAttributeDaoFilter.alwaysChoose());
        assertNull(wrongCaseResult);
        final IPersonAttributes correctCaseResult = impl.getPerson("awp9", IPersonAttributeDaoFilter.alwaysChoose());
        assertNotNull(correctCaseResult);
        assertEquals("AWP9", correctCaseResult.getName());
    }

    public void testCaseInsensitiveUsernameQuery() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        // above was all boilerplate... here's the important stuff...
        impl.setCaseInsensitiveQueryAttributesAsCollection(Util.genList("username"));

        final IPersonAttributes wrongCaseResult = impl.getPerson("AWP9", IPersonAttributeDaoFilter.alwaysChoose());
        assertNotNull(wrongCaseResult);
        assertEquals("AWP9", wrongCaseResult.getName());
        // both casings should work
        final IPersonAttributes correctCaseResult = impl.getPerson("awp9", IPersonAttributeDaoFilter.alwaysChoose());
        assertNotNull(correctCaseResult);
        assertEquals("awp9", correctCaseResult.getName());

    }

    public void testCaseInsensitiveUsernameQuery_CanonicalizedUsernameResult() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<>();
        columnsToAttributes.put("netid", "username");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<>();
        attributesToColumns.put("username", "netid");
        impl.setQueryAttributeMapping(attributesToColumns);
        // above was all boilerplate... here's the important stuff...
        impl.setCaseInsensitiveQueryAttributesAsCollection(Util.genList("username"));

        // username is a weird edge case... here you'd normally get the
        // casing from the value passed in to getPerson(); we're just proving
        // it can be coerced to an arbitrary casing in the mapped result.
        impl.setUsernameCaseCanonicalizationMode(CaseCanonicalizationMode.LOWER);
        final IPersonAttributes wrongCaseResult1 = impl.getPerson("AWP9", IPersonAttributeDaoFilter.alwaysChoose());
        assertNotNull(wrongCaseResult1);
        assertEquals("awp9", wrongCaseResult1.getName());

        // and now show we can go the other way too
        impl.setUsernameCaseCanonicalizationMode(CaseCanonicalizationMode.UPPER);
        final IPersonAttributes wrongCaseResult2 = impl.getPerson("AwP9", IPersonAttributeDaoFilter.alwaysChoose());
        assertNotNull(wrongCaseResult2);
        assertEquals("AWP9", wrongCaseResult2.getName());

    }

    public void testCaseSensitiveNonUsernameAttributeQuery() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        beforeNonUsernameQuery(impl);

        final Map<String, Object> wrongCase = new LinkedHashMap<>();
        wrongCase.put("firstName", "ANDREW");
        final Set<IPersonAttributes> wrongCaseResult = impl.getPeople(wrongCase, IPersonAttributeDaoFilter.alwaysChoose());
        assertEquals(0, wrongCaseResult.size());

        final Map<String, Object> correctCase = new LinkedHashMap<>();
        correctCase.put("firstName", "Andrew");
        final Set<IPersonAttributes> correctCaseResult = impl.getPeople(correctCase, IPersonAttributeDaoFilter.alwaysChoose());
        assertEquals(2, correctCaseResult.size());
        final Iterator<IPersonAttributes> correctCaseResultIterator = correctCaseResult.iterator();
        IPersonAttributes currentResult = correctCaseResultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = correctCaseResultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
    }

    public void testCaseSensitiveNonUsernameAttributeQuery_CanonicalizedResult() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setCaseInsensitiveResultAttributesAsCollection(Util.genList("firstName"));
        beforeNonUsernameQuery(impl);

        final Map<String, Object> wrongCase = new LinkedHashMap<>();
        wrongCase.put("firstName", "ANDREW");
        final Set<IPersonAttributes> wrongCaseResult = impl.getPeople(wrongCase, IPersonAttributeDaoFilter.alwaysChoose());
        assertEquals(0, wrongCaseResult.size());

        final Map<String, Object> correctCase = new LinkedHashMap<>();
        correctCase.put("firstName", "Andrew");
        final Set<IPersonAttributes> correctCaseResult = impl.getPeople(correctCase, IPersonAttributeDaoFilter.alwaysChoose());
        assertEquals(2, correctCaseResult.size());
        final Iterator<IPersonAttributes> correctCaseResultIterator = correctCaseResult.iterator();
        IPersonAttributes currentResult = correctCaseResultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));
        currentResult = correctCaseResultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));
    }

    public void testCaseInsensitiveNonUsernameAttributeQuery() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        // above was all boilerplate... here's the important stuff...
        // intentionally not setting "name" in the
        // caseInsensitiveResultAttributes to verify that we have the option
        // of preserving data-layer casing when mapping values out, even if
        // the original query on that field was case-insensitive
        impl.setCaseInsensitiveQueryAttributesAsCollection(Util.genList("firstName"));
        impl.setCaseInsensitiveDataAttributesAsCollection(Util.genList("name"));
        beforeNonUsernameQuery(impl);

        final Map<String, Object> wrongCase = new LinkedHashMap<>();
        wrongCase.put("firstName", "ANDREW");
        final Set<IPersonAttributes> wrongCaseResult = impl.getPeople(wrongCase, IPersonAttributeDaoFilter.alwaysChoose());
        assertEquals(2, wrongCaseResult.size());
        Iterator<IPersonAttributes> resultIterator = wrongCaseResult.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));

        final Map<String, Object> correctCase = new LinkedHashMap<>();
        correctCase.put("firstName", "Andrew");
        final Set<IPersonAttributes> correctCaseResult = impl.getPeople(correctCase, IPersonAttributeDaoFilter.alwaysChoose());
        assertEquals(2, correctCaseResult.size());
        resultIterator = correctCaseResult.iterator();
        currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
    }

    public void testCaseInsensitiveNonUsernameAttributeQuery_CanonicalizedResult() {
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        impl.setQueryAttributeMapping(attributesToColumns);
        // above was all boilerplate... here's the important stuff...
        // (actually same as non-_CanonicalizedResult except we do configure
        // a case-insensitive result query)
        impl.setCaseInsensitiveQueryAttributesAsCollection(Util.genList("firstName"));
        impl.setCaseInsensitiveDataAttributesAsCollection(Util.genList("name"));
        impl.setCaseInsensitiveResultAttributesAsCollection(Util.genList("firstName"));
        beforeNonUsernameQuery(impl);

        final Map<String, Object> wrongCase = new LinkedHashMap<>();
        wrongCase.put("firstName", "ANDREW");
        final Set<IPersonAttributes> wrongCaseResult = impl.getPeople(wrongCase, IPersonAttributeDaoFilter.alwaysChoose());
        assertEquals(2, wrongCaseResult.size());
        Iterator<IPersonAttributes> resultIterator = wrongCaseResult.iterator();
        IPersonAttributes currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));

        final Map<String, Object> correctCase = new LinkedHashMap<>();
        correctCase.put("firstName", "Andrew");
        final Set<IPersonAttributes> correctCaseResult = impl.getPeople(correctCase, IPersonAttributeDaoFilter.alwaysChoose());
        assertEquals(2, correctCaseResult.size());
        resultIterator = correctCaseResult.iterator();
        currentResult = resultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));
        currentResult = resultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it overrode data-layer casing
        assertEquals("andrew", currentResult.getAttributeValue("firstName"));
    }

    // Guards against a bug discovered in the original SSP-1668/PERSONDIR-74
    // patch where setting any caseInsensitiveDataAttributes config would
    // cause all data attributes to be canonicalized
    public void testCaseSensitiveNonUsernameAttributeQuery_OtherCaseInsensitiveDataAttributes() {
        if (!(supportsPerDataAttributeCaseSensitivity())) {
            // Some DAOs, e.g. MultiRowJdbcPersonAttributeDao cannot distinguish
            // between mulitple data attributes for case canonicalization purposes.
            return;
        }
        final AbstractJdbcPersonAttributeDao<Map<String, Object>> impl = newDao(testDataSource);
        impl.setUseAllQueryAttributes(false);
        final Map<String, Object> columnsToAttributes = new LinkedHashMap<>();
        columnsToAttributes.put("netid", "username");
        columnsToAttributes.put("name", "firstName");
        columnsToAttributes.put("email", "emailAddr");
        impl.setResultAttributeMapping(columnsToAttributes);
        final Map<String, Object> attributesToColumns = new LinkedHashMap<>();
        attributesToColumns.put("username", "netid");
        attributesToColumns.put("firstName", "name");
        attributesToColumns.put("emailAddr", "email");
        impl.setQueryAttributeMapping(attributesToColumns);
        impl.setCaseInsensitiveDataAttributesAsCollection(Util.genList("email"));
        beforeNonUsernameQuery(impl);

        final Map<String, Object> wrongCase = new LinkedHashMap<>();
        wrongCase.put("firstName", "ANDREW");
        final Set<IPersonAttributes> wrongCaseResult = impl.getPeople(wrongCase, IPersonAttributeDaoFilter.alwaysChoose());
        assertEquals(0, wrongCaseResult.size());

        final Map<String, Object> correctCase = new LinkedHashMap<>();
        correctCase.put("firstName", "Andrew");
        final Set<IPersonAttributes> correctCaseResult = impl.getPeople(correctCase, IPersonAttributeDaoFilter.alwaysChoose());
        assertEquals(2, correctCaseResult.size());
        final Iterator<IPersonAttributes> correctCaseResultIterator = correctCaseResult.iterator();
        IPersonAttributes currentResult = correctCaseResultIterator.next();
        assertEquals("awp9", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
        currentResult = correctCaseResultIterator.next();
        assertEquals("atest", currentResult.getName());
        // make sure it preserved data-layer casing
        assertEquals("Andrew", currentResult.getAttributeValue("firstName"));
    }


}
