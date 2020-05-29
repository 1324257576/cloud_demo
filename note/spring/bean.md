实例化bean对象
设置bean属性
注入aware接口的依赖
`BeanPostProcessor.postProcessBeforeInitialization()`
`InitializingBean.afterPropertiesSet`
`Init-Method``@PostConstruct`
`BeanPostProcessor.postProcessAfterInitialization()`
创建完成
`DisposableBean.destroy()`
`Destroy-method``@PreDestroy`
销毁对象完成





注册`mappingInfo`：

`AbstractHandlerMethodMapping#initHandlerMethods`

扫描context中的所有bean（`AbstractHandlerMethodMapping#getCandidateBeanNames`），

通过检查是否存在类型级别的@Controller或者`@RequestMapping`注解（`RequestMappingHandlerMapping#isHandler`），

从而决定是否对bean进行`detectHandlerMethods(beanName)`检测。

对类型级别和方法级别的`@RequestMapping`信息进行合并成为`RequestMappingInfo`（`RequestMappingHandlerMapping#getMappingForMethod`）。

对`RequestMappingInfo`注册到`mappingRegistry`中（`AbstractHandlerMethodMapping#registerHandlerMethod`），保存到

`mappingRegistry`的`Map<T, HandlerMethod> mappingLookup`、`MultiValueMap<String, T> urlLookup`等map中。



匹配：

`AbstractHandlerMethodMapping#lookupHandlerMethod`根据request请求解析出的`lookupPath`在`HanlderMethods`中找到合适的`HandlerMethod`，根据匹配规则选择最匹配的一个，并用于处理request请求。































