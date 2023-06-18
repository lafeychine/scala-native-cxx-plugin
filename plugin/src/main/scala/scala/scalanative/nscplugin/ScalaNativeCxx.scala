package scala
package scalanative
package nscplugin

import dotty.tools.dotc.ast.tpd
import dotty.tools.dotc.core.Annotations.Annotation
import dotty.tools.dotc.core.Constants.Constant
import dotty.tools.dotc.core.Contexts.Context
import dotty.tools.dotc.core.Symbols.requiredClassRef
import dotty.tools.dotc.core.Types.{ConstantType, TypeRef}
import dotty.tools.dotc.plugins.PluginPhase
import dotty.tools.dotc.transform.Pickler

class ScalaNativeCxx extends PluginPhase:
    import tpd.*

    val phaseName = "scalanative-cxx"

    override val runsAfter = Set(Pickler.name)
    override val runsBefore = Set("scalanative-genNIR")

    private var cxxAnnotation: Boolean = false

    def externAnnotation(using Context): TypeRef = requiredClassRef("scala.scalanative.unsafe.extern")
    def nscNameAnnotation(using Context) = requiredClassRef("scala.scalanative.unsafe.name")

    override def prepareForTypeDef(tree: TypeDef)(using ctx: Context): Context =
        cxxAnnotation = false

        if !tree.isClassDef then return ctx

        for objectAnnotation <- tree.symbol.denot.annotations do
            if objectAnnotation.matches(externAnnotation.symbol.asClass) then cxxAnnotation = true

        ctx

    override def transformDefDef(tree: DefDef)(using Context): Tree =
        /* Check if a transform is required */
        if !cxxAnnotation then return tree

        /* Check if the method is expecting a binding */
        if tree.rhs.symbol.showFullName != "scala.scalanative.unsafe.extern" then return tree

        /* Apply the annotation */
        val constant = Constant("_ZN3FooC2Ev") // Currently, this is just an example
        val mangledNameAnnotation = Annotation(nscNameAnnotation, Literal(constant).withType(ConstantType(constant)))

        tree.symbol.addAnnotation(mangledNameAnnotation)
        tree
