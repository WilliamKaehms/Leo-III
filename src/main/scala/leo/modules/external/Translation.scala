package leo.modules.external

import leo.modules.input
import leo.modules.output._
import leo.datastructures.{Role, Signature, Term, TPTP}
import javax.management.relation.Role

/**
  * TODO Create some Code
  * @param myParam Some test
  */
object Translation {

  final def apply(externalResult: String): String = {
    val res = cleanExternalResult(externalResult.split("\n"))
    val test = deleteAddedDeclaration(convertToAnnotaded(res))
    leo.Out.output(test.mkString("\n"))
    res.mkString("\n")
  }

  def translateToTHF(externalResult: String): String = {
    val res = cleanExternalResult(externalResult.split("\n"))
    res.mkString("\n")
  } 
  private val cleanExternalResult = (externalResult: Seq[String]) => for {
    line0 <- externalResult.filter(line => line.contains("tff(") || line.contains("tcf("))
  } yield s"thf${line0.substring(3,line0.length())}"

  private val convertToAnnotaded = (externalResult: Seq[String]) => for {
    line <- externalResult
  } yield input.Input.parseAnnotated(line)

  private val deleteAddedDeclaration = (externalResult: Seq[TPTP.AnnotatedFormula]) => for {
    line <- externalResult
  } yield line.name
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

