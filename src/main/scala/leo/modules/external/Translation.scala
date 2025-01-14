package leo.modules.external

import leo.modules.input
import leo.modules.output._
import leo.datastructures.{Role, Signature, Term, TPTP}
import javax.management.relation.Role
import leo.modules.external

/**
  * TODO Create some Code
  * @param myParam Some test
  */
object Translation {

  final def apply(externalResult: String): String = {
    //val res = cleanExternalResult(externalResult.split("\n"))
    val res = filterProof(externalResult.split("\n"))
    val test = deleteAddedDeclaration(convertToAnnotaded(res))
    leo.Out.output(convertedToString(test).mkString("\n"))
    res.mkString("\n")
  }

  private val filterProof = (externalResult: Seq[String]) => 
    externalResult.filter(line => line.contains("tff(") || line.contains("tcf("))

  private val convertToAnnotaded = (externalResult: Seq[String]) => for {
    line <- externalResult
  } yield input.Input.parseAnnotated(line)

  private val deleteAddedDeclaration = (externalResult: Seq[TPTP.AnnotatedFormula]) => externalResult.filter(line => 
    !(line.symbols.contains("$tType") && setContainsxx(line.symbols)) )

  private def setContainsxx(set: Set[String]): Boolean = {
    val setW = for { element <- set } yield element.startsWith("xx")
    setW.contains(true)
  }

  private val convertedToString = (externalResult: Seq[TPTP.AnnotatedFormula]) =>
    externalResult.map(line => line.pretty)
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

