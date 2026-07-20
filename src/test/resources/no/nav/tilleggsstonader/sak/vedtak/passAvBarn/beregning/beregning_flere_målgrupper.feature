# language: no
# encoding: UTF-8

Egenskap: Beregning - Flere målgrupper

  Scenario: En vedtaksperiode inneholdendes flere målgrupper av samme type faktisk målgruppe
    # Mål: Beregningen skal ikke gi resultat for februar fordi det ikke er utgifter i denne perioden

    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2023 | 31.01.2023 | TILTAK    | NEDSATT_ARBEIDSEVNE |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 31.01.2023 | TILTAK    | 5               |

    Gitt følgende målgrupper
      | Fom        | Tom        | Målgruppe           |
      | 01.01.2023 | 10.01.2023 | AAP                 |
      | 11.01.2023 | 20.01.2023 | NEDSATT_ARBEIDSEVNE |
      | 21.01.2023 | 31.01.2023 | UFØRETRYGD          |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 22           | 1000   | 650         |