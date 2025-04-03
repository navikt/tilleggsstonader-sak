# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler - flere vedtaksperioder v2

  Scenario: Flere vedtaksperioder
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 01.04.2024 | 31.05.2024 | AAP       |
      | 15.08.2024 | 30.09.2024 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.04.2024 | 31.05.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 15.08.2024 | 30.09.2024 | NEDSATT_ARBEIDSEVNE | TILTAK    |


    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.04.2024      |
      | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 01.04.2024      |
      | 15.08.2024 | 14.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |
      | 15.09.2024 | 30.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | NEDSATT_ARBEIDSEVNE | 15.08.2024      |

  Scenario: Flere vedtaksperioder innenfor den samme løpende måneden
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 06.01.2025 | 31.03.2025 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 06.01.2025 | 06.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 05.02.2025 | 05.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 06.01.2025 | 05.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 06.01.2025      |

  Scenario: Flere vedtaksperioder der vedtaksperiode 2 løper i den første og flere andre måneder
    Gitt følgende målgrupper for læremidler
      | Fom        | Tom        | Målgruppe |
      | 06.01.2025 | 15.03.2025 | AAP       |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 06.01.2025 | 06.01.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 05.02.2025 | 15.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |


    Når beregner stønad for læremidler uten overlappsperiode

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 06.01.2025 | 05.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 06.01.2025      |
      # Utbetalingsdato for februar og mars får utbetalingsdato 6 feb fordi det er då den nye vedtaksperioden "begynner da"
      | 06.02.2025 | 05.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 06.02.2025      |
      | 06.03.2025 | 15.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 06.02.2025      |
