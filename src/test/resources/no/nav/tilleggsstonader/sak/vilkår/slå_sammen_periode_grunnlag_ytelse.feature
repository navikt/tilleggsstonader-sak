# language: no
# encoding: UTF-8

Egenskap: Slå sammen periode grunnlag ytelse

  Scenario: Ingen perioder som skal slås sammen

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 01.02.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |

    Når Slår sammen grunnlagsperioder

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 01.02.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |


  Scenario: Slå sammen påfølgende perioder av alle typer

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 01.02.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |
      | 02.02.2024 | 01.03.2024 | AAP               |
      | 02.02.2024 | 01.03.2024 | ENSLIG_FORSØRGER  |
      | 02.02.2024 | 01.03.2024 | OMSTILLINGSSTØNAD |

    Når Slår sammen grunnlagsperioder

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 01.03.2024 | AAP               |
      | 01.01.2024 | 01.03.2024 | ENSLIG_FORSØRGER  |
      | 01.01.2024 | 01.03.2024 | OMSTILLINGSSTØNAD |

  Scenario: Slå sammen overlappende perioder av alle typer

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 01.02.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |
      | 01.02.2024 | 01.03.2024 | AAP               |
      | 01.02.2024 | 01.03.2024 | ENSLIG_FORSØRGER  |
      | 01.02.2024 | 01.03.2024 | OMSTILLINGSSTØNAD |

    Når Slår sammen grunnlagsperioder

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 01.03.2024 | AAP               |
      | 01.01.2024 | 01.03.2024 | ENSLIG_FORSØRGER  |
      | 01.01.2024 | 01.03.2024 | OMSTILLINGSSTØNAD |


  Scenario: Slå sammen overlappende og sammenhengende perioder som skilles av annen ytelse

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 15.01.2024 | AAP               |
      | 14.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 14.01.2024 | 19.01.2024 | AAP               |
      | 20.01.2024 | 01.02.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |

    Når Slår sammen grunnlagsperioder

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

    Når Slår sammen grunnlagsperioder

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type              |
      | 01.01.2024 | 15.01.2024 | AAP               |
      | 01.01.2024 | 01.02.2024 | OMSTILLINGSSTØNAD |
      | 14.01.2024 | 01.02.2024 | ENSLIG_FORSØRGER  |
      | 14.01.2024 |            | AAP               |


  Scenario: Perioder med lik enslig forsørger stønadstype skal slåes sammen

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type             | Subtype         |
      | 01.01.2024 | 09.01.2024 | ENSLIG_FORSØRGER | OVERGANGSSTØNAD |
      | 10.01.2024 | 19.01.2024 | ENSLIG_FORSØRGER | OVERGANGSSTØNAD |

    Når Slår sammen grunnlagsperioder

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type             | Subtype         |
      | 01.01.2024 | 19.01.2024 | ENSLIG_FORSØRGER | OVERGANGSSTØNAD |

  Scenario: Perioder med ulik enslig forsørger stønadstype skal ikke slåes sammen

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type             | Subtype         |
      | 01.01.2024 | 09.01.2024 | ENSLIG_FORSØRGER | OVERGANGSSTØNAD |
      | 10.01.2024 | 19.01.2024 | ENSLIG_FORSØRGER | SKOLEPENGER     |

    Når Slår sammen grunnlagsperioder

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type             | Subtype         |
      | 01.01.2024 | 09.01.2024 | ENSLIG_FORSØRGER | OVERGANGSSTØNAD |
      | 10.01.2024 | 19.01.2024 | ENSLIG_FORSØRGER | SKOLEPENGER     |

  Scenario: AAP skal slås sammen med AAP perioder som er påfølgende

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type | Subtype |
      | 01.01.2024 | 08.01.2024 | AAP  |         |
      | 09.01.2024 | 31.01.2024 | AAP  |         |

    Når Slår sammen grunnlagsperioder

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type | Subtype |
      | 01.01.2024 | 31.01.2024 | AAP  |         |

  Scenario: AAP skal ikke slås sammen med AAP-ferdigavklarte perioder som er påfølgende

    Gitt Følgende grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type | Subtype            |
      | 01.01.2024 | 08.01.2024 | AAP  |                    |
      | 09.01.2024 | 31.01.2024 | AAP  | AAP_FERDIG_AVKLART |

    Når Slår sammen grunnlagsperioder

    Så Forvent grunnlagsperioderfor ytelse
      | Fom        | Tom        | Type | Subtype            |
      | 01.01.2024 | 08.01.2024 | AAP  |                    |
      | 09.01.2024 | 31.01.2024 | AAP  | AAP_FERDIG_AVKLART |



