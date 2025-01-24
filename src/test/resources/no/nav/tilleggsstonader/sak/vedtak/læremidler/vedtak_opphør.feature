# language: no
# encoding: UTF-8

Egenskap: Opphør av læremidler

  Scenario: Skal avkorte tidligere perioder
    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende stønadsperioder for læremidler behandling=1
      | Fom        | Tom        | Målgruppe | Aktivitet |
      | 01.01.2025 | 31.03.2025 | AAP       | TILTAK    |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 01.01.2025 | 31.03.2025 |

    Så forvent andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1353  | LÆREMIDLER_AAP | 01.01.2025      |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når opphør behandling=2 med revurderFra=15.02.2025

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 01.01.2025      |
      | 01.02.2025 | 14.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | AAP       | 01.01.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 902   | LÆREMIDLER_AAP | 01.01.2025      |

    Så forvent vedtaksperioder for behandling=2
      | Fom        | Tom        |
      | 01.01.2025 | 14.02.2025 |