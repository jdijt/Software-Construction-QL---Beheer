package model

import ast._

class FormModel(form: Form) {
  val questionsWithShowConditions: Seq[(Question, Seq[ExpressionNode])] = form.block.statements.flatMap(flattenForm(_, Nil))

  val expressions: Seq[(ExpressionNode, Type)] = extractExpressions(form)

  val questions: Seq[Question] = questionsWithShowConditions.map { case (q, _) => q }

  val questionLabels: Seq[String] = questions.map(_.label)

  val definedIdentifiers: Seq[String] = questions.map(_.identifier)

  val identifiersWithType: Seq[(String, Type)] = questions.map(q => (q.identifier, q.`type`))

  val referencedIdentifiers: Set[String] = expressions.map { case (e, _) => extractIdentifiers(e) }.reduce(_ ++ _)

  val displayQuestions: Seq[DisplayQuestion] = questionsWithShowConditions.map {
    case (Question(i, l, t, Some(e)), conditions) => ComputedQuestion(i, l, t, conditions, e)
    case (Question(i, l, t, None), conditions) => OpenQuestion(i, l, t, conditions)
  }

  val questionsWithReferences: Map[String, Set[String]] = questionsWithShowConditions.map {
    case (Question(identifier, _, _, None), conditionals) => (identifier, extractIdentifiers(conditionals))
    case (Question(identifier, _, _, Some(expr)), conditionals) => (identifier, extractIdentifiers(expr) ++ extractIdentifiers(conditionals))
  }.toMap

  private def extractExpressions(parentNode: FormNode): Seq[(ExpressionNode, Type)] = parentNode match {
    case Question(_, _, questionType, Some(expr)) => Seq((expr, questionType))
    case Conditional(expr, block) => (expr, BooleanType) +: extractExpressions(block)
    case Block(statements) => statements.flatMap(e => extractExpressions(e))
    case Form(_, block) => extractExpressions(block)
    case _ => Nil
  }

  private def extractIdentifiers(expressionNodes: Seq[ExpressionNode]): Set[String] = expressionNodes match {
    case Nil => Set.empty
    case e :: es => extractIdentifiers(e) ++ extractIdentifiers(es)
  }

  private def extractIdentifiers(expressionNode: ExpressionNode): Set[String] = expressionNode match {
    case Identifier(value) => Set(value)
    case i: InfixNode => extractIdentifiers(i.lhs) ++ extractIdentifiers(i.rhs)
    case p: PrefixNode => extractIdentifiers(p.operand)
    case _ => Set.empty
  }

  private def flattenForm(statement: Statement, conditionals: Seq[ExpressionNode]): Seq[(Question, Seq[ExpressionNode])] = statement match {
    case Conditional(condition, Block(statements)) => statements.flatMap(flattenForm(_, condition +: conditionals))
    case q: Question => Seq((q, conditionals))
  }
}