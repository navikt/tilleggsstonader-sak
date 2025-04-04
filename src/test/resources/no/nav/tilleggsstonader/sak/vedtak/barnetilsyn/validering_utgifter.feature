# language: no
# encoding: UTF-8

Egenskap: Beregning barnetilsyn - validering av utgifter

  Scenario: Sender inn tomme utgifter

    Gitt følgende vedtaksperioder
      | Fom     | Tom     | Målgruppe | Aktivitet |
      | 01.2023 | 02.2023 | AAP       | TILTAK    |

    Gitt følgende aktiviteter
      | Fom     | Tom     | Aktivitet | Aktivitetsdager |
      | 01.2023 | 02.2023 | TILTAK    | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom | Tom | Utgift |

    Når beregner

    Så forvent følgende feil: Kan ikke innvilge når det ikke finnes utgifter hele vedtaksperioden

  Scenario: Sender inn overlappende utgiftsperioder

    Gitt følgende vedtaksperioder
      | Fom     | Tom     | Målgruppe | Aktivitet |
      | 01.2023 | 03.2023 | AAP       | TILTAK    |

    Gitt følgende aktiviteter
      | Fom     | Tom     | Aktivitet | Aktivitetsdager |
      | 01.2023 | 03.2023 | TILTAK    | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 02.2023 | 100    |
      | 02.2023 | 03.2023 | 100    |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder overlapper

  Scenario: Sender inn negativ utgift

    Gitt følgende vedtaksperioder
      | Fom     | Tom     | Målgruppe | Aktivitet |
      | 01.2023 | 03.2023 | AAP       | TILTAK    |

    Gitt følgende aktiviteter
      | Fom     | Tom     | Aktivitet | Aktivitetsdager |
      | 01.2023 | 03.2023 | TILTAK    | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 03.2023 | -100   |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder inneholder ugyldig utgift: -100