package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår

import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.security.token.support.spring.SpringTokenValidationContextHolder
import no.nav.tilleggsstonader.sak.behandling.domain.BehandlingRepository
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.TilsynBarnVedtakRepository
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.VedtakTilsynBarn
import no.nav.tilleggsstonader.sak.vedtak.barnetilsyn.dto.Utgift
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårRepository
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/vilkar/admin")
@ProtectedWithClaims(issuer = "azuread")
@Validated
class FlyttBeløpsperioderTilVilkårController(
    private val vedtakTilsynBarnVedtakRepository: TilsynBarnVedtakRepository,
    private val jdbcTemplate: NamedParameterJdbcTemplate,
    private val behandlingRepository: BehandlingRepository,
    private val vilkårRepository: VilkårRepository,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private fun utførEndringSomSystem() {
        SpringTokenValidationContextHolder().setTokenValidationContext(null)
    }

    @PostMapping
    @Transactional
    fun oppdaterVilkår() {
        utførEndringSomSystem()
        val alleVedtak = vedtakTilsynBarnVedtakRepository.findAll()
        // alleVedtak.forEach { håndterVedtak(it) }

        oppdaterBehandlingerSomErPåVedtakUtenVedtak(alleVedtak)
    }

    private fun håndterVedtak(vedtak: VedtakTilsynBarn) {
        val vedtakData = vedtak.vedtak
        val behandlingId = vedtak.behandlingId
        if (vedtakData == null) {
            return
        }
        val vilkårForBehandling = vilkårRepository.findByBehandlingId(behandlingId).associateBy { it.barnId }
        /*
        Hvordan skal vi populere vilkår med fom/tom der det ikke finnes noe fra vedtaket?
        Er kanskje greit at disse fortsatt forblir nullable og at man må ta stilling til de vid neste tilfelle man revurderer?
         */
        vedtakData.utgifter.forEach { (barnId, utgifter) ->
            if (utgifter.isNotEmpty()) {
                val vilkår = vilkårForBehandling[barnId]
                    ?: error("Finner ikke vilkår for barnId=$barnId behandling=$behandlingId")
                oppdaterVilkårMedPeriode(vilkår, utgifter)
                leggTilNyeVilkårForAndreUtgifter(vilkår, utgifter.drop(1))
            }
        }
    }

    private fun oppdaterBehandlingerSomErPåVedtakUtenVedtak(alleVedtak: Iterable<VedtakTilsynBarn>) {
        val behandlingerSomHarVedtak = alleVedtak.map { it.behandlingId }.toSet()
        behandlingRepository.findAll().forEach { behandling ->
            if (behandling.steg == StegType.BESLUTTE_VEDTAK && !behandling.erAvsluttet() && !behandlingerSomHarVedtak.contains(behandling.id)) {
                logger.info("Setter behandling til Inngangsvilkår for å kunne legge inn perioder")
                jdbcTemplate.update(
                    "UPDATE behandling SET steg=:steg WHERE id=:id",
                    mapOf(
                        "id" to behandling.id,
                        "steg" to StegType.INNGANGSVILKÅR.name,
                    ),
                )
            }
        }
    }

    private fun oppdaterVilkårMedPeriode(
        vilkår: Vilkår,
        utgifter: List<Utgift>,
    ) {
        jdbcTemplate.update(
            "UPDATE vilkar SET fom=:fom, tom=:tom, utgift=:utgift WHERE id=:id",
            mapOf(
                "id" to vilkår.id,
                "fom" to utgifter.first().fom.atDay(1),
                "tom" to utgifter.first().tom.atEndOfMonth(),
                "utgift" to utgifter.first().utgift,
            ),
        )
    }

    private fun leggTilNyeVilkårForAndreUtgifter(
        vilkår: Vilkår,
        utgifter: List<Utgift>,
    ) {
        utgifter.forEach {
            jdbcTemplate.update(
                """
                    INSERT INTO vilkar (id,behandling_id,opprettet_av,opprettet_tid,endret_av,endret_tid,resultat,
                    type,begrunnelse,unntak,delvilkar,barn_id,opphavsvilkaar_behandling_id,
                    opphavsvilkaar_vurderingstidspunkt,fom,tom,utgift) 
                    SELECT :id,
                    behandling_id,opprettet_av,opprettet_tid,endret_av,endret_tid,resultat,type,begrunnelse,unntak,
                    delvilkar,barn_id,opphavsvilkaar_behandling_id,opphavsvilkaar_vurderingstidspunkt,
                    :fom,:tom,:utgift 
                    FROM vilkar WHERE id=:kopierFraId
                """.trimIndent(),
                mapOf(
                    "id" to UUID.randomUUID(),
                    "kopierFraId" to vilkår.id,
                    "fom" to it.fom.atDay(1),
                    "tom" to it.tom.atEndOfMonth(),
                    "utgift" to it.utgift,
                ),
            )
        }
    }
}
