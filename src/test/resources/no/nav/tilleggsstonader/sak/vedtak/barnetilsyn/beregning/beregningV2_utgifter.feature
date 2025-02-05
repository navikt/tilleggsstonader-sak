# language: no
# encoding: UTF-8

Egenskap: Beregning - Utgifter

  Scenario: Stønadsperiode skal begrenses av perioder med utgifter:
    # Mål: Beregningen skal ikke gi resultat for februar fordi det ikke er utgifter i denne perioden

    Gitt V2 - følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 28.02.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt V2 - følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 28.02.2023 | UTDANNING | 5               |

    Gitt V2 - følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når V2 - beregner

    Så V2 - forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 22           | 1000   | 650         |
