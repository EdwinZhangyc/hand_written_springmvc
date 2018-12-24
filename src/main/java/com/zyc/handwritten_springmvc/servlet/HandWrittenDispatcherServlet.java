package com.zyc.handwritten_springmvc.servlet;


import com.zyc.handwritten_springmvc.annotation.HandWrittenAutowired;
import com.zyc.handwritten_springmvc.annotation.HandWrittenController;
import com.zyc.handwritten_springmvc.annotation.HandWrittenRequestMapping;
import com.zyc.handwritten_springmvc.annotation.HandWrittenService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @author zyc
 * @date 2018/12/21
 * @description  手写 servlet
 */
public class HandWrittenDispatcherServlet extends HttpServlet {

    // 跟 web.xml 中 parame-name 的值一致
    private static final String LOCATION = "contextConfigLocation";

    // 保存所有信息
    private Properties properties = new Properties();

    // 保存所有被扫描到的相关的类名
    private List<String> classNames = new ArrayList<String>();

    // 核心 ioc 容器，保存所有初始化的 bean
    private Map<String, Object> ioc = new HashMap<String, Object>();

    // 保存所有的 Url 和方法的映射关系
    private Map<String, Method> handlerMapping = new HashMap<String, Method>();



    public HandWrittenDispatcherServlet () {
        super();
    }

    /**
     * 当 servlet 容器启动时，会调用 HandWrittenDispatcherServlet 的 init() 方法，
     * 从 init method's parameter 中，可以拿到主配置文件的路径，从而能够读取到配置文件中的信息。
     * @throws ServletException
     */
    @Override
    public void init(ServletConfig config) throws ServletException {

        // 1、加载配置文件
        doLoadConfig (config.getInitParameter(LOCATION));

        // 2、扫描所有相关的类
        doScanner (properties.getProperty("scanPackage"));

        // 3、初始化所有相关类的实例，并保存到 IOC 容器中
        doInstance ();

        // 4、依赖注入
        doAutowired ();

        // 5、构造 HandlerMapping
        initHandlerMapping ();

        // 6、等待请求，匹配 URL，定位方法，反射调用执行
        // 调用 doGet 或者 doPost 方法

        // 提示信息 初始化完成
        System.out.println("handwritten mvcframework is init");
    }

    private void initHandlerMapping() {
        if (ioc.isEmpty()) return;
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {

            Class<?> clazz = entry.getValue().getClass();
            if (!clazz.isAnnotationPresent(HandWrittenController.class)) continue;

            String baseUrl = "";
            // 获取 Controller 的 Url 配置
            if (clazz.isAnnotationPresent(HandWrittenRequestMapping.class)) {
                HandWrittenRequestMapping requestMappingClass = clazz.getAnnotation(HandWrittenRequestMapping.class);
                baseUrl = requestMappingClass.value();
            }

            // 获取到 Method 的 Url 配置
            Method[] methods = clazz.getDeclaredMethods();
            for (Method method : methods) {

                // 没有加 requestMapping 的方法直接跳过
                if (!method.isAnnotationPresent(HandWrittenRequestMapping.class)) continue;

                // 映射 URL
                HandWrittenRequestMapping requestMappingMethod = method.getAnnotation(HandWrittenRequestMapping.class);
                String url = (baseUrl + requestMappingMethod.value().replaceAll("/+", "/"));
                handlerMapping.put(url, method);
                System.out.println("mapped" + url + "," + method) ;
            }
        }
    }

    // 将初始化到 IOC 容器中的类，需要赋值的字段进行赋值
    // 进行依赖注入
    private void doAutowired() {

        if (ioc.isEmpty()) return;

        // 遍历所有 ioc
        for (Map.Entry<String, Object> entry : ioc.entrySet()) {
            // 拿到实例对象中的所有属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : fields) {

                if (!field.isAnnotationPresent(HandWrittenAutowired.class)) continue;

                HandWrittenAutowired autowired = field.getAnnotation(HandWrittenAutowired.class);
                String beanName = autowired.value().trim();

                if ("".equals(beanName))
                    beanName = field.getType().getName();

                // 设置私有属性的访问权限
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }

            }
        }
    }

    // 初始化所有相关的类 并放入到 IOC 容器中
    // ioc 容器默认首字母小写，如有自定义首先使用自定义 所以先有一个处理首字母的工具方法
    private void doInstance() {

        if (classNames.size() == 0 ) return;

        try {
            for (String className : classNames) {
                // 得到相关的类型
                Class<?> clazz = Class.forName(className);
                // 判断是否是 controller 类型
                if (clazz.isAnnotationPresent(HandWrittenController.class)) {

                    // 注册 bean 的名称
                    String beanName = null;
                    // 获取到当前类的注解
                    HandWrittenController controller = clazz.getAnnotation(HandWrittenController.class);
                    // 等到当前注解的值
                    String controllerValue = controller.value();
                    // 如果没有自定义 controller 名称则使用默认首字母小写名称
                    if ("".equals(controllerValue.trim())) {

                        // 默认将首字母小写写作为 beanName
                        beanName = lowerFirstCase(clazz.getSimpleName());
                    } else
                        beanName = controllerValue;
                    ioc.put(beanName, clazz.newInstance());
                }
                // 判断是否是 service 类型
                else if (clazz.isAnnotationPresent(HandWrittenService.class)) {

                    HandWrittenService service = clazz.getAnnotation(HandWrittenService.class);
                    String serviceValue = service.value();
                    // 如果用户设置了名字，就使用用户自己设置
                    if (!"".equals(serviceValue)) {
                        ioc.put(serviceValue, clazz.newInstance());
                        continue;
                    }
                    // 如果自己没有设置，就按接口类型创建一个实例
                    Class<?>[] interfaces = clazz.getInterfaces();
                    for (Class<?> anInterface : interfaces) {
                        ioc.put(anInterface.getName(),clazz.newInstance());
                    }
                }
                // 都不是则跳过当前类型
                else continue;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 首字母小写处理
     * @param str
     * @return
     */
    private String lowerFirstCase (String str) {
        char[] chars = str.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    // 递归扫描出所有的 Class 文件
    private void doScanner(String packageName) {
        // 将所有的包路径转化为文件路径
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.","/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            // 如果是文件夹，继续递归
            if (file.isDirectory())
                doScanner(packageName + "." + file.getName());
            else
                classNames.add(packageName + "." + file.getName().replace(".class", "").trim());
        }
    }

    // 将文件读取到 Properties 对象中
    private void doLoadConfig(String location) {

        InputStream fis = null;
        try {
            // 得到当前文件绝对路径
            fis = this.getClass().getClassLoader().getResourceAsStream(location);
            // 1、读取配置文件
            properties.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    /**
     * 执行业务逻辑
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        try {

            doDispatch (req, resp);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {

        if (this.handlerMapping.isEmpty()) return;
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 not found");
            return;
        }

        Method method = this.handlerMapping.get(url);
        // 获取方法参数列表
        Class<?>[] paramTypes = method.getParameterTypes();
        // 获取请求参数
        Map<String, String[]> paramMap = req.getParameterMap();
        // 保存参数值
        Object[] paramValues = new Object[paramTypes.length];
        // 方法参数列表
        for (int i=0; i<paramTypes.length; i++) {
            // 根据参数名称，做某些处理
            Class paramType = paramTypes[i];
            if (paramType == HttpServletRequest.class) {
                // 参数类型已明确，这边强转类型
                paramValues[i] = req;
                continue;
            } else if (paramType == HttpServletResponse.class) {
                // 参数类型已明确，这边强转类型
                paramValues[i] = resp;
                continue;
            } else if (paramType == String.class) {
                for (Map.Entry<String, String[]> entry : paramMap.entrySet()) {
                    String value = Arrays.toString(entry.getValue())
                                         .replaceAll("\\[|\\]","")
                                         .replaceAll(",\\s",",");
                    paramValues[i] = value;
                }
            }

        }

        String beanName = lowerFirstCase(method.getDeclaringClass().getSimpleName());
        // 利用反射来调用
        method.invoke(this.ioc.get(beanName), paramValues);
    }
}