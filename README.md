# MyFrameWork

简单IOC容器+AOP实现

本容器支持IOC单例注入和原型非单列注入，支持postProcessor，允许在初始化之后依据自己的需要再次对bean进行处理。IOC支持属性注入，方法注入；支持构造器注入和工厂方法注入。

AOP基于JDK接口动态代理功能实现，支持方法级增强，被增强的方法需要来自接口。

主要注解：
- @Bean 声明一个bean
- @MFConfig 声明一个配置类
- @Import 在一个配置类中引入其他配置类
- @Inject 声明需要注入，并且声明按名字注入还是按类型注入，默认按名字注入
- @Order 声明Processor的生效顺序，越大越优先
- @Enhancer 声明一个AOP切面定义类
- @EnhanceDefinition 声明一个切面，支持前增强，后增强，环绕增强

使用基本流程
1. 使用@MFConfig注解主入口，可以使用@Import注解引入其他的配置类。配置类中可以定义bean的构建bean工厂方法，在一个方法上使用@Bean注解即可定义一个bean，容器会自动执行该方法并将返回值作为一个bean。
同时该方法可以有需要IOC容器注入的bean的参数，使用@Inject注解可以指明参数bean的注入方式
2. 使用@Bean在需要声明为bean的类上注解，可以使用构造方法注入。如果使用构造方法注入只需在特定的构造方法上使用@Inject注解标记，只有第一个被注解的构造方法有效。如果不使用构造方法注入那么必须
保证拥有一个无参构造方法。属性可以使用@Inject标注声明需要IOC容器注入，其他实例方法也可以使用@Inject注解声明需要IOC容器注入。如果是构造方法构造bean那么属性和实例方法的@Inject不会生效。
如果是无参构造的bean，其中所有每@Inject注解标记的属性都会被IOC容器尝试注入，所有被注解的实例方法会被IOC容器尝试注入和执行。被标记的实例方法需要是public修饰。
3. 使用@Bean声明一个实现了BeanProcessor接口的类，该类会被IOC容器识别为后处理类，里面的唯一函数方法会在每个bean初始化之后应用一次。BeanProcessor默认不支持任何注入，如果需要注入则需要在初始化BeanFactory时指定，BeanProcessor依赖链上的所有bean都不会被本processor处理。
4. 使用@Enhancer注解在一个类上声明为AOP增强声明类。该类自动被声明为一个bean。其中所有的方法可以时用@EnhanceDefinition注解标记，被标记的方法被视为切面增强方法，在该注解中指定某个bean的某个签名的方法为当前增强方法的目标方法。
5. 在需要时用bean的时候实例化AdvancedBeanFactory，构造方法需要传入主配置类，可以选择指定processor是否支持依赖注入。

注意事项
- @Bean中type为singleton标记的类被视为是单例bean，该bean由容器保管，始终只有一个bean。如果声明为Prototype类型，那么则是非单例bean，每次要求获取该bean的时候由IOC容器临时创建并返回，IOC容器不管理prototype类型的bean。
- 单例bean支持循环依赖注入，非单例bean不支持循环依赖注入。processor依赖链上的bean不会被其处理，processor也不处理自身。
- AOP基于JDK的接口动态代理实现，只有来自接口的方法才能被AOP增强。

testEntity包下有简单的使用范例。
