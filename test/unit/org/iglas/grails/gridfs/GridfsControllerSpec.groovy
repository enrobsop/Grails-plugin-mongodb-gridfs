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
		configureWith()
	}
	
	def "upload should fail when idparent is missing"() {
		given: "no idparent param"
			// nothing
		
		when: "attempt upload"
			controller.upload()
			
		then: "an error message is given"
			flash.message != null
			flash.message =~ ~/(?i).*invalid.*/
		and: "the client is redirected to the error controller"
			response.redirectUrl == "/error/index"
	}
	
	def "upload should fail when file is missing"() {
		
		given: "no file"
		and: "the other rrequired params"
			def theId = "anId"
			params.idparent = theId
		
		when: "upload is attempted"
			controller.upload()
			
		then: "an error message is given"
			flash.message != null
			flash.message =~ ~/(?i).*no file.*/
		and: "the client is redirected to the error controller"
			response.redirectUrl == "/error/index/$theId"

	}
	
	def "upload should fail when file is empty"() {
		
		given: "an empty file"
			def theFile = new MockMultipartFile('file', 'empty.jpg', 'image/jpeg', '' as byte[])
			request.addFile(theFile)

		and: "the other required params"
			def theId = "anId"
			params.idparent = theId
			
		when: "upload is attempted"
			controller.upload()
			
		then: "an error message is given"
			flash.message != null
			flash.message =~ ~/(?i).*no file.*/
		and: "the client is redirected to the error controller"
			response.redirectUrl == "/error/index/$theId"
			// compare with redirectUrl for invalid file extension... should they use the same ID?			
	}
	
	def "upload should fail when file extension is not on allowed list"() {
		
		given: "a file with a disallowed extension"
			def disallowedExtension	= "bad"
			def theFile				= new MockMultipartFile('file', "aFile.$disallowedExtension", 'image/jpeg', '123' as byte[])
			request.addFile(theFile)

		and: "the other required params"
			def theId = "anId"
			params.idparent = theId
			def paramId = "123"
			params.id = paramId
		
		when: "upload is attempted"
			controller.upload()
			
		then: "an error message is given"
			flash.message != null
			flash.message =~ ~/(?i).*unauthorized extension.*${disallowedExtension}.*/
			flash.message =~ ~/(?i).*allowed extensions.*jpg.*/
		and: "the client is redirected to the error controller"
			response.redirectUrl == "/error/index/$paramId"
			// compare with redirectUrl for missing file... should they use the same ID?
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
				successAction:		"index",
				errorController:	"error",
				errorAction:		"index",
			]
		]
		def config = defaultConfig << options 
		
		def configHelper = Mock(ConfigHelper)
		configHelper.getConfig(_) >> config
		controller.configHelper = configHelper
	}
	
}
