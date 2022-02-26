package com.spring;

import java.io.File;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class HopeApplicationContext {

    private Class configClass;
    // 模拟单例池
    private ConcurrentHashMap<String,Object> singletonObjects = new ConcurrentHashMap<>();
    // beanDefinition map
    private ConcurrentHashMap<String,BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();

    private List<BeanPostProcessor> beanPostProcessorList = new ArrayList<>();

    public HopeApplicationContext(Class configClass) throws NoSuchMethodException {
        this.configClass = configClass;
        // 解析配置类
        // ComponentScan注解--> 扫描路径 --> 扫描 --> BeanDefinition -> BeanDefinitionMap;
        scan(configClass);
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")){
                Object bean = createBean(beanName,beanDefinition); //  单例bean
                singletonObjects.put(beanName,bean);
            }
        }

    }

    public Object createBean(String beanName,BeanDefinition beanDefinition){
        Class clazz = beanDefinition.getClazz();
        try {
            Object instance = clazz.getDeclaredConstructor().newInstance();
            // 依赖注入
            for (Field declaredFiled : clazz.getDeclaredFields()) {
                if (declaredFiled.isAnnotationPresent(Autowired.class)){
                    Object bean = getBean(declaredFiled.getName());
//                    if (bean == null){
//                        // 抛出异常
//                    }
                    declaredFiled.setAccessible(true);
                    declaredFiled.set(instance ,bean);
                }
            }
            // Aware 回调
            if (instance instanceof BeanNameAware){
                ((BeanNameAware)instance).setBeanName(beanName);
            }
            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessBeforeInitialization(instance,beanName);
            }
            // 初始化
            if (instance instanceof InitializingBean){
                try {
                    ((InitializingBean)instance).afterPropertiesSet();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            for (BeanPostProcessor beanPostProcessor : beanPostProcessorList) {
                instance = beanPostProcessor.postProcessAfterInitialization(instance,beanName);
            }
            return instance;
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void scan(Class configClass) throws NoSuchMethodException {
        ComponentScan componentScan =  (ComponentScan) configClass.getDeclaredAnnotation(ComponentScan.class);
        // 扫描路径
        String path = componentScan.value();
        path = path.replace(".","/");
        // 扫描  根据类加载器  Bootstrap   ---> jre/lib   Etx --> jre/ext/lib   app ---> classpath
        ClassLoader classLoader = HopeApplicationContext.class.getClassLoader();  // app类加载器
        URL resource = classLoader.getResource(path);
        File file  = new File(resource.getFile());
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f: files){
                String fileName = f.getAbsolutePath();
                if (fileName.endsWith(".class")) {
                    String className = fileName.substring(fileName.indexOf("com"), fileName.indexOf(".class"));
                    className = className.replace("\\", ".");
                    try {
                        Class<?> aClass = classLoader.loadClass(className);
                        if (aClass.isAnnotationPresent(Component.class)){
                            //  表示当前类是一个bean
                            // 解析当前bean 是单例bean  还是原型 prototype的bean

                            // 判断当前类是否实现了 BeanPostProcessor 接口
                            if (BeanPostProcessor.class.isAssignableFrom(aClass)) {
                                BeanPostProcessor instance = (BeanPostProcessor) aClass.getDeclaredConstructor().newInstance();
                                beanPostProcessorList.add(instance);
                            }
                            Component componentAnnotation = aClass.getDeclaredAnnotation(Component.class);
                            String beanName = componentAnnotation.value();

                            BeanDefinition beanDefinition = new BeanDefinition();
                            beanDefinition.setClazz(aClass);
                            if (aClass.isAnnotationPresent(Scope.class)) {
                                Scope scopeAnnotation = aClass.getDeclaredAnnotation(Scope.class);
                                beanDefinition.setScope(scopeAnnotation.value());
                            }else {
                                //  默认没有注解是单例的
                                beanDefinition.setScope("singleton");
                            }
                            beanDefinitionMap.put(beanName,beanDefinition);

                        }
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public Object getBean(String beanName){
        if (beanDefinitionMap.keySet().contains(beanName)){
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
            if (beanDefinition.getScope().equals("singleton")){
                return singletonObjects.get(beanName);
            }else {
                //  创建bean对象
                Object bean = createBean(beanName,beanDefinition);
                return bean;
            }
        }else {
            // 不存在该bean
            throw new NullPointerException();
        }
    }
}
