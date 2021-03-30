package com.kt.remotecontrol.service.command;

import com.kt.remotecontrol.util.CharConstant;
import com.kt.remotecontrol.util.ErrorCode;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class CommandService {
    protected Map publishCommand;

    protected CommandService() {
        publishCommand = new HashMap();
    }

    protected String execute(Object executeObject, String methodName) {
        return execute(executeObject, methodName, null);
    }

    protected String execute(Object executeObject, String methodName, Properties params) {
        Class[] classes = null;
        Object[] objects = null;

        if (params != null) {
            classes = new Class[] { Properties.class };
            objects = new Object[] { params };
        }

        return execute(executeObject, executeObject.getClass(), methodName, classes, objects);
    }

    protected String executeSuperClass(Object executeObject, String methodName, Properties params) {
        Class superClass = executeObject.getClass().getSuperclass();

        return execute(executeObject, superClass, methodName, new Class[] { Properties.class }, new Object[] { params} );
    }

    private String execute(Object executeObject, Class objectClass, String methodName, Class[] parameterTypes, Object[] args) {
        try {
            Method method = objectClass.getDeclaredMethod(methodName, parameterTypes);

            return (String) method.invoke(executeObject, args);
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    protected StringBuffer getSUCCESS() {
        return getSUCCESS("");
    }

    protected StringBuffer getSUCCESS(String value) {
        StringBuffer data = new StringBuffer();
        data.append(ErrorCode.SUCCESS).append(CharConstant.CHAR_CARET);
        data.append(value);

        return data;
    }
}
