# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler - ulike år

  Scenario: En vedtaksperiode som løper innenfor en løpende måned men i 2 ulike år vil bli splittet opp
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 12.12.2024 | 11.01.2025 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.12.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.12.2024 | 31.03.2025 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 12.12.2024 | 31.12.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE       | 12.12.2024      |
      | 01.01.2025 | 11.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE       | 01.01.2025      |
