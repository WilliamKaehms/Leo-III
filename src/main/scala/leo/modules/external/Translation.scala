package leo.modules.external

/**
  * TODO Create some Code
  * @param myParam Some test
  */
object Translation {
  
  def cleanExternalResult(externalResult: String): String = {
    val cleanedResult = new StringBuilder()
    val iter = Iterator(externalResult.split("\n"))
    while(iter.hasNext) {
      if(iter.next().contains("tff(")) {
        cleanedResult.append(iter.next()+"\n")
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

  private def annotationConversion(input0: String): String = {
    val test = "Example"
  }
}
