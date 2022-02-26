package com.hope.service;

import com.spring.BeanPostProcessor;
import com.spring.Component;

@Component
public class HopeBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println("初始化前");
        if (beanName.equals("userService")){
            ((UserService) bean).setName("自定义属性名称");
        }
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println("初始化后");
        return bean;
    }
}
