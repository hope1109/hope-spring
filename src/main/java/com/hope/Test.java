package com.hope;

import com.hope.service.UserService;
import com.spring.HopeApplicationContext;

public class Test {
    public static void main(String[] args) throws NoSuchMethodException {
        HopeApplicationContext hopeApplicationContext = new HopeApplicationContext(AppConfig.class);
        UserService userService = (UserService) hopeApplicationContext.getBean("userService");
        userService.test();


    }
}
