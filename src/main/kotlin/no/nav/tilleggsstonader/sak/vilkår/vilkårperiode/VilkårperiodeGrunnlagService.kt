package no.nav.tilleggsstonader.sak.vilkår.vilkårperiode

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.kontrakter.ytelse.EnsligForsørgerStønadstype
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.behandling.domain.Saksbehandling
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.felles.domain.BehandlingId
import no.nav.tilleggsstonader.sak.infrastruktur.database.repository.findByIdOrThrow
import no.nav.tilleggsstonader.sak.infrastruktur.exception.brukerfeilHvisIkke
import no.nav.tilleggsstonader.sak.infrastruktur.exception.feilHvis
import no.nav.tilleggsstonader.sak.infrastruktur.sikkerhet.BehandlerRolle
import no.nav.tilleggsstonader.sak.opplysninger.aktivitet.RegisterAktivitetService
import no.nav.tilleggsstonader.sak.opplysninger.søknad.SøknadService
import no.nav.tilleggsstonader.sak.opplysninger.ytelse.YtelseService
import no.nav.tilleggsstonader.sak.tilgang.TilgangService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.GrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.HentetInformasjon
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.PeriodeGrunnlagYtelse
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.RegisterAktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.SlåSammenPeriodeGrunnlagYtelseUtil.slåSammenOverlappendeEllerPåfølgende
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlag
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagDomain
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.VilkårperioderGrunnlagRepository
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.grunnlag.tilDomenetype
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.temporal.ChronoUnit

@Service
class VilkårperiodeGrunnlagService(
    private val behandlingService: BehandlingService,
    private val vilkårperioderGrunnlagRepository: VilkårperioderGrunnlagRepository,
    private val registerAktivitetService: RegisterAktivitetService,
    private val ytelseService: YtelseService,
    private val søknadService: SøknadService,
    private val tilgangService: TilgangService,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun oppdaterGrunnlag(behandlingId: BehandlingId) {
        val behandling = behandlingService.hentBehandling(behandlingId)
        feilHvis(behandling.status.behandlingErLåstForVidereRedigering()) {
            "Kan ikke oppdatere grunnlag når behandlingen er låst"
        }
        feilHvis(behandling.steg != StegType.INNGANGSVILKÅR) {
            "Kan ikke oppdatere grunnlag når behandlingen er i annet steg enn vilkår."
        }

        val eksisterendeGrunnlag = vilkårperioderGrunnlagRepository.findByIdOrThrow(behandlingId)

        val eksisterendeHentetFom = eksisterendeGrunnlag.grunnlag.hentetInformasjon.fom
        val tom = YearMonth.now().plusYears(1).atEndOfMonth()

        val nyGrunnlagsdata = hentGrunnlagsdata(behandlingId, eksisterendeHentetFom, tom)
        vilkårperioderGrunnlagRepository.update(eksisterendeGrunnlag.copy(grunnlag = nyGrunnlagsdata))
        val tidSidenForrigeHenting =
            ChronoUnit.HOURS.between(nyGrunnlagsdata.hentetInformasjon.tidspunktHentet, LocalDateTime.now())
        logger.info("Oppdatert grunnlagsdata for behandling=$behandlingId timerSidenForrige=$tidSidenForrigeHenting")
    }

    fun hentEllerOpprettGrunnlag(behandlingId: BehandlingId): VilkårperioderGrunnlag? {
        val grunnlag = vilkårperioderGrunnlagRepository.findByBehandlingId(behandlingId)?.grunnlag

        if (grunnlag != null) {
            return grunnlag
        }

        val behandling = behandlingService.hentSaksbehandling(behandlingId)
        return if (behandling.status.behandlingErLåstForVidereRedigering()) {
            null
        } else {
            opprettGrunnlagsdata(behandling).grunnlag
        }
    }

    private fun opprettGrunnlagsdata(behandling: Saksbehandling): VilkårperioderGrunnlagDomain {
        brukerfeilHvisIkke(tilgangService.harTilgangTilRolle(BehandlerRolle.SAKSBEHANDLER)) {
            "Behandlingen er ikke påbegynt. Kan ikke opprette vilkårperiode hvis man ikke er saksbehandler"
        }

        val søknadMetadata = søknadService.hentSøknadMetadata(behandling.id)
        val utgangspunktDato = søknadMetadata?.mottattTidspunkt ?: behandling.opprettetTid

        val antallMånederBakITiden = behandling.stønadstype.antallMånederBakITiden()
        val fom = YearMonth.from(utgangspunktDato).minusMonths(antallMånederBakITiden).atDay(1)
        val tom = YearMonth.from(utgangspunktDato).plusYears(1).atEndOfMonth()

        val grunnlag = hentGrunnlagsdata(behandling.id, fom, tom)
        return vilkårperioderGrunnlagRepository.insert(
            VilkårperioderGrunnlagDomain(
                behandlingId = behandling.id,
                grunnlag = grunnlag,
            ),
        )
    }

    /**
     * Ulike stønader har ulikt behov for antall måneder bak i tiden som skal hentes
     */
    private fun Stønadstype.antallMånederBakITiden(): Long = when (this) {
        Stønadstype.BARNETILSYN -> 3
        Stønadstype.LÆREMIDLER -> 6
    }

    private fun hentGrunnlagsdata(
        behandlingId: BehandlingId,
        fom: LocalDate,
        tom: LocalDate,
    ): VilkårperioderGrunnlag {
        return VilkårperioderGrunnlag(
            aktivitet = hentGrunnlagAktvititet(behandlingId, fom, tom),
            ytelse = hentGrunnlagYtelse(behandlingId, fom, tom),
            hentetInformasjon = HentetInformasjon(
                fom = fom,
                tom = tom,
                tidspunktHentet = LocalDateTime.now(),
            ),
        )
    }

    private fun hentGrunnlagAktvititet(
        behandlingId: BehandlingId,
        fom: LocalDate,
        tom: LocalDate,
    ) = GrunnlagAktivitet(
        aktiviteter = registerAktivitetService.hentAktiviteterForGrunnlagsdata(
            ident = behandlingService.hentSaksbehandling(behandlingId).ident,
            fom = fom,
            tom = tom,
        ).map {
            RegisterAktivitet(
                id = it.id,
                fom = it.fom,
                tom = it.tom,
                type = it.type,
                typeNavn = it.typeNavn,
                status = it.status,
                statusArena = it.statusArena,
                antallDagerPerUke = it.antallDagerPerUke,
                prosentDeltakelse = it.prosentDeltakelse,
                erStønadsberettiget = it.erStønadsberettiget,
                erUtdanning = it.erUtdanning,
                arrangør = it.arrangør,
                kilde = it.kilde,
            )
        },
    )

    private fun hentGrunnlagYtelse(
        behandlingId: BehandlingId,
        fom: LocalDate,
        tom: LocalDate,
    ): GrunnlagYtelse {
        val ytelserFraRegister = ytelseService.hentYtelseForGrunnlag(behandlingId = behandlingId, fom = fom, tom = tom)

        return GrunnlagYtelse(
            perioder = ytelserFraRegister.perioder
                .filter { it.aapErFerdigAvklart != true }
                .filter { it.ensligForsørgerStønadstype != EnsligForsørgerStønadstype.BARNETILSYN }
                .map {
                    PeriodeGrunnlagYtelse(
                        type = it.type,
                        fom = it.fom,
                        tom = it.tom,
                        ensligForsørgerStønadstype = it.ensligForsørgerStønadstype?.tilDomenetype(),
                    )
                }
                .slåSammenOverlappendeEllerPåfølgende(),
        )
    }
}
