package scala
package scalanative
package nscplugin

import dotty.tools.dotc.plugins.{PluginPhase, StandardPlugin}

class ScalaNativeCxxPlugin extends StandardPlugin:
    val name = "scalanative-cxx"
    val description = "Scala Native: C++ support"

    def init(options: List[String]): List[PluginPhase] = List(ScalaNativeCxx())
