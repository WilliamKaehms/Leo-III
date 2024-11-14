package leo.modules.external

import leo.modules.input
import leo.datastructures.{Role, Signature, Term, TPTP}
import javax.management.relation.Role

/**
  * TODO Create some Code
  * @param myParam Some test
  */
object Translation {

  final def apply(externalResult: String): String = {
    val res = cleanExternalResult(externalResult.split("\n"))
    res.mkString("\n")
  }

  def translateToTHF(externalResult: String): String = {
    val res = cleanExternalResult(externalResult.split("\n"))
    res.mkString("\n")
  } 
  /*
  private def cleanExternalResult(externalResult: Seq[String]): Seq[String] = {
    if (!externalResult.isEmpty) {
      val line = externalResult(0)
      val externalResult0 = externalResult.drop(1)
      if (line.contains("tff(") || line.contains("tcf(")) {
        val res = Seq[String](s"thf${line.substring(3,line.length())}")
        res ++ cleanExternalResult(externalResult0)
      } else {
        cleanExternalResult(externalResult0)
      }
    } else {
      externalResult
    }
  }
  */
  private val cleanExternalResult = (externalResult: Seq[String]) => for {
    line0 <- externalResult.filter(line => line.contains("tff(") || line.contains("tcf("))
  } yield s"thf${line0.substring(3,line0.length())}"

  private def convertResultToTPTP(externalResult: Seq[String]) : Seq[TPTP.AnnotatedFormula] = {
    import leo.modules.encoding.TypedFOLEncodingSignature
    var formulas: Seq[TPTP.AnnotatedFormula] = Seq.empty
    val externalIter = externalResult.iterator
    while(externalIter.hasNext) {
      formulas :+ input.Input.parseAnnotated(externalIter.next())
    }
    val foSig = TypedFOLEncodingSignature()
    input.Input.processProblem(formulas)(foSig)
    formulas
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

