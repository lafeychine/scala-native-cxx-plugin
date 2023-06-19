package scala.scalanative.unsafe.cxx

import scala.annotation.StaticAnnotation

/** An annotation that is used to mark objects that contain externally-defined C++ members */
final class externCxx(val namespace: String = "") extends StaticAnnotation
