package leo.modules.phase

import leo.agents._
import leo.agents.impl.{FiniteHerbrandEnumerateAgent, _}
import leo.datastructures._
import leo.datastructures.blackboard._
import leo.datastructures.blackboard.impl.{FormulaDataStore, SZSDataStore, SZSStore}
import leo.datastructures.blackboard.scheduler.Scheduler
import leo.datastructures.context.{BetaSplit, Context}
import leo.datastructures.impl.Signature
import leo.datastructures.term.Term
import leo.modules.normalization.{DefExpansion, NegationNormal, Simplification, Skolemization}
import leo.modules.output._
import leo.modules.proofCalculi.enumeration.SimpleEnum
import leo.modules.proofCalculi.splitting.ClauseHornSplit
import leo.modules.proofCalculi.{IdComparison, Paramodulation, PropParamodulation}


object Phase {
  def getStdPhases : Seq[Phase] = List(new LoadPhase(true), SimplificationPhase, ParamodPhase)
  def getHOStdPhase : Seq[Phase] = List(new LoadPhase(true), PreprocessPhase, SimpleEnumerationPhase, ParamodPhase)
  def getSplitFirst : Seq[Phase] = List(new LoadPhase(true), PreprocessPhase, ExhaustiveClausificationPhase, SplitPhase, ParamodPhase)
  def getCounterSat : Seq[Phase] =  List(new LoadPhase(false), FiniteHerbrandEnumeratePhase, PreprocessPhase, ParamodPhase)
  def getCounterSatRemote : Seq[Phase] =  List(new LoadPhase(false), FiniteHerbrandEnumeratePhase, RemoteCounterSatPhase)
  def getExternalPhases : Seq[Phase] = List(new LoadPhase(true), PreprocessPhase, ExternalProverPhase)

  /**
   * Creates a complete phase from a List of Agents.
   *
   * @param dname - Name of the Phase
   * @param dagents - Agents to be used in this phase.
   * @return - A phase executing all agents until nothing is left to do.
   */
  def apply(dname : String, dagents : Seq[AgentController]) : Phase = new CompletePhase {
    override protected def agents: Seq[AgentController] = dagents
    override def name: String = dname
  }
}

/**
 * Trait for a MainPhase in Leo-III
 *
 * @author Max Wisniewski
 * @since 12/1/14
 */
trait Phase {
  /**
   * Executes the Phase.
   *
   * @return true, if the phase was performed successful and the next phase is allowed to commence. false, otherwise
   */
  def execute() : Boolean

  /**
   * Returns the name of the phase.
   * @return
   */
  def name : String

  /**
   * Returns a short description and
   * all agents, that were started, for this phase.
   *
   * @return
   */
  lazy val description : String = s"  Agents used:\n    ${agents.map(_.name).mkString("\n    ")}"

  /**
   * A list of all agents to be started.
   * @return
   */
  protected def agents : Seq[AgentController]

  /**
   * Method to start the agents, defined in `agents`
   */
  protected def init() : Unit = {
    agents.foreach(_.register())
  }

  /**
   * Method to finish the agents.
   */
  protected def end() : Unit = {
    Scheduler().pause()
    agents.foreach(_.unregister())
    Scheduler().clear()
  }
}

/**
 * Abstract Phase, that implements
 * the execute to start the agents and wait for all to finish.
 */
trait CompletePhase extends Phase {
  private def getName = name
  protected var waitAgent : CompleteWait = null
  private var wController : AgentController = null


  def initWait() : Unit = {
    waitAgent = new CompleteWait
    wController = new FifoController(waitAgent)
    wController.register()
  }

  override def end() : Unit = {
    super.end()
    wController.unregister()
    wController = null
    waitAgent = null
  }

  /**
   * Waits until the Wait Agent signals
   * the end of the execution
   *
   * @return true, if the execution was sucessfull, false otherwise
   */
  def waitTillEnd() : Boolean = {
    Scheduler().signal()
    waitAgent.synchronized{while(!waitAgent.finish) waitAgent.wait()}
    if(waitAgent.scedKill) return false
    return true
  }

  /**
   * Executes all defined agents and waits till no work is left.
   */
  override def execute() : Boolean = {
    // Starting all agents and signal scheduler
    init()
    initWait()

    if(!waitTillEnd()) return false
    // Ending all agents and clear the scheduler
    end()

    // If executing till the end, we will always return true, if other behaviour is wished, it has to be implemented
    return true
  }

  protected class CompleteWait extends Agent {
    var finish = false
    var scedKill = false
    override def toFilter(event: Event): Iterable[Task] = event match {
      case d : DoneEvent =>
        synchronized{finish = true; notifyAll()};List()
      case DataEvent(SZSStore(s,c), StatusType) if c.parentContext == null && c.isClosed => // The root context was closed
        synchronized{finish = true; notifyAll()};List()
      case _ => List()
    }
    override def name: String = s"${getName}Terminator"
    override def run(t: Task): Result = Result()
    override def kill(): Unit = synchronized{
      scedKill = true
      finish = true
      notifyAll()
    }
  }
}


class LoadPhase(negateConjecture : Boolean, problemfile: String = Configuration.PROBLEMFILE) extends Phase{
  override val name = "LoadPhase"

  override val agents : Seq[AgentController] = if(negateConjecture) List(new FifoController(new ConjectureAgent)) else Nil

  var finish : Boolean = false

  override def execute(): Boolean = {
    val file = problemfile
    val wait = new FifoController(new Wait(this))

    if(negateConjecture) {
      init()
      Scheduler().signal()
    }
    try {
      Utility.load(file)
    } catch {
      case e : SZSException =>
        // Out.output(SZSOutput(e.status))
        SZSDataStore.forceStatus(Context())(e.status)
        return false
      case e : Throwable =>
        Out.severe("Unexpected Exception")
        e.printStackTrace()
        SZSDataStore.forceStatus(Context())(SZS_Error)
        //Out.output((SZSOutput(SZS_Error)))
        return false
    }
    if(negateConjecture) {
      wait.register()
      Scheduler().signal()
      synchronized {
        while (!finish) this.wait()
      }


      end()
      wait.unregister()
    }
    return true
  }

  private class Wait(lock : AnyRef) extends Agent{
    override def toFilter(event: Event): Iterable[Task] = event match {
      case d : DoneEvent => finish = true; Out.comment("GOt done event.");lock.synchronized(lock.notifyAll());List()
      case _ => List()
    }
    override def name: String = "PreprocessPhaseTerminator"
    override def run(t: Task): Result = Result()
  }
}


object DomainConstrainedPhase extends CompletePhase{
  override val name = "DomainConstrainedPhase"

  val da = new FifoController(new DomainConstrainedSplitAgent)

  override val agents : Seq[AgentController] = List(da)

  var finish : Boolean = false

  override def execute(): Boolean = {
    init()


    val card : Seq[Int] = Configuration.valueOf("card").fold(List(1,2,3)){s => try{(s map {c => c.toInt}).toList} catch {case _:Throwable => List(1,2,3)}}



    Blackboard().send(DomainConstrainedMessage(card),da)

    initWait()

    if(!waitTillEnd()) return false

    end()
    return true
  }
}

object SimpleEnumerationPhase extends CompletePhase {
  override val name = "SimpleEnumerationPhase"
  override lazy val description = "Agents used:\n    FiniteHerbrandEnumerationAgent"

  protected var agents: Seq[AgentController] = List(new FifoController(new FiniteHerbrandEnumerateAgent(Context(), Map.empty)))

  override def execute(): Boolean = {
    val s1 : Set[Type] = (Signature.get.baseTypes - 0 - 1 - 3 - 4 - 5).map(Type.mkType(_))
    val enumse : Map[Type, Seq[Term]] = s1.map{ty => (ty, SimpleEnum.enum(ty).toSeq)}.toMap
    val fhb = new FiniteHerbrandEnumerateAgent(Context(), enumse)
    Out.finest(enumse.toString())
    agents = List(new FifoController(fhb))

    init()
    initWait()

    if(!waitTillEnd()) return false

    FormulaDataStore.rmAll(Context()){f => f.clause.lits.exists{l => fhb.containsDomain(l.term)}}

    end()
    return true
  }
}

object FiniteHerbrandEnumeratePhase extends CompletePhase {
  override val name = "FiniteHerbrandEnumeratePhase"

  val size : Int = 3
  override lazy val description = "Agents used:\n    FiniteHerbrandEnumerationAgent"

  /**
   * A list of all agents to be started.
   * @return
   */
  protected var agents: Seq[AgentController] = List(new FifoController(new FiniteHerbrandEnumerateAgent(Context(), Map.empty))) // A bit of schmu, but I do not want to list the agents here

  /**
   * Executes the Phase.
   *
   * @return true, if the phase was performed successful and the next phase is allowed to commence. false, otherwise
   */
  override def execute(): Boolean = {
    if(!Context().split(BetaSplit, size)) {
      // Set context and reason???
      return false
    }

    agents = Nil

    val s1 : Set[Signature#Key] = Signature.get.baseTypes - 0 - 1 - 3 - 4 - 5// Without kind, numbers and boolean
    val s : Set[Type]= s1.map {k => Type.mkType(k)}

    var it : Int = 0
    var agents1 : Seq[FiniteHerbrandEnumerateAgent] = Nil

    val cs : Seq[Context] = Context().childContext.toList
    // Each context, assign the maximal number of elements per domain
    // Then generate teh new clauses and insert them into the blackboard.
    // If it is done build the agents from it.
    (1 to size).zip(cs).foreach { case (i,c1) =>
      // Generate and insert new constants
      val cons : Map[Type, Seq[Clause]] = s.map{ty => (ty, (1 to i).map{_ => newConstant(ty)}.toList)}.toMap

      //TODO Add some constraints?
      //cons.values.map(_.foreach{c => Blackboard().addFormula(s"domainConstrain_${c1.contextID}_${val s = it; it+=1; s}",c,Role_Axiom, c1)})

      // Generate an agent for this setting of domains
      val agent = new FiniteHerbrandEnumerateAgent(c1, cons.mapValues(_.map(_.lits.head.term)))
      agents1 = agent +: agents1
      agents = new FifoController(agent) +: agents
    }

    init()

    initWait()

    if(!waitTillEnd()) return false
    // Remove all formulas containing one of the domains. (Hacky. Move the Test Function to the module package.
    val  a : FiniteHerbrandEnumerateAgent = agents1.head

    FormulaDataStore.rmAll(Context()){f => f.clause.lits.exists{l => a.containsDomain(l.term)}}

    end()

    return true
  }

  private def newConstant(ty : Type) : Clause = {
    val s = Signature.get
    return Clause.mkClause(List(Literal(s.freshSkolemVar(ty), true)), Derived)
  }
}

object ExternalProverPhase extends CompletePhase {
  override def name: String = "ExternalProverPhase"

  lazy val prover = if (Configuration.isSet("with-prover")) {
    Configuration.valueOf("with-prover") match {
      case None => throw new SZSException(SZS_UsageError, "--with-prover parameter used without <prover> argument.")
      case Some(str) => str.head match {
        case "leo2" => {
          val path = System.getenv("LEO2_PATH")
          if (path != null) {
            "scripts/leoexec.sh"
          } else {
            throw new SZSException(SZS_UsageError, "--with-prover used with LEO2 prover, but $LEO2_PATH is not set.")
          }
        }
        case "satallax" => {
          val path = System.getenv("SATALLAX_PATH")
          if (path != null) {
            "scripts/satallaxexec.sh"
          } else {
            throw new SZSException(SZS_UsageError, "--with-prover used with satallax prover, but $SATALLAX_PATH is not set.")
          }
        }
        case "remote-leo2" => "scripts/remote-leoexec.sh"
        case _ => throw new SZSException(SZS_UsageError, "--with-prover parameter used with unrecognized <prover> argument.")
      }
    }
  } else {
    throw new SZSException(SZS_Error, "This is considered an system error, please report this problem.", "CL parameter with-prover lost")
  }

  lazy val extProver : AgentController = new FifoController(SZSScriptAgent(prover)(x => x))


  override protected def agents: Seq[AgentController] = List(extProver)

  override def execute(): Boolean = {
    init()


  val conj = FormulaDataStore.getAll(_.role == Role_NegConjecture).head
  Blackboard().send(SZSScriptMessage(conj)(conj.context), extProver)

  initWait()

  if(!waitTillEnd()) return false

  end()
  return true


  }
}

/**
 * Invokes external scripts if the context was split previoulsy.
 */
object RemoteCounterSatPhase extends CompletePhase {
  override def name: String = "RemoteCounterSatPhase"

  val da : AgentController = new FifoController(SZSScriptAgent("scripts/leoexec.sh")(reInt))

  override protected def agents: Seq[AgentController] = List(da)

  private def reInt(in : StatusSZS) : StatusSZS = in match {
    case SZS_Theorem => SZS_CounterSatisfiable    // TODO Sat -> Countersat
    case e => e
  }
  var finish : Boolean = false

  override def execute(): Boolean = {
    init()


    //val maxCard = Configuration.valueOf("maxCard").fold(3){s => try{s.head.toInt} catch {case _ => 3}}

    // Send all messages
    val it = Context().childContext.iterator
    var con : FormulaStore = null
    try {
      con = FormulaDataStore.getAll(_.role == Role_Conjecture).head
    } catch {
      case _: Throwable => end(); return false
    }
    while(it.hasNext) {
      val c = it.next()
      Blackboard().send(SZSScriptMessage(con.newContext(c))(c), da)
    }

    initWait()

    Scheduler().signal()
    synchronized{while(!finish) this.wait()}

    end()
    return true
  }
}

object PreprocessPhase extends CompletePhase {
  override val name = "PreprocessPhase"
  override protected val agents: Seq[AgentController] = List(new FifoController(new NormalClauseAgent(DefExpansion)), new FifoController(new NormalClauseAgent(Simplification)), new FifoController(new NormalClauseAgent(NegationNormal)),new FifoController(new NormalClauseAgent(Skolemization)))
}

object SimplificationPhase extends CompletePhase {
  override val name = "PreprocessPhase"
  override protected val agents: Seq[AgentController] = List(new FifoController(new NormalClauseAgent(DefExpansion)), new FifoController(new NormalClauseAgent(Simplification)))
}

object ExhaustiveClausificationPhase extends CompletePhase {
  override val name = "ClausificationPhase"
  override protected val agents : Seq[AgentController] = List(new FifoController(new ClausificationAgent()))
}

object SplitPhase extends CompletePhase {
  override val name = "SplitPhase"
  override protected val agents: Seq[AgentController] = List(new FifoController(new SplittingAgent(ClauseHornSplit)))
}

object ParamodPhase extends CompletePhase {
  override val name : String = "ParamodPhase"
  override protected val agents: Seq[AgentController] = List(new PriorityController(new ParamodulationAgent(Paramodulation, IdComparison)), new PriorityController(new ParamodulationAgent(PropParamodulation, IdComparison)), new PriorityController(new ClausificationAgent()))
}