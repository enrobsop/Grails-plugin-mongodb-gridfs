package org.iglas.grails.utils

import org.iglas.grails.gridfs.GridfsService

/**
 * Created by IntelliJ IDEA.
 * User: devel
 * Date: 28.02.12
 * Time: 21:04
 * To change this template use File | Settings | File Templates.
 */
class Gridfs {
	
    public static makeConfig(params){
        def config = new UserConfig(GridfsService.configName).get()
		makeConfig(config, params)
    }
	
	public static makeConfig(userConfig, withUpdates) {
		def newConfig = [controllers:[:]]
		userConfig.controllers.each { key, value ->
			if(withUpdates?."${key}") {
				newConfig.controllers[key] = withUpdates."${key}"
			}
		}
		newConfig
	}
	
}
