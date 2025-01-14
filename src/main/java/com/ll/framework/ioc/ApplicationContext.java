package com.ll.framework.ioc;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ApplicationContext {
  private Map<String, Object> singletonObjects;
  private List<String> classNames;

  public ApplicationContext() {
    this.singletonObjects = new HashMap<>();
    String directoryPath = "out/production/classes/com/ll";
    String basePackage = "com.ll";
    this.classNames = findClasses(directoryPath, basePackage);
  }

  public <T> T genBean(String beanName) {
    if (singletonObjects.containsKey(beanName)) {
      return (T) singletonObjects.get(beanName);
    }

    String className = classNames.stream()
        .filter(name -> name.endsWith(beanName.substring(0,1).toUpperCase() + beanName.substring(1)))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("찾은 클래스 "+classNames+" 찾아야 할 클래스 "+beanName));

    try {
     Class<?> clazz = Class.forName(className);

      Class<?>[] parameterTypes = Arrays.stream(clazz.getDeclaredFields())
          .filter(field -> Modifier.isFinal(field.getModifiers()))
          .map(Field::getType)
          .toArray(Class<?>[]::new);


      Constructor<T> constructor = (Constructor<T>) clazz.getConstructor(parameterTypes);

      Object[] parameters = new Object[parameterTypes.length];
      for (int i = 0; i < parameterTypes.length; i++) {
        String injectBeanName = parameterTypes[i].getSimpleName();
        injectBeanName = injectBeanName.substring(0, 1).toLowerCase() + injectBeanName.substring(1);
        parameters[i] = genBean(injectBeanName);
      }

      T instance = constructor.newInstance(parameters);

      singletonObjects.put(beanName, instance);

      return instance;

    }catch (Exception e) {
      e.printStackTrace();
    }
    return null;
  }

  public static List<String> findClasses(String directoryPath, String basePackage) {

    List<String> classNames = new ArrayList<>();
    File directory = new File(directoryPath);
    if (!directory.exists()) {
      System.out.println("디렉토리가 존재하지 않습니다.");
      return classNames;
    }

    File[] files = directory.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isFile() && file.getName().endsWith(".class")) {
          // basePackage.subpackage.ClassName 형식으로 변환
          String className = basePackage.isEmpty()
              ? file.getName().replace(".class", "")
              : basePackage + "." + file.getName().replace(".class", "");
          classNames.add(className);
        } else if (file.isDirectory()) {
          // 하위 패키지의 경우 패키지명 추가
          String newBasePackage = basePackage.isEmpty()
              ? file.getName()
              : basePackage + "." + file.getName();
          classNames.addAll(findClasses(file.getAbsolutePath(), newBasePackage));
        }
      }
    }
    return classNames;
  }
}
