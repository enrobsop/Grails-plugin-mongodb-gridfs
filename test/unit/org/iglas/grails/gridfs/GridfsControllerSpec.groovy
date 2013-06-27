package org.iglas.grails.gridfs

import grails.plugin.spock.UnitSpec
import grails.test.mixin.*

import org.iglas.grails.utils.ConfigHelper
import org.springframework.mock.web.MockMultipartFile

import spock.lang.Shared

import com.mongodb.gridfs.GridFSInputFile

@TestFor(GridfsController)
class GridfsControllerSpec extends UnitSpec {

	@Shared gridfsService

	def setup() {
		gridfsService = Mock(GridfsService)
		controller.gridfsService = gridfsService		
	}	
	
	def "upload should forward to the successController when successType is 'forward'"() {
		
		given: "a mock file"
			def theFile = new MockMultipartFile('file', 'myImage.jpg', 'image/jpeg', 123 as byte[])
			theFile.size() >> 1234567L 
			request.addFile(theFile)
		and: "a configuration"
			configureWith([
				controllers:		[
					successController:	"home",
					successAction:		"afterUpload",
					successType:		"forward"
				]
			])
		and: "some params" 
			params.file					= theFile
			params.idparent 			= "testUser"
			params.successController 	= "mySuccessController"
			params.successAction		= "theAction"
		and: "'forward' on success is set"
			params.successType 			= "forward"
		and: "file is added okay"
			def theGridFile = []
			gridfsService.addToGridFS(_,_,_,_) >> theGridFile
		and: "upload access is given"
			gridfsService.attemptUpload(_, _) >> [isAllowed: true, msg: null]
		
		when: "uploaded successfully"
			controller.upload()
		
		then: "there are no error messages"
			flash.message == null
		and: "the request should be forwarded to the successController"
			assert response.forwardedUrl == "/grails/home/afterUpload.dispatch"
		
	}
	
	private def configureWith(options=[:]) {
		def defaultConfig = [
			allowedExtensions:	["jpg"],
			db:					[host: "mongoHost"],
			controllers:		[
				successController:	"home",
				successAction:		"index"
			]
		]
		def config = defaultConfig << options 
		println config
		
		def configHelper = Mock(ConfigHelper)
		configHelper.getConfig(_) >> config
		controller.configHelper = configHelper
	}
	
}
