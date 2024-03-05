# language: no
# encoding: UTF-8

Egenskap: Beregning barnetilsyn - validering av utgifter

  Scenario: Sender inn tomme utgifter

    Gitt følgende støndsperioder
      | Fom     | Tom     | Målgruppe | Aktivitet |
      | 01.2023 | 02.2023 | AAP       | TILTAK    |

    Gitt følgende aktiviteter
      | Fom     | Tom     | Aktivitet | Aktivitetsdager |
      | 01.2023 | 02.2023 | TILTAK    | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom | Tom | Utgift |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder mangler

  Scenario: Sender inn usorterte utgifter

    Gitt følgende støndsperioder
      | Fom     | Tom     | Målgruppe | Aktivitet |
      | 01.2023 | 02.2023 | AAP       | TILTAK    |

    Gitt følgende aktiviteter
      | Fom     | Tom     | Aktivitet | Aktivitetsdager |
      | 01.2023 | 02.2023 | TILTAK    | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 02.2023 | 02.2023 | 100    |
      | 01.2023 | 01.2023 | 100    |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder er ikke sortert

  Scenario: Sender inn overlappende utgiftsperioder

    Gitt følgende støndsperioder
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

  Scenario: Sender inn 0 som utgift

    Gitt følgende støndsperioder
      | Fom     | Tom     | Målgruppe | Aktivitet |
      | 01.2023 | 03.2023 | AAP       | TILTAK    |

    Gitt følgende aktiviteter
      | Fom     | Tom     | Aktivitet | Aktivitetsdager |
      | 01.2023 | 03.2023 | TILTAK    | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 02.2023 | 0      |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder inneholder ugyldig verdi: 0

  Scenario: Sender inn negativ utgift

    Gitt følgende støndsperioder
      | Fom     | Tom     | Målgruppe | Aktivitet |
      | 01.2023 | 03.2023 | AAP       | TILTAK    |

    Gitt følgende aktiviteter
      | Fom     | Tom     | Aktivitet | Aktivitetsdager |
      | 01.2023 | 03.2023 | TILTAK    | 5               |

    Gitt følgende utgifter for barn med id: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 02.2023 | -100   |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder inneholder ugyldig verdi: -100