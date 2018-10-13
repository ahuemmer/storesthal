package de.huemmerich.web.wsobjectstore;

import de.huemmerich.web.wsobjectstore.testtypes.*;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Vector;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class IdFinderTest {

	@Test
	public void testWithSimpleModelType() throws IdFinderException {
		Vector<Member> ids = IdFinder.findIds(SimpleType.class);
		assertEquals(ids.size(),1);
		assertEquals(ids.get(0).getName(),"getId");
		assertThat(ids.get(0), instanceOf(Method.class));
	}

	@Test
	public void testWithSimpleModelType2() throws IdFinderException {
		Vector<Member> ids = IdFinder.findIds(SimpleType2.class);
		assertEquals(ids.size(),1);
		assertEquals(ids.get(0).getName(),"getId");
		assertThat(ids.get(0), instanceOf(Method.class));
	}
	
	@Test
	public void testWithSimpleModelType3() throws IdFinderException {
		Vector<Member> ids = IdFinder.findIds(SimpleType3.class);
		assertEquals(ids.size(),1);
		assertEquals(ids.get(0).getName(),"getId");
		assertThat(ids.get(0), instanceOf(Method.class));
	}
	
	@Test
	public void testWithSimpleModelType4() throws IdFinderException {
		Vector<Member> ids = IdFinder.findIds(SimpleType4.class);
		assertEquals(ids.size(),1);
		assertEquals(ids.get(0).getName(),"getName");
		assertThat(ids.get(0), instanceOf(Method.class));
	}
	
	@Test
	public void testWithSimpleModelType5() throws IdFinderException {
		Vector<Member> ids = IdFinder.findIds(SimpleType5.class);
		assertEquals(ids.size(),1);
		assertEquals(ids.get(0).getName(),"imApublicIDString");
		assertThat(ids.get(0), instanceOf(Field.class));
	}
	
	@Test
	public void testWithComplexModelType1() throws IdFinderException {
		Vector<Member> ids = IdFinder.findIds(ComplexType1.class);
		assertEquals(ids.size(),3);
		assertEquals(ids.get(0).getName(),"getId1");
		assertThat(ids.get(0), instanceOf(Method.class));
		assertEquals(ids.get(1).getName(),"getId2");
		assertThat(ids.get(1), instanceOf(Method.class));
		assertEquals(ids.get(2).getName(),"id3");
		assertThat(ids.get(2), instanceOf(Field.class));
	}
	
	@Test(expected=IdFinderException.class)
	public void testWithoutIdAnnotation() throws IdFinderException {
		try {
			IdFinder.findIds(IdLessType.class);
		} catch (IdFinderException e) {
			System.out.println(e.getMessage());
			throw(e);
		}
	}
	
	@Test(expected=IdFinderException.class)
	public void testFaultyType1() throws IdFinderException {
		try {
			IdFinder.findIds(FaultyType1.class);
		} catch (IdFinderException e) {
			System.out.println(e.getMessage());
			throw(e);
		}
	}
	
	@Test(expected=IdFinderException.class)
	public void testFaultyType2() throws IdFinderException {
		try {
			IdFinder.findIds(FaultyType2.class);
		} catch (IdFinderException e) {
			System.out.println(e.getMessage());
			throw(e);
		}
	}
	
	@Test(expected=IdFinderException.class)
	public void testFaultyType3() throws IdFinderException {
		try {
			IdFinder.findIds(FaultyType3.class);
		} catch (IdFinderException e) {
			System.out.println(e.getMessage());
			throw(e);
		}
	}
	
	@Test(expected=IdFinderException.class)
	public void testFaultyType4() throws IdFinderException {
		try {
			IdFinder.findIds(FaultyType4.class);
		} catch (IdFinderException e) {
			System.out.println(e.getMessage());
			throw(e);
		}
	}
	
	@Test(expected=IdFinderException.class)
	public void testFaultyType5() throws IdFinderException {
		try {
			IdFinder.findIds(FaultyType5.class);
		} catch (IdFinderException e) {
			System.out.println(e.getMessage());
			throw(e);
		}
	}
}
