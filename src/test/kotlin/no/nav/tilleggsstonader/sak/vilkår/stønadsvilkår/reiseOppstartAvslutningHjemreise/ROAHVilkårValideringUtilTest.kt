package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise

import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.feil.Feil
import no.nav.tilleggsstonader.libs.utils.dato.februar
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.libs.utils.dato.mars
import no.nav.tilleggsstonader.sak.util.vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaReiseOppstartAvslutningHjemreiseUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.ReiseId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkår
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårStatus
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.VilkårType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.Vilkårsresultat
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.FaktaUbestemtType
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårMidlertidigBosted
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.LagreVilkårReiseOppstartAvslutningHjemreise
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.reiseOppstartAvslutningHjemreise.domain.TypeReiseformål
import no.nav.tilleggsstonader.sak.vilkår.vilkårperiode.domain.VilkårperiodeGlobalId
import org.assertj.core.api.Assertions.assertThatCode
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ROAHVilkårValideringUtilTest {
    private val aktivitetId = VilkårperiodeGlobalId.random()

    @Test
    fun `reise innenfor paraplyens periode er gyldig, også på grensene`() {
        val paraply = paraply(fom = 1 februar 2026, tom = 28 februar 2026)

        assertThatCode { valider(reise(1 februar 2026, 28 februar 2026), paraply) }.doesNotThrowAnyException()
    }

    @Test
    fun `reise som starter før paraplyen avvises som brukerfeil`() {
        val paraply = paraply(fom = 1 februar 2026, tom = 28 februar 2026)

        assertThatThrownBy { valider(reise(31 januar 2026, 28 februar 2026), paraply) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Reisens periode må være innenfor perioden hvor bruker bor borte")
    }

    @Test
    fun `reise som slutter etter paraplyen avvises som brukerfeil`() {
        val paraply = paraply(fom = 1 februar 2026, tom = 28 februar 2026)

        assertThatThrownBy { valider(reise(1 februar 2026, 1 mars 2026), paraply) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Reisens periode må være innenfor perioden hvor bruker bor borte")
    }

    @Test
    fun `paraply uten periode gir intern feil`() {
        val paraply = paraply(fom = null, tom = null)

        assertThatThrownBy { valider(reise(1 februar 2026, 28 februar 2026), paraply) }
            .isInstanceOf(IllegalStateException::class.java)
            .hasMessageContaining("Forventer at fom er satt")
    }

    @Test
    fun `paraply på annen aktivitet gir intern feil`() {
        val paraply = paraply(fom = 1 januar 2026, tom = 31 mars 2026, aktivitetId = VilkårperiodeGlobalId.random())

        assertThatThrownBy { valider(reise(1 februar 2026, 28 februar 2026), paraply) }
            .isInstanceOf(Feil::class.java)
            .hasMessageContaining("Reisen må høre til samme aktivitet som paraplyvilkåret")
    }

    @Test
    fun `paraplyvilkår med adresser er gyldig`() {
        assertThatCode { ROAHVilkårValideringUtil.validerMidlertidigBosted(midlertidigBosted("Fra", "Til")) }
            .doesNotThrowAnyException()
    }

    @Test
    fun `paraplyvilkår uten fra adresse avvises som brukerfeil`() {
        assertThatThrownBy { ROAHVilkårValideringUtil.validerMidlertidigBosted(midlertidigBosted(" ", "Til")) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Fra adresse må fylles ut")
    }

    @Test
    fun `paraplyvilkår uten til adresse avvises som brukerfeil`() {
        assertThatThrownBy { ROAHVilkårValideringUtil.validerMidlertidigBosted(midlertidigBosted("Fra", "")) }
            .isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("Til adresse må fylles ut")
    }

    @Test
    fun `ny paraplyperiode som fortsatt inneholder reisene er gyldig`() {
        val reise = reiseVilkår(15 februar 2026)

        assertThatCode {
            ROAHVilkårValideringUtil.validerReiserErInnenforParaplyensNyePeriode(
                midlertidigBosted("Fra", "Til", fom = 1 februar 2026, tom = 15 februar 2026),
                listOf(reise),
            )
        }.doesNotThrowAnyException()
    }

    @Test
    fun `ny paraplyperiode som utelater en reise gir varsel`() {
        val reiseInnenfor = reiseVilkår(10 februar 2026)
        val reiseUtenfor = reiseVilkår(20 februar 2026)

        assertThatThrownBy {
            ROAHVilkårValideringUtil.validerReiserErInnenforParaplyensNyePeriode(
                midlertidigBosted("Fra", "Til", fom = 1 februar 2026, tom = 15 februar 2026),
                listOf(reiseInnenfor, reiseUtenfor),
            )
        }.isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("1 reise(r) ville havnet utenfor")
    }

    @Test
    fun `paraplyvilkår som overlapper et annet på samme aktivitet avvises`() {
        val eksisterende = paraply(fom = 1 februar 2026, tom = 15 februar 2026)

        assertThatThrownBy {
            ROAHVilkårValideringUtil.validerIkkeOverlappendeParaplyvilkår(
                midlertidigBosted("Fra", "Til", fom = 15 februar 2026, tom = 28 februar 2026),
                eksisterendeVilkår = null,
                vilkårIBehandling = listOf(eksisterende),
            )
        }.isInstanceOf(ApiFeil::class.java)
            .hasMessageContaining("overlapper med en annen periode hvor bruker bor borte")
    }

    @Test
    fun `paraplyvilkår som grenser til, er på annen aktivitet, er slettet eller er seg selv, er gyldig`() {
        val eksisterende = paraply(fom = 1 februar 2026, tom = 15 februar 2026)
        val annenAktivitet = paraply(fom = 1 februar 2026, tom = 28 februar 2026, aktivitetId = VilkårperiodeGlobalId.random())
        val slettet =
            paraply(
                fom = 1 februar 2026,
                tom = 28 februar 2026,
            ).copy(status = VilkårStatus.SLETTET, resultat = Vilkårsresultat.SLETTET, slettetKommentar = "slettet")

        fun valider(
            nytt: LagreVilkårMidlertidigBosted,
            eksisterendeVilkår: Vilkår? = null,
            vilkår: List<Vilkår>,
        ) = ROAHVilkårValideringUtil.validerIkkeOverlappendeParaplyvilkår(nytt, eksisterendeVilkår, vilkår)

        assertThatCode {
            valider(midlertidigBosted("Fra", "Til", fom = 16 februar 2026, tom = 28 februar 2026), vilkår = listOf(eksisterende))
            valider(midlertidigBosted("Fra", "Til"), vilkår = listOf(annenAktivitet, slettet))
            valider(midlertidigBosted("Fra", "Til", fom = 10 februar 2026, tom = 20 februar 2026), eksisterende, listOf(eksisterende))
        }.doesNotThrowAnyException()
    }

    private fun reiseVilkår(dato: LocalDate): Vilkår =
        vilkår(
            type = VilkårType.ROAH_OPPSTART_AVSLUTNING,
            fom = dato,
            tom = dato,
            fakta =
                FaktaReiseOppstartAvslutningHjemreiseUbestemt(
                    reiseId = ReiseId.random(),
                    aktivitetId = aktivitetId,
                    typeReiseformål = TypeReiseformål.AVSLUTNING,
                ),
        )

    private fun midlertidigBosted(
        fraAdresse: String,
        tilAdresse: String,
        fom: LocalDate = 1 februar 2026,
        tom: LocalDate = 28 februar 2026,
    ) = LagreVilkårMidlertidigBosted(
        fom = fom,
        tom = tom,
        aktivitetId = aktivitetId,
        svar = emptyMap(),
        fraAdresse = fraAdresse,
        tilAdresse = tilAdresse,
    )

    private fun valider(
        reise: LagreVilkårReiseOppstartAvslutningHjemreise,
        paraply: Vilkår?,
    ) = ROAHVilkårValideringUtil.validerTilhørendeParaplyvilkår(reise, paraply, vilkårId = null)

    private fun reise(
        fom: LocalDate,
        tom: LocalDate,
    ) = LagreVilkårReiseOppstartAvslutningHjemreise(
        fom = fom,
        tom = tom,
        reiseId = ReiseId.random(),
        aktivitetId = aktivitetId,
        typeReiseformål = TypeReiseformål.AVSLUTNING,
        svar = emptyMap(),
        fakta = FaktaUbestemtType,
    )

    private fun paraply(
        fom: LocalDate?,
        tom: LocalDate?,
        aktivitetId: VilkårperiodeGlobalId = this.aktivitetId,
    ): Vilkår =
        vilkår(
            type = VilkårType.ROAH_MIDLERTIDIG_BOSTED_PERIODE,
            fom = fom,
            tom = tom,
            fakta = FaktaReiseOppstartAvslutningHjemreiseMidlertidigBosted(aktivitetId, "Fra", "Til"),
        )
}
