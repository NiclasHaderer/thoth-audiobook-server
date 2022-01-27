package io.github.huiibuh.utils

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CoroutineScheduler(val limit: Int) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val taskList = mutableListOf<Any>()


    fun schedule(callback: suspend CoroutineScope.() -> Any) {
        scope.launch {
            synchronized(this@CoroutineScheduler) {

            }
        }
    }
}
