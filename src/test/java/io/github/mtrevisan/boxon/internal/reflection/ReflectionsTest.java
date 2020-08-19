package io.github.mtrevisan.boxon.internal.reflection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


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
		class A1{}

		@B
		class B1{}

		@A
		class A2{}

		@Retention(RetentionPolicy.RUNTIME)
		public @interface TestAnnotation{
			public String value();
		}

		@TestAnnotation("foo foo foo")
		public class ActualFunctionalityClass{
			@TestAnnotation("bar bar bar")
			class Thing{}
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


//	@Test
//	void test_cyclic_annotation(){
//		Reflections reflections = new Reflections(ReflectionsTestModel.class);
//		Assertions.assertThat(reflections.getTypesAnnotatedWith(ReflectionsTestModel.CyclicAnnotation.class), are(ReflectionsTestModel.CyclicAnnotation.class));
//	}
//
//	@Test
//	void no_exception_when_configured_scanner_store_is_empty(){
//		Reflections reflections = new Reflections(new ConfigurationBuilder()
//			.withUrls(ClasspathHelper.forPackage("my.project.prefix"));
//
//		reflections.getSubTypesOf(String.class);
//	}
//
//	@Test
//	void getAllAnnotated_returns_meta_annotations(){
//		Reflections reflections = new Reflections(ReflectionsTestModel.class);
//		for(Class<?> type : reflections.getTypesAnnotatedWith(ReflectionsTestModel.Meta.class)){
//			Set<Annotation> allAnnotations = ReflectionUtils.getAllAnnotations(type);
//			List<? extends Class<? extends Annotation>> collect = allAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toList());
//			Assertions.assertTrue(collect.contains(ReflectionsTestModel.Meta.class));
//		}
//
//		ReflectionsTestModel.Meta meta = new ReflectionsTestModel.Meta(){
//			@Override
//			public String value(){ return "a"; }
//
//			@Override
//			public Class<? extends Annotation> annotationType(){ return Meta.class; }
//		};
//		for(Class<?> type : reflections.getTypesAnnotatedWith(meta)){
//			Set<Annotation> allAnnotations = ReflectionUtils.getAllAnnotations(type);
//			List<? extends Class<? extends Annotation>> collect = allAnnotations.stream().map(Annotation::annotationType).collect(Collectors.toList());
//			Assertions.assertTrue(collect.contains(ReflectionsTestModel.Meta.class));
//		}
//	}
//
//	@Test
//	void test_custom_url_class_loader() throws MalformedURLException{
//		URL externalUrl = new URL("jar:file:" + ReflectionsTest.getUserDir() + "/src/test/resources/another-project.jar!/");
//		URLClassLoader externalClassLoader = new URLClassLoader(new URL[]{externalUrl}, Thread.currentThread().getContextClassLoader());
//
//		Reflections reflections = new Reflections(new ConfigurationBuilder().addUrls(ClasspathHelper.forClass(TestModel.class)).addUrls(externalUrl).addClassLoaders(externalClassLoader));
//
//		Assertions.assertEquals(toStringSorted(reflections.getSubTypesOf(TestModel.C1.class)), "[class another.project.AnotherTestModel$C2, " + "class org.reflections.TestModel$C2, " + "class org.reflections.TestModel$C3, " + "class org.reflections.TestModel$C5]");
//	}
//
//	@Test
//	void test_repeatable(){
//		Reflections ref = new Reflections(ReflectionsTestModel.class);
//		Set<Class<?>> clazzes = ref.getTypesAnnotatedWith(ReflectionsTestModel.Name.class);
//		Assertions.assertTrue(clazzes.contains(ReflectionsTestModel.SingleName.class));
//		Assertions.assertFalse(clazzes.contains(ReflectionsTestModel.MultiName.class));
//
//		clazzes = ref.getTypesAnnotatedWith(ReflectionsTestModel.Names.class);
//		Assertions.assertFalse(clazzes.contains(ReflectionsTestModel.SingleName.class));
//		Assertions.assertTrue(clazzes.contains(ReflectionsTestModel.MultiName.class));
//	}

}
