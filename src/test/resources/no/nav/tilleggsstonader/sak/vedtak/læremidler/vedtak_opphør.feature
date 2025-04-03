# language: no
# encoding: UTF-8

Egenskap: Opphør av læremidler

  Scenario: Skal avkorte tidligere perioder
    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 31.03.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.03.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 31.03.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 1353  | LÆREMIDLER_AAP | 01.01.2025      |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når opphør behandling=2 med revurderFra=15.02.2025

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |
      | 01.02.2025 | 14.02.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 902   | LÆREMIDLER_AAP | 01.01.2025      |

    Så forvent vedtaksperioder for behandling=2
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 14.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: Flere aktiviteter - 50 og 100% som tidligere blev 100% - må reberegne siste måned og blir nå 50%
    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 14.01.2025 | TILTAK    | VIDEREGÅENDE | 50            |
      | 15.01.2025 | 31.01.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 31.01.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        |
      | 01.01.2025 | 31.01.2025 |

    Så forvent andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 451   | LÆREMIDLER_AAP | 01.01.2025      |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når opphør behandling=2 med revurderFra=15.01.2025

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2025 | 14.01.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 226   | LÆREMIDLER_AAP | 01.01.2025      |

    Så forvent vedtaksperioder for behandling=2
      | Fom        | Tom        |
      | 01.01.2025 | 14.01.2025 |

  Scenario: Flere målgrupper - Overgangsstønad og AAP som tidligere ble AAP - må reberegne siste måned blir nå Overgangsstønad
    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 31.01.2025 | UTDANNING | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe       |
      | 01.01.2025 | 14.01.2025 | OVERGANGSSTØNAD |
      | 15.01.2025 | 31.01.2025 | AAP             |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 14.01.2025 | ENSLIG_FORSØRGER    | UTDANNING |
      | 15.01.2025 | 31.01.2025 | NEDSATT_ARBEIDSEVNE | UTDANNING |

    Så forvent andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 451   | LÆREMIDLER_AAP | 01.01.2025      |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når opphør behandling=2 med revurderFra=10.01.2025

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe        | Aktivitet | Utbetalingsdato |
      | 01.01.2025 | 09.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | ENSLIG_FORSØRGER | UTDANNING | 01.01.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type                        | Utbetalingsdato |
      | 01.01.2025 | 451   | LÆREMIDLER_ENSLIG_FORSØRGER | 01.01.2025      |

    Så forvent vedtaksperioder for behandling=2
      | Fom        | Tom        | Målgruppe        | Aktivitet |
      | 01.01.2025 | 09.01.2025 | ENSLIG_FORSØRGER | UTDANNING |

  Scenario: Flere aktiviteter - 50 og 100% som tidligere blev 100% - må reberegne siste måned og beholde utbetalingsdato
    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 14.02.2025 | TILTAK    | VIDEREGÅENDE | 50            |
      | 15.02.2025 | 28.02.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 28.02.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 28.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 677   | LÆREMIDLER_AAP | 01.01.2025      |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når opphør behandling=2 med revurderFra=15.02.2025

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |
      | 01.02.2025 | 14.02.2025 | 226   | VIDEREGÅENDE | 50            | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 452   | LÆREMIDLER_AAP | 01.01.2025      |

    Så forvent vedtaksperioder for behandling=2
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 14.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

  Scenario: Opphør sånn at siste løpende måneden kun er over en helg skal ikke ta med den måneden
    Gitt følgende aktiviteter for læremidler behandling=1
      | Fom        | Tom        | Aktivitet | Studienivå   | Studieprosent |
      | 01.01.2025 | 14.02.2025 | TILTAK    | VIDEREGÅENDE | 100           |

    Gitt følgende målgrupper for læremidler behandling=1
      | Fom        | Tom        | Målgruppe |
      | 01.01.2025 | 14.02.2025 | AAP       |

    Når innvilger vedtaksperioder for behandling=1
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2025 | 14.02.2025 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Så forvent andeler for behandling=1
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 902   | LÆREMIDLER_AAP | 01.01.2025      |

    Når kopierer perioder fra forrige behandling for behandling=2

    Når opphør behandling=2 med revurderFra=03.02.2025

    Så forvent beregningsresultatet for behandling=2
      | Fom        | Tom        | Beløp | Studienivå   | Studieprosent | Sats | Målgruppe           | Utbetalingsdato |
      | 01.01.2025 | 31.01.2025 | 451   | VIDEREGÅENDE | 100           | 451  | NEDSATT_ARBEIDSEVNE | 01.01.2025      |

    Så forvent andeler for behandling=2
      | Fom        | Beløp | Type           | Utbetalingsdato |
      | 01.01.2025 | 451   | LÆREMIDLER_AAP | 01.01.2025      |

    Så forvent vedtaksperioder for behandling=2
      | Fom        | Tom        |
      | 01.01.2025 | 02.02.2025 |
