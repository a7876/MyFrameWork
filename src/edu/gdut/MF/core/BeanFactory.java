package edu.gdut.MF.core;

import edu.gdut.MF.annotation.Bean;
import edu.gdut.MF.annotation.Inject;
import edu.gdut.MF.exception.MFException;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class BeanFactory {
    private final List<String> classPath;
    private final Class<?> configClass;
    private final List<Class<?>> beanClassList = new ArrayList<>();
    private final List<AfterInitialProcessor> processorList = new ArrayList<>();
    private final HashMap<Class<?>, Object> beanSet = new HashMap<>();
    private final HashMap<String, Object> beanNameSet = new HashMap<>();

    public BeanFactory(List<String> classPath, Class<?> configClass) {
        this.classPath = classPath;
        this.configClass = configClass;
        init();
        build();
    }

    public void init() {

        classPath.forEach(item -> {
            try {
                Class<?> tmp;
                item = item.replace(File.separator, ".").replace(".class", "");
                tmp = Class.forName(item);
                if (checkAnnotation(tmp)) {
                    beanClassList.add(tmp);
                }
            } catch (ClassNotFoundException e) {
                throw new MFException("cant load class called " + item, e);
            }
        });
    }

    private boolean checkAnnotation(Class<?> target) {
        return target.isAnnotationPresent(Bean.class);
    }

    private void build() {
        // 为所有对象创建bean, 按照类型注入
        // 全部构建
        beanClassList.forEach(item -> {
            try {
                Object tmp = item.newInstance();
                beanSet.put(item, tmp);
                beanNameSet.put(getBeanNameLowCase(item), tmp);
                if (getProcessor(item))
                    processorList.add((AfterInitialProcessor) tmp);
            } catch (InstantiationException | IllegalAccessException e) {
                throw new MFException("construct failed is there none param constructor");
            }
        });
        // 属性注入
        beanSet.forEach((K, V) -> {
            Field[] fields = K.getDeclaredFields();
            Object tmp;
            Class<?> need;
            for (Field f : fields) {
                if (f.isAnnotationPresent(Inject.class)) {
                    need = f.getType();
//                    if (need == K)
//                        throw new MFException("loop injection rejected!");
                    tmp = beanSet.get(need);
                    if (tmp == null)
                        throw new MFException("cant inject " + K);
                    f.setAccessible(true);
                    try {
                        f.set(V, tmp);
                    } catch (IllegalAccessException ignored) {
                    }
                }
            }
            processorList.forEach(item -> {  // 执行自定义处理器
                item.operateOnObject(V, K);
            });
        });
        buildByMethod(); // 通过方法注入
    }

    private void buildByMethod() {
        Method[] methods = configClass.getMethods();
        Object target;
        try {
            target = configClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new MFException("Config class illegally defined!");
        }
        Class<?> tmp;
        for (Method m :
                methods) {
            if (!m.isAnnotationPresent(Bean.class))
                break;
            tmp = m.getReturnType();
            Object o;
            if (beanSet.containsKey(tmp))
                break;
            try {
                o = m.invoke(target);
                beanSet.put(tmp, o);
                beanNameSet.put(getBeanNameLowCase(tmp), o);
                if (!m.getName().equals(getBeanNameLowCase(tmp)))
                    beanNameSet.put(m.getName(), o);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new MFException("bean definition method illegal");
            }
        }
    }

    private String getBeanNameLowCase(Class<?> type) {
        String beanName = type.getName().substring(type.getName().lastIndexOf(".") + 1);
        char[] chars = beanName.toCharArray();
        chars[0] = Character.toLowerCase(chars[0]);
        return new String(chars);
    }

    private boolean getProcessor(Class<?> type) {
        Class<?>[] interfaces = type.getInterfaces();
        for (Class<?> c :
                interfaces) {
            if (c == AfterInitialProcessor.class) {
                return true;
            }
        }
        return false;
    }

    public Object get(Class<?> type) {
        return beanSet.get(type);
    }

    public Object get(String beanName) {
        return beanNameSet.get(beanName);
    }
}
