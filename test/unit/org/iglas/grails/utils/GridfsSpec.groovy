package org.iglas.grails.utils

import grails.plugin.spock.UnitSpec
import grails.test.mixin.*

class GridfsSpec extends UnitSpec {
	
	def "makeConfig should filter and update user config controller settings"() {
		
		given: "a mock user config"
			def initialUserConfig = [
				thumbconfig: [
					publicdir:"tmp/imagesthumb/[idparent]",
					x_size:128,
					y_size:128
				],
				controllers: [
					errorController:	"gridfs",
					errorAction:		"help",
					successController:	"gridfs",
					successAction:		"help"
				] 
			] 
		and: "some values to update"
			def updates = [
				successController:	"home",
				successAction:		"index",
				somethingElse:		"not needed by user config"
			]
		
		when: "making a new config"
			def result = Gridfs.makeConfig(initialUserConfig, updates)
			
		then: "a result is given"
			result != null
		and: "updates have been applied"
			result ==  [
				controllers:	[
					successController:	"home",
					successAction:		"index"
				]
			]
		
	}

}
