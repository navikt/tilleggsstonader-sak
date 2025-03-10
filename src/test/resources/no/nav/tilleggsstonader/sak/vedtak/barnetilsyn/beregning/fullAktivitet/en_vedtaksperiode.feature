# language: no
# encoding: UTF-8

Egenskap: Beregning - En vedtaksperiode med full aktivitet

  Scenario: Vedtaksperiode og utgift for full måned:
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 31.01.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 31.01.2023 | UTDANNING | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 22           | 1000   | 650         |

  Scenario: Vedtaksperiode kun deler av måned

    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 02.01.2023 | 11.01.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2023 | 11.01.2023 | UTDANNING | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedbeløp |
      | 01.2023 | 29.53   | 8            | 1000   | 236        |

    Så forvent følgende vedtaksperioder for: 01.2023
      | Fom        | Tom        | Målgruppe       | Aktivitet |
      | 02.01.2023 | 11.01.2023 | OVERGANGSSTØNAD | UTDANNING |

  Scenario: Vedtaksperiode og utgift over flere måneder - start og slutt midt i måned
    # Mål: Beregning skal dele opp vedtaksperiode som går over flere måneder uten å endre fom og tom

    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Målgruppe       | Aktivitet |
      | 10.04.2023 | 26.05.2023 | OVERGANGSSTØNAD | UTDANNING |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 10.04.2023 | 26.05.2023 | UTDANNING | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 06.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 04.2023 | 29.53   | 15           | 1000   | 443         |
      | 05.2023 | 29.53   | 20           | 1000   | 591         |

    Så forvent følgende vedtaksperioder for: 04.2023
      | Fom        | Tom        | Målgruppe       | Aktivitet |
      | 10.04.2023 | 30.04.2023 | OVERGANGSSTØNAD | UTDANNING |

    Så forvent følgende vedtaksperioder for: 05.2023
      | Fom        | Tom        | Målgruppe       | Aktivitet |
      | 01.05.2023 | 26.05.2023 | OVERGANGSSTØNAD | UTDANNING |