# language: no
# encoding: UTF-8

Egenskap: Beregning - En aktivitet med delvisaktivitet

  Scenario: Tre fulle uker med delvisaktivitet
    # Mål: Antall dager skal bli aktivitetsdager x antall uker

    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 02.01.2023 | 21.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2023 | 21.01.2023 | TILTAK    | 3               |

    Gitt følgende målgrupper
      | Fom        | Tom        | Målgruppe |
      | 02.01.2023 | 31.05.2023 | AAP       |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 9            | 1000   | 266         |

  Scenario: Færre dager i vedtaksperiode enn antall aktivitetsdager:
      # Mål: Antall dager skal ikke bli flere enn det er plass til i en uke
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 02.01.2023 | 04.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2023 | 04.01.2023 | TILTAK    | 4               |

    Gitt følgende målgrupper
      | Fom        | Tom        | Målgruppe |
      | 02.01.2023 | 31.05.2023 | AAP       |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 3            | 1000   | 89          |