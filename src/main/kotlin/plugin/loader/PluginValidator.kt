package plugin.loader

import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy
import java.util.*

class ProxyImpl(private val underlying: Any) : InvocationHandler {
    override fun invoke(proxy: Any, method: Method, args: Array<out Any?>?): Any? {
        val proxyMethods = underlying.javaClass.methods
        for (fn in proxyMethods) {
            if (
                fn.name == method.name &&
                Arrays.equals(fn.parameterTypes, method.parameterTypes) &&
                fn.returnType == method.returnType
            ) {
                return if (args == null) {
                    fn.invoke(underlying)
                } else {
                    fn.invoke(underlying, *args)
                }
            }
        }
        throw UnsupportedOperationException("$method with $args could not be called")
    }
}

inline fun <reified T> Any.viewAs(): T {
    return Proxy.newProxyInstance(javaClass.classLoader, arrayOf(T::class.java), ProxyImpl(this)) as T
}
