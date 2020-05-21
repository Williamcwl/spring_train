package com.lagou.edu.factory;

import com.lagou.edu.annotation.Autowired;
import com.lagou.edu.annotation.Service;
import com.lagou.edu.annotation.Transactional;
import com.lagou.edu.pojo.Account;
import com.lagou.edu.service.TransferService;
import com.lagou.edu.utils.TransactionManager;
import com.sun.jdi.BooleanType;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * @author 应癫
 *
 *
 * 代理对象工厂：生成代理对象的
 */
@Service("proxyFactory")
public class ProxyFactory {

    @Autowired
    private TransactionManager transactionManager;

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    /*private ProxyFactory(){

    }

    private static ProxyFactory proxyFactory = new ProxyFactory();

    public static ProxyFactory getInstance() {
        return proxyFactory;
    }*/



    /**
     * Jdk动态代理
     * @param obj  委托对象
     * @return   代理对象
     */
    public Object getJdkProxy(Object obj) {

        // 获取代理对象
        return  Proxy.newProxyInstance(obj.getClass().getClassLoader(), obj.getClass().getInterfaces(),
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Object result = null;

                        try{
                            // 开启事务(关闭事务的自动提交)
                            transactionManager.beginTransaction();

                            result = method.invoke(obj,args);

                            // 提交事务

                            transactionManager.commit();
                        }catch (Exception e) {
                            e.printStackTrace();
                            // 回滚事务
                            transactionManager.rollback();

                            // 抛出异常便于上层servlet捕获
                            throw e;

                        }

                        return result;
                    }
                });

    }


    /**
     * 使用cglib动态代理生成代理对象
     * @param obj 委托对象
     * @return
     */
    public Object getCglibProxy(Object obj) {
        return  Enhancer.create(obj.getClass(), new MethodInterceptor() {
            @Override
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                Object result = null;
                Boolean flag = Boolean.FALSE;
                try{

                    // 开启事务(关闭事务的自动提交)
                    Annotation[] annotations = method.getAnnotations();
                    if(annotations.length!=0) {
                        for (Annotation annotation : annotations) {
                            if (annotation instanceof Transactional) {
                                System.out.println("开启事务");
                                transactionManager.beginTransaction();
                                flag = Boolean.TRUE;
                            }
                        }
                    }

                    result = method.invoke(obj,objects);

                    if(flag){
                        // 提交事务
                        System.out.println("提交事务");
                        transactionManager.commit();
                    }

                }catch (Exception e) {
                    e.printStackTrace();
                    if(flag){
                        // 回滚事务
                        transactionManager.rollback();
                        System.out.println("回滚事务");
                    }
                    // 抛出异常便于上层servlet捕获
                    throw e;

                }
                return result;
            }
        });
    }

    public static void main(String[] args) {
        ProxyFactory proxyFactory = new ProxyFactory();
        TransferService transferService = (TransferService) BeanFactory.getBean("transferService");
        transferService = (TransferService) proxyFactory.getJdkProxy(transferService);
        System.out.println(transferService);
    }
}
