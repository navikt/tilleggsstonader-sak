# language: no
# encoding: UTF-8

Egenskap: Beregning - Håndtering av helgdager

  Scenario: Håndtering av periode som starter en søndag
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.01.2023 | 31.01.2023 | UTDANNING | NEDSATT_ARBEIDSEVNE |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 31.01.2023 | UTDANNING | 5               |

    Gitt følgende målgrupper
      | Fom        | Tom        | Målgruppe |
      | 01.01.2023 | 31.01.2023 | AAP       |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 01.2023 | 29.53   | 22           | 1000   | 650         |

    Så forvent følgende vedtaksperiodeGrunnlag for: 01.2023
      | Fom        | Tom        | Målgruppe           | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.01.2023 | 31.01.2023 | NEDSATT_ARBEIDSEVNE | UTDANNING | 1                  | 22           |

    Så forvent følgende beløpsperioder for: 01.2023
      | Dato       | Beløp | Målgruppe           |
      # 01.01.2023 er en søndag, lager en beløpsperiode som er
      | 02.01.2023 | 650   | NEDSATT_ARBEIDSEVNE |

  Scenario: Håndtering av periode som starter en lørdag
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe           |
      | 01.04.2023 | 30.04.2023 | UTDANNING | NEDSATT_ARBEIDSEVNE |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.04.2023 | 30.04.2023 | UTDANNING | 5               |

    Gitt følgende målgrupper
      | Fom        | Tom        | Målgruppe |
      | 01.01.2023 | 31.01.2023 | AAP       |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 04.2023 | 04.2023 | 1000   |

    Når beregner

    Så forvent følgende beregningsresultat
      | Måned   | Dagsats | Antall dager | Utgift | Månedsbeløp |
      | 04.2023 | 29.53   | 20           | 1000   | 591         |

    Så forvent følgende vedtaksperiodeGrunnlag for: 04.2023
      | Fom        | Tom        | Målgruppe           | Aktivitet | Antall aktiviteter | Antall dager |
      | 01.04.2023 | 30.04.2023 | NEDSATT_ARBEIDSEVNE | UTDANNING | 1                  | 20           |

    Så forvent følgende beløpsperioder for: 04.2023
      | Dato       | Beløp | Målgruppe           |
      # 01.04.2023 er en lørdag, lager en beløpsperiode som er
      | 03.04.2023 | 591   | NEDSATT_ARBEIDSEVNE |
