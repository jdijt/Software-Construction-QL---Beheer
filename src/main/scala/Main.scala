import java.io.FileReader

import parser._

object Main extends App {
  val filename = "src/main/resources/example.ql"
  val parsedForm = FormParser(new FileReader(filename))

  private def printIssues(issues: Iterable[Issue]) = issues.foreach {
    case Warning(message) => println(s"${Console.YELLOW}[WARNING] ${Console.RESET}$message")
    case Error(message) => println(s"${Console.RED}[ERROR] ${Console.RESET}$message")
  }

  FormChecker(parsedForm) match {
    case Left(issues) => printIssues(issues)
    case Right((form, warnings)) =>
      println(form.questions.mkString("\n"))
      printIssues(warnings)
  }
}
