# language: no
# encoding: UTF-8

Egenskap: Beregning barnetilsyn - validering av stønadsperioder

  Scenario: Sender inn tomme stønadsperioder

    Gitt følgende støndsperioder
      | Fom | Tom |

    Når beregner

    Så forvent følgende feil: Stønadsperioder mangler

  Scenario: Sender inn usorterte stønadsperioder

    Gitt følgende støndsperioder
      | Fom     | Tom     |
      | 02.2023 | 02.2023 |
      | 01.2023 | 01.2023 |

    Når beregner

    Så forvent følgende feil: Stønadsperioder er ikke sortert

  Scenario: Sender inn overlappende stønadsperioder

    Gitt følgende støndsperioder
      | Fom     | Tom     |
      | 01.2023 | 03.2023 |
      | 02.2023 | 02.2023 |

    Når beregner

    Så forvent følgende feil: Stønadsperioder overlapper