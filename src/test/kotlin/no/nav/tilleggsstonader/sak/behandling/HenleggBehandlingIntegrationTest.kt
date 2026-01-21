package no.nav.tilleggsstonader.sak.behandling

import no.nav.tilleggsstonader.kontrakter.felles.Stønadstype
import no.nav.tilleggsstonader.sak.CleanDatabaseIntegrationTest
import no.nav.tilleggsstonader.sak.behandling.domain.HenlagtÅrsak
import no.nav.tilleggsstonader.sak.behandling.dto.HenlagtDto
import no.nav.tilleggsstonader.sak.behandlingsflyt.StegType
import no.nav.tilleggsstonader.sak.integrasjonstest.opprettBehandlingOgGjennomførBehandlingsløp
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.StatusIverksetting
import no.nav.tilleggsstonader.sak.utbetaling.tilkjentytelse.domain.TilkjentYtelseRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class HenleggBehandlingIntegrationTest : CleanDatabaseIntegrationTest() {
    @Autowired
    lateinit var tilkjentYtelseRepository: TilkjentYtelseRepository

    @Test
    fun `oppretter og beregner behandling og får andeler som venter på satsendring, henlegger, andeler blir ugyldig`() {
        // 2 års-periode for at man skal få andeler som venter på satsendring
        val fom = LocalDate.now()
        val tom = LocalDate.now().plusYears(2)
        val behandlingId =
            opprettBehandlingOgGjennomførBehandlingsløp(
                stønadstype = Stønadstype.LÆREMIDLER,
                tilSteg = StegType.SEND_TIL_BESLUTTER,
            ) {
                aktivitet {
                    opprett {
                        aktivitetUtdanningLæremidler(fom, tom)
                    }
                }
                målgruppe {
                    opprett {
                        målgruppeAAP(fom, tom)
                    }
                }
            }

        val tilkjentYtelse = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        assertThat(
            tilkjentYtelse!!.andelerTilkjentYtelse.filter { it.statusIverksetting == StatusIverksetting.VENTER_PÅ_SATS_ENDRING },
        ).isNotEmpty

        kall.behandling.henlegg(behandlingId, HenlagtDto(årsak = HenlagtÅrsak.TRUKKET_TILBAKE))

        val tilkjentYtelseEtterHenleggelse = tilkjentYtelseRepository.findByBehandlingId(behandlingId)
        assertThat(
            tilkjentYtelseEtterHenleggelse!!.andelerTilkjentYtelse.filter {
                it.statusIverksetting ==
                    StatusIverksetting.VENTER_PÅ_SATS_ENDRING
            },
        ).isEmpty()
    }
}
