package no.nav.tilleggsstonader.sak.infrastruktur.felles

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@Service
class TransactionHandler {

    @Transactional(propagation = Propagation.REQUIRED)
    fun <T> runInTransaction(fn: () -> T) = fn()

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun <T> runInNewTransaction(fn: () -> T) = fn()
}
