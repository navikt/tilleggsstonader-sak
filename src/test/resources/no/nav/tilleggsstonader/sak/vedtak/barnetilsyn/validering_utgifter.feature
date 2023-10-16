# language: no
# encoding: UTF-8

Egenskap: Beregning barnetilsyn - validering av utgifter

  Scenario: Sender inn tomme utgifter

    Gitt følgende støndsperioder
      | Fom     | Tom     |
      | 01.2023 | 02.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom | Tom | Utgift |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder mangler

  Scenario: Sender inn usorterte utgifter

    Gitt følgende støndsperioder
      | Fom     | Tom     |
      | 01.2023 | 02.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom     | Tom     | Utgift |
      | 02.2023 | 02.2023 | 100    |
      | 01.2023 | 01.2023 | 100    |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder er ikke sortert

  Scenario: Sender inn overlappende utgiftsperioder

    Gitt følgende støndsperioder
      | Fom     | Tom     |
      | 01.2023 | 03.2023 |

    Gitt følgende utgifter for barn: 1
      | Fom     | Tom     | Utgift |
      | 01.2023 | 02.2023 | 100    |
      | 02.2023 | 01.2023 | 100    |

    Når beregner

    Så forvent følgende feil: Utgiftsperioder overlapper