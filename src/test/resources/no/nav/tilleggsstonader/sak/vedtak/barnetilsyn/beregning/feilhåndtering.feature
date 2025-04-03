# language: no
# encoding: UTF-8

Egenskap: Feilhåndtering i beregning
  # Tester feilhåndtering i beregning
  # Feilene som kastes her bør aldri ses av bruker fordi det er states
  # som valideringen av vedtaksperioder skal fange opp

  Scenario: Ingen aktiviteter i samme periode som vedtaksperiode
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 31.01.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.02.2023 | 28.02.2023 | UTDANNING | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende feil: Ingen aktiviteter for måned

  Scenario: Ingen aktiviteter i deler en av månedene strekker seg over
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 01.01.2023 | 28.02.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 01.01.2023 | 31.01.2023 | UTDANNING | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 02.2023 | 1000   |

    Når beregner

    Så forvent følgende feil: Ingen aktiviteter for måned 2023-02

  Scenario: Ingen aktivitet av riktig type
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 02.01.2023 | 15.01.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2023 | 08.01.2023 | TILTAK    | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende feil: Finnes ingen aktiviteter av type UTDANNING som passer med vedtaksperiode

  Scenario: Ingen aktiviteter i en uke vedtaksperiode strekker seg over
    Gitt følgende vedtaksperioder
      | Fom        | Tom        | Aktivitet | Målgruppe       |
      | 02.01.2023 | 15.01.2023 | UTDANNING | OVERGANGSSTØNAD |

    Gitt følgende aktiviteter
      | Fom        | Tom        | Aktivitet | Aktivitetsdager |
      | 02.01.2023 | 08.01.2023 | UTDANNING | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 01.2023 | 1000   |

    Når beregner

    Så forvent følgende feil: Ingen aktivitet i uke fom=2023-01-09