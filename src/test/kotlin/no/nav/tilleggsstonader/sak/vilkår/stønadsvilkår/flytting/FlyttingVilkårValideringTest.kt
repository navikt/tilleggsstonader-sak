package no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting

import no.nav.tilleggsstonader.libs.feil.ApiFeil
import no.nav.tilleggsstonader.libs.utils.dato.januar
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttebyrå
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaFlyttingUbestemt
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FaktaKjøreSelv
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.FlyttebyråTilbud
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.domain.SvarOgBegrunnelse
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.flytting.domain.LagreVilkårFlytting
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.RegelId
import no.nav.tilleggsstonader.sak.vilkår.stønadsvilkår.regler.SvarId
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class FlyttingVilkårValideringTest {
    @Test
    fun `skal tillate ufullstendige tilbud ved validering av oppgitte fakta`() {
        assertDoesNotThrow {
            FlyttingVilkårValidering.validerOppgitteFakta(
                FaktaFlyttebyrå(
                    tilbud1 = FlyttebyråTilbud(navn = "Flyttebyrå A", pris = null),
                    tilbud2 = FlyttebyråTilbud(navn = null, pris = null),
                    adresse = "Flytteveien 1",
                ),
            )
        }
    }

    @Test
    fun `skal avvise like firmanavn selv med forskjellig mellomrom og store bokstaver`() {
        assertThrows<ApiFeil> {
            FlyttingVilkårValidering.validerOppgitteFakta(
                FaktaFlyttebyrå(
                    tilbud1 = FlyttebyråTilbud(navn = "Flyttebyrå A", pris = 10000),
                    tilbud2 = FlyttebyråTilbud(navn = " flyttebyrå a ", pris = 12000),
                    adresse = "Flytteveien 1",
                ),
            )
        }
    }

    @Test
    fun `skal avvise negativ kostnad for henger`() {
        assertThrows<ApiFeil> {
            FlyttingVilkårValidering.validerOppgitteFakta(
                FaktaKjøreSelv(
                    avstandEnVei = 250,
                    henger = -1,
                    bompenger = null,
                    ferge = null,
                    parkering = null,
                    adresse = "Flytteveien 1",
                ),
            )
        }
    }

    @Test
    fun `skal avvise tom flytteadresse`() {
        assertThrows<ApiFeil> {
            FlyttingVilkårValidering.validerOppgitteFakta(FaktaFlyttingUbestemt(adresse = " "))
        }
    }

    @Test
    fun `skal ikke være fullstendig uten svar`() {
        assertThat(
            FlyttingVilkårValidering.erFullstendig(
                LagreVilkårFlytting(
                    fom = 1 januar 2026,
                    tom = 31 januar 2026,
                    svar = emptyMap(),
                    fakta = FaktaFlyttingUbestemt(),
                ),
            ),
        ).isFalse()
    }

    @Test
    fun `skal være fullstendig når begge spørsmål er besvart med nei`() {
        assertThat(
            FlyttingVilkårValidering.erFullstendig(
                LagreVilkårFlytting(
                    fom = 1 januar 2026,
                    tom = 31 januar 2026,
                    svar =
                        mapOf(
                            RegelId.SKAL_BRUKE_FLYTTEBYRÅ to SvarOgBegrunnelse(SvarId.NEI),
                            RegelId.SKAL_KJØRE_SELV to SvarOgBegrunnelse(SvarId.NEI),
                        ),
                    fakta = FaktaFlyttingUbestemt(adresse = "Flytteveien 1"),
                ),
            ),
        ).isTrue()
    }

    @Test
    fun `skal ikke være fullstendig uten flytteadresse`() {
        assertThat(
            FlyttingVilkårValidering.erFullstendig(
                LagreVilkårFlytting(
                    fom = 1 januar 2026,
                    tom = 31 januar 2026,
                    svar =
                        mapOf(
                            RegelId.SKAL_BRUKE_FLYTTEBYRÅ to SvarOgBegrunnelse(SvarId.NEI),
                            RegelId.SKAL_KJØRE_SELV to SvarOgBegrunnelse(SvarId.NEI),
                        ),
                    fakta = FaktaFlyttingUbestemt(),
                ),
            ),
        ).isFalse()
    }
}
