package no.nav.tilleggsstonader.sak.vedtak.forslag

import io.mockk.every
import io.mockk.mockk
import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.utils.dato.desember
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.behandling.BehandlingService
import no.nav.tilleggsstonader.sak.felles.domain.FaktiskMålgruppe
import no.nav.tilleggsstonader.sak.tidligsteendring.UtledTidligsteEndringService
import no.nav.tilleggsstonader.sak.util.fagsak
import no.nav.tilleggsstonader.sak.util.saksbehandling
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.VilkårService
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeService
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.aktivitet
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.faktaOgVurderingMålgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.VilkårperiodeTestUtil.målgruppe
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.MålgruppeType
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.Vilkårperioder
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class ForeslåVedtaksperioderUtenAktivitetTest {
    @ParameterizedTest
    @EnumSource(Stønadstype::class, names = ["REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSO", "REISE_OPPSTART_AVSLUTNING_HJEMREISE_TSR"])
    fun `ROAH forslag omfatter kun reiser selv om bostedsperioden dekker hele aktiviteten`(stønadstype: Stønadstype) {
        val behandling = saksbehandling(fagsak = fagsak(stønadstype = stønadstype))
        val behandlingService = mockk<BehandlingService>()
        val vilkårService = mockk<VilkårService>()
        val periodeService = mockk<VilkårperiodeService>()
        val tidligsteEndringService = mockk<UtledTidligsteEndringService>()
        every { behandlingService.hentSaksbehandling(behandling.id) } returns behandling
        every { tidligsteEndringService.utledTidligsteEndringIgnorerVedtaksperioder(behandling.id, kunROAHReiser = true) } returns null
        every { periodeService.hentVilkårperioder(behandling.id) } returns
            Vilkårperioder(
                målgrupper = listOf(målgruppe(fom = 1 januar 2026, tom = 31 mars 2026)),
                aktiviteter = listOf(aktivitet(fom = 1 januar 2026, tom = 31 mars 2026)),
            )
        val oppstart = vilkår(type = VilkårType.ROAH_OPPSTART_AVSLUTNING, fom = 5 januar 2026, tom = 5 januar 2026)
        val hjemreise = vilkår(type = VilkårType.ROAH_HJEMREISE, fom = 10 februar 2026, tom = 12 februar 2026)
        val bosted = vilkår(type = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE, fom = 1 januar 2026, tom = 31 mars 2026)
        every { vilkårService.hentVilkår(behandling.id) } returns listOf(bosted, oppstart, hjemreise)
        val service = ForeslåVedtaksperiodeService(behandlingService, periodeService, vilkårService, mockk(), tidligsteEndringService)
        assertThat(service.foreslåPerioder(behandling.id).map { it.fom to it.tom }).containsExactly(
            (5 januar 2026) to (5 januar 2026),
            (10 februar 2026) to (12 februar 2026),
        )
    }

    @Test
    fun `skal foreslå snitt av målgruppe og oppfylte vilkår uten aktivitet`() {
        val vilkårperioder =
            Vilkårperioder(
                målgrupper = listOf(målgruppe(fom = 1 januar 2026, tom = 31 mars 2026)),
                aktiviteter = emptyList(),
            )
        val vilkår =
            listOf(
                vilkår(fom = 15 desember 2025, tom = 10 januar 2026),
                vilkår(fom = 1 februar 2026, tom = 1 februar 2026),
                vilkår(fom = 1 mars 2026, tom = 1 mars 2026, resultat = Vilkårsresultat.IKKE_OPPFYLT),
            )

        val forslag = ForeslåVedtaksperioderUtil.foreslåPerioderUtenAktivitet(vilkårperioder, vilkår)

        assertThat(forslag.map { Triple(it.fom, it.tom, it.målgruppe) }).containsExactly(
            Triple(1 januar 2026, 10 januar 2026, FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE),
            Triple(1 februar 2026, 1 februar 2026, FaktiskMålgruppe.NEDSATT_ARBEIDSEVNE),
        )
        assertThat(forslag.map { it.aktivitet }).containsOnlyNulls()
    }

    @Test
    fun `skal slå sammen sammenhengende perioder`() {
        val vilkårperioder =
            Vilkårperioder(
                målgrupper = listOf(målgruppe(fom = 1 januar 2026, tom = 31 mars 2026)),
                aktiviteter = emptyList(),
            )
        val vilkår =
            listOf(
                vilkår(fom = 1 januar 2026, tom = 31 januar 2026),
                vilkår(fom = 1 februar 2026, tom = 28 februar 2026),
            )

        val forslag = ForeslåVedtaksperioderUtil.foreslåPerioderUtenAktivitet(vilkårperioder, vilkår)

        assertThat(forslag).hasSize(1)
        assertThat(forslag.single().fom).isEqualTo(1 januar 2026)
        assertThat(forslag.single().tom).isEqualTo(28 februar 2026)
    }

    @Test
    fun `skal kaste feil når det ikke finnes overlapp mellom målgruppe og vilkår`() {
        val vilkårperioder =
            Vilkårperioder(
                målgrupper = listOf(målgruppe(fom = 1 januar 2026, tom = 31 januar 2026)),
                aktiviteter = emptyList(),
            )
        val vilkår = listOf(vilkår(fom = 1 mars 2026, tom = 1 mars 2026))

        assertThatThrownBy { ForeslåVedtaksperioderUtil.foreslåPerioderUtenAktivitet(vilkårperioder, vilkår) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessage("Fant ingen gyldig overlapp mellom målgruppe og utgiftsperiodene")
    }

    @Test
    fun `skal kaste feil når ulike målgrupper overlapper`() {
        val vilkårperioder =
            Vilkårperioder(
                målgrupper =
                    listOf(
                        målgruppe(fom = 1 januar 2026, tom = 31 januar 2026),
                        målgruppe(
                            fom = 15 januar 2026,
                            tom = 28 februar 2026,
                            faktaOgVurdering = faktaOgVurderingMålgruppe(type = MålgruppeType.OVERGANGSSTØNAD),
                        ),
                    ),
                aktiviteter = emptyList(),
            )
        val vilkår = listOf(vilkår(fom = 20 januar 2026, tom = 20 januar 2026))

        assertThatThrownBy { ForeslåVedtaksperioderUtil.foreslåPerioderUtenAktivitet(vilkårperioder, vilkår) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("ikke er overlapp mellom ulike målgrupper")
    }
}
