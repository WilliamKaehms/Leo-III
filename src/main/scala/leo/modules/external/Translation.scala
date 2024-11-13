package leo.modules.external

import leo.modules.input

/**
  * TODO Create some Code
  * @param myParam Some test
  */
object Translation {

  def translateToTHF(externalResult: String): String = {
    var res = cleanExternalResult(externalResult.split("\n"))
    res = languageToTHF(res)
    res.mkString("\n")
  } 
  
  private def cleanExternalResult(externalResult: Seq[String]): Seq[String] = {
    if (!externalResult.isEmpty) {
      val line = externalResult(0)
      val externalResult0 = externalResult.drop(1)
      if (line.contains("tff(") || line.contains("tcf(")) {
        val res = Seq[String]("thf" + line.substring(3,line.length()))
        res ++ cleanExternalResult(externalResult0)
      } else {
        cleanExternalResult(externalResult0)
      }
    } else {
      externalResult
    }
  }
}

  /*
    object Role {
    def apply(role: String): Role = role.trim match {
      case "axiom" => Role_Axiom
      case "hypothesis" => Role_Axiom // Note: Implicit mapping to axiom
      case "definition" => Role_Definition
      case "assumption" => Role_Assumption
      case "lemma" => Role_Axiom // Note: Implicit mapping to axiom
      case "theorem" => Role_Axiom // Note: Implicit mapping to axiom
      case "conjecture" => Role_Conjecture
      case "negated_conjecture" => Role_NegConjecture
      case "plain" => Role_Plain
      case "type" => Role_Type
      case "unknown" => Role_Unknown
      case _ => Role_Unknown // Note: fi_* roles are not handled at the moment
    }
  }
  */

/*
  private def annotationConversion(input0: Seq[TPTP.AnnotatedFormula]): Seq[TPTP.AnnotatedFormula] = {
    val test: String = "Example"
  }
  */

