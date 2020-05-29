



























`DefaultListableBeanFactory`是整个bean加载的核心部分，是Spring注册及加载bean的默认实现。

```java
public class DefaultListableBeanFactory extends AbstractAutowireCapableBeanFactory
		implements ConfigurableListableBeanFactory, BeanDefinitionRegistry
```































分析始于`SpringApplication.run(IOCApp.class, args);`

调用`SpringApplication`的静态`run(Class<?>[] primarySources,String[] args)`

```java
org.springframework.boot.SpringApplication#run(java.lang.Class<?>[], java.lang.String[])

public static ConfigurableApplicationContext run(Class<?>[] primarySources, String[] args) {
		return new SpringApplication(primarySources).run(args);
}
```



实例化`SpringApplication`对象

```java
public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {
		//传入args:SpringApplication(null,=IOCApp.class)
    	this.resourceLoader = resourceLoader;
		this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));
    	//通过判断类路径下是否存在对应的类来推断WebApplicationType
        //WebApplicationType枚举定义：NONE/SERVLET/REACTIVE
		this.webApplicationType = WebApplicationType.deduceFromClasspath();
		setInitializers((Collection) 
 		//从FactoryNameMap中获取对应的type实例对象，详情看下面分析      
                        			getSpringFactoriesInstances(ApplicationContextInitializer.class));
		setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));
    //根据运行时栈的栈帧方法是否为main推断mainApplicationClass
		this.mainApplicationClass = deduceMainApplicationClass();
	}
```





`SpringApplication#getSpringFactoriesInstances(java.lang.Class<T>)`

```java
private <T> Collection<T> getSpringFactoriesInstances(Class<T> type) {
    return getSpringFactoriesInstances(type, new Class<?>[] {});
}
```

->

```java
private <T> Collection<T> getSpringFactoriesInstances(Class<T> type, Class<?>[] parameterTypes, Object... args) {
    //传入args：getSpringFactoriesInstances(ApplicationContextInitializer.class, new Class<?>[] {})
		ClassLoader classLoader = getClassLoader();
    //从FactoryNameMap<String,List<String>>中获取对应type的className的List
		Set<String> names = new LinkedHashSet<>(SpringFactoriesLoader.loadFactoryNames(type, classLoader));
    //对names中的className进行对象实例化
		List<T> instances = createSpringFactoriesInstances(type, parameterTypes, classLoader, args, names);
    //排序：比较器实现类AnnotationAwareOrderComparator
    //按照以下顺序：PriorityOrdered接口->Ordered接口->@Order注解->@Priority注解来获取指定的order值进行integer比较，如果没有则使用默认值Ordered.LOWEST_PRECEDENCE（=Integer.MAX_VALUE）
		AnnotationAwareOrderComparator.sort(instances);
		return instances;
	}
```



```java
public static List<String> loadFactoryNames(Class<?> factoryType, @Nullable ClassLoader classLoader) {
		String factoryTypeName = factoryType.getName();
		return loadSpringFactories(classLoader).getOrDefault(factoryTypeName, Collections.emptyList());
	}
```



   

遍历类路径下所有的`FACTORIES_RESOURCE_LOCATION (META-INF/spring.factories)`文件，对文件中的配置信息保存到以`factoryTypeName`为KEY，以保存`ClassName`的`List<String>`作为VALUE的`Map<String, List<String>>`中，最后将该Map保存到`SpringFactoriesLoader#cache`对象中（`Map<ClassLoader, Map<String, List<String>>>`）

```java
private static Map<String, List<String>> loadSpringFactories(@Nullable ClassLoader classLoader) {
    MultiValueMap<String, String> result = cache.get(classLoader);
    if (result != null) {
        return result;
    }
	
    Enumeration<URL> urls = (classLoader != null ?
                             classLoader.getResources(FACTORIES_RESOURCE_LOCATION) :
                        ClassLoader.getSystemResources(FACTORIES_RESOURCE_LOCATION));
    result = new LinkedMultiValueMap<>();
    while (urls.hasMoreElements()) {
        URL url = urls.nextElement();
        UrlResource resource = new UrlResource(url);
        Properties properties = PropertiesLoaderUtils.loadProperties(resource);
        for (Map.Entry<?, ?> entry : properties.entrySet()) {
            String factoryTypeName = ((String) entry.getKey()).trim();
            for (String factoryImplementationName : StringUtils.commaDelimitedListToStringArray((String) entry.getValue())) {
                result.add(factoryTypeName, factoryImplementationName.trim());
            }
        }
    }
    cache.put(classLoader, result);
    return result;
}
```







到这里，`SpringApplication`对象实例化分析完成，从这里开始研究`SpringApplication#run(String[])`方法

```java
public ConfigurableApplicationContext run(String... args) {
		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		ConfigurableApplicationContext context = null;
		Collection<SpringBootExceptionReporter> exceptionReporters = new ArrayList<>();
    //指定System.Property中的SYSTEM_PROPERTY_JAVA_AWT_HEADLESS，默认值为true
		configureHeadlessProperty();
    //实例化FactoryNameMap中的SpringApplicationRunListener类型的对象（EventPublishingRunListener），添加到SpringApplicationRunListeners对象的List<SpringApplicationRunListener>中
		SpringApplicationRunListeners listeners = getRunListeners(args);
    //回调EventPublishingRunListener的starting方法：发布ApplicationStartingEvent事件，
    //从而触发SpringListener回调对应的方法
		listeners.starting();
		try {
            //对cmdArgs进行解析，保存到ApplicationArguments对象中
			ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);
            //获取并初始化Environment对象
			ConfigurableEnvironment environment = prepareEnvironment(listeners, applicationArguments);
            //设置System Property：spring.beaninfo.ignore=true
			configureIgnoreBeanInfo(environment);
            
			Banner printedBanner = printBanner(environment);
            
            //根据webApplicationType获取context对象的ClassName：AnnotationConfigApplicationContext
            //调用BeanUtils.instantiateClass(contextClass)方法实例化context对象
			context = createApplicationContext();
            //获取SpringBootExceptionReporter类型的对象：FailureAnalyzers,该对象的属性List<FailureAnalyzer>保存着FactoryNameMap中FailureAnalyzer类型的实例化对象
			exceptionReporters = getSpringFactoriesInstances(SpringBootExceptionReporter.class,
					new Class[] { ConfigurableApplicationContext.class }, context);
            //见下文SpringApplication#prepareContext
			prepareContext(context, environment, listeners, applicationArguments, printedBanner);
            
            //见下文AbstractApplicationContext#refresh
			refreshContext(context);
			afterRefresh(context, applicationArguments);
			stopWatch.stop();
			if (this.logStartupInfo) {
				new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), stopWatch);
			}
			listeners.started(context);
			callRunners(context, applicationArguments);
		}
		catch (Throwable ex) {
			handleRunFailure(context, ex, exceptionReporters, listeners);
			throw new IllegalStateException(ex);
		}

		try {
			listeners.running(context);
		}
		catch (Throwable ex) {
			handleRunFailure(context, ex, exceptionReporters, null);
			throw new IllegalStateException(ex);
		}
		return context;
	}
```





```java
public EventPublishingRunListener(SpringApplication application, String[] args) {
    //传入SpringApplication对象
		this.application = application;
    //args={}
		this.args = args;
		this.initialMulticaster = new SimpleApplicationEventMulticaster();
    //application.getListeners()的值是在实例化SpringApplication对象时调用setListeners(getSpringFactoriesInstances(ApplicationListener.class))的结果
		for (ApplicationListener<?> listener : application.getListeners()) {
			this.initialMulticaster.addApplicationListener(listener);
		}
	}
```



发布`ApplicationStartingEvent`触发的关键方法：`BackgroundPreinitializer#performPreinitialization`进行对应类/属性设置的初始化

```java
this.runSafely(new BackgroundPreinitializer.ConversionServiceInitializer());
                    this.runSafely(new BackgroundPreinitializer.ValidationInitializer());
                    this.runSafely(new BackgroundPreinitializer.MessageConverterInitializer());
                    this.runSafely(new BackgroundPreinitializer.JacksonInitializer());
                    this.runSafely(new BackgroundPreinitializer.CharsetInitializer());
```







`SpringApplication#prepareEnvironment`

```java
private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,
			ApplicationArguments applicationArguments) {
		// 根据webApplicationType的值创建Environment对象：StandardEnvironment
		ConfigurableEnvironment environment = getOrCreateEnvironment();
    
    //1.如果args不为空则解析args为SimpleCommandLinePropertySource，以commandLineArgs为name保存到PropertySource对象，并添加到env的propertySourcesList中
    //2.设置env的ActiveProfiles
		configureEnvironment(environment, applicationArguments.getSourceArgs());
    
    //将env原有的propertySourcesList保存到SpringConfigurationPropertySources对象，以configurationProperties为name，保存到PropertySource对象，并保存到env的propertySourcesList的下标[0]位置（如果存在则先删除再addFirst）
		ConfigurationPropertySources.attach(environment);
    
    //回调EventPublishingRunListener的environmentPrepared方法：使得EventPublishingRunListener发布ApplicationEnvironmentPreparedEvent事件
		listeners.environmentPrepared(environment);
    
    	//绑定env对象到springApplication对象
		bindToSpringApplication(environment);
		if (!this.isCustomEnvironment) {
            //是否需要进行环境对象类型的转换
			environment = new EnvironmentConverter(getClassLoader()).convertEnvironmentIfNecessary(environment,
					deduceEnvironmentClass());
		}
		ConfigurationPropertySources.attach(environment);
		return environment;
	}

```





发布`ApplicationEnvironmentPreparedEvent`事件触发的关键方法：

`ConfigFileApplicationListener#onApplicationEnvironmentPreparedEvent`

```java
private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {
    //实例化从FactoryNameMap中获取EnvironmentPostProcessor的ClassName
		List<EnvironmentPostProcessor> postProcessors = loadPostProcessors();
		postProcessors.add(this);
		AnnotationAwareOrderComparator.sort(postProcessors);
    //回调EnvironmentPostProcessor的postProcessEnvironment方法
		for (EnvironmentPostProcessor postProcessor : postProcessors) {
			postProcessor.postProcessEnvironment(event.getEnvironment(), event.getSpringApplication());
		}
	}
```

`SystemEnvironmentPropertySourceEnvironmentPostProcessor#postProcessEnvironment`：使用`OriginAwareSystemEnvironmentPropertySource`替换`SystemEnvironmentPropertySource`

`ConfigFileApplicationListener#postProcessEnvironment`:添加`RandomValuePropertySource(RANDOM_PROPERTY_SOURCE_NAME)`到`env`的`propertySourceList`中









`AnnotationConfigApplicationContext`对象的创建



父类构造器初始化属性：

```java
String id=ObjectUtils.identityToString(this)
ResourcePatternResolver resourcePatternResolver = getResourcePatternResolver();
DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
```

自身的无参构造方法：

```java
public AnnotationConfigApplicationContext() {
	//AnnotatedBeanDefinitionReader
    this.reader = new AnnotatedBeanDefinitionReader(this);
    //ClassPathBeanDefinitionScanner
		this.scanner = new ClassPathBeanDefinitionScanner(this);
	}
```



`AnnotatedBeanDefinitionReader`的创建：

```java
public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {
		this.registry = registry;
		this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);
    //分析该方法：见下面
		AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
	}
```



![1586169750884](D:\Temp\cloud_demo\note\ioc\1586169750884.png)



```java
public static Set<BeanDefinitionHolder> registerAnnotationConfigProcessors(
			BeanDefinitionRegistry registry, @Nullable Object source) {
		//传入source=null
    
		DefaultListableBeanFactory beanFactory = unwrapDefaultListableBeanFactory(registry);
		if (beanFactory != null) {
			if (!(beanFactory.getDependencyComparator() instanceof AnnotationAwareOrderComparator)) {
			//设置依赖项的比较器：AnnotationAwareOrderComparator（在前面提及过）
                beanFactory.setDependencyComparator(AnnotationAwareOrderComparator.INSTANCE);
			}
			if (!(beanFactory.getAutowireCandidateResolver() instanceof ContextAnnotationAutowireCandidateResolver)) {
                //ContextAnnotationAutowireCandidateResolver(实现BeanFactoryAware接口)
                //valueAnnotationType = @Value，qualifierType=@Qualifier
				beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
			}
		}

		Set<BeanDefinitionHolder> beanDefs = new LinkedHashSet<>(8);
    
    
		//是否包含internalConfigurationAnnotationProcessor的BeanDefinition
		if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            
            //ConfigurationClassPostProcessor 实现了BeanDefinitionRegistryPostProcessor接口
			RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);
            //BeanDefinition默认属性：autowireMode=AUTOWIRE_NO，autowireCandidate=true，role=ROLE_APPLICATION
			def.setSource(source);
            //registerPostProcessor方法：1.指定role=ROLE_INFRASTRUCTURE,
            //2.并调用DefaultListableBeanFactory#registerBeanDefinition方法，将beanDifition和beanName添加到DefaultListableBeanFactory的beanDefinitionMap与beanDefinitionNames中
			beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

    //是否包含internalAutowiredAnnotationProcessor
		if (!registry.containsBeanDefinition(AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            //AutowiredAnnotationBeanPostProcessor实现了SmartInstantiationAwareBeanPostProcessor和MergedBeanDefinitionPostProcessor接口
			RootBeanDefinition def = new RootBeanDefinition(AutowiredAnnotationBeanPostProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, AUTOWIRED_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		// internalCommonAnnotationProcessor
		if (jsr250Present && !registry.containsBeanDefinition(COMMON_ANNOTATION_PROCESSOR_BEAN_NAME)) {
            //CommonAnnotationBeanPostProcessor处理Resource/PreDestroy/PostConstruct等JSR-250支持的注解
			RootBeanDefinition def = new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, COMMON_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		// Check for JPA support, and if present add the PersistenceAnnotationBeanPostProcessor.
		if (jpaPresent && !registry.containsBeanDefinition(PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME)) {
			RootBeanDefinition def = new RootBeanDefinition();
			try {
				def.setBeanClass(ClassUtils.forName(PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME,
						AnnotationConfigUtils.class.getClassLoader()));
			}
			catch (ClassNotFoundException ex) {
				throw new IllegalStateException(
						"Cannot load optional framework class: " + PERSISTENCE_ANNOTATION_PROCESSOR_CLASS_NAME, ex);
			}
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, PERSISTENCE_ANNOTATION_PROCESSOR_BEAN_NAME));
		}

		if (!registry.containsBeanDefinition(EVENT_LISTENER_PROCESSOR_BEAN_NAME)) {
            
            //EventListenerMethodProcessor处理EventListener注解？
			RootBeanDefinition def = new RootBeanDefinition(EventListenerMethodProcessor.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_PROCESSOR_BEAN_NAME));
		}

		if (!registry.containsBeanDefinition(EVENT_LISTENER_FACTORY_BEAN_NAME)) {
            //DefaultEventListenerFactory处理常规的EventListener注解
			RootBeanDefinition def = new RootBeanDefinition(DefaultEventListenerFactory.class);
			def.setSource(source);
			beanDefs.add(registerPostProcessor(registry, def, EVENT_LISTENER_FACTORY_BEAN_NAME));
		}

		return beanDefs;
	}
```





`org.springframework.beans.factory.support.DefaultListableBeanFactory#registerBeanDefinition`

```java
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)
			throws BeanDefinitionStoreException {


		if (beanDefinition instanceof AbstractBeanDefinition) {
			try {
                //如果hasMethodOverrides==true，进行prepareMethodOverrides
				((AbstractBeanDefinition) beanDefinition).validate();
			}
			catch (BeanDefinitionValidationException ex) {
				throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,
						"Validation of bean definition failed", ex);
			}
		}

		BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);
		if (existingDefinition != null) {
			if (!isAllowBeanDefinitionOverriding()) {
				throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);
			}
			else if (existingDefinition.getRole() < beanDefinition.getRole()) {
				// e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE
				if (logger.isInfoEnabled()) {
					logger.info("Overriding user-defined bean definition for bean '" + beanName +
							"' with a framework-generated bean definition: replacing [" +
							existingDefinition + "] with [" + beanDefinition + "]");
				}
			}
			else if (!beanDefinition.equals(existingDefinition)) {
				if (logger.isDebugEnabled()) {
					logger.debug("Overriding bean definition for bean '" + beanName +
							"' with a different definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			else {
				if (logger.isTraceEnabled()) {
					logger.trace("Overriding bean definition for bean '" + beanName +
							"' with an equivalent definition: replacing [" + existingDefinition +
							"] with [" + beanDefinition + "]");
				}
			}
			this.beanDefinitionMap.put(beanName, beanDefinition);
		}
		else {
			if (hasBeanCreationStarted()) {
				// Cannot modify startup-time collection elements anymore (for stable iteration)
				synchronized (this.beanDefinitionMap) {
					this.beanDefinitionMap.put(beanName, beanDefinition);
					List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);
					updatedDefinitions.addAll(this.beanDefinitionNames);
					updatedDefinitions.add(beanName);
					this.beanDefinitionNames = updatedDefinitions;
					removeManualSingletonName(beanName);
				}
			}
			else {
				// 仍处于启动注册阶段
                //添加到DefaultListableBeanFactory的Map<String, BeanDefinition> beanDefinitionMap
				this.beanDefinitionMap.put(beanName, beanDefinition);
				 //添加到DefaultListableBeanFactory的List<String> beanDefinitionNames 
                this.beanDefinitionNames.add(beanName);
                //如果Set<String> manualSingletonNames（手动注册单例名称列表）包含beanName，则将其从set中进行remove
				removeManualSingletonName(beanName);
			}
			this.frozenBeanDefinitionNames = null;
		}

		if (existingDefinition != null || containsSingleton(beanName)) {
			resetBeanDefinition(beanName);
		}
	}
```

`ClassPathBeanDefinitionScanner`的创建，

初始化`List<TypeFilter> includeFilters`，支持三种注解：`Component`/`javax.annotation.ManagedBean`/`javax.inject.Named`。

默认值`String DEFAULT_RESOURCE_PATTERN = "**/*.class"`







`org.springframework.boot.SpringApplication#prepareContext`

```java
private void prepareContext(ConfigurableApplicationContext context, ConfigurableEnvironment environment,
			SpringApplicationRunListeners listeners, ApplicationArguments applicationArguments, Banner printedBanner) {
    //为context/context.reader/context.scanner设置environment
		context.setEnvironment(environment);
    //为context的beanFactory设置conversionService
		postProcessApplicationContext(context);
    //应用所有的ApplicationContextInitializer：setInitializers(getSpringFactoriesInstances(ApplicationContextInitializer.class))
		applyInitializers(context);
    //回调EventPublishingRunListener#contextPrepared方法，发布ApplicationContextInitializedEvent事件
		listeners.contextPrepared(context);
		if (this.logStartupInfo) {
			logStartupInfo(context.getParent() == null);
			logStartupProfileInfo(context);
		}
		// Add boot specific singleton beans
		ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();
    //通过DefaultSingletonBeanRegistry#addSingleton注册applicationArguments单例对象
		beanFactory.registerSingleton("springApplicationArguments", applicationArguments);
		if (printedBanner != null) {
			beanFactory.registerSingleton("springBootBanner", printedBanner);
		}
		if (beanFactory instanceof DefaultListableBeanFactory) {
            //allowBeanDefinitionOverriding=false
			((DefaultListableBeanFactory) beanFactory)
					.setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);
		}
    //lazyInitialization==false
		if (this.lazyInitialization) {
			context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());
		}
		// Load the sources，获取primarySources
		Set<Object> sources = getAllSources();
		Assert.notEmpty(sources, "Sources must not be empty");
    	//见下文分析
		load(context, sources.toArray(new Object[0]));
    	//回调EventPublishingRunListener#contextLoaded方法：
    	//将springApplication对象的applicationListeners所有的ApplicationListener添加到context的applicationListeners中
    	//发布ApplicationPreparedEvent事件
		listeners.contextLoaded(context);
	}
```



`applyInitializers`方法中关键方法：

`SharedMetadataReaderFactoryContextInitializer#initialize`

```java
public void initialize(ConfigurableApplicationContext applicationContext) {
    //添加CachingMetadataReaderFactoryPostProcessor(实现了BeanDefinitionRegistryPostProcessor接口)
        applicationContext.addBeanFactoryPostProcessor(new SharedMetadataReaderFactoryContextInitializer.CachingMetadataReaderFactoryPostProcessor());
    }
```

`CachingMetadataReaderFactoryPostProcessor`:

```java
public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
    //添加beanName为internalCachingMetadataReaderFactory，type为SharedMetadataReaderFactoryBean的beanDefinition
            this.register(registry);
    //对ConfigurationClassPostProcessor的BeanDefinition进行属性配置补充
            this.configureConfigurationClassPostProcessor(registry);
        }

        private void register(BeanDefinitionRegistry registry) {
            BeanDefinition definition = BeanDefinitionBuilder.genericBeanDefinition(SharedMetadataReaderFactoryContextInitializer.SharedMetadataReaderFactoryBean.class, SharedMetadataReaderFactoryContextInitializer.SharedMetadataReaderFactoryBean::new).getBeanDefinition();
            registry.registerBeanDefinition("org.springframework.boot.autoconfigure.internalCachingMetadataReaderFactory", definition);
        }

        private void configureConfigurationClassPostProcessor(BeanDefinitionRegistry registry) {
            try {
                //对ConfigurationClassPostProcessor的BeanDefinition进行补充，对MetadataReaderFactory metadataReaderFactory进行赋值：SharedMetadataReaderFactoryBean的RuntimeBeanReference
                //RuntimeBeanReference:对应Bean对象还没有在容器中创建，使用RuntimeBeanReference以保存对实际的Bean对象引用，在spring处理依赖关系时，最终会对引用替换为实际Bean的引用
                
                BeanDefinition definition = registry.getBeanDefinition("org.springframework.context.annotation.internalConfigurationAnnotationProcessor");
                definition.getPropertyValues().add("metadataReaderFactory", new RuntimeBeanReference("org.springframework.boot.autoconfigure.internalCachingMetadataReaderFactory"));
            } catch (NoSuchBeanDefinitionException var3) {
            }

        }
```



`org.springframework.boot.context.ContextIdApplicationContextInitializer#initialize`

设置`applicationContext`的id属性为`application`，并通过`DefaultSingletonBeanRegistry#addSingleton`方法添加`ContextId`对象

```java
protected void addSingleton(String beanName, Object singletonObject) {
		synchronized (this.singletonObjects) {
			this.singletonObjects.put(beanName, singletonObject);
			this.singletonFactories.remove(beanName);
			this.earlySingletonObjects.remove(beanName);
			this.registeredSingletons.add(beanName);
		}
	}
```



`org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer#initialize`

```java
public void initialize(ConfigurableApplicationContext context) {
    //用于检查包扫描的错误报告：PROBLEM_PACKAGES=[org.springframework,org]
		context.addBeanFactoryPostProcessor(new ConfigurationWarningsPostProcessor(getChecks()));
	}
```

`org.springframework.boot.rsocket.context.RSocketPortInfoApplicationContextInitializer#initialize`

```java
public void initialize(ConfigurableApplicationContext applicationContext) {
    //Listener implements ApplicationListener<RSocketServerInitializedEvent>
    //onApplicationEvent方法：setPortProperty(this.applicationContext, event.getServer().address().getPort());
		applicationContext.addApplicationListener(new Listener(applicationContext));
}
```



`org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer#initialize`

```java
public void initialize(ConfigurableApplicationContext applicationContext) {
    //添加当前对象（当前类实现了ApplicationListener<WebServerInitializedEvent>）
		applicationContext.addApplicationListener(this);
	}

@Override
public void onApplicationEvent(WebServerInitializedEvent event) {
    String propertyName = "local." + getName(event.getApplicationContext()) + ".port";
    //设置context的对应propertyName的端口值
    setPortProperty(event.getApplicationContext(), propertyName, event.getWebServer().getPort());
}
```







`org.springframework.boot.SpringApplication#load`

```java
protected void load(ApplicationContext context, Object[] sources) {
    //创建BeanDefinitionLoader对象
		BeanDefinitionLoader loader = createBeanDefinitionLoader(getBeanDefinitionRegistry(context), sources);
		if (this.beanNameGenerator != null) {
			loader.setBeanNameGenerator(this.beanNameGenerator);
		}
		if (this.resourceLoader != null) {
			loader.setResourceLoader(this.resourceLoader);
		}
		if (this.environment != null) {
			loader.setEnvironment(this.environment);
		}
    //
		loader.load();
	}
```



```java
BeanDefinitionLoader(BeanDefinitionRegistry registry, Object... sources) {
		this.sources = sources;
		this.annotatedReader = new AnnotatedBeanDefinitionReader(registry);
		this.xmlReader = new XmlBeanDefinitionReader(registry);
		if (isGroovyPresent()) {
			this.groovyReader = new GroovyBeanDefinitionReader(registry);
		}
		this.scanner = new ClassPathBeanDefinitionScanner(registry);
    //对sources的ClassName添加到ClassExcludeFilter.classNames中，以保证sources的类不会被包扫描重复添加
		this.scanner.addExcludeFilter(new ClassExcludeFilter(sources));
	}
```



`org.springframework.boot.BeanDefinitionLoader#load(java.lang.Object)`

```java
private int load(Object source) {
		if (source instanceof Class<?>) {
			return load((Class<?>) source);
		}
		if (source instanceof Resource) {
			return load((Resource) source);
		}
		if (source instanceof Package) {
			return load((Package) source);
		}
		if (source instanceof CharSequence) {
			return load((CharSequence) source);
		}
		throw new IllegalArgumentException("Invalid source type " + source.getClass());
	}


//由于这里使用的是指定source的Class
private int load(Class<?> source) {
		if (isGroovyPresent() && GroovyBeanDefinitionSource.class.isAssignableFrom(source)) {
			// Any GroovyLoaders added in beans{} DSL can contribute beans here
			GroovyBeanDefinitionSource loader = BeanUtils.instantiateClass(source, GroovyBeanDefinitionSource.class);
			load(loader);
		}
    //是否存在Component注解
		if (isComponent(source)) {
            //仅注册source的Bean
			this.annotatedReader.register(source);
			return 1;
		}
		return 0;
	}
```



`org.springframework.context.annotation.AnnotatedBeanDefinitionReader#registerBean(java.lang.Class<?>)`

```java
public void registerBean(Class<?> beanClass) {
    doRegisterBean(beanClass, null, null, null, null);
}

private <T> void doRegisterBean(Class<T> beanClass, @Nullable String name,
                                @Nullable Class<? extends Annotation>[] qualifiers, @Nullable Supplier<T> supplier,
                                @Nullable BeanDefinitionCustomizer[] customizers) {

    //创建AnnotatedGenericBeanDefinition，设置beanClass和注解metadata信息
    AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);
    //根据adb的注解元信息判断是否存在Conditional注解，是否进行跳过注册
    if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {
        return;
    }

    abd.setInstanceSupplier(supplier);
    ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
    abd.setScope(scopeMetadata.getScopeName());
    String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));

    //处理Lazy/Primary/DependsOn/Role/Description注解
    AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);
    
    //==null
    if (qualifiers != null) {
        for (Class<? extends Annotation> qualifier : qualifiers) {
            if (Primary.class == qualifier) {
                abd.setPrimary(true);
            }
            else if (Lazy.class == qualifier) {
                abd.setLazyInit(true);
            }
            else {
                abd.addQualifier(new AutowireCandidateQualifier(qualifier));
            }
        }
    }
    //==null
    if (customizers != null) {
        for (BeanDefinitionCustomizer customizer : customizers) {
            customizer.customize(abd);
        }
    }

    BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
    //是否对beanDefinition进行代理而生成代理bean
    definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
    
    //将beanName和beanDefinition添加到beanDefinitionNames/beanDefinitionMap中
    BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
}

```



发布`ApplicationPreparedEvent`事件的触发的关键方法：

`ConfigFileApplicationListener#onApplicationPreparedEvent`：添加`PropertySourceOrderingPostProcessor`

```java
protected void addPostProcessors(ConfigurableApplicationContext context) {
    context.addBeanFactoryPostProcessor(new PropertySourceOrderingPostProcessor(context));
}

//PropertySourceOrderingPostProcessor的作用是：对env的defaultProperties重新排序，remove-》addLast
```

`LoggingApplicationListener#onApplicationPreparedEvent`

```java
private void onApplicationPreparedEvent(ApplicationPreparedEvent event) {
		ConfigurableListableBeanFactory beanFactory = event.getApplicationContext().getBeanFactory();
		if (!beanFactory.containsBean(LOGGING_SYSTEM_BEAN_NAME)) {
            //注册单例bean对象
			beanFactory.registerSingleton(LOGGING_SYSTEM_BEAN_NAME, this.loggingSystem);
		}
		if (this.logFile != null && !beanFactory.containsBean(LOG_FILE_BEAN_NAME)) {
			beanFactory.registerSingleton(LOG_FILE_BEAN_NAME, this.logFile);
		}
		if (this.loggerGroups != null && !beanFactory.containsBean(LOGGER_GROUPS_BEAN_NAME)) {
            //注册单例bean对象
			beanFactory.registerSingleton(LOGGER_GROUPS_BEAN_NAME, this.loggerGroups);
		}
	}
```





`org.springframework.context.support.AbstractApplicationContext#refresh`

```java
public void refresh() throws BeansException, IllegalStateException {
    synchronized (this.startupShutdownMonitor) {
        // 设置AbstractApplicationContext#active=true（context is active）
        prepareRefresh();

        // 设置GenericApplicationContext#refreshed=true，设置GenericApplicationContext#refreshed="application"
        ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();

        // Prepare the bean factory for use in this context.
        prepareBeanFactory(beanFactory);

        try {
            // Allows post-processing of the bean factory in context subclasses.
            postProcessBeanFactory(beanFactory);

            // Invoke factory processors registered as beans in the context.
            invokeBeanFactoryPostProcessors(beanFactory);

            // Register bean processors that intercept bean creation.
            registerBeanPostProcessors(beanFactory);

            // Initialize message source for this context.
            initMessageSource();

            // Initialize event multicaster for this context.
            initApplicationEventMulticaster();

            // Initialize other special beans in specific context subclasses.
            onRefresh();

            // Check for listener beans and register them.
            registerListeners();

            // Instantiate all remaining (non-lazy-init) singletons.
            finishBeanFactoryInitialization(beanFactory);

            // Last step: publish corresponding event.
            finishRefresh();
        }

        catch (BeansException ex) {
            if (logger.isWarnEnabled()) {
                logger.warn("Exception encountered during context initialization - " +
                            "cancelling refresh attempt: " + ex);
            }

            // Destroy already created singletons to avoid dangling resources.
            destroyBeans();

            // Reset 'active' flag.
            cancelRefresh(ex);

            // Propagate exception to caller.
            throw ex;
        }

        finally {
            // Reset common introspection caches in Spring's core, since we
            // might not ever need metadata for singleton beans anymore...
            resetCommonCaches();
        }
    }
}
```



```java
protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    // 使用context的classLoader
    beanFactory.setBeanClassLoader(getClassLoader());
    //设置SpelExpressionParser ： #{ }
    beanFactory.setBeanExpressionResolver(new StandardBeanExpressionResolver(beanFactory.getBeanClassLoader()));
    //添加一系列的PropertyEditor，具体可以查看ResourceEditorRegistrar#registerCustomEditors
    beanFactory.addPropertyEditorRegistrar(new ResourceEditorRegistrar(this, getEnvironment()));

    //通过ApplicationContextAwareProcessor对实现aware接口的Bean进行invokeAwareInterfaces，aware接口有：
    //EnvironmentAware/EmbeddedValueResolverAware/EmbeddedValueResolverAware/ResourceLoaderAware/ApplicationEventPublisherAware/MessageSourceAware/ApplicationContextAware
    beanFactory.addBeanPostProcessor(new ApplicationContextAwareProcessor(this));
    beanFactory.ignoreDependencyInterface(EnvironmentAware.class);
    beanFactory.ignoreDependencyInterface(EmbeddedValueResolverAware.class);
    beanFactory.ignoreDependencyInterface(ResourceLoaderAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationEventPublisherAware.class);
    beanFactory.ignoreDependencyInterface(MessageSourceAware.class);
    beanFactory.ignoreDependencyInterface(ApplicationContextAware.class);

    // BeanFactory interface not registered as resolvable type in a plain factory.
    // MessageSource registered (and found for autowiring) as a bean.
    //注册依赖类型的自动注入解析规则：第一个参数为依赖类型，第二个参数为自动注入对象值
    //registerResolvableDependency(Class<?> dependencyType, @Nullable Object autowiredValue)
    beanFactory.registerResolvableDependency(BeanFactory.class, beanFactory);
    beanFactory.registerResolvableDependency(ResourceLoader.class, this);
    beanFactory.registerResolvableDependency(ApplicationEventPublisher.class, this);
    beanFactory.registerResolvableDependency(ApplicationContext.class, this);

    // 检测实现ApplicationListener接口的bean，如果是单例bean则添加到context的applicationListeners集合中
    beanFactory.addBeanPostProcessor(new ApplicationListenerDetector(this));

    // Detect a LoadTimeWeaver and prepare for weaving, if found. loadTimeWeaver
    if (beanFactory.containsBean(LOAD_TIME_WEAVER_BEAN_NAME)) {
        //对实现LoadTimeWeaverAware接口的bean进行setLoadTimeWeaver
        beanFactory.addBeanPostProcessor(new LoadTimeWeaverAwareProcessor(beanFactory));
        // Set a temporary ClassLoader for type matching.
        beanFactory.setTempClassLoader(new ContextTypeMatchClassLoader(beanFactory.getBeanClassLoader()));
    }

    // 注册environment（ConfigurableEnvironment）/systemProperties/systemEnvironment的单例bean
    if (!beanFactory.containsLocalBean(ENVIRONMENT_BEAN_NAME)) {
        beanFactory.registerSingleton(ENVIRONMENT_BEAN_NAME, getEnvironment());
    }
    if (!beanFactory.containsLocalBean(SYSTEM_PROPERTIES_BEAN_NAME)) {
        beanFactory.registerSingleton(SYSTEM_PROPERTIES_BEAN_NAME, getEnvironment().getSystemProperties());
    }
    if (!beanFactory.containsLocalBean(SYSTEM_ENVIRONMENT_BEAN_NAME)) {
        beanFactory.registerSingleton(SYSTEM_ENVIRONMENT_BEAN_NAME, getEnvironment().getSystemEnvironment());
    }
}
```





`ApplicationContextAwareProcessor#postProcessBeforeInitialization`（实现了`BeanPostProcessor`接口）

`postProcessBeforeInitialization`方法里面进行了`invokeAwareInterfaces()`

`ApplicationContextAwareProcessor#invokeAwareInterfaces`

```java
private void invokeAwareInterfaces(Object bean) {
    if (bean instanceof EnvironmentAware) {
        ((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());
    }
    if (bean instanceof EmbeddedValueResolverAware) {
        ((EmbeddedValueResolverAware) bean).setEmbeddedValueResolver(this.embeddedValueResolver);
    }
    if (bean instanceof ResourceLoaderAware) {
        ((ResourceLoaderAware) bean).setResourceLoader(this.applicationContext);
    }
    if (bean instanceof ApplicationEventPublisherAware) {
        ((ApplicationEventPublisherAware) bean).setApplicationEventPublisher(this.applicationContext);
    }
    if (bean instanceof MessageSourceAware) {
        ((MessageSourceAware) bean).setMessageSource(this.applicationContext);
    }
    if (bean instanceof ApplicationContextAware) {
        ((ApplicationContextAware) bean).setApplicationContext(this.applicationContext);
    }
}
```



`org.springframework.context.support.AbstractApplicationContext#invokeBeanFactoryPostProcessors`方法主要是执行了

`PostProcessorRegistrationDelegate#invokeBeanFactoryPostProcessors`方法，其中第二个参数`beanFactoryPostProcessors`=



![1586227788766](D:\Temp\cloud_demo\note\ioc\1586227788766.png)





```java
public static void invokeBeanFactoryPostProcessors(
			ConfigurableListableBeanFactory beanFactory, List<BeanFactoryPostProcessor> beanFactoryPostProcessors) {

    // Invoke BeanDefinitionRegistryPostProcessors first, if any.
    Set<String> processedBeans = new HashSet<>();

    if (beanFactory instanceof BeanDefinitionRegistry) {
        BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
        //对beanFactoryPostProcessors是否实现BeanDefinitionRegistryPostProcessor接口进行分组
        List<BeanFactoryPostProcessor> regularPostProcessors = new ArrayList<>();
        List<BeanDefinitionRegistryPostProcessor> registryProcessors = new ArrayList<>();

        for (BeanFactoryPostProcessor postProcessor : beanFactoryPostProcessors) {
            if (postProcessor instanceof BeanDefinitionRegistryPostProcessor) {
                BeanDefinitionRegistryPostProcessor registryProcessor =
                    (BeanDefinitionRegistryPostProcessor) postProcessor;
                //BeanDefinitionRegistryPostProcessors回调postProcessBeanDefinitionRegistry方法
                registryProcessor.postProcessBeanDefinitionRegistry(registry);
                registryProcessors.add(registryProcessor);
            }
            else {
                regularPostProcessors.add(postProcessor);
            }
        }

        // Do not initialize FactoryBeans here: We need to leave all regular beans
        // uninitialized to let the bean factory post-processors apply to them!
        // Separate between BeanDefinitionRegistryPostProcessors that implement
        // PriorityOrdered, Ordered, and the rest.
        List<BeanDefinitionRegistryPostProcessor> currentRegistryProcessors = new ArrayList<>();

        // First, invoke the BeanDefinitionRegistryPostProcessors that implement PriorityOrdered.
        
        //从beanDefinitionNames和manualSingletonNames中获取对应类型的beanName
        //在这里的测试获取到只有：internalConfigurationAnnotationProcessor
        String[] postProcessorNames =
            beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
        for (String ppName : postProcessorNames) {
            if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
                currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                processedBeans.add(ppName);
            }
        }
        sortPostProcessors(currentRegistryProcessors, beanFactory);
        registryProcessors.addAll(currentRegistryProcessors);
        invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
        currentRegistryProcessors.clear();

        // Next, invoke the BeanDefinitionRegistryPostProcessors that implement Ordered.
        postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
        for (String ppName : postProcessorNames) {
            if (!processedBeans.contains(ppName) && beanFactory.isTypeMatch(ppName, Ordered.class)) {
                currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                processedBeans.add(ppName);
            }
        }
        sortPostProcessors(currentRegistryProcessors, beanFactory);
        registryProcessors.addAll(currentRegistryProcessors);
        invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
        currentRegistryProcessors.clear();

        // Finally, invoke all other BeanDefinitionRegistryPostProcessors until no further ones appear.
        boolean reiterate = true;
        while (reiterate) {
            reiterate = false;
            postProcessorNames = beanFactory.getBeanNamesForType(BeanDefinitionRegistryPostProcessor.class, true, false);
            for (String ppName : postProcessorNames) {
                if (!processedBeans.contains(ppName)) {
                    currentRegistryProcessors.add(beanFactory.getBean(ppName, BeanDefinitionRegistryPostProcessor.class));
                    processedBeans.add(ppName);
                    reiterate = true;
                }
            }
            sortPostProcessors(currentRegistryProcessors, beanFactory);
            registryProcessors.addAll(currentRegistryProcessors);
            invokeBeanDefinitionRegistryPostProcessors(currentRegistryProcessors, registry);
            currentRegistryProcessors.clear();
        }

        // Now, invoke the postProcessBeanFactory callback of all processors handled so far.
        invokeBeanFactoryPostProcessors(registryProcessors, beanFactory);
        invokeBeanFactoryPostProcessors(regularPostProcessors, beanFactory);
    }

    else {
        // Invoke factory processors registered with the context instance.
        invokeBeanFactoryPostProcessors(beanFactoryPostProcessors, beanFactory);
    }

    // Do not initialize FactoryBeans here: We need to leave all regular beans
    // uninitialized to let the bean factory post-processors apply to them!
    String[] postProcessorNames =
        beanFactory.getBeanNamesForType(BeanFactoryPostProcessor.class, true, false);

    // Separate between BeanFactoryPostProcessors that implement PriorityOrdered,
    // Ordered, and the rest.
    List<BeanFactoryPostProcessor> priorityOrderedPostProcessors = new ArrayList<>();
    List<String> orderedPostProcessorNames = new ArrayList<>();
    List<String> nonOrderedPostProcessorNames = new ArrayList<>();
    for (String ppName : postProcessorNames) {
        if (processedBeans.contains(ppName)) {
            // skip - already processed in first phase above
        }
        else if (beanFactory.isTypeMatch(ppName, PriorityOrdered.class)) {
            priorityOrderedPostProcessors.add(beanFactory.getBean(ppName, BeanFactoryPostProcessor.class));
        }
        else if (beanFactory.isTypeMatch(ppName, Ordered.class)) {
            orderedPostProcessorNames.add(ppName);
        }
        else {
            nonOrderedPostProcessorNames.add(ppName);
        }
    }

    // First, invoke the BeanFactoryPostProcessors that implement PriorityOrdered.
    sortPostProcessors(priorityOrderedPostProcessors, beanFactory);
    invokeBeanFactoryPostProcessors(priorityOrderedPostProcessors, beanFactory);

    // Next, invoke the BeanFactoryPostProcessors that implement Ordered.
    List<BeanFactoryPostProcessor> orderedPostProcessors = new ArrayList<>(orderedPostProcessorNames.size());
    for (String postProcessorName : orderedPostProcessorNames) {
        orderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
    }
    sortPostProcessors(orderedPostProcessors, beanFactory);
    invokeBeanFactoryPostProcessors(orderedPostProcessors, beanFactory);

    // Finally, invoke all other BeanFactoryPostProcessors.
    List<BeanFactoryPostProcessor> nonOrderedPostProcessors = new ArrayList<>(nonOrderedPostProcessorNames.size());
    for (String postProcessorName : nonOrderedPostProcessorNames) {
        nonOrderedPostProcessors.add(beanFactory.getBean(postProcessorName, BeanFactoryPostProcessor.class));
    }
    invokeBeanFactoryPostProcessors(nonOrderedPostProcessors, beanFactory);

    // Clear cached merged bean definitions since the post-processors might have
    // modified the original metadata, e.g. replacing placeholders in values...
    beanFactory.clearMetadataCache();
}
```





`AbstractBeanFactory#getBean(java.lang.String, java.lang.Class<T>)`

```java
public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
    return doGetBean(name, requiredType, null, false);
}


protected <T> T doGetBean(final String name, @Nullable final Class<T> requiredType,
                          @Nullable final Object[] args, boolean typeCheckOnly) throws BeansException {

    //如果beanName以FACTORY_BEAN_PREFIX开头则对beanName进行substrings删减掉PREFIX，并判断是否为别名，获取原生的BeanName
    final String beanName = transformedBeanName(name);
    Object bean;

    // 调用getSingleton(beanName, true)，从singletonObjects获取bean对象，
    Object sharedInstance = getSingleton(beanName);
    
    //
    if (sharedInstance != null && args == null) {    
        bean = getObjectForBeanInstance(sharedInstance, name, beanName, null);
    }

    else {
      //是否当前线程正在创建原型的bean
        if (isPrototypeCurrentlyInCreation(beanName)) {
            throw new BeanCurrentlyInCreationException(beanName);
        }

        // Check if bean definition exists in this factory.
        BeanFactory parentBeanFactory = getParentBeanFactory();
        //parentBeanFactory==null
        if (parentBeanFactory != null && !containsBeanDefinition(beanName)) {
            // Not found -> check parent.
            String nameToLookup = originalBeanName(name);
            if (parentBeanFactory instanceof AbstractBeanFactory) {
                return ((AbstractBeanFactory) parentBeanFactory).doGetBean(
                    nameToLookup, requiredType, args, typeCheckOnly);
            }
            else if (args != null) {
                // Delegation to parent with explicit args.
                return (T) parentBeanFactory.getBean(nameToLookup, args);
            }
            else if (requiredType != null) {
                // No args -> delegate to standard getBean method.
                return parentBeanFactory.getBean(nameToLookup, requiredType);
            }
            else {
                return (T) parentBeanFactory.getBean(nameToLookup);
            }
        }

        if (!typeCheckOnly) {
            //标记当前beanName的bean将要被创建或已经创建
            //如果是RootBeanDefinition，则设置state=true（该definition是否需要重新合并）
            //将beanName添加到beanFactory的alreadyCreated集合中
            markBeanAsCreated(beanName);
        }

        try {
            //从mergedBeanDefinitions中获取，如果RootBeanDefinition的state=true则从beanDefinitionMap中获取
            final RootBeanDefinition mbd = getMergedLocalBeanDefinition(beanName);
            checkMergedBeanDefinition(mbd, beanName, args);

            // Guarantee initialization of beans that the current bean depends on.
            String[] dependsOn = mbd.getDependsOn();
            if (dependsOn != null) {
                for (String dep : dependsOn) {
                    if (isDependent(beanName, dep)) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                                        "Circular depends-on relationship between '" + beanName + "' and '" + dep + "'");
                    }
                    registerDependentBean(dep, beanName);
                    try {
                        getBean(dep);
                    }
                    catch (NoSuchBeanDefinitionException ex) {
                        throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                                        "'" + beanName + "' depends on missing bean '" + dep + "'", ex);
                    }
                }
            }

           //创建单例bean的实例化对象
            if (mbd.isSingleton()) {
                sharedInstance = getSingleton(beanName, () -> {
                    try {
                        return createBean(beanName, mbd, args);
                    }
                    catch (BeansException ex) {
                        // Explicitly remove instance from singleton cache: It might have been put there
                        // eagerly by the creation process, to allow for circular reference resolution.
                        // Also remove any beans that received a temporary reference to the bean.
                        destroySingleton(beanName);
                        throw ex;
                    }
                });
                bean = getObjectForBeanInstance(sharedInstance, name, beanName, mbd);
            }

            else if (mbd.isPrototype()) {
                // It's a prototype -> create a new instance.
                Object prototypeInstance = null;
                try {
                    beforePrototypeCreation(beanName);
                    prototypeInstance = createBean(beanName, mbd, args);
                }
                finally {
                    afterPrototypeCreation(beanName);
                }
                bean = getObjectForBeanInstance(prototypeInstance, name, beanName, mbd);
            }

            else {
                String scopeName = mbd.getScope();
                final Scope scope = this.scopes.get(scopeName);
                if (scope == null) {
                    throw new IllegalStateException("No Scope registered for scope name '" + scopeName + "'");
                }
                try {
                    Object scopedInstance = scope.get(beanName, () -> {
                        beforePrototypeCreation(beanName);
                        try {
                            return createBean(beanName, mbd, args);
                        }
                        finally {
                            afterPrototypeCreation(beanName);
                        }
                    });
                    bean = getObjectForBeanInstance(scopedInstance, name, beanName, mbd);
                }
                catch (IllegalStateException ex) {
                    throw new BeanCreationException(beanName,
                                                    "Scope '" + scopeName + "' is not active for the current thread; consider " +
                                                    "defining a scoped proxy for this bean if you intend to refer to it from a singleton",
                                                    ex);
                }
            }
        }
        catch (BeansException ex) {
            cleanupAfterBeanCreationFailure(beanName);
            throw ex;
        }
    }

    // Check if required type matches the type of the actual bean instance.
    if (requiredType != null && !requiredType.isInstance(bean)) {
        try {
            T convertedBean = getTypeConverter().convertIfNecessary(bean, requiredType);
            if (convertedBean == null) {
                throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
            }
            return convertedBean;
        }
        catch (TypeMismatchException ex) {
            if (logger.isTraceEnabled()) {
                logger.trace("Failed to convert bean '" + name + "' to required type '" +
                             ClassUtils.getQualifiedName(requiredType) + "'", ex);
            }
            throw new BeanNotOfRequiredTypeException(name, requiredType, bean.getClass());
        }
    }
    return (T) bean;
}
```



`org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton(java.lang.String, boolean)`

获取顺序：从`singletonObjects`获取bean -> 从`earlySingletonObjects`获取bean -> 从 `singletonFactories`获取`ObjectFactory`，通过`ObjecFactory#getObject`创建bean

```java
protected Object getSingleton(String beanName, boolean allowEarlyReference) {
    //从存放单例Bean的singletonObjects中获取
		Object singletonObject = this.singletonObjects.get(beanName);
    //如果beanName在singletonsCurrentlyInCreation中，
		if (singletonObject == null && isSingletonCurrentlyInCreation(beanName)) {
			synchronized (this.singletonObjects) {
                //如果在earlySingletonObjects中存在
				singletonObject = this.earlySingletonObjects.get(beanName);
                //allowEarlyReference==true
				if (singletonObject == null && allowEarlyReference) {
                    //从singletonFactories中获取ObjectFactory
					ObjectFactory<?> singletonFactory = this.singletonFactories.get(beanName);
					if (singletonFactory != null) {
                        //通过ObjecFactory的getObject()方法创建
						singletonObject = singletonFactory.getObject();
						this.earlySingletonObjects.put(beanName, singletonObject);
						this.singletonFactories.remove(beanName);
					}
				}
			}
		}
		return singletonObject;
	}
```





`org.springframework.beans.factory.support.DefaultSingletonBeanRegistry#getSingleton(java.lang.String, org.springframework.beans.factory.ObjectFactory<?>)`

```java
public Object getSingleton(String beanName, ObjectFactory<?> singletonFactory) {
    synchronized (this.singletonObjects) {
        Object singletonObject = this.singletonObjects.get(beanName);
        if (singletonObject == null) {
            //判断beanName是否inCreationCheckExclusions，并添加到singletonsCurrentlyInCreation中
            beforeSingletonCreation(beanName);
            boolean newSingleton = false;
            boolean recordSuppressedExceptions = (this.suppressedExceptions == null);
            if (recordSuppressedExceptions) {
                this.suppressedExceptions = new LinkedHashSet<>();
            }
            try {
                //AbstractAutowireCapableBeanFactory#createBean(java.lang.String, RootBeanDefinition, Object[])
                singletonObject = singletonFactory.getObject();
                newSingleton = true;
            }
            catch (IllegalStateException ex) {
                // Has the singleton object implicitly appeared in the meantime ->
                // if yes, proceed with it since the exception indicates that state.
                singletonObject = this.singletonObjects.get(beanName);
                if (singletonObject == null) {
                    throw ex;
                }
            }
            catch (BeanCreationException ex) {
                if (recordSuppressedExceptions) {
                    for (Exception suppressedException : this.suppressedExceptions) {
                        ex.addRelatedCause(suppressedException);
                    }
                }
                throw ex;
            }
            finally {
                if (recordSuppressedExceptions) {
                    this.suppressedExceptions = null;
                }
                afterSingletonCreation(beanName);
            }
            if (newSingleton) {
                addSingleton(beanName, singletonObject);
            }
        }
        return singletonObject;
    }
}

```



`AbstractAutowireCapableBeanFactory#createBean(java.lang.String, RootBeanDefinition, Object[])`

```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args)
    throws BeanCreationException {
    RootBeanDefinition mbdToUse = mbd;

    //解析Bean的Class
    Class<?> resolvedClass = resolveBeanClass(mbd, beanName);
    if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {
        mbdToUse = new RootBeanDefinition(mbd);
        mbdToUse.setBeanClass(resolvedClass);
    }

    // Prepare method overrides.
    try {
        mbdToUse.prepareMethodOverrides();
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),
                                               beanName, "Validation of method overrides failed", ex);
    }

    try {
       //执行所有的InstantiationAwareBeanPostProcessor，进行实例化前的回调
        Object bean = resolveBeforeInstantiation(beanName, mbdToUse);
        if (bean != null) {
            return bean;
        }
    }
    catch (Throwable ex) {
        throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,
                                        "BeanPostProcessor before instantiation of bean failed", ex);
    }

    try {
        //进行实例化bean
        Object beanInstance = doCreateBean(beanName, mbdToUse, args);
        if (logger.isTraceEnabled()) {
            logger.trace("Finished creating instance of bean '" + beanName + "'");
        }
        return beanInstance;
    }
    catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
        // A previously detected exception with proper bean creation context already,
        // or illegal singleton state to be communicated up to DefaultSingletonBeanRegistry.
        throw ex;
    }
    catch (Throwable ex) {
        throw new BeanCreationException(
            mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);
    }
}
```





```java
protected Object doCreateBean(final String beanName, final RootBeanDefinition mbd, final @Nullable Object[] args)
    throws BeanCreationException {

    // Instantiate the bean.
    BeanWrapper instanceWrapper = null;
    if (mbd.isSingleton()) {
        instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);
    }
    if (instanceWrapper == null) {
        instanceWrapper = createBeanInstance(beanName, mbd, args);
    }
    final Object bean = instanceWrapper.getWrappedInstance();
    Class<?> beanType = instanceWrapper.getWrappedClass();
    if (beanType != NullBean.class) {
        mbd.resolvedTargetType = beanType;
    }

    // Allow post-processors to modify the merged bean definition.
    synchronized (mbd.postProcessingLock) {
        if (!mbd.postProcessed) {
            try {
                //回调所有MergedBeanDefinitionPostProcessor的postProcessMergedBeanDefinition方法
                applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);
            }
            catch (Throwable ex) {
                throw new BeanCreationException(mbd.getResourceDescription(), beanName,
                                                "Post-processing of merged bean definition failed", ex);
            }
            mbd.postProcessed = true;
        }
    }

    // Eagerly cache singletons to be able to resolve circular references
    // even when triggered by lifecycle interfaces like BeanFactoryAware.
    
    //是否进行earlySingletonExposure暴露：true， allowCircularReferences==true
    //将bean添加到singletonObjects中
    boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences &&
                                      isSingletonCurrentlyInCreation(beanName));
    if (earlySingletonExposure) {
        if (logger.isTraceEnabled()) {
            logger.trace("Eagerly caching bean '" + beanName +
                         "' to allow for resolving potential circular references");
        }
        addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));
    }

    // Initialize the bean instance.
    Object exposedObject = bean;
    try {
        populateBean(beanName, mbd, instanceWrapper);
        exposedObject = initializeBean(beanName, exposedObject, mbd);
    }
    catch (Throwable ex) {
        if (ex instanceof BeanCreationException && beanName.equals(((BeanCreationException) ex).getBeanName())) {
            throw (BeanCreationException) ex;
        }
        else {
            throw new BeanCreationException(
                mbd.getResourceDescription(), beanName, "Initialization of bean failed", ex);
        }
    }

    if (earlySingletonExposure) {
        Object earlySingletonReference = getSingleton(beanName, false);
        if (earlySingletonReference != null) {
            if (exposedObject == bean) {
                exposedObject = earlySingletonReference;
            }
            else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {
                String[] dependentBeans = getDependentBeans(beanName);
                Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);
                for (String dependentBean : dependentBeans) {
                    if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {
                        actualDependentBeans.add(dependentBean);
                    }
                }
                if (!actualDependentBeans.isEmpty()) {
                    throw new BeanCurrentlyInCreationException(beanName,
                                                               "Bean with name '" + beanName + "' has been injected into other beans [" +
                                                               StringUtils.collectionToCommaDelimitedString(actualDependentBeans) +
                                                               "] in its raw version as part of a circular reference, but has eventually been " +
                                                               "wrapped. This means that said other beans do not use the final version of the " +
                                                               "bean. This is often the result of over-eager type matching - consider using " +
                                                               "'getBeanNamesOfType' with the 'allowEagerInit' flag turned off, for example.");
                }
            }
        }
    }

    // Register bean as disposable.
    try {
        registerDisposableBeanIfNecessary(beanName, bean, mbd);
    }
    catch (BeanDefinitionValidationException ex) {
        throw new BeanCreationException(
            mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);
    }

    return exposedObject;
}
```





`org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory#createBeanInstance`

```java
protected BeanWrapper createBeanInstance(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) {
    // Make sure bean class is actually resolved at this point.
    Class<?> beanClass = resolveBeanClass(mbd, beanName);

    Supplier<?> instanceSupplier = mbd.getInstanceSupplier();
    if (instanceSupplier != null) {
        return obtainFromSupplier(instanceSupplier, beanName);
    }

    if (mbd.getFactoryMethodName() != null) {
        return instantiateUsingFactoryMethod(beanName, mbd, args);
    }

    // Shortcut when re-creating the same bean...
    boolean resolved = false;
    boolean autowireNecessary = false;
    if (args == null) {
        synchronized (mbd.constructorArgumentLock) {
            if (mbd.resolvedConstructorOrFactoryMethod != null) {
                resolved = true;
                autowireNecessary = mbd.constructorArgumentsResolved;
            }
        }
    }
    if (resolved) {
        if (autowireNecessary) {
            return autowireConstructor(beanName, mbd, null, null);
        }
        else {
            return instantiateBean(beanName, mbd);
        }
    }

    // Candidate constructors for autowiring?
    //如果存在任一InstantiationAwareBeanPostProcessors已被注册，则通过SmartInstantiationAwareBeanPostProcessor推断候选构造方法
    Constructor<?>[] ctors = determineConstructorsFromBeanPostProcessors(beanClass, beanName);
    if (ctors != null || mbd.getResolvedAutowireMode() == AUTOWIRE_CONSTRUCTOR ||
        mbd.hasConstructorArgumentValues() || !ObjectUtils.isEmpty(args)) {
        return autowireConstructor(beanName, mbd, ctors, args);
    }

    // Preferred constructors for default construction?
    ctors = mbd.getPreferredConstructors();
    if (ctors != null) {
        return autowireConstructor(beanName, mbd, ctors, null);
    }

    // 使用无参构造方法实例化，并进行PropertyEditor的属性修改方法
    return instantiateBean(beanName, mbd);
}
```







bean生命周期

1. 执行所有的InstantiationAwareBeanPostProcessor，进行实例化前的回调
2. 如果存在任一InstantiationAwareBeanPostProcessors已被注册，则通过SmartInstantiationAwareBeanPostProcessor推断候选构造方法
3. 使用无参构造方法实例化，并进行PropertyEditor的属性修改方法
4.  回调所有MergedBeanDefinitionPostProcessor的postProcessMergedBeanDefinition方法
5.   populateBean(beanName, mbd, instanceWrapper); `InstantiationAwareBeanPostProcessor#postProcessAfterInstantiation`
6.  
7.  
8. 