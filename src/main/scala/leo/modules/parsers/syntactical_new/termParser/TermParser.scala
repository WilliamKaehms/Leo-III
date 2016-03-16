package leo.modules.parsers.syntactical_new.termParser

import leo.modules.parsers.lexical.TPTPTokens
import leo.datastructures.tptp.Commons._

import leo.modules.parsers.syntactical_new.combinators.Combinators

/**
  * Created by samuel on 10.03.16.
  */
object TermParser
  extends TPTPTokens
  with Combinators
{
  def parse(tokens: Seq[Token]) =
    z0(List.empty)(tokens).right flatMap {
      case (TermEntry(term) :: Nil, restTokens) =>
        Right((term, restTokens))
      case _ =>
        Left("Stack is not empty!")
    }

  /* this is the type of the nodes of the abstract syntax tree: */
  abstract sealed class StackEntry
  case class TokenEntry(data: Token) extends StackEntry
  case class TermEntry(data: Term) extends StackEntry
  case class NumberEntry(data: Number) extends StackEntry
  case class TermEntries(data: List[Term]) extends StackEntry

  type PStack = List[StackEntry]
  type PP = Parser[Token, PStack]

  import scala.reflect.ClassTag


  // recognize one token of type "T"
  def tokenP[T <: Token](s: PStack)(implicit ct: ClassTag[T]) = {
    for( t <- terminalParse )
      yield( TokenEntry(t) :: s )
  }

  // this is for recognizing tokens which are modeled as "objects" (e.g.: Comma, LeftBracket, ...)
  def uniqueTokenP(x: Token)(s: PStack) = {
    for(
      y <- parseIf((y: Token) => "")(_ == x)
    )
    yield( TokenEntry(y) :: s )
  }

  /*
  Z0 ->
     "upper_word"  action_2
    |"single_quoted"   Z12
    |"real" action_20 action_16
    |"rational" action_19 action_16
    |"lower_word"   Z12
    |"integer" action_18 action_16
    |"dollar_word"   Z14
    |"dollar_dollar_word"   Z15
    |"distinct_object" action_17
  */
  def z0(s: PStack): PP = (
    (tokenP[UpperWord](s) map action_2)
    | (tokenP[SingleQuoted](s) flatMap z12)
    | (tokenP[Real](s) map action_20 map action_16)
    | (tokenP[Rational](s) map action_19 map action_16)
    | (tokenP[LowerWord](s) flatMap z12)
    | (tokenP[Integer](s) map action_18 map action_16)
    | (tokenP[DollarWord](s) flatMap z14)
    | (tokenP[DollarDollarWord](s) flatMap z15)
    | (tokenP[DistinctObject](s) map action_17)
  )

  /*
  Z12 ->
      action_6
    |"(" Z2
  */
  def z12(s: PStack): PP = (
    (uniqueTokenP(LeftParenthesis)(s) flatMap (z2(_)))
    | (ret(s) map (action_6(_)))
  )

  /*
  Z2 ->
     "upper_word"  action_2 Z16
    |"single_quoted"   Z17
    |"real" action_20 action_16    Z16
    |"rational" action_19 action_16    Z16
    |"lower_word"   Z17
    |"integer" action_18 action_16    Z16
    |"dollar_word"   Z22
    |"dollar_dollar_word"   Z23
    |"distinct_object" action_17    Z16
  */
  def z2(s: PStack): PP = (
    (tokenP[UpperWord](s) map (action_2(_)) flatMap (z16(_)))
    | (tokenP[SingleQuoted](s) flatMap (z17(_)))
    | (tokenP[Real](s) map (action_20(_)) map (action_16 (_)) flatMap (z16(_)))
    | (tokenP[Rational](s) map (action_19(_)) map (action_16 (_)) flatMap (z16(_)))
    | (tokenP[LowerWord](s) flatMap (z17(_)))
    | (tokenP[Integer](s) map (action_18(_)) map (action_16 (_)) flatMap (z16(_)))
    | (tokenP[DollarWord](s) flatMap (z22(_)))
    | (tokenP[DollarDollarWord](s) flatMap (z23(_)))
    | (tokenP[DistinctObject](s) map (action_17(_)) flatMap (z23(_)))
  )
  /*(
    terminalParse[UpperWord] flatMap (z16(_))
    | terminalParse[SingleQuoted] flatMap (z17(_))
  )
  */

  /*
  Z16 ->
    "," Z5 Z3
    | action_12 Z3
  */
  def z16(s: PStack): PP = (
    (uniqueTokenP(Comma)(s) flatMap (z5(_)) flatMap (z3(_)))
    | (ret(s) map action_12 flatMap z3)
  )

  /*
  Z3 ->
     ")" action_7
  */
  def z3(s: PStack): PP = (
    uniqueTokenP(RightParenthesis)(s) map action_7
  )

  /*
  Z5 ->
     "upper_word"  action_2 Z67
    |"single_quoted"   Z68
    |"real" action_20 action_16    Z67
    |"rational" action_19 action_16    Z67
    |"lower_word"   Z68
    |"integer" action_18 action_16    Z67
    |"dollar_word"   Z73
    |"dollar_dollar_word"   Z74
    |"distinct_object" action_17    Z67
 */
  def z5(s: PStack): PP = (
    (tokenP[UpperWord](s) map action_2 flatMap z67)
    | (tokenP[SingleQuoted](s) flatMap z68)
    | (tokenP[Real](s) map action_20 map action_16 flatMap z67)
    | (tokenP[Rational](s) map action_19 map action_16 flatMap z67)
    | (tokenP[LowerWord](s) flatMap z68)
    | (tokenP[Integer](s) map action_18 map action_16 flatMap z67)
    | (tokenP[DollarWord](s) flatMap z73)
    | (tokenP[DollarDollarWord](s) flatMap z74)
    | (tokenP[DistinctObject](s) map action_17 flatMap z67)
  )

  /*
   Z67 ->

     |"," Z5 action_13
  */
  def z67(s: PStack): PP = (
    (uniqueTokenP(Comma)(s) flatMap z5 map action_13)
    | (ret(s) map action_12 map action_13)
  )

  /*
   Z68 ->
       action_6   Z67
     |"(" Z2   Z67
  */
  def z68(s: PStack): PP = (
    (uniqueTokenP(LeftParenthesis)(s) flatMap z2 flatMap z67)
    | (ret(s) map action_6 flatMap z67)
  )

  /*
   Z73 ->
            Z67
     |"(" Z7     Z67
  */
  def z73(s: PStack): PP = (
    (uniqueTokenP(LeftParenthesis)(s) flatMap z7 flatMap z67)
    | z67(s)
  )

  /*
   Z7 ->
      "upper_word"  action_2 Z33
     |"single_quoted"   Z34
     |"real" action_20 action_16    Z33
     |"rational" action_19 action_16    Z33
     |"lower_word"   Z34
     |"integer" action_18 action_16    Z33
     |"dollar_word"   Z39
     |"dollar_dollar_word"   Z40
     |"distinct_object" action_17    Z33
  */
  def z7(s: PStack): PP = (
    (tokenP[UpperWord](s) map action_2 flatMap z33)
    | (tokenP[SingleQuoted](s) flatMap z34)
    | (tokenP[Real](s) map action_20 map action_16 flatMap z33)
    | (tokenP[Rational](s) map action_19 map action_16 flatMap z33)
    | (tokenP[LowerWord](s) flatMap z34)
    | (tokenP[Integer](s) map action_18 map action_16 flatMap z33)
    | (tokenP[DollarWord](s) flatMap z39)
    | (tokenP[DollarDollarWord](s) flatMap z40)
    | (tokenP[DistinctObject](s) map action_17 flatMap z33)
  )

  /*
   Z33 ->
     action_12 Z8
     |"," Z5 Z8
  */
  def z33(s: PStack): PP = (
    (uniqueTokenP(Comma)(s) flatMap z5 flatMap z8)
    | (ret(s) map action_12 flatMap z8)
  )

  /*
   Z8 ->
      ")" action_23
   */
  def z8(s: PStack): PP = (
    uniqueTokenP(RightParenthesis)(s) map action_23
  )

   /*
   Z34 ->
       action_6   Z33
     |"(" Z2   Z33
  */
  def z34(s: PStack): PP = (
    (uniqueTokenP(LeftParenthesis)(s) flatMap z2 flatMap z33)
    | (ret(s) map action_6 flatMap z33)
  )

  /*
   Z39 ->
            Z33
     |"(" Z7     Z33
  */
  def z39(s: PStack): PP = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z7 flatMap z33 )
    | z33(s)
  )

  /*
   Z40 ->
          Z33
     |"(" Z10   Z33
  */
  def z40(s: PStack): PP = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z10 flatMap z33 )
    | z33(s)
  )

  /*
   Z10 ->
      "upper_word"  action_2 Z50
     |"single_quoted"   Z51
     |"real" action_20 action_16    Z50
     |"rational" action_19 action_16    Z50
     |"lower_word"   Z51
     |"integer" action_18 action_16    Z50
     |"dollar_word"   Z56
     |"dollar_dollar_word"   Z57
     |"distinct_object" action_17    Z50
  */
  def z10(s: PStack): PP = (
    (tokenP[UpperWord](s) map action_2 flatMap z50)
    | (tokenP[UpperWord](s) flatMap z51)
    | (tokenP[Real](s) map action_20 map action_16 flatMap z50)
    | (tokenP[Rational](s) map action_19 map action_16 flatMap z50)
    | (tokenP[LowerWord](s) flatMap z51)
    | (tokenP[Integer](s) map action_18 map action_16 flatMap z50)
    | (tokenP[DollarWord](s) flatMap z56)
    | (tokenP[DollarDollarWord](s) flatMap z57)
    | (tokenP[DistinctObject](s) map action_17 flatMap z50)
  )

  /*
   Z50 ->
     action_12 Z11
     |"," Z5 Z11
  */
  def z50(s: PStack): PP = (
    (uniqueTokenP(Comma)(s) flatMap z5 flatMap z11)
    | (ret(s) map action_12 flatMap z11)
  )

  /*
   Z11 ->
      ")" action_28
   */
  def z11(s: PStack): PP = (
    uniqueTokenP(RightParenthesis)(s) map action_28
  )

   /*
   Z51 ->
       action_6   Z50
     |"(" Z2   Z50
  */
  def z51(s: PStack): PP = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z2 flatMap z50 )
    | (ret(s) map action_6 flatMap z50)
  )

  /*
   Z56 ->
            Z50
     |"(" Z7     Z50
  */
  def z56(s: PStack): PP = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z7 flatMap z50 )
    | z50(s)
  )

  /*
   Z57 ->
          Z50
     |"(" Z10   Z50
  */
  def z57(s: PStack): PP = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z10 flatMap z50 )
    | z50(s)
  )

  /*
   Z74 ->
          Z67
     |"(" Z10   Z67
  */
  def z74(s: PStack): PP = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z10 flatMap z67 )
    | z67(s)
  )

  /*
   Z17 ->
       action_6   Z16
     |"(" Z2   Z16
  */
  def z17(s: PStack): PP = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z2 flatMap z16 )
    | (ret(s) map action_6 flatMap z16)
  )

  /*
   Z22 ->
            Z16
     |"(" Z7     Z16
  */
  def z22(s: PStack): PP = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z7 flatMap z16 )
    | z16(s)
  )

  /*
   Z23 ->
          Z16
     |"(" Z10   Z16
  */
  def z23(s: PStack): PP = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z10 flatMap z16 )
    | z16(s)
  )

  /*
   Z14 ->

     |"(" Z7
  */
  def z14(s: PStack): PP = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z7 )
    | ret(s)
  )

  /*
  Z15 ->

    |"(" Z10
  */
  def z15(s: PStack) = (
    ( uniqueTokenP(LeftParenthesis)(s) flatMap z10 )
    | ret(s)
  )

// -----------------------------------------------------------------
// actions
// -----------------------------------------------------------------

	/*
  action_2 { (variable:String) => Var(name=variable) }
    term -> variable

  */
  def action_2(s: PStack): PStack = s match {
    case TokenEntry(x: UpperWord) :: rest
      => TermEntry(Var(name=x.data)) :: rest
  }

  /*
  action_6 { (constant:LowerWord|SingleQuoted) => Func(name=constant.data, args=List[Term]()) }
	  plain_term -> constant

  */
  def action_6(s: PStack): PStack = s match {
    case TokenEntry(x: LowerWord) :: rest
      => TermEntry(Func(x.data, List.empty)) :: rest
    case TokenEntry(x: SingleQuoted) :: rest
      => TermEntry(Func(x.data, List.empty)) :: rest
  }

  /*
  action_7 { (functor:LowerWord|SingleQuoted, arguments:List[Term]) => Func(name=functor.data, args=arguments) }
	 plain_term -> functor "(" arguments ")"

  */
  def action_7(s: PStack) = s match {
    case TokenEntry(RightParenthesis) :: TermEntries(args) :: TokenEntry(LeftParenthesis) :: TokenEntry(x: LowerWord) :: rest
      => TermEntry(Func(x.data, args)) :: rest
    case TokenEntry(RightParenthesis) :: TermEntries(args) :: TokenEntry(LeftParenthesis) :: TokenEntry(x: SingleQuoted) :: rest
    => TermEntry(Func(x.data, args)) :: rest
    /*
    case _ =>
      println(s)
      s
    */
  }

  /*
  action_12 { (x: Term) => List[Term](x) }
    arguments -> term
  */
  def action_12(s: PStack) = s match {
    case TermEntry(term) :: rest => TermEntries(List(term)) :: rest
  }

  /*
  action_13 { (term: Term, arguments: List[Term]) => (term :: arguments) }
    arguments -> term "," arguments
  */
  def action_13(s: PStack) = s match {
    case TermEntries(arguments) :: TokenEntry(Comma) :: TermEntry(term) :: rest => TermEntries(term :: arguments) :: rest
  }

  /*
  action_16 { (number: Number) => NumberTerm(value=number) }
	  defined_atom -> number
  */
  def action_16(s: PStack) = s match {
    case NumberEntry(x) :: rest
      => TermEntry(NumberTerm(x)) :: rest
  }

  /*
  action_17 { (distinct_object:DistinctObject) => Distinct(data=distinct_object.data) }
	  defined_atom -> "distinct_object"
  */
  def action_17(s: PStack) = s match {
    case TokenEntry(x: DistinctObject) :: rest
      => TermEntry(Distinct(x.data)) :: rest
  }

  /*
  action_18 { (integer: Integer) => IntegerNumber(value=integer.value) }
	  number -> "integer"
   */
  def action_18(s: PStack) = s match {
    case TokenEntry(x: Integer) :: rest
      => NumberEntry(IntegerNumber(x.value)) :: rest
  }

  /*
  action_19 { (rational:Rational) => RationalNumber(rational.p: , rational.q) }
	  number -> "rational"
  */
  def action_19(s: PStack) = s match {
    case TokenEntry(x: Rational) :: rest
      => NumberEntry(RationalNumber(x.p, x.q)) :: rest
  }

  /*
  action_20 { (real:Real) => DoubleNumber(value=Math.power(real.coeff, real.exp)) }
	  number -> "real"
  */
  def action_20(s: PStack) = s match {
    case TokenEntry(x: Real) :: rest
      => NumberEntry(DoubleNumber(Math.pow(x.coeff, x.exp))) :: rest
  }

  /*
  action_23 { (defined_functor:DollarWord, arguments:List[Term]) => DefinedFunc(name=defined_functor.data, args=arguments) }
	  defined_plain_term -> defined_functor "(" arguments ")"
  */
  def action_23(s: PStack) = s match {
    case TokenEntry(RightParenthesis) :: TermEntries(args) ::TokenEntry(LeftParenthesis) :: TokenEntry(x: DollarWord) :: rest
      => TermEntry(DefinedFunc(x.data, args)) :: rest
  }

  /*
  action_28 { (system_functor:DollarDollarWord) => SystemFunc(name=system_functor.data, args=arguments) }
	  system_term -> system_functor "(" arguments ")"
  */
  def action_28(s: PStack) = s match {
    case TokenEntry(RightParenthesis) :: TermEntries(args) ::TokenEntry(LeftParenthesis) :: TokenEntry(x: DollarDollarWord) :: rest
    => TermEntry(SystemFunc(x.data, args)) :: rest
  }


  private def allAsTerm(x: PStack) = x map { case TermEntry(x) => x }

  def terminalParse[T <: Token](implicit ct: ClassTag[T]) : Parser[Token, T] =
    parseIf((t: Token) => s"token of type ${ } expected, but ${ t.toString } found!")(
      (t: Token) =>
        t.getClass == ct.runtimeClass
    ) map ((x) => x.asInstanceOf[T])
}

