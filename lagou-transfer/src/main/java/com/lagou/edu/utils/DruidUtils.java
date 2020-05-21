package com.lagou.edu.utils;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * @author 应癫
 */
public class DruidUtils {

    private DruidUtils(){
    }

    private static DruidDataSource druidDataSource = new DruidDataSource();


    static {
        druidDataSource.setDriverClassName("com.mysql.jdbc.Driver");
        druidDataSource.setUrl("jdbc:mysql://118.31.103.91:3306/lg?characterEncoding=UTF-8");
        druidDataSource.setUsername("test");
        druidDataSource.setPassword("12345678");

    }

    public static DruidDataSource getInstance() {
        return druidDataSource;
    }

}
