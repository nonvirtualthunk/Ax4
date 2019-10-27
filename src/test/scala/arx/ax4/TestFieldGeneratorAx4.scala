package arx.ax4

import arx.core.introspection.FieldGenerator
import org.scalatest.FunSuite

class TestFieldGeneratorAx4 extends FunSuite {

	test("generate fields for test data") {
		FieldGenerator.generate("arx.ax4", testClasses = true)
	}

}
