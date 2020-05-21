package com.lagou.edu.factory;

import com.lagou.edu.annotation.Autowired;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.annotation.Transactional;
import com.lagou.edu.service.TransferService;
import com.lagou.edu.utils.ClassUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

/**
 * @author 应癫
 *
 * 工厂类，生产对象（使用反射技术）
 */
public class BeanFactory {

    /**
     * 任务一：读取解析xml，通过反射技术实例化对象并且存储待用（map集合）
     * 任务二：对外提供获取实例对象的接口（根据id获取）
     */

    private static Map<String,Object> map = new HashMap<>();  // 按名字存储对象

//    private static Map<Class<?>,Object> typeMap = new HashMap<>(); //按类型存储对象


    static {
        Set<Class<?>> serviceClass = new LinkedHashSet<Class<?>>();
        //1、获取包下所有的类
        Set<Class<?>> allClass = ClassUtils.getClasses("com.lagou.edu");
        //2、获取所有含有Service注解的类并注入到map
        Iterator<Class<?>> it = allClass.iterator();
        while(it.hasNext()){
            Class<?> aClass = it.next();
            Annotation[] annotations = aClass.getAnnotations();
            if(annotations.length != 0){
                for(Annotation annotation : annotations){
                    if(annotation instanceof Service){
                        serviceClass.add(aClass);
                        try {
                            Object object = aClass.newInstance();
                            if("".equals(((Service) annotation).value())){
                                map.put(aClass.getName(),object);
                            }else{
                                map.put(((Service) annotation).value(),object);
                            }
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        break;
                    }
                }
            }
        }

        //3、处理Service注解的类中Autowired注解
        if(!map.isEmpty()){
            Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<String, Object> next = iterator.next();
                dealBeanWithAutowired(next.getValue());
            }
        }

        //4、处理Service注解的类中Transactional注解
        if(!map.isEmpty()){
            Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
            while(iterator.hasNext()){
                Map.Entry<String, Object> next = iterator.next();
                dealBeanWithTransactional(next);
            }
        }


    }

    private static void dealBeanWithTransactional(Map.Entry<String, Object> entry){
        Object object = entry.getValue();
        Method[] declaredMethods = object.getClass().getMethods();
        for( Method method :declaredMethods){
            Annotation[] annotations = method.getAnnotations();
            if(annotations.length!=0){
                for(Annotation annotation : annotations) {
                    if (annotation instanceof Transactional) {
                        ProxyFactory proxyFactory = (ProxyFactory) getBean("proxyFactory");
                        map.put(entry.getKey(), proxyFactory.getCglibProxy(object));
                        return;
                    }
                }
            }
        }
    }


    private static void dealBeanWithAutowired(Object object){
        Field[] fields = object.getClass().getDeclaredFields();
        for( Field field :fields){
            Annotation[] annotations = field.getAnnotations();
            if(annotations.length!=0){
                for(Annotation annotation : annotations) {
                    if (annotation instanceof Autowired) {
                        field.setAccessible(true);
                        try {
                            Object tempObj = getBeanByClass(field.getType());
                            if(tempObj != null){
                                dealBeanWithAutowired(tempObj);
                                field.set(object,tempObj);
                            }else{
                                throw new RuntimeException(field.getType()+"无注入");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public static Object getBeanByClass(Class<?> aClass){
        Iterator<Map.Entry<String, Object>> iterator = map.entrySet().iterator();
        while(iterator.hasNext()){
            Map.Entry<String, Object> next = iterator.next();
            if(next.getValue().getClass().equals(aClass) ){
                return next.getValue();
            }else{
                Class<?>[] interfaces = next.getValue().getClass().getInterfaces();
                for(Class a :interfaces){
                   if( a.equals(aClass)){
                       return next.getValue();
                   }
                }
            }
        }
        return null;
    }


    // 任务二：对外提供获取实例对象的接口（根据id获取）
    public static  Object getBean(String id) {
        return map.get(id);
    }

    public static void main(String[] args) {
        TransferService transferService = (TransferService)BeanFactory.getBean("transferService") ;
        System.out.println(transferService);
    }
}
