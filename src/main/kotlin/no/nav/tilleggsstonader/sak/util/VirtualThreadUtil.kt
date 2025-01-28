package no.nav.tilleggsstonader.sak.util

import org.slf4j.MDC
import java.util.concurrent.Callable
import java.util.concurrent.Executors

object VirtualThreadUtil {
    /**
     * Håndter flere funksjoner parallelt. Beholder MDCContext
     */
    fun <T> Collection<() -> T>.parallelt(): List<T> {
        val mdc = MDC.getCopyOfContextMap()
        return Executors
            .newVirtualThreadPerTaskExecutor()
            .use { vt ->
                map { fn ->
                    Callable {
                        MDC.setContextMap(mdc)
                        fn()
                    }
                }.let { vt.invokeAll(it) }
            }.map { it.get() }
    }
}
