# language: no
# encoding: UTF-8

Egenskap: Beregning - Komplisert scenario

  Scenario: Flere vedtaksperioder og aktiviteter over lenger periode
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Målgruppe           | Aktivitet |
      | 01.01.2023 | 15.02.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 16.02.2023 | 28.02.2023 | NEDSATT_ARBEIDSEVNE | UTDANNING |
      | 01.03.2023 | 30.04.2023 | ENSLIG_FORSØRGER    | UTDANNING |
      | 15.05.2023 | 04.06.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    |
      | 12.06.2023 | 30.06.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 30.06.2023 | UTDANNING | 5               |
      | 01.01.2023 | 15.02.2023 | TILTAK    | 3               |
      | 15.05.2023 | 04.06.2023 | TILTAK    | 2               |
      | 12.06.2023 | 30.06.2023 | TILTAK    | 3               |
      | 19.06.2023 | 30.06.2023 | TILTAK    | 2               |

    Gitt følgende målgrupper
      | Fom        | Tom        | Målgruppe       |
      | 01.01.2023 | 15.02.2023 | AAP             |
      | 16.02.2023 | 28.02.2023 | AAP             |
      | 01.03.2023 | 30.04.2023 | OVERGANGSSTØNAD |
      | 15.05.2023 | 04.06.2023 | AAP             |
      | 12.06.2023 | 30.06.2023 | AAP             |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 06.2023 | 1000   |

    Gitt følgende utgifter for barn med id: 2
      | Fom     | Tom     | Utgift |
      | 04.2023 | 06.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 14           | 1000   | 413         |
      | 02.2023 | 29.53   | 18           | 1000   | 532         |
      | 03.2023 | 29.53   | 23           | 1000   | 679         |
      | 04.2023 | 59.07   | 20           | 2000   | 1181        |
      | 05.2023 | 59.07   | 6            | 2000   | 354         |
      | 06.2023 | 59.07   | 15           | 2000   | 886         |

    Så forvent følgende vedtaksperiodeGrunnlag for: 01.2023
      | Fom        | Tom        | Målgruppe           | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.01.2023 | 31.01.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    | 1                  | 14           |

    Så forvent følgende beløpsperioder for: 01.2023
      | Dato       | Beløp | Målgruppe           |
      | 02.01.2023 | 413   | NEDSATT_ARBEIDSEVNE |

    Så forvent følgende vedtaksperiodeGrunnlag for: 02.2023
      | Fom        | Tom        | Målgruppe           | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.02.2023 | 15.02.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    | 1                  | 9            |
      | 16.02.2023 | 28.02.2023 | NEDSATT_ARBEIDSEVNE | UTDANNING | 1                  | 9            |

    Så forvent følgende beløpsperioder for: 02.2023
      | Dato       | Beløp | Målgruppe           |
      | 01.02.2023 | 266   | NEDSATT_ARBEIDSEVNE |
      | 16.02.2023 | 266   | NEDSATT_ARBEIDSEVNE |

    Så forvent følgende vedtaksperiodeGrunnlag for: 03.2023
      | Fom        | Tom        | Målgruppe        | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.03.2023 | 31.03.2023 | ENSLIG_FORSØRGER | UTDANNING | 1                  | 23           |

    Så forvent følgende beløpsperioder for: 03.2023
      | Dato       | Beløp | Målgruppe        |
      | 01.03.2023 | 679   | ENSLIG_FORSØRGER |

    Så forvent følgende vedtaksperiodeGrunnlag for: 04.2023
      | Fom        | Tom        | Målgruppe        | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.04.2023 | 30.04.2023 | ENSLIG_FORSØRGER | UTDANNING | 1                  | 20           |

    Så forvent følgende beløpsperioder for: 04.2023
      | Dato       | Beløp | Målgruppe        |
      | 03.04.2023 | 1181  | ENSLIG_FORSØRGER |

    Så forvent følgende vedtaksperiodeGrunnlag for: 05.2023
      | Fom        | Tom        | Målgruppe           | Aktivitet | Antall aktiviteter | Antall dager |
      | 15.05.2023 | 31.05.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    | 1                  | 6            |

    Så forvent følgende beløpsperioder for: 05.2023
      | Dato       | Beløp | Målgruppe           |
      | 15.05.2023 | 354   | NEDSATT_ARBEIDSEVNE |

    Så forvent følgende vedtaksperiodeGrunnlag for: 06.2023
      | Fom        | Tom        | Målgruppe           | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.06.2023 | 04.06.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    | 1                  | 2            |
      | 12.06.2023 | 30.06.2023 | NEDSATT_ARBEIDSEVNE | TILTAK    | 2                  | 13           |

    Så forvent følgende beløpsperioder for: 06.2023
      | Dato       | Beløp | Målgruppe           |
      | 01.06.2023 | 118   | NEDSATT_ARBEIDSEVNE |
      | 12.06.2023 | 768   | NEDSATT_ARBEIDSEVNE |
