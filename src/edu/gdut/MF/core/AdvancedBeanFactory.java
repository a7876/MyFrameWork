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
    private ClassLoader classLoader;

    private ResourceResolver resourceResolver;

    private final Map<String, BeanDefinition> beanDefinitionMap = new HashMap<>();

    private final Class<?> mainConfigClass;

    private List<BeanDefinition> processorList = new ArrayList<>();

    public AdvancedBeanFactory(Class<?> config) {
        if (!config.isAnnotationPresent(MFConfig.class) || config.isInterface() || config.isAnnotation())
            throw new MFException(config.getName() + " is not a config class");
        this.mainConfigClass = config;
        getClassLoader();
        getResolver();
        prepare();
        initial();
    }

    private void prepare() { // 找出所有的bean的定义并且收集完所需要的信息

        // 找出所有的配置类
        Set<Class<?>> allConfiguration = getAllConfiguration(mainConfigClass);
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
            Order annotation = item.beanClass.getAnnotation(Order.class);
            if (annotation == null)
                item.order = 0; // 默认是0
            else
                item.order = annotation.value();
        }).sorted((a,b) -> b.order - a.order).collect(Collectors.toList());
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
            return beanDefinition.beanInstance;
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
                return (T) beanDefinition.beanInstance;
        } catch (MFException | NullPointerException e) {
            return null;
        }
        if (isStrongDependence(beanDefinition))
            return (T) initForStrong(beanDefinition, null);
        else
            return (T) initForWeak(beanDefinition, null);
    }


    private Object initForStrong(BeanDefinition beanDefinition, Set<BeanDefinition> noLoop) {
        if (beanDefinition.beanInstance != null) // 不为空直接返回
            return beanDefinition.beanInstance;
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
        if (beanDefinition.scope == BeanScope.SINGLETON) // 如果是单例就记录该对象
            beanDefinition.beanInstance = instance;
        return instance;
    }

    private Object initForWeak(BeanDefinition beanDefinition, Set<BeanDefinition> noLoop) {
        if (beanDefinition.beanInstance != null) // 不为空直接返回
            return beanDefinition.beanInstance;
        Object instance;
        try {
            Constructor<?> constructor = beanDefinition.beanClass.getConstructor();
            constructor.setAccessible(true);
            instance = constructor.newInstance();
        } catch (InstantiationException | IllegalAccessException
                 | InvocationTargetException | NoSuchMethodException e) {
            throw new MFException("no param construct failed in initialization, " +
                    "please make sure there's a no param constructor");
        }
        if (beanDefinition.scope == BeanScope.SINGLETON) // Singleton保存单例, 一定要先保存再去注入，不然单例会被
            beanDefinition.beanInstance = instance;
        injectForWeak(beanDefinition, instance, noLoop); // 注入bean
        return instance;
    }

    private void injectForWeak(BeanDefinition beanDefinition, Object instance, Set<BeanDefinition> noLoop) {
        Class<?> cs = beanDefinition.beanClass;
        List<Field> declaredFields;
        List<Method> declaredMethods;
        Field[] f1 = cs.getDeclaredFields();
        Field[] f2 = cs.getSuperclass().getDeclaredFields(); // 连带父类的域都扫描了
        declaredFields = Stream.concat(Arrays.stream(f1), Arrays.stream(f2)).collect(Collectors.toList());
        Method[] m1 = cs.getDeclaredMethods();
        Method[] m2 = cs.getSuperclass().getDeclaredMethods(); // 连带父类的注入方法都扫描了
        declaredMethods = Stream.concat(Arrays.stream(m1), Arrays.stream(m2)).collect(Collectors.toList());
        Inject annotation;
        List<Object> res = new ArrayList<>(); // 存放临时的参数
        if (noLoop == null)
            noLoop = new HashSet<>();
        BeanDefinition tmp = null; // 暂存临时对象
        for (Field f : declaredFields) { // 寻找需要注入的域
            annotation = f.getAnnotation(Inject.class);
            if (annotation != null) {
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
            }
            noLoop.remove(tmp); // 本次完成就可以退出这个了（无论有没有都不影响）每个域之间不影响
        }
        for (Method m : declaredMethods) { // 寻找被标记的要注入的方法
            annotation = m.getAnnotation(Inject.class);
            if (annotation != null) {
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

    private boolean isStrongDependence(BeanDefinition beanDefinition) {
        return beanDefinition.constructor != null || beanDefinition.factoryMethod != null;
    }

    private boolean isProcessor(Class<?> beanClass) {
        List<Class<?>> allInterface = getAllInterface(beanClass, null);
        return allInterface.stream().anyMatch(item -> item == BeanProcessor.class);
    }

    private List<Class<?>> getAllInterface(Class<?> beanClass, List<Class<?>> list) {
        if (list == null)
            list = new ArrayList<>();
        Class<?>[] interfaces = beanClass.getInterfaces();
        for (Class<?> c : interfaces) {
            list.add(c);
            getAllInterface(c, list);
        }
        return list;
    }

    private String firstToLower(String s) {
        if (s.contains("."))
            s = s.substring(s.lastIndexOf(".") + 1);
        char[] chars = s.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private void getResolver() {
        resourceResolver = new ResourceResolver(mainConfigClass);
    }

    private void getClassLoader() {
        classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null)
            classLoader = getClass().getClassLoader();
    }

    private Set<Class<?>> getAllConfiguration(Class<?> mainConfig) {
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
            tmpSet) { // one可是普通对象也可是注解,第一次传入的必须的是普通类 成功返回找到的注解，不成功返回null
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

}
