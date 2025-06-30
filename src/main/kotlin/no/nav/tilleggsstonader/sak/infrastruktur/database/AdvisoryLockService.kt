package no.nav.tilleggsstonader.sak.infrastruktur.database

import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

/**
 * Service for å utføre databaselås.
 * Eks låse basert på en behandlingId, slik at kall som først sjekker om noe finnes i databasen før den
 * oppretter noe koblet til behandlingen blir stoppet, dersom det den leter etter opprettes i en annen transaksjon
 *
 * Alternativet er å bruke Row lock, med eks @Lock(LockMode.PESSIMISTIC_WRITE) i Repository,
 * som fungerer hvis tabellen allerede inneholder en rad
 */
@Service
class AdvisoryLockService(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Låser en transaksjon for å sikre at ingen andre transaksjoner kan endre dataene mens denne kjører.
     * Når en tabell ikke inneholder en rad, så klarer man ikke å bruke row lock, og da er det fint å bruke advisory lock.
     * propagation = [Propagation.MANDATORY] er viktig då locken er for hele transaksjonen, og releases automatisk når transaksjonen er ferdig.
     *
     * @param lock være en hvilken som helst objekt som brukes for å identifisere låsen, f.eks. en behandlingId. som blir hashet
     */
    @Transactional(propagation = Propagation.MANDATORY)
    fun <T> lockForTransaction(
        lock: Any,
        action: () -> T,
    ): T {
        val lockId = lock.hashCode()
        logger.debug("Låser lockId=$lockId")

        jdbcTemplate.query("SELECT pg_advisory_xact_lock(:lockId)", mapOf("lockId" to lockId)) { }

        return action()
    }
}
