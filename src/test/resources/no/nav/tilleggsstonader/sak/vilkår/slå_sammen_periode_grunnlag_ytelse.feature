# language: no
# encoding: UTF-8

Egenskap: Slå sammen periode grunnlag ytelse

  Scenario: Ingen perioder som skal slås sammen

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 01.02.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |

    Når Slår sammen

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 01.02.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |

  Scenario: Slå sammen overlappende og sammenhengende perioder som skilles av annen aktivitet

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 15.01.2024 | AAP               |
      | 14.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 14.01.2024 | 19.01.2024 | AAP               |
      | 20.01.2024 | 01.02.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |

    Når Slår sammen

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 01.02.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |
      | 14.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |

  Scenario: Perioder med manglende tom

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 15.01.2024 | AAP               |
      | 14.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 14.01.2024 |            | AAP               |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |

    Når Slår sammen

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 15.01.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |
      | 14.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 14.01.2024 |            | AAP               |


  Scenario: Perioder med ulik enslig forsørger stønadstype

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              | Enslig forsørger stønadstype |
      | 01.01.2024 | 20.01.2024 | AAP               | OVERGANGSSTØNAD              |
      | 14.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |                              |
      | 20.01.2024 | 01.02.2024 | AAP               | SKOLEPENGER                  |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |                              |

    Når Slår sammen

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              | Enslig forsørger stønadstype |
      | 01.01.2024 | 20.01.2024 | AAP               | OVERGANGSSTØNAD              |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |                              |
      | 14.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |                              |
      | 20.01.2024 | 01.02.2024 | AAP               | SKOLEPENGER                  |



