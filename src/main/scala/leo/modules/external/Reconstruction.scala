/*
package leo.modules.external

/**
  * TODO Create some Code
  * @param myParam Some test
  */
object Reconstructor {

    private def printExternalProofObject(): Unit = try {
      assert(terminated)
      val exitCode = process0.exitValue()
      val stdin = scala.io.Source.fromInputStream(process0.getInputStream).getLines().toSeq
      val stderr = scala.io.Source.fromInputStream(process0.getErrorStream).getLines().toSeq

      if (Configuration.isSet("atpdebug") & ) {
        val answer = stdin.mkString("\n")
        leo.Out.output("#############################")
        leo.Out.output("name:" + name)
        leo.Out.output("--------------------")
        leo.Out.output("output:" + answer)
        leo.Out.output("--------------------")
        if (answer.contains("error")) {
           leo.Out.output("+-+-+-+-+-+-+-+-+-+-+-+-+ Melon Melon!")
           originalProblem.foreach(c => leo.Out.output(c.pretty))
           leo.Out.output("+-+-+-+-+-+-+-+-+-+-+-+-+ Melon Melon!")
           throw new Error
        }
      }
      val errorMsg = stderr.mkString("\n")
      if (errorMsg != "") leo.Out.warn(s"Error message from $name:\n$errorMsg")

      val szsAnswer = atpAnswerToSZS(stdin.iterator)
      result = new TptpResultImpl(originalProblem, szsAnswer, exitCode,
        stdin, stderr)
    } catch {
      case e : Exception =>
        val error = if(Configuration.isSet("atpdebug")) Seq(e.toString) else Seq()
        result =  new TptpResultImpl(originalProblem, SZS_Error, -1,
        Seq(), error)
    }


}
*/