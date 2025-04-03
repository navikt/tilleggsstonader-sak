# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler - ulike år

  Scenario: En vedtaksperiode som løper innenfor en løpende måned men i 2 ulike år vil bli splittet opp
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 12.12.2024 | 11.01.2025 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.12.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 12.12.2024 | 11.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 12.12.2024 | 31.12.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 12.12.2024      |
      | 01.01.2025 | 11.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |
