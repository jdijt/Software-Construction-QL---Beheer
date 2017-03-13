package model

import ast.Stylesheet.{ Blocks, DefaultStyles, QuestionStyles }
import ast._

import scala.annotation.tailrec

class StyleModel(stylesheet: Stylesheet, questionWithTypes: Map[String, Type]) {
  val defaultStyles: DefaultStyles = stylesheet.pages.flatMap(extractDefaultStyles)
  val questionStyles: QuestionStyles = extractQuestionStyles(stylesheet.pages.flatMap(_.sections))
  val referencedQuestions: Seq[String] = questionStyles.map(_.identifier)
  val blocksWithResolvedStyles: Seq[(Page, Blocks)] = stylesheet.pages.map {
    case p @ Page(_, sections, defaultStyles) => (p, flattenStyles(sections, mergeDefaultStyles(defaultStyles, Map.empty)))
  }

  private def flattenStyles(blocks: Blocks, defaultStyles: Map[Type, DefaultStyle]): Blocks =
    blocks.flatMap(b => flattenStyles(b, defaultStyles))

  private def flattenStyles(block: Block, defaultStyles: Map[Type, DefaultStyle]): Blocks =
    block match {
      case s @ Section(_, blocks, styles) => s +: flattenStyles(blocks, mergeDefaultStyles(styles, defaultStyles))
      case QuestionStyle(identifier, styling, widget) => {
        val questionType = questionWithTypes.get(identifier) match {
          case Some(qt) => qt
          case None => sys.error("Unable to determine question type for styling.")
        }
        defaultStyles.get(questionType) match {
          case None => Seq(QuestionStyle(identifier, styling, widget))
          case Some(DefaultStyle(t, defaultStyling, defaultWidget)) if t == questionType =>
            Seq(QuestionStyle(identifier, defaultStyling ++ styling, mergeWidgetType(widget, defaultWidget)))
          case _ => sys.error("Type mismatch during flattening of style hierarchy")
        }
      }
    }

  @tailrec
  private def mergeDefaultStyles(newStyles: DefaultStyles, knownStyles: Map[Type, DefaultStyle]): Map[Type, DefaultStyle] = newStyles match {
    case Nil => knownStyles
    case (d @ DefaultStyle(newType, newStyling, newWidget)) :: tail => {
      val updatedStyles = knownStyles.get(newType) match {
        case None => knownStyles + (newType -> d)
        case Some(DefaultStyle(knownType, knownStyling, knownWidget)) if newType == knownType =>
          knownStyles + (newType -> DefaultStyle(newType, knownStyling ++ newStyling, mergeWidgetType(newWidget, knownWidget)))
        case _ => sys.error("Type mismatch during flattening of style hierarchy")
      }
      mergeDefaultStyles(tail, updatedStyles)
    }
  }

  private def mergeWidgetType(newW: Option[Widget], oldW: Option[Widget]): Option[Widget] = newW match {
    case None => oldW
    case Some(w) => Some(w)
  }

  private def extractDefaultStyles(page: Page): DefaultStyles = page match {
    case Page(_, sections, default) => default ++ extractDefaultStyles(sections)
  }

  private def extractDefaultStyles(blocks: Blocks): DefaultStyles =
    blocks.flatMap(b => extractDefaultStyles(b))

  private def extractDefaultStyles(block: Block): DefaultStyles = block match {
    case Section(_, sections, default) => default ++ extractDefaultStyles(sections)
    case _: QuestionStyle => Nil
  }

  private def extractQuestionStyles(blocks: Blocks): QuestionStyles =
    blocks.flatMap(b => extractQuestionStyles(b))

  private def extractQuestionStyles(block: Block): QuestionStyles = block match {
    case Section(_, blocks, _) => extractQuestionStyles(blocks)
    case q: QuestionStyle => Seq(q)
  }
}
