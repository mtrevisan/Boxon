package io.github.mtrevisan.boxon.internal.reflection;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;


class ReflectionsTest{

	private static class ReflectionsTestModel{

		@CyclicAnnotation
		public @Retention(RetentionPolicy.RUNTIME)
		@interface CyclicAnnotation{}

		@Target(ElementType.TYPE)
		@Retention(RetentionPolicy.RUNTIME)
		@interface Meta{
			String value();
		}

		@Meta("a")
		@Retention(RetentionPolicy.RUNTIME)
		@interface A{}

		@Meta("b")
		@Retention(RetentionPolicy.RUNTIME)
		@interface B{}

		@A
		static class A1{}

		@B
		static class B1{}

		@A
		static class A2{}

		@Retention(RetentionPolicy.RUNTIME)
		public @interface TestAnnotation{
			String value();
		}

		@TestAnnotation("foo foo foo")
		public static class ActualFunctionalityClass{
			@TestAnnotation("bar bar bar")
			static class Thing{}
		}

		@Repeatable(Names.class)
		@Retention(RetentionPolicy.RUNTIME)
		@Target({ElementType.TYPE})
		public @interface Name{
			String name();
		}

		@Name(name = "foo")
		@Name(name = "bar")
		public static class MultiName{}

		@Retention(RetentionPolicy.RUNTIME)
		@Target({ElementType.TYPE})
		public @interface Names{
			Name[] value() default {};
		}

		@Name(name = "foo")
		public static class SingleName{}

		public static class ParamNames{
			public ParamNames(){
				String testLocal = "local";
			}

			public ParamNames(String param1){
				String testLocal = "local";
			}

			public void test(String testParam){
				String testLocal = "local";
			}

			public void test(String testParam1, String testParam2){
				String testLocal1 = "local";
				String testLocal2 = "local";
			}

			public static void test2(String testParam){
				String testLocal = "local";
			}
		}
	}

	static Reflections reflections;


	@BeforeAll
	static void init(){
		reflections = new Reflections(new ConfigurationBuilder()
			.withPackages(ReflectionsTestModel.class));
	}

	@Test
	void cyclicAnnotation(){
		final Set<Class<?>> annotations = reflections.getTypesAnnotatedWith(ReflectionsTestModel.CyclicAnnotation.class);

		for(Class<?> annotation : annotations)
			Assertions.assertEquals(ReflectionsTestModel.CyclicAnnotation.class, annotation);
	}

	@Test
	void repeatable(){
		Set<Class<?>> classes = reflections.getTypesAnnotatedWith(ReflectionsTestModel.Name.class);
		Assertions.assertTrue(classes.contains(ReflectionsTestModel.SingleName.class));
		Assertions.assertFalse(classes.contains(ReflectionsTestModel.MultiName.class));

		classes = reflections.getTypesAnnotatedWith(ReflectionsTestModel.Names.class);
		Assertions.assertFalse(classes.contains(ReflectionsTestModel.SingleName.class));
		Assertions.assertTrue(classes.contains(ReflectionsTestModel.MultiName.class));
	}

	@Test
	void testSubTypesOf() {
		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class)), reflections.getSubTypesOf(TestModel.I1.class));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.C2.class, TestModel.C3.class, TestModel.C5.class)), reflections.getSubTypesOf(TestModel.C1.class));
	}

	@Test
	void typesAnnotatedWith() {
		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.AI1.class)), reflections.getTypesAnnotatedWithHonorInherited(TestModel.MAI1.class));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.I2.class)), reflections.getTypesAnnotatedWithHonorInherited(TestModel.AI2.class));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class)), reflections.getTypesAnnotatedWithHonorInherited(TestModel.AC1.class));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.C1.class)), reflections.getTypesAnnotatedWithHonorInherited(TestModel.AC1n.class));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.AI1.class, TestModel.I1.class, TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class)), reflections.getTypesAnnotatedWith(TestModel.MAI1.class));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.I1.class, TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class)), reflections.getTypesAnnotatedWith(TestModel.AI1.class));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class)), reflections.getTypesAnnotatedWith(TestModel.AI2.class));
		Assertions.assertTrue(reflections.getTypesAnnotatedWith(TestModel.AM1.class).isEmpty());

		//annotation member value matching
		TestModel.AC2 ac2 = new TestModel.AC2() {
			public String value() {return "ugh?!";}
			public Class<? extends Annotation> annotationType() {return TestModel.AC2.class;}};

		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.C3.class, TestModel.C5.class, TestModel.I3.class, TestModel.C6.class, TestModel.AC3.class, TestModel.C7.class)), reflections.getTypesAnnotatedWith(ac2));
		Assertions.assertEquals(new HashSet<>(Arrays.asList(TestModel.C3.class, TestModel.I3.class, TestModel.AC3.class)), reflections.getTypesAnnotatedWithHonorInherited(ac2));
	}

}
