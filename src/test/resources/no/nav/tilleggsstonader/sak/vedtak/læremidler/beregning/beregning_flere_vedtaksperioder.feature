# language: no
# encoding: UTF-8

Egenskap: Beregning av læremidler - flere vedtaksperioder

  Scenario: Flere vedtaksperioder
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 01.04.2024 | 31.05.2024 |
      | 15.08.2024 | 30.09.2024 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2025 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 01.04.2024 | 30.04.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.04.2024      |
      | 01.05.2024 | 31.05.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 01.04.2024      |
      | 15.08.2024 | 14.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 15.08.2024      |
      | 15.09.2024 | 30.09.2024 | 438   | VIDEREGÅENDE | 100           | 438  | AAP       | 15.08.2024      |

  Scenario: Flere vedtaksperioder innenfor den samme løpende måneden
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 06.01.2025 | 06.01.2025 |
      | 05.02.2025 | 05.02.2025 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2025 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 06.01.2025 | 05.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 06.01.2025      |

  Scenario: Flere vedtaksperioder der vedtaksperiode 2 løper i den første og flere andre måneder
    Gitt følgende vedtaksperioder for læremidler
      | Fom        | Tom        |
      | 06.01.2025 | 06.01.2025 |
      | 05.02.2025 | 15.03.2025 |

    Gitt følgende aktiviteter for læremidler
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2024 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2024 | 31.03.2025 | AAP       | TILTAK    |


    Når beregner stønad for læremidler

    Så skal stønaden være
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 06.01.2025 | 05.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 06.01.2025      |
      # Utbetalingsdato for februar og mars får utbetalingsdato 6 feb fordi det er då den nye vedtaksperioden "begynner da"
      | 06.02.2025 | 05.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 06.02.2025      |
      | 06.03.2025 | 15.03.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 06.02.2025      |
