package org.iglas.grails.utils

import org.iglas.grails.gridfs.GridfsService

class ConfigHelper {

	def getConfig(params) {
		new UserConfig(GridfsService.configName).get(Gridfs.makeConfig(params))
	}
	
}
