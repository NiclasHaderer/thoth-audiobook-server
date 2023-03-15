package io.thoth.openapi.schema;


import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;


public class TypeToken2<T> {
    private final Type type;

    public TypeToken2() {
        this.type = getTypeTokenTypeArgument();
    }

    private Type getTypeTokenTypeArgument() {
        Type superClass = getClass().getGenericSuperclass();
        if (superClass instanceof ParameterizedType parameterized) {
            if (parameterized.getRawType() == TypeToken2.class) {
                return parameterized.getActualTypeArguments()[0];
            }
        } else if (superClass == TypeToken2.class) {
            throw new IllegalStateException("TypeToken must be created with a type argument: When using code shrinkers (ProGuard, R8, ...) make sure that generic signatures are preserved.");
        }

        throw new IllegalStateException("Must only create direct subclasses of TypeToken");
    }

    public Type getType() {
        return type;
    }
}
