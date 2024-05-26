package com.creclm.extension.loader;

import com.creclm.adapter.InjectionAdapter;
import com.creclm.annotation.*;
import com.creclm.service.InjectionServiceFactory;
import com.creclm.extension.cache.BeanCacheFactory;
import com.creclm.extension.support.Holder;
import com.creclm.util.SystemResourcesUtil;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static com.creclm.util.InjectionUtil.injection;


/**
 * 扩展 服务（SPI） 加载工厂
 * 功能：
 * 1、每个服务都对应一个 服务（SPI） 加载工厂
 * 加载工厂保存服务提供商的一系列缓存（类信息、别名信息、实例信息）
 * <p>
 * 2、实现每个服务提供商的实例化对象的依赖注入  injectExtension
 * （迭代深度优先注入，无法注入就是配置出现了问题）
 */
public class ServiceLoaderFactory<T> {
    // 日志记录
    private static final Logger logger = LoggerFactory.getLogger(BeanCacheFactory.class);

    // 配置文件路径（支持多种配置）
    private static final String CRECLM_INTERNAL_DIRECTORY = "META-INF/creclm/internal/";
    private static final String CRECLM_TEST_DIRECTORY = "META-INF/creclm/test/";
    private static final String CRECLM_DIRECTORY = "META-INF/creclm/";
    private static final String SERVICES_DIRECTORY = "META-INF/services/";

    private static final String INJECTION_ADAPTER_MANAGER_NAME = "injectionManager";

    // 依赖注入模块实现的目标工厂
    private final Holder<InjectionServiceFactory> injectionProviderFactory = new Holder<InjectionServiceFactory>();

    // 当前扩展加载器服务类Class对象  始终是一个标记了@SPI的 接口类型
    private final Holder<Class<?>> service = new Holder<Class<?>>();

    public Holder<InjectionServiceFactory> getInjectionProviderFactory() {
        return injectionProviderFactory;
    }

    // 服务加载器工厂构造函数
    private ServiceLoaderFactory(Class<?> service) {
        this.service.set(service);

        // 任何服务工厂的初始化之前都要先初始化这个依赖注入的服务工厂
        if (service != InjectionServiceFactory.class) {
            ServiceLoaderFactory<InjectionServiceFactory> factory =
                    ServiceLoaderFactory.providersLoader(InjectionServiceFactory.class);
            InjectionAdapter adapter = (InjectionAdapter)
                    factory.loadInjectionInstance();
            this.injectionProviderFactory.set(adapter);
        }
    }

    /**
     * 实例化并加载所有依赖注入的工厂 服务提供者    返回 管理者（也是一个服务，但是管理其他所有的同类服务）
     *
     * @return
     */
    public T loadInjectionInstance() {
        String adapterName = null;
        InjectionAdapter ret;
        // 获取依赖注入适配器实例对象
        Holder<Object> instance = BeanCacheFactory.getCacheInjectionAdapterInstance();
        if (instance.get() == null) {
            synchronized (this) {
                instance = BeanCacheFactory.getCacheInjectionAdapterInstance();
                if (instance.get() == null) {
                    Map<String, Class<?>> providerClasses = this.loadAllProviders();
                    Class<?> clazz = null;
                    try {
                        // 需要缓存 适配器 的Class和实例以及全局实例（全局Class不用缓存）
                        for (String name : providerClasses.keySet()) {
                            clazz = providerClasses.get(name);
                            if (clazz == null) continue;
                            // 缓存实例化的provider对象 （这里是依赖注入的对象）
                            if (clazz.isAnnotationPresent(Adapter.class)
                                    && BeanCacheFactory.getCacheInjectionAdapterInstance().get() == null) {
                                BeanCacheFactory.getCacheInjectionAdapterClass().set(clazz);
                                Object instanceTmp = injection(clazz.newInstance());
                                instance.set(instanceTmp);
                                adapterName = name;
                                BeanCacheFactory.addInstanceToCacheIfAbsent(adapterName, instanceTmp);
                            } else if (clazz.isAnnotationPresent(Adapter.class)
                                    && BeanCacheFactory.getCacheInjectionAdapterInstance().get().getClass() != clazz) {
                                logger.error("不能实例化两个不同的依赖注入适配者实例对象");
                                throw new IllegalStateException("不能实例化两个不同的依赖注入适配者实例对象");
                            } else if (!clazz.isAnnotationPresent(Adapter.class)
                                    && BeanCacheFactory.getLoadedInstance(clazz) == null) {
                                BeanCacheFactory.addInstanceToCacheIfAbsent(name, injection(clazz.newInstance()));
                            } else if (!clazz.isAnnotationPresent(Adapter.class)
                                    && BeanCacheFactory.getLoadedInstance(clazz).getClass() != clazz) {
                                logger.debug("已经实例化过了这个对象哦" + clazz.getName());
                            }
                        }
                        ret = (InjectionAdapter) BeanCacheFactory.getCacheInjectionAdapterInstance().get();
                        // 将其他的服务提供者缓存进入adapter适配器进行管理（依赖注入）
                        for (String name : providerClasses.keySet()) {
                            if (!name.equals(adapterName)) {
                                Object implTmp = BeanCacheFactory.getLoadedInstance(providerClasses.get(name));
                                ret.getImpls().add((InjectionServiceFactory) implTmp);
                            }
                        }
                    } catch (Throwable e) {
                        logger.error("实例化对象失败 class: " + clazz.getName());
                    }
                }
            }
        }
        return (T) BeanCacheFactory.getCacheInjectionAdapterInstance().get();
    }

    /**
     * 实现加载service服务的全部服务提供商providers
     *
     * @param <T>
     * @return
     */
    private <T> Map<String, Class<?>> loadAllProviders() {
        Map<String, Class<?>> providerClasses = new HashMap<String, Class<?>>();
        this.loadServiceDirectory(providerClasses, ServiceLoaderFactory.CRECLM_INTERNAL_DIRECTORY);
        this.loadServiceDirectory(providerClasses, ServiceLoaderFactory.CRECLM_DIRECTORY);
        this.loadServiceDirectory(providerClasses, ServiceLoaderFactory.CRECLM_TEST_DIRECTORY);
        this.loadServiceDirectory(providerClasses, ServiceLoaderFactory.SERVICES_DIRECTORY);
        if (providerClasses.size() == 0) {
            throw new IllegalStateException("初始化加载依赖注入provider错误");
        }
        logger.debug("加载" + providerClasses.size() + "个依赖注入provider");
        // 这里不用考虑添加进入Bean缓存工厂了，加载资源时候已经加入缓存了
        return providerClasses;
    }

    /**
     * 加载对应服务 dir 的服务提供商（providers）资源 到 extensionClasses中
     *
     * @param providerClasses
     * @param dir
     */
    private void loadServiceDirectory(Map<String, Class<?>> providerClasses, String dir) {
        // 读取 dir目录下的的 type服务Service接口的内容，查询是否存在服务提供商 Provider
        // this.service.getName获取当前type的Class对象的全限定名
        String fileName = dir + this.service.get().getName();
        try {
            ClassLoader classLoader = SystemResourcesUtil.getClassLoader(this.getClass());
            // Enumeration是一种特殊的枚举抽象接口，实现类会实现迭代器
            Enumeration<URL> urls;
            if (classLoader != null) {
                // 根据fileName查找 类输出目录（target）下面的资源
                urls = classLoader.getResources(fileName);
            } else {
                // 直接用 Java 运行时环境(JRE)提供的系统类加载器
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls != null) {
                // 存在多个资源时, 遍历所有的资源url
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    logger.debug("存在这个资源文件 url：" + url);
                    this.loadServiceResource(providerClasses, url, classLoader);
                }
            } else {
                logger.error(fileName + " 文件不存在");
            }
        } catch (Throwable e) {
            logger.error("加载扩展服务 " + this.service.get().getName() + " 的Providers失败");
        }
    }

    /**
     * 加载 url 对应服务的服务提供商（providers）资源到 extensionClasses中
     *
     * @param providerClasses
     * @param url
     * @param classLoader
     */
    private void loadServiceResource(Map<String, Class<?>> providerClasses, URL url, ClassLoader classLoader) {
        try {
            // 使用 url 打开一个文件输出流
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(url.openStream(), "utf-8"));
            String line;
            try {
                // 逐行读取
                String name;
                String qualifiedName;
                while ((line = reader.readLine()) != null) {
                    // 解析抛弃注释信息   name=xxx.xxx.xxx(全限定名)
                    int index = line.indexOf(35);  // 注释信息 # 的索引
                    if (index >= 0) {
                        line = line.substring(0, index); // 截断注释信息
                    }
                    if (line.length() > 0) {
                        index = line.indexOf(61); // 等于号 =
                        // 可以没有等于号，就是在Provider注解上备注了别名
                        if (index < 0) {
                            name = "";
                            qualifiedName = line.trim();
                        } else {
                            name = line.substring(0, index).trim();
                            qualifiedName = line.substring(index + 1).trim();
                        }

                        if (qualifiedName.length() > 0) {
                            this.loadProviderClass(providerClasses, name, qualifiedName, classLoader);
                        }
                    }
                }
            } catch (Throwable e) {
                logger.error("读取服务扩展文件" + this.service.toString() + "失败，class文件资源在" + url + "这个url中");
            }
        } catch (Throwable e) {
            logger.error("打开服务扩展文件" + this.service.toString() + "失败，class文件资源在" + url + "这个url中");
        }
    }

    /**
     * 根据限定名加载扩展服务 提供商
     *
     * @param providerClasses ： 存在到这个容器中
     * @param name            ： 别名
     * @param qualifiedName   ： 限定名
     * @return
     */
    private void loadProviderClass(Map<String, Class<?>> providerClasses, String name,
                                   String qualifiedName, ClassLoader classLoader) {
        // 查询缓存是否存在这个Class对象
        String aliasName = getAliasName(name);
        if (BeanCacheFactory.getLoadedClass(aliasName) != null) {
            return;
        }
        Class<?> clazz;
        try {
            clazz = Class.forName(qualifiedName, true, classLoader);
            // 进行类的检查
            if (!this.service.get().isAssignableFrom(clazz)) {
                throw new IllegalStateException("提供商" + clazz.getName() + "不是服务"
                        + this.service.get().getName() + "的实现");
            }
            // 检查类是否是依赖注入自适应扩展工厂 即标记了InjectionAdapter注解的类
            if (clazz.isAnnotationPresent(Adapter.class)) {
                Holder<Class<?>> adapterClass = BeanCacheFactory.getCacheInjectionAdapterClass();
                if (adapterClass.get() == null) {
                    // adapterClass为null，就加载为这个clazz
                    synchronized (this) {
                        adapterClass = BeanCacheFactory.getCacheInjectionAdapterClass();
                        if (adapterClass.get() == null) {
                            // 将这个Clazz对象也注入到Bean工厂的adapterClass和全局Class中
                            adapterClass.set(clazz);
                            BeanCacheFactory.addClassToCacheIfAbsent(aliasName, adapterClass.get());
                        }
                    }
                } else if (clazz.equals(adapterClass.get())) {
                    // 不为空，且前后适配管理服务实现类不一致，就抛出异常
                    throw new IllegalStateException("不能同时存在多个适配器类，请检查 " +
                            adapterClass.get() + "和" + clazz + "是否同时注解了InjectionAdapter");
                }
            } else if (isWrapperClass(clazz)) {
                // 如果是包装类，就将这个包装类的value存入Class对象中
                Class<?> type = clazz.getField("value").getType();
                String nameTmp = type.getName().substring(0, 1).toLowerCase() + type.getName().substring(1);
                BeanCacheFactory.addClassToCacheIfAbsent(nameTmp, type);
            }else if (clazz.isAnnotationPresent(other.class)) {
                // TODO 功能扩充点


            } else { // 外部自定义SPI服务和其他服务提供商 / 以及其他非特定注解类
                // 判断这个服务是否存在无参构造，不存在会报错
                clazz.getConstructor();
                // 存在Provider注解且备注了别名的话就用这个别名，而不用 aliasName
                if (clazz.isAnnotationPresent(Provider.class)
                        && !"".equals(clazz.getAnnotation(Provider.class).value())) {
                    aliasName = getAliasName(clazz.getAnnotation(Provider.class).value());
                }
                if (aliasName == null || "".equals(aliasName)) {
                    throw new IllegalStateException("没有配置服务提供者别名，检查配置文件或者Provider注解");
                }

                // 全局不存在这者服务提供者Class对象
                Class<?> loadedClass = BeanCacheFactory.getLoadedClass(aliasName, clazz);
                if (loadedClass == null) {
                    synchronized (this) {
                        loadedClass = BeanCacheFactory.getLoadedClass(aliasName, clazz);
                        if (loadedClass == null) {
                            // 进入Bean池缓存记录
                            BeanCacheFactory.addClassToCacheIfAbsent(aliasName, clazz);
                        }
                    }
                }
            }
            // 统一进行处理 providerClasses
            if (!providerClasses.containsValue(clazz)) {
                providerClasses.put(aliasName, clazz);
            } else {
                // 重复别名不会进行覆盖
                for (String key : providerClasses.keySet()) {
                    if (providerClasses.get(key).equals(clazz) && !key.equals(aliasName)) {
                        String errMsg = "一个服务下一个别名不能存在两个服务提供者，请检查这类提供者如：" + aliasName;
                        logger.error(errMsg);
                        throw new IllegalStateException(errMsg);
                    }
                }
            }
        } catch (Throwable e) {
            logger.error("反射加载类失败， class为：" + qualifiedName);
            e.printStackTrace();
        }
    }

    /**
     * 获取service这个服务的类加载对象
     *
     * @param service
     * @param <T>
     * @return
     */
    public static <T> ServiceLoaderFactory<T> providersLoader(Class<T> service) {
        // 服务的判定：1、不为null、是接口、存在SPI注解
        if (service == null || !service.isInterface() || !service.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("传入的服务类型出错，检查是否为null/不是接口/不存在SPI注解");
        }
        // 从Bean工厂中加载这个 service的实例对象
        ServiceLoaderFactory<T> instance = (ServiceLoaderFactory<T>) BeanCacheFactory.getLoadedInstance(service);
        if (instance == null) {  // 不存在这个缓存
            synchronized (service) {
                String name = service.getName() + "@";
                instance = (ServiceLoaderFactory<T>) BeanCacheFactory.getLoadedInstance(name);
                if (instance == null) {
                    instance = new ServiceLoaderFactory<T>(service);
                    // 服务对象别名为 全限定名 + @
                    BeanCacheFactory.addInstanceToCacheIfAbsent(service.getName() + "@", instance);
                    BeanCacheFactory.addClassToCacheIfAbsent(service.getName() + "@", service);
                }
            }
        }
        return instance;
    }

    /**
     * 通过别名获取实现服务对象
     *
     * @param name
     * @return
     */
    public T getProvider(String name) {
        String aliasName = getAliasName(name);
        // 查询缓存是否存在这个Class对象，存在的话再查询是否存在实例对象
        Class<?> clazz = BeanCacheFactory.getLoadedClass(aliasName);
        if (clazz == null) {
            // 加载Class对象先
            loadAllProviders();
            clazz = BeanCacheFactory.getLoadedClass(aliasName);
        }
        // 存在Class对象，查询是否存在实例对象
        Object instance = BeanCacheFactory.getLoadedInstance(aliasName);
        if (instance == null) {
            synchronized (this) {
                instance = BeanCacheFactory.getLoadedInstance(aliasName);
                if (instance == null) {
                    // 获取实例化对象
                    try {
                        instance = injection(clazz.newInstance());
                        if (instance == null) {
                            logger.error("没有这个实现Provider类，别名为：" + name);
                            throw new IllegalStateException("没有这个实现Provider类，别名为：" + name);
                        }
                        // 添加实例化缓存
                        BeanCacheFactory.addInstanceToCacheIfAbsent(aliasName, instance);
                    } catch (Throwable e) {
                        logger.error(name + "别名的对象实例化失败");
                    }
                }
            }
        }
        return (T) instance;
    }

    /**
     * 通过别名查询Bean工厂是否存在Bean实例化对象，存在返回实例化对象，否则返回null
     *
     * @param name
     * @return
     */
    public T getExistProvider(String name) {
        return (T) BeanCacheFactory.getLoadedInstance(getAliasName(name));
    }

    /**
     * 返回服务提供者的内部别名体系
     * 这样是为了统一管理Bean对象实时的别名体系
     * <p>
     * dubbo是将服务的提供者provider缓存到对应对象中，而不集中缓存Bean对象，就无需别名转换了
     * <p>
     * 本SPI框架的别名规则：
     * 本类对象的别名: 泛型类的全类名 + @
     * <p>
     * SPI -- service ： 接口不用缓存
     * provider ： service全类名 + "#" + provider全类型
     *
     * @param name：外部别名
     * @return
     */
    private String getAliasName(String name) {
        return this.service.get().getName() + "#" + name;
    }

    /**
     * 判断一个Class对象是否是一个Wrapper包装类(如Holder就是包装类)
     *
     * @return
     */
    public static boolean isWrapperClass(Class<?> clazz) {
        // 如果类只存在一个成员变量，且泛型类型和变量类型一致，就认为是包装类
        Field[] declaredFields = clazz.getDeclaredFields();
        TypeVariable<? extends Class<?>>[] typeParameters = clazz.getTypeParameters();
        if (declaredFields.length != 1 || typeParameters.length != 1) {
            return false;
        }
        if (!clazz.getTypeParameters()[0].getName().equals(typeParameters[0].getName())) {
            return false;
        }
        // 包装类存在一个set和get方法
        long count = Arrays.stream(clazz.getDeclaredMethods())
                .filter(i -> i.getName().equals("set") || i.getName().equals("get"))
                .count();
        if (count != 2) {
            return false;
        }
        return true;
    }

}
