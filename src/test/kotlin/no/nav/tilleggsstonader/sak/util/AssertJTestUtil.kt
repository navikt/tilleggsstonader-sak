package no.nav.tilleggsstonader.sak.util

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Condition
import org.assertj.core.api.ThrowableAssert

fun hasCauseMessageContaining(msg: String) =
    Condition<Throwable>({
        val message = it.cause?.message ?: error("Mangler cause/message")
        assertThat(message).contains(msg)
        true
    }, "")

inline fun <reified T : Throwable> catchThrowableOfType(shouldRaiseThrowable: ThrowableAssert.ThrowingCallable): T =
    Assertions.catchThrowableOfType(shouldRaiseThrowable, T::class.java)
