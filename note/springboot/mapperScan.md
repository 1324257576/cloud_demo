`MapperScan`注解的定义：

```java
@Import(MapperScannerRegistrar.class)
@Repeatable(MapperScans.class)
public @interface MapperScan {
	// Alias for the {@link #basePackages()} attribute
  String[] value() default {};

  String[] basePackages() default {};
	//将指定类的所在包作为包扫描路径，例如指定java.lang.String，则等价于basePackages=java.lang
  Class<?>[] basePackageClasses() default {};

  Class<? extends BeanNameGenerator> nameGenerator() default BeanNameGenerator.class;

     //扫描包下指定特定注解的接口
  Class<? extends Annotation> annotationClass() default Annotation.class;

	//扫描包的指定markerInterface的接口
  Class<?> markerInterface() default Class.class;

	//sqlSessionTemplateRef/sqlSessionFactoryRef通常仅在当存在多数据源的情况下才需要指定值
  String sqlSessionTemplateRef() default "";
  String sqlSessionFactoryRef() default "";

	//通过MapperFactoryBean返回Mybatis的mapper代理对象作为spring的bean
  Class<? extends MapperFactoryBean> factoryBean() default MapperFactoryBean.class;


  String lazyInitialization() default "";

}

```

其中，`MapperScannerRegistrar`类实现了`ImportBeanDefinitionRegistrar`接口。

```java
public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AnnotationAttributes mapperScanAttrs = AnnotationAttributes
        .fromMap(importingClassMetadata.getAnnotationAttributes(MapperScan.class.getName()));
    if (mapperScanAttrs != null) {
  		//注册MapperScannerConfigurer的BeanDefinition
        registerBeanDefinitions(mapperScanAttrs, registry, generateBaseBeanName(importingClassMetadata, 0));
    }
}
```



```java
void registerBeanDefinitions(AnnotationAttributes annoAttrs, BeanDefinitionRegistry registry, String beanName) {
	//注册MapperScannerConfigurer的BeanDefinition，并根据MapperScan的AnnotationAttributes添加PropertyValue
    BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MapperScannerConfigurer.class);
    builder.addPropertyValue("processPropertyPlaceHolders", true);
    //...
	//如果MapperScan的annotationClass/markerInterface/sqlSessionTemplateRef/sqlSessionFactoryRef/factoryBean/
    //lazyInitialization等属性指定了特定值，将MapperScannerConfigurer中对应的属性值设置为对应特定值
    List<String> basePackages = new ArrayList<>();
    //basePackage包括value的包路径、basePackages包路径、以及basePackageClasses指定的类所在的包路径
    basePackages.addAll(
        Arrays.stream(annoAttrs.getStringArray("value")).filter(StringUtils::hasText).collect(Collectors.toList()));

    basePackages.addAll(Arrays.stream(annoAttrs.getStringArray("basePackages")).filter(StringUtils::hasText)
                        .collect(Collectors.toList()));

    basePackages.addAll(Arrays.stream(annoAttrs.getClassArray("basePackageClasses")).map(ClassUtils::getPackageName)
                        .collect(Collectors.toList()));
    builder.addPropertyValue("basePackage", StringUtils.collectionToCommaDelimitedString(basePackages));

    registry.registerBeanDefinition(beanName, builder.getBeanDefinition());

}
```



而`MapperScannerConfigurer`类实现了`BeanDefinitionRegistryPostProcessor`接口。

```java
@Override
public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {
    //processPropertyPlaceHolders==true
    if (this.processPropertyPlaceHolders) {
        //处理beanDefinition的PropertyValue
        processPropertyPlaceHolders();
    }

    ClassPathMapperScanner scanner = new ClassPathMapperScanner(registry);
    //addToConfig==true
    scanner.setAddToConfig(this.addToConfig);
    scanner.setAnnotationClass(this.annotationClass);
    scanner.setMarkerInterface(this.markerInterface);
    scanner.setSqlSessionFactory(this.sqlSessionFactory);
    scanner.setSqlSessionTemplate(this.sqlSessionTemplate);
    scanner.setSqlSessionFactoryBeanName(this.sqlSessionFactoryBeanName);
    scanner.setSqlSessionTemplateBeanName(this.sqlSessionTemplateBeanName);
    scanner.setResourceLoader(this.applicationContext);
    scanner.setBeanNameGenerator(this.nameGenerator);
    scanner.setMapperFactoryBeanClass(this.mapperFactoryBeanClass);
    if (StringUtils.hasText(lazyInitialization)) {
        scanner.setLazyInitialization(Boolean.valueOf(lazyInitialization));
    }
    //排除包：package-info
    scanner.registerFilters();
    scanner.scan(
        StringUtils.tokenizeToStringArray(this.basePackage, ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS));
}
```

```java
public int scan(String... basePackages) {
    int beanCountAtScanStart = this.registry.getBeanDefinitionCount();

    doScan(basePackages);

    // Register annotation config processors, if necessary.
    //includeAnnotationConfig==true
    //为registry注册注解配置处理器，如果还没注册的话。可能在之前已注册过
    if (this.includeAnnotationConfig) {
        AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
    }

    return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);
}
```

核心代码见`MapperFactoryBean`