package edu.gdut.MF.core;

import edu.gdut.MF.Enum.EnhanceType;
import edu.gdut.MF.Enum.InjectionType;
import edu.gdut.MF.annotation.*;
import edu.gdut.MF.exception.MFException;

import java.lang.reflect.*;
import java.util.*;
import java.util.stream.Collectors;

@Bean
@Order
class BeanAopProcessor implements BeanProcessor {
    // 核心AOP支持类
    private final List<Method> enhancerMethodList; // 记录所有的增强方法

    private List<BeanDefinition> enhanceBeanList = new ArrayList<>();  // 存储所有的增强定义bean并进行初始化

    private final Map<Method, Object> whoseMethod = new HashMap<>();  // 记录每个增强方法是来自哪个增强bean

    private final AdvancedBeanFactory advancedBeanFactory; // 当前的BeanFactory

    BeanAopProcessor(AdvancedBeanFactory advancedBeanFactory) {
        this.advancedBeanFactory = advancedBeanFactory;
        Map<String, BeanDefinition> beanDefinitionMap = advancedBeanFactory.getBeanDefinitionMap();
        enhancerMethodList = enhanceScan(beanDefinitionMap); // 扫描构建本处理器
    }

    private List<Method> enhanceScan(final Map<String, BeanDefinition> beanDefinitionMap) {
        enhanceBeanList = beanDefinitionMap.values().stream() // 找出增强bean
                .filter(item -> item.beanClass.isAnnotationPresent(Enhancer.class)).collect(Collectors.toList());
        initEnhanceBean(); // 初始化增强bean（不再由ioc容器负责初始化）
        // 找出所有的增强bean
        return enhanceBeanList.stream().flatMap((item -> {
                    Method[] declaredMethods = item.beanClass.getDeclaredMethods();
                    for (Method m : declaredMethods) {
                        whoseMethod.put(m, item.processedInstance); // 记录每个增强方法的bean对象
                    }
                    return Arrays.stream(declaredMethods)
                            .filter(method -> method.isAnnotationPresent(EnhanceDefinition.class));
                }))
                .collect(Collectors.toList()); // 找出所有的增强方法
    }


    @Override
    public Object operateOnBeanAfterInitialization(Object bean, String beanName) { // 处理器处理目标bean
        Set<Class<?>> needToImplement = new HashSet<>(); // 记录需要代理实现的接口
        Set<MethodDescriptor> targetMethods = new HashSet<>();  // 记录被增强的目标方法（使用方法描述类标记）
        Map<MethodDescriptor, List<MethodAndOwner>> enhanceMethodForTarget = new HashMap<>();
        // 记录每个目标方法的增强方法及其增强bean的信息

        this.enhancerMethodList.stream() // 记录以上的信息
                .filter(item -> item.getAnnotation(EnhanceDefinition.class).targetName().equals(beanName))
                .forEach(item -> {  // 找出所有的增强信息以构建代理类
                    EnhanceDefinition annotation = item.getAnnotation(EnhanceDefinition.class);
                    Class<?> aClass;
                    Method targetMethod;
                    try { // 找出继承自哪个接口
                        targetMethod = bean.getClass().getMethod(annotation.targetMethod(), annotation.parameters());
                        aClass = methodFrom(annotation.targetMethod(), annotation.parameters(), bean.getClass());
                    } catch (NoSuchMethodException e) {
                        throw new MFException("can't find the enhance target method");
                    }
                    if (aClass == null)
                        throw new MFException("target enhance method not come from any interface");
                    List<MethodAndOwner> tmp;
                    MethodDescriptor descriptor = new MethodDescriptor(targetMethod);
                    if ((tmp = enhanceMethodForTarget.get(descriptor)) == null) {
                        tmp = new ArrayList<>();
                        enhanceMethodForTarget.put(descriptor, tmp);
                    }
                    tmp.add(new MethodAndOwner(item, whoseMethod.get(item))); // 记录目标方法和增强方法
                    needToImplement.add(aClass); // 记录需要实现的接口
                    targetMethods.add(descriptor); // 记录目标方法
                });
        if (needToImplement.size() != 0) {
            // 可以被增强，返回代理类
            return Proxy.newProxyInstance(advancedBeanFactory.classLoader,
                    needToImplement.toArray(new Class<?>[0]),
                    new InnerHandler(enhanceMethodForTarget, bean, targetMethods));
        }
        return null;
    }

    public void initEnhanceBean() {
        // 代替IOC容器初始化增强类
        for (BeanDefinition b : enhanceBeanList) {
            try {
                Object o = b.beanClass.newInstance();
                b.beanInstance = o;
                b.processedInstance = o;
            } catch (InstantiationException | IllegalAccessException e) {
                throw new MFException("there is no none parameter construct or not accessible for " +
                        "enhancer bean named " + b.beanName);
            }
        }
    }

    private Class<?> methodFrom(String methodName, Class<?>[] parameters, Class<?> target) {
        // 找出方法来自哪个接口
        List<Class<?>> allInterface = advancedBeanFactory.getAllInterface(target, null);
        Method method = null;
        for (Class<?> c : allInterface) {
            try {
                method = c.getDeclaredMethod(methodName, parameters);
            } catch (NoSuchMethodException ignored) {
            }
            if (method != null)
                return c;
        }
        return null;
    }

    class InnerHandler implements InvocationHandler {
        // 增强构建类
        private final Map<MethodDescriptor, List<MethodAndOwner>> enhancedMethod; // 目标方法和增强方法的信息
        private final Set<MethodDescriptor> targetMethods; // 目标方法的信息
        private final Object source; // 被代理的对象
        private final Map<Method, MethodDescriptor> methodDescriptorCache = new HashMap<>(); // 缓存方法定义信息以免每次新建

        public InnerHandler(Map<MethodDescriptor, List<MethodAndOwner>> enhancedMethod,
                            Object source, Set<MethodDescriptor> targetMethods) {
            this.enhancedMethod = enhancedMethod;
            this.targetMethods = targetMethods;
            this.source = source;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object o = null;
            boolean needToInvoke = true;
            MethodDescriptor descriptor;
            List<MethodAndOwner> list;
            if ((descriptor = isThisMethod(method)) != null) { // 判断当前被执行的方法是否需要增强
                list = enhancedMethod.get(descriptor);
                for (MethodAndOwner ma : list) { // before invoke
                    if (ma.method.getAnnotation(EnhanceDefinition.class).type() == EnhanceType.BEFORE_METHOD_INVOKE)
                        methodInvoke(ma, method, source, args);  // 执行增强方法
                }
                for (MethodAndOwner ma : list) { // around invoke
                    if (ma.method.getAnnotation(EnhanceDefinition.class).type() == EnhanceType.AROUND_METHOD_INVOKE) {
                        o = methodInvoke(ma, method, source, args);
                        needToInvoke = false; // 如果有环绕method就设为false
                    }
                }
                if (needToInvoke)
                    o = method.invoke(source, args);
                for (MethodAndOwner ma : list) { // after invoke
                    if (ma.method.getAnnotation(EnhanceDefinition.class).type() == EnhanceType.AFTER_METHOD_INVOKE)
                        methodInvoke(ma, method, source, args);
                }
            } else {
                o = method.invoke(source, args); // 正常执行
            }
            return o;
        }

        private MethodDescriptor isThisMethod(Method method) {
            // 判断是否是被增强的方法并返回方法描述
            MethodDescriptor descriptor = methodDescriptorCache.get(method);
            if (descriptor == null) {
                descriptor = new MethodDescriptor(method);
                methodDescriptorCache.put(method, descriptor);
            }
            if (targetMethods.contains(descriptor))
                return descriptor;
            return null;
        }

        public Object methodInvoke(MethodAndOwner ma, Method originMethod, Object originBean, Object[] originParam) {
            // 执行增强方法
            // Method originMethod 目标方法, Object originBean 被代理对象, Object[] originParam 目标方法的执行参数
            Parameter[] parameters = ma.method.getParameters();
            Object[] objects = new Object[parameters.length];
            Inject annotation = ma.method.getAnnotation(Inject.class);
            if (annotation == null || annotation.value() == InjectionType.INJECT_BY_NAME) {
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].getType() == WeaveEnv.class) // 织入点参数注入
                        objects[i] = new WeaveEnv(originMethod, originParam, originBean);
                    else
                        objects[i] = advancedBeanFactory.getBean(advancedBeanFactory
                                .firstToLower(parameters[i].getType().getName()));
                    if (objects[i] == null)
                        throw new MFException("bean for enhanced method not found!");
                }
            } else {
                for (int i = 0; i < parameters.length; i++) {
                    if (parameters[i].getType() == WeaveEnv.class) // 织入点参数注入
                        objects[i] = new WeaveEnv(originMethod, originParam, originBean);
                    else
                        objects[i] = advancedBeanFactory.getBean(parameters[i].getType());
                    if (objects[i] == null)
                        throw new MFException("bean for enhanced method not found!");
                }
            }
            try {
                return ma.method.invoke(ma.owner, objects); // 执行增强方法
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new MFException("enhancer method invoke failed, is it accessible ?");
            }
        }
    }

    class MethodAndOwner {
        // 记录增强方法和它对应的增强bean
        Method method;
        Object owner;

        public MethodAndOwner(Method method, Object owner) {
            this.method = method;
            this.owner = owner;
        }
    }

    class MethodDescriptor {
        // 方法描述类
        private final String methodName;
        private final Class<?>[] paramArray;

        public MethodDescriptor(Method method) {
            this.methodName = method.getName();
            this.paramArray = method.getParameterTypes();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            MethodDescriptor that = (MethodDescriptor) o;
            return methodName.equals(that.methodName) && Arrays.equals(paramArray, that.paramArray);
        }

        @Override
        public int hashCode() {
            int result = Objects.hash(methodName);
            result = 31 * result + Arrays.hashCode(paramArray);
            return result;
        }
    }
}