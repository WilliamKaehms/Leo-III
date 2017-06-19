package leo.modules

import leo.Configuration
import leo.datastructures._
import leo.modules.output.{ToTFF, ToTPTP}

/**
  * Created by lex on 3/22/17.
  */
package object external {
  object TPTPProblem {
    type Flag = Int
    final val NONE: Flag = 0
    final val WITHDEF: Flag = 1
  }


  /** Return a clause representing the axiom of choice for symbol `symbol`, i.e.
    * return clause `C` with
    * {{{
    * C = [P X]^f \/ [P (symbol P)]^f
    * }}}
    * where `P` and `X` are free variables to the clause.
    */
  private final def acInstance(symbol: Signature.Key)(implicit sig: Signature): Clause = {
    val symbType = sig(symbol)._ty // (ty > o) > ty
    assert(symbType.isFunType)
    val ty0 = symbType._funDomainType // ty > o
    val resultTy = symbType.codomainType // ty
    assert(ty0.isFunType)
    val ty00 = ty0._funDomainType // ty
    val ty01 = ty0.codomainType // o
    assert(resultTy == ty00)
    assert(ty01 == HOLSignature.o)

    val P: Term = Term.mkBound(ty0, 1)
    val X: Term = Term.mkBound(ty00, 2)
    val eps: Term = Term.mkAtom(symbol)(sig)
    val negLit = Literal.mkLit(
      Term.mkTermApp(P, X)
      , false)
    val posLit = Literal.mkLit(
      Term.mkTermApp(P, Term.mkTermApp(eps, P))
      , true)
    Clause(Seq(negLit, posLit))
  }

  final def generateSpecialAxioms(sig: Signature): Set[Clause] = {
    var result: Set[Clause] = Set.empty
    val userSymbols = sig.uninterpretedSymbols.iterator
    while (userSymbols.hasNext) {
      val symb = userSymbols.next()
      if (isPropSet(Signature.PropChoice, sig(symb).flag)) {
        // include choice axiom for symb
        result += acInstance(symb)(sig)
      } // ... more to come
    }
    result
  }

  final def createTFFProblem(problem: Set[Clause])(implicit sig: Signature): String = {
    val problemIt = problem.iterator
    val leoVersion = Configuration.VERSION
    val sb: StringBuffer = new StringBuffer
    sb.append(s"%%% This file was generated by Leo-III $leoVersion")
    sb.append("\n\n")
    sb.append(s"%% Explicit typings")
    sb.append("\n")
    sb.append(ToTFF(sig))
    sb.append("\n\n")
    sb.append(s"%% User axioms")
    sb.append("\n")
    var counter: Int = 1
    while (problemIt.hasNext) {
      val cl = problemIt.next()
      sb.append(ToTFF(cl, Role_Axiom, s"ax_$counter"))
      sb.append("\n")
      counter += 1
    }
    sb.toString
  }

  final def createTHFProblem(problem: Set[Clause], flag: TPTPProblem.Flag = TPTPProblem.NONE, conjecture: Clause = null)(implicit sig: Signature): String = {
    val problemIt = problem.iterator
    val leoVersion = Configuration.VERSION
    val sb: StringBuffer = new StringBuffer
    sb.append(s"%%% This file was generated by Leo-III $leoVersion")
    sb.append("\n\n")
    sb.append(s"%% Explicit typings")
    sb.append("\n")
    sb.append(ToTPTP(sig))
    sb.append("\n\n")
    if (leo.datastructures.isPropSet(TPTPProblem.WITHDEF, flag)) {
      sb.append(s"%% Definitions")
      sb.append("\n")
      sb.append(ToTPTP.printDefinitions(sig))
      sb.append("\n\n")
    }
    sb.append(s"%% User axioms")
    sb.append("\n")
    var counter: Int = 1
    while (problemIt.hasNext) {
      val cl = problemIt.next()
      sb.append(ToTPTP.toTPTP(s"ax_$counter", cl, Role_Axiom)(sig))
      sb.append("\n")
      counter += 1
    }
    if (conjecture != null) {
      sb.append("\n\n")
      sb.append(s"%% Conjecture\n")
      sb.append(ToTPTP.toTPTP("conjecture", conjecture, Role_Conjecture)(sig))
    }
    sb.toString
  }

}
