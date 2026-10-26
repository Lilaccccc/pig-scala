package pig
package utils.route

import scala.annotation.tailrec
import scala.quoted.*

// 宏定义：编译时分析特定特质并创建所有实现类的实例
inline def initControllers[T]: List[T] = ${ initControllersImpl[T] }

def initControllersImpl[T: Type](using Quotes): Expr[List[T]] = {
  import quotes.reflect.*

  // 获取目标特质的符号
  val traitSymbol = TypeRepr.of[T].typeSymbol

  // 验证传入的类型确实是特质
  if !traitSymbol.flags.is(Flags.Trait) then report.errorAndAbort(s"${traitSymbol.name} is not a trait.")

  // 递归收集符号树，返回所有相关符号的列表
  def collect(
               sym: Symbol,
               visited: Set[String] = Set.empty
             ): List[Symbol] =
    // 终止条件：无效符号或已访问过的符号
    if sym.isNoSymbol || visited.contains(sym.fullName) then Nil
    else {
      // 过滤子符号：只处理包、类和类型定义，排除系统包，避免深度遍历系统符号
      val children = sym.declarations.filter { child =>
        (child.isPackageDef && (
          !sym.fullName.equals("scala") &&
            !sym.fullName.startsWith("scala") &&
            !sym.fullName.startsWith("java.") &&
            !sym.fullName.startsWith("dotty.")
          )) || child.isClassDef || child.isTypeDef
      }
      // 递归收集：当前符号 + 所有符合条件的子符号
      sym +: children.flatMap { child =>
        collect(child, visited + sym.fullName)
      }
    }

  // 查找根级符号所有者：向上定位到最顶层的包（如 pig），
  // 这样能扫描到 pig 下所有兄弟子包（pig.controller、pig.auth.controller 等）
  @tailrec
  def findRootOwner(sym: Symbol): Symbol =
    if sym.owner.isNoSymbol then sym
    else if sym.isPackageDef && !sym.fullName.contains(".") then sym
    else findRootOwner(sym.owner)

  // 检查类是否实现了目标特质，如果类实现了特质则返回 true，否则返回 false
  def implementsTrait(classSymbol: Symbol, traitSymbol: Symbol): Boolean =
    try {
      val classType = classSymbol.typeRef
      val traitType = traitSymbol.typeRef
      classType <:< traitType
    } catch case _: Exception => false

  // 收集所有符号 -> 去重 -> 过滤出符合条件的实现类
  val allSymbols = collect(findRootOwner(Symbol.spliceOwner)).distinct.collect {
    case classSymbol
      if classSymbol.isClassDef               // 必须是类定义
        && !classSymbol.flags.is(Flags.Trait) // 不能是特质本身
        && classSymbol != traitSymbol         // 排除目标特质
        && !classSymbol.name.startsWith("<")  // 排除编译器生成的符号（<none>, <init>等）
        && classSymbol.name.nonEmpty          // 确保名称不为空
        && implementsTrait(classSymbol, traitSymbol) => // 必须实现目标特质
      classSymbol
  }

  // 生成创建实例的代码列表
  val instances = allSymbols.map { cls =>
    New(TypeIdent(cls)).select(cls.primaryConstructor).appliedToNone.asExprOf[T]
  }
  Expr.ofList(instances)
}