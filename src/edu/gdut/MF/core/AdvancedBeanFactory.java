package edu.gdut.MF.core;

import edu.gdut.MF.Enum.BeanScope;
import edu.gdut.MF.Enum.InjectionType;
import edu.gdut.MF.annotation.*;
import edu.gdut.MF.exception.MFException;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AdvancedBeanFactory {
    // 这个类不是线程安全的
    ClassLoader classLoader; // 保存类加载器

    private ResourceResolver resourceResolver; // 保存资源解析器

    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>(); // bean储存map

    private final Class<?> mainConfigClass; // 主配置类

    private final BeanProcessor.ProcessorOption processorOption; // 处理器模式选项（处理器构建能否注入）

    private List<BeanDefinition> processorList = new ArrayList<>(); // 处理器储存list
    private Set<Class<?>> allConfiguration = new HashSet<>(); // 所有配置类的储存set

    Map<BeanDefinition, Set<BeanDefinition>> processorDependenceMap = new HashMap<>();
    // 处理器依赖bean的储存map，启用处理器注入允许选项启用

    public AdvancedBeanFactory(Class<?> config) {
        if (!config.isAnnotationPresent(MFConfig.class) || config.isInterface() || config.isAnnotation())
            throw new MFException(config.getName() + " is not a config class");
        this.mainConfigClass = config;
        processorOption = BeanProcessor.ProcessorOption.DEFAULT; // 设置为默认处理器模式
        getClassLoader();
        getResolver();
        prepare();
        initial();
    }

    public AdvancedBeanFactory(Class<?> config, BeanProcessor.ProcessorOption processorOption) {
        if (!config.isAnnotationPresent(MFConfig.class) || config.isInterface() || config.isAnnotation())
            throw new MFException(config.getName() + " is not a config class");
        this.mainConfigClass = config;
        this.processorOption = processorOption;
        getClassLoader();
        getResolver();
        prepare();
        initial();
    }

    private void prepare() { // 找出所有的bean的定义并且收集完所需要的信息

        // 找出所有的配置类
        allConfiguration = getAllConfiguration(mainConfigClass);
        Set<String> needToInit = new HashSet<>();
        allConfiguration.forEach(item -> { // 扫描出所有的class
            String name = item.getPackage().getName();
            resourceResolver.scan(name, ResourceResolver.ResourceType.CLASS, ResourceResolver.ResourceMapper.GETCLASS);
            needToInit.addAll(resourceResolver.getResultList());
        });

        // 创建出所有的beanDefinition，可来自类上的Bean和方法上的Bean
        // 配置类中方法的bean
        allConfiguration.forEach(item -> {
            // 获取所有自己的方法，但是不能是private
            Method[] declaredMethods = item.getDeclaredMethods();
            for (Method m : declaredMethods) {
                if (!m.isAnnotationPresent(Bean.class))
                    continue;
                Bean annotation = m.getAnnotation(Bean.class);
                BeanDefinition beanDefinition = new BeanDefinition();
                if (annotation.value().equals("")) { // bean名字
                    beanDefinition.beanName = firstToLower(m.getName());
                } else {
                    beanDefinition.beanName = annotation.value();
                }
                if (beanDefinitionMap.putIfAbsent(beanDefinition.beanName, beanDefinition) != null)
                    throw new MFException("duplicated name of bean");
                if (m.getReturnType().equals(Void.TYPE))
                    throw new MFException("factory method can't return void");
                beanDefinition.beanClass = m.getReturnType();
                beanDefinition.factoryMethod = m;
                beanDefinition.factoryName = firstToLower(item.getName()); // 记录本配置类的名称，配置类只能是取类名
                beanDefinition.priority = annotation.priority();
                beanDefinition.scope = annotation.scope();
                if (isProcessor(beanDefinition.beanClass)) { // 找出processor的定义
                    processorList.add(beanDefinition);
                    Order order = m.getAnnotation(Order.class);
                    if (order != null)
                        beanDefinition.order = order.value();
                }
            }
        });
        List<BeanDefinition> tmplist = new ArrayList<>(); // 用于暂时存配置类
        // 类上定义的Bean
        needToInit.forEach(item -> {
            Class<?> cs;
            try {
                cs = classLoader.loadClass(item);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            if (cs.isAnnotation() || cs.isInterface()) // 注解和接口不参与构建bean（不能实例化没有意义）
                return;
            Bean annotation = (Bean) isAnnotationOn(Bean.class, cs, null);
            if (annotation == null)
                return;
            BeanDefinition beanDefinition = new BeanDefinition();
            if (annotation.value().equals("")) { // bean名字
                beanDefinition.beanName = firstToLower(cs.getName());
            } else {
                beanDefinition.beanName = annotation.value();
            }
            if (beanDefinitionMap.put(beanDefinition.beanName, beanDefinition) != null)// 存入
                throw new MFException("duplicated name of bean");
            beanDefinition.beanClass = cs;
            beanDefinition.priority = annotation.priority();
            beanDefinition.scope = annotation.scope();
            if (isAnnotationOn(MFConfig.class, cs, null) != null) {
                // 如果是配置类就直接创建实例，不再在后面创建, 默认是弱依赖，加入list待处理)
                tmplist.add(beanDefinition);
            } else { // 不是配置类的还需找出是否是构造器注入
                Constructor<?>[] declaredConstructors = cs.getDeclaredConstructors();
                for (Constructor<?> constructor : declaredConstructors)
                    if (constructor.isAnnotationPresent(Inject.class)) {
                        if (constructor.getParameters().length != 0) // 无参的归入弱依赖
                            beanDefinition.constructor = constructor; // 只能有一个生效
                        break;
                    }
            }
            if (isProcessor(beanDefinition.beanClass))  // 找出processor的定义
                processorList.add(beanDefinition);
        });
        tmplist.forEach(item -> initForWeak(item, null)); // 先初始化化所有的配置类
        processorList = processorList.stream().peek(item -> {
            checkProcessorInject(item);
            if (item.order != null) // 已经取得了order
                return;
            Order annotation = item.beanClass.getAnnotation(Order.class);
            if (annotation == null)
                item.order = 0; // 默认是0
            else
                item.order = annotation.value();
            if (item.beanClass == BeanAopProcessor.class){ // 内置的AOP处理器 直接完成bean的构建和初始化
                Object instance = new BeanAopProcessor(this);
                item.beanInstance = instance;
                item.processedInstance = instance;
            }
        }).sorted((a, b) -> b.order - a.order).collect(Collectors.toList());
        // 为processor排序,准备就绪
    }

    private void initial() {// 构建和注入
        beanDefinitionMap.values().forEach(item -> {
            if (item.scope == BeanScope.PROTOTYPE)
                return; // prototype类型不主动构建和注入
            if (isStrongDependence(item))
                initForStrong(item, null);
            else
                initForWeak(item, null);
        });
        // 四种依赖注入  构造器，工厂方法，字段注入和get/set注入
        // 构造器和工厂方法注入（强依赖注入，循环者不能注入）
    }

    public Object getBean(String beanName) {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition == null)
            return null;
        if (beanDefinition.scope == BeanScope.SINGLETON)
            return beanDefinition.processedInstance;
        if (isStrongDependence(beanDefinition))
            return initForStrong(beanDefinition, null);
        else
            return initForWeak(beanDefinition, null);
    }

    public <T> T getBean(Class<T> beanType) {
        BeanDefinition beanDefinition;
        try {
            beanDefinition = findBeanDefinitionByType(beanType);
            if (beanDefinition.scope == BeanScope.SINGLETON)
                return (T) beanDefinition.processedInstance;
        } catch (MFException | NullPointerException e) {
            return null;
        }
        if (isStrongDependence(beanDefinition))
            return (T) initForStrong(beanDefinition, null);
        else
            return (T) initForWeak(beanDefinition, null);
    }


    Object initForStrong(BeanDefinition beanDefinition, Set<BeanDefinition> noLoop) {
        if (beanDefinition.processedInstance != null) // 不为空直接返回
            return beanDefinition.processedInstance;
        if (noLoop == null) // 初始Set防止循环注入
            noLoop = new HashSet<>();
        Parameter[] parameters;
        List<Object> res = new ArrayList<>(); // 暂存结果
        Method factoryMethod = beanDefinition.factoryMethod;
        Constructor<?> constructor = beanDefinition.constructor;
        BeanDefinition tmp; // 暂中间产生的bean定义
        Inject inject; // 暂存Inject注解
        if (beanDefinition.factoryMethod != null) { // 工厂方法初始化
//            if (factoryMethod.getModifiers() != Modifier.PUBLIC)
//                throw new MFException("factory method is not public");
            parameters = factoryMethod.getParameters();
            inject = factoryMethod.getAnnotation(Inject.class);
        } else { // 指定构造器初始化
            parameters = constructor.getParameters();
            inject = constructor.getAnnotation(Inject.class);
        }
        if (inject == null || inject.value() == InjectionType.INJECTBYNAME) {
            // 判断是否有Inject注解，确定按照何种方式寻找参数实例
            String name;
            for (Parameter p : parameters) {
                name = firstToLower(p.getType().getName());
                tmp = beanDefinitionMap.get(name);
                if (tmp == null)
                    throw new MFException("undeclared bean named " + name);
                if (noLoop.contains(tmp))
                    throw new MFException("loop injection in method or constructor!");
                noLoop.add(tmp); // 这里路径有几种，如果路径上一直是强依赖，那么只要出现重复，必然是循环注入
                if (isStrongDependence(tmp)) // 获取参数的实例
                    res.add(initForStrong(tmp, noLoop));
                else
                    res.add(initForWeak(tmp, noLoop));
                noLoop.remove(tmp); // 成功出来就可以退出了,每个参数之间不影响
            }
        } else {
            Class<?> pclass;
            for (Parameter p : parameters) {
                pclass = p.getType();
                tmp = findBeanDefinitionByType(pclass);
                if (tmp == null)
                    throw new MFException("too many bean for this type, may set someone be priority");
                if (noLoop.contains(tmp))
                    throw new MFException("loop injection!");
                noLoop.add(tmp);
                if (isStrongDependence(tmp)) {
                    res.add(initForStrong(tmp, noLoop));
                } else
                    res.add(initForWeak(tmp, noLoop));
                noLoop.remove(tmp); // 成功出来就可以退出了,每个参数之间不影响
            }
        }
        Object instance; // 记录结果
        try {
            if (constructor != null) {
                constructor.setAccessible(true);
                instance = constructor.newInstance(res.toArray());
            } else {
                Object factory = beanDefinitionMap.get(beanDefinition.factoryName).beanInstance;
                instance = factoryMethod.invoke(factory, res.toArray());
            }
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new MFException("injected mothod not accessible");
        }
        Object afterProcess = applyProcessor(beanDefinition, instance);
        if (beanDefinition.scope == BeanScope.SINGLETON) { // Singleton保存单例, 一定要先保存再去注入，不然单例会被
            beanDefinition.beanInstance = instance;
            beanDefinition.processedInstance = afterProcess;
        }
        return afterProcess;
    }

    Object initForWeak(BeanDefinition beanDefinition, Set<BeanDefinition> noLoop) {
        if (beanDefinition.processedInstance != null) // 不为空直接返回
            return beanDefinition.processedInstance;
        Object instance;
        try {
            Constructor<?> constructor = beanDefinition.beanClass.getConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException
                 | InvocationTargetException | NoSuchMethodException e) {
            System.out.println(e);
            throw new MFException("no param construct failed in initialization, " +
                    "please make sure there's a no param and public constructor");
        }
        Object afterProcess = applyProcessor(beanDefinition, instance);
        if (beanDefinition.scope == BeanScope.SINGLETON) { // Singleton保存单例, 一定要先保存再去注入，不然单例会被
            beanDefinition.beanInstance = instance;
            beanDefinition.processedInstance = afterProcess;
        }
        injectForWeak(beanDefinition, instance, noLoop); // 注入bean
        return afterProcess;
    }

    private void injectForWeak(BeanDefinition beanDefinition, Object instance, Set<BeanDefinition> noLoop) {
        Class<?> cs = beanDefinition.beanClass;
        List<Field> declaredFields = getAllDependenceForField(cs);
        List<Method> declaredMethods = getAllDependenceForMethod(cs);
        Inject annotation;
        List<Object> res = new ArrayList<>(); // 存放临时的参数
        if (noLoop == null)
            noLoop = new HashSet<>();
        BeanDefinition tmp = null; // 暂存临时对象
        for (Field f : declaredFields) { // 寻找需要注入的域
            annotation = f.getAnnotation(Inject.class);
            if (annotation.value() == InjectionType.INJECTBYNAME) { //按名字注入
                String name = firstToLower(f.getName());
                tmp = beanDefinitionMap.get(name);
                if (tmp == null)
                    throw new MFException("undeclared bean named " + name);
            } else { // 按类型注入
                tmp = findBeanDefinitionByType(f.getType());
                if (tmp == null)
                    throw new MFException("too many bean for this type, may set someone be priority");
            }
            f.setAccessible(true);
            try {
                if (isStrongDependence(tmp)) {
                    f.set(instance, initForStrong(tmp, noLoop));// 递归
                } else {
                    if (tmp.scope == BeanScope.PROTOTYPE) {
                        if (tmp.beanClass == beanDefinition.beanClass || noLoop.contains(tmp))
                            throw new MFException("prototype bean loop injection");
                        noLoop.add(tmp); // 防止prototype循环注入
                    }
                    f.set(instance, initForWeak(tmp, noLoop)); // 递归
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
            noLoop.remove(tmp); // 本次完成就可以退出这个了（无论有没有都不影响）每个域之间不影响
        }
        for (Method m : declaredMethods) { // 寻找被标记的要注入的方法
            annotation = m.getAnnotation(Inject.class);
            Parameter[] parameters = m.getParameters();
            for (Parameter p : parameters) {
                if (annotation.value() == InjectionType.INJECTBYNAME) { // 按参数类型名字注入
                    String name = firstToLower(p.getType().getName());
                    tmp = beanDefinitionMap.get(name);
                    if (tmp == null)
                        throw new MFException("undeclared bean named " + name);
                } else {
                    tmp = findBeanDefinitionByType(p.getType());
                    if (tmp == null)
                        throw new MFException("too many bean for this type, may set someone be priority");
                }
                if (isStrongDependence(tmp)) {
                    res.add(initForStrong(tmp, noLoop));
                } else {
                    if (tmp.scope == BeanScope.PROTOTYPE) {
                        if (tmp.beanClass == beanDefinition.beanClass || noLoop.contains(tmp))
                            throw new MFException("prototype bean loop injection");
                        noLoop.add(tmp);
                    }
                    res.add(initForWeak(tmp, noLoop));
                }
                noLoop.remove(tmp); // 每个方法之间不影响
            }
            try {
                m.invoke(instance, res.toArray());
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new MFException("injected mothod not accessible");
            }
            res.clear();
        }
    }

    private BeanDefinition findBeanDefinitionByType(Class<?> declareType) {
        List<BeanDefinition> fit = new ArrayList<>(); // 暂存结果
        List<BeanDefinition> priority = new ArrayList<>(); // 暂存优先标注的bean定义
        beanDefinitionMap.values().forEach(item -> {
            if (item.beanClass == declareType) // 是否就是本类型
                fit.add(item);
            if (declareType.isInterface()) { // 声明的是否是接口
                List<Class<?>> interfaces = getAllInterface(item.beanClass, null);
                for (Class<?> i : interfaces) {
                    if (i == declareType)
                        fit.add(item);
                }
            } else {
                Class<?> superclass = item.beanClass.getSuperclass();
                if (superclass == declareType) // 声明的是否是超类
                    fit.add(item);
            }
        });
        if (fit.size() == 1) // 唯一
            return fit.get(0);
        if (fit.size() == 0) // 没有找到
            throw new MFException("no suitable bean for this type " + declareType);
        fit.forEach(item -> {
            if (item.priority)
                priority.add(item);
        });
        if (priority.size() == 1) // 有唯一优先
            return priority.get(0);
        throw new MFException("too many priority bean for this type " + declareType); // 太多了，不能确定
    }

    private Object applyProcessor(BeanDefinition beanDefinition, Object instance) {
        Object tmp;
        BeanProcessor processor;
        for (BeanDefinition processorBean : processorList) {
            if (processorList.contains(beanDefinition) || allConfiguration.contains(beanDefinition.beanClass))
                continue;  // 处理器不处理处理器 和 配置类
            if (processorOption == BeanProcessor.ProcessorOption.ALLOWTOINJECT &&
                    processorDependenceMap.get(processorBean).contains(beanDefinition))
                continue;  // 不处理依赖器自身依赖的bean
            if (isStrongDependence(processorBean)) { // 获取处理器实例
                processor = (BeanProcessor) initForStrong(processorBean, null);
            } else {
                processor = (BeanProcessor) initForWeak(processorBean, null);
            }
            tmp = processor.operateOnBeanAfterInitialization(instance, beanDefinition.beanName);
            if (tmp != null)
                instance = tmp;
        }
        if (instanceOf(instance, beanDefinition.beanClass))
            return instance;
        throw new MFException("processed bean not fit to declared type");
    }


    private boolean isStrongDependence(BeanDefinition beanDefinition) {
        return beanDefinition.constructor != null || beanDefinition.factoryMethod != null;
    }

    private boolean isProcessor(Class<?> beanClass) {
        return beanClass == BeanProcessor.class ||
                getAllInterface(beanClass, null).stream().anyMatch(item -> item == BeanProcessor.class);
    }

    private void checkProcessorInject(BeanDefinition beanDefinition) {
        if (processorOption == BeanProcessor.ProcessorOption.ALLOWTOINJECT) { // 如果允许注入就找出依赖链
            processorDependenceMap.put(beanDefinition, getAllDependence(beanDefinition, null));
            return;
        }
        if (beanDefinition.constructor != null) {
            if (beanDefinition.constructor.getParameters().length != 0)
                throw new MFException("default protocol : processor can't inject to construct, if needed use" +
                        " allow inject option");
        } else if (beanDefinition.factoryMethod != null) {
            if (beanDefinition.factoryMethod.getParameters().length != 0)
                throw new MFException("default protocol : processor can't inject to factory method," +
                        " if needed use allow inject option");
        } else {
            Class<?> cs = beanDefinition.beanClass;
            Field[] f1 = cs.getDeclaredFields();
            Field[] f2 = cs.getSuperclass().getDeclaredFields(); // 连带父类的域都扫描了
            if (Stream.concat(Arrays.stream(f1), Arrays.stream(f2)).
                    anyMatch(item -> item.isAnnotationPresent(Inject.class)))
                throw new MFException("default protocol : processor can't inject to field," +
                        " if needed use allow inject option");
            Method[] m1 = cs.getDeclaredMethods();
            Method[] m2 = cs.getSuperclass().getDeclaredMethods(); // 连带父类的注入方法都扫描了
            if (Stream.concat(Arrays.stream(m1), Arrays.stream(m2)).
                    anyMatch(item -> item.isAnnotationPresent(Inject.class)))
                throw new MFException("default protocol : processor can't inject to method," +
                        " if needed use allow inject option");
        }
    }

    private Set<BeanDefinition> getAllDependence(BeanDefinition beanDefinition, Set<BeanDefinition> set) {
        if (set == null) // 初始化暂存依赖链集
            set = new HashSet<>();
        BeanDefinition tmp;
        Inject annotation;
        if (beanDefinition.factoryMethod != null || beanDefinition.constructor != null) {
            Parameter[] parameters;
            if (beanDefinition.factoryMethod != null) { // 工厂方法注入
                annotation = beanDefinition.factoryMethod.getAnnotation(Inject.class);
                parameters = beanDefinition.factoryMethod.getParameters();
            } else { // 构造器注入
                annotation = beanDefinition.constructor.getAnnotation(Inject.class);
                parameters = beanDefinition.constructor.getParameters();
            }
            for (Parameter p : parameters) {  // 对参数递归注入分析
                if (annotation == null || annotation.value() == InjectionType.INJECTBYNAME) {
                    tmp = beanDefinitionMap.get(firstToLower(p.getType().getName()));
                    if (tmp == null)
                        throw new MFException("undeclared bean" + firstToLower(p.getType().getName()));
                } else {
                    tmp = findBeanDefinitionByType(p.getType());
                }
                if (tmp == beanDefinition) // 自己依赖自己不再去寻找
                    continue;
                if (!set.contains(tmp)) // 不包含就继续去找
                    getAllDependence(tmp, set);
                set.add(tmp); // 添加自己
            }
        } else { // 域和方法注入
            List<Method> allDependenceForMethod = getAllDependenceForMethod(beanDefinition.beanClass);
            List<Field> allDependenceForField = getAllDependenceForField(beanDefinition.beanClass);
            for (Field f : allDependenceForField) { // 域注入
                annotation = f.getAnnotation(Inject.class);
                if (annotation.value() == InjectionType.INJECTBYNAME) {
                    tmp = beanDefinitionMap.get(firstToLower(f.getName()));
                    if (tmp == null)
                        throw new MFException("undeclared bean " + firstToLower(f.getName()));
                } else {
                    tmp = findBeanDefinitionByType(f.getType());
                }
                if (tmp == beanDefinition) // 自己依赖自己不再去寻找
                    continue;
                if (!set.contains(tmp)) // 不包含就继续去找
                    getAllDependence(tmp, set);
                set.add(tmp); // 添加自己
            }
            Parameter[] parameters;
            for (Method m : allDependenceForMethod) {
                parameters = m.getParameters();
                annotation = m.getAnnotation(Inject.class);
                for (Parameter p : parameters) {
                    if (annotation.value() == InjectionType.INJECTBYNAME) {
                        tmp = beanDefinitionMap.get(firstToLower(p.getType().getName()));
                        if (tmp == null)
                            throw new MFException("undeclared bean" + firstToLower(p.getType().getName()));
                    } else
                        tmp = findBeanDefinitionByType(p.getType());
                    if (tmp == beanDefinition) // 自己依赖自己不再去寻找
                        continue;
                    if (!set.contains(tmp)) // 不包含就继续去找
                        getAllDependence(tmp, set);
                    set.add(tmp); // 添加自己
                }
            }
        }
        return set;
    }

    private List<Field> getAllDependenceForField(Class<?> cs) {
        // 找出自己和父类的注入定义（依赖）域
        Field[] f1 = cs.getDeclaredFields();
        Field[] f2 = cs.getSuperclass().getDeclaredFields(); // 连带父类的域都扫描了
        return Stream.concat(Arrays.stream(f1), Arrays.stream(f2)).
                filter(item -> item.isAnnotationPresent(Inject.class)).collect(Collectors.toList()); // 过滤
    }

    private List<Method> getAllDependenceForMethod(Class<?> cs) {
        // 找出自己和父类的注入定义（依赖）方法
        Method[] m1 = cs.getDeclaredMethods();
        Method[] m2 = cs.getSuperclass().getDeclaredMethods(); // 连带父类的注入方法都扫描了
        return Stream.concat(Arrays.stream(m1), Arrays.stream(m2)).
                filter(item -> item.isAnnotationPresent(Inject.class)).collect(Collectors.toList());
    }

    private boolean instanceOf(Object o, Class<?> target) {
        // 判断是否是某个类的实例或者是某个类接口的实例 或者两个对象具有相同的接口（为了兼容aop）
        Class<?> aClass = o.getClass();
        if (aClass == target)
            return true;
        if (aClass.getSuperclass() == target)
            return true;
        List<Class<?>> aInterface = getAllInterface(aClass, null);
        if (aInterface.contains(target))
            return true;
        List<Class<?>> targetInterface = getAllInterface(target,null);
        return aInterface.stream().anyMatch(targetInterface::contains);
    }

    List<Class<?>> getAllInterface(Class<?> beanClass, List<Class<?>> list) {
        // 递归找出所有的接口
        if (list == null)
            list = new ArrayList<>();
        Class<?>[] interfaces = beanClass.getInterfaces();
        for (Class<?> c : interfaces) {
            list.add(c);
            getAllInterface(c, list);
        }
        return list;
    }

    String firstToLower(String s) {
        // 名字处理函数
        s = s.substring(s.lastIndexOf(".") + 1); // 如果有.就只要最后一个，没有就是全部串
        char[] chars = s.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private void getResolver() {
        // 获取解析器，传入的是主配置类，主配置类和配置类要确保囊括所有待扫描路径
        resourceResolver = new ResourceResolver(mainConfigClass);
    }

    private void getClassLoader() {
        // 获取类加载器，优先时用上下文加载器
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) // 没有就是用当前类加载器
            classLoader = getClass().getClassLoader();
    }

    private Set<Class<?>> getAllConfiguration(Class<?> mainConfig) {
        // 扫描获取所有的配置类
        Set<Class<?>> res = new HashSet<>();
        res.add(mainConfig);
        while (true) {
            Import annotation = mainConfig.getAnnotation(Import.class);
            if (annotation == null || annotation.value().equals("")) // 错误配置
                return res;
            try {
                Class<?> config = classLoader.loadClass(annotation.value());
                if (res.contains(config) || !config.isAnnotationPresent(MFConfig.class)
                        || config.isInterface() || config.isAnnotation()) // 防止循环Import和引用非配置类
                    return res;
                res.add(config);
                mainConfig = config;
            } catch (ClassNotFoundException e) { // 无法加载
                throw new MFException("Config loading Error");
            }
        }
    }

    private Annotation isAnnotationOn(Class<? extends Annotation> target, Class<?> one, Set<Class<?>>
            tmpSet) { // one可是普通对象也可是注解,第一次传入的必须的是普通类 递归扫描成功返回找到的注解，不成功返回null
        Annotation[] annotations = one.getAnnotations();
        Class<? extends Annotation> type;
        for (Annotation a :
                annotations) {
            type = a.annotationType();
            if (type.getPackage().getName().startsWith("java.lang"))
                continue;
            if (type == target) // 找到
                return a;
            if (tmpSet == null)
                tmpSet = new HashSet<>();
            if (tmpSet.contains(type)) // 有人找了，不要再去找了，防止循环注解
                continue;
            tmpSet.add(a.annotationType());
            return isAnnotationOn(target, a.annotationType(), tmpSet);
        }
        return null;
    }

    Map<String, BeanDefinition> getBeanDefinitionMap(){
        // internally use only
        return beanDefinitionMap;
    }

}
