package com.pranav.autocompletion.api;

import java.lang.reflect.Method;

public class MethodItem extends Item {

  private Method method;

  private ArrayList<String> parameters = new ArrayList<>();

  private Class<?> declaringClass;

  public MethodItem(Method method) {
    this.method = method;
for (Class<?> cl : method.getParameterTypes()) {
parameters.add(cl.getName());
}
    declaringClass = method.getDeclaringClass();
  }

  public String getName() {
    return method.getName();
  }

  public String getFullName() {
    return getName() + getParameters();
  }

  public String getParameters() {
    StringBuilder builder = new StringBuilder();
    for (String param : parameters) {
      builder.append(param);
      builder.append(", ");
    }
  }

  public Class<?> getClass() {
    return declaringClass;
  }
}